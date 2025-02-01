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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import privastead.camera.databinding.FragmentCameraBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null

    private val binding get() = _binding!!

    private val newCameraActivityRequestCode = 2
    private val cameraViewModel: CameraViewModel by viewModels {
        CameraViewModelFactory((parentFragment?.activity?.application as PrivasteadCameraApplication).repository)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        // Switch to the settings fragment if IP not set (should not happen)
        val sharedPref = activity?.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE) ?: return root

        val recyclerView: RecyclerView = binding.recyclerview
        val adapter = CameraListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this.context)

        val fab: FloatingActionButton = binding.fab
        fab.setOnClickListener {
            val intent = Intent(
                parentFragment?.activity?.applicationContext,
                NewCameraActivity::class.java
            )
            startActivityForResult(intent, newCameraActivityRequestCode)
        }

        cameraViewModel.allCameras.observe(viewLifecycleOwner) { cameras ->
            cameras.let { adapter.submitList(it) }

            // Do the FCM update only after we have a camera paired.
            // If we call RustNativeInterface().initialize(..., first_time = false) before
            // any camera is paired, the native code will crash since it will try to retrieve
            // non-existing state from the file system.
            if (!cameras.isEmpty()) {
                var needFcmUpdate =
                    sharedPref.getBoolean(getString(R.string.need_update_fcm_token), false)

                if (needFcmUpdate) {
                    sharedPref.getString(getString(R.string.fcm_token), "")
                        ?.let {
                            val cameraSet = sharedPref.getStringSet(getString(R.string.camera_set), emptySet())
                            cameraSet?.forEach { name ->
                                RustNativeInterface().updateToken(
                                    name, it, sharedPref,
                                    requireParentFragment().requireActivity().applicationContext
                                )
                            }
                        }
                    with(sharedPref.edit()) {
                        //FIXME: we don't check the return value from updateToken. What if it failed?
                        putBoolean(getString(R.string.need_update_fcm_token), false)
                        apply()
                    }
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == newCameraActivityRequestCode && resultCode == Activity.RESULT_OK) {
            // Set this shared variable early to prevent the user from clicking on the fab
            // button again while we're processing the request here.
            Toast.makeText(
                parentFragment?.activity?.applicationContext,
                getString(R.string.wait_for_add),
                Toast.LENGTH_LONG
            ).show()

            var cameraName = intentData?.getStringExtra(getString(R.string.intent_extra_camera_name))
            var cameraIP = intentData?.getStringExtra(getString(R.string.intent_extra_camera_ip))
            var cameraSecret = intentData?.getByteArrayExtra(getString(R.string.intent_extra_camera_secret))
            if (cameraName != null && cameraIP != null && cameraSecret != null) {
                val sharedPref = activity?.getSharedPreferences(
                    getString(R.string.shared_preferences),
                    Context.MODE_PRIVATE
                )
                GlobalScope.launch {
                    if (RustNativeInterface().addCamera(
                            cameraName,
                            cameraIP,
                            cameraSecret,
                            sharedPref!!,
                            requireParentFragment().requireActivity().applicationContext
                        )
                    ) {
                        val camera = Camera(cameraName)
                        cameraViewModel.insert(camera)

                        // We also store the set of camera names in sharedPreferences
                        val cameraSet =
                            sharedPref.getStringSet(getString(R.string.camera_set), mutableSetOf())
                                ?.toMutableSet()
                        with(sharedPref!!.edit()) {
                            cameraSet?.add(cameraName)
                            putStringSet(getString(R.string.camera_set), cameraSet)
                            putBoolean(
                                getString(R.string.first_time_connection_done) + "_" + cameraName,
                                true
                            )
                            apply()
                        }
                    } else {
                        /* FIXME: can't show Toast here.
                        Toast.makeText(
                            parentFragment?.activity?.applicationContext,
                            getString(R.string.add_failed),
                            Toast.LENGTH_LONG
                        ).show()
                         */
                    }
                }
            } else {
                Toast.makeText(
                    parentFragment?.activity?.applicationContext,
                    getString(R.string.add_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}