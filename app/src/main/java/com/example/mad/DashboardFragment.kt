package com.example.mad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mad.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FinanceViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // Initialize ViewModel
        val prefsHelper = PrefsHelper(requireContext())
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(
            requireActivity(),  // Use requireActivity() for shared ViewModel
            FinanceViewModelFactory(prefsHelper, database)
        ).get(FinanceViewModel::class.java)

        // Set up observers
        viewModel.transactions.observe(viewLifecycleOwner) { updateUI() }
        viewModel.monthlyBudget.observe(viewLifecycleOwner) { updateUI() }
        viewModel.currency.observe(viewLifecycleOwner) { updateUI() }

        return binding.root
    }

    private fun updateUI() {
        viewModel.getTotalIncome().observe(viewLifecycleOwner) { totalIncome ->
            viewModel.getTotalExpenses().observe(viewLifecycleOwner) { totalExpenses ->
                viewModel.getCurrentMonthExpenses().observe(viewLifecycleOwner) { currentMonthExpenses ->
                    val monthlyBudget = viewModel.monthlyBudget.value ?: 0.0
                    val currencyCode = viewModel.currency.value ?: "USD"

                    val nf = try {
                        NumberFormat.getCurrencyInstance().apply {
                            currency = Currency.getInstance(currencyCode)
                        }
                    } catch (e: IllegalArgumentException) {
                        NumberFormat.getCurrencyInstance(Locale.US)
                    }

                    with(binding) {
                        tvTotalIncome.text = "Income: ${nf.format(totalIncome)}"
                        tvTotalExpenses.text = "Expenses: ${nf.format(totalExpenses)}"
                        tvBalance.text = "Balance: ${nf.format(totalIncome - totalExpenses)}"
                        tvMonthlyBudget.text = "Monthly Budget: ${nf.format(monthlyBudget)}"
                        tvMonthlySpending.text = "This Month: ${nf.format(currentMonthExpenses)}"
                    }

                    // Budget warning
                    if (monthlyBudget > 0 && currentMonthExpenses > monthlyBudget * 0.9) {
                        val message = if (currentMonthExpenses >= monthlyBudget) {
                            "You've exceeded your monthly budget!"
                        } else {
                            "You're approaching your monthly budget!"
                        }
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }

                    // Pie chart setup
                    viewModel.getExpensesByCategory().observe(viewLifecycleOwner) { expensesByCategory ->
                        val pieChart: PieChart = binding.pieChart
                        val entries = mutableListOf<PieEntry>()

                        expensesByCategory.forEach { (category, amount) ->
                            entries.add(PieEntry(amount.toFloat(), category))
                        }

                        PieDataSet(entries, "Expenses by Category").apply {
                            colors = ColorTemplate.MATERIAL_COLORS.toList()
                            pieChart.data = PieData(this)
                            pieChart.invalidate()
                        }
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