package tl.jake.ktree.parser

import tl.jake.ktree.Tree
import tl.jake.ktree.TreeNotation

/**
 * Parse tree notion.
 */
class Parser(val notation: TreeNotation) {
    private val nodeBreakSymbol = notation.nodeBreakSymbol
    private val wordBreakSymbol = notation.wordBreakSymbol
    private val edgeSymbol = notation.edgeSymbol
    private val lineBreak = notation.lineBreak

    /**
     * Tokenize text
     */
    fun lex(text: String, startIndex: Int = 0): List<Token> = Lexer(text, startIndex).lex()

    /**
     * Convert tokens into an AST
     */
    fun ast(inputFile: InputFile, tokens: List<Token>) = TokenParser(inputFile, tokens).parse()

    /**
     * Convert an AST into a Tree
     */
    fun tree(ast: AST.FileNode): Tree.Root {
        val root = Tree.Root(ast)
        val stack = mutableListOf<Tree>(root)

        fun parent() = stack.last()
        fun depth() = stack.size - 1
        fun parentLevel() = when (notation.overIndentBehavior) {
            TreeNotation.OverIndentBehavior.Strict -> depth() - 1
            TreeNotation.OverIndentBehavior.EquallyIndentedChildrenAreSiblings -> parent().indent
        }

        for (node in ast.nodes) {
            // Pop previous indented nodes off the stack until the last node in the stack
            // is less indented than the node.
            while (parentLevel() >= node.indent) {
                val child = stack.removeLast() as Tree.Node
                val siblings = parent().children as MutableList<Tree.Node>
                siblings.add(child)
            }

            // Example:
            // Parent word1 word2
            //  Child word1 word2
            //      Overindented word1 word2
            //  ~~~~ (this is overindented)
            val overindent = node.indent - depth()
            val overindentWords: List<String> = when {
                overindent == 0 -> listOf()
                // Produce empty words equal to the extra edge symbols
                edgeSymbol == wordBreakSymbol -> (0 until overindent).map { "" }
                // Produce a single word containg all the extra edge symbols
                else -> listOf((edgeSymbol ?: "").repeat(overindent))
            }

            val words: MutableList<String> =
                (overindentWords + node.cellNodes.map { it.content }).toMutableList()
            val indent = node.indent
            val child = Tree.Node(parent = parent(), astNode = node, indent = indent, cells = words)

            if (overindent > 0) {
                child.warnings.add(
                    Warning.ChildNodeOverIndented(
                        node = child,
                        span = Span(
                            node.edgeTokens.first().span.start,
                            node.edgeTokens.last().span.end
                        ),
                        indent = node.indent,
                        parent = parent(),
                        parentSpan = parent().astNode!!.span,
                        parentIndent = parent().indent,
                    )
                )
            }

            stack.add(child)
        }

        // Consume remaining stack.
        while (stack.size > 1) {
            val child = stack.removeLast() as Tree.Node
            val siblings = parent().children as MutableList<Tree.Node>
            siblings.add(child)
        }


        return root
    }

    /**
     * Parse an InputFile's content into a Tree.
     */
    fun parse(text: String, filename: String = "<Memory>", startIndex: Int = 0): Tree.Root {
        val inputFile = InputFile(filename = filename, content = text)
        val tokens = lex(inputFile.content, startIndex)
        val root = ast(inputFile, tokens)
        return tree(root)
    }

    /**
     * Transforms tokens into AST nodes.
     */
    private inner class TokenParser(val inputFile: InputFile, val tokens: List<Token>) {
        val nodes = mutableListOf<AST.TreeNode>()

        fun parse(): AST.FileNode {
            // Node parsing
            var nodeStart: Position? = null
            var edgeCount = 0
            var words = mutableListOf<AST.CellNode>()
            var nodeTokens = mutableListOf<Token>()

            // Word parsing
            var wordStart: Position? = null
            var wordTokens = mutableListOf<Token>()

            for (i in tokens.indices) {
                val token = tokens[i]

                fun incrementNode(edgeToken: Token?) {
                    if (nodeStart == null) {
                        nodeStart = token.span.start
                    }

                    if (edgeToken != null) {
                        nodeTokens.add(token)
                        edgeCount++
                    }
                }

                fun incrementWord() {
                    incrementNode(null)
                    if (wordStart == null) {
                        wordStart = token.span.start
                    }
                    wordTokens.add(token)
                }

                fun inWords() = words.isNotEmpty() || wordTokens.isNotEmpty() || wordStart != null

                fun finishWord() {
                    val word = AST.CellNode(
                        span = Span(wordStart ?: token.span.start, token.span.start),
                        tokens = wordTokens,
                        content = wordTokens.joinToString("") { it.string }
                    )
                    words.add(word)
                    wordStart = null
                    wordTokens = mutableListOf()
                }

                fun parseEdge() {
                    if (inWords()) {
                        // Edge token inside a word
                        incrementWord()
                        return
                    }

                    // Edge before words in token
                    incrementNode(token)
                }

                fun finishNode() {
                    if (inWords()) {
                        finishWord()
                    }
                    val node = AST.TreeNode(
                        span = Span(
                            nodeStart ?: token.span.start,
                            token.span.start
                        ),
                        indent = edgeCount,
                        edgeTokens = nodeTokens,
                        cellNodes = words
                    )
                    nodes.add(node)

                    nodeStart = null
                    nodeTokens = mutableListOf()
                    words = mutableListOf()
                    edgeCount = 0
                }

                when (token.type) {
                    Token.Type.Edge -> parseEdge()
                    Token.Type.Text -> incrementWord()
                    Token.Type.WordBreak -> finishWord()
                    Token.Type.WordBreakOrEdge -> if (inWords()) finishWord()
                    else parseEdge()
                    Token.Type.NodeBreak -> finishNode()
                }
            }

            val span = if (tokens.size == 0) {
                Span(Position(0, 0, 0), Position(0, 0, 0))
            } else {
                Span(tokens.first().span.start, tokens.last().span.end)
            }
            return AST.FileNode(
                span = span,
                nodes = nodes,
                raw = inputFile,
            )
        }
    }

