package tl.jake.ktree.serialization

/**
 * Mark a field as able to be inlined into a cell when encoding
 * the containing type to tree notation.
 *
 * When decoding, the cell will be parsed if present. Otherwise, it must
 * be specified in a child tree node inside the type.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class Inline