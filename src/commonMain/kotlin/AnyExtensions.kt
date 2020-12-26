package tl.jake.ktree

/**
 * Call on the result of a when {} to use the when {} as an expression,
 * which forces it to be exhaustive.
 * https://proandroiddev.com/til-when-is-when-exhaustive-31d69f630a8b
 */
val <T> T.exhaustive: T
    get() = this