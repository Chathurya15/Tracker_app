package com.example.mad

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface TransactionDao {




    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)


    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): Transaction?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM transactions WHERE type = 'expense'")
    fun getAllExpenses(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'income'")
    fun getAllIncomes(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'expense' AND strftime('%m', date/1000, 'unixepoch') = :month AND strftime('%Y', date/1000, 'unixepoch') = :year")
    fun getMonthlyExpenses(month: String, year: String): LiveData<List<Transaction>>
}
