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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import privastead.camera.databinding.ActivityMainBinding

private var serverActivityLaunched = false

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val serverActivityRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (serverActivityLaunched) {
            return
        }

        // Ask for notification permission if needed
        if (!notificationPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        val sharedPref = getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)

        // Set theme
        val isChecked = sharedPref.getBoolean(getString(R.string.saved_theme_switch_state), true)
        toggleTheme(isChecked)

        /*
        // If the app crashes while livestreaming, motion notifications remain paused indefinitely.
        // Resume them at every app launch time.
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.pause_motion_notification), false)
            apply()
        }
         */

        val userCredentials = sharedPref.getString(getString(R.string.user_credentials), "Error")
        if (userCredentials == "Error") {
            serverActivityLaunched = true
            val intent = Intent(this, ServerActivity::class.java)
            startActivityForResult(intent, serverActivityRequestCode)
            return
        }

        renderFragments()
    }

    private fun notificationPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                POST_NOTIFICATIONS
            ).toTypedArray()
    }

    private fun renderFragments() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_camera, R.id.nav_settings), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)
        //FIXME: what if result is not ok?
        if (requestCode == serverActivityRequestCode && resultCode == Activity.RESULT_OK) {
            renderFragments()
        }
        serverActivityLaunched = false
    }

    //FIXME: we have the same function in SettingsFragment
    private fun toggleTheme(nightMode: Boolean) {
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}