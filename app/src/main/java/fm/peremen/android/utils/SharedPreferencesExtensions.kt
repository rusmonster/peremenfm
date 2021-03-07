package fm.peremen.android.utils

import android.content.SharedPreferences

private const val KEY_IS_AUDIO_CACHED = "KEY_IS_AUDIO_CACHED"
private const val KEY_IS_AUDIO_DECODED = "KEY_IS_AUDIO_DECODED_1"
private const val KEY_CHANNEL_COUNT = "KEY_CHANNEL_COUNT"
private const val KEY_SAMPLE_RATE = "KEY_SAMPLE_RATE"
private const val KEY_IS_PLAYBACK_STARTED = "KEY_IS_PLAYBACK_STARTED"

var SharedPreferences.isAudioCached: Boolean
    get() = getBoolean(KEY_IS_AUDIO_CACHED, false)
    set(value) { edit().putBoolean(KEY_IS_AUDIO_CACHED, value).apply() }

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
