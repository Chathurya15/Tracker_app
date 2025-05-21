package com.example.mad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mad.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: FinanceViewModel
    private lateinit var prefsHelper: PrefsHelper
    private lateinit var notificationHelper: NotificationHelper

    // PIN authentication variables
    private var isPinAuthenticated = false
    private var lastPinAuthTime: Long = 0
    private var pinAttempts = 0
    private val maxPinAttempts = 3

    // Notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkAndScheduleReminders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize binding first
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            prefsHelper = PrefsHelper(this)
            notificationHelper = NotificationHelper(this) // Moved this up

            // Check onboarding status
            if (!prefsHelper.isOnboardingCompleted()) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return
            }

            // Initialize ViewModel after notificationHelper is ready
            viewModel = ViewModelProvider(
                this,
                FinanceViewModelFactory(
                    prefsHelper,
                    AppDatabase.getDatabase(applicationContext)
                )
            ).get(FinanceViewModel::class.java).apply {
                setNotificationHelper(notificationHelper) // Now notificationHelper exists
            }

            // Setup UI components
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)

            setupBottomNavigation()
            requestNotificationPermission()

            // Check PIN authentication
            if (prefsHelper.getAppPin() != null && !isPinAuthenticated()) {
                showPinAuthentication()
                return
            }
            if (intent?.getBooleanExtra("PIN_VERIFIED", false) == true) {
                isPinAuthenticated = true
                lastPinAuthTime = System.currentTimeMillis()
            }
            if (prefsHelper.isAppLocked()) {
                showAppLockedMessage()
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 3000)
                return
            }


            // Set default fragment if no state saved
            if (savedInstanceState == null) {
                binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization failed", e)
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("App initialization failed. Please restart.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
        }
    }

    private fun showAppLockedMessage() {
        val remainingMillis = prefsHelper.getRemainingLockTime()
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) + 1
        AlertDialog.Builder(this)
            .setTitle("App Locked")
            .setMessage("Too many incorrect attempts. Please try again in $minutes minutes.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Check if app is locked when returning to app
        if (prefsHelper.isAppLocked()) {
            showAppLockedMessage()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment? = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_budget -> BudgetFragment()
                else -> null
            }

            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }
    }

//    override fun onResume() {
//        super.onResume()
//        // Check PIN when returning to app
//        if (prefsHelper.getAppPin() != null && !isPinAuthenticated()) {
//            showPinAuthentication()
//        }
//    }

    private fun isPinAuthenticated(): Boolean {
        // Check if PIN authentication was recently done (within 5 minutes)
        return isPinAuthenticated && (System.currentTimeMillis() - lastPinAuthTime < TimeUnit.MINUTES.toMillis(5))
    }

    private fun showPinAuthentication() {
        val pin = prefsHelper.getAppPin()
        if (pin == null) {
            initializeAfterPinVerification()
            return
        }

        val dialog = PinEntryDialog { enteredPin ->
            if (prefsHelper.validatePin(enteredPin)) {
                isPinAuthenticated = true
                lastPinAuthTime = System.currentTimeMillis()
                initializeAfterPinVerification()
            } else {
                pinAttempts++
                if (pinAttempts >= maxPinAttempts) {
                    Toast.makeText(this, "Too many attempts. Exiting.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this,
                        "Invalid PIN (${maxPinAttempts - pinAttempts} attempts left)",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "PinEntryDialog")
    }


    private fun initializeAfterPinVerification() {
        try {
            // Setup UI components that need PIN verification first
            setupBottomNavigation()

            // Check if we need to set default fragment
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Post-PIN initialization failed", e)
        }
    }

    internal fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Notifications Permission")
            .setMessage("This app needs notification permission to send you budget alerts and daily reminders.")
            .setPositiveButton("OK") { dialog, which ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndScheduleReminders() {
        if (prefsHelper.getDailyReminderEnabled()) {
            scheduleDailyReminder(this)
        } else {
            cancelDailyReminder()
        }
    }

    fun scheduleDailyReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    internal fun cancelDailyReminder() {
        WorkManager.getInstance(this).cancelUniqueWork("daily_reminder")
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .addToBackStack(null)
                    .commit()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}