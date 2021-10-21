package com.ajt.simplenote.spans

import android.text.style.UnderlineSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableUnderlineSpan : UnderlineSpan(), Externalizable {

    override fun writeExternal(out: ObjectOutput?) = Unit

    override fun readExternal(`in`: ObjectInput?) = Unit

}