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
    private val fillColor = Color.parseColor("#E3F2FD")
    
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
        
        // Horizontal grid lines
        for (i in 0..4) {
            val y = chartPadding + (chartHeight / 4) * i
            canvas.drawLine(chartPadding, y, width - chartPadding, y, paint)
        }
        
        // Vertical grid lines
        for (i in 0..6) {
            val x = chartPadding + (chartWidth / 6) * i
            canvas.drawLine(x, chartPadding, x, height - chartPadding, paint)
        }
    }
    
    private fun drawAreaFill(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float, minValue: Float, valueRange: Float) {
        fillPath.reset()
        
        for (i in animatedValues.indices) {
            val x = chartPadding + (chartWidth / (animatedValues.size - 1)) * i
            val normalizedValue = (animatedValues[i] - minValue) / valueRange
            val y = height - chartPadding - (chartHeight * normalizedValue)
            
            if (i == 0) {
                fillPath.moveTo(x, height - chartPadding)
                fillPath.lineTo(x, y)
            } else {
                fillPath.lineTo(x, y)
            }
        }
        
        fillPath.lineTo(width - chartPadding, height - chartPadding)
        fillPath.close()
        
        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawPath(fillPath, paint)
    }
    
    private fun drawLineChart(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float, minValue: Float, valueRange: Float) {
        path.reset()
        
        for (i in animatedValues.indices) {
            val x = chartPadding + (chartWidth / (animatedValues.size - 1)) * i
            val normalizedValue = (animatedValues[i] - minValue) / valueRange
            val y = height - chartPadding - (chartHeight * normalizedValue)
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        paint.style = Paint.Style.STROKE
        paint.color = primaryColor
        paint.strokeWidth = lineWidth
        canvas.drawPath(path, paint)
    }
    
    private fun drawDataPoints(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float, minValue: Float, valueRange: Float) {
        paint.style = Paint.Style.FILL
        
        for (i in animatedValues.indices) {
            val x = chartPadding + (chartWidth / (animatedValues.size - 1)) * i
            val normalizedValue = (animatedValues[i] - minValue) / valueRange
            val y = height - chartPadding - (chartHeight * normalizedValue)
            
            // Draw outer circle (white background)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, pointRadius + 2, paint)
            
            // Draw inner circle (primary color)
            paint.color = primaryColor
            canvas.drawCircle(x, y, pointRadius, paint)
        }
    }
    
    private fun drawLabels(canvas: Canvas, width: Float, height: Float, chartWidth: Float, chartHeight: Float) {
        // Y-axis labels (mood levels)
        val moodLabels = arrayOf("😊", "😐", "😡")
        for (i in moodLabels.indices) {
            val y = chartPadding + (chartHeight / 2) * i + (chartHeight / 4)
            textPaint.textSize = 28f
            canvas.drawText(moodLabels[i], chartPadding - 30f, y + 10f, textPaint)
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