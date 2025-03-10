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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import privastead.camera.databinding.FragmentCameraBinding
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null

    private val binding get() = _binding!!

    private val cameraViewModel: CameraViewModel by viewModels {
        CameraViewModelFactory((parentFragment?.activity?.application as PrivasteadCameraApplication).repository)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
                NewCameraSelectorActivity::class.java
            )

            startForResult.launch(intent)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Set this shared variable early to prevent the user from clicking on the fab
            // button again while we're processing the request here.
            Toast.makeText(
                parentFragment?.activity?.applicationContext,
                getString(R.string.wait_for_add),
                Toast.LENGTH_LONG
            ).show()

            val intentData: Intent? = result.data
            val cameraName = intentData?.getStringExtra(getString(R.string.intent_extra_camera_name))
            if (cameraName != null) {
                val sharedPref = activity?.getSharedPreferences(
                    getString(R.string.shared_preferences),
                    Context.MODE_PRIVATE
                )

                val camera = Camera(cameraName)
                cameraViewModel.insert(camera)

                // We also store the set of camera names in sharedPreferences
                val cameraSet =
                    sharedPref!!.getStringSet(
                        getString(R.string.camera_set),
                        mutableSetOf()
                    )
                        ?.toMutableSet()
                with(sharedPref.edit()) {
                    cameraSet?.add(cameraName)
                    putStringSet(getString(R.string.camera_set), cameraSet)
                    apply()
                }
            } else {
                Log.e(getString(R.string.app_name),"Unexpected parameters from NewCameraActivity")
                Toast.makeText(
                    parentFragment?.activity?.applicationContext,
                    getString(R.string.add_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}