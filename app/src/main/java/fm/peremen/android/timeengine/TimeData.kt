package fm.peremen.android.timeengine

sealed class TimeData {

    object Empty : TimeData()

    abstract class Data : TimeData() {
        abstract val sourceId: Int
        abstract val timeOffset: Long
    }

    data class Fetched(
        override val sourceId: Int,
        override val timeOffset: Long,
        val offsetAccuracy: Long,
        val probeCount: Int,
        val sourceAccuracy: Double,
    ) : Data()

    data class Saved(
        override val sourceId: Int,
        override val timeOffset: Long,
    ) : Data()
}
