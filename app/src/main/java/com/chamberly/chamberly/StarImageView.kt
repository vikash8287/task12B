package com.chamberly.chamberly

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class StarImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Create a star path with rounded corners
        createRoundedStarPath(path, width, height)

        canvas.save()
        canvas.clipPath(path)
        super.onDraw(canvas)
        canvas.restore()
    }

    private fun createRoundedStarPath(path: Path, width: Float, height: Float) {
        path.reset()

        // Define star points with rounded ends (you can adjust these points)
        val cx = width / 2
        val cy = height / 2
        val outerRadius = width / 2.2f
        val innerRadius = outerRadius / 2.5f
        val numPoints = 5

        for (i in 0 until numPoints * 2) {
            val angle = Math.PI / numPoints * i
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = (cx + Math.cos(angle) * radius).toFloat()
            val y = (cy + Math.sin(angle) * radius).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
    }
}