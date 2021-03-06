package fm.peremen.android

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import fm.peremen.android.databinding.ActivityMainBinding
import fm.peremen.android.utils.hasGpsPermission
import fm.peremen.android.utils.isGpsExplanationShown

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.buttonView.setOnClickListener { viewModel.onButtonClick() }
        binding.buttonShare.setOnClickListener { share() }
        binding.buttonInfo.setOnClickListener { showInfoDialog() }

        if (savedInstanceState == null) {
            ensureLocationPermission()
        }
    }

    private fun share() {
        val messageLink = getString(R.string.share_link)
        val messageText = getString(R.string.share_text, messageLink)

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, messageText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        runCatching { startActivity(shareIntent) }
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_info_title)
            .setView(R.layout.dialog_info)
            .setPositiveButton(R.string.ok) { _, _ -> }
            .show()
    }

    private fun ensureLocationPermission() {
        if (hasGpsPermission()) {
            return
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isGpsExplanationShown = sharedPreferences.isGpsExplanationShown
        val shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

        if (!isGpsExplanationShown || shouldShowRequestPermissionRationale) {
            LocationPermissionExplanationDialog().show(supportFragmentManager, null)
            sharedPreferences.isGpsExplanationShown = true
            return
        }

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
    }
}
