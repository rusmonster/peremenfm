package fm.peremen.android.timeengine

import android.os.SystemClock
import fm.peremen.android.utils.withCancellableContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.net.URL

class PeremenTimeSource(id: Int, private val startDelay: Long) : AccuracyTimeSource(id) {

    override suspend fun timeRequestFlow(): Flow<TimeRequestResult> = flow {
        delay(startDelay)

        while (true) {
            delay(1000 + (1000 * Math.random().toLong()))

            val result = runCatching { performServerTimeRequest() }.getOrNull() ?: continue
            emit(result)
        }
    }

    private suspend fun performServerTimeRequest(attemptCount: Int = 3): TimeRequestResult = supervisorScope {
        val requests = List(attemptCount) {
            async {
                runCatching {
                    val requestTimestamp = SystemClock.elapsedRealtime()
                    val serverTimestamp = withTimeout(10_000) {
                        withCancellableContext(Dispatchers.IO) { URL("https://prmn.rogozhin.pro/time2").readText().toLong() }
                    }
                    val responseTimestamp = SystemClock.elapsedRealtime()

                    return@runCatching TimeRequestResult(
                        serverTimestamp - responseTimestamp,
                        (responseTimestamp - requestTimestamp).toDouble()
                    )
                }
            }
        }

        val bestResult = requests
            .awaitAll()
            .mapNotNull { it.getOrNull() }
            .minByOrNull { it.sourceAccuracy }
            .also { Timber.d("Peremen bestResult: $it") }
            ?: throw RuntimeException("All serverTimestamp requests failed")

        return@supervisorScope bestResult
    }
}
