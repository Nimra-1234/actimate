package com.example.actimate.ui.components

import android.graphics.Typeface
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A completely redesigned Activity Duration Chart with beautiful animations and styling
 * This chart shows data from the APP DATABASE ONLY, not from Google Fit
 */
@Composable
fun HistogramActivityChart(durations: Map<String, Double>) {
    // Map exactly matching the activity labels from the app
    val activityColors = mapOf(
        "downstairs" to Color(0xFFE7CC61),  // Yellow
        "upstairs" to Color(0xFFE79669),     // Orange
        "walking" to Color(0xFF7B8BD9),      // Blue-purple
        "running" to Color(0xFF6BD79D),      // Teal
        "standing" to Color(0xFFE7A4E3)      // Light purple
    )

    // Default color for any activities not in the mapping
    val defaultColor = Color(0xFF8B70D8)  // Default purple

    // Calculate the total duration
    val totalDuration = durations.values.sum().takeIf { it > 0.0 } ?: 1.0

    // Get the max duration for scaling
    val maxDuration = durations.values.maxOrNull() ?: 0.0
    val maxDurationFormatted = max(maxDuration.roundToInt(), 1)

    // Sort durations from highest to lowest for better visualization
    val sortedDurations = durations.entries.sortedByDescending { it.value }

    // Control animation state
    var startAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = durations) {
        startAnimation = false
        // Small delay before starting animation
        kotlinx.coroutines.delay(300)
        startAnimation = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (durations.isEmpty()) {
                // If no data, show centered message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity data available",
                        style = TextStyle(
                            color = Color(0xFF8B70D8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                // Activity bars and labels
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                ) {
                    // Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Activity",
                            style = TextStyle(
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Text(
                            text = "Minutes",
                            style = TextStyle(
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    // Bars for each activity
                    sortedDurations.forEachIndexed { index, (activity, duration) ->
                        // Only show if duration is > 0
                        if (duration > 0) {
                            val animatedWidth = remember { Animatable(0f) }

                            // Start animation when triggered
                            LaunchedEffect(key1 = activity, key2 = duration, key3 = startAnimation) {
                                if (startAnimation) {
                                    animatedWidth.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                } else {
                                    animatedWidth.snapTo(0f)
                                }
                            }

                            ActivityBar(
                                activity = activity,
                                duration = duration,
                                maxDuration = maxDurationFormatted.toDouble(),
                                color = activityColors[activity.lowercase()] ?: defaultColor,
                                animatedProgress = animatedWidth.value,
                                index = index
                            )

                            // Small spacer between bars
                            if (index < sortedDurations.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityBar(
    activity: String,
    duration: Double,
    maxDuration: Double,
    color: Color,
    animatedProgress: Float,
    index: Int
) {
    val formattedDuration = duration.roundToInt()
    val barHeight = 36.dp

    // Apply a staggered delay based on index
    val staggeredDelay = 100 * index
    val delayedProgress by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 500, delayMillis = staggeredDelay)
    )

    // Format activity name nicely
    val formattedActivity = activity.replaceFirstChar { it.uppercase() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity label
            Text(
                text = formattedActivity,
                style = TextStyle(
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.width(100.dp)
            )

            // Bar chart
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .padding(vertical = 8.dp)
            ) {
                // Background (empty) bar
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    drawRoundRect(
                        color = Color(0xFFEEEEEE),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Fill
                    )
                }

                // Filled bar with animation
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(delayedProgress * (duration / maxDuration).toFloat())
                ) {
                    // Create gradient
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.8f),
                            color
                        )
                    )

                    // Draw filled bar with gradient and rounded corners
                    drawRoundRect(
                        brush = gradient,
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Fill
                    )

                    // Add glass effect - subtle highlight at the top
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height / 2
                        ),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Fill
                    )
                }
            }

            // Duration value with animation
            val animatedDuration = (formattedDuration * delayedProgress).roundToInt()

            // Width for the duration value
            Box(
                modifier = Modifier.width(50.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "$animatedDuration min",
                    style = TextStyle(
                        color = color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                )
            }
        }
    }
}