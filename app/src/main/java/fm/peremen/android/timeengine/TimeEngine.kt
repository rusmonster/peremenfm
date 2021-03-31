package fm.peremen.android.timeengine

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.math.abs

private const val kNtpOffsetCorrectionMills: Long = -30
private const val kGpsOffsetCorrectionMills: Long = 20

class TimeEngine(private val context: Context, val onTimeChanged: (Long) -> Unit) {

    private val timeOffsetStorage = TimeOffsetStorage(context)

    private val firstServerOffset = CompletableDeferred<Long>()

    private var isStarted = false

    suspend fun ensureServerOffset() {
        firstServerOffset.await()
    }

    suspend fun start() {
        if (isStarted) return

        isStarted = true
        Timber.d("TimeEngine started")

        createFlow().collect {
            onTimeChanged(it)
            firstServerOffset.complete(it)
        }

        isStarted = false
        Timber.d("TimeEngine finished")
    }

    private fun createFlow(): Flow<Long> {
        val gpsSource = GpsTimeSource(0, context).timeDataFlow()
            .filterSourceAccuracy { it > 0 && it < 6 }
            .distinctUntilSourceAccuracyChanged()
            .map { it.addCorrectionMills(kGpsOffsetCorrectionMills) }

        val ntpSource = NtpTimeSource(1, startDelay = 1000).timeDataFlow()
            .map { it.addCorrectionMills(kNtpOffsetCorrectionMills) }

        val peremenSource = PeremenTimeSource(2, startDelay = 0).timeDataFlow()
        val savedOffsetSource = flowOf(timeOffsetStorage.getSavedTimeData(sourceId = 3))

        // Necessary to keep untilMillsAfterConditionMatch() working when all others flows cannot produce new value
        val tickerFlow = ticker(1000).consumeAsFlow()
            .map { TimeData.Empty }

        val combinedFlow = combine(tickerFlow, savedOffsetSource, gpsSource, ntpSource, peremenSource) { dataItems ->
            Timber.d("savedOffset:   ${dataItems[1].accuracyLevel}; ${dataItems[1]}")
            Timber.d("gpsSource:     ${dataItems[2].accuracyLevel}; ${dataItems[2]}")
            Timber.d("ntpSource:     ${dataItems[3].accuracyLevel}; ${dataItems[3]}")
            Timber.d("peremenSource: ${dataItems[4].accuracyLevel}; ${dataItems[4]}")

            dataItems.maxByOrNull { it.priority }!!
        }

        return combinedFlow
            .conflate()
            .onEach { delay(1000) }
            .untilMillsAfterConditionMatch(120_000) { it.accuracyLevel == AccuracyLevel.PERFECT }
            .filter { it.accuracyLevel >= AccuracyLevel.GOOD }
            .onEach { Timber.d("combinedFlow: ${it.accuracyLevel}; $it") }
            .mapNotNull { it as? TimeData.Data }
            .saveIfPerfect(timeOffsetStorage)
            .map { it.timeOffset }
    }
}

private enum class AccuracyLevel(val value: Int) {
    BAD(0),
    GOOD(1),
    PERFECT(2),
}

private val TimeData.accuracyLevel get() = when(this) {
    TimeData.Empty -> AccuracyLevel.BAD

    is TimeData.Fetched -> when {
        probeCount >= 5 && abs(offsetAccuracy) < 10 -> AccuracyLevel.PERFECT
        probeCount >= 3 && abs(offsetAccuracy) < 50 -> AccuracyLevel.GOOD
        else -> AccuracyLevel.BAD
    }

    is TimeData.Saved -> AccuracyLevel.GOOD

    is TimeData.Data -> TODO("Never happens; Remove after switching to sealed interface in kotlin 1.5")
}

private val TimeData.priority get() = when(this) {
    TimeData.Empty -> 0
    is TimeData.Data  -> accuracyLevel.value * 100 + sourceId
}

private fun TimeData.addCorrectionMills(correctionMills: Long) = when(this) {
    is TimeData.Fetched  -> copy(timeOffset = timeOffset + correctionMills)
    else -> this
}

private fun Flow<TimeData>.filterSourceAccuracy(predicate: (Double) -> Boolean) = filter { timeData ->
    when(timeData) {
        is TimeData.Fetched -> predicate(timeData.sourceAccuracy)
        else -> true
    }
}

private fun Flow<TimeData>.distinctUntilSourceAccuracyChanged() = distinctUntilChanged { old, new ->
    when {
        old is TimeData.Fetched && new is TimeData.Fetched -> abs(old.sourceAccuracy - new.sourceAccuracy) < 0.01
        else -> false
    }
}

private fun Flow<TimeData>.untilMillsAfterConditionMatch(mills: Long, condition: (TimeData) -> Boolean): Flow<TimeData> {
    val firstConditionMatch = object { var timestamp: Long? = null }

    return takeWhile { timeData ->
        firstConditionMatch.timestamp
            ?.takeIf { SystemClock.elapsedRealtime() - it > mills }
            ?.also { Timber.d("untilMillsAfterConditionMatch: stop flow") }
            ?.run { return@takeWhile false }

        if (!condition(timeData)) {
            return@takeWhile true
        }

        if (firstConditionMatch.timestamp == null) {
            firstConditionMatch.timestamp = SystemClock.elapsedRealtime()
        }

        return@takeWhile true
    }
}

private fun Flow<TimeData.Data>.saveIfPerfect(timeOffsetStorage: TimeOffsetStorage) = onEach { timeData ->
    if (timeData.accuracyLevel == AccuracyLevel.PERFECT) {
        timeOffsetStorage.saveTimeOffset(timeData.timeOffset)
    }
}
