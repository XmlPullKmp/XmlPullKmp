package io.github.xmlpullkmp.utils

import com.fleeksoft.io.InputStream

class CombinedInputStream(
    private val first: InputStream,
    private val second: InputStream
) : InputStream() {

    override fun read(): Int {
        val result = first.read()
        return if (result != -1) result else second.read()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val bytesRead = first.read(b, off, len)
        return if (bytesRead != -1) bytesRead else second.read(b, off, len)
    }

    override fun available(): Int {
        return first.available() + second.available()
    }

    override fun close() {
        first.close()
        second.close()
    }
}
