package privastead.camera

/*
 * Copyright (C) 2024  Ardalan Amiri Sani
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

/*
 * This file uses and modifies some code from:
 * https://github.com/android/codelab-android-room-with-a-view (see header below)
 * Apache License, Version 2.0
 *
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity
import android.content.Intent
import android.net.InetAddresses
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class NewCameraActivity : AppCompatActivity(), CameraRepository.RepoCallback {
    private var resultReceived: Boolean = false
    private var resultVal: Int = 0
    private var qrScanned: Boolean = false
    private var cameraSecret: ByteArray = byteArrayOf()

    private val QrScannerActivityRequestCode = 4

    @RequiresApi(Build.VERSION_CODES.Q)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_camera)
        val editCameraNameView = findViewById<EditText>(R.id.edit_camera_name)
        val editCameraIPView = findViewById<EditText>(R.id.edit_camera_ip)

        val buttonSave = findViewById<Button>(R.id.button_save)
        buttonSave.setOnClickListener {

            if (TextUtils.isEmpty(editCameraNameView.text)) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.enter_camera_name),
                    Toast.LENGTH_LONG
                ).show()
            } else if (editCameraNameView.text.toString().contains(" ")) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.camera_name_no_space),
                    Toast.LENGTH_LONG
                ).show()
            } else if (TextUtils.isEmpty((editCameraIPView.text))) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.enter_camera_ip),
                    Toast.LENGTH_LONG
                ).show()
            } else if (!InetAddresses.isNumericAddress(editCameraIPView.text.toString())) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.invalid_ip),
                    Toast.LENGTH_LONG
                ).show()

                editCameraIPView.setText("")
                editCameraIPView.hint = getString(R.string.camera_ip_hint)
            } else if (!qrScanned) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.qr_not_scanned_toast),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                /* TODO: this code checks whether this camera name has been used before or not.
                 * It's buggy. The code marked with FIXME below causes hangs.
                 * For now, we don't need this code since we only allow one camera.
                 * Needs to be fixed if we want to support more than one camera.
                val repository = (this.application as PrivasteadCameraApplication).repository
                resultReceived = false
                repository.cameraExists(editCameraNameView.text.toString(), this)
                // FIXME: not a great trick. The wait should be short, but regardless not great.
                while (!resultReceived) {
                }

                if (resultVal > 0) {
                    editCameraNameView.setText("")
                    editCameraNameView.hint = getString(R.string.camera_name_hint)

                    Toast.makeText(
                        this.applicationContext,
                        getString(R.string.camera_name_exists),
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                 */
                    val replyIntent = Intent()

                    val cameraName = editCameraNameView.text.toString()
                    val cameraIP = editCameraIPView.text.toString()
                    replyIntent.putExtra(getString(R.string.intent_extra_camera_name), cameraName)
                    replyIntent.putExtra(getString(R.string.intent_extra_camera_ip), cameraIP)
                    replyIntent.putExtra(
                        getString(R.string.intent_extra_camera_secret),
                        cameraSecret
                    )
                    setResult(Activity.RESULT_OK, replyIntent)
                    finish()
                //}
            }
        }

        val buttonQr = findViewById<Button>(R.id.button_qr_code)
        buttonQr.setOnClickListener {
            val intent = Intent(this.applicationContext, QrScannerActivity::class.java)
            startActivityForResult(intent, QrScannerActivityRequestCode)
        }
    }

    override fun resultReady(result: Int) {
        resultVal = result
        resultReceived = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == QrScannerActivityRequestCode && resultCode == Activity.RESULT_OK) {
            var barcode = intentData?.getByteArrayExtra(getString(R.string.intent_extra_barcode))
            if (barcode != null) {
                cameraSecret = barcode
                qrScanned = true
                val qrScanStatusTextView = findViewById<TextView>(R.id.qr_scan_status)
                qrScanStatusTextView.setText(R.string.qr_scanned)
            }
        }
    }
}