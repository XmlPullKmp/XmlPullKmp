package io.github.xmlpullkmp

import com.fleeksoft.io.InputStream
import com.fleeksoft.io.InputStreamReader
import com.fleeksoft.io.Reader
import com.fleeksoft.io.exception.EOFException
import com.fleeksoft.io.exception.IOException
import com.fleeksoft.lang.Character
import io.github.xmlpullkmp.codePoints.CodePoints
import io.github.xmlpullkmp.codePoints.CodePoints.highSurrogate
import io.github.xmlpullkmp.codePoints.CodePoints.isBmpCodePoint
import io.github.xmlpullkmp.codePoints.CodePoints.lowSurrogate
import io.github.xmlpullkmp.exceptions.XmlPullParserException
import io.github.xmlpullkmp.exceptions.XmlStreamReaderException
import io.github.xmlpullkmp.reader.XmlStreamReader
import io.github.xmlpullkmp.utils.codePointAt
import io.github.xmlpullkmp.utils.codePointCount
import kotlin.math.min

class XmlPullParserKmp : XmlPullParser {

    private val allStringsInterned = false

    private fun resetStringCache() {
    }

    private fun newString(cbuf: CharArray, off: Int, len: Int): String = cbuf.concatToString(off, off + len)

    private var processNamespaces = false

    private var roundtripSupported = false

    private var location: String? = null

    private var lineNumber: Int = 0

    override fun getLineNumber(): Int = lineNumber

    private var columnNumber: Int = 0

    override fun getColumnNumber() = columnNumber

    private var seenRoot = false

    private var reachedEnd = false

    private var eventType: Int = 0

    override fun getEventType(): Int {
        return eventType
    }

    private var emptyElementTag = false

    private var depth: Int = 0

    override fun getDepth(): Int = depth

    private var elRawName: Array<CharArray?> = arrayOf()

    private var elRawNameEnd: IntArray = intArrayOf()

    private var elRawNameLine: IntArray = intArrayOf()

    private var elName: Array<String?> = arrayOf()

    private var elPrefix: Array<String?> = arrayOf()

    private var elUri: Array<String?> = arrayOf()

    private var elNamespaceCount: IntArray = intArrayOf()

    private fun ensureElementsCapacity() {
        if (depth + 1 >= elName.size) {
            val newSize = (if (depth >= 7) 2 * depth else 8) + 2
            if (TRACE_SIZING) println("TRACE_SIZING elStackSize ${elName.size} ==> $newSize")

            elName = elName.copyOf(newSize)
            elPrefix = elPrefix.copyOf(newSize)
            elUri = elUri.copyOf(newSize)

            elNamespaceCount = elNamespaceCount.copyOf(newSize).apply { if (isEmpty()) this[0] = 0 }
            elRawNameEnd = elRawNameEnd.copyOf(newSize)
            elRawNameLine = elRawNameLine.copyOf(newSize)
            elRawName = elRawName.copyOf(newSize)
        }
    }

    private var attributeCount = 0

    private var attributeName: Array<String> = arrayOf()

    private lateinit var attributeNameHash: IntArray

    private lateinit var attributePrefix: Array<String?>

    private lateinit var attributeUri: Array<String>

    private lateinit var attributeValue: Array<String>

    private fun ensureAttributesCapacity(size: Int) {
        val attrPosSize = attributeName.size

        if (size >= attrPosSize) {
            val newSize = if (size > 7) 2 * size else 8
            if (TRACE_SIZING) println("TRACE_SIZING attrPosSize $attrPosSize ==> $newSize")
            val needsCopying = attrPosSize > 0

            attributeName = Array(newSize) { "" }.apply {
                if (needsCopying) attributeName.copyInto(this, 0, 0, attrPosSize)
            }

            attributePrefix = Array(newSize) { "" as String? }.apply {
                if (needsCopying) attributePrefix.copyInto(this, 0, 0, attrPosSize)
            }

            attributeUri = Array(newSize) { "" }.apply {
                if (needsCopying) attributeUri.copyInto(this, 0, 0, attrPosSize)
            }

            attributeValue = Array(newSize) { "" }.apply {
                if (needsCopying) attributeValue.copyInto(this, 0, 0, attrPosSize)
            }

            if (!allStringsInterned) {
                attributeNameHash = IntArray(newSize).apply {
                    if (needsCopying) attributeNameHash.copyInto(this, 0, 0, attrPosSize)
                }
            }
        }
    }

    private var namespaceEnd = 0

    private var namespacePrefix: Array<String> = arrayOf()

    private var namespacePrefixHash: IntArray = intArrayOf()

    private lateinit var namespaceUri: Array<String>

    private fun ensureNamespacesCapacity(size: Int) {
        val namespaceSize = namespacePrefix.size
        if (size >= namespaceSize) {
            val newSize = if (size > 7) 2 * size else 8
            if (TRACE_SIZING) println("TRACE_SIZING namespaceSize $namespaceSize ==> $newSize")

            namespacePrefix = Array(newSize) { "" }.apply {
                namespacePrefix.copyInto(this, 0, 0, namespaceEnd)
            }
            namespaceUri = Array(newSize) { "" }.apply {
                namespaceUri.copyInto(this, 0, 0, namespaceEnd)
            }

            if (!allStringsInterned) {
                namespacePrefixHash = IntArray(newSize).apply {
                    namespacePrefixHash.copyInto(this, 0, 0, namespaceEnd)
                }
            }
        }
    }

    private var entityEnd = 0

    private var entityName: Array<String> = arrayOf()

    private var entityNameBuf: Array<CharArray> = arrayOf()

    private var entityReplacement: Array<String> = arrayOf()

    private var entityReplacementBuf: Array<CharArray> = arrayOf()

    private var entityNameHash: IntArray = intArrayOf()

    private val replacementMapTemplate: EntityReplacementMap?

    private fun ensureEntityCapacity() {
        val entitySize = entityReplacementBuf.size
        if (entityEnd >= entitySize) {
            val newSize = if (entityEnd > 7) 2 * entityEnd else 8

            if (TRACE_SIZING) {
                println("TRACE_SIZING entitySize $entitySize ==> $newSize")
            }

            entityName = Array(newSize) { "" }.apply {
                entityName.copyInto(this, 0, 0, entityEnd)
            }

            entityNameBuf = Array(newSize) { charArrayOf() }.apply {
                entityNameBuf.copyInto(this, 0, 0, entityEnd)
            }

            entityReplacement = Array(newSize) { "" }.apply {
                entityReplacement.copyInto(this, 0, 0, entityEnd)
            }

            entityReplacementBuf = Array(newSize) { charArrayOf() }.apply {
                entityReplacementBuf.copyInto(this, 0, 0, entityEnd)
            }

            if (!allStringsInterned) {
                entityNameHash = IntArray(newSize).apply {
                    entityNameHash.copyInto(this, 0, 0, entityEnd)
                }
            }
        }
    }

    private var reader: Reader? = null

    private var inputEncoding: String? = null

    private val bufLoadFactor = 95
    private val bufferLoadFactor = bufLoadFactor / 100f

    private var buf = CharArray(256)
    private var bufSoftLimit = (bufferLoadFactor * buf.size).toInt()
    private var preventBufferCompaction = false

    private var bufAbsoluteStart = 0
    private var bufStart = 0

    private var bufEnd = 0

    private var pos = 0

    private var posStart = 0

    private var posEnd = 0

    private var pc = CharArray(64)
    private var pcStart = 0

    private var pcEnd = 0

    private var usePC = false

    private var seenStartTag = false

    private var seenEndTag = false

    private var pastEndTag = false

    private var seenAmpersand = false

    private var seenMarkup = false

    private var seenDocdecl = false

    private var tokenize = false

    private var text: String? = null

    private var entityRefName: String? = null

    private var xmlDeclVersion: String? = null

    private var xmlDeclStandalone: Boolean? = null

    private var xmlDeclContent: String? = null

    private fun reset() {
        location = null
        lineNumber = 1
        columnNumber = 1
        seenRoot = false
        reachedEnd = false
        eventType = XmlPullParser.START_DOCUMENT
        emptyElementTag = false

        depth = 0

        attributeCount = 0

        namespaceEnd = 0

        entityEnd = 0
        setupFromTemplate()

        reader = null
        inputEncoding = null

        preventBufferCompaction = false
        bufAbsoluteStart = 0
        bufStart = 0
        bufEnd = bufStart
        posEnd = 0
        posStart = posEnd
        pos = posStart

        pcStart = 0
        pcEnd = pcStart

        usePC = false

        seenStartTag = false
        seenEndTag = false
        pastEndTag = false
        seenAmpersand = false
        seenMarkup = false
        seenDocdecl = false

        xmlDeclVersion = null
        xmlDeclStandalone = null
        xmlDeclContent = null

        resetStringCache()
    }

    constructor() {
        replacementMapTemplate = EntityReplacementMap.defaultEntityReplacementMap
    }

    constructor(entityReplacementMap: EntityReplacementMap?) {
        this.replacementMapTemplate = entityReplacementMap
    }

    fun setupFromTemplate() {
        if (replacementMapTemplate != null) {
            val length: Int = replacementMapTemplate.entityEnd

            entityName = replacementMapTemplate.entityName.mapNotNull { it }.toTypedArray()
            entityNameBuf = replacementMapTemplate.entityNameBuf.mapNotNull { it }.toTypedArray()
            entityReplacement = replacementMapTemplate.entityReplacement.mapNotNull { it }.toTypedArray()
            entityReplacementBuf = replacementMapTemplate.entityReplacementBuf.mapNotNull { it }.toTypedArray()
            entityNameHash = replacementMapTemplate.entityNameHash
            entityEnd = length
        }
    }