    /**
     * Transforms an input string into tokens.
     */
    private inner class Lexer(val input: String, startIndex: Int) {
        val tokens = mutableListOf<Token>()
        val scanner = Scanner(input)

        init {
            scanner.index = startIndex
        }

        fun lex(): List<Token> {
            val wordBreakTokenType =
                if (wordBreakSymbol == edgeSymbol) Token.Type.WordBreakOrEdge else Token.Type.WordBreak
            val edgeTokenType =
                if (wordBreakSymbol == edgeSymbol) Token.Type.WordBreakOrEdge else Token.Type.Edge

            var startIndex = scanner.index
            while (!scanner.ended()) {
                when {
                    scanner.matchString(nodeBreakSymbol) -> {
                        if (startIndex != scanner.index) {
                            produce(Token.Type.Text, startIndex, scanner.index)
                        }
                        produce(
                            Token.Type.NodeBreak,
                            scanner.index,
                            scanner.index + nodeBreakSymbol.length
                        )
                        scanner.pop(nodeBreakSymbol.length)
                        startIndex = scanner.index
                    }

                    edgeSymbol != null && scanner.matchString(edgeSymbol) -> {
                        if (startIndex != scanner.index) {
                            produce(Token.Type.Text, startIndex, scanner.index)
                        }
                        produce(edgeTokenType, scanner.index, scanner.index + edgeSymbol.length)
                        scanner.pop(edgeSymbol.length)
                        startIndex = scanner.index
                    }

                    scanner.matchString(wordBreakSymbol) -> {
                        if (startIndex != scanner.index) {
                            produce(Token.Type.Text, startIndex, scanner.index)
                        }
                        produce(
                            wordBreakTokenType,
                            scanner.index,
                            scanner.index + wordBreakSymbol.length
                        )
                        scanner.pop(wordBreakSymbol.length)
                        startIndex = scanner.index
                    }

                    else -> scanner.pop()
                }
            }

            // Complete current word, if any
            if (startIndex != scanner.index) {
                produce(Token.Type.Text, startIndex, scanner.index)
            }

            // Ensure a NodeBreak token at EOF.
            if (tokens.size > 0 && tokens.last().type !== Token.Type.NodeBreak) {
                produce(Token.Type.NodeBreak, scanner.index, scanner.index)
            }

            return tokens
        }

        private fun produce(type: Token.Type, startIndex: Int, endIndex: Int): Token {
            val value = input.slice(startIndex until endIndex)
            val token = Token(type, value, Span(position(startIndex), position(endIndex)))
            tokens.add(token)
            return token
        }

        private fun position(index: Int): Position {
            val before = input.slice(0 until index)
            val lines = before.split(lineBreak)
            val column = lines.last().length
            return Position(index = index, line = lines.size, column = column)
        }
    }
}

class Scanner(val input: String, var index: Int = 0) {
    fun peek(count: Int = 1): String {
        return input.slice(index until index + count)
    }

    fun matchString(expected: String) = peek(expected.length) == expected

    fun pop(count: Int = 1): String {
        val result = peek(count)
        index += count
        return result
    }

    fun ended(): Boolean {
        return index >= input.length
    }
}


data class InputFile(val filename: String, val content: String, val startIndex: Int = 0)
data class Position(val index: Int, val line: Int, val column: Int)
data class Span(val start: Position, val end: Position)
data class Token(val type: Type, val string: String, val span: Span) {
    enum class Type {
        NodeBreak,
        WordBreak,
        Edge,
        WordBreakOrEdge,
        Text,
    }
}

sealed class AST {
    abstract val span: Span

    data class CellNode(
        override val span: Span,
        val tokens: List<Token>,
        val content: String,
    ) : AST()

    data class TreeNode(
        override val span: Span,
        val indent: Int,
        val edgeTokens: List<Token>,
        val cellNodes: List<CellNode>
    ) : AST()

    data class FileNode(
        override val span: Span,
        val nodes: List<TreeNode>,
        val raw: InputFile
    ) : AST()
}

sealed class Warning {
    data class ChildNodeOverIndented(
        val node: Tree.Node,
        val parent: Tree,
        val span: Span,
        val indent: Int,
        val parentSpan: Span,
        val parentIndent: Int,
    ) : Warning() {
        override fun toString(): String {
            return """
               Warning[${span.end.line}:${span.end.column}] Child node may be over-indented.
            """.trimIndent()
        }
    }
}
