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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import privastead.camera.CameraListAdapter.CameraViewHolder
import java.io.File

class CameraListAdapter : ListAdapter<Camera, CameraViewHolder>(CAMERAS_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        return CameraViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.camera)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, VideoListActivity::class.java)
            val bundle = Bundle()
            bundle.putString("camera", current.camera)
            intent.putExtras(bundle)
            holder.itemView.context.startActivity(intent);
        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Confirm!")
                .setMessage(holder.itemView.context.getString(R.string.delete_camera_videos_confirmation_text))
                .setPositiveButton("Yes") { _, _ ->
                    // Remove camera and its videos from databases
                    val camera = Camera(current.camera)
                    val repository = (holder.itemView.context.applicationContext as PrivasteadCameraApplication).repository
                    repository.deleteCameraVideos(current.camera)
                    repository.deleteCamera(camera)

                    // Deregister from the server
                    val sharedPref = holder.itemView.context.getSharedPreferences(holder.itemView.context.getString(R.string.shared_preferences), Context.MODE_PRIVATE)
                    RustNativeInterface().deregister(current.camera, sharedPref, holder.itemView.context)

                    // Remove camera name from camera_set and update a few other sharedpref vars.
                    val cameraSet = sharedPref.getStringSet(holder.itemView.context.getString(R.string.camera_set), mutableSetOf())?.toMutableSet()

                    with(sharedPref.edit()) {
                        // FIXME: why do we set NeedUpdateFCMToken to true here?
                        putBoolean(holder.itemView.context.getString(R.string.need_update_fcm_token), true)
                        remove(holder.itemView.context.getString(R.string.first_time_connection_done) + "_" + current.camera)
                        cameraSet?.remove(current.camera)
                        putStringSet(holder.itemView.context.getString(R.string.camera_set), cameraSet)
                        apply()
                    }

                    // Finally, delete all the files related to this camera.
                    var cameraDir = File(holder.itemView.context.getFilesDir().toString() + "/camera_dir_" + current.camera)
                    if (!cameraDir.deleteRecursively()) {
                        Toast.makeText(
                            holder.itemView.context.applicationContext,
                            holder.itemView.context.getString(R.string.delete_all_failed),
                            Toast.LENGTH_LONG)
                    }
                }
                .setNegativeButton("No", null)
                .show()

            true
        }
    }

    class CameraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cameraItemView: TextView = itemView.findViewById(R.id.textView)

        fun bind(text: String?) {
            cameraItemView.text = text
        }

        companion object {
            fun create(parent: ViewGroup): CameraViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return CameraViewHolder(view)
            }
        }
    }

    companion object {
        private val CAMERAS_COMPARATOR = object : DiffUtil.ItemCallback<Camera>() {
            override fun areItemsTheSame(oldItem: Camera, newItem: Camera): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Camera, newItem: Camera): Boolean {
                return oldItem.camera == newItem.camera
            }
        }
    }
}