package com.example.mad

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mad.databinding.DialogPinEntryBinding
import java.util.concurrent.TimeUnit

class PinEntryDialog(private val onPinEntered: (String) -> Unit) : DialogFragment() {
    private var _binding: DialogPinEntryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinEntryBinding.inflate(layoutInflater)

        return AlertDialog.Builder(requireActivity())
            .setTitle("Enter Your PIN")
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Submit") { _, _ ->
                validateAndSubmit()
            }
            .setNegativeButton("Cancel") { _, _ ->
                requireActivity().finishAffinity()
            }
            .create()
    }

    private fun validateAndSubmit() {
        val pin = binding.etPin.text.toString()
        val prefsHelper = PrefsHelper(requireContext())

        if (prefsHelper.isAppLocked()) {
            val remainingMillis = prefsHelper.getRemainingLockTime()
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) + 1
            Toast.makeText(requireContext(),
                "App locked. Try again in $minutes minutes",
                Toast.LENGTH_LONG).show()
            dismiss()
            requireActivity().finish()
            return
        }

        when {
            pin.length != 4 -> Toast.makeText(requireContext(),
                "PIN must be 4 digits",
                Toast.LENGTH_SHORT).show()
            else -> {
                if (prefsHelper.validatePin(pin)) {
                    prefsHelper.resetPinAttempts()
                    prefsHelper.setAppLocked(false)
                    onPinEntered(pin)
                    dismiss()
                } else {
                    prefsHelper.incrementPinAttempts()
                    if (prefsHelper.getPinAttempts() >= 3) {
                        prefsHelper.setAppLocked(true)
                        Toast.makeText(requireContext(),
                            "Too many attempts. App locked for 5 minutes.",
                            Toast.LENGTH_LONG).show()
                        dismiss()
                        requireActivity().finish()
                    } else {
                        val attemptsLeft = 3 - prefsHelper.getPinAttempts()
                        Toast.makeText(requireContext(),
                            "Wrong PIN. Attempts left: $attemptsLeft",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}