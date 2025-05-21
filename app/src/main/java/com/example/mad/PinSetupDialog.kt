package com.example.mad

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mad.databinding.DialogPinSetupBinding

class PinSetupDialog(private val onPinSet: (String) -> Unit) : DialogFragment() {
    private var _binding: DialogPinSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinSetupBinding.inflate(layoutInflater)

        // Configure PIN input fields
        setupPinInputFields()

        return AlertDialog.Builder(requireActivity())
            .setTitle("Set Up Security PIN")
            .setView(binding.root)
            .setPositiveButton("Submit", null) // Set to null to override default behavior
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .setCancelable(false)
            .create()
            .apply {
                // Override positive button to prevent auto-dismiss
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        validateAndSubmit()
                    }
                }
            }
    }

    private fun setupPinInputFields() {
        // Only allow numeric input
        binding.etPin1.inputType = InputType.TYPE_CLASS_NUMBER
        binding.etPin2.inputType = InputType.TYPE_CLASS_NUMBER

        // Limit to 4 digits
        binding.etPin1.filters = arrayOf(InputFilter.LengthFilter(4))
        binding.etPin2.filters = arrayOf(InputFilter.LengthFilter(4))

        // Clear fields when focused
        binding.etPin1.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etPin1.text?.clear()
        }
        binding.etPin2.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etPin2.text?.clear()
        }
    }

    private fun validateAndSubmit() {
        val pin1 = binding.etPin1.text.toString()
        val pin2 = binding.etPin2.text.toString()

        when {
            pin1.length != 4 || pin2.length != 4 -> {
                showError("PIN must be 4 digits")
                binding.etPin1.text?.clear()
                binding.etPin2.text?.clear()
            }
            pin1 != pin2 -> {
                showError("PINs don't match")
                binding.etPin2.text?.clear()
            }
            else -> {
                onPinSet(pin1)
                dismiss()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}