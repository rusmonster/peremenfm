package fm.peremen.android.timeengine

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class TimeOffsetStorage(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("StoredTimeSourceData", Context.MODE_PRIVATE)

    fun saveTimeOffset(timeOffset: Long) {
        sharedPreferences.timeOffset = timeOffset
    }

    fun getSavedTimeData(sourceId: Int) = sharedPreferences.getSavedTimeData(sourceId)
}

private const val KEY_TIMESTAMP_VALUE = "KEY_TIMESTAMP_VALUE"
private const val KEY_TIMESTAMP_UPDATED = "KEY_TIMESTAMP_UPDATED"
private const val KEY_TIMESTAMP_DELTA = "KEY_TIMESTAMP_DELTA"
private const val MILLS_PER_DAY = 24 * 60 * 60 * 1000

private var SharedPreferences.timeOffset
    get() = getLong(KEY_TIMESTAMP_VALUE, 0)
    set(value) {
        edit()
            .putLong(KEY_TIMESTAMP_VALUE, value)
            .putLong(KEY_TIMESTAMP_UPDATED, SystemClock.elapsedRealtime())
            .putLong(KEY_TIMESTAMP_DELTA, timeDelta())
            .apply()
    }

private val SharedPreferences.hasValidTimeOffset
    get() = isUpdatedLastTwoDays && !isDeviceHasBeenRebooted

private val SharedPreferences.isUpdatedLastTwoDays
    get() = SystemClock.elapsedRealtime() - getLong(KEY_TIMESTAMP_UPDATED, 0) < 2 * MILLS_PER_DAY

private val SharedPreferences.isDeviceHasBeenRebooted
    get() = timeDelta() - getLong(KEY_TIMESTAMP_DELTA, 0) > 100

private fun timeDelta() = System.currentTimeMillis() - SystemClock.elapsedRealtime()

private fun SharedPreferences.getSavedTimeData(sourceId: Int)
        = if (hasValidTimeOffset) TimeData.Saved(sourceId, timeOffset) else TimeData.Empty
