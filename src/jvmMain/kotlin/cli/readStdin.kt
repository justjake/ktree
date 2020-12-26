package tl.jake.ktree.cli

import java.io.File

actual fun readFile(filename: String): String {
    return File(filename).readText()
}

actual fun readStdin(): String {
    return System.`in`.readAllBytes().decodeToString()
}

actual fun writeOut(data: String) {
    System.out.write(data.toByteArray())
    System.out.flush()
}

actual fun writeError(data: String) {
    System.err.write(data.toByteArray())
    System.err.flush()
}