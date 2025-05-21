package com.example.mad

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mad.databinding.FragmentBudgetBinding
import com.google.android.material.snackbar.Snackbar

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FinanceViewModel
    private lateinit var notificationHelper: NotificationHelper

    private val supportedCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "LKR")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateBudgetProgress()
        } else {
            showSnackbar("Notifications disabled - budget alerts won't work")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        notificationHelper = NotificationHelper(requireContext())

        val prefsHelper = PrefsHelper(requireContext())
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            requireActivity(),
            FinanceViewModelFactory(prefsHelper, database)
        ).get(FinanceViewModel::class.java)

        setupCurrencySpinner()
        setupObservers()
        setupSaveButton()

        return binding.root
    }

    private fun setupSaveButton() {
        binding.btnSaveSettings.setOnClickListener {
            try {
                val budgetText = binding.etBudget.text.toString()
                val budget = budgetText.toDoubleOrNull() ?: 0.0

                if (budget >= 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (notificationHelper.hasNotificationPermission()) {
                            saveBudget(budget)
                        } else {
                            requestNotificationPermission()
                        }
                    } else {
                        saveBudget(budget)
                    }
                } else {
                    showSnackbar("Please enter a valid budget")
                }
            } catch (e: Exception) {
                Log.e("BudgetFragment", "Error saving budget", e)
                showSnackbar("Error saving budget")
            }
        }
    }

    private fun saveBudget(budget: Double) {
        viewModel.setMonthlyBudget(budget)
        showSnackbar("Budget saved")
        updateBudgetProgress()
    }

    private fun setupObservers() {
        viewModel.monthlyBudget.observe(viewLifecycleOwner) { budget ->
            binding.etBudget.setText(budget.toString())
            updateBudgetProgress()
        }

        viewModel.currency.observe(viewLifecycleOwner) { savedCurrency ->
            if (supportedCurrencies.contains(savedCurrency)) {
                (binding.currencySpinner as? AutoCompleteTextView)?.setText(savedCurrency, false)
            }
            updateBudgetProgress()
        }

        viewModel.getCurrentMonthExpenses().observe(viewLifecycleOwner) {
            updateBudgetProgress()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSnackbar(message: String) {
        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            binding.tvSpent.text = message
            Log.w("BudgetFragment", "Couldn't show Snackbar: ${e.message}")
        }
    }

    private fun setupCurrencySpinner() {
        try {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, supportedCurrencies)
            (binding.currencySpinner as? AutoCompleteTextView)?.setAdapter(adapter)

            (binding.currencySpinner as? AutoCompleteTextView)?.setOnItemClickListener { parent, _, position, _ ->
                try {
                    val selectedCurrency = parent.getItemAtPosition(position) as String
                    viewModel.setCurrency(selectedCurrency)
                    showSnackbar("Currency set to $selectedCurrency")
                    updateBudgetProgress()
                } catch (e: Exception) {
                    Log.e("BudgetFragment", "Error setting currency", e)
                    showSnackbar("Error setting currency")
                }
            }
        } catch (e: Exception) {
            Log.e("BudgetFragment", "Error setting up currency spinner", e)
            showSnackbar("Error setting up currency selection")
        }
    }

    private fun updateBudgetProgress() {
        val budget = viewModel.monthlyBudget.value ?: 0.0
        viewModel.getCurrentMonthExpenses().observe(viewLifecycleOwner) { spent ->
            val remaining = budget - spent

            binding.tvSpent.text = "Spent: ${viewModel.formatCurrency(spent)}" // Using formatCurrency as in your ViewModel
            binding.tvRemaining.text = "Remaining: ${viewModel.formatCurrency(remaining)}" // Using formatCurrency as in your ViewModel

            if (budget > 0) {
                val progress = (spent / budget * 100).toInt()
                binding.progressBudget.max = 100
                binding.progressBudget.progress = progress.coerceIn(0, 100)
                binding.tvProgressPercent.text = "$progress% used"

                val colorRes = when {
                    progress >= 100 -> R.color.error
                    progress >= 75 -> R.color.warning
                    else -> R.color.success
                }
                val color = ContextCompat.getColor(requireContext(), colorRes)
                binding.progressBudget.setIndicatorColor(color)
                binding.tvProgressPercent.setTextColor(color)

                checkAndNotifyBudgetStatus(progress, spent, budget)
            } else {
                binding.progressBudget.progress = 0
                binding.tvProgressPercent.text = "0% used"
            }
        }
    }

    private fun checkAndNotifyBudgetStatus(progress: Int, spent: Double, budget: Double) {
        if (progress >= 75) {
            val message = if (progress >= 100) {
                "Budget exceeded! You've spent ${viewModel.formatCurrency(spent)} of ${viewModel.formatCurrency(budget)}" // Using formatCurrency as in your ViewModel
            } else {
                "Budget warning! You've used $progress% of your budget"
            }
            showSnackbar(message)

            if (notificationHelper.hasNotificationPermission()) {
                notificationHelper.showBudgetWarning(progress)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (e: Exception) {
                Log.e("BudgetFragment", "Error requesting notification permission", e)
                showSnackbar("Error requesting notification permission")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}