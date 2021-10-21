package com.ajt.simplenote.spans

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.style.ImageSpan
import androidx.annotation.IntRange
import androidx.core.net.toFile
import com.ajt.simplenote.*
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableImageSpan(var uri: Uri = Uri.EMPTY) : ImageSpan(ApplicationSingleton.app, uri), Externalizable {

    private val context get() = ApplicationSingleton.app

    var isVisible = false
    var bitmap: Bitmap? = null

    override fun getSize(paint: Paint, text: CharSequence?,
                         @androidx.annotation.IntRange(from = 0) start: Int, @IntRange(from = 0) end: Int,
                         fm: FontMetricsInt?): Int {
        val d: Drawable = drawable
        val rect = d.bounds
        if (fm != null) {
            fm.ascent = -rect.bottom
            fm.descent = 0
            fm.top = fm.ascent
            fm.bottom = 0
        }
        return rect.right
    }

    override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        this.log("drawing! $bitmap")
        val b: Drawable = drawable
        canvas.save()

        var transY = bottom - b.bounds.bottom
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.fontMetricsInt.descent
        } else if (mVerticalAlignment == ALIGN_CENTER) {
            transY = (bottom - top) / 2 - b.bounds.height() / 2
        }

        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }

    override fun getDrawable(): Drawable {
        val image = if (isVisible) BitmapDrawable(context.resources, bitmap) else context.getDrawable(R.drawable.loading)!!
        image.setBounds(0, 0, bitmap?.width ?: dpToPx(100), bitmap?.height ?: dpToPx(100))
        return image
    }

    fun clearMemory() {
        this.log("cleared from memory!")
        if (bitmap != null) {
            isVisible = false
            bitmap?.recycle()
            bitmap = null
        }
    }

    override fun writeExternal(out: ObjectOutput?) = out?.writeUTF("$uri") ?: Unit

    override fun readExternal(`in`: ObjectInput?) {
        uri = Uri.parse(`in`?.readUTF())
    }

    override fun getSource(): String = uri.toFile().name
}