package privastead.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import privastead.camera.databinding.ActivityInternetConnectBinding

class InternetConnectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInternetConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInternetConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraName = intent.getStringExtra(getString(R.string.intent_extra_camera_name))

        val buttonFinish = findViewById<Button>(R.id.button_finish)
        buttonFinish.setOnClickListener {
            // register with the delivery service
            val sharedPref = this.applicationContext.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
            if (RustNativeInterface().connect(
                    cameraName!!,
                    sharedPref,
                    applicationContext,
                    true
            )) {
                val replyIntent = Intent()
                replyIntent.putExtra(getString(R.string.intent_extra_camera_name), cameraName)
                setResult(Activity.RESULT_OK, replyIntent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.add_failed_no_internet),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}