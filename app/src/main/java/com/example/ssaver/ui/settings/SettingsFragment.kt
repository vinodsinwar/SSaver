package com.example.ssaver.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.ssaver.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSwitch()
        setupDarkModeSwitch()
        setupAboutButton()
    }

    private fun setupThemeSwitch() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.themeSwitch.isChecked = prefs.getBoolean("material_theme", true)

        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("material_theme", isChecked).apply()
            // Theme implementation will be added later
        }
    }

    private fun setupDarkModeSwitch() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.darkModeSwitch.isChecked = prefs.getBoolean("dark_mode", false)

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupAboutButton() {
        binding.aboutButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("About")
                .setMessage("Status Saver\nVersion 1.0\n\nA simple app to save WhatsApp statuses.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 