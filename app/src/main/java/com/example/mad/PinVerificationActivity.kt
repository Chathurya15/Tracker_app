package com.example.mad

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad.databinding.ActivityPinVerificationBinding
import java.util.concurrent.TimeUnit

class PinVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinVerificationBinding
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            prefsHelper = PrefsHelper(this)

            // Skip if no PIN set
            if (prefsHelper.getAppPin() == null) {
                startMainActivity()
                return
            }

            // Check if locked out
            if (prefsHelper.isPinLockedOut()) {
                showLockoutMessage()
                return
            }

            binding = ActivityPinVerificationBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupPinInput()
            setupSubmitButton()
        } catch (e: Exception) {
            Log.e("PinVerification", "Error in onCreate", e)
            // Fallback to main activity if verification fails
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupPinInput() {
        binding.etPin.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etPin.text?.clear()
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            val enteredPin = binding.etPin.text?.toString() ?: ""

            if (enteredPin.length != 4) {
                showToast("PIN must be 4 digits")
                return@setOnClickListener
            }

            if (prefsHelper.isAppLocked()) {
                showLockoutMessage()
                return@setOnClickListener
            }

            if (prefsHelper.validatePin(enteredPin)) {
                // Reset attempts on successful verification
                prefsHelper.resetPinAttempts()
                prefsHelper.setAppLocked(false)
                startMainActivity()
            } else {
                prefsHelper.incrementPinAttempts()
                if (prefsHelper.getPinAttempts() >= 3) {
                    prefsHelper.setAppLocked(true)
                    showLockoutMessage()
                } else {
                    val attemptsLeft = 3 - prefsHelper.getPinAttempts()
                    showToast("Wrong PIN. Attempts left: $attemptsLeft")
                    binding.etPin.text?.clear()
                }
            }
        }
    }

    private fun showLockoutMessage() {
        val remainingMillis = prefsHelper.getRemainingLockTime()
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) + 1 // Round up
        showToast("App locked. Try again in $minutes minutes")

        // Disable UI elements
        binding.etPin.isEnabled = false
        binding.btnSubmit.isEnabled = false

        // Automatically finish after showing message
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 3000)
    }

    private fun handleFailedAttempt() {
        if (prefsHelper.isPinLockedOut()) {
            showLockoutMessage()
        } else {
            val attemptsLeft = 3 - prefsHelper.getPinAttempts()
            showToast("Wrong PIN. Attempts left: $attemptsLeft")
            binding.etPin.text?.clear()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            // Add flag to indicate successful PIN verification
            putExtra("PIN_VERIFIED", true)
        }
        startActivity(intent)
        finish()
    }
}