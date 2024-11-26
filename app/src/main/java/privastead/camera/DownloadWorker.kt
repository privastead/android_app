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

import android.content.Context
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

        retrieveVideos()

        with(sharedPref.edit()) {
            putBoolean(
                applicationContext.getString(R.string.downloading_motion_videos),
                false
            )
            apply()
        }

        return Result.success()
    }

    private fun receive(): String {
        val sharedPref = applicationContext.getSharedPreferences(applicationContext.getString(R.string.shared_preferences), Context.MODE_PRIVATE)
        return RustNativeInterface().receive(sharedPref, applicationContext)
    }

    private fun retrieveVideos() {
        val repository = (applicationContext as PrivasteadCameraApplication).repository

        var response = receive()
        if (response == "None" || response == "Error") {
            return
        } else {
            val videoNames = response.split(",").toTypedArray()
            for (videoName in videoNames) {
                val cameraName = videoName.split("_").toTypedArray().get(1)
                val videoPending = Video(cameraName, videoName, false, true)
                repository.deleteVideo(videoPending)
                val video = Video(cameraName, videoName, true, true)
                repository.insertVideo(video)
            }
        }
    }
}