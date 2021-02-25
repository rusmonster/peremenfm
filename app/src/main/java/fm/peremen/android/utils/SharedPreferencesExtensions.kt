package fm.peremen.android.utils

import android.content.SharedPreferences

private val KEY_IS_AUDIO_CACHED = "KEY_IS_AUDIO_CACHED"
private val KEY_IS_PLAYBACK_STARTED = "KEY_IS_PLAYBACK_STARTED"

var SharedPreferences.isAudioCached: Boolean
    get() = getBoolean(KEY_IS_AUDIO_CACHED, false)
    set(value) { edit().putBoolean(KEY_IS_AUDIO_CACHED, value).apply() }

var SharedPreferences.isPlaybackStarted: Boolean
    get() = getBoolean(KEY_IS_PLAYBACK_STARTED, false)
    set(value) { edit().putBoolean(KEY_IS_PLAYBACK_STARTED, value).apply() }
