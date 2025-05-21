package com.example.mad

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mad.databinding.DialogTransactionBinding
import com.example.mad.databinding.FragmentTransactionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment(), TransactionAdapter.OnTransactionClickListener {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private val viewModel: FinanceViewModel by viewModels(
        factoryProducer = {
            FinanceViewModelFactory(
                PrefsHelper(requireContext()),
                AppDatabase.getDatabase(requireContext())
            )
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionAdapter(this, viewModel)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionsFragment.adapter
            setHasFixedSize(true)
        }

        setupObservers()
        setupFab()
        setupChipGroup()

        viewModel.currency.observe(viewLifecycleOwner) { currency ->
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupObservers() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            val sortedTransactions = transactions.sortedByDescending { it.date }
            adapter.submitList(sortedTransactions)
            binding.emptyState.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {
            showTransactionDialog(null)
        }
    }

    private fun setupChipGroup() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val currentList = viewModel.transactions.value ?: emptyList()
            val filteredList = when {
                checkedIds.isEmpty() || R.id.chip_all in checkedIds -> currentList
                R.id.chip_income in checkedIds -> currentList.filter { it.type == "income" }
                R.id.chip_expense in checkedIds -> currentList.filter { it.type == "expense" }
                else -> currentList
            }
            adapter.submitList(filteredList.sortedByDescending { it.date })
        }
    } // Added this missing closing brace

    private fun showDeleteConfirmation(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
                showSuccess("Transaction deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onTransactionClick(transaction: Transaction) {
        showTransactionDialog(transaction)
    }

    override fun onTransactionLongClick(transaction: Transaction) {
        showDeleteConfirmation(transaction)
    }

    @SuppressLint("InflateParams")
    private fun showTransactionDialog(transaction: Transaction?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction, null)
        val dialogBinding = DialogTransactionBinding.bind(dialogView)

        // Setup spinners with default "Select" options
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerCategory.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerType.adapter = adapter
        }

        // Set values if editing
        transaction?.let {
            dialogBinding.etTitle.setText(it.title)
            dialogBinding.etAmount.setText(it.amount.toString())
            val categories = resources.getStringArray(R.array.transaction_categories)
            val types = resources.getStringArray(R.array.transaction_types)
            val categoryPos = categories.indexOf(it.category).takeIf { pos -> pos != -1 } ?: 0
            val typePos = types.indexOf(it.type.replaceFirstChar { c -> c.uppercase() }).takeIf { pos -> pos != -1 } ?: 0
            dialogBinding.spinnerCategory.setSelection(categoryPos)
            dialogBinding.spinnerType.setSelection(typePos)
        }

        // Date picker setup
        val calendar = Calendar.getInstance().apply {
            transaction?.date?.let { time = it }
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dialogBinding.etDate.setText(dateFormat.format(calendar.time))
        dialogBinding.etDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dialogBinding.etDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (transaction == null) "Add Transaction" else "Edit Transaction")
            .setView(dialogView)
            .setPositiveButton(if (transaction == null) "Add" else "Save") { _, _ ->
                handleTransactionSave(
                    dialogBinding,
                    calendar.time,
                    transaction?.id ?: UUID.randomUUID().toString()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleTransactionSave(
        binding: DialogTransactionBinding,
        date: Date,
        transactionId: String
    ) {
        val title = binding.etTitle.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val category = binding.spinnerCategory.selectedItem?.toString() ?: ""
        val type = (binding.spinnerType.selectedItem?.toString() ?: "").lowercase(Locale.ROOT)

        when {
            title.isEmpty() -> showError("Please enter a title")
            amount <= 0 -> showError("Please enter a valid amount")
            category == "Select Category" -> showError("Please select a category")
            type == "select type" -> showError("Please select a type")
            else -> {
                val transaction = Transaction(
                    id = transactionId,
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    date = date
                )

                if (viewModel.transactions.value?.any { it.id == transactionId } == true) {
                    viewModel.updateTransaction(transaction)
                    showSuccess("Transaction updated")
                } else {
                    viewModel.addTransaction(transaction)
                    showSuccess("Transaction added")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}