package com.ajt.simplenote.spans

import android.text.style.SubscriptSpan
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class SerializableSubscriptSpan : SubscriptSpan(), Externalizable {

    override fun writeExternal(out: ObjectOutput?) = Unit

    override fun readExternal(`in`: ObjectInput?) = Unit

}