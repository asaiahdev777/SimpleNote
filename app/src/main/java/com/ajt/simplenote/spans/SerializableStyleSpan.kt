package com.ajt.simplenote.spans

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.StyleSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableStyleSpan(private var styleToUse: Int = 0) : StyleSpan(styleToUse), Externalizable {

    override fun writeExternal(out: ObjectOutput?) = out?.writeInt(styleToUse) ?: Unit

    override fun readExternal(`in`: ObjectInput?) {
        styleToUse = `in`?.readInt() as Int
    }

    override fun getStyle() = styleToUse


    override fun updateDrawState(ds: TextPaint) = apply(ds)

    override fun updateMeasureState(paint: TextPaint) = apply(paint)

    @SuppressLint("WrongConstant")
    private fun apply(paint: Paint) {
        val oldStyle: Int
        val old = paint.typeface
        oldStyle = old?.style ?: 0
        val want = oldStyle or style
        val tf = if (old == null) Typeface.defaultFromStyle(want) else Typeface.create(old, want)

        val fake = want and tf.style.inv()
        if (fake and Typeface.BOLD != 0) paint.isFakeBoldText = true
        if (fake and Typeface.ITALIC != 0) paint.textSkewX = -0.25f
        paint.typeface = tf
    }
}