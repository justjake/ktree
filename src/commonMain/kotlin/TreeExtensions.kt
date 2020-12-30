package tl.jake.ktree

// Transforms for working with child data
fun Tree.Node.hasPrefix(type: String, vararg dataPrefix: String): Boolean =
    typeCell == type &&
            dataCells.size >= dataPrefix.size &&
            dataCells.subList(0, dataPrefix.size) == dataPrefix.toList()


fun Tree.getChild(type: String, vararg dataPrefix: String): Tree.Node? = children.find {
    it.hasPrefix(type, *dataPrefix)
}

fun Tree.getChildData(type: String, vararg dataPrefix: String): Tree.Node? =
    getChild(type, *dataPrefix)?.cloneData(*dataPrefix)

fun Tree.setChildData(type: String, vararg dataPrefix: String, node: Tree.Node) {
    val newNode = when {
        node.hasPrefix(type, *dataPrefix) -> node
        else -> node.cloneWith(cells = false) {
            cell(type)
            cells(*dataPrefix)
            cells(*node.cells.toTypedArray())
        }
    }

    return when (val child = getChild(type, *dataPrefix)) {
        null -> this.addChild(newNode)
        else -> this.addChild(newNode, AddingAt.Replace(child))
    }
}

fun Tree.removeChildData(type: String, vararg dataPrefix: String): Tree.Node? {
    val existing = getChild(type, *dataPrefix) ?: return null
    val result = existing.cloneData(type, *dataPrefix)
    setChildData(type, *dataPrefix, node = NodeBuilder.build())
    return result
}

fun Tree.removeChild(type: String, vararg dataPrefix: String) {
    val child = getChild(type, *dataPrefix)
    if (child != null) {
        removeChild(child)
    }
}

// TODO: rename to cloneWithoutPrefix?
fun Tree.Node.cloneData(vararg omitDataPrefix: String): Tree.Node =
    cloneWith(cells = false) {
        this@cloneData.dataCells.forEachIndexed { index, s ->
            when {
                index >= omitDataPrefix.size -> cell(s)
                index < omitDataPrefix.size -> {
                    if (s != omitDataPrefix[index]) {
                        cell(s)
                    }
                }
            }
        }
    }

fun Tree.Node.cloneData(startAt: Int): Tree.Node =
    cloneWith(cells = false) {
        this@cloneData.cells.forEachIndexed { index, s ->
            if (index >= startAt) cell(s)
        }
    }
