package tl.jake.ktree

/**
 * Print trees to strings (and other string-shaped things)
 */
data class Formatter(val settings: TreeNotation) {
    interface Output {
        fun write(string: String): Unit
    }

    private class StringOutput : Output {
        val buffer = StringBuilder()
        override fun write(string: String) {
            buffer.append(string)
        }
    }

    fun format(tree: Tree): String {
        val output = StringOutput()
        print(tree, output)
        return output.buffer.substring(0)
    }

    fun print(tree: Tree, out: Output) {
        when (tree) {
            is Tree.Root -> printRoot(tree, out)
            is Tree.Node -> printNode(tree, out)
        }
    }

    private fun printRoot(root: Tree.Root, out: Output) {
        root.children.forEach { this.print(it, out) }
    }

    private fun printNode(node: Tree.Node, out: Output) {
        // Edge
        val edge = settings.edgeSymbol ?: ""
        repeat(node.depth) { out.write(edge) }

        // Words
        node.cells.forEachIndexed { i, word ->
            out.write(word)
            if (i < node.cells.size - 1) {
                out.write(settings.wordBreakSymbol)
            }
        }

        // Children
        out.write(settings.nodeBreakSymbol)
        node.children.forEach { print(it, out) }
    }
}