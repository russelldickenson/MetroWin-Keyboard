package com.metrowin.keyboard.settings

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R
import java.util.concurrent.Executors

/** A keyboard cannot show a permission dialog or a file picker, so it opens this invisible screen to do it and closes it at once. */
class HelperActivity : AppCompatActivity() {
    private val mic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) explainMicDenied(this); finish() }
    private val picture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) { finish(); return@registerForActivityResult }
        val app = KeyboardApp.of(this)
        Executors.newSingleThreadExecutor().execute {
            runCatching { contentResolver.openInputStream(uri)?.let { app.clipboard.addImage(it, (contentResolver.getType(uri) ?: "image/jpeg").substringAfter('/')) } }
            runOnUiThread { finish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_MIC -> mic.launch(Manifest.permission.RECORD_AUDIO)
            MODE_CLIP_IMAGE -> picture.launch("image/*")
            else -> finish()
        }
    }

    companion object {
        /** Set once the microphone was refused, so that "never asked yet" and "Android will not ask again" can be told apart. */
        const val PREF_MIC_DENIED = "mic_denied"

        /** True when the microphone was refused and Android will not show the dialog again. */
        fun micBlocked(a: android.app.Activity): Boolean =
            KeyboardApp.of(a).prefs.sp.getBoolean(PREF_MIC_DENIED, false) &&
                androidx.core.content.ContextCompat.checkSelfPermission(a, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(a, Manifest.permission.RECORD_AUDIO)

        fun openAppSettings(a: android.app.Activity) {
            runCatching { a.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", a.packageName, null))) }
        }

        /**
         * The microphone was refused: say why voice typing does not start, and, when Android will not ask again, open the screen where it can be turned on
         * (the system shows no dialog then, so without this a tap on the microphone would do nothing at all).
         */
        fun explainMicDenied(a: android.app.Activity) {
            KeyboardApp.of(a).prefs.sp.edit().putBoolean(PREF_MIC_DENIED, true).apply()
            val blocked = !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(a, Manifest.permission.RECORD_AUDIO)
            android.widget.Toast.makeText(a, if (blocked) R.string.voice_permission_blocked else R.string.voice_permission_denied, android.widget.Toast.LENGTH_LONG).show()
            if (blocked) openAppSettings(a)
        }

        const val EXTRA_MODE = "mode"
        const val MODE_MIC = "mic"
        const val MODE_CLIP_IMAGE = "clip_image"
    }
}
