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

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import java.io.File

class RustNativeInterface {

    //FIXME: we can get sharedPref from context: val sharedPref = context.getSharedPreferences(context.getString(R.string.shared_preferences), Context.MODE_PRIVATE)
    private fun initCore(cameraName: String, sharedPref: SharedPreferences, context: Context, firstTime: Boolean): Boolean {
        /*
        var serverIP = sharedPref.getString(context.getString(R.string.saved_ip), "Error")
        if (serverIP == null || serverIP == "Error") {
            Log.e(context.getString(R.string.app_name), "Error: Failed to retrieve the server IP address")
            return false
        }

        var fcmToken = sharedPref.getString(context.getString(R.string.fcm_token), "Error")
        if (fcmToken == null || fcmToken == "Error") {
            Log.e(context.getString(R.string.app_name), "Error: Failed to retrieve the FCM token")
            return false
        }

        var userCredentialsString = sharedPref.getString(context.getString(R.string.user_credentials), "Error")
        if (userCredentialsString == null || userCredentialsString == "Error") {
            Log.e(context.getString(R.string.app_name), "Error: Failed to retrieve user credentials")
            return false
        }

        var userCredentials = Base64.decode(userCredentialsString, Base64.DEFAULT)
         */

        var filesDir = context.getFilesDir().toString() + "/camera_dir_" + cameraName

        // Create directory if it doesn't exist
        val dir = File(filesDir)
        if (!dir.exists()) {
            val success = dir.mkdirs()
            if (!success) {
                Log.e(context.getString(R.string.app_name), context.getString(R.string.error_create_dir))
            }
        }
        
        return RustNative().initialize(filesDir, cameraName, firstTime)
    }

    fun init(cameraName: String, sharedPref: SharedPreferences, context: Context): Boolean {
        // FIXME: There's a potential race here for determining the firstTimeConnectionDone.
        // For example, if there's a push notification to retrieve videos when we want to pair
        // a camera, we might end up with two initializations with firstTime=true.
        var firstTimeConnectionDone =
            sharedPref.getBoolean(context.getString(R.string.first_time_connection_done) + "_" + cameraName, false)

        if (!firstTimeConnectionDone) {
            val success = initCore(cameraName, sharedPref, context, true)
            
            //FIXME: the goal here was to inform the user that we couldn't connect.
            //But this crashed on the very first call by updateToken at install time.
            //In general, we need a better mechanism to let the user know that the IP address
            //they set is wrong.
            /*
            else {
                Toast.makeText(
                    context,
                    context.getString(R.string.not_connected_server),
                    Toast.LENGTH_LONG
                ).show()

            }
             */

            return success
        } else {
            return initCore(cameraName, sharedPref, context, false)
        }
    }

    fun deregister(cameraName: String, sharedPref: SharedPreferences, context: Context): Boolean {
        if (!init(cameraName, sharedPref, context)) {
            return false
        }

        RustNative().deregister(cameraName)

        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.first_time_connection_done), false)
            apply()
        }

        return true
    }

    fun addCamera(cameraName: String, cameraIP: String, cameraSecret: ByteArray,
                  standaloneCamera: Boolean, wifiSsid: String, wifiPassword: String,
                  sharedPref: SharedPreferences, context: Context): Boolean {
        if (!init(cameraName, sharedPref, context)) {
            return false
        }

        return RustNative().addCamera(cameraName, cameraIP, cameraSecret, standaloneCamera, wifiSsid, wifiPassword)
    }

    fun decryptVideo(cameraName: String, encVideoFileName: String, sharedPref: SharedPreferences, context: Context): String {
        if (!init(cameraName, sharedPref, context)) {
            return "Error"
        }

        return RustNative().decryptVideo(cameraName, encVideoFileName)
    }

    fun decryptFcmTimestamp(cameraName: String, msg: ByteArray, sharedPref: SharedPreferences, context: Context): String {
        if (!init(cameraName, sharedPref, context)) {
            return "Error"
        }

        return RustNative().decryptFcmTimestamp(cameraName, msg)
    }

    fun getMotionGroupName(cameraName: String, sharedPref: SharedPreferences, context: Context): String {
        if (!init(cameraName, sharedPref, context)) {
            return "Error"
        }

        return RustNative().getMotionGroupName(cameraName)
    }

    fun getLivestreamGroupName(cameraName: String, sharedPref: SharedPreferences, context: Context): String {
        if (!init(cameraName, sharedPref, context)) {
            return "Error"
        }

        return RustNative().getLivestreamGroupName(cameraName)
    }

    fun livestreamDecrypt(cameraName: String, encData: ByteArray, sharedPref: SharedPreferences, context: Context): ByteArray {
        if (!init(cameraName, sharedPref, context)) {
            return ByteArray(0)
        }

        return RustNative().livestreamDecrypt(cameraName, encData)
    }

    fun livestreamUpdate(cameraName: String, commitMsg: ByteArray, sharedPref: SharedPreferences, context: Context): Boolean {
        if (!init(cameraName, sharedPref, context)) {
            return false
        }

        return RustNative().livestreamUpdate(cameraName, commitMsg)
    }
}