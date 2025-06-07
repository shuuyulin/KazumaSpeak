package com.example.japaneselearning

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.japaneselearning.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIRST: Initialize binding and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SECOND: Set up status bar (after setContentView)
        setupStatusBar()

        // THIRD: Set up navigation
        setupNavigation()
    }

    private fun setupStatusBar() {
        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_primary)

        // Make status bar icons dark (for light background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun setupNavigation() {
        // Set up bottom navigation with NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
    }
}