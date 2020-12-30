package tl.jake.ktree.serialization

/**
 * Mark a field containing a primitive type as able to be inlined into a cell when encoding
 * the containing type to tree notation.
 *
 * When decoding, the cell will be parsed if present. Otherwise, it must
 * be specified in a child tree node inside the type.
 *
 * For non-primitive types, @Inline is ignored.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class Inline

/**
 * Mark a field as anonymous so that it will be coded without printing the
 * name of the field.
 *
 * When decoding, anonymous fields will be decoded based on the order they appear
 * in the class. Using more than one anonymous field in a type may make that
 * type's tree encoding difficult to write correctly for humans.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class Anonymous