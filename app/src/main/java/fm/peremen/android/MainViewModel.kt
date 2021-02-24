package fm.peremen.android

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import fm.peremen.android.PeremenManager.Status

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val text = MutableLiveData<String>()

    val isStarted = MutableLiveData(false)

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    init {
        update()
        PeremenManager.onChanged += this::update
    }

    override fun onCleared() {
        super.onCleared()
        PeremenManager.onChanged -= this::update
    }

    private fun update() {
        isStarted.value = PeremenManager.isStarted

        when (PeremenManager.status) {

            Status.IDLE ->
                text.value = if (PeremenManager.isError) context.getString(R.string.error_occured) else ""

            Status.CACHING ->
                text.value = context.getString(R.string.caching)

            Status.POSITIONING ->
                text.value = context.getString(R.string.positioning)

            Status.SYNCHRONIZING ->
                text.value = context.getString(R.string.synchronizing, PeremenManager.synchronizationOffset)

            Status.PLAYING ->
                text.value = context.getString(R.string.playing,
                        PeremenManager.playbackPosition, PeremenManager.synchronizationOffset)
        }
    }

    fun onButtonClick() {
        if (!PeremenManager.isStarted) {
            PeremenManager.start()
        } else {
            PeremenManager.stop()
        }
    }
}
