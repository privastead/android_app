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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object HttpClient {
    fun downloadVideo(
        context: Context,
        sharedPref: SharedPreferences,
        cameraName: String,
        epoch: Long,
        fileName: String
    ): Result<File> {
        val serverIp = sharedPref.getString(context.getString(R.string.saved_ip), null)
            ?: return Result.failure(Exception("Failed to retrieve the server IP address"))

        val username = sharedPref.getString(context.getString(R.string.server_username), null)
            ?: return Result.failure(Exception("Failed to retrieve the server username"))

        val password = sharedPref.getString(context.getString(R.string.server_password), null)
            ?: return Result.failure(Exception("Failed to retrieve the server password"))

        val cameraMotionGroupName = RustNativeInterface().getMotionGroupName(cameraName, sharedPref, context)

        val filesDir = context.filesDir.toString() + "/camera_dir_" + cameraName
        val dir = File(filesDir)
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.failure(Exception("Failed to create directory"))
        }

        val fileUrl = "http://$serverIp:8080/$cameraMotionGroupName/$epoch"
        val client = OkHttpClient()

        val credentials = "$username:$password"
        val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url(fileUrl)
            .addHeader("Authorization", basicAuthHeader)
            .build()

        val outputFile = File(filesDir, fileName)

        val delRequest = Request.Builder()
            .url(fileUrl)
            .addHeader("Authorization", basicAuthHeader)
            .delete()
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to download file: ${response.code} ${response.message}"))
            }

            response.body?.byteStream()?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val delResponse = client.newCall(delRequest).execute()
            if (!delResponse.isSuccessful) {
                return Result.failure(Exception("Failed to delete video from the server: ${response.code} ${response.message}"))
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadFcmToken(
        context: Context,
        sharedPref: SharedPreferences,
        token: String
    ): Result<Unit> {
        val serverIp = sharedPref.getString(context.getString(R.string.saved_ip), null)
            ?: return Result.failure(Exception("Failed to retrieve the server IP address"))

        val username = sharedPref.getString(context.getString(R.string.server_username), null)
            ?: return Result.failure(Exception("Failed to retrieve the server username"))

        val password = sharedPref.getString(context.getString(R.string.server_password), null)
            ?: return Result.failure(Exception("Failed to retrieve the server password"))

        val client = OkHttpClient()

        val credentials = "$username:$password"
        val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val requestBody = token.toRequestBody("text/plain".toMediaType())

        val request = Request.Builder()
            .url("http://$serverIp:8080/fcm_token")
            .addHeader("Authorization", basicAuthHeader)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to send data: ${response.code} ${response.message}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun livestreamStart(
        context: Context,
        sharedPref: SharedPreferences,
        cameraName: String
    ): Result<Unit> {
        val serverIp = sharedPref.getString(context.getString(R.string.saved_ip), null)
            ?: return Result.failure(Exception("Failed to retrieve the server IP address"))

        val username = sharedPref.getString(context.getString(R.string.server_username), null)
            ?: return Result.failure(Exception("Failed to retrieve the server username"))

        val password = sharedPref.getString(context.getString(R.string.server_password), null)
            ?: return Result.failure(Exception("Failed to retrieve the server password"))

        val cameraLivestreamGroupName = RustNativeInterface().getLivestreamGroupName(cameraName, sharedPref, context)

        val client = OkHttpClient()

        val credentials = "$username:$password"
        val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val requestBody = "".toRequestBody("text/plain".toMediaType())

        val request = Request.Builder()
            .url("http://$serverIp:8080/livestream/$cameraLivestreamGroupName")
            .addHeader("Authorization", basicAuthHeader)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to send data: ${response.code} ${response.message}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun livestreamRetrieve(
        context: Context,
        sharedPref: SharedPreferences,
        cameraName: String,
        chunkNumber: ULong,
    ): Result<ByteArray> {
        val serverIp = sharedPref.getString(context.getString(R.string.saved_ip), null)
            ?: return Result.failure(Exception("Failed to retrieve the server IP address"))

        val username = sharedPref.getString(context.getString(R.string.server_username), null)
            ?: return Result.failure(Exception("Failed to retrieve the server username"))

        val password = sharedPref.getString(context.getString(R.string.server_password), null)
            ?: return Result.failure(Exception("Failed to retrieve the server password"))

        val cameraLivestreamGroupName = RustNativeInterface().getLivestreamGroupName(cameraName, sharedPref, context)

        val client = OkHttpClient()

        val credentials = "$username:$password"
        val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url("http://$serverIp:8080/livestream/$cameraLivestreamGroupName/$chunkNumber")
            .addHeader("Authorization", basicAuthHeader)
            .build()

        val delRequest = Request.Builder()
            .url("http://$serverIp:8080/$cameraLivestreamGroupName/$chunkNumber")
            .addHeader("Authorization", basicAuthHeader)
            .delete()
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch data: ${response.code} ${response.message}"))
            }

            val delResponse = client.newCall(delRequest).execute()
            if (!delResponse.isSuccessful) {
                return Result.failure(Exception("Failed to delete video from the server: ${response.code} ${response.message}"))
            }

            val byteArray = response.body?.bytes()
                ?: return Result.failure(Exception("Response body is null"))

            Result.success(byteArray)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIXME: shares a lot of code with livestreamStart
    fun livestreamEnd(
        context: Context,
        sharedPref: SharedPreferences,
        cameraName: String
    ): Result<Unit> {
        val serverIp = sharedPref.getString(context.getString(R.string.saved_ip), null)
            ?: return Result.failure(Exception("Failed to retrieve the server IP address"))

        val username = sharedPref.getString(context.getString(R.string.server_username), null)
            ?: return Result.failure(Exception("Failed to retrieve the server username"))

        val password = sharedPref.getString(context.getString(R.string.server_password), null)
            ?: return Result.failure(Exception("Failed to retrieve the server password"))

        val cameraLivestreamGroupName = RustNativeInterface().getLivestreamGroupName(cameraName, sharedPref, context)

        val client = OkHttpClient()

        val credentials = "$username:$password"
        val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val requestBody = "".toRequestBody("text/plain".toMediaType())

        val request = Request.Builder()
            .url("http://$serverIp:8080/livestream_end/$cameraLivestreamGroupName")
            .addHeader("Authorization", basicAuthHeader)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to send data: ${response.code} ${response.message}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}