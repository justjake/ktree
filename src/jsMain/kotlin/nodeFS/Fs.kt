@file:JsModule("fs")
@file:JsNonModule

package tl.jake.ktree.nodeFS

external fun readFileSync(filename: String, mode: String): String
external fun readFileSync(fd: Int, mode: String): String