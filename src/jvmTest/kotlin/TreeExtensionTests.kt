package tl.jake.ktree

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeExtensionTestData {
    lateinit var author: Tree.Node
    lateinit var none: Tree.Node
    val root = TreeBuilder.build {
        node("name", "ktree")
        node("author") {
            ref { author = it }
            node("name", "Jake")
            node("email", "jake@example.com")
        }
    }
    val other = NodeBuilder.build("dependencies") {
        node("none") {
            ref { none = it }
        }
    }
}

class TreeExtensionTests {
    fun dummyNode() = NodeBuilder.build()

    @Test
    fun `run toString`() {
        println(TreeExtensionTestData().root)
    }

    @Test
    fun `test moveTo`() {
        val testData = TreeExtensionTestData()
        testData.other.moveTo(testData.root)
        assertTreesHaveSameContent(
            TreeBuilder.build {
                node("name", "ktree")
                node("author") {
                    node("name", "Jake")
                    node("email", "jake@example.com")
                }
                node("dependencies") {
                    node("none")
                }
            },
            testData.root
        )
    }

    @Test
    fun `test addAt with Replace`() = testAdd(AddingAt.Replace(dummyNode()))

    @Test
    fun `test addAt with Index`() = testAdd(AddingAt.Index(0))

    @Test
    fun `test addAt Before`() = testAdd(AddingAt.Before(dummyNode()))

    @Test
    fun `test addAt After`() = testAdd(AddingAt.After(dummyNode()))

    @Test
    fun `test addAt Start`() = testAdd(AddingAt.Start)

    @Test
    fun `test addAt End`() = testAdd(AddingAt.End)

    @Test
    fun `test addAt with low level index`() = testAdd(null)

    @Test
    fun `test cloneData`() {
        val node = NodeBuilder.build {
            cell("CoolType")
            cell("data1")
            cell("data2")
            node("CoolChild1", "childData1")
            node("CoolChild2", "childData1")
        }
        val expected = NodeBuilder.build {
            cell("data1")
            cell("data2")
            node("CoolChild1", "childData1")
            node("CoolChild2", "childData1")
        }

        val clonedData = node.cloneData("CoolType")
        assertTreesHaveSameContent(expected, clonedData)
    }

    private fun testAdd(type: AddingAt?) {
        val needle = NodeBuilder.build("needle")
        val haystack = NodeBuilder.build("haystack") {
            node("0")
            node("1")
            node("2")
            node("3")
            node("4")
            node("5")
        }

        when (type) {
            is AddingAt.Replace -> {
                val target = haystack.children[3]
                haystack.addChild(needle, AddingAt.Replace(target))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         needle
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }
            is AddingAt.ReplaceIndex -> {
                haystack.addChild(needle, AddingAt.ReplaceIndex(3))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         needle
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

            }
            is AddingAt.Index -> {
                haystack.addChild(needle, AddingAt.Index(0))
                assertEquals(
                    """
                        haystack
                         needle
                         0
                         1
                         2
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                haystack.addChild(needle, AddingAt.Index(6))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         3
                         4
                         5
                         needle
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                haystack.addChild(needle, AddingAt.Index(3))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         needle
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }

            is AddingAt.Before -> {
                // Before middle
                haystack.addChild(needle, AddingAt.Before(haystack.children[2]))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         needle
                         2
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                // Before first
                haystack.addChild(needle, AddingAt.Before(haystack.children[0]))
                assertEquals(
                    """
                        haystack
                         needle
                         0
                         1
                         2
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                // Before missing
                haystack.addChild(needle, AddingAt.Before(dummyNode()))
                assertEquals(
                    """
                        haystack
                         needle
                         0
                         1
                         2
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }
            is AddingAt.After -> {
                // Before middle
                haystack.addChild(needle, AddingAt.After(haystack.children[2]))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         needle
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                // After last
                haystack.addChild(needle, AddingAt.After(haystack.children[5]))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         3
                         4
                         5
                         needle
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                // After missing
                haystack.addChild(needle, AddingAt.After(dummyNode()))
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         3
                         4
                         5
                         needle
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }

            is AddingAt.Start -> {
                haystack.addChild(needle, AddingAt.Start)
                assertEquals(
                    """
                        haystack
                         needle
                         0
                         1
                         2
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }

            is AddingAt.End -> {
                haystack.addChild(needle, AddingAt.End)
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         3
                         4
                         5
                         needle
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }

            null -> {
                haystack.addChild(0, needle)
                assertEquals(
                    """
                        haystack
                         needle
                         0
                         1
                         2
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                haystack.addChild(6, needle)
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         3
                         4
                         5
                         needle
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )

                haystack.addChild(3, needle)
                assertEquals(
                    """
                        haystack
                         0
                         1
                         2
                         needle
                         3
                         4
                         5
                    """.trimIndent(),
                    TreeNotation.Spaces.format(haystack).trimEnd()
                )
            }
        }.exhaustive
    }
}