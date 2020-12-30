import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tl.jake.ktree.serialization.Inline

@Serializable
data class PackageSpec(
    @SerialName("package") val name: String,
    val author: Author,
    val dependencies: Map<String, Dependency>,
)

@Serializable
data class Author(val name: String, val email: String)
@Serializable
data class Dependency(
    @Inline val constraint: String,
    val resolved: String? = null,
    val checksum: String? = null,
)