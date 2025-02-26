package io.github.xmlpullkmp

import com.fleeksoft.io.exception.ArrayIndexOutOfBoundsException

class EntityReplacementMap(replacements: Array<Array<String>>) {
    val entityName: Array<String?> = arrayOfNulls(replacements.size)
    val entityNameBuf: Array<CharArray?> = arrayOfNulls(replacements.size)
    val entityReplacement: Array<String?> = arrayOfNulls(replacements.size)
    val entityReplacementBuf: Array<CharArray?> = arrayOfNulls(replacements.size)
    var entityEnd: Int = 0
    val entityNameHash: IntArray = IntArray(replacements.size)

    init {
        for (replacement in replacements) {
            defineEntityReplacementText(replacement[0], replacement[1])
        }
    }

    private fun defineEntityReplacementText(entityName: String, replacementText: String) {
        var replacementTextCopy = replacementText
        if (!replacementTextCopy.startsWith("&#") && this.entityName.isNotEmpty() && replacementTextCopy.length > 1) {
            val tmp = replacementTextCopy.substring(1, replacementTextCopy.length - 1)
            for (i in this.entityName.indices) {
                if (this.entityName[i] != null && this.entityName[i] == tmp) {
                    replacementTextCopy = this.entityReplacement[i]!!
                }
            }
        }

        if (entityEnd >= this.entityName.size) {
            throw ArrayIndexOutOfBoundsException("Entity end index $entityEnd out of bounds for length ${this.entityName.size}")
        }

        val entityNameCharData = entityName.toCharArray()
        this.entityName[entityEnd] = entityNameCharData.concatToString(0, entityName.length)
        entityNameBuf[entityEnd] = entityNameCharData

        entityReplacement[entityEnd] = replacementTextCopy
        entityReplacementBuf[entityEnd] = replacementTextCopy.toCharArray()
        entityNameHash[entityEnd] = fastHash(entityNameBuf[entityEnd], 0, entityNameBuf[entityEnd]!!.size)
        ++entityEnd
    }

