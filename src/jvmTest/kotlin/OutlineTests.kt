package tl.jake.ktree

import kotlin.test.Test
import kotlin.test.assertEquals

class OutlineTests {
    @Test
    fun testOutline() {
        val example = """
package ktree

author
 name Jake
 email jake@example.com

dependencies
 multiplatform >= 2
  resolved https://example.com/multiplatform
  checksum abcdef1234
        """.trimIndent()
        val tree = TreeNotation.Spaces.parse(example)

        val expected = """
├─ 0. [package, ktree]
├─ 1. []
├─ 2. [author]
│  ├─ 0. [name, Jake]
│  └─ 1. [email, jake@example.com]
├─ 3. []
└─ 4. [dependencies]
   └─ 0. [multiplatform, >=, 2]
      ├─ 0. [resolved, https://example.com/multiplatform]
      └─ 1. [checksum, abcdef1234]
        """.trimIndent()
        assertEquals(expected, tree.toOutline())
    }
}