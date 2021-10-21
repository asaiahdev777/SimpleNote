package com.ajt.simplenote.spans

import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableBackgroundColorSpan(private var color : Int = 0) : BackgroundColorSpan(color), Externalizable {

    override fun getBackgroundColor() = color

    override fun writeExternal(out: ObjectOutput?) = out?.writeInt(color) ?: Unit

    override fun readExternal(`in`: ObjectInput?) {
        color = `in`?.readInt() ?: 0
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.bgColor = color
    }
}