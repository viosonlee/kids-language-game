package com.kidslanguage.companion.core.audio

import kotlinx.coroutines.CompletableDeferred

/** Serialized on the main thread by the Android player. */
internal class PlaybackGate {
    class Ticket(val id: String, val completion: CompletableDeferred<Unit> = CompletableDeferred())
    private var sequence = 0L
    private var active: Ticket? = null

    fun begin(): Ticket {
        cancel()
        return Ticket("prompt_${++sequence}").also { active = it }
    }

    fun complete(id: String?) {
        active?.takeIf { it.id == id }?.completion?.complete(Unit)
    }

    fun isActive(ticket: Ticket): Boolean = active === ticket && !ticket.completion.isCompleted

    fun finish(ticket: Ticket): Boolean {
        if (active !== ticket) return false
        active = null
        return true
    }

    fun cancel() {
        val previous = active
        active = null
        previous?.completion?.cancel()
    }
}
