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
            if (RustNativeInterface().init(
                    cameraName!!,
                    sharedPref,
                    applicationContext,
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