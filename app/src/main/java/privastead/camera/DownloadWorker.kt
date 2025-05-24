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
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class DownloadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val sharedPref = applicationContext.getSharedPreferences(
            applicationContext.getString(R.string.shared_preferences),
            Context.MODE_PRIVATE
        )

        with(sharedPref.edit()) {
            putBoolean(
                applicationContext.getString(R.string.downloading_motion_videos),
                true
            )
            apply()
        }

        val cameraName = inputData.getString(applicationContext.getString(R.string.camera_name_key))

        retrieveVideos(cameraName!!)

        with(sharedPref.edit()) {
            putBoolean(
                applicationContext.getString(R.string.downloading_motion_videos),
                false
            )
            apply()
        }

        return Result.success()
    }

    private fun retrieveVideos(cameraName: String) {
        val repository = (applicationContext as PrivasteadCameraApplication).repository

        val sharedPref = applicationContext.getSharedPreferences(applicationContext.getString(R.string.shared_preferences), Context.MODE_PRIVATE)
        var epoch = sharedPref.getLong("epoch$cameraName", 2)

        while (true) {
            val result = HttpClient.downloadVideo(
                applicationContext,
                sharedPref,
                cameraName,
                epoch,
                "encVideo$epoch"
            )
            result.fold(
                onSuccess = { file ->
                    val decFileName = RustNativeInterface().decryptVideo(
                        cameraName, file.name, sharedPref,
                        applicationContext,
                    )
                    file.delete()

                    if (decFileName != "Error") {
                        //add to database
                        val videoPending = Video(cameraName, decFileName, false, true)
                        repository.deleteVideo(videoPending)
                        val video = Video(cameraName, decFileName, true, true)
                        repository.insertVideo(video)
                    } else {
                        // This could happen if the encrypted file has been tampered with.
                        // FIXME: we should remove the entry from the database. But we don't have timestamp here!
                        //val videoPending = Video(cameraName, "video_" + timestamp + ".mp4", false, true)
                        //repository.deleteVideo(videoPending)
                        // FIXME: should we inform the user in addition to removing the pending video?
                    }

                    epoch += 1;
                    //advance the epoch
                    with(sharedPref.edit()) {
                        putLong("epoch$cameraName", epoch)
                        apply()
                    }
                },
                onFailure = { error ->
                    return
                }
            )
        }
    }
}