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
import android.content.SharedPreferences
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.BaseDataSource
import com.google.android.exoplayer2.upstream.DataSpec
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// FIXME: we have one potential race condition here.
// if we receive a motion video when we're starting the livestream,
// we will get interrupted and won't read the livestream fast enough,
// resulting in the camera terminating livestream without us knowing it.

class RustNativeDataSource(isNetwork: Boolean, cam: String,
                           shrdPref: SharedPreferences, ctxt: Context): BaseDataSource(isNetwork) {
    private var camera: String = cam
    private var sharedPref: SharedPreferences = shrdPref
    private var context: Context = ctxt
    private lateinit var outputStream: FileOutputStream
    private var needToCreateFile: Boolean = false
    private var needToCloseFile: Boolean = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        // Wait for any ongoing download of motion videos to finish
        while (sharedPref.getBoolean(context.getString(R.string.downloading_motion_videos), false)) {
            Thread.sleep(1000)
        }

        var connected = false

        if (!sharedPref.getBoolean(context.getString(R.string.livestream_end_processed), true)) {
            // Clean up the last session if we didn't get a chance to do that on close()
            RustNativeInterface().livestreamEnd(camera, sharedPref, context)
            connected = true
        }

        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.livestream_end_processed), false)
            apply()
        }

        if (connected) {
            if (!RustNativeInterface().livestreamStartNoConnect(camera)) {
                throw IOException("Error: livestreamStart failed!")
            }
        } else {
            if (!RustNativeInterface().livestreamStart(camera, sharedPref, context)) {
                throw IOException("Error: livestreamStart failed!")
            }
        }

        needToCreateFile = true

        return C.LENGTH_UNSET.toLong()
    }

    override fun getUri(): Uri? {
        //FIXME: not needed
        return Uri.parse("tcp://0.0.0.0:0")
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        var videoBuffer = RustNativeInterface().livestreamReadNoConnect(camera, readLength)
        var videoBufferLen = videoBuffer.size
        if (videoBufferLen == 0) {
            return 0
        }

        if (needToCreateFile) {
            // Create directory if it doesn't exist
            val directory = File(context.getFilesDir().toString() + "/camera_dir_" + camera)
            directory.mkdirs()

            val timestamp: Long = System.currentTimeMillis() / 1000
            val filePath = context.getFilesDir().toString() + "/camera_dir_" + camera +
                    "/video_" + camera + "_" + timestamp + ".mp4"
            outputStream = FileOutputStream(filePath)

            val repository = (context as PrivasteadCameraApplication).repository
            val videoName = "video_" + camera + "_" + timestamp + ".mp4"
            val video = Video(camera, videoName, true, false)
            repository.insertVideo(video)

            needToCreateFile = false
            needToCloseFile = true
        }

        outputStream.write(videoBuffer)

        videoBuffer.copyInto(buffer, offset, 0, videoBufferLen)

        return videoBufferLen
    }

    @Throws(IOException::class)
    override fun close() {
        if (needToCloseFile) {
            outputStream.close()
            needToCloseFile = false
        }

        RustNativeInterface().livestreamEndNoConnect(camera)

        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.livestream_end_processed), true)
            apply()
        }
    }
}