package fm.peremen.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import fm.peremen.android.MainViewModel.State.*
import fm.peremen.android.utils.downloadFileToCache
import fm.peremen.android.utils.isAudioCached
import fm.peremen.android.utils.performServerTimeOffsetRequest
import fm.peremen.android.utils.playFileFromCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TIME_URL = "https://prmn.rogozhin.pro/time2"
private const val AUDIO_FILE_URL = "https://prmn.rogozhin.pro/snd/peremen.mp3"
private const val AUDIO_FILE_NAME = "peremen.mp3"
private const val AUDIO_FILE_LENGTH = 296250L
private const val RADIO_START_TIMESTAMP = 1612384206341

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val text = MutableLiveData<String>()

    val isStarted = MutableLiveData(false)

    private enum class State { IDLE, STARTED, STOPPING }

    private var state = IDLE

    private val applicationContext = application.applicationContext

    private lateinit var currentJob: Job

    private var serverOffset: Long? = null

    fun onButtonClick() {
        when (state) {
            IDLE -> {
                state = STARTED
                currentJob = startJob()
            }
            STARTED -> {
                state = STOPPING
                currentJob.cancel()
            }
            STOPPING -> {} // do nothing, just wait until job finishes
        }
    }

    private fun startJob() = viewModelScope.launch {
        isStarted.value = true
        PeremenService.start(applicationContext)
        try {
            doJob()
        } catch (e: CancellationException) {
            Timber.d("Job cancelled")
            text.value = ""
        } catch (e: Exception) {
            Timber.e(e)
            text.value = applicationContext.getString(R.string.error_occured)
        } finally {
            isStarted.value = false
            PeremenService.stop(applicationContext)
            state = IDLE
        }
    }

    private suspend fun doJob() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        if (!sharedPreferences.isAudioCached) {
            Timber.d("Caching begin")
            text.value = applicationContext.getString(R.string.caching)
            downloadFileToCache(applicationContext, AUDIO_FILE_URL, AUDIO_FILE_NAME)
            sharedPreferences.isAudioCached = true
            Timber.d("Caching success")
        }

        if (serverOffset == null) {
            Timber.d("Positioning begin")
            text.value = applicationContext.getString(R.string.positioning)
            serverOffset = performServerTimeOffsetRequest(TIME_URL)
        }

        Timber.d("Playback begin")
        playFileFromCache(applicationContext, AUDIO_FILE_NAME, AUDIO_FILE_LENGTH, serverOffset!!, RADIO_START_TIMESTAMP) { isSynchronizing, position, offset ->
            if (isSynchronizing) {
                text.value = applicationContext.getString(R.string.synchronizing, offset)
            } else {
//                text.value = applicationContext.getString(R.string.playing, offset)
                text.value = "Position: $position ($offset)"
            }
        }
    }
}
