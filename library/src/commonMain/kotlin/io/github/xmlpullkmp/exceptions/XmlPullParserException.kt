package io.github.xmlpullkmp.exceptions

import io.github.xmlpullkmp.XmlPullParser

class XmlPullParserException : Exception {
    protected var detail: Throwable? = null

    var lineNumber: Int = -1
        protected set

    var columnNumber: Int = -1
        protected set

    constructor(s: String?) : super(s)

    constructor(msg: String?, parser: XmlPullParser?, chain: Throwable?) : super(
        ((if (msg == null) "" else "$msg ")
                + (if (parser == null) "" else "(position:" + parser.getPositionDescription() + ") ")
                + (if (chain == null) "" else "caused by: $chain")),
        chain
    ) {
        if (parser != null) {
            this.lineNumber = parser.getLineNumber()
            this.columnNumber = parser.getColumnNumber()
        }
        this.detail = chain
    }
}