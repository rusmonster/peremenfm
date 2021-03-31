package fm.peremen.android

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import fm.peremen.android.databinding.ActivityMainBinding
import fm.peremen.android.utils.hasGpsPermission

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.buttonView.setOnClickListener { viewModel.onButtonClick() }

        if (savedInstanceState == null) {
            ensureLocationPermission()
        }
    }

    private fun ensureLocationPermission() {
        if (hasGpsPermission()) {
            return
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationPermissionExplanationDialog().show(supportFragmentManager, null)
            return
        }

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
    }
}
