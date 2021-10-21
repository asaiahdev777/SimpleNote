package com.ajt.simplenote.spans

import android.text.Layout
import android.text.style.AlignmentSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableAlignSpan(private var align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL) : AlignmentSpan.Standard(align), Externalizable {

    override fun writeExternal(out: ObjectOutput?) = out?.writeObject(align) ?: Unit

    override fun readExternal(`in`: ObjectInput?) {
        align = `in`?.readObject() as Layout.Alignment
    }

    override fun getAlignment() = align

}