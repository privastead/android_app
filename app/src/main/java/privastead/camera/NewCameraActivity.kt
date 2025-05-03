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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.InetAddresses
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class NewCameraActivity : AppCompatActivity() {
    private var qrScanned: Boolean = false
    private var cameraSecret: ByteArray = byteArrayOf()

    @RequiresApi(Build.VERSION_CODES.Q)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_camera)

        val editCameraNameView = findViewById<EditText>(R.id.edit_camera_name)
        val editCameraIPView = findViewById<EditText>(R.id.edit_camera_ip)
        val editWifiSsid = findViewById<EditText>(R.id.edit_wifi_ssid)
        val editWifiPassword = findViewById<EditText>(R.id.edit_wifi_password)
        val editWifiPasswordToggle = findViewById<TextInputLayout>(R.id.edit_wifi_password_toggle)

        val standaloneCamera = intent.getBooleanExtra(getString(R.string.camera_type_standalone), true)
        if (standaloneCamera) {
            editCameraIPView.visibility = View.GONE
        } else {
            editWifiSsid.visibility = View.GONE
            editWifiPassword.visibility = View.GONE
            editWifiPasswordToggle.visibility = View.GONE
        }

        val buttonSave = findViewById<Button>(R.id.button_save)
        buttonSave.setOnClickListener {
            val cameraIp = if (standaloneCamera) {
                "10.42.0.1"
            } else {
                editCameraIPView.text.toString()
            }

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
            } else if (TextUtils.isEmpty(cameraIp)) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.enter_camera_ip),
                    Toast.LENGTH_LONG
                ).show()
            } else if (!InetAddresses.isNumericAddress(cameraIp)) {
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
            } else if (standaloneCamera && TextUtils.isEmpty((editWifiSsid.text))) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.enter_wifi_ssid),
                    Toast.LENGTH_LONG
                ).show()
            } else if (standaloneCamera && TextUtils.isEmpty((editWifiPassword.text))) {
                Toast.makeText(
                    this.applicationContext,
                    getString(R.string.enter_wifi_password),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val cameraName = editCameraNameView.text.toString()
                var cameraNameTaken = false
                val sharedPref = this.applicationContext.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
                val cameraSet = sharedPref.getStringSet(getString(R.string.camera_set), emptySet())
                cameraSet?.forEach { name ->
                    if (name == cameraName) {
                        cameraNameTaken = true
                    }
                }

                if (cameraNameTaken) {
                    editCameraNameView.setText("")
                    editCameraNameView.hint = getString(R.string.camera_name_hint)

                    Toast.makeText(
                        this.applicationContext,
                        getString(R.string.camera_name_exists),
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    val wifiSsid = if (standaloneCamera) {
                        editWifiSsid.text.toString()
                    } else {
                        ""
                    }

                    val wifiPassword = if (standaloneCamera) {
                        editWifiPassword.text.toString()
                    } else {
                        ""
                    }

                    if (addCamera(cameraName, cameraIp, cameraSecret, standaloneCamera, wifiSsid, wifiPassword)) {
                        if (standaloneCamera) {
                            val intent =
                                Intent(this.applicationContext, InternetConnectActivity::class.java)
                            intent.putExtra(
                                getString(R.string.intent_extra_camera_name),
                                cameraName
                            )
                            startForResultRelay.launch(intent)
                        } else {
                            // FIXME: do we still need to call init here?
                            if (RustNativeInterface().init(
                                    cameraName,
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
                    } else {
                        Log.e(getString(R.string.app_name), "Rust add_camera failed.")
                        Toast.makeText(
                            this,
                            getString(R.string.add_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        val buttonQr = findViewById<Button>(R.id.button_qr_code)
        buttonQr.setOnClickListener {
            val intent = Intent(this.applicationContext, QrScannerActivity::class.java)
            startForResult.launch(intent)
        }
    }

    private fun addCamera(cameraName: String, cameraIp: String, cameraSecret: ByteArray, standaloneCamera: Boolean, wifiSsid: String, wifiPassword: String): Boolean {
        Toast.makeText(
            this,
            getString(R.string.wait_for_add),
            Toast.LENGTH_LONG
        ).show()

        /*
        if (!bindToWifi(applicationContext)) {
            Toast.makeText(
                this,
                getString(R.string.not_connected_to_hotspot),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        */

        val sharedPref = getSharedPreferences(
            getString(R.string.shared_preferences),
            Context.MODE_PRIVATE
        )

        if (RustNativeInterface().addCamera(
                cameraName,
                cameraIp,
                cameraSecret,
                standaloneCamera,
                wifiSsid,
                wifiPassword,
                sharedPref!!,
                applicationContext
            )) {
            with(sharedPref.edit()) {
                putBoolean(
                    getString(R.string.first_time_connection_done) + "_" + cameraName,
                    true
                )
                apply()
            }
            return true
        }

        return false
    }

    /*
    private fun bindToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network? = connectivityManager.activeNetwork

        if (activeNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                connectivityManager.bindProcessToNetwork(activeNetwork)
                return true
            }
        }
        return false
    }
    */

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData: Intent? = result.data
            val barcode = intentData?.getByteArrayExtra(getString(R.string.intent_extra_barcode))
            if (barcode != null) {
                cameraSecret = barcode
                qrScanned = true
                val qrScanStatusTextView = findViewById<TextView>(R.id.qr_scan_status)
                qrScanStatusTextView.setText(R.string.qr_scanned)
            }
        }
    }

    private val startForResultRelay = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Forward the result back
            setResult(Activity.RESULT_OK, result.data)
            finish()
        }
    }
}