package com.kidslanguage.companion

import com.kidslanguage.companion.core.asr.*
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.vosk.android.RecognitionListener

@OptIn(ExperimentalCoroutinesApi::class)
class VoskSpeechRecognizerEngineTest {
    private class Mic : OfflineSpeechMicrophone {
        lateinit var listener: RecognitionListener
        var startError: Throwable? = null
        var cancelError: Throwable? = null
        var stopError: Throwable? = null
        var shutdownError: Throwable? = null
        var started = true
        var stopFinal = false
        var cancelled = 0
        var shutdowns = 0
        override fun start(listener: RecognitionListener, timeoutMillis: Int): Boolean {
            assertEquals(10_000, timeoutMillis)
            this.listener = listener
            startError?.let { throw it }
            return started
        }
        override fun stop() {
            stopError?.let { throw it }
            if (stopFinal) listener.onFinalResult("{\"text\":\"hello\"}")
        }
        override fun cancel() {
            cancelled++
            cancelError?.let { throw it }
            // Real services can deliver a queued final while cancellation is in progress.
            if (::listener.isInitialized) listener.onFinalResult("{\"text\":\"stale\"}")
        }
        override fun shutdown() {
            shutdowns++
            shutdownError?.let { throw it }
        }
    }

    private class Decoder : OfflineSpeechRecognizer {
        val mic = Mic()
        var micError: Throwable? = null
        var decodeError: Throwable? = null
        var closed = 0
        var accepted = byteArrayOf()
        override fun microphone(sampleRate: Float): OfflineSpeechMicrophone {
            assertEquals(16_000f, sampleRate)
            micError?.let { throw it }
            return mic
        }
        override fun accept(pcm: ByteArray) {
            accepted = pcm
            decodeError?.let { throw it }
        }
        override fun finalResult() = "{\"text\":\"hello\"}"
        override fun close() { closed++ }
    }

    private class Model(private val events: MutableList<String>, private val id: Int) : OfflineSpeechModel {
        val decoders = mutableListOf<Decoder>()
        var decoderError: Throwable? = null
        var configureDecoder: (Decoder) -> Unit = {}
        var closed = 0
        override fun recognizer(sampleRate: Float): OfflineSpeechRecognizer {
            assertEquals(16_000f, sampleRate)
            decoderError?.let { throw it }
            return Decoder().also { configureDecoder(it); decoders += it }
        }
        override fun close() { closed++; events += "close$id" }
    }

    private class Fixture(dispatcher: CoroutineDispatcher) {
        var path: String? = "model"
        var resolve: suspend (LanguageCode) -> String? = { path }
        var openError: Throwable? = null
        var duringOpen: () -> Unit = {}
        val events = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        val models = mutableListOf<Model>()
        val results = mutableListOf<RecognitionResult>()
        val engine = VoskSpeechRecognizerEngine(
            modelPath = { resolve(it) },
            openModel = {
                openError?.let { throw it }
                Model(events, models.size).also { model ->
                    events += "open${models.size}"
                    models += model
                    duringOpen()
                }
            },
            ioDispatcher = dispatcher,
            report = errors::add,
        )
        suspend fun load(): Model {
            assertTrue(engine.load(LanguageCode.EN_US).isSuccess)
            return models.last()
        }
        suspend fun start() = engine.startListening(LanguageCode.EN_US) { results += it }
    }

