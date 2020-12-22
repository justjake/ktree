data class TreeOutline(
    val space: String = " ",
    val skip: String = "│",
    val dash: String = "─",
    val child: String = "├",
    val lastChild: String = "└",
    val lineBreak: String = "\n",
    val renderIndex: (Int)->String = fun (i) = "$i. "
) {
    private val childPrefix = child + dash + space
    private val lastChildPrefix = lastChild + dash + space
    private val skipPrefix = skip + space + space
    private val lastChildSkipPrefix = space + space + space

    fun format(tree: Tree): String = StringBuilder().let {
        writeToBuilder(tree, "", it)
        // Omit final \n
        it.substring(0, it.length - 1)
    }

    private fun writeToBuilder(tree: Tree, prefix: String, builder: StringBuilder) {
        if (tree is Tree.Node) {
            builder.append(tree.cells)
            builder.append(lineBreak)
        }

        tree.children.forEachIndexed { index, node ->
            var nodePrefix = childPrefix
            var childPrefixAddition = skipPrefix
            if (index == tree.children.size - 1) {
                nodePrefix = lastChildPrefix
                childPrefixAddition = lastChildSkipPrefix
            }
            builder.append(prefix)
            builder.append(nodePrefix)
            builder.append(renderIndex(index))
            writeToBuilder(node, prefix + childPrefixAddition, builder)
        }
    }
}

fun Tree.toOutline(outline: TreeOutline = TreeOutline()): String = outline.format(this)
