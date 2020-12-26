# ktree

Kotlin Multiplatorm implementation of [Tree Notation](https://treenotation.org/).

Tree Notation specifies a very simple language for indented parsing text into a tree data structure.
Here's an example of a tree written in Tree Notation:

```
$KTREE
```

With the appropriate notation parser settings, this text parses into the following tree structure:

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

Experimental. Outstanding TODOs:

- [x] Figure out if I need to put `package ktree` at the top of every file
    - [x] Added `package tl.jake.ktree`, etc, to every file
- [ ] JVM (Maven? Bintray?) build and release. Apparently, things are more difficult in java-land
  than being the first person to
  `npm publish` with your package name in package.json. This is sad. So far, my research points to:
    - https://stackoverflow.com/questions/28846802/how-to-manually-publish-jar-to-maven-central
    - https://central.sonatype.org/pages/ossrh-guide.html
    - https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:repositories
    - [x] Open JIRA ticket https://issues.sonatype.org/browse/OSSRH-62956
    - [x] Create redirect at https://ktree.jake.tl -> https://www.github.com/justjake/ktree
    - [x] OSSRH repo created.
    - [ ] push first build to OSSRH
    - [ ] sync OSSRH to maven central
- [ ] NPM build and release.

This project intends to target all of Kotlin's supported platforms eventually. For now,
multiplatform kotlin projects are alpha status, so don't expect too much.

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

Each line (by default) in the file is a *node* in the tree. The text in a node is split into cells.
The children of a node are the lines underneath a node that are indented by one `edgeSymbol` (
default: one tab).

The example below contains a parent node `author` with two child nodes:

```
$AUTHOR
```

Tree notation is canonically serialized to JSON as objects with `cells: String[]`
and `children: Node[]` properties, which are omitted when empty. This serialization should make the
result of parsing clear:

```json
$AUTHOR_JSON
```

### Indentation

The official Tree Notation spec is ambiguous about how to handle over-indented child nodes. `ktree`
allows configuring this behavior.

#### Strict Indentation

```kotlin
import tl.jake.ktree.TreeNotation

TreeNotation() // default
TreeNotation(overIndentBehavior = TreeNotation.OverIndentBehavior.Strict)
```

In strict mode (the default), we only parse one additional indentation level for a node. The
remaining edge symbols are treated as part of the first cell, or as cell seperators. This has the
consiquence that similarly-over-indented nodes that visually appear to be siblings will be parsed as
parent-child instead.

```
$OVERINDENTED
```

parses to

```
$OVERINDENTED_STRICT_OUTLINE
```

<details>
  <summary>As JSON</summary>

```json
$OVERINDENTED_STRICT_JSON
```

</details>

#### Equally indented children are siblings

```kotlin
import tl.jake.ktree.TreeNotation

TreeNotation(
    overIndentBehavior = TreeNotation.OverIndentBehavior.EquallyIndentedChildrenAreSiblings
)
```

With this setting, all children with the same indentation level are treated as siblings. The extra
indents are parsed as part of the node's text, as either wordBreakSymbols, or as text of the first
word.

```
$OVERINDENTED
```

parses to

```
$OVERINDENTED_EQUAL_OUTLINE
```

<details>
  <summary>As JSON</summary>

```json
$OVERINDENTED_EQUAL_JSON
```

</details>
