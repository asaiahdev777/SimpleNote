package com.ajt.simplenote.spans

import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableForegroundColorSpan(private var color : Int = 0) : ForegroundColorSpan(color), Externalizable {

    override fun getForegroundColor() = color

    override fun writeExternal(out: ObjectOutput?) = out?.writeInt(color) ?: Unit

    override fun readExternal(`in`: ObjectInput?) {
        color = `in`?.readInt() ?: 0
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.color = color
    }
}