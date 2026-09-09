package com.kyhsgeekcode.minecraftenv

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TickSynchronizer {
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private var requested = 0L
    private var started = 0L
    private var completed = 0L
    private var terminating = false

    fun notifyServerTickStart() = lock.withLock {
        requested++
        changed.signalAll()
    }

    fun waitForClientAction() = lock.withLock {
        while (!terminating && started == requested) changed.await()
        if (!terminating) started++
    }

    fun notifyClientSendObservation() = lock.withLock {
        // Unsynchronized initialization ticks have no request to complete.
        completed = started
        changed.signalAll()
    }

    fun waitForServerTickCompletion() = lock.withLock {
        while (!terminating && completed < requested) changed.await()
    }

    fun terminate() = lock.withLock {
        terminating = true
        changed.signalAll()
    }
}
