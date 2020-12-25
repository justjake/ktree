package tl.jake.ktree.cli

import tl.jake.ktree.nodeFS.readFileSync

actual fun readStdin(): String {
    return readFileSync(0, "utf-8")
}

actual fun readFile(filename: String): String {
    return readFileSync(filename, "utf-8")
}

external val process: dynamic

actual fun writeOut(data: String) {
    process.stdout.write(data)
}

actual fun writeError(data: String) {
    process.stderr.write(data)
}