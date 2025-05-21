package com.example.mad


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(private val context: Context) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    private val onboardingItems = listOf(
        OnboardingItem(
            "Track Your Expenses",
            "Easily record and categorize your daily expenses",
            R.drawable.ic_onboarding_expenses // Replace with your drawable
        ),
        OnboardingItem(
            "Budget Management",
            "Set monthly budgets and get alerts when you're close to limits",
            R.drawable.ic_onboarding_budget // Replace with your drawable
        )
    )

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.imageOnboarding)
        private val title: TextView = view.findViewById(R.id.textTitle)
        private val description: TextView = view.findViewById(R.id.textDescription)

        fun bind(onboardingItem: OnboardingItem) {
            image.setImageResource(onboardingItem.image)
            title.text = onboardingItem.title
            description.text = onboardingItem.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return OnboardingViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_onboarding,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int {
        return onboardingItems.size
    }

    data class OnboardingItem(
        val title: String,
        val description: String,
        val image: Int
    )
}