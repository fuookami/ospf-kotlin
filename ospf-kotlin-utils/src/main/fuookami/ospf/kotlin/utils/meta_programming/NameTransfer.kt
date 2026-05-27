/**
 * 本文件提供命名风格转换工具，支持在不同命名约定之间进行转换。
 * This file provides a name transfer utility for converting between different naming conventions.
 */
package fuookami.ospf.kotlin.utils.meta_programming

import java.util.concurrent.ConcurrentHashMap

/**
 * 缓存键类，包含名称和缩写集合
 *
 * Cache key class containing name and abbreviations.
 *
 * RVW-008 修复：缓存键扩展为 (name, abbreviations)，避免不同缩写集混用时的缓存污染。
 * Fix for RVW-008: Cache key extended to include (name, abbreviations) to prevent
 * cache pollution when different abbreviation sets are mixed in the same process.
 *
 * @property name          名称 / Name
 * @property abbreviations 缩写集合（排序后存储以保证一致性） / Abbreviations (stored sorted for consistency)
 */
private data class NameTransferCacheKey(
    val name: String,
    val abbreviations: List<String>  // Use sorted List for deterministic hashCode/equals
) {
    companion object {
        /**
         * 从名称和缩写集合创建缓存键
         *
         * Create cache key from name and abbreviation set.
         *
         * @param name              名称 / Name
         * @param abbreviations     缩写集合 / Abbreviation set
         * @return                  缓存键 / Cache key
         */
        fun from(name: String, abbreviations: Set<String>): NameTransferCacheKey {
            return NameTransferCacheKey(name, abbreviations.sorted())
        }
    }
}

/**
 * 名称转换实现
 *
 * Implementation for name transfer.
 *
 * @property frontend       前端命名系统 / Frontend naming system
 * @property backend        后端命名系统 / Backend naming system
 * @property cache          转换结果缓存（线程安全） / Cache of transferring results (thread-safe)
 */
private class NameTransferImpl(
    val frontend: NamingSystem,
    val backend: NamingSystem
) {
    /**
     * 线程安全的缓存
     *
     * Thread-safe cache using ConcurrentHashMap.
     *
     * RVW-002 修复：使用 ConcurrentHashMap 替代 HashMap + synchronized，
     * 确保并发场景下的线程安全。
     * Fix for RVW-002: Use ConcurrentHashMap instead of HashMap + synchronized
     * to ensure thread safety in concurrent scenarios.
     *
     * RVW-008 修复：缓存键使用 NameTransferCacheKey (name + abbreviations)，
     * 避免同进程混用不同缩写集时的缓存命中错误。
     * Fix for RVW-008: Cache key uses NameTransferCacheKey (name + abbreviations)
     * to prevent cache hit errors when mixing different abbreviation sets.
     */
    val cache: ConcurrentHashMap<NameTransferCacheKey, String> = ConcurrentHashMap()

    /**
     * 将名称从前端命名系统转换为后端命名系统
     *
     * Transform a name from the frontend naming system to the backend naming system.
     *
     * @param name              给定名称 / Given name
     * @param abbreviations     缩写集合 / Abbreviation set
     * @return                  对应后端命名系统的名称 / The name corresponding the backend naming system
     */
    operator fun invoke(name: String, abbreviations: Set<String>): String {
        return cache.computeIfAbsent(NameTransferCacheKey.from(name, abbreviations)) {
            val il = frontend.frontend(name, abbreviations)
            backend.backend(il)
        }
    }

    /**
     * 将名称从后端命名系统转换为前端命名系统
     *
     * Transform a name from the backend naming system to the frontend naming system.
     *
     * @param name              给定名称 / Given name
     * @param abbreviations     缩写集合 / Abbreviation set
     * @return                  对应前端命名系统的名称 / The name corresponding the frontend naming system
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
 * 名称转换器
 *
 * Name transfer.
 *
 * @property frontend           前端命名系统 / Frontend naming system
 * @property backend            后端命名系统 / Backend naming system
 * @property abbreviations      缩写集合 / Abbreviation set
 * @property impl               实现 / Implementation
 */
class NameTransfer(
    val frontend: NamingSystem,
    val backend: NamingSystem,
    val abbreviations: Set<String> = emptySet()
) {
    private val impl: NameTransferImpl? = transfers[Pair(frontend, backend)]

    /**
     * 将名称从前端命名系统转换为后端命名系统
     *
     * Transform a name from the frontend naming system to the backend naming system.
     *
     * @param name              给定名称 / Given name
     * @return                  对应后端命名系统的名称 / The name corresponding the backend naming system
     */
    operator fun invoke(name: String): String {
        return impl?.invoke(name, abbreviations) ?: name
    }

    /**
     * 将名称从后端命名系统转换为前端命名系统
     *
     * Transform a name from the backend naming system to the frontend naming system.
     *
     * @param name              给定名称 / Given name
     * @return                  对应前端命名系统的名称 / The name corresponding the frontend naming system
     */
    fun reverse(name: String): String {
        return impl?.reverse(name) ?: name
    }
}
