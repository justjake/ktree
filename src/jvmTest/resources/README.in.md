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

Experimental. All features probably contain significant bugs and are under heavy development.

Outstanding TODOs before releasing version `0.0.1`:

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
- [ ] Serialization / encoding / decoding ("arbito")
    - [x] First draft
    - [ ] Polymorphic serialization
    - [ ] `@Inline` annotation
    - [ ] Spec work
    - [x] Pick a name
        - This is called "arbito"

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

## Serialization Language (`arbito`)

`ktree` provides [`kotlinx.serialization`][kserial] encoders and decoders that allow you to convert
Kotlin data structures to and from a Tree Notation representation. This representation is named
"arbito", a shortening of "arbolito", which means "little tree" in Spanish.

We can use the following Kotlin classes to decode the first example in a type-safe manner:

[kserial]: https://github.com/Kotlin/kotlinx.serialization

```kotlin
$KTREE_KOTLIN
```

To decode Tree Notation as a `PackageSpec` instance, call `decodeFromTree`:

```kotlin
import tl.jake.ktree.TreeNotation
import tl.jake.ktree.serialization.decodeFromTree

val tree = TreeNotation.Spaces.parse(example)
val pkg = decodeFromTree<PackageSpec>(tree)
println(pkg.prettyToString())
```

```
$KTREE_KOTLIN_STRING
```

### Specification by example

**Classes** values encoded as a node, and its fields are encoded as child nodes inside the node. By
default, each field is encoded as a child node with the first cell as the field name, and the
remaining cells and descendants as the field value.

* If a field is annotated `@Inline`, it *may* be encoded as a cell in the value's root node. Inline
  cells are decoded from the root node in order of declaration.

* If a field is annotated `@Anonymous`, it *may* be encoded as a child node without the field name.
  Anonymous fields are decoded in order of declaration.

Here's some examples of encoding the following class:

```kotlin
class Team(val lead: Person)
class Person(
    @Inline val name: String,
    val age: Int,
    @Anonymous val occupation: String
)
```

<table>
<tr>
<td>

Compact encoding

```
lead Jake
 age 29
 Software engineer
```

</td>
<td>

Unambiguous encoding

```
lead
 name Jake
 age 29
 occupation Software engineer
```

</td>
</tr>
</table>

**Primitives** like strings and numbers are typically coded as single cells within a node. There are
no type sigils to differentiate between the different numeric and string types, so the serialization
is not entirely self-describing.

**Numeric primitives** are coded as their Kotlin `.toString()` and `String.toType()`
mirror methods.

**Null** is encoded as a cell containing the string `null`. In cases where this is ambiguous, for
example a field with type `String?`, a string `"null"` is encoded as `\null`.

**Strings** are coded as JSON strings without the opening or closing quotes. Control characters like
newlines and tabs are escaped as `\n` and `\t` in output.

- **TODO** If a string cells contains annotated `@Multiline`, it *may* be encoded as a cell
  containing
  `|` and then indented as child nodes. The child nodes will be joined together with newlines and
  all of their cells joined together with tabs.

  A string containing just a `|` that is not a multiline string is encoded as a cell containing `\|`

- **TODO**: If a string cell ends with a `\` character, the string continues into the next cell.
  The `\` is removed when decoding and replaced with the `cellBreakSymbol`.

- Strings that start with a `\` character are encoded with an extra `\` prepended. For example, to
  encode the string `"\ is the worst character"` would encode to cell
  `\\\ is the worst character`.

Here's an example of encoding a `Team` if all string fields were `@Multiline`.

```
lead
 name |
  Jake
 age 29
 occupation |
  Software engineer
```

#### Collections

**Lists** are encoded as child nodes inside a parent. List items may be prefixed by a cell
containing `-`, which is ignored, but makes visual indentation clearer. If an inline cell contains
just a `-`, it should be written as `\-`.

Examples serializing class `Squad`:

```kotlin
class Squad(val members: List<Person>)
```

Default, unambiguous encoding:

```
members
 -
  name Jake
  age 29
  occupation SWE
 -
  name Tamago
  age 33
  occupation Product Manager
```

Member fields compacted:

```
members
 - Jake
  age 29
  SWE
 - Tamago
  age 33
  Product Manager
```

Without leading -, but with unambiguous fields. This encoding gives a lot of breathing room to list
members.

```
members
 
  name Jake
  age 29
  occupation SWE
  
  name Tamago
  age 33
  occupation Product Manager
```

Here's the most compact encoding possible. No `-` markers, and all inline and anonymous fields
compacted.

```
members
 Jake
  age 29
  SWE
 Tamago
  age 33
  Product Manager
```

**Maps** have two encodings, depending on the kind of *key* used in the map.

**Maps with primitive keys** are encoded like a class. Each key-value pair is a child node of the
map. The first cells of the child encodes the key. The remaining cells and children of the node
encode the value. (As usual, a string containing the cell break character can escape it with \)

```kotlin
data class GameState(val scores: Map<String, Int>)
GameState(mapOf("Jake TL" to 5, "Tamago" to 12))
```

```
scores
 Jake\ TL 5
 Tamago 12
```

**Maps with complex keys** are encoded like a list of pairs with fields `key` and `value`. This
encoding is a bummer.

```kotlin
data class ComplexGameState(val scores: Map<Person, Int>)
GameState(mapOf(
  Person("Jake TL", 29, "SWE") to 5,
  Person("Tamago", 30, "Product manager") to 12,
))
```

```
scores
 -
  key Jake\ TL
   age 29
   SWE
  value 12
 -
  key Tamago
   age 30
   SWE
  value 12
```

It may help to think of a map as an encoding of this Kotlin type:

```kotlin
data class KV<K, V>(
    @Inline @Anonymous val key: K,
    @Inline @Anonymous val value: V,
)
```

**Comments** are coded as any node where the first cell contains exactly `//`. Comments are ignored
when decoding. To encode a node with a first cell containing the string `"//"`, prepend the cell
with a `\`.

```kotlin
data class GameState(val scores: Map<String, Int>)
GameState(mapOf("//" to 5))
```

```
scores
 \// 5
```
