
/**
 * Parse tree notion.
 */
class Parser(settings: NotationSettings) {
    val nodeBreakSymbol = settings.nodeBreakSymbol
    val wordBreakSymbol = settings.wordBreakSymbol
    val edgeSymbol = settings.edgeSymbol
    val lineBreak = settings.lineBreak

    fun lex(text: String): List<Token> = Lexer(text).lex()

    fun ast(inputFile: InputFile, tokens: List<Token>) = TokenParser(inputFile, tokens).parse()

    fun tree(ast: AST.File): Tree.Root {
        val root = Tree.Root(ast)
        val stack = mutableListOf<Tree>(root)

        fun parent() = stack.last()

        for (node in ast.nodes) {
            // Pop previous indented nodes off the stack until the last node in the stack
            // is less indented than the node.
            while (parent().indent >= node.indent) {
                val child = stack.removeLast() as Tree.Node
                val siblings = parent().children as MutableList<Tree.Node>
                siblings.add(child)
            }

            // Example:
            // Parent word1 word2
            //  Child word1 word2
            //      Overindented word1 word2
            //  ~~~~ (this is overindented)
            val overindent = node.indent - parent().indent - 1
            val overindentWords = (0 until overindent).map { "" }
            val words: List<String> = overindentWords + node.words.map { it.content }

            val indent = parent().indent + 1
            val child = Tree.Node.create(parent(), node, indent, words)

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
    fun parse(text: String, filename: String = "<Memory>"): Tree.Root {
        val inputFile = InputFile(filename = filename, content = text)
        val tokens = lex(inputFile.content)
        val root = ast(inputFile, tokens)
        return tree(root)
    }

    /**
     * Transforms tokens into AST nodes.
     */
    private inner class TokenParser(val inputFile: InputFile, val tokens: List<Token>) {
        val nodes = mutableListOf<AST.TreeNode>()

        fun parse(): AST.File {
            // Node parsing
            var nodeStart: Position? = null
            var edgeCount = 0
            var words = mutableListOf<AST.Word>()
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
                    val word = AST.Word(
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
                        words = words
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
                    else -> TODO("unreachable")
                }
            }

            // TODO: finishWord() and finishNode() ?

            return AST.File(
                span = Span(tokens.first().span.start, tokens.last().span.end),
                nodes = nodes,
                raw = inputFile,
            )
        }
    }

    /**
     * Transforms an input string into tokens.
     */
    private inner class Lexer(val input: String) {
        val tokens = mutableListOf<Token>()
        val scanner = Scanner(input)

        fun lex(): List<Token> {
            val wordBreakTokenType =
                if (wordBreakSymbol == edgeSymbol) Token.Type.WordBreakOrEdge else Token.Type.WordBreak
            val edgeTokenType =
                if (wordBreakSymbol == edgeSymbol) Token.Type.WordBreakOrEdge else Token.Type.Edge

            var startIndex = scanner.index
            while (!scanner.ended()) {
                when (scanner.peek()) {
                    nodeBreakSymbol -> {
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
                    wordBreakSymbol -> {
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
                    edgeSymbol -> {
                        if (startIndex != scanner.index) {
                            produce(Token.Type.Text, startIndex, scanner.index)
                        }
                        produce(edgeTokenType, scanner.index, scanner.index + edgeSymbol.length)
                        scanner.pop(edgeSymbol.length)
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

    fun pop(count: Int = 1): String {
        val result = peek(count)
        index += count
        return result
    }

    fun popString(expected: String): String? {
        if (peek(expected.length) == expected) {
            return pop(expected.length)
        }
        return null
    }

    fun ended(): Boolean {
        return index >= input.length
    }
}


data class InputFile(val filename: String, val content: String)
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

    data class Word(
        override val span: Span,
        val tokens: List<Token>,
        val content: String,
    ) : AST()

    data class TreeNode(
        override val span: Span,
        val indent: Int,
        val edgeTokens: List<Token>,
        val words: List<Word>
    ) : AST()

    data class File(
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
    ) : Warning()
}
