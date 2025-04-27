package com.example.actimate.ui.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.actimate.R
import com.example.actimate.activity.MainActivity
import com.example.actimate.databinding.OnboardingSettingsBinding

class OnboardingSettings : Fragment() {
    private var _binding: OnboardingSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OnboardingSettingsBinding.inflate(inflater, container, false)

        // Hide views initially for animation
        binding.cardView?.alpha = 0f
        binding.welcomeTitle?.alpha = 0f
        binding.welcomeSubtitle?.alpha = 0f

        setupSaveButton()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply black hole animation after view is created
        animateBlackHoleEffect()
    }

    private fun animateBlackHoleEffect() {
        // First animate welcome title
        animateViewFromBlackHole(binding.welcomeTitle, 0)

        // Then animate welcome subtitle with slight delay
        Handler(Looper.getMainLooper()).postDelayed({
            animateViewFromBlackHole(binding.welcomeSubtitle, 0)
        }, 200)

        // Then animate the card with a bit more delay
        Handler(Looper.getMainLooper()).postDelayed({
            animateViewFromBlackHole(binding.cardView, 0)
        }, 400)
    }

    private fun animateViewFromBlackHole(view: View?, rotationOffset: Int) {
        view?.alpha = 1f

        // Skip animation if view is null
        if (view == null) return

        // Store the original state
        val originalScaleX = view.scaleX
        val originalScaleY = view.scaleY
        val originalRotation = view.rotation

        // Set initial animation state
        view.scaleX = 0f
        view.scaleY = 0f
        view.rotation = 180f + rotationOffset

        // Create animator set for complex animation
        val animatorSet = AnimatorSet()

        // Scale from 0 to original
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, originalScaleX)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, originalScaleY)

        // Rotate for spiral effect
        val rotation = ObjectAnimator.ofFloat(view, View.ROTATION, 180f + rotationOffset, originalRotation)

        // Translate for added effect
        val translateY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -20f, 0f)

        // Play all animations together
        animatorSet.playTogether(scaleX, scaleY, rotation, translateY)

        // Set duration and interpolator
        animatorSet.duration = 800
        animatorSet.interpolator = DecelerateInterpolator(1.5f)

        // Start animation
        animatorSet.start()

        // Optional: add a slight bounce at the end
        Handler(Looper.getMainLooper()).postDelayed({
            val bounceAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, originalScaleX, originalScaleX * 1.05f, originalScaleX)
            val bounceAnimator2 = ObjectAnimator.ofFloat(view, View.SCALE_Y, originalScaleY, originalScaleY * 1.05f, originalScaleY)
            bounceAnimator.duration = 300
            bounceAnimator2.duration = 300
            bounceAnimator.interpolator = OvershootInterpolator()
            bounceAnimator2.interpolator = OvershootInterpolator()
            bounceAnimator.start()
            bounceAnimator2.start()
        }, 800)
    }

    // THE UNCHANGED ORIGINAL FUNCTIONS BELOW

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (validateInput()) {
                hideKeyboard()
                saveUserData()
            }
        }
    }

    private fun validateInput(): Boolean {
        val weight = binding.weightEditText.text.toString()
        val height = binding.heightEditText.text.toString()
        val age = binding.ageEditText.text.toString()

        // Check if any field is empty
        if (weight.isEmpty() || height.isEmpty() || age.isEmpty()) {
            showToast("Please fill in all fields")
            return false
        }

        // Validate weight (reasonable range: 30-300 kg)
        val weightValue = weight.toIntOrNull()
        if (weightValue == null || weightValue < 30 || weightValue > 300) {
            showToast("Please enter a valid weight (30-300 kg)")
            return false
        }

        // Validate height (reasonable range: 100-250 cm)
        val heightValue = height.toIntOrNull()
        if (heightValue == null || heightValue < 100 || heightValue > 250) {
            showToast("Please enter a valid height (100-250 cm)")
            return false
        }

        // Validate age (reasonable range: 13-120 years)
        val ageValue = age.toIntOrNull()
        if (ageValue == null || ageValue < 13 || ageValue > 120) {
            showToast("Please enter a valid age (13-120 years)")
            return false
        }

        // Check if gender is selected
        if (binding.genderRadioGroup.checkedRadioButtonId == -1) {
            showToast("Please select your gender")
            return false
        }

        return true
    }

    private fun hideKeyboard() {
        try {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        } catch (e: Exception) {
            // Log error but continue with saving
        }
    }

    private fun saveUserData() {
        try {
            // Get values (already validated)
            val weight = binding.weightEditText.text.toString().toInt()
            val height = binding.heightEditText.text.toString().toInt()
            val age = binding.ageEditText.text.toString().toInt()
            val gender = when (binding.genderRadioGroup.checkedRadioButtonId) {
                R.id.maleRadioButton -> "Male"
                R.id.femaleRadioButton -> "Female"
                else -> throw IllegalStateException("Gender not selected")
            }

            // Save to SharedPreferences
            requireContext().getSharedPreferences("userdata", Context.MODE_PRIVATE)
                .edit()
                .putInt("weight", weight)
                .putInt("height", height)
                .putInt("age", age)
                .putString("gender", gender)
                .apply()

            showToast("User data saved successfully!")

            // Simple fade out animation before transitioning
            binding.cardView?.let {
                val fadeOut = ObjectAnimator.ofFloat(it, View.ALPHA, 1f, 0f)
                fadeOut.duration = 300
                fadeOut.start()
            }

            // Switch to main content after short delay
            Handler(Looper.getMainLooper()).postDelayed({
                (activity as? MainActivity)?.switchToMainContent()
            }, 500) // Reduced delay to 500ms for better UX

        } catch (e: Exception) {
            showToast("Error saving data. Please try again.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}