package io.github.xmlpullkmp.exceptions

import com.fleeksoft.io.InputStream


abstract class XmlReaderException(
    val msg: String?,
    val contentTypeMime: String?,
    val contentTypeEncoding: String?,
    val bomEncoding: String?,
    val xmlGuessEncoding: String?,
    val xmlEncoding: String?, inputStream: InputStream?
) : Exception(msg) {
    private val _is: InputStream? = inputStream

    constructor(msg: String?, bomEnc: String?, xmlGuessEnc: String?, xmlEnc: String?, inputStream: InputStream?) : this(msg, null, null, bomEnc, xmlGuessEnc, xmlEnc, inputStream)

    val inputStream: InputStream?
        get() = _is
}
