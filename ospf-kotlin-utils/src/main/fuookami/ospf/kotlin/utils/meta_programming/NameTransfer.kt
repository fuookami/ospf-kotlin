package fuookami.ospf.kotlin.utils.meta_programming

/**
 * implementation for name transfer
 *
 * @property frontend       frontend naming system
 * @property backend        backend naming system
 * @property cache          cache of transferring results
 */
private class NameTransferImpl(
    val frontend: NamingSystem,
    val backend: NamingSystem
) {
    @get:Synchronized
    val cache: HashMap<String, String> = HashMap()

    /**
     * transform a name from the frontend naming system to the backend naming system
     *
     * @param name              given name
     * @param abbreviations     abbreviation set
     * @return                  the name corresponding the backend naming system
     */
    operator fun invoke(name: String, abbreviations: Set<String>): String {
        return cache.getOrPut(name) {
            val il = frontend.frontend(name, abbreviations)
            backend.backend(il)
        }
    }

    /**
     * transform a name from the backend naming system to the frontend naming system
     *
     * @param name              given name
     * @param abbreviations     abbreviation set
     * @return                   the name corresponding the frontend naming system
     */
    fun reverse(name: String, abbreviations: Set<String> = emptySet()): String {
        return transfers[Pair(backend, frontend)]!!(name, abbreviations)
    }
}

/**
 * build a name transfer with given frontend and backend naming system
 *
 * @param frontend      frontend naming system
 * @param backend       backend naming system
 * @return              name transfer for given frontend and backend naming system
 */
private fun nameTransferOf(
    frontend: NamingSystem,
    backend: NamingSystem
): Pair<Pair<NamingSystem, NamingSystem>, NameTransferImpl> {
    return Pair(Pair(frontend, backend), NameTransferImpl(frontend, backend))
}

/**
 * name transfers set
 */
private val transfers = mapOf(
    nameTransferOf(NamingSystem.SnakeCase, NamingSystem.UpperSnakeCase),
    nameTransferOf(NamingSystem.SnakeCase, NamingSystem.KebabCase),
    nameTransferOf(NamingSystem.SnakeCase, NamingSystem.CamelCase),
    nameTransferOf(NamingSystem.SnakeCase, NamingSystem.PascalCase),

    nameTransferOf(NamingSystem.UpperSnakeCase, NamingSystem.SnakeCase),
    nameTransferOf(NamingSystem.UpperSnakeCase, NamingSystem.KebabCase),
    nameTransferOf(NamingSystem.UpperSnakeCase, NamingSystem.CamelCase),
    nameTransferOf(NamingSystem.UpperSnakeCase, NamingSystem.PascalCase),

    nameTransferOf(NamingSystem.KebabCase, NamingSystem.SnakeCase),
    nameTransferOf(NamingSystem.KebabCase, NamingSystem.UpperSnakeCase),
    nameTransferOf(NamingSystem.KebabCase, NamingSystem.CamelCase),
    nameTransferOf(NamingSystem.KebabCase, NamingSystem.PascalCase),

    nameTransferOf(NamingSystem.CamelCase, NamingSystem.SnakeCase),
    nameTransferOf(NamingSystem.CamelCase, NamingSystem.UpperSnakeCase),
    nameTransferOf(NamingSystem.CamelCase, NamingSystem.KebabCase),
    nameTransferOf(NamingSystem.CamelCase, NamingSystem.PascalCase),

    nameTransferOf(NamingSystem.PascalCase, NamingSystem.SnakeCase),
    nameTransferOf(NamingSystem.PascalCase, NamingSystem.UpperSnakeCase),
    nameTransferOf(NamingSystem.PascalCase, NamingSystem.KebabCase),
    nameTransferOf(NamingSystem.PascalCase, NamingSystem.CamelCase),
)

/**
 * name transfer
 *
 * @property frontend           frontend naming system
 * @property backend            backend naming system
 * @property abbreviations      abbreviation set
 * @property impl               implementation
 */
class NameTransfer(
    val frontend: NamingSystem,
    val backend: NamingSystem,
    val abbreviations: Set<String> = emptySet()
) {
    private val impl: NameTransferImpl? = transfers[Pair(frontend, backend)]

    /**
     * transform a name from the frontend naming system to the backend naming system
     *
     * @param name              given name
     * @return                  the name corresponding the backend naming system
     */
    operator fun invoke(name: String): String {
        return impl?.invoke(name, abbreviations) ?: name
    }

    /**
     * transform a name from the backend naming system to the frontend naming system
     *
     * @param name              given name
     * @return                  the name corresponding the frontend naming system
     */
    fun reverse(name: String): String {
        return impl?.reverse(name) ?: name
    }
}
