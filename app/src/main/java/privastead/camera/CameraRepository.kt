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

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.*

class CameraRepository(private val cameraDao: CameraDao) {

    val allCameras: Flow<List<Camera>> = cameraDao.getAlphabetizedCameras()

    fun allVideos(camera: String): Flow<List<Video>> {
        return cameraDao.getAlphabetizedVideos(camera)
    }

    @WorkerThread
    fun insert(camera: Camera) {
        GlobalScope.launch {
            cameraDao.insert(camera)
        }
    }

    @WorkerThread
    fun deleteCamera(camera: Camera) {
        GlobalScope.launch {
            cameraDao.deleteCamera(camera)
        }
    }

    interface RepoCallback {
        fun resultReady(result: Int)
    }

    @WorkerThread
    fun cameraExists(camera: String, callback: RepoCallback) {
        GlobalScope.launch {
            val result: Int = cameraDao.cameraExists(camera)
            callback.resultReady(result)
        }
    }

    @WorkerThread
    fun insertVideo(video: Video) {
        GlobalScope.launch {
            cameraDao.insertVideo(video)
        }
    }

    @WorkerThread
    fun deleteVideo(video: Video) {
        GlobalScope.launch {
            cameraDao.deleteVideo(video)
        }
    }

    @WorkerThread
    fun deleteCameraVideos(camera: String) {
        GlobalScope.launch {
            cameraDao.deleteCameraVideos(camera)
        }
    }
}