package io.github.xmlpullkmp.reader

import com.fleeksoft.io.InputStream

class XmlStreamReader : XmlReader {
    constructor(inputStream: InputStream, lenient: Boolean) : super(inputStream, lenient)
}