package com.example.ssaver

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.ssaver.databinding.ActivityMainBinding
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set default mode to light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            
            // Set up the AppBarConfiguration
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_wapp, R.id.navigation_help, R.id.navigation_settings
                )
            )

            // Set the toolbar title
            binding.toolbar.title = getString(R.string.app_title)

            // Set up bottom navigation
            binding.navView.setupWithNavController(navController)

            // Observe navigation changes to maintain the title
            navController.addOnDestinationChangedListener { _, _, _ ->
                binding.toolbar.title = getString(R.string.app_title)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            throw e
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}