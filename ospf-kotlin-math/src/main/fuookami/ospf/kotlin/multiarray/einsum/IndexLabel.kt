/**
 * 索引标签和索引列表定义
 * Index labels and index lists definitions
 *
 * 提供爱因斯坦表示法的索引标记系统。
 * Provides index label system for Einstein notation.
 *
 * 圌Rust 中使用类型级别的编程实现编译期索引跟踪，
 * 圌Kotlin 中使用枚举和运行时列表实现类似功能。
 * In Rust, type-level programming is used for compile-time index tracking.
 * In Kotlin, enums and runtime lists are used for similar functionality.
 */
package fuookami.ospf.kotlin.multiarray.einsum

// ============================================================================
// IndexLabel - 索引标签枚举
// ============================================================================

/**
 * 索引标签 / Index label
 *
 * 用于标识张量的索引维度。
 * Used to identify tensor dimension indices.
 *
 * 内置索引标签，
 * Built-in index labels:
 * - I: 第一维度索引 / First dimension index (id=0)
 * - J: 第二维度索引 / Second dimension index (id=1)
 * - K: 第三维度索引 / Third dimension index (id=2)
 * - L: 第四维度索引 / Fourth dimension index (id=3)
 * - M: 第五维度索引 / Fifth dimension index (id=4)
 * - N: 第六维度索引 / Sixth dimension index (id=5)
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.IndexLabel
 *
 * // 使用内置索引标签
 * // Use built-in index labels
 * val i = IndexLabel.I
 * val j = IndexLabel.J
 *
 * println(i.name)  // "i"
 * println(i.id)    // 0
 * ```
 */
enum class IndexLabel(
    /**
     * 索引名称（用于调试和显示，
     * Index name (for debugging and display)
     */
    val labelName: String,
    /**
     * 索引的唯一标识笌
     * Unique identifier for the index
     *
     * 用于在运行时比较索引是否相同。
     * Used to compare indices at runtime.
     */
    val id: Int
) {
    I("i", 0),
    J("j", 1),
    K("k", 2),
    L("l", 3),
    M("m", 4),
    N("n", 5);

    companion object {
        /**
         * 根据名称查找索引标签
         * Find index label by name
         *
         * @param name 索引名称（如 "i", "j", "k"，
         * @return 对应的索引标签，如果不存在则返回 null
         */
        fun fromName(name: String): IndexLabel? {
            return values().find { it.labelName == name }
        }

        /**
         * 根据 ID 查找索引标签
         * Find index label by ID
         *
         * @param id 索引 ID，-5，
         * @return 对应的索引标签，如果不存在则返回 null
         */
        fun fromId(id: Int): IndexLabel? {
            return values().find { it.id == id }
        }
    }
}

// ============================================================================
// IndexList - 索引列表
// ============================================================================

/**
 * 索引列表 / Index list
 *
 * 表示一组索引的有序集合。
 * Represents an ordered collection of indices.
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.*
 *
 * // 创建索引列表
 * // Create index list
 * val indices = IndexList(IndexLabel.I, IndexLabel.J)
 *
 * println(indices.names)  // "i, j"
 * println(indices.ids)    // [0, 1]
 * println(indices.length) // 2
 * ```
 *
 * @param indices 索引标签数组
 */
