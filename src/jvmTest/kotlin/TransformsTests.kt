import kotlin.test.Test
import kotlin.test.assertEquals

class TestData {
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

class TransformsTests {
    fun dummyNode() = NodeBuilder.build()

    @Test
    fun testMove() {
        val testData = TestData()
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
    fun testAddAtReplace() = testAdd(AddingAt.Replace(dummyNode()))

    @Test
    fun testAddAtIndex() = testAdd(AddingAt.Index(0))

    @Test
    fun testAddBefore() = testAdd(AddingAt.Before(dummyNode()))

    @Test
    fun testAddAfter() = testAdd(AddingAt.After(dummyNode()))

    @Test
    fun testAddStart() = testAdd(AddingAt.Start)

    @Test
    fun testAddEnd() = testAdd(AddingAt.End)

    @Test
    fun testAddLowLevel() = testAdd(null)

    fun testAdd(type: AddingAt?) {
        val needle = NodeBuilder.build("needle")
        val haystack = NodeBuilder.build("haystack") {
            node("0")
            node("1")
            node("2")
            node("3")
            node("4")
            node("5")
        }

        when(type) {
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
                )

                // After missing
                haystack.addChild(needle, AddingAt.Before(dummyNode()))
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
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
                    NotationSettings.Spaces.format(haystack).trimEnd()
                )
            }
        }.exhaustive
    }
}