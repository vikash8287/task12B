package com.chamberly.chamberly.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TaskScheduler {
    private val handler = Handler(Looper.getMainLooper())
    private val timers: MutableMap<String, TimerModel> = mutableMapOf()

    companion object {
        val instance: TaskScheduler by lazy { TaskScheduler() }
    }

    suspend fun scheduleTask(
        topicID: String,
        forUID: String,
        timeInterval: Long,
        repeats: Boolean,
        task: () -> Unit = {}
    ) {
        return suspendCancellableCoroutine { continuation ->
            val runnable = object : Runnable {
                override fun run() {
                    try {
                        task()
                        if (repeats) {
                            handler.postDelayed(this, timeInterval)
                        } else {
                            continuation.resume(Unit)
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }

            handler.postDelayed(runnable, timeInterval)
            timers[topicID] = TimerModel(runnable, forUID)

            // To handle coroutine cancellation
            continuation.invokeOnCancellation {
                handler.removeCallbacks(runnable)
            }
        }
    }

    fun completeCurrentTask(topicID: String) {
        // TODO: When this function is called, end the current timer such that the task callback is not run and the TaskScheduler can be called again for the same topicID but a different forUID
    }

    fun invalidateTimer(topicID: String) {
        timers[topicID]?.let {
            handler.removeCallbacks(it.runnable)
            timers.remove(topicID)
        }
    }

    fun invalidateAllTimers() {
        timers.values.forEach {
            handler.removeCallbacks(it.runnable)
        }
        timers.clear()
    }

    fun info(topicID: String): TimerModel? {
        return timers[topicID]
    }

}

data class TimerModel(
    val runnable: Runnable,
    val forUID: String
)