data class IndexList(
    val indices: List<IndexLabel>
) {
    /**
     * 列表长度
     * List length
     */
    val length: Int = indices.size

    /**
     * 将索引列表转换为索引 ID 列表
     * Convert index list to list of index IDs
     */
    val ids: List<Int> = indices.map { it.id }

    /**
     * 将索引列表转换为名称字符串
     * Convert index list to name string
     */
    val names: String = indices.joinToString(", ") { it.labelName }

    /**
     * 检查是否为空
     * Check if empty
     *
     * @return 如果为空则返回true / true if empty
     */
    fun isEmpty(): Boolean = indices.isEmpty()

    /**
     * 检查是否包含指定索引
     * Check if contains specified index
     *
     * @param label 要检查的索引标签
     * @return 如果包含则返回true
     */
    fun contains(label: IndexLabel): Boolean = indices.contains(label)

    /**
     * 检查是否包含指定索引ID
     * Check if contains specified index ID
     *
     * @param id 要检查的索引 ID
     * @return 如果包含则返回true
     */
    fun containsId(id: Int): Boolean = ids.contains(id)

    /**
     * 获取指定位置索引的维度位罌
     * Get dimension position for index at specified position
     *
     * @param label 索引标签
     * @return 该索引在列表中的位置，如果不存在则返回null
     */
    fun positionOf(label: IndexLabel): Int? = indices.indexOf(label).let { if (it >= 0) it else null }

    /**
     * 获取指定索引 ID 的维度位罌
     * Get dimension position for specified index ID
     *
     * @param id 索引 ID
     * @return 该索引在列表中的位置，如果不存在则返回null
     */
    fun positionOfId(id: Int): Int? = ids.indexOf(id).let { if (it >= 0) it else null }

    companion object {
        /**
         * 空索引列表
         * Empty index list
         */
        val Empty = IndexList(emptyList())

        /**
         * 从索引标签创建索引列表
         * Create index list from index labels
         *
         * @param labels the index labels / 索引标签
         * @return the created index list / 创建的索引列表
         */
        fun of(vararg labels: IndexLabel): IndexList = IndexList(labels.toList())

        /**
         * 从索引ID 创建索引列表
         * Create index list from index IDs
         *
         * @param ids the index IDs / 索引 ID 列表
         * @return the created index list / 创建的索引列表
         */
        fun fromIds(ids: List<Int>): IndexList {
            return IndexList(ids.mapNotNull { IndexLabel.fromId(it) })
        }

        /**
         * 从名称字符串解析索引列表
         * Parse index list from name string
         *
         * @param names 索引名称字符串（妌"i,j,k" 戌"ijk"，
         * @return 解析后的索引列表
         */
        fun fromNames(names: String): IndexList {
            val trimmed = names.trim()
            if (trimmed.isEmpty()) return Empty

            // 支持逗号分隔或不分隔的格式
            // Support comma-separated or non-separated format
            val parts = if (trimmed.contains(",")) {
                trimmed.split(",").map { it.trim() }
            } else {
                trimmed.chunked(1)
            }

            return IndexList(parts.mapNotNull { IndexLabel.fromName(it) })
        }
    }
}

// ============================================================================
// 辅助函数 / Helper Functions
// ============================================================================

/**
 * 查找两个索引列表的公共索引（求和索引，
 * Find common indices (summation indices) between two index lists
 *
 * 爱因斯坦表示法中，公共索引表示需要求和的维度。
 * In Einstein notation, common indices represent dimensions to be summed over.
 *
 * 示例 / Example:
 *
 * ```kotlin
 * val lhs = IndexList.of(IndexLabel.I, IndexLabel.J, IndexLabel.K)  // i, j, k
 * val rhs = IndexList.of(IndexLabel.J, IndexLabel.K, IndexLabel.L)  // j, k, l
 *
 * val common = findCommonIndices(lhs, rhs)
 * // 结果: j, k
 * // Result: j, k
 * ```
 *
 * @param lhs 左操作数的索引列表
 * @param rhs 右操作数的索引列表
 * @return 公共索引列表
 */
fun findCommonIndices(lhs: IndexList, rhs: IndexList): IndexList {
    val common = lhs.indices.filter { rhs.contains(it) }
    return IndexList(common)
}

/**
 * 从索引列表中移除指定索引
 * Remove specified indices from an index list
 *
 * 示例 / Example:
 *
 * ```kotlin
 * val indices = IndexList.of(IndexLabel.I, IndexLabel.J, IndexLabel.K, IndexLabel.L)
 * val toRemove = IndexList.of(IndexLabel.J, IndexLabel.L)
 *
 * val result = removeIndices(indices, toRemove)
 * // 结果: i, k
 * // Result: i, k
 * ```
 *
 * @param indices 原索引列表
 * @param toRemove 要移除的索引列表
 * @return 移除指定索引后的新列表
 */
fun removeIndices(indices: IndexList, toRemove: IndexList): IndexList {
    return IndexList(indices.indices.filter { !toRemove.contains(it) })
}

/**
 * 合并两个索引列表，保持唯一怌
 * Merge two index lists, maintaining uniqueness
 *
 * @param lhs 左操作数的索引列表
 * @param rhs 右操作数的索引列表
 * @return 合并后的唯一索引列表
 */
fun mergeIndices(lhs: IndexList, rhs: IndexList): IndexList {
    val merged = lhs.indices.toMutableList()
    for (idx in rhs.indices) {
        if (!merged.contains(idx)) {
            merged.add(idx)
        }
    }
    return IndexList(merged)
}
