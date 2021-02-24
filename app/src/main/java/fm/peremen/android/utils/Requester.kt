package fm.peremen.android.utils

import android.os.SystemClock
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.URL

suspend fun performServerTimeOffsetRequest(url: String): Long = coroutineScope {

    data class Sample(val requestTimestamp: Long, val responseTimestamp: Long, val serverTimestamp: Long)

    val requests = List(10) {
        async {
            runCatching {
                val requestTimestamp = SystemClock.elapsedRealtime()
                val serverTimestamp = withCancellableContext(Dispatchers.IO) { URL(url).readText().toLong() }
                val responseTimestamp = SystemClock.elapsedRealtime()

                Timber.d("serverTimestamp: $serverTimestamp; resp-req: ${responseTimestamp - requestTimestamp}")
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

    with(bestResult) {
        val networkLatency = (responseTimestamp - requestTimestamp) / 2
        Timber.d("calculated networkLatency: $networkLatency")

        return@coroutineScope serverTimestamp + networkLatency - responseTimestamp // return server time offset relative local elapsedRealtime
    }
}
