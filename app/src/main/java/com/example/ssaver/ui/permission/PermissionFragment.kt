package com.example.ssaver.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ssaver.R
import com.example.ssaver.databinding.FragmentPermissionBinding
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionFragment : Fragment() {

    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!
    private val TAG = "PermissionFragment"

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Multiple permissions result: $permissions")
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkAndRequestManageStorage()
        }
    }

    private val requestSinglePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Single permission granted: $isGranted")
        if (isGranted) {
            checkAndRequestManageStorage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentPermissionBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            binding.grantPermissionButton.setOnClickListener {
                requestStoragePermission()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if all permissions are granted when returning from settings
        if (checkAllPermissions()) {
            navigateToWappFragment()
        }
    }

    private fun checkAllPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                hasAndroid13Permissions() && hasManageExternalStoragePermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                hasAndroid10Permissions() && hasManageExternalStoragePermission()
            }
            else -> {
                hasLegacyStoragePermission()
            }
        }
    }

    private fun hasAndroid13Permissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAndroid10Permissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLegacyStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasManageExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not required for Android 9 and below
        }
    }

    private fun requestStoragePermission() {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    Log.d(TAG, "Requesting Android 13+ permissions")
                    val permissions = arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                    
                    if (permissions.all { permission ->
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    }) {
                        checkAndRequestManageStorage()
                    } else {
                        requestMultiplePermissions.launch(permissions)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    Log.d(TAG, "Requesting Android 10+ storage permissions")
                    if (hasAndroid10Permissions()) {
                        checkAndRequestManageStorage()
                    } else {
                        requestSinglePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                else -> {
                    Log.d(TAG, "Requesting legacy storage permission")
                    when {
                        hasLegacyStoragePermission() -> {
                            navigateToWappFragment()
                        }
                        shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                            showStoragePermissionRationale()
                        }
                        else -> {
                            requestSinglePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in requestStoragePermission", e)
            throw e
        }
    }

    private fun checkAndRequestManageStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showManageStoragePermissionDialog()
        } else {
            navigateToWappFragment()
        }
    }

    private fun showManageStoragePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.manage_storage_permission_title)
            .setMessage(R.string.manage_storage_permission_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openManageStorageSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showStoragePermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storage_permission_title)
            .setMessage(R.string.storage_permission_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                requestSinglePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening manage storage settings", e)
                // Fallback to general storage settings if app-specific page fails
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun navigateToWappFragment() {
        try {
            Log.d(TAG, "Navigating to WappFragment")
            findNavController().navigate(R.id.action_permission_to_wapp)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to wapp fragment", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 