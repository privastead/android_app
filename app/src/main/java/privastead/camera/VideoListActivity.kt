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

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoListActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)

        val bundle = intent.extras
        if (bundle != null) {
            var camera = bundle.getString("camera")

            if (camera != null) {
                val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
                val adapter = VideoListAdapter()
                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(this)

                val cameraNameText = findViewById<TextView>(R.id.camera_name)
                cameraNameText.text = "Camera: $camera"

                val videoViewModel: VideoViewModel by viewModels {
                    VideoViewModelFactory(
                        (application as PrivasteadCameraApplication).repository,
                        camera
                    )
                }

                videoViewModel.allVideos.observe(this) { videos ->
                    videos.let { adapter.submitList(it) }
                }

                val livestream = findViewById<Button>(R.id.livestream)
                livestream.setOnClickListener {
                    val intent = Intent(this, VideoPlayerActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("camera", camera)
                    bundle.putBoolean("livestream", true)
                    intent.putExtras(bundle)
                    this.startActivity(intent)
                }

                val deleteAll = findViewById<Button>(R.id.delete_all)
                deleteAll.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Confirm!")
                        .setMessage(getString(R.string.delete_all_confirmation_text))
                        .setPositiveButton("Yes") { _, _ ->
                            var cameraDir = File(
                                this.getFilesDir()
                                    .toString() + "/camera_dir_" + camera
                            )

                            // Delete all videos in the camera repository
                            val repository =
                                (applicationContext as PrivasteadCameraApplication).repository
                            repository.deleteCameraVideos(camera)

                            // Delete all videos in the camera directory (which all are prefixed with video_)
                            CoroutineScope(Dispatchers.IO).launch {
                                val listOfFiles = cameraDir.listFiles()?.filter { it.name.startsWith("video_") } ?: emptyList()
                                val failedFiles = listOfFiles.filterNot { it.delete() }

                                withContext(Dispatchers.Main) {
                                    if (failedFiles.isNotEmpty()) {
                                        Toast.makeText(applicationContext, getString(R.string.delete_all_failed), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.error_camera_access),
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}