data class NotationSettings(
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
    val lineBreak: String = "\n"
)

/**
 * A tree, as described by Tree Notation.
 */
sealed class Tree {
    abstract val children: List<Node>
    abstract val parent: Tree?
    abstract val astNode: AST?
    abstract val indent: Int
    abstract val warnings: List<Warning>

    protected fun childrenToString(): String {
        if (children.isEmpty()) return "[]"
        return children.joinToString(
            separator = "\n",
            prefix = "[\n",
            postfix = "\n]"
        ) { "\t${it}" }
    }

    fun allWarnings(): List<Warning> {
        return warnings + children.flatMap { it.allWarnings() }
    }

    /**
     * A tree Root never has a parent, and is un-indented.
     */
    data class Root(
        override val astNode: AST.File?,
        override val children: MutableList<Node> = mutableListOf<Node>()
    ) : Tree() {
        override val parent: Tree? = null
        override val indent = -1
        override val warnings = mutableListOf<Warning>()
        override fun toString(): String {
            return "Tree.Root(indent=$indent, children=${childrenToString()}"
        }
    }

    sealed class Node : Tree() {
        abstract val cells: List<String>
        override val children = mutableListOf<Node>()
        override val warnings = mutableListOf<Warning>()

        /**
         * A tree node with no words.
         * This is typically an empty line.
         */
        data class Empty(
            override val parent: Tree?,
            override val astNode: AST.TreeNode?,
            override val indent: Int,
        ) : Node() {
            override val cells = listOf<String>()
            override fun toString(): String {
                return "Node.Empty(indent=$indent, children=${childrenToString()})"
            }
        }

        /**
         * A tree node where the first word is blank ("").
         * Possibly the result of over-indenting a child.
         */
        data class Blank(
            override val parent: Tree?,
            override val astNode: AST.TreeNode?,
            override val indent: Int,
            override val cells: List<String>
        ) : Node() {
            override fun toString(): String {
                return "Node.Blank(indent=$indent, cells=$cells children=${childrenToString()})"
            }
        }

        /**
         * A tree node with words in it.
         */
        data class Typed(
            override val parent: Tree?,
            override val astNode: AST.TreeNode?,
            override val indent: Int,
            val type: String,
            val content: List<String>
        ) : Node() {
            override val cells: List<String> = listOf(type) + content
            override fun toString(): String {
                return "Node.Typed(indent=$indent, type=$type, content=$content children=${childrenToString()})"
            }
        }

        companion object {
            /**
             * Create a Node, picking the Node subtype based on `cells`.
             */
            fun create(
                parent: Tree?,
                astNode: AST.TreeNode?,
                indent: Int,
                cells: List<String>
            ): Node = when {
                cells.isEmpty() -> Empty(parent, astNode, indent)
                cells[0] == "" -> Blank(parent, astNode, indent, cells)
                else -> Typed(
                    parent,
                    astNode,
                    indent,
                    cells[0],
                    cells.slice(1 until cells.size)
                )
            }
        }
    }
}
