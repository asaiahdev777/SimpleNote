package com.ajt.simplenote.spans

import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableAbsoluteSizeSpan(private var sizeToUse: Int = 0) : AbsoluteSizeSpan(sizeToUse, true), Externalizable {

    override fun getDip() = true

    override fun getSize() = sizeToUse

    override fun updateDrawState(ds: TextPaint) = performSizeChange(ds)

    override fun updateMeasureState(ds: TextPaint) = performSizeChange(ds)

    private fun performSizeChange(ds: TextPaint) {
        if (dip) {
            ds.textSize = sizeToUse * ds.density
        } else {
            ds.textSize = sizeToUse.toFloat()
        }
    }

    override fun writeExternal(out: ObjectOutput?) = out?.writeInt(sizeToUse) ?: Unit

    override fun readExternal(`in`: ObjectInput?) {
        sizeToUse = `in`?.readInt() ?: 0
    }
}