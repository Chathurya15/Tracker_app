package com.example.mad

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mad.databinding.ActivityOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
// Make sure you have the correct imports for your other classes
import com.example.mad.PrefsHelper // Assuming this is in the same package
import com.example.mad.OnboardingAdapter // Assuming this is in the same package
import com.example.mad.PinSetupDialog // Assuming this is in the same package


class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PrefsHelper(this)

        // Removed the onboarding completion check here, it's now in MainActivity
        // if (prefsHelper.isOnboardingCompleted()) {
        //     startMainActivity()
        //     return
        // }

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(this)
        binding.viewPager.adapter = adapter



        // Add page change callback
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.btnPrevious.isEnabled = position != 0
                binding.btnNext.text = if (position == adapter.itemCount - 1) "Get Started" else "Next"
            }
        })
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem == binding.viewPager.adapter?.itemCount?.minus(1) ?: 0) {
                completeOnboarding()
            } else {
                binding.viewPager.currentItem = binding.viewPager.currentItem + 1
            }
        }

        binding.btnPrevious.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }

        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        prefsHelper.setOnboardingCompleted(true)
        showPinSetupDialog()
    }

    private fun showPinSetupDialog() {
        val dialog = PinSetupDialog { pin ->
            prefsHelper.setAppPin(pin)
            startMainActivity()
        }
        dialog.show(supportFragmentManager, "PinSetupDialog")
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}