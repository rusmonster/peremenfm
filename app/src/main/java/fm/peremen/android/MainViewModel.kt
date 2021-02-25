package fm.peremen.android

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import fm.peremen.android.utils.getActivityReadableStatus

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
        text.value = PeremenManager.getActivityReadableStatus(context)
    }

    fun onButtonClick() {
        if (!PeremenManager.isStarted) {
            PeremenManager.start()
        } else {
            PeremenManager.stop()
        }
    }
}
