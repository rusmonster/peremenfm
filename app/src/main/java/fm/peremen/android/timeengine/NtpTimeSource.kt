package fm.peremen.android.timeengine

import fm.peremen.android.utils.withCancellableContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import timber.log.Timber

private const val timeoutMs = 10_000

class NtpTimeSource(id: Int, private val startDelay: Long) : AccuracyTimeSource(id) {

    override suspend fun timeRequestFlow(): Flow<TimeRequestResult> = flow {
        delay(startDelay)

        while (true) {
            delay(1000 + (1000 * Math.random().toLong()))

            val result = runCatching { performServerTimeRequest() }.getOrNull() ?: continue
            emit(result)
        }
    }

    private suspend fun performServerTimeRequest(attemptCount: Int = 3) = supervisorScope {
        val hostAddress = withTimeout(timeoutMs.toLong()) {
            withCancellableContext(Dispatchers.IO) { AndroidSntpClient.queryHostAddress("time.google.com") }
        }

        val requests = List(attemptCount) {
            async {
                runCatching {
                    withCancellableContext(Dispatchers.IO) {
                        AndroidSntpClient.requestTime(hostAddress, AndroidSntpClient.NTP_PORT, timeoutMs) as? SntpClient.Result.Success
                    }
                }
            }
        }

        val bestResult = requests
            .awaitAll()
            .mapNotNull { it.getOrNull() }
            .minByOrNull { it.roundTripTimeMs }
            .also { Timber.d("Ntp bestResult: $it") }
            ?: throw RuntimeException("All serverTimestamp requests failed")

        return@supervisorScope TimeRequestResult(
            bestResult.ntpTimeMs - bestResult.uptimeReferenceMs,
            bestResult.roundTripTimeMs.toDouble()
        )
    }
}
