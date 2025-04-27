package com.example.actimate.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Custom view for the BMI donut chart
 */
class BmiDonutChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects for drawing
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0") // Light gray
        style = Paint.Style.STROKE
        strokeWidth = 30f // Donut thickness
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFA726") // Default: Orange for obese
        style = Paint.Style.STROKE
        strokeWidth = 30f // Donut thickness
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

    // Drawing boundaries
    private val oval = RectF()

    // Progress value (0-100)
    var progress = 0
        set(value) {
            field = value
            invalidate() // Redraw when progress changes
        }

    // Progress color
    var progressColor = Color.parseColor("#FFA726") // Default orange
        set(value) {
            field = value
            progressPaint.color = value
            invalidate() // Redraw when color changes
        }

    // BMI value to display
    var bmiValue = "0.0"
        set(value) {
            field = value
            invalidate() // Redraw when BMI value changes
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate dimensions
        val width = width.toFloat()
        val height = height.toFloat()
        val size = min(width, height)
        val strokeWidth = size / 8f

        // Adjust paint stroke width based on view size
        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth

        // Define the drawing area
        val padding = strokeWidth / 2
        oval.set(padding, padding, width - padding, height - padding)

        // Draw background circle (full 360 degrees)
        canvas.drawArc(oval, 0f, 360f, false, backgroundPaint)

        // Draw progress arc (from 0 to calculated degrees based on progress)
        val sweepAngle = 360f * progress / 100f
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        // Center text (disabled as we will use a separate TextView in the layout)
        // val centerX = width / 2
        // val centerY = height / 2 + textPaint.textSize / 3 // Adjust for text baseline
        // canvas.drawText(bmiValue, centerX, centerY, textPaint)
    }
}