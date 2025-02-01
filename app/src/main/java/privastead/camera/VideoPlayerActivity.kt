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
 * https://github.com/yusufcakmak/ExoPlayerSample
 * Apache License, Version 2.0.
 */

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import privastead.camera.databinding.ActivityVideoPlayerBinding
import java.io.File
import java.io.FileInputStream


class VideoPlayerActivity : Activity() {

    private lateinit var simpleExoPlayer: ExoPlayer
    private lateinit var binding: ActivityVideoPlayerBinding
    private var videoPath: String? = null
    private var isLivestream: Boolean = false
    private var camera: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        if (bundle != null) {
            camera = bundle.getString("camera")
            isLivestream = bundle.getBoolean("livestream")
            val saveVideo = findViewById<Button>(R.id.save_video)

            if (!isLivestream) {
                var video = bundle.getString("video")

                if (video != null) {
                    videoPath = File(
                        getFilesDir().toString() + "/camera_dir_" + camera.toString() +
                                "/" + video.toString()
                    ).toString()

                    saveVideo.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Confirm!")
                            .setMessage(getString(R.string.download_confirmation_text))
                            .setPositiveButton("Yes") { _, _ ->
                                saveVideoToGallery(applicationContext, videoPath!!, video)
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                }
            } else {
                saveVideo.visibility = View.GONE
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, VideoListActivity::class.java)
        val bundle = Bundle()
        bundle.putString("camera", camera)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun saveVideoToGallery(context: Context, srcFilePath: String, dstFileName: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, dstFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }

        val uri: Uri? = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(srcFilePath).copyTo(outputStream)
            }
        }

        if (uri != null) {
            // Notify gallery about file
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), dstFileName)
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { path, uri ->
                // FIXME: Toast doesn't show
                Toast.makeText(
                    context,
                    getString(R.string.video_downloaded),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initializePlayerFile(videoUrl: String) {
        val mediaDataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)

        val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        val mediaSourceFactory = DefaultMediaSourceFactory(mediaDataSourceFactory)

        simpleExoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        simpleExoPlayer.addMediaSource(mediaSource)

        simpleExoPlayer.playWhenReady = true
        simpleExoPlayer.prepare()
        binding.playerView.player = simpleExoPlayer
        binding.playerView.requestFocus()
    }

    private fun initializePlayerLivestream() {
        val sharedPref = getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
        val dataSourceFactory = RustNativeDataSourceFactory(camera!!, sharedPref, applicationContext)
        val mediaDataSourceFactory = DefaultDataSource.Factory(this, dataSourceFactory)

        //FIXME: we don't need the URI
        val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri("tcp://192.168.0.130:8888"))

        val mediaSourceFactory = DefaultMediaSourceFactory(mediaDataSourceFactory)

        simpleExoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        simpleExoPlayer.addMediaSource(mediaSource)

        simpleExoPlayer.playWhenReady = true
        simpleExoPlayer.prepare()
        binding.playerView.player = simpleExoPlayer
        binding.playerView.requestFocus()
        binding.playerView.useController = false
        binding.playerView.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING)
    }

    private fun releasePlayer() {
        simpleExoPlayer.release()
    }

    public override fun onStart() {
        super.onStart()

        if (Util.SDK_INT > 23) {
            if (isLivestream) {
                initializePlayerLivestream()
            } else {
                videoPath?.let { initializePlayerFile(it) }
            }
        }
    }

    public override fun onResume() {
        super.onResume()

        if (Util.SDK_INT <= 23) {
            if (isLivestream) {
                initializePlayerLivestream()
            } else {
                videoPath?.let { initializePlayerFile(it) }
            }
        }
    }

    public override fun onPause() {
        super.onPause()

        if (Util.SDK_INT <= 23) releasePlayer()
    }

    public override fun onStop() {
        super.onStop()

        if (Util.SDK_INT > 23) releasePlayer()
    }
}