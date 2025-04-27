package com.example.actimate.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.actimate.R
import com.example.actimate.databinding.DataScreenBinding
import com.example.actimate.viewmodel.LastPredictionViewModel
import com.example.actimate.viewmodel.Prediction
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DataScreen"

class DataScreen : Fragment() {

    private var _binding: DataScreenBinding? = null
    private val binding get() = _binding!!

    private val lastPredictionViewModel: LastPredictionViewModel by activityViewModels()

    // Date and time formatter for consistent formatting
    private val dateTimeFormatter = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())

    // Adapter for the RecyclerView
    private lateinit var activitiesAdapter: ActivityListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DataScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        // Set initial state for the UI
        binding.predictionData.text = "No activity"

        // Initialize the adapter
        activitiesAdapter = ActivityListAdapter()
        binding.recentActivitiesListView.layoutManager = LinearLayoutManager(context)
        binding.recentActivitiesListView.adapter = activitiesAdapter

        // Set up observers
        setupObservers()
    }

    private fun setupObservers() {
        Log.d(TAG, "Setting up observers")

        // Observe the most recent prediction
        lastPredictionViewModel.lastPredictionData.observe(viewLifecycleOwner) { prediction ->
            Log.d(TAG, "Prediction updated: $prediction")

            if (prediction != null) {
                // Convert to title case
                binding.predictionData.text = toTitleCase(prediction)

                // Update the current activity icon based on the activity type
                binding.activityIcon.setImageResource(getActivityIconResource(prediction))

                // Add pulse animation to the activity wave
                pulseActivityWave()
            } else {
                binding.predictionData.text = "No Activity"
            }
        }

        // Observe the list of recent predictions
        lastPredictionViewModel.recentPredictions.observe(viewLifecycleOwner) { predictions ->
            Log.d(TAG, "Recent predictions updated: ${predictions?.size ?: 0} items")

            // Safely handle the predictions list
            if (predictions != null && predictions.isNotEmpty()) {
                // Hide the "no activities" message
                binding.noActivitiesText.visibility = View.GONE
                binding.recentActivitiesListView.visibility = View.VISIBLE

                // Update the adapter data
                activitiesAdapter.updatePredictions(predictions)
            } else {
                // Show "No activities" message
                Log.d(TAG, "No activities to display")
                binding.noActivitiesText.visibility = View.VISIBLE
                binding.recentActivitiesListView.visibility = View.GONE
                // Clear the adapter data
                activitiesAdapter.updatePredictions(emptyList())
            }
        }
    }

    /**
     * Converts a string to Title Case
     */
    private fun toTitleCase(text: String): String {
        if (text.isBlank()) return "Unknown"

        return text.lowercase(Locale.ROOT)
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
    }

    /**
     * Adds a pulse animation to the activity wave
     */
    private fun pulseActivityWave() {
        binding.activityWave.apply {
            // Reset any ongoing animations
            clearAnimation()
            alpha = 0.9f

            // Create pulse animation
            animate()
                .alpha(1f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(800)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .alpha(0.9f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(800)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
                .start()
        }
    }

    /**
     * Returns the appropriate icon resource based on activity type
     */
    private fun getActivityIconResource(activity: String): Int {
        return when (activity.lowercase(Locale.ROOT)) {
            "walking" -> R.drawable.ic_walking
            "running" -> R.drawable.ic_running
            "upstairs" -> R.drawable.ic_stairs_up
            "downstairs" -> R.drawable.ic_stairs_down
            "standing" -> R.drawable.ic_standing
            else -> R.drawable.ic_activity_default
        }
    }

    /**
     * Custom adapter for the activity list
     */
    inner class ActivityListAdapter :
        androidx.recyclerview.widget.RecyclerView.Adapter<ActivityListAdapter.ViewHolder>() {

        private val items = mutableListOf<Prediction>()

        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val timeIcon: ImageView = itemView.findViewById(R.id.timeIcon)
            val dateTimeText: TextView = itemView.findViewById(R.id.activityDateTime)
            val activityText: TextView = itemView.findViewById(R.id.activityName)
            val activityIcon: ImageView = itemView.findViewById(R.id.activityIcon)
        }

        fun updatePredictions(predictions: List<Prediction>) {
            items.clear()
            items.addAll(predictions)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.activity_item, parent, false)
            return ViewHolder(itemView)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            try {
                // Safely get the prediction at this position
                if (position < items.size) {
                    val prediction = items[position]

                    // Format date and time
                    try {
                        val timestamp = prediction.timestamp.toLongOrNull() ?: 0L
                        val date = Date(timestamp)
                        holder.dateTimeText.text = dateTimeFormatter.format(date)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error formatting timestamp: ${e.message}")
                        holder.dateTimeText.text = "Unknown time"
                    }

                    // Set activity name with title case
                    holder.activityText.text = toTitleCase(prediction.label)

                    // Set the activity icon based on the activity type
                    holder.activityIcon.setImageResource(getActivityIconResource(prediction.label))
                } else {
                    Log.e(TAG, "Invalid position: $position for list size: ${items.size}")
                    // Set default values
                    holder.dateTimeText.text = "Unknown time"
                    holder.activityText.text = "Unknown activity"
                    holder.activityIcon.setImageResource(R.drawable.ic_activity_default)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding view: ${e.message}")
                // Set default values in case of error
                holder.dateTimeText.text = "Unknown time"
                holder.activityText.text = "Unknown activity"
                holder.activityIcon.setImageResource(R.drawable.ic_activity_default)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}