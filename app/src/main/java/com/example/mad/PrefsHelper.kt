package com.example.mad

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

class PrefsHelper(context: Context) {
    private val PIN_ATTEMPTS = "pin_attempts"
    private val LAST_ATTEMPT_TIME = "last_attempt_time"
    private val IS_LOCKED = "is_locked"
    private val LOCK_UNTIL = "lock_until"
    private val sharedPref: SharedPreferences = context.getSharedPreferences("FinanceTrackerPrefs", Context.MODE_PRIVATE)

    // Daily reminder methods
    fun setDailyReminderEnabled(enabled: Boolean) {
        sharedPref.edit() { putBoolean("daily_reminder", enabled) }
    }

    fun getDailyReminderEnabled(): Boolean {
        return sharedPref.getBoolean("daily_reminder", false)
    }

    // Onboarding status methods
    fun setOnboardingCompleted(completed: Boolean) {
        sharedPref.edit() { putBoolean("onboarding_completed", completed) }
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPref.getBoolean("onboarding_completed", false)
    }

    // PIN management methods
    fun setAppPin(pin: String) {
        require(pin.length == 4) { "PIN must be 4 digits" }
        sharedPref.edit() { putString("app_pin", pin) }
    }

    fun clearAppPin() {
        sharedPref.edit() { remove("app_pin") }
    }

    fun getAppPin(): String? {
        return sharedPref.getString("app_pin", null)
    }

    fun validatePin(pin: String): Boolean {
        val savedPin = getAppPin()
        return savedPin != null && savedPin == pin && pin.length == 4
    }
    fun incrementPinAttempts() {
        val attempts = getPinAttempts() + 1
        sharedPref.edit { putInt(PIN_ATTEMPTS, attempts) }
        sharedPref.edit { putLong(LAST_ATTEMPT_TIME, System.currentTimeMillis()) }
    }

    fun resetPinAttempts() {
        sharedPref.edit {
            putInt(PIN_ATTEMPTS, 0)
            putLong(LAST_ATTEMPT_TIME, 0)
        }
    }

    fun getPinAttempts(): Int {
        return sharedPref.getInt(PIN_ATTEMPTS, 0)
    }

    fun getLastAttemptTime(): Long {
        return sharedPref.getLong(LAST_ATTEMPT_TIME, 0)
    }

    fun isPinLockedOut(lockoutDuration: Long = TimeUnit.MINUTES.toMillis(5)): Boolean {
        val currentTime = System.currentTimeMillis()
        return getPinAttempts() >= 3 && (currentTime - getLastAttemptTime()) < lockoutDuration
    }

    //pin new adds
    fun setAppLocked(locked: Boolean) {
        sharedPref.edit {
            putBoolean(IS_LOCKED, locked)
            if (locked) {
                putLong(LOCK_UNTIL, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5))
            } else {
                putLong(LOCK_UNTIL, 0)
            }
        }
    }

    fun isAppLocked(): Boolean {
        val lockUntil = sharedPref.getLong(LOCK_UNTIL, 0)
        return sharedPref.getBoolean(IS_LOCKED, false) &&
                System.currentTimeMillis() < lockUntil
    }

    fun getRemainingLockTime(): Long {
        val lockUntil = sharedPref.getLong(LOCK_UNTIL, 0)
        return maxOf(0, lockUntil - System.currentTimeMillis())
    }

    // Budget methods
    fun setMonthlyBudget(budget: Double) {
        sharedPref.edit() { putFloat("monthly_budget", budget.toFloat()) }
    }

    fun getMonthlyBudget(): Double {
        return sharedPref.getFloat("monthly_budget", 0f).toDouble()
    }

    // Currency methods
    fun setCurrency(currency: String) {
        sharedPref.edit() { putString("currency", currency) }
    }

    fun getCurrency(): String {
        return sharedPref.getString("currency", "LKR") ?: "LKR"
    }
}