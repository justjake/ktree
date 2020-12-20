@file:JsModule("fs")
@file:JsNonModule
@file:JsQualifier("promises")

package nodeFS

import kotlin.js.Promise

external fun readFile(path: String, encoding: String): Promise<String>