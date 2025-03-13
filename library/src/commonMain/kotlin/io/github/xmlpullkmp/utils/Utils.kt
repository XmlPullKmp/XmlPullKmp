package io.github.xmlpullkmp.utils

import io.github.xmlpullkmp.XmlPullParser

fun XmlPullParser.Companion.getTypeOf(type: Int): String {
    return XmlPullParser.TYPES[type]
}