    @Throws(XmlPullParserException::class)
    override fun setFeature(name: String, state: Boolean) {
        when (name) {
            XmlPullParser.FEATURE_PROCESS_NAMESPACES -> {
                if (eventType != XmlPullParser.START_DOCUMENT) {
                    throw XmlPullParserException("namespace processing feature can only be changed before parsing", this, null)
                }
                processNamespaces = state
            }
            FEATURE_NAMES_INTERNED                   -> {
                if (state) {
                    throw XmlPullParserException("interning names in this implementation is not supported")
                }
            }
            XmlPullParser.FEATURE_PROCESS_DOCDECL    -> {
                if (state) {
                    throw XmlPullParserException("processing DOCDECL is not supported")
                }
            }
            FEATURE_XML_ROUNDTRIP                    -> roundtripSupported = state
            else                                     -> throw XmlPullParserException("unsupported feature $name")
        }
    }

    override fun getFeature(name: String): Boolean = when (name) {
        XmlPullParser.FEATURE_PROCESS_NAMESPACES -> processNamespaces
        FEATURE_NAMES_INTERNED                   -> false
        XmlPullParser.FEATURE_PROCESS_DOCDECL    -> false
        FEATURE_XML_ROUNDTRIP                    -> roundtripSupported
        else                                     -> false
    }

    @Throws(XmlPullParserException::class)
    override fun setProperty(name: String, value: Any) {
        if (PROPERTY_LOCATION == name) {
            location = value as String
        } else {
            throw XmlPullParserException("unsupported property: '$name'")
        }
    }

    override fun getProperty(name: String): Any? = when (name) {
        PROPERTY_XMLDECL_VERSION    -> xmlDeclVersion
        PROPERTY_XMLDECL_STANDALONE -> xmlDeclStandalone
        PROPERTY_XMLDECL_CONTENT    -> xmlDeclContent
        PROPERTY_LOCATION           -> location
        else                        -> null
    }

    @Throws(XmlPullParserException::class)
    override fun setInput(input: Reader) {
        reset()
        reader = input
    }

    @Throws(XmlPullParserException::class)
    override fun setInput(inputStream: InputStream, inputEncoding: String?) {
        val reader: Reader

        try {
            reader = if (inputEncoding != null) {
                InputStreamReader(inputStream, inputEncoding)
            } else {
                XmlStreamReader(inputStream, false)
            }
        } catch (e: XmlStreamReaderException) {
            throw XmlPullParserException("could not create reader : $e", this, e)
        } catch (une: Exception) {
            throw XmlPullParserException("could not create reader for encoding $inputEncoding : $une", this, une)
        } catch (e: IOException) {
            throw XmlPullParserException("could not create reader : $e", this, e)
        }

        setInput(reader)

        this.inputEncoding = inputEncoding
    }

    override fun getInputEncoding(): String {
        return inputEncoding!!
    }

    @Throws(XmlPullParserException::class)
    override fun defineEntityReplacementText(entityName: String, replacementText: String) {
        var replacement = replacementText

        if (!replacement.startsWith("&#") && replacement.length > 1) {
            val tmp = replacement.substring(1, replacement.length - 1)

            for (i in this.entityName.indices) {
                if (this.entityName[i] == tmp) {
                    replacement = entityReplacement[i]
                }
            }
        }

        ensureEntityCapacity()

        val entityNameCharData = entityName.toCharArray()
        this.entityName[entityEnd] = this.newString(entityNameCharData, 0, entityName.length)
        entityNameBuf[entityEnd] = entityNameCharData

        entityReplacement[entityEnd] = replacement
        entityReplacementBuf[entityEnd] = replacement.toCharArray()

        if (!allStringsInterned) {
            entityNameHash[entityEnd] = fastHash(entityNameBuf[entityEnd], 0, entityNameBuf[entityEnd].size)
        }

        ++entityEnd
    }

    @Throws(XmlPullParserException::class)
    override fun getNamespaceCount(depth: Int): Int {
        if (!processNamespaces || depth == 0) {
            return 0
        }

        require(!(depth < 0 || depth > this.depth)) { "namespace count may be for depth 0.." + this.depth + " not " + depth }

        return elNamespaceCount[depth]
    }

    @Throws(XmlPullParserException::class)
    override fun getNamespacePrefix(pos: Int): String {

        if (pos < namespaceEnd) {
            return namespacePrefix[pos]
        } else {
            throw XmlPullParserException(
                "position $pos exceeded number of available namespaces $namespaceEnd"
            )
        }
    }

    @Throws(XmlPullParserException::class)
    override fun getNamespaceUri(pos: Int): String {
        if (pos < namespaceEnd) {
            return namespaceUri[pos]
        } else {
            throw XmlPullParserException(
                "position $pos exceeded number of available namespaces $namespaceEnd"
            )
        }
    }

    override fun getNamespace(prefix: String?): String? {
        if (prefix != null) {
            for (i in namespaceEnd - 1 downTo 0) {
                if (prefix == namespacePrefix[i]) {
                    return namespaceUri[i]
                }
            }

            if ("xml" == prefix) {
                return XML_URI
            } else if ("xmlns" == prefix) {
                return XMLNS_URI
            }
        }

        return null
    }

    override fun getPositionDescription(): String {
        var fragment: String? = null

        if (posStart <= pos) {
            val start = findFragment(0, buf, posStart, pos)
            if (start < pos) {
                fragment = buf.concatToString(start, pos)
            }
            if (bufAbsoluteStart > 0 || start > 0) fragment = "...$fragment"
        }

        val locationInfo = location ?: ""

        return " ${XmlPullParser.TYPES[eventType]}${if (fragment != null) " seen ${printable(fragment)}..." else ""} $locationInfo@$lineNumber:$columnNumber"
    }

