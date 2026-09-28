package com.kidslanguage.companion

import com.kidslanguage.companion.core.audio.PlaybackGate
import org.junit.Assert.*
import org.junit.Test

class PlaybackGateTest {
    @Test fun replacingPlaybackCancelsOldWaiterAndRejectsItsLateCallback() {
        val gate = PlaybackGate()
        val old = gate.begin()
        val current = gate.begin()
        assertTrue(old.completion.isCancelled)
        gate.complete(old.id)
        assertFalse(current.completion.isCompleted)
        assertFalse(gate.finish(old))
        gate.complete(current.id)
        assertTrue(current.completion.isCompleted)
        assertTrue(gate.finish(current))
    }

    @Test fun stoppedPlaybackCannotCompleteOrReleaseReplacement() {
        val gate = PlaybackGate()
        val stopped = gate.begin()
        gate.cancel()
        assertTrue(stopped.completion.isCancelled)
        val replacement = gate.begin()
        gate.complete(stopped.id)
        assertFalse(gate.finish(stopped))
        assertFalse(replacement.completion.isCompleted)
        assertTrue(gate.finish(replacement))
    }

    @Test fun duplicateCompletionAndNullIdsAreHarmless() {
        val gate = PlaybackGate()
        val ticket = gate.begin()
        gate.complete(null)
        assertFalse(ticket.completion.isCompleted)
        gate.complete(ticket.id)
        gate.complete(ticket.id)
        assertTrue(ticket.completion.isCompleted)
        assertTrue(gate.finish(ticket))
        assertFalse(gate.finish(ticket))
    }
}
