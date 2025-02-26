package io.github.xmlpullkmp

import com.fleeksoft.io.InputStream
import com.fleeksoft.io.Reader
import com.fleeksoft.io.exception.IOException
import io.github.xmlpullkmp.exceptions.XmlPullParserException

interface XmlPullParser {

    companion object {
        const val NO_NAMESPACE = ""
        const val START_DOCUMENT = 0
        const val END_DOCUMENT = 1
        const val START_TAG = 2
        const val END_TAG = 3
        const val TEXT = 4
        const val CDSECT = 5
        const val ENTITY_REF = 6
        const val IGNORABLE_WHITESPACE = 7
        const val PROCESSING_INSTRUCTION = 8
        const val COMMENT = 9
        const val DOCDECL = 10

        val TYPES = arrayOf(
            "START_DOCUMENT",
            "END_DOCUMENT",
            "START_TAG",
            "END_TAG",
            "TEXT",
            "CDSECT",
            "ENTITY_REF",
            "IGNORABLE_WHITESPACE",
            "PROCESSING_INSTRUCTION",
            "COMMENT",
            "DOCDECL"
        )

        const val FEATURE_PROCESS_NAMESPACES = "http://xmlpull.org/v1/doc/features.html#process-namespaces"
        const val FEATURE_REPORT_NAMESPACE_ATTRIBUTES = "http://xmlpull.org/v1/doc/features.html#report-namespace-prefixes"
        const val FEATURE_PROCESS_DOCDECL = "http://xmlpull.org/v1/doc/features.html#process-docdecl"
        const val FEATURE_VALIDATION = "http://xmlpull.org/v1/doc/features.html#validation"
    }

    @Throws(XmlPullParserException::class)
    fun setFeature(name: String, state: Boolean)

    fun getFeature(name: String): Boolean

    @Throws(XmlPullParserException::class)
    fun setProperty(name: String, value: Any)

    fun getProperty(name: String): Any?

    @Throws(XmlPullParserException::class)
    fun setInput(`in`: Reader)

    @Throws(XmlPullParserException::class)
    fun setInput(inputStream: InputStream, inputEncoding: String?)

    fun getInputEncoding(): String?

    @Throws(XmlPullParserException::class)
    fun defineEntityReplacementText(entityName: String, replacementText: String)

    @Throws(XmlPullParserException::class)
    fun getNamespaceCount(depth: Int): Int

    @Throws(XmlPullParserException::class)
    fun getNamespacePrefix(pos: Int): String?

    @Throws(XmlPullParserException::class)
    fun getNamespaceUri(pos: Int): String?

    fun getNamespace(prefix: String?): String?

    fun getDepth(): Int

    fun getPositionDescription(): String

    fun getLineNumber(): Int

    fun getColumnNumber(): Int

    @Throws(XmlPullParserException::class)
    fun isWhitespace(): Boolean

    fun getText(): String?

    fun getTextCharacters(holderForStartAndLength: IntArray): CharArray?

    fun getNamespace(): String?

    fun getName(): String?

    fun getPrefix(): String?

    @Throws(XmlPullParserException::class)
    fun isEmptyElementTag(): Boolean

    fun getAttributeCount(): Int

    fun getAttributeNamespace(index: Int): String?

    fun getAttributeName(index: Int): String?

    fun getAttributePrefix(index: Int): String?

    fun getAttributeType(index: Int): String?

    fun isAttributeDefault(index: Int): Boolean

    fun getAttributeValue(index: Int): String?

    fun getAttributeValue(namespace: String, name: String): String?

    @Throws(XmlPullParserException::class)
    fun getEventType(): Int

    @Throws(XmlPullParserException::class, IOException::class)
    fun next(): Int

    @Throws(XmlPullParserException::class, IOException::class)
    fun nextToken(): Int

    @Throws(XmlPullParserException::class, IOException::class)
    fun require(type: Int, namespace: String?, name: String?)

    @Throws(XmlPullParserException::class, IOException::class)
    fun nextText(): String?

    @Throws(XmlPullParserException::class, IOException::class)
    fun nextTag(): Int
}

