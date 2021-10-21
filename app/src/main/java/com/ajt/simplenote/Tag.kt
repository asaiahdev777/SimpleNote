package com.ajt.simplenote

import java.io.Externalizable

data class Tag<T : Externalizable>(val htmlTag: String, val spanFunction: () -> T)
