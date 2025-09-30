package com.sahil.pulseup.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val values = FloatArray(7)
    private val chartPadding = 40f
    private val pointRadius = 8f
    private val lineWidth = 4f

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = lineWidth
        paint.color = Color.parseColor("#4CAF50")
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }

    fun setValues(newValues: FloatArray) {
        System.arraycopy(newValues, 0, values, 0, minOf(newValues.size, values.size))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (values.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val chartWidth = width - 2 * chartPadding
        val chartHeight = height - 2 * chartPadding

        // Find min and max values
        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 1f
        val valueRange = maxValue - minValue

        if (valueRange == 0f) return

        // Draw grid lines
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f
        for (i in 0..4) {
            val y = chartPadding + (chartHeight / 4) * i
            canvas.drawLine(chartPadding, y, width - chartPadding, y, paint)
        }

        // Draw line chart
        paint.color = Color.parseColor("#4CAF50")
        paint.strokeWidth = lineWidth
        path.reset()

        for (i in values.indices) {
            val x = chartPadding + (chartWidth / (values.size - 1)) * i
            val normalizedValue = (values[i] - minValue) / valueRange
            val y = height - chartPadding - (chartHeight * normalizedValue)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, paint)

        // Draw points
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#4CAF50")
        for (i in values.indices) {
            val x = chartPadding + (chartWidth / (values.size - 1)) * i
            val normalizedValue = (values[i] - minValue) / valueRange
            val y = height - chartPadding - (chartHeight * normalizedValue)
            canvas.drawCircle(x, y, pointRadius, paint)
        }

        // Reset paint style
        paint.style = Paint.Style.STROKE
    }
}