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