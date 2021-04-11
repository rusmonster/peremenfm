package fm.peremen.android.utils

import android.content.SharedPreferences

private const val KEY_IS_AUDIO_DECODED = "KEY_IS_AUDIO_DECODED"
private const val KEY_CHANNEL_COUNT = "KEY_CHANNEL_COUNT"
private const val KEY_SAMPLE_RATE = "KEY_SAMPLE_RATE"
private const val KEY_IS_PLAYBACK_STARTED = "KEY_IS_PLAYBACK_STARTED"
private const val KEY_IS_GPS_EXPLANATION_SHOWN = "KEY_IS_GPS_EXPLANATION_SHOWN"

var SharedPreferences.isAudioDecoded: Boolean
    get() = getBoolean(KEY_IS_AUDIO_DECODED, false)
    set(value) { edit().putBoolean(KEY_IS_AUDIO_DECODED, value).apply() }

var SharedPreferences.channelCount: Int
    get() = getInt(KEY_CHANNEL_COUNT, 0)
    set(value) { edit().putInt(KEY_CHANNEL_COUNT, value).apply() }

var SharedPreferences.sampleRate: Int
    get() = getInt(KEY_SAMPLE_RATE, 0)
    set(value) { edit().putInt(KEY_SAMPLE_RATE, value).apply() }

var SharedPreferences.isPlaybackStarted: Boolean
    get() = getBoolean(KEY_IS_PLAYBACK_STARTED, false)
    set(value) { edit().putBoolean(KEY_IS_PLAYBACK_STARTED, value).apply() }

var SharedPreferences.isGpsExplanationShown: Boolean
    get() = getBoolean(KEY_IS_GPS_EXPLANATION_SHOWN, false)
    set(value) { edit().putBoolean(KEY_IS_GPS_EXPLANATION_SHOWN, value).apply() }
