package com.ajt.simplenote

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.widget.EditText

class LinedBackground(private val editText: EditText) : Drawable() {

    private val paint = Paint().apply {
        strokeWidth = 2F
        isAntiAlias = true
        style = Paint.Style.STROKE
        alpha = (0.05 * 255).toInt()
    }

    override fun draw(canvas: Canvas) {

        val paddingStart = editText.paddingStart.toFloat()
        val paddingEnd = editText.paddingEnd

        val width = bounds.width() - paddingEnd
        val height = bounds.height()

        val lineHeight = editText.lineHeight

        for (i in (lineHeight + editText.paddingTop * 2)..height step lineHeight) {
            val y = i.toFloat() - paddingEnd
            canvas.drawLine(paddingStart, y, width.toFloat(), y, paint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity() = PixelFormat.OPAQUE
}