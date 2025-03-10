package privastead.camera

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import privastead.camera.databinding.ActivityNewCameraSelectorBinding

class NewCameraSelectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewCameraSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewCameraSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttonStandalone = findViewById<Button>(R.id.button_standalone)
        buttonStandalone.setOnClickListener {
            val intent = Intent(this.applicationContext, HotspotConnectActivity::class.java)
            startForResult.launch(intent)
        }

        val buttonIp = findViewById<Button>(R.id.button_ip)
        buttonIp.setOnClickListener {
            val intent = Intent(this.applicationContext, NewCameraActivity::class.java)
            intent.putExtra(getString(R.string.camera_type_standalone), false)
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