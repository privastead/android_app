package privastead.camera

/*
 * Copyright (C) 2025  Ardalan Amiri Sani
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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