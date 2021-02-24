package fm.peremen.android

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import fm.peremen.android.utils.downloadFileToCache
import fm.peremen.android.utils.isAudioCached
import fm.peremen.android.utils.performServerTimeOffsetRequest
import fm.peremen.android.utils.playFileFromCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.properties.Delegates

private const val TIME_URL = "https://prmn.rogozhin.pro/time2"
private const val AUDIO_FILE_URL = "https://prmn.rogozhin.pro/snd/peremen.mp3"
private const val AUDIO_FILE_NAME = "peremen.mp3"
private const val AUDIO_FILE_LENGTH = 296250L
private const val RADIO_START_TIMESTAMP = 1612384206341

@SuppressLint("StaticFieldLeak")
object PeremenManager {
    enum class Status { IDLE, CACHING, POSITIONING, SYNCHRONIZING, PLAYING }

    var status by Delegates.observable(Status.IDLE) { _, _, _ -> notifyChanged() }
        private set

    val isStarted get() = status != Status.IDLE

    var playbackPosition: Long = 0
        private set

    var synchronizationOffset: Long = 0
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

    private var serverOffset: Long? = null

    fun setup(context: Context) {
        this.context = context
    }

    fun start() {
        if (state != State.IDLE) return
        state = State.STARTED
        isError = false

        currentJob = managerScope.launch {
            PeremenService.start(context)
            try {
                ensureCache()
                play()
            } catch (e: CancellationException) {
                Timber.d("Job cancelled")
            } catch (e: Exception) {
                Timber.e(e)
                isError = true
            } finally {
                PeremenService.stop(context)
                state = State.IDLE
                status = Status.IDLE
            }
        }
    }

    fun stop() {
        if (state != State.STARTED) return
        state = State.STOPPING
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
    }

    private suspend fun play() {
        if (serverOffset == null) {
            status = Status.POSITIONING
            Timber.d("Positioning begin")
            serverOffset = performServerTimeOffsetRequest(TIME_URL)
        }

        Timber.d("Playback begin")
        playFileFromCache(context, AUDIO_FILE_NAME, AUDIO_FILE_LENGTH, serverOffset!!, RADIO_START_TIMESTAMP) { isSynchronizing, position, offset ->
            playbackPosition = position
            synchronizationOffset = offset
            status = if (isSynchronizing) Status.SYNCHRONIZING else Status.PLAYING
            notifyChanged()
        }
    }
}
