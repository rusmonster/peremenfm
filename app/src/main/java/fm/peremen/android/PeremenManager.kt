package fm.peremen.android

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaFormat
import android.os.SystemClock
import androidx.preference.PreferenceManager
import fm.peremen.android.timeengine.TimeEngine
import fm.peremen.android.utils.*
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.properties.Delegates

private const val AUDIO_FILE_NAME = "peremen2.mp3"
private const val AUDIO_FILE_NAME_PCM = "peremen2.raw"
private const val AUDIO_FILE_LENGTH = 296250L
private const val RADIO_START_TIMESTAMP = 1612384206000L

@SuppressLint("StaticFieldLeak")
object PeremenManager {
    enum class Status { IDLE, DECODING, POSITIONING, PLAYING }

    var status by Delegates.observable(Status.IDLE) { _, _, _ -> notifyChanged() }
        private set

    val isStarted get() = status != Status.IDLE

    var playbackPosition: Long = 0
        private set

    var totalPatchMills: Long = 0
        private set

    var playbackShift: Long = 0
        private set

    var synchronizationOffset: Long = 0
        private set

    var latency: Double = 0.0
        private set

    var isError = false
        private set

    val onChanged = mutableListOf<() -> Unit>()

    private fun notifyChanged() = onChanged.forEach { it() }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private enum class State { IDLE, STARTED, STOPPING }

    private var state = State.IDLE

    private lateinit var context: Context

    private lateinit var currentJob: Job

    private var serverOffsetOnStartPlaying: Long = 0

    private var serverOffset: Long = 0

    private lateinit var timeEngine: TimeEngine

    fun setup(context: Context) {
        this.context = context
        timeEngine = TimeEngine(context, this::updateServerOffset)
        managerScope.launch { timeEngine.start() }
    }

    fun start() {
        if (state != State.IDLE) return
        state = State.STARTED
        isError = false

        managerScope.launch { timeEngine.start() } // update timestamp even if we already have one

        currentJob = managerScope.launch {
            try {
                ensureCache()

                status = Status.POSITIONING
                ensureServerOffset()

                play()
            } catch (e: CancellationException) {
                Timber.d("Job cancelled")
            } catch (e: Exception) {
                Timber.e(e)
                isError = true
            } finally {
                state = State.IDLE
                status = Status.IDLE
            }
        }
    }

    fun stop() {
        if (state != State.STARTED) return
        state = State.STOPPING
        Timber.d("Cancelling job...")
        currentJob.cancel()
    }

    private suspend fun ensureCache() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (!sharedPreferences.isAudioDecoded) {
            status = Status.DECODING
            Timber.d("Decodinig begin")
            val mediaFormat = withContext(Dispatchers.IO) { convertMp3ToPcm(context, AUDIO_FILE_NAME, AUDIO_FILE_NAME_PCM) }
            sharedPreferences.channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            sharedPreferences.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            sharedPreferences.isAudioDecoded = true
            Timber.d("Decodinig success")
        }
    }

    suspend fun ensureServerOffset() = timeEngine.ensureServerOffset()

    private suspend fun play() {
        status = Status.PLAYING

        try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            PlaybackEngine.setDefaultStreamValues(context, sharedPreferences.sampleRate, sharedPreferences.channelCount)
            PlaybackEngine.create()

            val file = context.getFileStreamPath(AUDIO_FILE_NAME_PCM)
            PlaybackEngine.prepare(file.absolutePath)

            val offset = playbackOffset()
            Timber.d("Playback begin")
            PlaybackEngine.play(offset, AUDIO_FILE_LENGTH)

            serverOffsetOnStartPlaying = serverOffset

            while (true) {
                playbackPosition = PlaybackEngine.getCurrentPositionMillis()
                synchronizationOffset = playbackOffset() - playbackPosition
                latency = PlaybackEngine.getCurrentOutputLatencyMillis()
                totalPatchMills = PlaybackEngine.getTotalPathMills()

                notifyChanged()
                delay(1000)
            }
        } finally {
            PlaybackEngine.delete()
        }
    }

    private fun playbackOffset(): Long {
        val currentServerTime = SystemClock.elapsedRealtime() + serverOffset
        val globalDuration = currentServerTime - RADIO_START_TIMESTAMP
        return globalDuration % AUDIO_FILE_LENGTH
    }

    private fun updateServerOffset(offset: Long) {
        serverOffset = offset
        if (state != State.STARTED) return

        playbackShift = serverOffset - serverOffsetOnStartPlaying
        PlaybackEngine.setPlaybackShift(playbackShift)
    }
}
