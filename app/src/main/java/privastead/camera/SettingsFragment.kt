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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat.recreate
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import privastead.camera.databinding.FragmentSettingsBinding
import java.io.File


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val serverActivityRequestCode = 5

    // FIXME: look into the min version we support.
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Theme setting
        val sharedPref = activity?.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE) ?: return root
        binding.themeSwitch.isChecked = sharedPref.getBoolean(getString(R.string.saved_theme_switch_state), true)

        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleTheme(isChecked)
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.saved_theme_switch_state), isChecked)
                apply()
            }

            parentFragment?.activity?.parent?.let { recreate(it) }
        }

        // Notifications setting
        var needNotification = sharedPref.getBoolean(getString(R.string.saved_need_notification_state), true)
        binding.notificationSwitch.isChecked = needNotification

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.saved_need_notification_state), isChecked)
                apply()
            }
        }

        val updateIpButton = binding.buttonUpdateIp
        updateIpButton.setOnClickListener {
            val intent = Intent(parentFragment?.activity?.applicationContext, ServerActivity::class.java)
            startActivityForResult(intent, serverActivityRequestCode)
        }

        val useMobile = sharedPref.getBoolean(getString(R.string.use_mobile_state), false)
        val connectivityManager = parentFragment?.activity?.applicationContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isRestricted = (connectivityManager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED)

        binding.mobileNetworkSwitch.isChecked = useMobile && !isRestricted
        binding.mobileNetworkSwitch.setOnCheckedChangeListener { _, isChecked ->
            // FIXME: We're not sure if the user follows the instructions correctly.
            // But we currently have no way of checking.
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.use_mobile_state), isChecked)
                apply()
            }

            if (isChecked) {
                AlertDialog.Builder(parentFragment?.activity)
                    .setTitle("Next step")
                    .setMessage(getString(R.string.mobile_network_next_step_instructions))
                    .setPositiveButton("Continue") { _, _ ->
                        val intent =
                            Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                                data = Uri.parse("package:${context?.packageName}")
                            }
                        context?.startActivity(intent)
                    }
                    .setNegativeButton("Go back", null)
                    .show()
            }
        }

        return root
    }

    private fun toggleTheme(nightMode: Boolean) {
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}