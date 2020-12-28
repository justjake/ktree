package tl.jake.ktree

/**
 * Call on the result of a when {} to use the when {} as an expression,
 * which forces it to be exhaustive.
 * https://proandroiddev.com/til-when-is-when-exhaustive-31d69f630a8b
 */
val <T> T.exhaustive: T
    get() = this

fun <T> T.prettyToString(indentWidth: Int = 4): String {
    var indentLevel = 0
    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()
    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                indentLevel++
                stringBuilder.appendLine(char).append(padding())
            }
            ')', ']', '}' -> {
                indentLevel--
                stringBuilder.appendLine().append(padding()).append(char)
            }
            ',' -> {
                stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }
            else -> {
                stringBuilder.append(char)
            }
        }
        i++
    }

    return stringBuilder.toString()
}
