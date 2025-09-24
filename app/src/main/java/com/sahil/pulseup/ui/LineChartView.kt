package com.sahil.pulseup.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sahil.pulseup.R

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.darker_gray)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        style = Paint.Style.FILL
    }

    // Simple demo data to mimic the screenshot trend
    private val values = floatArrayOf(0.7f, 0.5f, 0.55f, 0.4f, 0.3f, 0.55f, 0.8f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = paddingLeft.toFloat()
        val paddingTop = paddingTop.toFloat()
        val paddingRight = paddingRight.toFloat()
        val paddingBottom = paddingBottom.toFloat()

        val width = width.toFloat() - paddingLeft - paddingRight
        val height = height.toFloat() - paddingTop - paddingBottom

        val chartLeft = paddingLeft
        val chartTop = paddingTop
        val chartRight = paddingLeft + width
        val chartBottom = paddingTop + height

        // Baseline grid line
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        if (values.isEmpty()) return

        val stepX = width / (values.size - 1).coerceAtLeast(1)

        val path = Path()
        values.forEachIndexed { index, v ->
            val x = chartLeft + stepX * index
            val y = chartTop + (1f - v.coerceIn(0f, 1f)) * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        canvas.drawPath(path, linePaint)

        // Last point emphasis
        val lastX = chartLeft + stepX * (values.size - 1)
        val lastY = chartTop + (1f - values.last().coerceIn(0f, 1f)) * height
        canvas.drawCircle(lastX, lastY, 8f, pointPaint)
    }
}


