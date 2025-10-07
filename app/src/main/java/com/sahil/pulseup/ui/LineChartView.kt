package com.sahil.pulseup.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sahil.pulseup.R

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val fillPath = Path()
    private val values = FloatArray(7)
    private val animatedValues = FloatArray(7)
    
    private val chartPadding = 60f
    private val pointRadius = 6f
    private val lineWidth = 3f
    private val gridLineWidth = 1f


    // Colors
    private val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)
    private val gridColor = Color.parseColor("#F0F0F0")
    private val textColor = Color.parseColor("#666666")
    
    // Mood-specific colors
    private val happyColor = Color.parseColor("#4CAF50")      // Green for happy
    private val neutralColor = Color.parseColor("#FF9800")   // Orange for neutral
    private val sadColor = Color.parseColor("#F44336")       // Red for sad
    
    // Gradient colors for area fill
    private val happyGradientStart = Color.parseColor("#E8F5E8")  // Light green
    private val happyGradientEnd = Color.parseColor("#C8E6C9")    // Medium green
    private val neutralGradientStart = Color.parseColor("#FFF3E0") // Light orange
    private val neutralGradientEnd = Color.parseColor("#FFE0B2")  // Medium orange
    private val sadGradientStart = Color.parseColor("#FFEBEE")    // Light red
    private val sadGradientEnd = Color.parseColor("#FFCDD2")     // Medium red
    
    // Animation
    private var animationProgress = 0f
    private var isAnimating = false

    init {
        // Line paint
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = lineWidth
        paint.color = primaryColor
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        
        // Text paint
        textPaint.color = textColor
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun setValues(newValues: FloatArray) {
        System.arraycopy(newValues, 0, values, 0, minOf(newValues.size, values.size))
        startAnimation()
    }
    
    private fun startAnimation() {
        if (isAnimating) return
        
        isAnimating = true
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            animationProgress = animation.animatedValue as Float
            updateAnimatedValues()
            invalidate()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isAnimating = false
                animationProgress = 1f
                updateAnimatedValues()
            }
        })
        animator.start()
    }
    
    private fun updateAnimatedValues() {
        for (i in values.indices) {
            animatedValues[i] = values[i] * animationProgress
        }
    }
    
    private fun getMoodColor(value: Float): Int {
        return when {
            value >= 0.7f -> happyColor      // Happy mood
            value >= 0.3f -> neutralColor    // Neutral mood
            else -> sadColor                 // Sad mood
        }
    }
    
    private fun getMoodGradientColors(value: Float): Pair<Int, Int> {
        return when {
            value >= 0.7f -> Pair(happyGradientStart, happyGradientEnd)
            value >= 0.3f -> Pair(neutralGradientStart, neutralGradientEnd)
            else -> Pair(sadGradientStart, sadGradientEnd)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (values.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val chartWidth = width - 2 * chartPadding
        val chartHeight = height - 2 * chartPadding

        // Find min and max values
        val minValue = 0f
        val maxValue = 1f
        val valueRange = maxValue - minValue

        if (valueRange == 0f) return

        // Draw background
        drawBackground(canvas, width, height)
        
        // Draw grid lines
        drawGridLines(canvas, width, height, chartWidth, chartHeight)
        
        // Draw area fill
        drawAreaFill(canvas, width, height, chartWidth, chartHeight, minValue, valueRange)
        
        // Draw line chart
        drawLineChart(canvas, width, height, chartWidth, chartHeight, minValue, valueRange)
        
        // Draw data points
        drawDataPoints(canvas, width, height, chartWidth, chartHeight, minValue, valueRange)
        
        // Draw labels
        drawLabels(canvas, width, height, chartWidth, chartHeight)
    }
    
    private fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width, height, paint)
    }
    
    private fun drawGridLines(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float) {
        paint.style = Paint.Style.STROKE
        paint.color = gridColor
        paint.strokeWidth = gridLineWidth
        
        // Horizontal grid lines aligned with mood levels
        val moodValues = arrayOf(0.1f, 0.5f, 0.8f)  // Sad, neutral, happy
        for (moodValue in moodValues) {
            val normalizedValue = (moodValue - 0f) / (1f - 0f)
            val y = height - chartPadding - (chartHeight * normalizedValue)
            canvas.drawLine(chartPadding, y, width - chartPadding, y, paint)
        }
        
        // Add top and bottom grid lines
        canvas.drawLine(chartPadding, chartPadding, width - chartPadding, chartPadding, paint)
        canvas.drawLine(chartPadding, height - chartPadding, width - chartPadding, height - chartPadding, paint)
        
        // Vertical grid lines
        for (i in 0..6) {
            val x = chartPadding + (chartWidth / 6) * i
            canvas.drawLine(x, chartPadding, x, height - chartPadding, paint)
        }
    }
    
    private fun drawAreaFill(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float, minValue: Float, valueRange: Float) {
        // Draw gradient fills for each segment based on mood values
        for (i in 0 until animatedValues.size - 1) {
            val x1 = chartPadding + (chartWidth / (animatedValues.size - 1)) * i
            val x2 = chartPadding + (chartWidth / (animatedValues.size - 1)) * (i + 1)
            
            val normalizedValue1 = (animatedValues[i] - minValue) / valueRange
            val normalizedValue2 = (animatedValues[i + 1] - minValue) / valueRange
            val y1 = height - chartPadding - (chartHeight * normalizedValue1)
            val y2 = height - chartPadding - (chartHeight * normalizedValue2)
            
            // Create gradient for this segment
            val gradientColors = getMoodGradientColors((animatedValues[i] + animatedValues[i + 1]) / 2)
            val gradient = LinearGradient(
                x1, height - chartPadding,
                x1, minOf(y1, y2),
                gradientColors.first,
                gradientColors.second,
                android.graphics.Shader.TileMode.CLAMP
            )
            
            // Create path for this segment
            val segmentPath = Path()
            segmentPath.moveTo(x1, height - chartPadding)
            segmentPath.lineTo(x1, y1)
            segmentPath.lineTo(x2, y2)
            segmentPath.lineTo(x2, height - chartPadding)
            segmentPath.close()
            
            paint.style = Paint.Style.FILL
            paint.shader = gradient
            canvas.drawPath(segmentPath, paint)
        }
        
        paint.shader = null // Reset shader
    }
    
    private fun drawLineChart(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float, minValue: Float, valueRange: Float) {
        // Draw line segments with mood-specific colors
        for (i in 0 until animatedValues.size - 1) {
            val x1 = chartPadding + (chartWidth / (animatedValues.size - 1)) * i
            val x2 = chartPadding + (chartWidth / (animatedValues.size - 1)) * (i + 1)
            
            val normalizedValue1 = (animatedValues[i] - minValue) / valueRange
            val normalizedValue2 = (animatedValues[i + 1] - minValue) / valueRange
            val y1 = height - chartPadding - (chartHeight * normalizedValue1)
            val y2 = height - chartPadding - (chartHeight * normalizedValue2)
            
            // Use average mood value to determine color for this segment
            val avgValue = (animatedValues[i] + animatedValues[i + 1]) / 2
            paint.style = Paint.Style.STROKE
            paint.color = getMoodColor(avgValue)
            paint.strokeWidth = lineWidth
            
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }
    
    private fun drawDataPoints(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float, minValue: Float, valueRange: Float) {
        paint.style = Paint.Style.FILL
        
        for (i in animatedValues.indices) {
            val x = chartPadding + (chartWidth / (animatedValues.size - 1)) * i
            val normalizedValue = (animatedValues[i] - minValue) / valueRange
            val y = height - chartPadding - (chartHeight * normalizedValue)
            
            // Draw outer circle (white background with mood-colored border)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, pointRadius + 3, paint)
            
            // Draw mood-colored border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = getMoodColor(animatedValues[i])
            canvas.drawCircle(x, y, pointRadius + 2, paint)
            
            // Draw inner circle (mood color)
            paint.style = Paint.Style.FILL
            paint.color = getMoodColor(animatedValues[i])
            canvas.drawCircle(x, y, pointRadius, paint)
        }
    }
    
    private fun drawLabels(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float) {
        // Y-axis labels (mood levels) - positioned correctly for 0-1 value range
        val moodLabels = arrayOf("😡", "😐", "😊")  // Bottom to top: sad, neutral, happy
        val moodValues = arrayOf(0.1f, 0.5f, 0.8f)  // Corresponding values
        
        for (i in moodLabels.indices) {
            // Calculate Y position based on actual mood value (0-1 range)
            val normalizedValue = (moodValues[i] - 0f) / (1f - 0f)  // Normalize to 0-1
            val y = height - chartPadding - (chartHeight * normalizedValue)
            
            textPaint.textSize = 28f
            textPaint.color = textColor
            canvas.drawText(moodLabels[i], chartPadding - 35f, y + 10f, textPaint)
        }
        
        // X-axis labels (days)
        val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
        for (i in dayLabels.indices) {
            val x = chartPadding + (chartWidth / 6) * i
            textPaint.textSize = 24f
            textPaint.color = textColor
            canvas.drawText(dayLabels[i], x, height - chartPadding + 30f, textPaint)
        }
    }
}