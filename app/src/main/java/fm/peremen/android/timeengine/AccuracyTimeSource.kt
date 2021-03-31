package fm.peremen.android.timeengine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import kotlin.math.abs

abstract class AccuracyTimeSource(private val id: Int) {

    protected data class TimeRequestResult(val serverOffset: Long, val sourceAccuracy: Double)

    private val serverTimeResults = sortedSetOf<TimeRequestResult>({ o1, o2 -> o1.sourceAccuracy.compareTo(o2.sourceAccuracy) })

    fun timeDataFlow() = flow {
        emit(TimeData.Empty)

        timeRequestFlow().collect { result ->
            val timeData = calcTimeData(result)
            emit(timeData)
        }
    }

    protected abstract suspend fun timeRequestFlow(): Flow<TimeRequestResult>

    private fun calcTimeData(result: TimeRequestResult): TimeData {
        serverTimeResults += result
        while (serverTimeResults.size > 10) serverTimeResults.remove(serverTimeResults.last())

        val avgOffset = serverTimeResults.sumOf { it.serverOffset } / serverTimeResults.size
        val avgDiff = serverTimeResults.sumOf { abs(it.serverOffset - avgOffset) } / serverTimeResults.size

        val preciseResults = serverTimeResults.filter { abs(it.serverOffset - avgOffset) <= avgDiff }
        val preciseAvgOffset = preciseResults.sumOf { it.serverOffset } / preciseResults.size
        val severOffsetAccuracy = preciseResults.sumOf { abs(it.serverOffset - preciseAvgOffset) } / preciseResults.size
        val preciseAvgSourceAccuracy = preciseResults.sumOf { it.sourceAccuracy } / preciseResults.size

        return TimeData.Fetched(
            id,
            preciseAvgOffset,
            severOffsetAccuracy,
            preciseResults.size,
            preciseAvgSourceAccuracy
        )
    }
}
