package fm.peremen.android.utils

import android.os.SystemClock
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.URL

private data class Sample(val requestTimestamp: Long, val responseTimestamp: Long, val serverTimestamp: Long)

suspend fun performServerTimeOffsetRequest(url: String): Long = coroutineScope {
    val requests = List(10) {
        async(Dispatchers.IO) {
            runCatching {
                val requestTimestamp = SystemClock.elapsedRealtime()
                val serverTimestamp = URL(url).readText().toLong()
                val responseTimestamp = SystemClock.elapsedRealtime()

                Timber.d("serverTimestamp: $serverTimestamp")
                return@runCatching Sample(requestTimestamp, responseTimestamp, serverTimestamp)
            }
        }
    }

    val bestResult = requests
        .awaitAll()
        .map { it.getOrNull() }
        .filterNotNull()
        .minByOrNull { it.responseTimestamp - it.requestTimestamp }
        ?: throw RuntimeException("All serverTimestamp requests failed")

    // return server time offset relative local elapsedRealtime
    return@coroutineScope with(bestResult) { serverTimestamp - responseTimestamp }
}
