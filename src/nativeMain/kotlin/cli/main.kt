package tl.jake.ktree.cli

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.*


// https://www.nequalsonelifestyle.com/2020/11/16/kotlin-native-file-io/
actual fun readFile(filename: String): String {
    val returnBuffer = StringBuilder()
    val file =
        fopen(filename, "r") ?: throw IllegalArgumentException("Cannot open input file $filename")

    try {
        memScoped {
            val readBufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(readBufferLength)
            var line = fgets(buffer, readBufferLength, file)?.toKString()
            while (line != null) {
                returnBuffer.append(line)
                line = fgets(buffer, readBufferLength, file)?.toKString()
            }
        }
    } finally {
        fclose(file)
    }

    return returnBuffer.toString()

}

actual fun readStdin(): String {
    val builder = StringBuilder()
    var line = readLine()
    while (line != null) {
        builder.append(line)

        line = readLine()
    }
    return builder.substring(0)
}

val STDERR = fdopen(2, "w")
actual fun writeError(data: String) {
    fprintf(STDERR, data)
    fflush(STDERR)
}

val STDOUT = fdopen(1, "w")
actual fun writeOut(data: String) {
    fprintf(STDOUT, data)
    fflush(STDOUT)
}