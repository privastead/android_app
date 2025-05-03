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
import privastead.camera.PrivasteadCameraApplication
import privastead.camera.R
import privastead.camera.VideoListAdapter.VideoViewHolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


class VideoListAdapter : ListAdapter<Video, VideoViewHolder>(VIDEOS_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.video, current.received, current.motion)

        holder.itemView.setOnClickListener {
            if (current.received) {
                val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
                val bundle = Bundle()
                bundle.putString("camera", current.camera)
                bundle.putBoolean("livestream", false)
                bundle.putString("video", current.video)
                intent.putExtras(bundle)
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    holder.itemView.context.getString(R.string.wait_pending_video),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        holder.itemView.setOnLongClickListener {
            if (current.received) {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Confirm!")
                    .setMessage(holder.itemView.context.getString(R.string.delete_confirmation_text))
                    .setPositiveButton("Yes") { _, _ ->
                        var videoFile = File(
                            holder.itemView.context.getFilesDir().toString() + "/" + current.video
                        )

                        val video = Video(current.camera, current.video, current.received, current.motion)
                        val repository =
                            (holder.itemView.context.applicationContext as PrivasteadCameraApplication).repository
                        repository.deleteVideo(video)
                        if (!videoFile.delete()) {
                            Toast.makeText(
                                holder.itemView.context.applicationContext,
                                holder.itemView.context.getString(R.string.delete_failed),
                                Toast.LENGTH_LONG
                            )
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Confirm!")
                    .setMessage(holder.itemView.context.getString(R.string.delete_pending_confirmation_text))
                    .setPositiveButton("Yes") { _, _ ->
                        val video = Video(current.camera, current.video, current.received, current.motion)
                        val repository =
                            (holder.itemView.context.applicationContext as PrivasteadCameraApplication).repository
                        repository.deleteVideo(video)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            true
        }
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoItemView: TextView = itemView.findViewById(R.id.textView)

        private fun convertTimeToDate(timeInMillis: Long): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = Date(timeInMillis)
            return dateFormat.format(date)
        }

        fun bind(text: String?, received: Boolean, motion: Boolean) {
            var time_string = text?.split("_")?.toTypedArray()?.get(1)
                ?.split(".")?.toTypedArray()?.get(0)
            var time_long: Long? = time_string?.toLongOrNull()
            val date = time_long?.let { convertTimeToDate(it * 1000L) }
            if (motion) {
                if (received) {
                    videoItemView.text = "Motion at $date"
                } else {
                    videoItemView.text = "Motion at $date (pending)"
                }
            } else {
                videoItemView.text = "Livestream at $date"
            }
        }

        companion object {
            fun create(parent: ViewGroup): VideoViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return VideoViewHolder(view)
            }
        }
    }

    companion object {
        private val VIDEOS_COMPARATOR = object : DiffUtil.ItemCallback<Video>() {
            override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
                return (oldItem.camera == newItem.camera &&
                        oldItem.video == newItem.video)
            }
        }
    }
}