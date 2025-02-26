package io.github.xmlpullkmp.reader

import com.fleeksoft.charset.Charsets
import com.fleeksoft.io.*
import com.fleeksoft.io.exception.IOException
import io.github.xmlpullkmp.exceptions.XmlStreamReaderException
import kotlin.jvm.JvmOverloads

@Deprecated("use XmlStreamReader")
open class XmlReader : Reader {
    private var _reader: Reader? = null

    var encoding: String? = null
        private set

    private var _defaultEncoding: String? = null

    @JvmOverloads
    constructor(inputStream: InputStream, lenient: Boolean = true) {
        _defaultEncoding = defaultEncoding
        try {
            doRawStream(inputStream, lenient)
        } catch (ex: XmlStreamReaderException) {
            if (!lenient) {
                throw ex
            } else {
                // doLenientDetection(null, ex)
            }
        }
    }

    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        return _reader?.read(buf, offset, len) ?: throw Exception("Reader is null")
    }

    override fun close() {
        _reader?.close()
    }

    @Throws(IOException::class)
    private fun doRawStream(inputStream: InputStream, lenient: Boolean) {
        val pis = BufferedInputStream(inputStream, BUFFER_SIZE)
        val bomEnc = getBOMEncoding(pis)
        val xmlGuessEnc = getXMLGuessEncoding(pis)
        val xmlEnc = getXmlProlog(pis, xmlGuessEnc)
        val encoding = calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc, pis)
        prepareReader(pis, encoding)
    }

    @Throws(IOException::class)
    private fun prepareReader(inputStream: InputStream, encoding: String) {
        _reader = InputStreamReader(inputStream, encoding)
        this.encoding = encoding
    }

    // InputStream is passed for XmlStreamReaderException creation only
    @Throws(IOException::class)
    private fun calculateRawEncoding(bomEnc: String?, xmlGuessEnc: String?, xmlEnc: String?, inputStream: InputStream): String {
        val encoding: String
        if (bomEnc == null) {
            encoding = if (xmlGuessEnc == null || xmlEnc == null) {
                _defaultEncoding ?: UTF_8
            } else if (xmlEnc == UTF_16 && (xmlGuessEnc == UTF_16BE || xmlGuessEnc == UTF_16LE)) {
                xmlGuessEnc
            } else {
                xmlEnc
            }
        } else if (bomEnc == UTF_8) {
            if (xmlGuessEnc != null && xmlGuessEnc != UTF_8) {
                throw XmlStreamReaderException(
//                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
            if (xmlEnc != null && xmlEnc != UTF_8) {
                throw XmlStreamReaderException(
//                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
            encoding = UTF_8
        } else if (bomEnc == UTF_16BE || bomEnc == UTF_16LE) {
            if (xmlGuessEnc != null && xmlGuessEnc != bomEnc
                || xmlEnc != null && (xmlEnc != UTF_16) && (xmlEnc != bomEnc)
            ) {
                throw XmlStreamReaderException(
//                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
            encoding = bomEnc
        } else {
            throw XmlStreamReaderException(
//                RAW_EX_2.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
            )
        }
        return encoding
    }

    companion object {
        private const val BUFFER_SIZE = 4096

        private const val UTF_8 = "UTF-8"

        private const val US_ASCII = "US-ASCII"

        private const val UTF_16BE = "UTF-16BE"

        private const val UTF_16LE = "UTF-16LE"

        private const val UTF_16 = "UTF-16"

        private const val EBCDIC = "CP1047"

        var defaultEncoding: String? = null

        private fun getContentTypeMime(httpContentType: String?): String? {
            var mime: String? = null
            if (httpContentType != null) {
                val i = httpContentType.indexOf(";")
                mime = (if (i == -1) httpContentType else httpContentType.substring(0, i)).trim { it <= ' ' }
            }
            return mime
        }

        private val CHARSET_PATTERN = Regex("charset=([.[^; ]]*)")

        private fun getContentTypeEncoding(httpContentType: String?): String? {
            var encoding: String? = null
            if (httpContentType != null) {
                val i = httpContentType.indexOf(";")
                if (i > -1) {
                    val postMime = httpContentType.substring(i + 1)
                    val m = CHARSET_PATTERN.matchEntire(postMime)
                    encoding = if (m != null) m.groupValues[1] else null
                    encoding = if (encoding != null) encoding.uppercase() else null
                }
            }
            return encoding
        }

        @Throws(IOException::class)
        private fun getBOMEncoding(inputStream: BufferedInputStream): String? {
            var encoding: String? = null
            val bytes = IntArray(3)
            inputStream.mark(3)
            bytes[0] = inputStream.read()
            bytes[1] = inputStream.read()
            bytes[2] = inputStream.read()

            if (bytes[0] == 0xFE && bytes[1] == 0xFF) {
                encoding = UTF_16BE
                inputStream.reset()
                inputStream.read()
                inputStream.read()
            } else if (bytes[0] == 0xFF && bytes[1] == 0xFE) {
                encoding = UTF_16LE
                inputStream.reset()
                inputStream.read()
                inputStream.read()
            } else if (bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
                encoding = UTF_8
            } else {
                inputStream.reset()
            }
            return encoding
        }

        @Throws(IOException::class)
        private fun getXMLGuessEncoding(inputStream: BufferedInputStream): String? {
            var encoding: String? = null
            val bytes = IntArray(4)
            inputStream.mark(4)
            bytes[0] = inputStream.read()
            bytes[1] = inputStream.read()
            bytes[2] = inputStream.read()
            bytes[3] = inputStream.read()
            inputStream.reset()

            if (bytes[0] == 0x00 && bytes[1] == 0x3C && bytes[2] == 0x00 && bytes[3] == 0x3F) {
                encoding = UTF_16BE
            } else if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x3F && bytes[3] == 0x00) {
                encoding = UTF_16LE
            } else if (bytes[0] == 0x3C && bytes[1] == 0x3F && bytes[2] == 0x78 && bytes[3] == 0x6D) {
                encoding = UTF_8
            } else if (bytes[0] == 0x4C && bytes[1] == 0x6F && bytes[2] == 0xA7 && bytes[3] == 0x94) {
                encoding = EBCDIC
            }
            return encoding
        }

        val ENCODING_PATTERN = Regex("<\\?xml.*encoding\\s*=\\s*(\".[^\"]*\"|'.[^']*')", RegexOption.MULTILINE)

        @Throws(IOException::class)
        private fun getXmlProlog(inputStream: BufferedInputStream, guessedEnc: String?): String? {
            var encoding: String? = null
            if (guessedEnc != null) {
                val bytes = ByteArray(BUFFER_SIZE)
                inputStream.mark(BUFFER_SIZE)
                var offset = 0
                var max = BUFFER_SIZE
                var readBytesLength: Int = inputStream.read(bytes, offset, max)
                var firstGT = -1
                var xmlProlog: String? = null
                while (readBytesLength != -1 && firstGT == -1 && offset < BUFFER_SIZE) {
                    offset += readBytesLength
                    max -= readBytesLength
                    readBytesLength = inputStream.read(bytes, offset, max)
                    xmlProlog = Charsets.forName(guessedEnc).decode(ByteBufferFactory.wrap(bytes.copyOfRange(0, offset))).toString() // decode // FIXME
                    firstGT = xmlProlog.indexOf('>')
                }
                if (firstGT == -1) {
                    if (readBytesLength == -1) {
                        throw IOException("Unexpected end of XML stream")
                    } else {
                        throw IOException("XML prolog or ROOT element not found on first $offset bytes")
                    }
                }
                val bytesRead = offset
                if (bytesRead > 0) {
                    inputStream.reset()
                    val bufferedReader = BufferedReader(StringReader(xmlProlog!!.substring(0, firstGT + 1)))
                    val prolog = StringBuilder()
                    var line: String? = bufferedReader.readLine()
                    while (line != null) {
                        prolog.append(line)
                        line = bufferedReader.readLine()
                    }
                    val matchResult = ENCODING_PATTERN.find(prolog)
                    if (matchResult != null) {
                        encoding = matchResult.groupValues[1].uppercase()
                        encoding = encoding.substring(1, encoding.length - 1)
                    }
                }
            }
            return encoding
        }
    }
}