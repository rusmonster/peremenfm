package fm.peremen.android

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fm.peremen.android.utils.getActivityReadableStatus
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val text = MutableLiveData<String>()

    val text2 = MutableLiveData<String>()

    val text3 = MutableLiveData<String>()

    val text4 = MutableLiveData<String>()

    val isStarted = MutableLiveData(false)

    val isKeepScreenOn = MutableLiveData(true)

    val showDebugInfo = BuildConfig.DEBUG

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    init {
        keepScreenOnUntilNoServerOffset()
        update()
        PeremenManager.onChanged += this::update
    }

    override fun onCleared() {
        super.onCleared()
        PeremenManager.onChanged -= this::update
    }

    private fun update() {
        isStarted.value = PeremenManager.isStarted
        text.value = PeremenManager.getActivityReadableStatus(context)
        text2.value = "Latency: ${String.format("%1.0f", PeremenManager.latency)}; TotalPatchMills ${PeremenManager.totalPatchMills}"
        text3.value = "Shift: ${PeremenManager.playbackShift}"
        text4.value = "PlaybackPosition: ${PeremenManager.playbackPosition} (${PeremenManager.synchronizationOffset})"
    }

    private fun keepScreenOnUntilNoServerOffset() = viewModelScope.launch {
        runCatching { PeremenManager.ensureServerOffset() }
        isKeepScreenOn.value = false
    }

    fun onButtonClick() {
        if (!PeremenManager.isStarted) {
            PeremenManager.start()
        } else {
            PeremenManager.stop()
        }
    }
}
