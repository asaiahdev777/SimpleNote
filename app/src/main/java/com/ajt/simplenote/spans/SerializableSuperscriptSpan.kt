package com.ajt.simplenote.spans

import android.text.style.SuperscriptSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableSuperscriptSpan : SuperscriptSpan(), Externalizable {

    override fun writeExternal(out: ObjectOutput?) = Unit

    override fun readExternal(`in`: ObjectInput?) = Unit
}