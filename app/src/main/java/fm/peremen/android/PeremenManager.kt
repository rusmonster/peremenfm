package fm.peremen.android

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaFormat
import android.os.SystemClock
import androidx.preference.PreferenceManager
import fm.peremen.android.utils.*
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs
import kotlin.properties.Delegates

private const val TIME_URL = "https://prmn.rogozhin.pro/time2"
private const val AUDIO_FILE_URL = "https://prmn.rogozhin.pro/snd/peremen.mp3"
private const val AUDIO_FILE_NAME = "peremen.mp3"
private const val AUDIO_FILE_NAME_PCM = "peremen.raw"
private const val AUDIO_FILE_LENGTH = 296250L
private const val RADIO_START_TIMESTAMP = 1612384206000L

@SuppressLint("StaticFieldLeak")
object PeremenManager {
    enum class Status { IDLE, CACHING, DECODING, POSITIONING, PLAYING }

    var status by Delegates.observable(Status.IDLE) { _, _, _ -> notifyChanged() }
        private set

    val isStarted get() = status != Status.IDLE

    var playbackPosition: Long = 0
        private set

    var totalPatchMills: Long = 0
        private set

    var playbackShift: Long = 0
        private set

    var severOffsetAccuracy: Long = 0
        private set

    var severOffsetUsedProbesCount: Int = 0
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

    private val serverTimeResults = sortedSetOf<TimeRequestResult>({ o1, o2 -> o1.networkLatency.compareTo(o2.networkLatency) })

    private var serverOffsetOnStartPlaying: Long = 0

    private var serverOffset: Long = 0

    fun setup(context: Context) {
        this.context = context
    }

    fun start() {
        if (state != State.IDLE) return
        state = State.STARTED
        isError = false

        currentJob = managerScope.launch {
            try {
                ensureCache()
                ensureStartPosition();
                launch { updateServerOffsetContinuously() }
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

        if (!sharedPreferences.isAudioCached) {
            status = Status.CACHING
            Timber.d("Caching begin")
            downloadFileToCache(context, AUDIO_FILE_URL, AUDIO_FILE_NAME)
            sharedPreferences.isAudioCached = true
            Timber.d("Caching success")
        }

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

    private suspend fun ensureStartPosition() {
        if (serverTimeResults.isEmpty()) {
            status = Status.POSITIONING
            Timber.d("Positioning begin")
            val serverTimeResult = performServerTimeRequest(TIME_URL, 5)
            Timber.d("initial network latency: ${serverTimeResult.networkLatency}")
            updateServerOffset(serverTimeResult)
        }
    }

    private suspend fun updateServerOffsetContinuously() {
        while (true) {
            delay(2000)

            val result = runCatching { performServerTimeRequest(TIME_URL, 5) }.getOrNull() ?: continue
            updateServerOffset(result)

            playbackShift = serverOffset - serverOffsetOnStartPlaying
            PlaybackEngine.setPlaybackShift(playbackShift)
        }
    }

    private suspend fun play() {
        status = Status.PLAYING

        try {
            PlaybackEngine.create(context)

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            PlaybackEngine.setChannelCount(sharedPreferences.channelCount)
            PlaybackEngine.setSampleRate(sharedPreferences.sampleRate)

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

    private fun updateServerOffset(result: TimeRequestResult) {
//        Timber.d("serverTimeResults:")
//        serverTimeResults.forEachIndexed { i, r -> Timber.d("\t$i) $r") }

        serverTimeResults += result
        while (serverTimeResults.size > 100) serverTimeResults.remove(serverTimeResults.last())

        val avgOffset = serverTimeResults.sumOf { it.serverOffset } / serverTimeResults.size
        val avgDiff = serverTimeResults.sumOf { abs(it.serverOffset - avgOffset) } / serverTimeResults.size

//        Timber.d("new result: $result")
//        Timber.d("new serverTimeResults:")
//        serverTimeResults.forEachIndexed { i, r -> Timber.d("\t$i) $r; diff: ${ abs(r.serverOffset - avgOffset) }") }

        val preciseResults = serverTimeResults.filter { abs(it.serverOffset - avgOffset) <= avgDiff }
        val preciseAvgOffset = preciseResults.sumOf { it.serverOffset } / preciseResults.size
        severOffsetAccuracy = preciseResults.sumOf { abs(it.serverOffset - preciseAvgOffset) } / preciseResults.size
        severOffsetUsedProbesCount = preciseResults.size

//        Timber.d("preciseResults:")
//        preciseResults.forEachIndexed { i, r -> Timber.d("\t$i) $r; diff: ${ abs(r.serverOffset - avgOffset) }") }

        Timber.d("preciseAvgOffset: $preciseAvgOffset; old: $serverOffset; diff: ${ abs(serverOffset - preciseAvgOffset) }")
        serverOffset = preciseAvgOffset
    }
}
