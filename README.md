# ktree

Kotlin Multiplatorm implementation of [Tree Notation](https://treenotation.org/).

Tree Notation specifies a very simple language for indented parsing text into a
tree data structure. Here's an example of a tree written in Tree Notation:

```
package ktree

author
 name Jake
 email jake@example.com

dependencies
 multiplatform >= 2
  resolved https://example.com/multiplatform
  checksum abcdef1234
```

With the appropriate notation parser settings, this text parses into the
following tree structure:

```
├── 0. [package,ktree]
├── 1. []
├── 2. [author]
│   ├── 0. [name,jake]
│   └── 1. [email,jake@example.com]
├── 3. []
└── 4. [dependencies]
    └── 0. [multiplatform,>=,2]
        ├── 0. [resolved,https://example.com/multiplatform]
        └── 1. [checksum,abcdef1234]
```

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
     * wordBreakSymbol delimits words in the node string.
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

The official Tree Notation spec is ambiguous about how to handle over-indented
child nodes. In a case like below, `ktree` ensures all children with the same
indentation level are treated as siblings. The extra indents are parsed as
part of the node's text, as either wordBreakSymbols, or as text of the first
word.

```
parent
   over-indented child 1
   over-indented child 2
       over-indented child 3
```

parses to

```
└── 0. [parent]
    ├── 0. [,,over-indented,child,1]
    └── 1. [,,over-indented,child,2]
        └── 0. [,,,over-indented,child,3]
```