    companion object {
        private fun fastHash(ch: CharArray?, off: Int, len: Int): Int {
            if (len == 0) return 0
            var hash = ch!![off].code // hash at beginning
            hash = (hash shl 7) + ch[off + len - 1].code // hash at the end
            if (len > 16) hash = (hash shl 7) + ch[off + (len / 4)].code // 1/4 from beginning

            if (len > 8) hash = (hash shl 7) + ch[off + (len / 2)].code // 1/2 of string size ...
            return hash
        }

        val defaultEntityReplacementMap: EntityReplacementMap = EntityReplacementMap(
            arrayOf(
                arrayOf("nbsp", "\u00a0"),
                arrayOf("iexcl", "\u00a1"),
                arrayOf("cent", "\u00a2"),
                arrayOf("pound", "\u00a3"),
                arrayOf("curren", "\u00a4"),
                arrayOf("yen", "\u00a5"),
                arrayOf("brvbar", "\u00a6"),
                arrayOf("sect", "\u00a7"),
                arrayOf("uml", "\u00a8"),
                arrayOf("copy", "\u00a9"),
                arrayOf("ordf", "\u00aa"),
                arrayOf("laquo", "\u00ab"),
                arrayOf("not", "\u00ac"),
                arrayOf("shy", "\u00ad"),
                arrayOf("reg", "\u00ae"),
                arrayOf("macr", "\u00af"),
                arrayOf("deg", "\u00b0"),
                arrayOf("plusmn", "\u00b1"),
                arrayOf("sup2", "\u00b2"),
                arrayOf("sup3", "\u00b3"),
                arrayOf("acute", "\u00b4"),
                arrayOf("micro", "\u00b5"),
                arrayOf("para", "\u00b6"),
                arrayOf("middot", "\u00b7"),
                arrayOf("cedil", "\u00b8"),
                arrayOf("sup1", "\u00b9"),
                arrayOf("ordm", "\u00ba"),
                arrayOf("raquo", "\u00bb"),
                arrayOf("frac14", "\u00bc"),
                arrayOf("frac12", "\u00bd"),
                arrayOf("frac34", "\u00be"),
                arrayOf("iquest", "\u00bf"),
                arrayOf("Agrave", "\u00c0"),
                arrayOf("Aacute", "\u00c1"),
                arrayOf("Acirc", "\u00c2"),
                arrayOf("Atilde", "\u00c3"),
                arrayOf("Auml", "\u00c4"),
                arrayOf("Aring", "\u00c5"),
                arrayOf("AElig", "\u00c6"),
                arrayOf("Ccedil", "\u00c7"),
                arrayOf("Egrave", "\u00c8"),
                arrayOf("Eacute", "\u00c9"),
                arrayOf("Ecirc", "\u00ca"),
                arrayOf("Euml", "\u00cb"),
                arrayOf("Igrave", "\u00cc"),
                arrayOf("Iacute", "\u00cd"),
                arrayOf("Icirc", "\u00ce"),
                arrayOf("Iuml", "\u00cf"),
                arrayOf("ETH", "\u00d0"),
                arrayOf("Ntilde", "\u00d1"),
                arrayOf("Ograve", "\u00d2"),
                arrayOf("Oacute", "\u00d3"),
                arrayOf("Ocirc", "\u00d4"),
                arrayOf("Otilde", "\u00d5"),
                arrayOf("Ouml", "\u00d6"),
                arrayOf("times", "\u00d7"),
                arrayOf("Oslash", "\u00d8"),
                arrayOf("Ugrave", "\u00d9"),
                arrayOf("Uacute", "\u00da"),
                arrayOf("Ucirc", "\u00db"),
                arrayOf("Uuml", "\u00dc"),
                arrayOf("Yacute", "\u00dd"),
                arrayOf("THORN", "\u00de"),
                arrayOf("szlig", "\u00df"),
                arrayOf("agrave", "\u00e0"),
                arrayOf("aacute", "\u00e1"),
                arrayOf("acirc", "\u00e2"),
                arrayOf("atilde", "\u00e3"),
                arrayOf("auml", "\u00e4"),
                arrayOf("aring", "\u00e5"),
                arrayOf("aelig", "\u00e6"),
                arrayOf("ccedil", "\u00e7"),
                arrayOf("egrave", "\u00e8"),
                arrayOf("eacute", "\u00e9"),
                arrayOf("ecirc", "\u00ea"),
                arrayOf("euml", "\u00eb"),
                arrayOf("igrave", "\u00ec"),
                arrayOf("iacute", "\u00ed"),
                arrayOf("icirc", "\u00ee"),
                arrayOf("iuml", "\u00ef"),
                arrayOf("eth", "\u00f0"),
                arrayOf("ntilde", "\u00f1"),
                arrayOf("ograve", "\u00f2"),
                arrayOf("oacute", "\u00f3"),
                arrayOf("ocirc", "\u00f4"),
                arrayOf("otilde", "\u00f5"),
                arrayOf("ouml", "\u00f6"),
                arrayOf("divide", "\u00f7"),
                arrayOf("oslash", "\u00f8"),
                arrayOf("ugrave", "\u00f9"),
                arrayOf("uacute", "\u00fa"),
                arrayOf("ucirc", "\u00fb"),
                arrayOf("uuml", "\u00fc"),
                arrayOf("yacute", "\u00fd"),
                arrayOf("thorn", "\u00fe"),
                arrayOf("yuml", "\u00ff"),
                // ----------------------------------------------------------------------
                // Special entities
                // ----------------------------------------------------------------------

                arrayOf("OElig", "\u0152"),
                arrayOf("oelig", "\u0153"),
                arrayOf("Scaron", "\u0160"),
                arrayOf("scaron", "\u0161"),
                arrayOf("Yuml", "\u0178"),
                arrayOf("circ", "\u02c6"),
                arrayOf("tilde", "\u02dc"),
                arrayOf("ensp", "\u2002"),
                arrayOf("emsp", "\u2003"),
                arrayOf("thinsp", "\u2009"),
                arrayOf("zwnj", "\u200c"),
                arrayOf("zwj", "\u200d"),
                arrayOf("lrm", "\u200e"),
                arrayOf("rlm", "\u200f"),
                arrayOf("ndash", "\u2013"),
                arrayOf("mdash", "\u2014"),
                arrayOf("lsquo", "\u2018"),
                arrayOf("rsquo", "\u2019"),
                arrayOf("sbquo", "\u201a"),
                arrayOf("ldquo", "\u201c"),
                arrayOf("rdquo", "\u201d"),
                arrayOf("bdquo", "\u201e"),
                arrayOf("dagger", "\u2020"),
                arrayOf("Dagger", "\u2021"),
                arrayOf("permil", "\u2030"),
                arrayOf("lsaquo", "\u2039"),
                arrayOf("rsaquo", "\u203a"),
                arrayOf("euro", "\u20ac"),
                // ----------------------------------------------------------------------
                // Symbol entities
                // ----------------------------------------------------------------------

                arrayOf("fnof", "\u0192"),
                arrayOf("Alpha", "\u0391"),
                arrayOf("Beta", "\u0392"),
                arrayOf("Gamma", "\u0393"),
                arrayOf("Delta", "\u0394"),
                arrayOf("Epsilon", "\u0395"),
                arrayOf("Zeta", "\u0396"),
                arrayOf("Eta", "\u0397"),
                arrayOf("Theta", "\u0398"),
                arrayOf("Iota", "\u0399"),
                arrayOf("Kappa", "\u039a"),
                arrayOf("Lambda", "\u039b"),
                arrayOf("Mu", "\u039c"),
                arrayOf("Nu", "\u039d"),
                arrayOf("Xi", "\u039e"),
                arrayOf("Omicron", "\u039f"),
                arrayOf("Pi", "\u03a0"),
                arrayOf("Rho", "\u03a1"),
                arrayOf("Sigma", "\u03a3"),
                arrayOf("Tau", "\u03a4"),
                arrayOf("Upsilon", "\u03a5"),
                arrayOf("Phi", "\u03a6"),
                arrayOf("Chi", "\u03a7"),
                arrayOf("Psi", "\u03a8"),
                arrayOf("Omega", "\u03a9"),
                arrayOf("alpha", "\u03b1"),
                arrayOf("beta", "\u03b2"),
                arrayOf("gamma", "\u03b3"),
                arrayOf("delta", "\u03b4"),
                arrayOf("epsilon", "\u03b5"),
                arrayOf("zeta", "\u03b6"),
                arrayOf("eta", "\u03b7"),
                arrayOf("theta", "\u03b8"),
                arrayOf("iota", "\u03b9"),
                arrayOf("kappa", "\u03ba"),
                arrayOf("lambda", "\u03bb"),
                arrayOf("mu", "\u03bc"),
                arrayOf("nu", "\u03bd"),
                arrayOf("xi", "\u03be"),
                arrayOf("omicron", "\u03bf"),
                arrayOf("pi", "\u03c0"),
                arrayOf("rho", "\u03c1"),
                arrayOf("sigmaf", "\u03c2"),
                arrayOf("sigma", "\u03c3"),
                arrayOf("tau", "\u03c4"),
                arrayOf("upsilon", "\u03c5"),
                arrayOf("phi", "\u03c6"),
                arrayOf("chi", "\u03c7"),
                arrayOf("psi", "\u03c8"),
                arrayOf("omega", "\u03c9"),
                arrayOf("thetasym", "\u03d1"),
                arrayOf("upsih", "\u03d2"),
                arrayOf("piv", "\u03d6"),
                arrayOf("bull", "\u2022"),
                arrayOf("hellip", "\u2026"),
                arrayOf("prime", "\u2032"),
                arrayOf("Prime", "\u2033"),
                arrayOf("oline", "\u203e"),
                arrayOf("frasl", "\u2044"),
                arrayOf("weierp", "\u2118"),
                arrayOf("image", "\u2111"),
                arrayOf("real", "\u211c"),
                arrayOf("trade", "\u2122"),
                arrayOf("alefsym", "\u2135"),
                arrayOf("larr", "\u2190"),
                arrayOf("uarr", "\u2191"),
                arrayOf("rarr", "\u2192"),
                arrayOf("darr", "\u2193"),
                arrayOf("harr", "\u2194"),
                arrayOf("crarr", "\u21b5"),
                arrayOf("lArr", "\u21d0"),
                arrayOf("uArr", "\u21d1"),
                arrayOf("rArr", "\u21d2"),
                arrayOf("dArr", "\u21d3"),
                arrayOf("hArr", "\u21d4"),
                arrayOf("forall", "\u2200"),
                arrayOf("part", "\u2202"),
                arrayOf("exist", "\u2203"),
                arrayOf("empty", "\u2205"),
                arrayOf("nabla", "\u2207"),
                arrayOf("isin", "\u2208"),
                arrayOf("notin", "\u2209"),
                arrayOf("ni", "\u220b"),
                arrayOf("prod", "\u220f"),
                arrayOf("sum", "\u2211"),
                arrayOf("minus", "\u2212"),
                arrayOf("lowast", "\u2217"),
                arrayOf("radic", "\u221a"),
                arrayOf("prop", "\u221d"),
                arrayOf("infin", "\u221e"),
                arrayOf("ang", "\u2220"),
                arrayOf("and", "\u2227"),
                arrayOf("or", "\u2228"),
                arrayOf("cap", "\u2229"),
                arrayOf("cup", "\u222a"),
                arrayOf("int", "\u222b"),
                arrayOf("there4", "\u2234"),
                arrayOf("sim", "\u223c"),
                arrayOf("cong", "\u2245"),
                arrayOf("asymp", "\u2248"),
                arrayOf("ne", "\u2260"),
                arrayOf("equiv", "\u2261"),
                arrayOf("le", "\u2264"),
                arrayOf("ge", "\u2265"),
                arrayOf("sub", "\u2282"),
                arrayOf("sup", "\u2283"),
                arrayOf("nsub", "\u2284"),
                arrayOf("sube", "\u2286"),
                arrayOf("supe", "\u2287"),
                arrayOf("oplus", "\u2295"),
                arrayOf("otimes", "\u2297"),
                arrayOf("perp", "\u22a5"),
                arrayOf("sdot", "\u22c5"),
                arrayOf("lceil", "\u2308"),
                arrayOf("rceil", "\u2309"),
                arrayOf("lfloor", "\u230a"),
                arrayOf("rfloor", "\u230b"),
                arrayOf("lang", "\u2329"),
                arrayOf("rang", "\u232a"),
                arrayOf("loz", "\u25ca"),
                arrayOf("spades", "\u2660"),
                arrayOf("clubs", "\u2663"),
                arrayOf("hearts", "\u2665"),
                arrayOf("diams", "\u2666")
            )
        )
    }
}