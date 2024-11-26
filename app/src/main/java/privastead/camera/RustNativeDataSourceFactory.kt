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
import com.google.android.exoplayer2.upstream.DataSource

class RustNativeDataSourceFactory(cam: String, shrdPref: SharedPreferences,
                                  ctxt: Context): DataSource.Factory {
    private var camera: String = cam
    private var sharedPref: SharedPreferences = shrdPref
    private var context: Context = ctxt

    override fun createDataSource(): DataSource {
        return RustNativeDataSource(true, camera, sharedPref, context)
    }
}