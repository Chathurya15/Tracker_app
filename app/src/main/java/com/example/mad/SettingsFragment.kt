package com.example.mad

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mad.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FinanceViewModel
    private lateinit var prefsHelper: PrefsHelper
    private lateinit var backupHelper: BackupRestoreHelper

    // Permission request code
    private companion object {
        const val REQUEST_CODE_STORAGE_PERMISSIONS = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefsHelper = PrefsHelper(requireContext())
        backupHelper = BackupRestoreHelper(requireContext())

        // Initialize ViewModel
        val prefsHelper = PrefsHelper(requireContext())
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            requireActivity(),  // Use requireActivity() for shared ViewModel
            FinanceViewModelFactory(prefsHelper, database)
        ).get(FinanceViewModel::class.java)

        // Set up daily reminder switch
        setupDailyReminderSwitch()

        // Set up backup/export buttons
        setupBackupButtons()

        // Set up PIN management
        setupPinControls()

        return binding.root
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSuccessSnackbar("Notification permission granted")
        } else {
            showErrorSnackbar("Notifications disabled - reminders won't work")
        }
    }




    private fun setupDailyReminderSwitch() {
        binding.switchDailyReminder.isChecked = prefsHelper.getDailyReminderEnabled()

        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.setDailyReminderEnabled(isChecked)
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted
                    showSuccessSnackbar("Daily reminders enabled")
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else if (!isChecked) {
                (activity as? MainActivity)?.cancelDailyReminder()
                showSuccessSnackbar("Daily reminders disabled")
            }
        }
    }

    private fun setupDarkModeSwitch() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        binding.switchDarkMode.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            requireActivity().recreate()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDarkModeSwitch()
    }

    private fun setupBackupButtons() {
        binding.btnExport.setOnClickListener {
            if (hasStoragePermissions()) {
                performExport()
            } else {
                requestStoragePermissions()
            }
        }

        binding.btnImport.setOnClickListener {
            if (hasStoragePermissions()) {
                showImportConfirmationDialog()
            } else {
                requestStoragePermissions()
            }
        }
    }

    private fun setupPinControls() {
        updatePinControls()

        binding.btnPinSetup.setOnClickListener {
            when {
                prefsHelper.getAppPin() == null -> {
                    PinSetupDialog { newPin ->
                        try {
                            prefsHelper.setAppPin(newPin)
                            showSuccessSnackbar("PIN set successfully")
                            updatePinControls()
                        } catch (e: IllegalArgumentException) {
                            showErrorSnackbar(e.message ?: "Invalid PIN")
                        }
                    }.show(childFragmentManager, "PinSetupDialog")
                }
                else -> {
                    showChangePinDialog()
                }
            }
        }

        binding.btnRemovePin.setOnClickListener {
            showRemovePinConfirmation()
        }
    }

    private fun showChangePinDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change PIN")
            .setMessage("Do you want to change your existing PIN?")
            .setPositiveButton("Change") { _, _ ->
                PinSetupDialog { newPin ->
                    try {
                        prefsHelper.setAppPin(newPin)
                        showSuccessSnackbar("PIN changed successfully")
                        updatePinControls()
                    } catch (e: IllegalArgumentException) {
                        showErrorSnackbar(e.message ?: "Invalid PIN")
                    }
                }.show(childFragmentManager, "PinSetupDialog")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemovePinConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove PIN")
            .setMessage("Are you sure you want to remove PIN protection?")
            .setPositiveButton("Remove") { _, _ ->
                prefsHelper.clearAppPin()
                showSuccessSnackbar("PIN removed")
                updatePinControls()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePinControls() {
        val pinExists = prefsHelper.getAppPin() != null
        binding.btnPinSetup.text = if (pinExists) "Change PIN" else "Set Up PIN"
        binding.btnRemovePin.visibility = if (pinExists) View.VISIBLE else View.GONE
    }

    private fun hasStoragePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_CODE_STORAGE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSIONS -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    when {
                        binding.btnExport.isPressed -> performExport()
                        binding.btnImport.isPressed -> showImportConfirmationDialog()
                    }
                } else {
                    showErrorSnackbar("Storage permissions are required for backup features")
                }
            }
        }
    }

    private fun performExport() {
        try {
            viewModel.transactions.value?.let { transactions ->
                if (transactions.isEmpty()) {
                    showErrorSnackbar("No transactions to export")
                    return
                }
                backupHelper.exportData(transactions) { success, message ->
                    activity?.runOnUiThread {
                        if (success) {
                            showSuccessDialog("Backup Successful",
                                "Data saved to Documents/finance_tracker_backup.json")
                        } else {
                            showErrorDialog("Backup Failed", message ?: "Unknown error")
                        }
                    }
                }
            } ?: showErrorSnackbar("No transactions data available")
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Export failed", e)
            showErrorSnackbar("Export failed: ${e.localizedMessage}")
        }
    }

    private fun showImportConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Backup")
            .setMessage("This will overwrite all current transactions. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                performImport()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performImport() {
        try {
            backupHelper.importData { transactions, message ->
                activity?.runOnUiThread {
                    if (transactions != null) {
                        viewModel.replaceAllTransactions(transactions)
                        showSuccessDialog("Restore Successful",
                            "Imported ${transactions.size} transactions")
                    } else {
                        showErrorDialog("Restore Failed", message ?: "Unknown error")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Import failed", e)
            showErrorSnackbar("Import failed: ${e.localizedMessage}")
        }
    }

    private fun showSuccessDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccessSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}