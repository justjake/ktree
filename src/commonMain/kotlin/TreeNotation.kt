package tl.jake.ktree

import kotlinx.serialization.*
import kotlinx.serialization.json.Json


/**
 * A tree notation variant. Describes how to parse a string
 * of indented lines into a Tree.
 * https://treenotation.org/
 */
@Serializable
data class TreeNotation(
    /**
     * nodeBreakSymbol delimits nodes.
     */
    val nodeBreakSymbol: String = "\n",

    /**
     * wordBreakSymbol delimits words in the node string.
     */
    val wordBreakSymbol: String = "\t",

    /**
     * edgeSymbol is used to indicate the parent/child relationship between nodes.
     * Nodes prefixed with the same number of edge symbols are siblings.
     * If null, parse as Grid Notation, with no parent-child concepts.
     */
    val edgeSymbol: String? = "\t",

    /**
     * Used for line breaking for error reporting. Not used for parsing.
     */
    val lineBreak: String = "\n",

    /**
     * How over-indentation is handled by the parser.
     */
    val overIndentBehavior: OverIndentBehavior = OverIndentBehavior.Strict,
) {
    @Serializable
    enum class OverIndentBehavior {
        @SerialName("strict")
        Strict,

        @SerialName("equally-indented-children-are-siblings")
        EquallyIndentedChildrenAreSiblings
    }

    init {
        if (nodeBreakSymbol == wordBreakSymbol || nodeBreakSymbol == edgeSymbol) {
            throw IllegalArgumentException("nodeBreakSymbol must be distinct from other symbols")
        }
    }


    companion object {
        /** One-space to indent and to separate words */
        val Spaces = TreeNotation(wordBreakSymbol = " ", edgeSymbol = " ")

        /** Tab to indent and space to separate words */
        val SpaceCells = TreeNotation(wordBreakSymbol = " ")

        /**
         * Tab to indent and tab to separate words.
         * Somewhat compatible with TSV files.
         */
        val Tabs = TreeNotation()

        /** Grid notation - no nesting, very similar to TSV. */
        val GridNotation = TreeNotation(edgeSymbol = null)

        private fun String.escaped() = Json.encodeToString(this)

        fun parseFromHashBang(data: String): Pair<TreeNotation, String>? {
            val ShebangRegex = Regex("""^#!\s*(\w+)(\s+(.*))?$""")
            val lines = data.lines()
            val match = ShebangRegex.find(lines.first()) ?: return null
            val args = if (match.groups.size > 2) match.groups[2]!!.value else ""
            val notation = Json { isLenient = true }.decodeFromString<TreeNotation>(args)
            val newContent = lines.subList(1, lines.size).joinToString("\n")
            println(notation)
            println(newContent.escaped())
            return Pair(notation, newContent)
        }
    }

    private fun String.escaped() = Json.encodeToString(this)

    override fun toString(): String {
        return "TreeNotation(nodeBreakSymbol=${nodeBreakSymbol.escaped()}, wordBreakSymbol=${wordBreakSymbol.escaped()}, edgeSymbol=${edgeSymbol?.escaped()}, lineBreak=${lineBreak.escaped()}, overIndentBehavior=$overIndentBehavior)"
    }
}

