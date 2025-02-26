package io.github.xmlpullkmp

import com.fleeksoft.io.Reader
import com.fleeksoft.io.exception.IOException

object TestUtilsCommon {
    @Throws(IOException::class)
    fun readAllFrom(input: Reader): String {
        val output: StringBuilder = StringBuilder()
        val buffer = CharArray(16384)
        var n = 0
        while (0 <= (input.read(buffer).also { n = it })) {
            output.appendRange(buffer, 0, 0 + n)
        }
//        output.flush()
        return output.toString()
    }

    /**
     *
     *
     * How many times is the substring in the larger String.
     *
     *
     *
     * `null` returns `0`.
     *
     *
     * @param str the String to check
     * @param sub the substring to count
     * @return the number of occurrences, 0 if the String is `null`
     * @throws NullPointerException if sub is `null`
     */
    fun countMatches(str: String?, sub: String): Int {
        if (sub.isEmpty()) {
            return 0
        }
        if (str == null) {
            return 0
        }
        var count = 0
        var idx = 0
        while ((str.indexOf(sub, idx).also { idx = it }) != -1) {
            count++
            idx += sub.length
        }
        return count
    }
}