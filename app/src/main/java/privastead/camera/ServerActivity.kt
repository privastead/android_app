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
import android.content.Context
import android.content.Intent
import android.net.InetAddresses
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class ServerActivity : AppCompatActivity() {
    private var qrScanned: Boolean = false
    private var userCredentials: ByteArray = byteArrayOf()

    private val qrScannerActivityRequestCode = 3

    @RequiresApi(Build.VERSION_CODES.Q)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        val editServerIPView = findViewById<EditText>(R.id.edit_server_ip)

        val buttonSave = findViewById<Button>(R.id.button_save)
        buttonSave.setOnClickListener {
            if (TextUtils.isEmpty((editServerIPView.text))) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.enter_server_ip),
                    Toast.LENGTH_LONG
                ).show()
            } else if (!InetAddresses.isNumericAddress(editServerIPView.text.toString())) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.invalid_ip),
                    Toast.LENGTH_LONG
                ).show()

                editServerIPView.setText("")
                editServerIPView.hint = getString(R.string.ip_hint)
            } else if (!qrScanned) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.server_qr_not_scanned_toast),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val serverIP = editServerIPView.text.toString()
                val userCredentialsString: String = Base64.encodeToString(userCredentials, Base64.DEFAULT)

                val sharedPref = getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)

                with(sharedPref.edit()) {
                    putString(getString(R.string.saved_ip), serverIP)
                    putString(getString(R.string.user_credentials), userCredentialsString)
                    apply()
                }

                val replyIntent = Intent()
                setResult(Activity.RESULT_OK, replyIntent)
                finish()
            }
        }

        val buttonQr = findViewById<Button>(R.id.button_qr_code)
        buttonQr.setOnClickListener {
            val intent = Intent(this.applicationContext, QrScannerActivity::class.java)
            startActivityForResult(intent, qrScannerActivityRequestCode)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == qrScannerActivityRequestCode && resultCode == Activity.RESULT_OK) {
            var barcode = intentData?.getByteArrayExtra(getString(R.string.intent_extra_barcode))
            if (barcode != null) {
                userCredentials = barcode
                qrScanned = true
                val qrScanStatusTextView = findViewById<TextView>(R.id.qr_scan_status)
                qrScanStatusTextView.setText(R.string.qr_scanned)
            }
        }
    }
}