    override fun isWhitespace(): Boolean {
        if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.CDSECT) {
            val range = if (usePC) pcStart until pcEnd else posStart until posEnd
            val buffer = if (usePC) pc else buf
            return range.all { isS(buffer[it]) }
        }
        if (eventType == XmlPullParser.IGNORABLE_WHITESPACE) return true
        throw XmlPullParserException("no content available to check for whitespaces")
    }

    override fun getText(): String? {
        if (eventType == XmlPullParser.START_DOCUMENT || eventType == XmlPullParser.END_DOCUMENT) {
            return null
        }
        if (eventType == XmlPullParser.ENTITY_REF) {
            return text
        }
        if (text == null) {
            text = if (!usePC || eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
                buf.concatToString(posStart, posEnd)
            } else {
                pc.concatToString(pcStart, pcEnd)
            }
        }
        return text
    }

    override fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? {
        if (eventType == XmlPullParser.TEXT) {
            if (usePC) {
                holderForStartAndLength[0] = pcStart
                holderForStartAndLength[1] = pcEnd - pcStart
                return pc
            } else {
                holderForStartAndLength[0] = posStart
                holderForStartAndLength[1] = posEnd - posStart
                return buf
            }
        } else if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.CDSECT || eventType == XmlPullParser.COMMENT || eventType == XmlPullParser.ENTITY_REF || eventType == XmlPullParser.PROCESSING_INSTRUCTION || eventType == XmlPullParser.IGNORABLE_WHITESPACE || eventType == XmlPullParser.DOCDECL) {
            holderForStartAndLength[0] = posStart
            holderForStartAndLength[1] = posEnd - posStart
            return buf
        } else if (eventType == XmlPullParser.START_DOCUMENT || eventType == XmlPullParser.END_DOCUMENT) {
            holderForStartAndLength[1] = -1
            holderForStartAndLength[0] = holderForStartAndLength[1]
            return null
        } else {
            throw IllegalArgumentException("unknown text eventType: $eventType")
        }
    }

    override fun getNamespace(): String? {
        return if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
            if (processNamespaces) elUri[depth] else XmlPullParser.NO_NAMESPACE
        } else {
            null
        }
    }

    override fun getName(): String? = when (eventType) {
        XmlPullParser.START_TAG  -> elName[depth]
        XmlPullParser.END_TAG    -> elName[depth]
        XmlPullParser.ENTITY_REF -> entityRefName ?: this.newString(buf, posStart, posEnd - posStart).also { entityRefName = it }
        else                     -> null
    }

    override fun getPrefix(): String? = if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
        elPrefix[depth]
    } else {
        null
    }

    @Throws(XmlPullParserException::class)
    override fun isEmptyElementTag(): Boolean {
        if (eventType != XmlPullParser.START_TAG) throw XmlPullParserException("parser must be on START_TAG to check for empty element", this, null)
        return emptyElementTag
    }

    override fun getAttributeCount(): Int {
        if (eventType != XmlPullParser.START_TAG) return -1
        return attributeCount
    }

    override fun getAttributeNamespace(index: Int): String {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (!processNamespaces) return XmlPullParser.NO_NAMESPACE
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )

        return attributeUri[index]
    }

    override fun getAttributeName(index: Int): String {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")

        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )

        return attributeName[index]
    }

    override fun getAttributePrefix(index: Int): String? {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")

        if (!processNamespaces) return null

        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )

        return attributePrefix[index]
    }

    override fun getAttributeType(index: Int): String {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")

        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )

        return "CDATA"
    }

    override fun isAttributeDefault(index: Int): Boolean {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")

        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )

        return false
    }

    override fun getAttributeValue(index: Int): String {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")

        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )

        return attributeValue[index]
    }

    override fun getAttributeValue(namespace: String, name: String): String? {
        if (eventType != XmlPullParser.START_TAG)
            throw IndexOutOfBoundsException("only START_TAG can have attributes" + getPositionDescription())

        val ns = if (processNamespaces) namespace else null

        for (i in 0 until attributeCount) {
            if (processNamespaces) {
                if (ns == attributeUri[i] && name == attributeName[i]) return attributeValue[i]
            } else {
                require(ns == null) { "when namespaces processing is disabled attribute namespace must be null" }
                if (name == attributeName[i]) return attributeValue[i]
            }
        }

        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun require(type: Int, namespace: String?, name: String?) {
        if (!processNamespaces && namespace != null) {
            throw XmlPullParserException(
                "processing namespaces must be enabled on parser (or factory) to have possible namespaces declared on elements (position: ${getPositionDescription()})"
            )
        }
        if (type != eventType || (namespace != null && namespace != this.getNamespace()) || (name != null && name != this.getName())) {
            throw XmlPullParserException(
                "expected event ${XmlPullParser.TYPES[type]}" +
                (if (name != null) " with name '$name'" else "") +
                (if (namespace != null && name != null) " and" else "") +
                (if (namespace != null) " with namespace '$namespace'" else "") +
                " but got" +
                (if (type != eventType) " ${XmlPullParser.TYPES[eventType]}" else "") +
                (if (name != null && this.getName() != null && name != this.getName()) " name '${this.getName()}'" else "") +
                (if (namespace != null && this.getNamespace() != null && namespace != this.getNamespace()) " namespace '${this.getNamespace()}'" else "") +
                " (position: ${getPositionDescription()})"
            )
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun skipSubTree() {
        require(XmlPullParser.START_TAG, null, null)
        var level = 1
        while (level > 0) {
            val eventType = next()
            if (eventType == XmlPullParser.END_TAG) {
                --level
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextText(): String {
        if (eventType != XmlPullParser.START_TAG) {
            throw XmlPullParserException("parser must be on START_TAG to read next text", this, null)
        }

        var eventType = next()

        when (eventType) {
            XmlPullParser.TEXT    -> {
                val result = getText()
                eventType = next()
                if (eventType != XmlPullParser.END_TAG) {
                    throw XmlPullParserException(
                        "TEXT must be immediately followed by END_TAG and not " + XmlPullParser.TYPES[this.eventType], this, null
                    )
                }
                return result!!
            }
            XmlPullParser.END_TAG -> {
                return ""
            }
            else                  -> {
                throw XmlPullParserException("parser must be on START_TAG or TEXT to read text", this, null)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextTag(): Int {
        next()

        if (eventType == XmlPullParser.TEXT && isWhitespace()) {
            next()
        }

        if (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_TAG) {
            throw XmlPullParserException("expected START_TAG or END_TAG not " + XmlPullParser.TYPES[eventType], this, null)
        }

        return eventType
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun next(): Int {
        tokenize = false
        return nextImpl()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextToken(): Int {
        tokenize = true
        return nextImpl()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun nextImpl(): Int {
        text = null
        pcStart = 0
        pcEnd = pcStart
        usePC = false
        bufStart = posEnd
        if (pastEndTag) {
            pastEndTag = false
            --depth
            namespaceEnd = elNamespaceCount[depth]
        }
        if (emptyElementTag) {
            emptyElementTag = false
            pastEndTag = true
            return XmlPullParser.END_TAG.also { eventType = it }
        }

        if (depth > 0) {
            if (seenStartTag) {
                seenStartTag = false
                return parseStartTag().also { eventType = it }
            }
            if (seenEndTag) {
                seenEndTag = false
                return parseEndTag().also { eventType = it }
            }

            var ch: Char
            if (seenMarkup) {
                seenMarkup = false
                ch = '<'
            } else if (seenAmpersand) {
                seenAmpersand = false
                ch = '&'
            } else {
                ch = more()
            }
            posStart = pos - 1
            var hadCharData = false

            var needsMerging = false

            MAIN_LOOP@ while (true) {
                if (ch == '<') {
                    if (hadCharData) {
                        if (tokenize) {
                            seenMarkup = true
                            return XmlPullParser.TEXT.also { eventType = it }
                        }
                    }
                    ch = more()
                    if (ch == '/') {
                        if (!tokenize && hadCharData) {
                            seenEndTag = true
                            return XmlPullParser.TEXT.also { eventType = it }
                        }
                        return parseEndTag().also { eventType = it }
                    } else if (ch == '!') {
                        ch = more()
                        if (ch == '-') {
                            parseComment()
                            if (tokenize) return XmlPullParser.COMMENT.also { eventType = it }
                            if (!usePC && hadCharData) {
                                needsMerging = true
                            } else {
                                posStart = pos
                            }
                        } else if (ch == '[') {
                            parseCDSect(hadCharData)
                            if (tokenize) return XmlPullParser.CDSECT.also { eventType = it }
                            val cdStart = posStart
                            val cdEnd = posEnd
                            val cdLen = cdEnd - cdStart

                            if (cdLen > 0) {
                                hadCharData = true
                                if (!usePC) {
                                    needsMerging = true
                                }
                            }

                        } else {
                            throw XmlPullParserException(
                                "unexpected character in markup " + printable(ch.code), this, null
                            )
                        }
                    } else if (ch == '?') {
                        parsePI()
                        if (tokenize) return XmlPullParser.PROCESSING_INSTRUCTION.also { eventType = it }
                        if (!usePC && hadCharData) {
                            needsMerging = true
                        } else {
                            posStart = pos
                        }
                    } else if (isNameStartChar(ch)) {
                        if (!tokenize && hadCharData) {
                            seenStartTag = true
                            return XmlPullParser.TEXT.also { eventType = it }
                        }
                        return parseStartTag().also { eventType = it }
                    } else {
                        throw XmlPullParserException("unexpected character in markup " + printable(ch.code), this, null)
                    }

                } else if (ch == '&') {
                    if (tokenize && hadCharData) {
                        seenAmpersand = true
                        return XmlPullParser.TEXT.also { eventType = it }
                    }
                    val oldStart = posStart + bufAbsoluteStart
                    val oldEnd = posEnd + bufAbsoluteStart
                    parseEntityRef()
                    if (tokenize) return XmlPullParser.ENTITY_REF.also { eventType = it }
                    if (resolvedEntityRefCharBuf.contentEquals(BUF_NOT_RESOLVED)) {
                        if (entityRefName == null) {
                            entityRefName = this.newString(buf, posStart, posEnd - posStart)
                        }
                        throw XmlPullParserException(
                            "could not resolve entity named '" + printable(entityRefName) + "'", this, null
                        )
                    }
                    posStart = oldStart - bufAbsoluteStart
                    posEnd = oldEnd - bufAbsoluteStart
                    if (!usePC) {
                        if (hadCharData) {
                            joinPC()
                            needsMerging = false
                        } else {
                            usePC = true
                            pcEnd = 0
                            pcStart = pcEnd
                        }
                    }
                    for (aResolvedEntity in resolvedEntityRefCharBuf) {
                        if (pcEnd >= pc.size) {
                            ensurePC(pcEnd)
                        }
                        pc[pcEnd++] = aResolvedEntity
                    }
                    hadCharData = true
                } else {
                    if (needsMerging) {
                        joinPC()
                        needsMerging = false
                    }


                    hadCharData = true

                    var normalizedCR = false
                    val normalizeInput = !tokenize || !roundtripSupported
                    var seenBracket = false
                    var seenBracketBracket = false
                    do {

                        if (ch == ']') {
                            if (seenBracket) {
                                seenBracketBracket = true
                            } else {
                                seenBracket = true
                            }
                        } else if (seenBracketBracket && ch == '>') {
                            throw XmlPullParserException("characters ]]> are not allowed in content", this, null)
                        } else {
                            if (seenBracket) {
                                seenBracket = false
                                seenBracketBracket = seenBracket
                            }
                        }
                        if (normalizeInput) {
                            when (ch) {
                                '\r' -> {
                                    normalizedCR = true
                                    posEnd = pos - 1
                                    if (!usePC) {
                                        if (posEnd > posStart) {
                                            joinPC()
                                        } else {
                                            usePC = true
                                            pcEnd = 0
                                            pcStart = pcEnd
                                        }
                                    }
                                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                                    pc[pcEnd++] = '\n'
                                }
                                '\n' -> {
                                    if (!normalizedCR && usePC) {
                                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                                        pc[pcEnd++] = '\n'
                                    }
                                    normalizedCR = false
                                }
                                else -> {
                                    if (usePC) {
                                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                                        pc[pcEnd++] = ch
                                    }
                                    normalizedCR = false
                                }
                            }
                        }

                        ch = more()
                    } while (ch != '<' && ch != '&')
                    posEnd = pos - 1
                    continue@MAIN_LOOP
                }
                ch = more()
            }
        } else {
            return if (seenRoot) {
                parseEpilog()
            } else {
                parseProlog()
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseProlog(): Int {

        var ch: Char
        ch = if (seenMarkup) {
            buf[pos - 1]
        } else {
            more()
        }

        if (eventType == XmlPullParser.START_DOCUMENT) {
            if (ch == '\uFFFE') {
                throw XmlPullParserException(
                    "first character in input was UNICODE noncharacter (0xFFFE)" + "- input requires int swapping",
                    this,
                    null
                )
            }
            if (ch == '\uFEFF') {
                ch = more()
            } else if (ch == '\uFFFD') {
                ch = more()
                if (ch == '\uFFFD') {
                    throw XmlPullParserException("UTF-16 BOM in a UTF-8 encoded file is incompatible", this, null)
                }
            }
        }
        seenMarkup = false
        var gotS = false
        posStart = pos - 1
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false
        while (true) {
            if (ch == '<') {
                if (gotS && tokenize) {
                    posEnd = pos - 1
                    seenMarkup = true
                    return XmlPullParser.IGNORABLE_WHITESPACE.also { eventType = it }
                }
                ch = more()
                if (ch == '?') {
                    parsePI()
                    if (tokenize) {
                        return XmlPullParser.PROCESSING_INSTRUCTION.also { eventType = it }
                    }
                } else if (ch == '!') {
                    ch = more()
                    if (ch == 'D') {
                        if (seenDocdecl) {
                            throw XmlPullParserException("only one docdecl allowed in XML document", this, null)
                        }
                        seenDocdecl = true
                        parseDocdecl()
                        if (tokenize) return XmlPullParser.DOCDECL.also { eventType = it }
                    } else if (ch == '-') {
                        parseComment()
                        if (tokenize) return XmlPullParser.COMMENT.also { eventType = it }
                    } else {
                        throw XmlPullParserException("unexpected markup <!" + printable(ch.code), this, null)
                    }
                } else if (ch == '/') {
                    throw XmlPullParserException("expected start tag name and not " + printable(ch.code), this, null)
                } else if (isNameStartChar(ch)) {
                    seenRoot = true
                    return parseStartTag()
                } else {
                    throw XmlPullParserException("expected start tag name and not " + printable(ch.code), this, null)
                }
            } else if (isS(ch)) {
                gotS = true
                if (normalizeIgnorableWS) {
                    when (ch) {
                        '\r' -> {
                            normalizedCR = true
                            if (!usePC) {
                                posEnd = pos - 1
                                if (posEnd > posStart) {
                                    joinPC()
                                } else {
                                    usePC = true
                                    pcEnd = 0
                                    pcStart = pcEnd
                                }
                            }
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        '\n' -> {
                            if (!normalizedCR && usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = '\n'
                            }
                            normalizedCR = false
                        }
                        else -> {
                            if (usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = ch
                            }
                            normalizedCR = false
                        }
                    }
                }
            } else {
                throw XmlPullParserException(
                    "only whitespace content allowed before start tag and not " + printable(ch.code), this, null
                )
            }
            ch = more()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseEpilog(): Int {
        if (eventType == XmlPullParser.END_DOCUMENT) {
            throw XmlPullParserException("already reached end of XML input", this, null)
        }
        if (reachedEnd) {
            return XmlPullParser.END_DOCUMENT.also { eventType = it }
        }
        var gotS = false
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false
        try {
            var ch: Char
            ch = if (seenMarkup) {
                buf[pos - 1]
            } else {
                more()
            }
            seenMarkup = false
            posStart = pos - 1
            if (!reachedEnd) {
                while (true) {
                    if (ch == '<') {
                        if (gotS && tokenize) {
                            posEnd = pos - 1
                            seenMarkup = true
                            return XmlPullParser.IGNORABLE_WHITESPACE.also { eventType = it }
                        }
                        ch = more()
                        if (reachedEnd) {
                            break
                        }
                        if (ch == '?') {
                            parsePI()
                            if (tokenize) return XmlPullParser.PROCESSING_INSTRUCTION.also { eventType = it }
                        } else if (ch == '!') {
                            ch = more()
                            if (reachedEnd) {
                                break
                            }
                            if (ch == 'D') {
                                parseDocdecl()
                                if (tokenize) return XmlPullParser.DOCDECL.also { eventType = it }
                            } else if (ch == '-') {
                                parseComment()
                                if (tokenize) return XmlPullParser.COMMENT.also { eventType = it }
                            } else {
                                throw XmlPullParserException("unexpected markup <!" + printable(ch.code), this, null)
                            }
                        } else if (ch == '/') {
                            throw XmlPullParserException(
                                "end tag not allowed in epilog but got " + printable(ch.code), this, null
                            )
                        } else if (isNameStartChar(ch)) {
                            throw XmlPullParserException(
                                "start tag not allowed in epilog but got " + printable(ch.code), this, null
                            )
                        } else {
                            throw XmlPullParserException(
                                "in epilog expected ignorable content and not " + printable(ch.code), this, null
                            )
                        }
                    } else if (isS(ch)) {
                        gotS = true
                        if (normalizeIgnorableWS) {
                            when (ch) {
                                '\r' -> {
                                    normalizedCR = true
                                    if (!usePC) {
                                        posEnd = pos - 1
                                        if (posEnd > posStart) {
                                            joinPC()
                                        } else {
                                            usePC = true
                                            pcEnd = 0
                                            pcStart = pcEnd
                                        }
                                    }
                                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                                    pc[pcEnd++] = '\n'
                                }
                                '\n' -> {
                                    if (!normalizedCR && usePC) {
                                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                                        pc[pcEnd++] = '\n'
                                    }
                                    normalizedCR = false
                                }
                                else -> {
                                    if (usePC) {
                                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                                        pc[pcEnd++] = ch
                                    }
                                    normalizedCR = false
                                }
                            }
                        }
                    } else {
                        throw XmlPullParserException(
                            "in epilog non whitespace content is not allowed but got " + printable(ch.code), this, null
                        )
                    }
                    ch = more()
                    if (reachedEnd) {
                        break
                    }
                }
            }

        } catch (ex: EOFException) {
            reachedEnd = true
        }
        if (tokenize && gotS) {
            posEnd = pos
            return XmlPullParser.IGNORABLE_WHITESPACE.also { eventType = it }
        }
        return XmlPullParser.END_DOCUMENT.also { eventType = it }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseEndTag(): Int {
        var ch = more()
        if (!isNameStartChar(ch)) {
            throw XmlPullParserException("expected name start and not " + printable(ch.code), this, null)
        }
        posStart = pos - 3
        val nameStart = pos - 1 + bufAbsoluteStart
        do {
            ch = more()
        } while (isNameChar(ch))


        var off = nameStart - bufAbsoluteStart
        val len = (pos - 1) - off
        val cbuf = elRawName[depth]
        if (elRawNameEnd[depth] != len) {
            val startname = cbuf!!.concatToString(0, 0 + elRawNameEnd[depth])
            val endname = buf.concatToString(off, off + len)
            throw XmlPullParserException(
                ("end tag name </" + endname + "> must match start tag name <" + startname + ">" + " from line "
                        + elRawNameLine[depth]),
                this,
                null
            )
        }
        for (i in 0..<len) {
            if (buf[off++] != cbuf!![i]) {
                val startname = cbuf.concatToString(0, 0 + len)
                val offset = off - i - 1
                val endname = buf.concatToString(offset, offset + len)
                throw XmlPullParserException(
                    ("end tag name </" + endname + "> must be the same as start tag <" + startname + ">"
                            + " from line " + elRawNameLine[depth]),
                    this,
                    null
                )
            }
        }

        while (isS(ch)) {
            ch = more()
        }
        if (ch != '>') {
            throw XmlPullParserException(
                "expected > to finsh end tag not " + printable(ch.code) + " from line " + elRawNameLine[depth],
                this,
                null
            )
        }

        posEnd = pos
        pastEndTag = true
        return XmlPullParser.END_TAG.also { eventType = it }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseStartTag(): Int {
        ++depth
        posStart = pos - 2

        emptyElementTag = false
        attributeCount = 0
        val nameStart = pos - 1 + bufAbsoluteStart
        var colonPos = -1
        var ch = buf[pos - 1]
        if (ch == ':' && processNamespaces) throw XmlPullParserException(
            "when namespaces processing enabled colon can not be at element name start", this, null
        )
        while (true) {
            ch = more()
            if (!isNameChar(ch)) break
            if (ch == ':' && processNamespaces) {
                if (colonPos != -1) throw XmlPullParserException(
                    "only one colon is allowed in name of element when namespaces are enabled", this, null
                )
                colonPos = pos - 1 + bufAbsoluteStart
            }
        }

        ensureElementsCapacity()

        val elLen = (pos - 1) - (nameStart - bufAbsoluteStart)
        if (elRawName[depth] == null || elRawName[depth]!!.size < elLen) {
            elRawName[depth] = CharArray(2 * elLen)
        }
        buf.copyInto(
            destination = elRawName[depth]!!,
            destinationOffset = 0,
            startIndex = nameStart - bufAbsoluteStart,
            endIndex = nameStart - bufAbsoluteStart + elLen
        )
        elRawNameEnd[depth] = elLen
        elRawNameLine[depth] = lineNumber

        var prefix: String? = null
        if (processNamespaces) {
            if (colonPos != -1) {
                elPrefix[depth] = this.newString(buf, nameStart - bufAbsoluteStart, colonPos - nameStart)
                prefix = elPrefix[depth]
                elName[depth] = this.newString(
                    buf,
                    colonPos + 1 - bufAbsoluteStart, pos - 2 - (colonPos - bufAbsoluteStart)
                )
            } else {
                elPrefix[depth] = null
                prefix = elPrefix[depth]
                elName[depth] = this.newString(buf, nameStart - bufAbsoluteStart, elLen)
            }
        } else {
            elName[depth] = this.newString(buf, nameStart - bufAbsoluteStart, elLen)
        }

        while (true) {
            while (isS(ch)) {
                ch = more()
            }

            if (ch == '>') {
                break
            } else if (ch == '/') {
                if (emptyElementTag) throw XmlPullParserException("repeated / in tag declaration", this, null)
                emptyElementTag = true
                ch = more()
                if (ch != '>') throw XmlPullParserException("expected > to end empty tag not " + printable(ch.code), this, null)
                break
            } else if (isNameStartChar(ch)) {
                ch = parseAttribute()
                ch = more()
            } else {
                throw XmlPullParserException("start tag unexpected character " + printable(ch.code), this, null)
            }
        }

        if (processNamespaces) {
            var uri = getNamespace(prefix)
            if (uri == null) {
                if (prefix == null) {
                    uri = XmlPullParser.NO_NAMESPACE
                } else {
                    throw XmlPullParserException(
                        "could not determine namespace bound to element prefix $prefix", this, null
                    )
                }
            }
            elUri[depth] = uri

            for (i in 0..<attributeCount) {
                val attrPrefix = attributePrefix[i]
                if (attrPrefix != null) {
                    val attrUri = getNamespace(attrPrefix)
                        ?: throw XmlPullParserException(
                            "could not determine namespace bound to attribute prefix $attrPrefix", this, null
                        )
                    attributeUri[i] = attrUri
                } else {
                    attributeUri[i] = XmlPullParser.NO_NAMESPACE
                }
            }

            for (i in 1..<attributeCount) {
                for (j in 0..<i) {
                    if (attributeUri[j] === attributeUri[i]
                        && (allStringsInterned && attributeName[j] == attributeName[i]
                                || (!allStringsInterned && attributeNameHash[j] == attributeNameHash[i] && attributeName[j] == attributeName[i]))
                    ) {

                        var attr1 = attributeName[j]
                        attr1 = attributeUri[j] + ":" + attr1
                        var attr2 = attributeName[i]
                        attr2 = attributeUri[i] + ":" + attr2
                        throw XmlPullParserException(
                            "duplicated attributes $attr1 and $attr2", this, null
                        )
                    }
                }
            }
        } else {

            for (i in 1..<attributeCount) {
                for (j in 0..<i) {
                    if ((allStringsInterned && attributeName[j] == attributeName[i]
                                || (!allStringsInterned && attributeNameHash[j] == attributeNameHash[i] && attributeName[j] == attributeName[i]))
                    ) {

                        val attr1 = attributeName[j]
                        val attr2 = attributeName[i]
                        throw XmlPullParserException(
                            "duplicated attributes $attr1 and $attr2", this, null
                        )
                    }
                }
            }
        }

        elNamespaceCount[depth] = namespaceEnd
        posEnd = pos
        return XmlPullParser.START_TAG.also { eventType = it }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseAttribute(): Char {
        val prevPosStart = posStart + bufAbsoluteStart
        val nameStart = pos - 1 + bufAbsoluteStart
        var colonPos = -1
        var ch = buf[pos - 1]
        if (ch == ':' && processNamespaces) throw XmlPullParserException(
            "when namespaces processing enabled colon can not be at attribute name start", this, null
        )

        var startsWithXmlns = processNamespaces && ch == 'x'
        var xmlnsPos = 0

        ch = more()
        while (isNameChar(ch)) {
            if (processNamespaces) {
                if (startsWithXmlns && xmlnsPos < 5) {
                    ++xmlnsPos
                    if (xmlnsPos == 1) {
                        if (ch != 'm') startsWithXmlns = false
                    } else if (xmlnsPos == 2) {
                        if (ch != 'l') startsWithXmlns = false
                    } else if (xmlnsPos == 3) {
                        if (ch != 'n') startsWithXmlns = false
                    } else if (xmlnsPos == 4) {
                        if (ch != 's') startsWithXmlns = false
                    } else if (xmlnsPos == 5) {
                        if (ch != ':') throw XmlPullParserException(
                            "after xmlns in attribute name must be colon" + "when namespaces are enabled",
                            this,
                            null
                        )
                    }
                }
                if (ch == ':') {
                    if (colonPos != -1) throw XmlPullParserException(
                        "only one colon is allowed in attribute name" + " when namespaces are enabled",
                        this,
                        null
                    )
                    colonPos = pos - 1 + bufAbsoluteStart
                }
            }
            ch = more()
        }

        ensureAttributesCapacity(attributeCount)

        var name: String? = null
        var prefix: String? = null
        if (processNamespaces) {
            if (xmlnsPos < 4) startsWithXmlns = false
            if (startsWithXmlns) {
                if (colonPos != -1) {
                    val nameLen = pos - 2 - (colonPos - bufAbsoluteStart)
                    if (nameLen == 0) {
                        throw XmlPullParserException(
                            "namespace prefix is required after xmlns: " + " when namespaces are enabled",
                            this,
                            null
                        )
                    }
                    name = this.newString(buf, colonPos - bufAbsoluteStart + 1, nameLen)
                }
            } else {
                if (colonPos != -1) {
                    val prefixLen = colonPos - nameStart
                    attributePrefix[attributeCount] = this.newString(buf, nameStart - bufAbsoluteStart, prefixLen)
                    prefix = attributePrefix[attributeCount]
                    val nameLen = pos - 2 - (colonPos - bufAbsoluteStart)
                    attributeName[attributeCount] = this.newString(buf, colonPos - bufAbsoluteStart + 1, nameLen)
                    name = attributeName[attributeCount]


                } else {
                    attributePrefix[attributeCount] = null
                    prefix = attributePrefix[attributeCount]
                    attributeName[attributeCount] =
                        this.newString(buf, nameStart - bufAbsoluteStart, pos - 1 - (nameStart - bufAbsoluteStart))
                    name = attributeName[attributeCount]
                }
                if (!allStringsInterned) {
                    attributeNameHash[attributeCount] = name.hashCode()
                }
            }
        } else {
            attributeName[attributeCount] =
                this.newString(buf, nameStart - bufAbsoluteStart, pos - 1 - (nameStart - bufAbsoluteStart))
            name = attributeName[attributeCount]
            if (!allStringsInterned) {
                attributeNameHash[attributeCount] = name.hashCode()
            }
        }

        while (isS(ch)) {
            ch = more()
        }
        if (ch != '=') throw XmlPullParserException("expected = after attribute name", this, null)
        ch = more()
        while (isS(ch)) {
            ch = more()
        }

        val delimit = ch
        if (delimit != '"' && delimit != '\'') throw XmlPullParserException(
            "attribute value must start with quotation or apostrophe not " + printable(delimit.code), this, null
        )

        var normalizedCR = false
        usePC = false
        pcStart = pcEnd
        posStart = pos

        while (true) {
            ch = more()
            if (ch == delimit) {
                break
            }
            if (ch == '<') {
                throw XmlPullParserException("markup not allowed inside attribute value - illegal < ", this, null)
            }
            if (ch == '&') {
                extractEntityRef()
            } else if (ch == '\t' || ch == '\n' || ch == '\r') {
                if (!usePC) {
                    posEnd = pos - 1
                    if (posEnd > posStart) {
                        joinPC()
                    } else {
                        usePC = true
                        pcStart = 0
                        pcEnd = pcStart
                    }
                }
                if (pcEnd >= pc.size) ensurePC(pcEnd)
                if (ch != '\n' || !normalizedCR) {
                    pc[pcEnd++] = ' '
                }
            } else {
                if (usePC) {
                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                    pc[pcEnd++] = ch
                }
            }
            normalizedCR = ch == '\r'
        }

        if (processNamespaces && startsWithXmlns) {
            val ns: String = if (!usePC) {
                this.newString(buf, posStart, pos - 1 - posStart)
            } else {
                this.newString(pc, pcStart, pcEnd - pcStart)
            }
            ensureNamespacesCapacity(namespaceEnd)
            var prefixHash = -1
            if (colonPos != -1) {
                if (ns.isEmpty()) {
                    throw XmlPullParserException(
                        "non-default namespace can not be declared to be empty string", this, null
                    )
                }
                namespacePrefix[namespaceEnd] = name!!
                if (!allStringsInterned) {
                    namespacePrefixHash[namespaceEnd] = name.hashCode()
                    prefixHash = namespacePrefixHash[namespaceEnd]
                }
            } else {
                namespacePrefix[namespaceEnd] = ""
                if (!allStringsInterned) {
                    namespacePrefixHash[namespaceEnd] = -1
                    prefixHash = namespacePrefixHash[namespaceEnd]
                }
            }
            namespaceUri[namespaceEnd] = ns

            val startNs = elNamespaceCount[depth - 1]
            for (i in namespaceEnd - 1 downTo startNs) {
                if (
                    (allStringsInterned || name == null) ||
                    (!allStringsInterned && namespacePrefixHash[i] == prefixHash && name == namespacePrefix[i])
                ) {
                    val s = "'$name'"
                    throw XmlPullParserException(
                        "duplicated namespace declaration for $s prefix", this, null
                    )
                }
            }

            ++namespaceEnd
        } else {
            if (!usePC) {
                attributeValue[attributeCount] = buf.concatToString(posStart, posStart + (pos - 1 - posStart))
            } else {
                attributeValue[attributeCount] = pc.concatToString(pcStart, pcStart + (pcEnd - pcStart))
            }
            ++attributeCount
        }
        posStart = prevPosStart - bufAbsoluteStart
        return ch
    }

    private var resolvedEntityRefCharBuf = BUF_NOT_RESOLVED

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCharOrPredefinedEntityRef(): Int {


        entityRefName = null
        posStart = pos
        var len: Int
        resolvedEntityRefCharBuf = BUF_NOT_RESOLVED
        var ch = more()
        if (ch == '#') {

            var charRef = 0.toChar()
            ch = more()
            val sb = StringBuilder()
            val isHex = (ch == 'x')

            if (isHex) {
                while (true) {
                    ch = more()
                    when (ch) {
                        in '0'..'9' -> {
                            charRef = (charRef.code * 16 + (ch.code - '0'.code)).toChar()
                            sb.append(ch)
                        }
                        in 'a'..'f' -> {
                            charRef = (charRef.code * 16 + (ch.code - ('a'.code - 10))).toChar()
                            sb.append(ch)
                        }
                        in 'A'..'F' -> {
                            charRef = (charRef.code * 16 + (ch.code - ('A'.code - 10))).toChar()
                            sb.append(ch)
                        }
                        ';'         -> {
                            break
                        }
                        else        -> {
                            throw XmlPullParserException(
                                "character reference (with hex value) may not contain " + printable(ch.code), this, null
                            )
                        }
                    }
                }
            } else {
                while (true) {
                    when (ch) {
                        in '0'..'9' -> {
                            charRef = (charRef.code * 10 + (ch.code - '0'.code)).toChar()
                            sb.append(ch)
                        }
                        ';'         -> {
                            break
                        }
                        else        -> {
                            throw XmlPullParserException(
                                "character reference (with decimal value) may not contain " + printable(ch.code),
                                this,
                                null
                            )
                        }
                    }
                    ch = more()
                }
            }

            var isValidCodePoint: Boolean
            try {
                val codePoint: Int = sb.toString().toInt(if (isHex) 16 else 10)
                isValidCodePoint = isValidCodePoint(codePoint)
                if (isValidCodePoint) {
                    resolvedEntityRefCharBuf = CodePoints.toChars(codePoint)
                }
            } catch (e: IllegalArgumentException) {
                isValidCodePoint = false
            }

            if (!isValidCodePoint) {
                throw XmlPullParserException(
                    ("character reference (with " + (if (isHex) "hex" else "decimal") + " value " + sb.toString()
                            + ") is invalid"),
                    this,
                    null
                )
            }

            if (tokenize) {
                text = this.newString(resolvedEntityRefCharBuf, 0, resolvedEntityRefCharBuf.size)
            }
            len = resolvedEntityRefCharBuf.size
        } else {
            if (!isNameStartChar(ch)) {
                throw XmlPullParserException(
                    "entity reference names can not start with character '" + printable(ch.code) + "'", this, null
                )
            }
            while (true) {
                ch = more()
                if (ch == ';') {
                    break
                }
                if (!isNameChar(ch)) {
                    throw XmlPullParserException(
                        "entity reference name can not contain character " + printable(ch.code) + "'", this, null
                    )
                }
            }
            len = (pos - 1) - posStart
            if (len == 2 && buf[posStart] == 'l' && buf[posStart + 1] == 't') {
                if (tokenize) {
                    text = "<"
                }
                resolvedEntityRefCharBuf = BUF_LT
            } else if (len == 3 && buf[posStart] == 'a' && buf[posStart + 1] == 'm' && buf[posStart + 2] == 'p') {
                if (tokenize) {
                    text = "&"
                }
                resolvedEntityRefCharBuf = BUF_AMP
            } else if (len == 2 && buf[posStart] == 'g' && buf[posStart + 1] == 't') {
                if (tokenize) {
                    text = ">"
                }
                resolvedEntityRefCharBuf = BUF_GT
            } else if (len == 4 && buf[posStart] == 'a' && buf[posStart + 1] == 'p' && buf[posStart + 2] == 'o' && buf[posStart + 3] == 's') {
                if (tokenize) {
                    text = "'"
                }
                resolvedEntityRefCharBuf = BUF_APO
            } else if (len == 4 && buf[posStart] == 'q' && buf[posStart + 1] == 'u' && buf[posStart + 2] == 'o' && buf[posStart + 3] == 't') {
                if (tokenize) {
                    text = "\""
                }
                resolvedEntityRefCharBuf = BUF_QUOT
            }
        }

        posEnd = pos

        return len
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseEntityRefInDocDecl() {
        parseCharOrPredefinedEntityRef()
        if (usePC) {
            posStart--
            joinPC()
        }

        if (!resolvedEntityRefCharBuf.contentEquals(BUF_NOT_RESOLVED)) return
        if (tokenize) text = null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseEntityRef() {
        val len = parseCharOrPredefinedEntityRef()

        posEnd--
        if (!resolvedEntityRefCharBuf.contentEquals(BUF_NOT_RESOLVED)) {
            return
        }

        resolvedEntityRefCharBuf = lookuEntityReplacement(len)
        if (!resolvedEntityRefCharBuf.contentEquals(BUF_NOT_RESOLVED)) {
            return
        }
        if (tokenize) text = null
    }

    private fun lookuEntityReplacement(entityNameLen: Int): CharArray {
        if (!allStringsInterned) {
            val hash = fastHash(buf, posStart, posEnd - posStart)
            LOOP@ for (i in entityEnd - 1 downTo 0) {
                if (hash == entityNameHash[i] && entityNameLen == entityNameBuf[i].size) {
                    val entityBuf = entityNameBuf[i]
                    for (j in 0..<entityNameLen) {
                        if (buf[posStart + j] != entityBuf[j]) continue@LOOP
                    }
                    if (tokenize) text = entityReplacement[i]
                    return entityReplacementBuf[i]
                }
            }
        } else {
            entityRefName = this.newString(buf, posStart, posEnd - posStart)
            for (i in entityEnd - 1 downTo 0) {
                if (entityRefName === entityName[i]) {
                    if (tokenize) text = entityReplacement[i]
                    return entityReplacementBuf[i]
                }
            }
        }
        return BUF_NOT_RESOLVED
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseComment() {


        var cch = more()
        if (cch != '-') throw XmlPullParserException("expected <!-- for comment start", this, null)
        if (tokenize) posStart = pos

        val curLine = lineNumber
        val curColumn = columnNumber - 4
        try {
            val normalizeIgnorableWS = tokenize && !roundtripSupported
            var normalizedCR = false

            var seenDash = false
            var seenDashDash = false
            while (true) {
                cch = more()
                val ch: Int
                val cch2: Char
                if (Character.isHighSurrogate(cch)) {
                    cch2 = more()
                    ch = Character.toCodePoint(cch, cch2)
                } else {
                    cch2 = 0.toChar()
                    ch = cch.code
                }
                if (seenDashDash && ch != '>'.code) {
                    throw XmlPullParserException(
                        "in comment after two dashes (--) next character must be >" + " not " + printable(ch),
                        this,
                        null
                    )
                }
                if (ch == '-'.code) {
                    if (!seenDash) {
                        seenDash = true
                    } else {
                        seenDashDash = true
                    }
                } else if (ch == '>'.code) {
                    if (seenDashDash) {
                        break
                    }
                    seenDash = false
                } else if (isValidCodePoint(ch)) {
                    seenDash = false
                } else {
                    throw XmlPullParserException(
                        "Illegal character 0x" + ch.toHexString() + " found in comment", this, null
                    )
                }
                if (normalizeIgnorableWS) {
                    when (ch) {
                        '\r'.code -> {
                            normalizedCR = true
                            if (!usePC) {
                                posEnd = pos - 1
                                if (posEnd > posStart) {
                                    joinPC()
                                } else {
                                    usePC = true
                                    pcEnd = 0
                                    pcStart = pcEnd
                                }
                            }
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        '\n'.code -> {
                            if (!normalizedCR && usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = '\n'
                            }
                            normalizedCR = false
                        }
                        else      -> {
                            if (usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = cch
                                if (cch2.code != 0) {
                                    pc[pcEnd++] = cch2
                                }
                            }
                            normalizedCR = false
                        }
                    }
                }
            }
        } catch (ex: EOFException) {
            throw XmlPullParserException(
                "comment started on line $curLine and column $curColumn was not closed", this, ex
            )
        }
        if (tokenize) {
            posEnd = pos - 3
            if (usePC) {
                pcEnd -= 2
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parsePI() {


        if (tokenize) posStart = pos
        val curLine = lineNumber
        val curColumn = columnNumber - 2
        val piTargetStart = pos
        var piTargetEnd = -1
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false

        try {
            var seenPITarget = false
            var seenInnerTag = false
            var seenQ = false
            var ch = more()
            if (isS(ch)) {
                throw XmlPullParserException(
                    "processing instruction PITarget must be exactly after <? and not white space character",
                    this,
                    null
                )
            }
            while (true) {

                if (ch == '?') {
                    if (!seenPITarget) {
                        throw XmlPullParserException("processing instruction PITarget name not found", this, null)
                    }
                    seenQ = true
                } else if (ch == '>') {
                    if (seenQ) {
                        break
                    }

                    if (!seenPITarget) {
                        throw XmlPullParserException("processing instruction PITarget name not found", this, null)
                    } else if (!seenInnerTag) {
                        throw XmlPullParserException(
                            ("processing instruction started on line " + curLine + " and column " + curColumn
                                    + " was not closed"),
                            this,
                            null
                        )
                    } else {
                        seenInnerTag = false
                    }
                } else if (ch == '<') {
                    seenInnerTag = true
                } else {
                    if (piTargetEnd == -1 && isS(ch)) {
                        piTargetEnd = pos - 1

                        if ((piTargetEnd - piTargetStart) >= 3) {
                            if ((buf[piTargetStart] == 'x' || buf[piTargetStart] == 'X')
                                && (buf[piTargetStart + 1] == 'm' || buf[piTargetStart + 1] == 'M')
                                && (buf[piTargetStart + 2] == 'l' || buf[piTargetStart + 2] == 'L')
                            ) {
                                if (piTargetStart > 2) {
                                    throw XmlPullParserException(
                                        if (eventType == 0)
                                            "XMLDecl is only allowed as first characters in input"
                                        else
                                            "processing instruction can not have PITarget with reserved xml name",
                                        this,
                                        null
                                    )
                                } else {
                                    if (buf[piTargetStart] != 'x' && buf[piTargetStart + 1] != 'm' && buf[piTargetStart + 2] != 'l') {
                                        throw XmlPullParserException(
                                            "XMLDecl must have xml name in lowercase", this, null
                                        )
                                    }
                                }
                                parseXmlDecl(ch)
                                if (tokenize) posEnd = pos - 2
                                val off = piTargetStart + 3
                                val len = pos - 2 - off
                                xmlDeclContent = this.newString(buf, off, len)
                                return
                            }
                        }
                    }

                    seenQ = false
                }
                if (normalizeIgnorableWS) {
                    when (ch) {
                        '\r' -> {
                            normalizedCR = true
                            if (!usePC) {
                                posEnd = pos - 1
                                if (posEnd > posStart) {
                                    joinPC()
                                } else {
                                    usePC = true
                                    pcEnd = 0
                                    pcStart = pcEnd
                                }
                            }
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        '\n' -> {
                            if (!normalizedCR && usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = '\n'
                            }
                            normalizedCR = false
                        }
                        else -> {
                            if (usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = ch
                            }
                            normalizedCR = false
                        }
                    }
                }
                seenPITarget = true
                ch = more()
            }
        } catch (ex: EOFException) {
            throw XmlPullParserException(
                ("processing instruction started on line " + curLine + " and column " + curColumn
                        + " was not closed"),
                this,
                ex
            )
        }
        if (piTargetEnd == -1) {
            piTargetEnd = pos - 2 + bufAbsoluteStart
        }
        if (tokenize) {
            posEnd = pos - 2
            if (normalizeIgnorableWS) {
                --pcEnd
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXmlDecl(ch: Char) {


        var ch = ch
        preventBufferCompaction = true
        bufStart = 0

        ch = skipS(ch)
        ch = requireInput(ch, VERSION)
        ch = skipS(ch)
        if (ch != '=') {
            throw XmlPullParserException(
                "expected equals sign (=) after version and not " + printable(ch.code), this, null
            )
        }
        ch = more()
        ch = skipS(ch)
        if (ch != '\'' && ch != '"') {
            throw XmlPullParserException(
                "expected apostrophe (') or quotation mark (\") after version and not " + printable(ch.code),
                this,
                null
            )
        }
        val quotChar = ch
        val versionStart = pos
        ch = more()
        while (ch != quotChar) {
            if ((ch < 'a' || ch > 'z')
                && (ch < 'A' || ch > 'Z')
                && (ch < '0' || ch > '9')
                && ch != '_' && ch != '.' && ch != ':' && ch != '-'
            ) {
                throw XmlPullParserException(
                    "<?xml version value expected to be in ([a-zA-Z0-9_.:] | '-')" + " not " + printable(ch.code),
                    this,
                    null
                )
            }
            ch = more()
        }
        val versionEnd = pos - 1
        parseXmlDeclWithVersion(versionStart, versionEnd)
        preventBufferCompaction = false
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXmlDeclWithVersion(versionStart: Int, versionEnd: Int) {
        if ((versionEnd - versionStart != 3)
            || buf[versionStart] != '1' || buf[versionStart + 1] != '.' || buf[versionStart + 2] != '0'
        ) {
            throw XmlPullParserException(
                ("only 1.0 is supported as <?xml version not '"
                        + printable(buf.concatToString(versionStart, versionStart + (versionEnd - versionStart))) + "'"),
                this,
                null
            )
        }
        xmlDeclVersion = this.newString(buf, versionStart, versionEnd - versionStart)

        var lastParsedAttr = "version"

        var ch = more()
        var prevCh = ch
        ch = skipS(ch)

        if (ch != 'e' && ch != 's' && ch != '?' && ch != '>') {
            throw XmlPullParserException("unexpected character " + printable(ch.code), this, null)
        }

        if (ch == 'e') {
            if (!isS(prevCh)) {
                throw XmlPullParserException(
                    "expected a space after " + lastParsedAttr + " and not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = requireInput(ch, NCODING)
            ch = skipS(ch)
            if (ch != '=') {
                throw XmlPullParserException(
                    "expected equals sign (=) after encoding and not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = skipS(ch)
            if (ch != '\'' && ch != '"') {
                throw XmlPullParserException(
                    "expected apostrophe (') or quotation mark (\") after encoding and not " + printable(ch.code),
                    this,
                    null
                )
            }
            val quotChar = ch
            val encodingStart = pos
            ch = more()
            if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z')) {
                throw XmlPullParserException(
                    "<?xml encoding name expected to start with [A-Za-z]" + " not " + printable(ch.code), this, null
                )
            }
            ch = more()
            while (ch != quotChar) {
                if ((ch < 'a' || ch > 'z')
                    && (ch < 'A' || ch > 'Z')
                    && (ch < '0' || ch > '9')
                    && ch != '.' && ch != '_' && ch != '-'
                ) {
                    throw XmlPullParserException(
                        "<?xml encoding value expected to be in ([A-Za-z0-9._] | '-')" + " not " + printable(ch.code),
                        this,
                        null
                    )
                }
                ch = more()
            }
            val encodingEnd = pos - 1

            inputEncoding = this.newString(buf, encodingStart, encodingEnd - encodingStart)

            lastParsedAttr = "encoding"

            ch = more()
            prevCh = ch
            ch = skipS(ch)
        }

        if (ch == 's') {
            if (!isS(prevCh)) {
                throw XmlPullParserException(
                    "expected a space after " + lastParsedAttr + " and not " + printable(ch.code), this, null
                )
            }

            ch = more()
            ch = requireInput(ch, TANDALONE)
            ch = skipS(ch)
            if (ch != '=') {
                throw XmlPullParserException(
                    "expected equals sign (=) after standalone and not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = skipS(ch)
            if (ch != '\'' && ch != '"') {
                throw XmlPullParserException(
                    "expected apostrophe (') or quotation mark (\") after standalone and not " + printable(ch.code),
                    this,
                    null
                )
            }
            val quotChar = ch
            ch = more()
            when (ch) {
                'y'  -> {
                    ch = requireInput(ch, YES)
                    xmlDeclStandalone = true
                }
                'n'  -> {
                    ch = requireInput(ch, NO)
                    xmlDeclStandalone = false
                }
                else -> {
                    throw XmlPullParserException(
                        "expected 'yes' or 'no' after standalone and not " + printable(ch.code), this, null
                    )
                }
            }
            if (ch != quotChar) {
                throw XmlPullParserException(
                    "expected " + quotChar + " after standalone value not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = skipS(ch)
        }

        if (ch != '?') {
            throw XmlPullParserException("expected ?> as last part of <?xml not " + printable(ch.code), this, null)
        }
        ch = more()
        if (ch != '>') {
            throw XmlPullParserException("expected ?> as last part of <?xml not " + printable(ch.code), this, null)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseDocdecl() {
        var ch = more()
        if (ch != 'O') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'C') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'T') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'Y') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'P') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'E') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        posStart = pos


        var bracketLevel = 0
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false
        while (true) {
            ch = more()
            if (ch == '[') ++bracketLevel
            else if (ch == ']') --bracketLevel
            else if (ch == '>' && bracketLevel == 0) break
            else if (ch == '&') {
                extractEntityRefInDocDecl()
                continue
            }
            if (normalizeIgnorableWS) {
                when (ch) {
                    '\r' -> {
                        normalizedCR = true
                        if (!usePC) {
                            posEnd = pos - 1
                            if (posEnd > posStart) {
                                joinPC()
                            } else {
                                usePC = true
                                pcEnd = 0
                                pcStart = pcEnd
                            }
                        }
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = '\n'
                    }
                    '\n' -> {
                        if (!normalizedCR && usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        normalizedCR = false
                    }
                    else -> {
                        if (usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = ch
                        }
                        normalizedCR = false
                    }
                }
            }
        }
        posEnd = pos - 1
        text = null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun extractEntityRefInDocDecl() {
        posEnd = pos - 1

        val prevPosStart = posStart
        parseEntityRefInDocDecl()

        posStart = prevPosStart
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun extractEntityRef() {
        posEnd = pos - 1
        if (!usePC) {
            val hadCharData = posEnd > posStart
            if (hadCharData) {
                joinPC()
            } else {
                usePC = true
                pcEnd = 0
                pcStart = pcEnd
            }
        }

        parseEntityRef()
        if (resolvedEntityRefCharBuf.contentEquals(BUF_NOT_RESOLVED)) {
            if (entityRefName == null) {
                entityRefName = this.newString(buf, posStart, posEnd - posStart)
            }
            throw XmlPullParserException(
                "could not resolve entity named '" + printable(entityRefName) + "'", this, null
            )
        }
        for (aResolvedEntity in resolvedEntityRefCharBuf) {
            if (pcEnd >= pc.size) {
                ensurePC(pcEnd)
            }
            pc[pcEnd++] = aResolvedEntity
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCDSect(hadCharData: Boolean) {


        var ch = more()
        if (ch != 'C') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'D') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'A') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'T') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'A') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != '[') throw XmlPullParserException("expected <![CDATA[ for comment start", this, null)

        val cdStart = pos + bufAbsoluteStart
        val curLine = lineNumber
        val curColumn = columnNumber
        val normalizeInput = !tokenize || !roundtripSupported
        try {
            if (normalizeInput) {
                if (hadCharData) {
                    if (!usePC) {
                        if (posEnd > posStart) {
                            joinPC()
                        } else {
                            usePC = true
                            pcEnd = 0
                            pcStart = pcEnd
                        }
                    }
                }
            }
            var seenBracket = false
            var seenBracketBracket = false
            var normalizedCR = false
            while (true) {
                ch = more()
                if (ch == ']') {
                    if (!seenBracket) {
                        seenBracket = true
                    } else {
                        seenBracketBracket = true
                    }
                } else if (ch == '>') {
                    if (seenBracket && seenBracketBracket) {
                        break
                    } else {
                        seenBracketBracket = false
                    }
                    seenBracket = false
                } else {
                    if (seenBracket) {
                        seenBracket = false
                    }
                }
                if (normalizeInput) {
                    when (ch) {
                        '\r' -> {
                            normalizedCR = true
                            posStart = cdStart - bufAbsoluteStart
                            posEnd = pos - 1
                            if (!usePC) {
                                if (posEnd > posStart) {
                                    joinPC()
                                } else {
                                    usePC = true
                                    pcEnd = 0
                                    pcStart = pcEnd
                                }
                            }
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        '\n' -> {
                            if (!normalizedCR && usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = '\n'
                            }
                            normalizedCR = false
                        }
                        else -> {
                            if (usePC) {
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = ch
                            }
                            normalizedCR = false
                        }
                    }
                }
            }
        } catch (ex: EOFException) {
            throw XmlPullParserException(
                "CDATA section started on line $curLine and column $curColumn was not closed",
                this,
                ex
            )
        }
        if (normalizeInput) {
            if (usePC) {
                pcEnd -= 2
            }
        }
        posStart = cdStart - bufAbsoluteStart
        posEnd = pos - 3
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun fillBuf() {
        if (reader == null) throw XmlPullParserException("reader must be set before parsing is started")

        if (bufEnd > bufSoftLimit) {

            val compact = !preventBufferCompaction && (bufStart > bufSoftLimit || bufStart >= buf.size / 2)

            if (compact) {
                buf.copyInto(buf, destinationOffset = 0, startIndex = bufStart, endIndex = bufEnd)
                if (TRACE_SIZING) println(
                    ("TRACE_SIZING fillBuf() compacting " + bufStart + " bufEnd=" + bufEnd + " pos="
                            + pos + " posStart=" + posStart + " posEnd=" + posEnd + " buf first 100 chars:"
                            + buf.concatToString(bufStart, bufStart + min(bufEnd - bufStart, 100)))
                )
            } else {
                val newSize = 2 * buf.size
                val newBuf = CharArray(newSize)
                if (TRACE_SIZING) println("TRACE_SIZING fillBuf() " + buf.size + " => " + newSize)
                buf.copyInto(newBuf, destinationOffset = 0, startIndex = bufStart, endIndex = bufEnd)
                buf = newBuf
                if (bufLoadFactor > 0) {
                    bufSoftLimit = (bufferLoadFactor * buf.size).toInt()
                }
            }
            bufEnd -= bufStart
            pos -= bufStart
            posStart -= bufStart
            posEnd -= bufStart
            bufAbsoluteStart += bufStart
            bufStart = 0
            if (TRACE_SIZING) println(
                ("TRACE_SIZING fillBuf() after bufEnd=" + bufEnd + " pos=" + pos + " posStart="
                        + posStart + " posEnd=" + posEnd + " buf first 100 chars:"
                        + buf.concatToString(0, 0 + min(bufEnd, 100)))
            )
        }
        val len = min(buf.size - bufEnd, READ_CHUNK_SIZE)
        val ret: Int = reader!!.read(buf, bufEnd, len)
        if (ret > 0) {
            bufEnd += ret
            if (TRACE_SIZING) println(
                ("TRACE_SIZING fillBuf() after filling in buffer" + " buf first 100 chars:"
                        + buf.concatToString(0, 0 + min(bufEnd, 100)))
            )

            return
        }
        if (ret == -1) {
            if (bufAbsoluteStart == 0 && pos == 0) {
                throw EOFException("input contained no data")
            } else {
                if (seenRoot && depth == 0) {
                    reachedEnd = true
                    return
                } else {
                    val expectedTagStack = StringBuilder()
                    if (depth > 0) {
                        if (depth >= elRawName.size || elRawName[depth] == null || elRawName[depth]?.size == 0) {
                            val offset = posStart + 1
                            val tagName = buf.concatToString(offset, offset + (pos - posStart - 1))
                            expectedTagStack
                                .append(" - expected the opening tag <")
                                .append(tagName)
                                .append("...>")
                        } else {
                            expectedTagStack.append(" - expected end tag")
                            if (depth > 1) {
                                expectedTagStack.append("s")
                            }
                            expectedTagStack.append(" ")

                            for (i in depth downTo 1) {
                                if (elRawName[i] == null) {
                                    val offset = posStart + 1
                                    val tagName = buf.concatToString(offset, offset + (pos - posStart - 1))
                                    expectedTagStack
                                        .append(" - expected the opening tag <")
                                        .append(tagName)
                                        .append("...>")
                                } else {
                                    val tagName = elRawName[i]!!.concatToString(0, 0 + elRawNameEnd[i])
                                    expectedTagStack
                                        .append("</")
                                        .append(tagName)
                                        .append('>')
                                }
                            }
                            expectedTagStack.append(" to close")
                            for (i in depth downTo 1) {
                                if (i != depth) {
                                    expectedTagStack.append(" and")
                                }
                                if (elRawName[i] == null) {
                                    val offset = posStart + 1
                                    val tagName = buf.concatToString(offset, offset + (pos - posStart - 1))
                                    expectedTagStack
                                        .append(" start tag <")
                                        .append(tagName)
                                        .append(">")
                                    expectedTagStack.append(" from line ").append(elRawNameLine[i])
                                } else {
                                    val tagName = elRawName[i]!!.concatToString(0, 0 + elRawNameEnd[i])
                                    expectedTagStack
                                        .append(" start tag <")
                                        .append(tagName)
                                        .append(">")
                                    expectedTagStack.append(" from line ").append(elRawNameLine[i])
                                }
                            }
                            expectedTagStack.append(", parser stopped on")
                        }
                    }
                    throw EOFException(
                        "no more data available" + expectedTagStack.toString() + getPositionDescription()
                    )
                }
            }
        } else {
            throw IOException("error reading input, returned $ret")
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun more(): Char {
        if (pos >= bufEnd) {
            fillBuf()
            if (reachedEnd) throw EOFException("no more data available" + getPositionDescription())
        }
        val ch = buf[pos++]
        if (ch == '\n') {
            ++lineNumber
            columnNumber = 1
        } else {
            ++columnNumber
        }
        return ch
    }

    private fun ensurePC(end: Int) {
        val newSize = if (end > READ_CHUNK_SIZE) 2 * end else 2 * READ_CHUNK_SIZE
        val newPC = CharArray(newSize)
        if (TRACE_SIZING) println("TRACE_SIZING ensurePC() " + pc.size + " ==> " + newSize + " end=" + end)
        pc.copyInto(newPC, destinationOffset = 0, startIndex = 0, endIndex = pcEnd)
        pc = newPC
    }

    private fun joinPC() {
        val len = posEnd - posStart
        val newEnd = pcEnd + len + 1
        if (newEnd >= pc.size) ensurePC(newEnd)
        buf.copyInto(pc, destinationOffset = pcEnd, startIndex = posStart, endIndex = posStart + len)
        pcEnd += len
        usePC = true
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun requireInput(ch: Char, input: CharArray): Char {
        var ch = ch
        for (anInput in input) {
            if (ch != anInput) {
                throw XmlPullParserException(
                    "expected " + printable(anInput.code) + " in " + input.concatToString() + " and not " + printable(ch.code),
                    this,
                    null
                )
            }
            ch = more()
        }
        return ch
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skipS(ch: Char): Char {
        var ch = ch
        while (isS(ch)) {
            ch = more()
        }
        return ch
    }

    companion object {
        private const val XML_URI = "http://www.w3.org/XML/1998/namespace"

        private const val XMLNS_URI = "http://www.w3.org/2000/xmlns/"

        private const val FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip"

        private const val FEATURE_NAMES_INTERNED = "http://xmlpull.org/v1/doc/features.html#names-interned"

        private const val PROPERTY_XMLDECL_VERSION = "http://xmlpull.org/v1/doc/properties.html#xmldecl-version"

        private const val PROPERTY_XMLDECL_STANDALONE = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone"

        private const val PROPERTY_XMLDECL_CONTENT = "http://xmlpull.org/v1/doc/properties.html#xmldecl-content"

        private const val PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location"

        private const val TRACE_SIZING = false


        private fun fastHash(ch: CharArray?, off: Int, len: Int): Int {
            if (len == 0) return 0
            var hash = ch!![off].code
            hash = (hash shl 7) + ch[off + len - 1].code
            if (len > 16) hash = (hash shl 7) + ch[off + (len / 4)].code
            if (len > 8) hash = (hash shl 7) + ch[off + (len / 2)].code
            return hash
        }

        private const val READ_CHUNK_SIZE = 8 * 1024
        private fun findFragment(bufMinPos: Int, b: CharArray, start: Int, end: Int): Int {
            var start = start
            if (start < bufMinPos) {
                start = bufMinPos
                if (start > end) start = end
                return start
            }
            if (end - start > 65) {
                start = end - 10
            }
            var i = start + 1
            while (--i > bufMinPos) {
                if ((end - i) > 65) break
                val c = b[i]
                if (c == '<' && (start - i) > 10) break
            }
            return i
        }

        private val BUF_NOT_RESOLVED = CharArray(0)

        private val BUF_LT = charArrayOf('<')
        private val BUF_AMP = charArrayOf('&')
        private val BUF_GT = charArrayOf('>')
        private val BUF_APO = charArrayOf('\'')
        private val BUF_QUOT = charArrayOf('"')

        private fun isValidCodePoint(codePoint: Int): Boolean {
            return codePoint == 0x9 ||
                    codePoint == 0xA ||
                    codePoint == 0xD ||
                    codePoint in 0x20..0xD7FF ||
                    codePoint in 0xE000..0xFFFD ||
                    codePoint in 0x10000..0x10FFFF
        }

        private val VERSION = "version".toCharArray()

        private val NCODING = "ncoding".toCharArray()

        private val TANDALONE = "tandalone".toCharArray()

        private val YES = "yes".toCharArray()

        private val NO = "no".toCharArray()

        private const val LOOKUP_MAX = 0x400

        private const val LOOKUP_MAX_CHAR = LOOKUP_MAX.toChar()

        private val lookupNameStartChar = BooleanArray(LOOKUP_MAX)

        private val lookupNameChar = BooleanArray(LOOKUP_MAX)

        private fun setName(ch: Char) {
            lookupNameChar[ch.code] = true
        }

        private fun setNameStart(ch: Char) {
            lookupNameStartChar[ch.code] = true
            setName(ch)
        }

        init {
            setNameStart(':')
            run {
                var ch = 'A'
                while (ch <= 'Z') {
                    setNameStart(ch)
                    ++ch
                }
            }
            setNameStart('_')
            run {
                var ch = 'a'
                while (ch <= 'z') {
                    setNameStart(ch)
                    ++ch
                }
            }
            run {
                var ch = '\u00c0'
                while (ch <= '\u02FF') {
                    setNameStart(ch)
                    ++ch
                }
            }
            run {
                var ch = '\u0370'
                while (ch <= '\u037d') {
                    setNameStart(ch)
                    ++ch
                }
            }
            run {
                var ch = '\u037f'
                while (ch < '\u0400') {
                    setNameStart(ch)
                    ++ch
                }
            }

            setName('-')
            setName('.')
            run {
                var ch = '0'
                while (ch <= '9') {
                    setName(ch)
                    ++ch
                }
            }
            setName('\u00b7')
            var ch = '\u0300'
            while (ch <= '\u036f') {
                setName(ch)
                ++ch
            }
        }

        private fun isNameStartChar(ch: Char): Boolean {
            return if (ch < LOOKUP_MAX_CHAR)
                lookupNameStartChar[ch.code]
            else
                ch <= '\u2027' ||
                        ch in '\u202A'..'\u218F' ||
                        ch in '\u2800'..'\uFFEF'


        }

        private fun isNameChar(ch: Char): Boolean {


            return if (ch < LOOKUP_MAX_CHAR)
                lookupNameChar[ch.code]
            else
                ch <= '\u2027' ||
                        ch in '\u202A'..'\u218F' ||
                        ch in '\u2800'..'\uFFEF'


        }

        private fun isS(ch: Char): Boolean {
            return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t')
        }

        private fun printable(ch: Int): String = when {
            ch == '\n'.code     -> "\\n"
            ch == '\r'.code     -> "\\r"
            ch == '\t'.code     -> "\\t"
            ch == '\''.code     -> "\\'"
            ch > 127 || ch < 32 -> "\\u" + ch.toString(16).padStart(4, '0')
            isBmpCodePoint(ch)  -> ch.toChar().toString()
            else                -> charArrayOf(highSurrogate(ch), lowSurrogate(ch)).concatToString()
        }

        private fun printable(s: String?): String? {
            var s = s ?: return null
            val sLen: Int = s.codePointCount(0, s.length)
            val buf = StringBuilder(sLen + 10)
            for (i in 0..<sLen) {
                buf.append(printable(s.codePointAt(i)))
            }
            s = buf.toString()
            return s
        }
    }
}