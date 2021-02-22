package fm.peremen.android.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.abs

private const val MAX_ALLOWED_OFFSET = 30 // ms

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun playFileFromCache(context: Context, fileName: String, audioDuration: Long, serverOffset: Long, globalStartTimestamp: Long, onProgress: (Boolean, Int, Long) -> Unit) {
    onProgress(true, 0, 0)
    val player = MediaPlayer()

    try {
        val file = context.getFileStreamPath(fileName)
        player.setDataSource(file.absolutePath)
        player.isLooping = true
        player.prepareAndWait()

        Timber.d("player.duration: ${player.duration}")
        Timber.d("audioDuration: $audioDuration")

        fun playbackOffset(): Long {
            val currentServerTime = SystemClock.elapsedRealtime() + serverOffset
            val globalDuration = currentServerTime - globalStartTimestamp
            return globalDuration % audioDuration
        }

        val offset = playbackOffset()
        Timber.d("offset: $offset")

        player.setVolume(0f, 0f)
        player.seekAndWait(offset)
        player.start()

        delay(1000)

        var magicOffset = playbackOffset() - player.currentPosition
        Timber.d("magicOffset: $magicOffset")

        while (true) { // play until job is cancelled
            val currentOffset = playbackOffset() - player.currentPosition
            Timber.d("currentOffset: $currentOffset")

            if (abs(currentOffset) > MAX_ALLOWED_OFFSET) {
                Timber.d("Correcting: $currentOffset")
                onProgress(true, player.currentPosition, currentOffset)
                player.setVolume(0f, 0f)

                val correctedOffset = maxOf(playbackOffset() + magicOffset, 0)
                Timber.d("currentPosition: ${player.currentPosition}")
                Timber.d("correctedOffset: $correctedOffset")

                player.seekAndWait(correctedOffset)

                delay(1000)

                magicOffset += playbackOffset() - player.currentPosition
                if (abs(magicOffset) > 1000) magicOffset = 0 // here apply a bit of magic to the magicOffset :)

                Timber.d("magicOffset: $magicOffset")

            } else {
                onProgress(false, player.currentPosition, currentOffset)
                player.setVolume(1f, 1f)
                delay(1000)
            }
        }
    } finally {
        player.stop()
        player.release()
    }
}