    private fun scenario(block: suspend TestScope.(Fixture) -> Unit) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val fixture = Fixture(dispatcher)
        try { block(fixture) } finally {
            fixture.engine.unload()
            Dispatchers.resetMain()
        }
    }

    @Test fun missingModelAndNativeLinkFailureReturnErrorsAndCanRetry() = scenario { f ->
        f.path = null
        assertTrue(f.engine.load(LanguageCode.EN_US).isFailure)
        f.start()
        assertFalse(f.results.single().isSuccess)
        assertTrue(f.models.isEmpty())
        f.path = "model"
        val linkage = UnsatisfiedLinkError("native library unavailable")
        f.openError = linkage
        val reported = f.engine.load(LanguageCode.EN_US).exceptionOrNull()
        // Coroutine stack-trace recovery may copy an exception across dispatchers.
        assertTrue(reported is UnsatisfiedLinkError)
        assertEquals(linkage.message, reported?.message)
        assertFalse(f.engine.recognizeFromAudio(byteArrayOf()).isSuccess)
        f.openError = null
        f.load()
        f.start()
        assertEquals(1, f.models.single().decoders.size)
    }

    @Test fun ordinaryInitializationFailureDoesNotLeaveALoadedLanguage() = scenario { f ->
        f.openError = IllegalStateException("bad model")
        assertTrue(f.engine.load(LanguageCode.EN_US).isFailure)
        f.openError = null
        val model = f.load()
        f.engine.startListening(LanguageCode.ZH_CN) { f.results += it }
        assertFalse(f.results.single().isSuccess)
        assertTrue(model.decoders.isEmpty())
    }

    @Test fun languageSwitchClosesOldModelBeforeOpeningNextAndReusesSameLanguage() = scenario { f ->
        val first = f.load()
        f.load()
        assertEquals(listOf("open0"), f.events)
        f.start()
        assertTrue(f.engine.load(LanguageCode.ZH_CN).isSuccess)
        assertEquals(listOf("open0", "close0", "open1"), f.events)
        assertEquals(1, first.decoders.single().closed)
        first.decoders.single().mic.listener.onFinalResult("{\"text\":\"old\"}")
        assertTrue(f.results.isEmpty())
    }

    @Test fun cancelledLoadClosesModelCreatedBeforeReturningFromIo() = scenario { f ->
        lateinit var loading: Job
        f.duringOpen = { loading.cancel() }
        loading = launch { f.engine.load(LanguageCode.EN_US) }
        advanceUntilIdle()
        assertTrue(loading.isCancelled)
        assertEquals(1, f.models.single().closed)
        f.duringOpen = {}
        f.load()
        assertEquals(listOf("open0", "close0", "open1"), f.events)
    }

    @Test fun concurrentLanguageLoadsAreSerialized() = scenario { f ->
        val release = CompletableDeferred<Unit>()
        val requested = mutableListOf<LanguageCode>()
        f.resolve = { language ->
            requested += language
            if (language == LanguageCode.EN_US) release.await()
            "model"
        }
        val first = async { f.engine.load(LanguageCode.EN_US) }
        runCurrent()
        val second = async { f.engine.load(LanguageCode.ZH_CN) }
        runCurrent()
        assertEquals(listOf(LanguageCode.EN_US), requested)
        release.complete(Unit)
        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        assertEquals(listOf("open0", "close0", "open1"), f.events)
    }

    @Test fun microphoneConstructionFailureClosesTheRecognizerAndAllowsRetry() = scenario { f ->
        val model = f.load()
        model.configureDecoder = { it.micError = java.io.IOException("microphone busy") }
        f.start()
        assertFalse(f.results.single().isSuccess)
        assertEquals(1, model.decoders.single().closed)
        assertEquals(0, model.closed)
        model.configureDecoder = {}
        f.start()
        model.decoders.last().mic.listener.onFinalResult("{\"text\":\"hello\"}")
        assertEquals("hello", f.results.last().text)
    }

    @Test fun failedStartAndRecognizerLinkErrorsDeliverOnlyOneError() = scenario { f ->
        val model = f.load()
        model.decoderError = NoClassDefFoundError("Vosk")
        f.start()
        assertEquals(1, f.results.size)
        assertFalse(f.results.last().isSuccess)
        model.decoderError = null
        model.configureDecoder = { it.mic.started = false }
        f.start()
        assertEquals(2, f.results.size)
        assertFalse(f.results.last().isSuccess)
        assertEquals(1, model.decoders.single().closed)
        assertEquals(1, model.decoders.single().mic.shutdowns)
    }

    @Test fun throwingStartReleasesMicrophoneAndIgnoresItsLateCallbacks() = scenario { f ->
        val model = f.load()
        model.configureDecoder = { it.mic.startError = SecurityException("permission revoked") }
        f.start()
        val decoder = model.decoders.single()
        assertFalse(f.results.single().isSuccess)
        assertEquals(1, decoder.closed)
        assertEquals(1, decoder.mic.shutdowns)
        decoder.mic.listener.onError(IllegalStateException("late error"))
        assertEquals(1, f.results.size)
    }

    @Test fun stopFailureAndShutdownFailureStillEndSessionAndCloseRecognizer() = scenario { f ->
        val model = f.load()
        f.start()
        val decoder = model.decoders.single()
        decoder.mic.stopError = IllegalStateException("stop failed")
        decoder.mic.shutdownError = IllegalStateException("release failed")
        f.engine.stopListening()
        assertFalse(f.results.single().isSuccess)
        assertEquals(1, decoder.closed)
        assertEquals(1, decoder.mic.cancelled)
        assertEquals(1, decoder.mic.shutdowns)
        f.engine.cancelListening()
        assertEquals(1, decoder.closed)
    }

    @Test fun failedCancelRetriesStopButNeverClosesANativeWorkerStillInUse() = scenario { f ->
        val model = f.load()
        f.start()
        val decoder = model.decoders.single()
        decoder.mic.cancelError = IllegalStateException("cancel failed")
        decoder.mic.stopError = IllegalStateException("join failed")
        f.engine.cancelListening()
        assertEquals(0, decoder.closed)
        assertTrue(f.engine.load(LanguageCode.ZH_CN).isFailure)
        assertEquals(0, model.closed)
        assertEquals(1, f.models.size)
        decoder.mic.stopError = null
        assertTrue(f.engine.load(LanguageCode.ZH_CN).isSuccess)
        assertEquals(1, decoder.closed)
        assertEquals(1, model.closed)
        assertTrue(f.results.isEmpty())
    }

    @Test fun successIsDeliveredOnceAndOldListenersCannotCompleteANewRecording() = scenario { f ->
        val model = f.load()
        f.start()
        val old = model.decoders.single().mic.listener
        old.onPartialResult("{\"partial\":\"hello\"}")
        old.onResult("{\"text\":\"\"}")
        assertTrue(f.results.isEmpty())
        old.onResult("{\"text\":\" hello \"}")
        old.onFinalResult("{\"text\":\"duplicate\"}")
        assertEquals(listOf("hello"), f.results.map { it.text })
        f.start()
        old.onError(IllegalStateException("late"))
        assertEquals(1, f.results.size)
        f.engine.cancelListening()
        model.decoders.last().mic.listener.onFinalResult("{\"text\":\"late\"}")
        assertEquals(1, f.results.size)
    }

    @Test fun synchronousFinalOnStopIsNotLostOrRepeatedByCleanup() = scenario { f ->
        val model = f.load()
        f.start()
        model.decoders.single().mic.stopFinal = true
        f.engine.stopListening()
        assertEquals("hello", f.results.single().text)
        assertEquals(1, model.decoders.single().closed)
    }

    @Test fun cancellingStartPropagatesCancellationAfterResourceCleanup() = scenario { f ->
        val model = f.load()
        model.configureDecoder = { it.mic.startError = CancellationException("cancelled") }
        try { f.start(); fail("Cancellation must propagate") } catch (_: CancellationException) { }
        assertTrue(f.results.isEmpty())
        assertEquals(1, model.decoders.single().closed)
        assertEquals(1, model.decoders.single().mic.shutdowns)
    }

    @Test fun pcmDecodeClosesTemporaryRecognizerOnSuccessAndNativeFailure() = scenario { f ->
        val model = f.load()
        val bytes = byteArrayOf(1, 2)
        assertEquals("hello", f.engine.recognizeFromAudio(bytes).text)
        assertArrayEquals(bytes, model.decoders.single().accepted)
        assertEquals(1, model.decoders.single().closed)
        model.configureDecoder = { it.decodeError = UnsatisfiedLinkError("decode unavailable") }
        assertFalse(f.engine.recognizeFromAudio(bytes).isSuccess)
        assertEquals(1, model.decoders.last().closed)
    }

    @Test fun timeoutAndMalformedFinalKeepExistingEmptyResultSemantics() = scenario { f ->
        val model = f.load()
        f.start()
        model.decoders.last().mic.listener.onTimeout()
        assertTrue(f.results.single().isSuccess)
        assertEquals("Timeout", f.results.single().error)
        assertEquals("", f.results.single().text)
        f.start()
        model.decoders.last().mic.listener.onFinalResult("not JSON")
        assertEquals("", f.results.last().text)
        assertTrue(f.results.last().isSuccess)
    }
}
