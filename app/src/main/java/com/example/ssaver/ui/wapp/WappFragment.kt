package com.example.ssaver.ui.wapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.ssaver.R
import com.example.ssaver.databinding.FragmentWappBinding
import com.example.ssaver.ui.permission.PermissionFragment
import com.google.android.material.tabs.TabLayoutMediator
import androidx.navigation.fragment.findNavController

class WappFragment : Fragment() {

    private var _binding: FragmentWappBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WappViewModel
    private val TAG = "WappFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            viewModel = ViewModelProvider(this).get(WappViewModel::class.java)
            _binding = FragmentWappBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            if (checkPermissions()) {
                setupViewPager()
            } else {
                Log.d(TAG, "Permissions not granted, showing permission fragment")
                requestPermissions()
            }

            binding.swipeRefresh.setOnRefreshListener {
                viewModel.refreshStatuses()
                binding.swipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            throw e
        }
    }

    private fun setupViewPager() {
        try {
            binding.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount() = 2

                override fun createFragment(position: Int) = when (position) {
                    0 -> StatusGridFragment.newInstance(StatusType.IMAGE)
                    else -> StatusGridFragment.newInstance(StatusType.VIDEO)
                }
            }

            TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.tab_images)
                    else -> getString(R.string.tab_videos)
                }
            }.attach()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupViewPager", e)
            throw e
        }
    }

    private fun checkPermissions(): Boolean {
        val context = requireContext()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Checking Android 13+ permissions")
            val hasImagePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val hasVideoPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            
            Log.d(TAG, "Permission status - READ_MEDIA_IMAGES: $hasImagePermission, READ_MEDIA_VIDEO: $hasVideoPermission")
            hasImagePermission && hasVideoPermission
        } else {
            Log.d(TAG, "Checking legacy storage permission")
            val hasStoragePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permission status - READ_EXTERNAL_STORAGE: $hasStoragePermission")
            hasStoragePermission
        }
    }

    private fun requestPermissions() {
        try {
            Log.d(TAG, "Requesting permissions")
            findNavController().navigate(R.id.action_wapp_to_permission)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to permission fragment", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 