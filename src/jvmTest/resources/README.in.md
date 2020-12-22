# ktree

Kotlin Multiplatorm implementation of [Tree Notation](https://treenotation.org/).

Tree Notation specifies a very simple language for indented parsing text into a
tree data structure. Here's an example of a tree written in Tree Notation:

```
$KTREE
```

With the appropriate notation parser settings, this text parses into the
following tree structure:

```
$KTREE_OUTLINE
```

<details>
  <summary>As JSON</summary>

```json
$KTREE_JSON
```

</details>

## Project Status

Experimental.

This project intends to target all of Kotlin's supported platforms eventually.
For now, multiplatform kotlin projects are alpha status, so don't expect too much.

Platform feature support:

- All platforms: "Pure" features like parsing, tree manipulation, and printing
- JVM, NodeJS: filesystem access

## Tree Notation

[Official spec](https://github.com/treenotation/faq.treenotation.org/blob/master/spec.txt).

A Tree Notation variant is defined by three symbols:

```kotlin
data class TreeNotation(
    /**
     * nodeBreakSymbol delimits nodes.
     */
    val nodeBreakSymbol: String = "\n",

    /**
     * wordBreakSymbol delimits cells in the node string.
     */
    val wordBreakSymbol: String = "\t",

    /**
     * edgeSymbol is used to indicate the parent/child relationship between nodes.
     * Nodes prefixed with the same number of edge symbols are siblings.
     * A node prefixed with more edge symbols than the previous node is the previous
     * node's child.
     * If null, parse as Grid Notation, with no parent-child concepts.
     */
    val edgeSymbol: String? = "\t",
)
```

### Nodes

Each line (by default) in the file is a *node* in the tree.
The text in a node is split into cells. The children of a node are the lines underneath a node that
are indented by one tab.

The example below contains a parent node `author` with two child nodes:

```
$AUTHOR
```

Tree notation is canonically serialized to JSON as objects with `cells: String[]`
and `children: Node[]` properties, which are omitted when empty. This serialization should
make the result of parsing clear:

```json
$AUTHOR_JSON
```

### Indentation

The official Tree Notation spec is ambiguous about how to handle over-indented
child nodes. In a case like below, `ktree` ensures all children with the same
indentation level are treated as siblings. The extra indents are parsed as
part of the node's text, as either wordBreakSymbols, or as text of the first
word. *This behavior is non-standard and will not be the default in the future*.

```
$OVERINDENTED
```

parses to

```
$OVERINDENTED_OUTLINE
```

<details>
  <summary>As JSON</summary>

```json
$OVERINDENTED_JSON
```

</details>
