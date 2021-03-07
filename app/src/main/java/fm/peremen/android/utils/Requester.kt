package fm.peremen.android.utils

import android.os.SystemClock
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.URL

class TimeRequestResult(requestTimestamp: Long, responseTimestamp: Long, serverTimestamp: Long) {
    val networkLatency = responseTimestamp - requestTimestamp
    val serverOffset = serverTimestamp + networkLatency / 2 - responseTimestamp

    override fun toString() = "{ networkLatency: $networkLatency; serverOffset: $serverOffset }"
}

suspend fun performServerTimeRequest(url: String, attemptCount: Int = 10): TimeRequestResult = coroutineScope {

    val requests = List(attemptCount) {
        async {
            runCatching {
                val requestTimestamp = SystemClock.elapsedRealtime()
                val serverTimestamp = withCancellableContext(Dispatchers.IO) { URL(url).readText().toLong() }
                val responseTimestamp = SystemClock.elapsedRealtime()

                Timber.d("serverTimestamp: $serverTimestamp; resp-req: ${responseTimestamp - requestTimestamp}")
                return@runCatching TimeRequestResult(requestTimestamp, responseTimestamp, serverTimestamp)
            }
        }
    }

    val bestResult = requests
        .awaitAll()
        .map { it.getOrNull() }
        .filterNotNull()
        .minByOrNull { it.networkLatency }
        ?: throw RuntimeException("All serverTimestamp requests failed")

    return@coroutineScope bestResult
}
