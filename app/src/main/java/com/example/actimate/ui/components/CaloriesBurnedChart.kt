package com.example.actimate.ui.components

import android.graphics.Color
import android.graphics.DashPathEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color as ComposeColor

// Data model to represent activities
data class ActivityData(
    val hour: Int,            // Hour of the day (0-23) or Day of week (0-6)
    val activityType: String, // Type of activity or day of week
    val caloriesBurned: Float // Calories burned during that hour/day
)

/**
 * Redesigned and improved chart for displaying calories data (weekly or hourly)
 */
@Composable
fun CaloriesBurnedChart(activityData: List<ActivityData>) {
    val scope = rememberCoroutineScope()

    // Track animation progress
    val animationProgress = remember { Animatable(0f) }

    // Check if data appears to be daily/weekly (fewer than 24 points, using days of week as activityType)
    val isDailyView = activityData.size <= 7 &&
            activityData.any { it.activityType.length <= 3 } // Day abbreviations like "Mon", "Tue"

    // Reset animation when data changes
    LaunchedEffect(activityData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )
    }

    if (activityData.isEmpty()) {
        // If no data, show message
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No calories data available",
                style = TextStyle(
                    color = ComposeColor(0xFF8B70D8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
        return
    }

    // Format for calories display
    val caloriesFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    // Sort data by hour/day index
    val sortedData = activityData.sortedBy { it.hour }

    // Calculate max calories for scaling
    val maxCalories = sortedData.maxOfOrNull { it.caloriesBurned } ?: 0f

    // Card container
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = ComposeColor.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Heading for the chart
            Text(
                text = if (isDailyView) "Weekly Calories" else "Daily Calories",
                style = TextStyle(
                    color = ComposeColor.DarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Main chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val usableHeight = height * 0.85f  // Leave space for labels
                    val barWidth = width / (sortedData.size * 2f)
                    val spacing = barWidth / 2

                    // Calculate scaling factor for bars
                    val scaleFactor = if (maxCalories > 0) usableHeight / maxCalories else 0f

                    // Draw horizontal guide lines
                    val guideLinesCount = 5
                    val guideLineSpacing = usableHeight / guideLinesCount

                    for (i in 0..guideLinesCount) {
                        val y = height - (i * guideLineSpacing)

                        // Draw dashed guide line
                        val pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#E0E0E0")
                            strokeWidth = 1f
                            style = android.graphics.Paint.Style.STROKE
                            this.pathEffect = pathEffect
                        }

                        drawContext.canvas.nativeCanvas.drawLine(
                            0f, y, width, y, paint
                        )

                        // Draw guide line value
                        val value = ((i * maxCalories) / guideLinesCount).roundToInt()
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#9E9E9E")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            value.toString(),
                            40f,
                            y - 5f,
                            textPaint
                        )
                    }

                    // Draw bars with gradient and animated progress
                    sortedData.forEachIndexed { index, data ->
                        val currentProgress = animationProgress.value
                        val animatedHeight = data.caloriesBurned * scaleFactor * currentProgress

                        // Start position for the bar
                        val startX = (index * (barWidth * 2)) + spacing + 50f // Add offset for labels

                        // Skip zero-height bars
                        if (animatedHeight > 0) {
                            // Create gradient for the bar
                            val barGradient = Brush.verticalGradient(
                                colors = listOf(
                                    ComposeColor(0x808B70D8), // Semi-transparent top
                                    ComposeColor(0xFF8B70D8)  // Solid bottom
                                ),
                                startY = height - animatedHeight,
                                endY = height
                            )

                            // Draw the bar with rounded top
                            val cornerRadius = barWidth / 2

                            // Bar path with rounded top corners
                            val barPath = Path().apply {
                                // Start at bottom left
                                moveTo(startX, height)
                                // Line to bottom right
                                lineTo(startX + barWidth, height)
                                // Line to top right (with small offset for rounded corner)
                                lineTo(startX + barWidth, height - animatedHeight + cornerRadius)
                                // Draw top right rounded corner
                                quadraticBezierTo(
                                    startX + barWidth, height - animatedHeight,
                                    startX + barWidth - cornerRadius, height - animatedHeight
                                )
                                // Line to top left (with small offset for rounded corner)
                                lineTo(startX + cornerRadius, height - animatedHeight)
                                // Draw top left rounded corner
                                quadraticBezierTo(
                                    startX, height - animatedHeight,
                                    startX, height - animatedHeight + cornerRadius
                                )
                                // Close the path
                                close()
                            }

                            // Draw the bar
                            drawPath(
                                path = barPath,
                                brush = barGradient
                            )

                            // Add a subtle highlight at the top of each bar
                            val highlightPath = Path().apply {
                                moveTo(startX, height - animatedHeight + cornerRadius)
                                quadraticBezierTo(
                                    startX, height - animatedHeight,
                                    startX + cornerRadius, height - animatedHeight
                                )
                                lineTo(startX + barWidth - cornerRadius, height - animatedHeight)
                                quadraticBezierTo(
                                    startX + barWidth, height - animatedHeight,
                                    startX + barWidth, height - animatedHeight + cornerRadius
                                )
                            }

                            drawPath(
                                path = highlightPath,
                                color = ComposeColor.White.copy(alpha = 0.5f),
                                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Draw value on top of bar if it's tall enough
                            if (animatedHeight > 40) {
                                val valuePaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 28f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }

                                val valueText = caloriesFormat.format(data.caloriesBurned)
                                drawContext.canvas.nativeCanvas.drawText(
                                    valueText,
                                    startX + barWidth / 2,
                                    height - animatedHeight - 10f,
                                    valuePaint
                                )
                            }

                            // Draw x-axis label
                            val labelPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#757575")
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            val label = if (isDailyView) {
                                data.activityType // Use day abbreviation
                            } else {
                                // Format hour with AM/PM
                                val hour = data.hour
                                if (hour == 0) "12 AM"
                                else if (hour < 12) "${hour} AM"
                                else if (hour == 12) "12 PM"
                                else "${hour - 12} PM"
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                startX + barWidth / 2,
                                height + 30f,
                                labelPaint
                            )
                        }
                    }
                }
            }
        }
    }
}