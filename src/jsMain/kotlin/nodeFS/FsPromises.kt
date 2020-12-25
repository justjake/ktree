@file:JsModule("fs")
@file:JsNonModule
@file:JsQualifier("promises")

package tl.jake.ktree.nodeFS

import kotlin.js.Promise

external fun readFile(path: String, encoding: String): Promise<String>