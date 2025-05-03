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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Query("SELECT * FROM camera_table ORDER BY camera ASC")
    fun getAlphabetizedCameras(): Flow<List<Camera>>

    @Insert(Camera::class, onConflict = OnConflictStrategy.IGNORE)
    fun insert(camera: Camera)

    @Delete(Camera::class)
    fun deleteCamera(camera: Camera)

    @Query("SELECT COUNT(*) FROM camera_table WHERE camera = :camera")
    fun cameraExists(camera: String): Int

    @Query("SELECT * FROM video_table WHERE camera = :camera ORDER BY video DESC")
    fun getAlphabetizedVideos(camera: String): Flow<List<Video>>

    @Insert(Video::class, onConflict = OnConflictStrategy.IGNORE)
    fun insertVideo(video: Video)

    @Delete(Video::class)
    fun deleteVideo(video: Video)

    @Query("DELETE FROM video_Table WHERE camera = :camera")
    fun deleteCameraVideos(camera: String): Int
}