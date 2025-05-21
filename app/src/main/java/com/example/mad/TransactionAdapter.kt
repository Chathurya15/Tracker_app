package com.example.mad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mad.TransactionAdapter.TransactionViewHolder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val listener: OnTransactionClickListener,
    private val viewModel: FinanceViewModel
) : ListAdapter<Transaction, TransactionViewHolder>(TransactionDiffCallback()) {

    interface OnTransactionClickListener {
        fun onTransactionClick(transaction: Transaction)
        fun onTransactionLongClick(transaction: Transaction)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentTransaction = getItem(position)
        holder.bind(currentTransaction)
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)

        fun bind(transaction: Transaction) {
            tvTitle.text = transaction.title
            tvCategory.text = transaction.category

            // Format amount using ViewModel's method
            val formattedAmount = viewModel.formatCurrency(transaction.amount)
            tvAmount.text = formattedAmount

            tvAmount.setTextColor(
                if (transaction.type == "income") {
                    itemView.context.getColor(android.R.color.holo_green_dark)
                } else {
                    itemView.context.getColor(android.R.color.holo_red_dark)
                }
            )

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvDate.text = transaction.date?.let { dateFormat.format(it) } ?: "No date"

            itemView.setOnClickListener {
                listener.onTransactionClick(transaction)
            }

            itemView.setOnLongClickListener {
                listener.onTransactionLongClick(transaction)
                true
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}