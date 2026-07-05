/**
 * 排序模型
 * Sort Model
 *
 * 定义查询排序规则，支持多字段、方向和空值顺序。
 * Defines query sorting rules, supporting multiple fields, directions, and nulls order.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import kotlin.reflect.KProperty1

/**
 * 排序定义
 * Sort Definition
 *
 * 表示一个或多个排序项的集合。
 * Represents a collection of one or more sort items.
 *
 * 示例 / Example:
 * ```kotlin
 * val sort = SortBy.asc("name") + SortBy.desc("createdAt", NullsOrder.NullsLast)
 * ```
 */
data class SortBy(
    val items: List<SortItem>
) {
    companion object {
        /**
         * 空排序
         * Empty sort
         */
        val empty = SortBy(emptyList())

        /**
         * 升序排序
         * Ascending sort
         *
         * @param path 字段路径 / Field path
         * @param nulls 空值排序策略（可为 null） / Nulls order strategy (nullable)
         * @return 排序定义 / Sort definition
         */
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
            SortBy(listOf(SortItem(path, SortDirection.Asc, nulls)))

        /**
         * 降序排序
         * Descending sort
         *
         * @param path 字段路径 / Field path
         * @param nulls 空值排序策略（可为 null） / Nulls order strategy (nullable)
         * @return 排序定义 / Sort definition
         */
        fun desc(path: String, nulls: NullsOrder? = null): SortBy =
            SortBy(listOf(SortItem(path, SortDirection.Desc, nulls)))

        /**
         * 按属性升序排序
         * Ascending sort by property
         *
         * @param property 属性引用 / Property reference
         * @param nulls 空值排序策略（可为 null） / Nulls order strategy (nullable)
         * @return 排序定义 / Sort definition
         */
        fun <E, T> asc(property: KProperty1<E, T>, nulls: NullsOrder? = null): SortBy =
            asc(property.name, nulls)

        /**
         * 按属性降序排序
         * Descending sort by property
         *
         * @param property 属性引用 / Property reference
         * @param nulls 空值排序策略（可为 null） / Nulls order strategy (nullable)
         * @return 排序定义 / Sort definition
         */
        fun <E, T> desc(property: KProperty1<E, T>, nulls: NullsOrder? = null): SortBy =
            desc(property.name, nulls)

        /**
         * 从多个路径创建升序排序
         * Create ascending sort from multiple paths
         *
         * @param paths 字段路径列表 / Field path list
         * @return 排序定义 / Sort definition
         */
        fun asc(vararg paths: String): SortBy =
            SortBy(paths.map { SortItem(it, SortDirection.Asc) })

        /**
         * 从多个路径创建降序排序
         * Create descending sort from multiple paths
         *
         * @param paths 字段路径列表 / Field path list
         * @return 排序定义 / Sort definition
         */
        fun desc(vararg paths: String): SortBy =
            SortBy(paths.map { SortItem(it, SortDirection.Desc) })

        /**
         * 从多个属性创建升序排序
         * Create ascending sort from multiple properties
         *
         * @param properties 属性引用列表 / Property reference list
         * @return 排序定义 / Sort definition
         */
        fun <E> asc(vararg properties: KProperty1<E, *>): SortBy =
            SortBy(properties.map { SortItem(it.name, SortDirection.Asc) })

        /**
         * 从多个属性创建降序排序
         * Create descending sort from multiple properties
         *
         * @param properties 属性引用列表 / Property reference list
         * @return 排序定义 / Sort definition
         */
        fun <E> desc(vararg properties: KProperty1<E, *>): SortBy =
            SortBy(properties.map { SortItem(it.name, SortDirection.Desc) })
    }

    /**
     * 组合多个排序
     * Combine multiple sorts
     */
    operator fun plus(other: SortBy): SortBy = SortBy(items + other.items)

    /**
     * 添加升序排序项
     * Add ascending sort item
     *
     * @param path 字段路径 / Field path
     * @param nulls 空值排序策略（可为 null） / Nulls order strategy (nullable)
     * @return 排序定义 / Sort definition
     */
    fun thenAsc(path: String, nulls: NullsOrder? = null): SortBy =
        SortBy(items + SortItem(path, SortDirection.Asc, nulls))

    /**
     * 添加降序排序项
     * Add descending sort item
     *
     * @param path 字段路径 / Field path
     * @param nulls 空值排序策略（可为 null） / Nulls order strategy (nullable)
     * @return 排序定义 / Sort definition
     */
    fun thenDesc(path: String, nulls: NullsOrder? = null): SortBy =
        SortBy(items + SortItem(path, SortDirection.Desc, nulls))

    /**
     * 添加属性升序排序
     * Add ascending property sort item
     */
    fun <E, T> thenAsc(property: KProperty1<E, T>, nulls: NullsOrder? = null): SortBy =
        thenAsc(property.name, nulls)

    /**
     * 添加属性降序排序
     * Add descending property sort item
     */
    fun <E, T> thenDesc(property: KProperty1<E, T>, nulls: NullsOrder? = null): SortBy =
        thenDesc(property.name, nulls)

    /**
     * 是否为空
     * Check if empty
     */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * 是否非空
     * Check if not empty
     */
    fun isNotEmpty(): Boolean = items.isNotEmpty()
}

/**
 * 排序模型
 * Sort Item
 *
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
 * Represents sorting rule for a single field.
 *
 * @property path 字段路径 / Field path
 * @property direction 排序方向 / Sort direction
 * @property nulls 空值排序策略 / Nulls order strategy
 */
data class SortItem(
    val path: String,
    val direction: SortDirection,
    val nulls: NullsOrder? = null
)

/**
 * 排序方向
 * Sort Direction
 */
enum class SortDirection {
    /**
     * 升序（从小到大）
     * Ascending (small to large)
     */
    Asc,

    /**
     * 降序（从大到小）
     * Descending (large to small)
     */
    Desc
}

/**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
 * Nulls Order Strategy
 */
enum class NullsOrder {
    /**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
     * Nulls first
     */
    NullsFirst,

    /**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
     * Nulls last
     */
    NullsLast
}

/**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
 * Nulls Order Support Detection
 *
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
 * Used to detect if database supports NULLS FIRST/LAST syntax.
 */
enum class NullsOrderSupport {
    /**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
     * Auto detect based on database type
     */
    Auto,

    /**
     * 总是支持
     * Always supported
     */
    Always,

    /**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
     * Never supported, use fallback strategy
     */
    Never,

    /**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
     * Only supported for ascending order
     */
    OnlyAsc;

    /**
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
     * Check if nulls order is supported for the given sort item
     */
    fun isSupported(item: SortItem): Boolean {
        return when (this) {
            Auto -> true  // 需要根据数据库类型判断 / Need to check based on database type
            Always -> true
            Never -> false
            OnlyAsc -> item.direction == SortDirection.Asc
        }
    }
}
