package com.example.mad

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.mad.AppDatabase
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import java.text.NumberFormat
import java.util.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class FinanceViewModel(private val prefsHelper: PrefsHelper, private val database: AppDatabase) : ViewModel() {

    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CAD" to "CA$",
        "AUD" to "A$",
        "LKR" to "Rs."
    )

    private val transactionDao = database.transactionDao()

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()

    private val _monthlyBudget = MutableLiveData<Double>()
    val monthlyBudget: LiveData<Double> = _monthlyBudget

    private val _currency = MutableLiveData<String>()
    val currency: LiveData<String> = _currency

    private lateinit var notificationHelper: NotificationHelper

    fun setNotificationHelper(helper: NotificationHelper) {
        notificationHelper = helper
    }

    init {
        loadBudget()
        loadCurrency()
    }

    private fun loadBudget() {
        _monthlyBudget.value = prefsHelper.getMonthlyBudget()
    }

    private fun loadCurrency() {
        _currency.value = prefsHelper.getCurrency()
    }

    fun replaceAllTransactions(newTransactions: List<Transaction>) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteAllTransactions()
            newTransactions.forEach { transactionDao.insert(it) }
        }
    }
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.insert(transaction)
            checkBudgetThreshold()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.update(transaction)
            checkBudgetThreshold()
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteById(transactionId)
            checkBudgetThreshold()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun setMonthlyBudget(budget: Double) {
        prefsHelper.setMonthlyBudget(budget)
        _monthlyBudget.value = budget
        checkBudgetThreshold()
    }

    fun getExpensesByCategory(): LiveData<Map<String, Double>> {
        return transactionDao.getAllExpenses().map { transactions ->
            transactions.groupBy { it.category }
                .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
        }
    }

    fun getTotalExpenses(): LiveData<Double> {
        return transactionDao.getAllExpenses().map { transactions ->
            transactions.sumOf { it.amount }
        }
    }

    fun getTotalIncome(): LiveData<Double> {
        return transactionDao.getAllIncomes().map { transactions ->
            transactions.sumOf { it.amount }
        }
    }

    fun getCurrentMonthExpenses(): LiveData<Double> {
        val calendar = Calendar.getInstance()
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val year = calendar.get(Calendar.YEAR).toString()

        return transactionDao.getMonthlyExpenses(month, year).map { transactions ->
            transactions.sumOf { it.amount }
        }
    }
    fun setCurrency(currency: String) {
        prefsHelper.setCurrency(currency)
        _currency.value = currency
    }

    fun formatCurrency(amount: Double): String {
        return try {
            val currencyCode = _currency.value ?: "USD"
            val locale = when (currencyCode) {
                "USD" -> Locale.US
                "EUR" -> Locale.GERMANY
                "GBP" -> Locale.UK
                "JPY" -> Locale.JAPAN
                "CAD" -> Locale.CANADA
                "AUD" -> Locale("en", "AU")
                "LKR" -> Locale("en", "LK")
                else -> Locale.getDefault()
            }
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = Currency.getInstance(currencyCode)
            format.format(amount)
        } catch (e: Exception) {
            NumberFormat.getCurrencyInstance().format(amount)
        }
    }




    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun checkBudgetThreshold() {
        viewModelScope.launch(Dispatchers.IO) {
            val budget = _monthlyBudget.value ?: 0.0
            if (budget <= 0) return@launch

            val currentSpending = getCurrentMonthExpenses().value ?: 0.0
            val percentage = (currentSpending / budget * 100).toInt()

            if (percentage >= 75 && ::notificationHelper.isInitialized) {
                when {
                    percentage >= 100 -> notificationHelper.showBudgetWarning(percentage)
                    percentage >= 90 -> notificationHelper.showBudgetWarning(percentage)
                    percentage >= 75 -> notificationHelper.showBudgetWarning(percentage)
                }
            }
        }
    }
}