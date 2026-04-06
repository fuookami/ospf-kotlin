/**
 * 排序模型
 * Sort Model
 *
 * 定义查询排序规则，支持多字段、方向和空值顺序。
 * Defines query sorting rules, supporting multiple fields, directions, and nulls order.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

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
         */
        fun asc(path: String, nulls: NullsOrder? = null): SortBy =
            SortBy(listOf(SortItem(path, SortDirection.Asc, nulls)))

        /**
         * 降序排序
         * Descending sort
         */
        fun desc(path: String, nulls: NullsOrder? = null): SortBy =
            SortBy(listOf(SortItem(path, SortDirection.Desc, nulls)))

        /**
         * 从多个路径创建升序排序
         * Create ascending sort from multiple paths
         */
        fun asc(vararg paths: String): SortBy =
            SortBy(paths.map { SortItem(it, SortDirection.Asc) })

        /**
         * 从多个路径创建降序排序
         * Create descending sort from multiple paths
         */
        fun desc(vararg paths: String): SortBy =
            SortBy(paths.map { SortItem(it, SortDirection.Desc) })
    }

    /**
     * 组合多个排序
     * Combine multiple sorts
     */
    operator fun plus(other: SortBy): SortBy = SortBy(items + other.items)

    /**
     * 添加升序排序项
     * Add ascending sort item
     */
    fun thenAsc(path: String, nulls: NullsOrder? = null): SortBy =
        SortBy(items + SortItem(path, SortDirection.Asc, nulls))

    /**
     * 添加降序排序项
     * Add descending sort item
     */
    fun thenDesc(path: String, nulls: NullsOrder? = null): SortBy =
        SortBy(items + SortItem(path, SortDirection.Desc, nulls))

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
 * 排序项
 * Sort Item
 *
 * 表示单个字段的排序规则。
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
 * 空值排序策略
 * Nulls Order Strategy
 */
enum class NullsOrder {
    /**
     * 空值排在前面
     * Nulls first
     */
    NullsFirst,

    /**
     * 空值排在后面
     * Nulls last
     */
    NullsLast
}

/**
 * 空值排序支持检测
 * Nulls Order Support Detection
 *
 * 用于检测数据库是否支持 NULLS FIRST/LAST 语法。
 * Used to detect if database supports NULLS FIRST/LAST syntax.
 */
enum class NullsOrderSupport {
    /**
     * 自动检测
     * Auto detect based on database type
     */
    Auto,

    /**
     * 总是支持
     * Always supported
     */
    Always,

    /**
     * 从不支持，使用降级策略
     * Never supported, use fallback strategy
     */
    Never,

    /**
     * 仅在升序时支持
     * Only supported for ascending order
     */
    OnlyAsc;

    /**
     * 检查是否支持指定排序项的空值排序
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