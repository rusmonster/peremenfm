package fm.peremen.android.utils

import android.content.Context
import fm.peremen.android.PeremenManager
import fm.peremen.android.PeremenManager.Status
import fm.peremen.android.R

fun PeremenManager.getActivityReadableStatus(context: Context): String = when(status) {

    Status.IDLE -> if (isError) context.getString(R.string.error_occured) else ""

    Status.DECODING -> context.getString(R.string.decoding)

    Status.POSITIONING -> context.getString(R.string.positioning)

    Status.PLAYING -> context.getString(R.string.playing, readablePlaybackPosition)
}

fun PeremenManager.getNotificationReadableStatus(context: Context): String = when(status) {

    Status.IDLE -> if (isError) context.getString(R.string.error_occured) else context.getString(R.string.paused)

    Status.DECODING -> context.getString(R.string.decoding)

    Status.POSITIONING -> context.getString(R.string.positioning)

    Status.PLAYING -> context.getString(R.string.playing, readablePlaybackPosition)
}

private val PeremenManager.readablePlaybackPosition: String
    get() {
        val positionSeconds = playbackPosition / 1000
        val minutes = positionSeconds / 60
        val seconds = positionSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
