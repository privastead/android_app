package privastead.camera

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import privastead.camera.databinding.ActivityHotspotConnectBinding

class HotspotConnectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHotspotConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHotspotConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val textViewConnectStatus = findViewById<TextView>(R.id.hotspot_connect_status)
        val buttonNext = findViewById<Button>(R.id.button_next)

        val buttonConnect = findViewById<Button>(R.id.button_hotspot_connect)
        buttonConnect.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Next step")
                .setMessage(getString(R.string.connect_hotspot_instructions))
                .setPositiveButton("Continue") { _, _ ->
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                    textViewConnectStatus.text = getString(R.string.hotspot_connected)
                    buttonNext.isEnabled = true
                }
                .setNegativeButton("Go back", null)
                .show()
        }

        buttonNext.setOnClickListener {
            val intent = Intent(this.applicationContext, NewCameraActivity::class.java)
            intent.putExtra(getString(R.string.camera_type_standalone), true)
            startForResult.launch(intent)
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Forward the result back to CameraFragment
            setResult(Activity.RESULT_OK, result.data)
            finish()
        }
    }
}