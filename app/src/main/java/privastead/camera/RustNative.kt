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
    external fun initialize(serverIP: String, token: String, filesDir: String, cameraName: String, firstTime: Boolean, userCredentials: ByteArray, needNetwork: Boolean): Boolean

    /// Used to deregister a camera.
    /// Note: Currently, we only allow one camera only, therefore no input parameter is needed to
    /// specify the camera.
    /// returns false on error.
    external fun deregister(cameraName: String)

    /// Used the FCM token in the server.
    /// returns false on error.
    external fun updateToken(token: String, cameraName: String): Boolean

    /// Connect to a new camera.
    /// returns false on error.
    external fun addCamera(cameraName: String, cameraIP: String, cameraSecret: ByteArray, standaloneCamera: Boolean, wifiSsid: String, wifiPassword: String): Boolean

    /// Used to process incoming messages and receive videos.
    /// Returns an array of "camera-name_timestamp" on success.
    /// returns "None" when there's nothing to return.
    /// returns "Error" on error.
    external fun receive(cameraName: String): String

    /// Used to decrypt an MLS message (received via FCM)
    /// Returns "camera-name_timestamp" on success.
    /// returns "None" when there's nothing to return.
    /// returns "Error" on error.
    external fun decrypt(cameraName: String, msg: ByteArray): String

    /// Used to start the livestream from the camera
    /// returns false on error.
    external fun livestreamStart(cameraName: String): Boolean

    /// Used to end the livestream from the camera
    /// returns false on error.
    external fun livestreamEnd(cameraName: String): Boolean

    /// Used to read the next bytearray from the livestream
    /// Currently, panics on error.
    external fun livestreamRead(cameraName: String, len: Int): ByteArray
}