package com.example.actimate.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.actimate.R
import com.example.actimate.activity.MainActivity
import com.example.actimate.databinding.SettingsScreenBinding
import com.example.actimate.util.getSharedPreferences
import com.example.actimate.util.setSharedPreferences
import kotlin.math.pow
import kotlin.math.roundToInt

private const val TAG = "Profile"

class Settings : Fragment() {
    private var _binding: SettingsScreenBinding? = null
    private val binding get() = _binding!!

    private val userData = UserSettings()
    private class UserSettings {
        var selectedMode: String = "maxbatterysaving"
        var weight: String = ""
        var height: String = ""
        var age: String = ""
        var gender: String = "Male"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsScreenBinding.inflate(inflater, container, false)
        initializeSettings()
        return binding.root
    }

    private fun initializeSettings() {
        loadSettings()
        setupModeSelection()
        setupUserInformation()
        setupUpdateButton()
        setupStopButton()
        updateBmiChart() // Initialize BMI chart
    }

    private fun loadSettings() {
        requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE).apply {
            userData.selectedMode = getString("workingmode", "maxaccuracy") ?: "maxaccuracy"
        }

        requireContext().getSharedPreferences("userdata", Context.MODE_PRIVATE).apply {
            userData.weight = getInt("weight", -1).takeIf { it != -1 }?.toString() ?: "Not Set"
            userData.height = getInt("height", -1).takeIf { it != -1 }?.toString() ?: "Not Set"
            userData.age = getInt("age", -1).takeIf { it != -1 }?.toString() ?: "Not Set"
            userData.gender = getString("gender", "Male") ?: "Male"
        }
    }

    private fun setupModeSelection() {
        binding.ModeRadioGroup.apply {
            // Check the appropriate button based on the saved mode
            when (userData.selectedMode) {
                "maxaccuracy" -> check(R.id.modeMaxAccuracyRadioButton)
                "maxbatterysaving" -> check(R.id.modesavedModeRadioButton)
            }

            // Add listener for button toggle changes
            addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    userData.selectedMode = when (checkedId) {
                        R.id.modeMaxAccuracyRadioButton -> "maxaccuracy"
                        R.id.modesavedModeRadioButton -> "maxbatterysaving"
                        else -> userData.selectedMode
                    }

                    // Update UI to visually reflect selection
                    val maxAccuracyButton = binding.modeMaxAccuracyRadioButton
                    val batterySavingButton = binding.modesavedModeRadioButton

                    if (checkedId == R.id.modeMaxAccuracyRadioButton) {
                        maxAccuracyButton.setBackgroundColor(resources.getColor(R.color.primary, null))
                        maxAccuracyButton.setTextColor(resources.getColor(R.color.white, null))
                        batterySavingButton.setBackgroundColor(resources.getColor(R.color.primary_container, null))
                        batterySavingButton.setTextColor(resources.getColor(R.color.on_primary_container, null))
                    } else {
                        batterySavingButton.setBackgroundColor(resources.getColor(R.color.primary, null))
                        batterySavingButton.setTextColor(resources.getColor(R.color.white, null))
                        maxAccuracyButton.setBackgroundColor(resources.getColor(R.color.primary_container, null))
                        maxAccuracyButton.setTextColor(resources.getColor(R.color.on_primary_container, null))
                    }
                }
            }
        }
    }

    private fun setupUserInformation() {
        binding.apply {
            weightEditText.apply {
                setText(userData.weight)
                setupTextWatcher("weight")
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        updateBmiChart()
                    }
                })
            }
            heightEditText.apply {
                setText(userData.height)
                setupTextWatcher("height")
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        updateBmiChart()
                    }
                })
            }
            ageEditText.apply {
                setText(userData.age)
                setupTextWatcher("age")
            }

            genderRadioGroup.apply {
                check(if (userData.gender == "Male") R.id.genderMaleRadioButton else R.id.genderFemaleRadioButton)
                setOnCheckedChangeListener { _, checkedId ->
                    userData.gender = when (checkedId) {
                        R.id.genderMaleRadioButton -> "Male"
                        R.id.genderFemaleRadioButton -> "Female"
                        else -> userData.gender
                    }
                    updateUserData("gender", userData.gender)
                }
            }
        }
    }

    private fun EditText.setupTextWatcher(key: String) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateUserData(key, s?.toString() ?: "")
            }
        })
    }

    private fun setupUpdateButton() {
        binding.updateButton.setOnClickListener {
            if (validateUserInput()) {
                saveUserData()

                // Save mode settings and send the broadcast
                requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString("workingmode", userData.selectedMode)
                    .apply()

                val intent = Intent("com.example.actimate.UPDATE_SAMPLING_RATE")
                intent.putExtra("workingMode", userData.selectedMode)
                requireContext().sendBroadcast(intent)

                hideKeyboard()
            }
        }
    }

    private fun setupStopButton() {
        binding.stop.setOnClickListener {
            try {
                val activity = requireActivity() as MainActivity

                // First, stop the UnifiedSensorService
                activity.stopUnifiedSensorService()

                // Set a flag in SharedPreferences to indicate the service should not restart
                requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("service_stopped", true)
                    .apply()

                // Send a broadcast to ensure all components know the service is stopped
                val intent = Intent("com.example.actimate.SERVICE_STOPPED")
                requireContext().sendBroadcast(intent)

                // Show a confirmation toast
                showToast("Movement detection service stopped")

                // Optional: Delay the app finish to allow time for the service to shut down properly
                Handler(Looper.getMainLooper()).postDelayed({
                    activity.finish()
                }, 500) // 500ms delay

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service: ${e.message}")
                showToast("Error stopping service. Please try again.")
            }
        }
    }

    private fun validateUserInput(): Boolean {
        val weight = binding.weightEditText.text.toString()
        val height = binding.heightEditText.text.toString()
        val age = binding.ageEditText.text.toString()

        return when {
            weight.isEmpty() || height.isEmpty() || age.isEmpty() -> {
                showToast("Please fill in all fields")
                false
            }
            !validateWeight(weight.toIntOrNull()) -> false
            !validateHeight(height.toIntOrNull()) -> false
            !validateAge(age.toIntOrNull()) -> false
            binding.genderRadioGroup.checkedRadioButtonId == -1 -> {
                showToast("Please select your gender")
                false
            }
            binding.ModeRadioGroup.checkedButtonId == View.NO_ID -> {
                showToast("Please select mode")
                false
            }
            else -> true
        }
    }

    private fun validateWeight(weight: Int?) = when {
        weight == null || weight < 30 || weight > 300 -> {
            showToast("Please enter a valid weight (30-300 kg)")
            false
        }
        else -> true
    }

    private fun validateHeight(height: Int?) = when {
        height == null || height < 100 || height > 250 -> {
            showToast("Please enter a valid height (100-250 cm)")
            false
        }
        else -> true
    }

    private fun validateAge(age: Int?) = when {
        age == null || age < 13 || age > 120 -> {
            showToast("Please enter a valid age (13-120 years)")
            false
        }
        else -> true
    }

    private fun saveUserData() {
        try {
            requireContext().getSharedPreferences("userdata", Context.MODE_PRIVATE)
                .edit()
                .putInt("weight", binding.weightEditText.text.toString().toInt())
                .putInt("height", binding.heightEditText.text.toString().toInt())
                .putInt("age", binding.ageEditText.text.toString().toInt())
                .putString("gender", userData.gender)
                .apply()

            showToast("Settings updated successfully")

            // Update BMI chart after saving
            updateBmiChart()
        } catch (e: Exception) {
            showToast("Error saving settings. Please try again.")
        }
    }

    private fun updateUserData(key: String, value: String) {
        try {
            // Get current user data
            val userData = getSharedPreferences(requireContext(), "userdata", "user_data_key")?.toMutableMap()
                ?: mutableMapOf()

            // Update the specified key
            userData[key] = value

            // Save back to SharedPreferences
            setSharedPreferences(requireContext(), userData, "userdata", "user_data_key")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user data: ${e.message}")
        }
    }

    private fun updateBmiChart() {
        try {
            val weight = binding.weightEditText.text.toString().toFloatOrNull() ?: return
            val heightCm = binding.heightEditText.text.toString().toFloatOrNull() ?: return

            if (weight < 30 || heightCm < 100) return  // Invalid values

            // Calculate BMI: weight(kg) / height(m)²
            val heightM = heightCm / 100.0
            val bmi = weight / (heightM * heightM)
            val bmiFormatted = (bmi * 10).roundToInt() / 10.0  // Round to 1 decimal place

            // Update BMI value in the chart
            binding.bmiValue.text = bmiFormatted.toString()

            // Determine BMI category and color
            val (category, color) = when {
                bmi < 18.5 -> Pair(BmiCategory.UNDERWEIGHT, "#8B70D8") // Purple
                bmi < 25 -> Pair(BmiCategory.NORMAL, "#4FC3F7") // Blue
                bmi < 30 -> Pair(BmiCategory.OVERWEIGHT, "#81C784") // Green
                else -> Pair(BmiCategory.OBESE, "#FFA726") // Orange
            }

            // Update progress indicator
            val progress = if (bmi >= 40) 100 else ((bmi / 40.0) * 100).toInt()
            binding.bmiDonut.progress = progress
            binding.bmiDonut.setIndicatorColor(Color.parseColor(color))

            // Reset all text colors to default
            binding.underweightText.setTextColor(Color.parseColor("#666666"))
            binding.normalText.setTextColor(Color.parseColor("#666666"))
            binding.overweightText.setTextColor(Color.parseColor("#666666"))
            binding.obeseText.setTextColor(Color.parseColor("#666666"))

            // Highlight the current category
            when (category) {
                BmiCategory.UNDERWEIGHT -> binding.underweightText.setTextColor(Color.parseColor(color))
                BmiCategory.NORMAL -> binding.normalText.setTextColor(Color.parseColor(color))
                BmiCategory.OVERWEIGHT -> binding.overweightText.setTextColor(Color.parseColor(color))
                BmiCategory.OBESE -> binding.obeseText.setTextColor(Color.parseColor(color))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating BMI chart: ${e.message}")
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        } catch (e: Exception) {
            // Keyboard hiding failed silently
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Enum for BMI categories
    private enum class BmiCategory {
        UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE
    }
}