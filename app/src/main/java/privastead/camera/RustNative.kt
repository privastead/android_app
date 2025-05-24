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

/// Usage note:
/// 1. If any of the functions other than initialize() returns an error, then
///    the next function that is called must be initialize().
class RustNative {
    init {
        System.loadLibrary("privastead_android_app_native")
    }

    /// Used when connecting to the server and create the local state.
    /// @firstTime: true if it's the very first time we're doing this.
    ///             false if we want to initialize using persisted state.
    /// returns false on error.
    external fun initialize(filesDir: String, cameraName: String, firstTime: Boolean): Boolean

    /// Used to deregister a camera.
    /// Note: Currently, we only allow one camera only, therefore no input parameter is needed to
    /// specify the camera.
    /// returns false on error.
    external fun deregister(cameraName: String)

    /// Connect to a new camera.
    /// returns false on error.
    external fun addCamera(cameraName: String, cameraIP: String, cameraSecret: ByteArray, standaloneCamera: Boolean, wifiSsid: String, wifiPassword: String): Boolean

    /// Used to decrypt a motion video
    /// Returns the decrypted video filename on success.
    /// returns "Error" on error.
    external fun decryptVideo(cameraName: String, encVideoFileName: String): String

    /// Used to decrypt a video timestamp received via FCM
    /// Returns "timestamp" on success.
    /// returns "Error" on error.
    external fun decryptFcmTimestamp(cameraName: String, msg: ByteArray): String

    /// Returns the MLS group name used for motion videos.
    /// The name is needed to retrieve encrypted videos from the server
    external fun getMotionGroupName(cameraName: String): String

    /// Returns the MLS group name used for livestream videos.
    /// The name is needed to retrieve encrypted video chunks from the server
    external fun getLivestreamGroupName(cameraName: String): String

    /// Used to decrypt a chunk of decrypted livestream data
    /// Returns an empty array on error
    external fun livestreamDecrypt(cameraName: String, encData: ByteArray, expectedChunkNumber: Long): ByteArray

    /// Used to apply an MLS commit message sent prior to each livestream session
    /// returns false on error.
    external fun livestreamUpdate(cameraName: String, commitMsg: ByteArray): Boolean
}