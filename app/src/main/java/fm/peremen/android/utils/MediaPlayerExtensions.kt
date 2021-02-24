package fm.peremen.android.utils

import android.media.MediaPlayer
import android.media.MediaPlayer.SEEK_CLOSEST
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun MediaPlayer.prepareAndWait() = suspendCancellableCoroutine<Unit> { continuation ->
    setOnPreparedListener { continuation.resume(Unit) }
    prepareAsync()
}

suspend fun MediaPlayer.seekAndWait(offset: Long) = suspendCancellableCoroutine<Unit> { continuation ->
    setOnSeekCompleteListener { continuation.resume(Unit) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        seekTo(offset, SEEK_CLOSEST)
    } else {
        seekTo(offset.toInt())
    }
}
