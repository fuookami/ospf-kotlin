/**
 * 多维列表和查找结果
 *
 * Multi-dimensional list type aliases and extension functions.
 * 多维列表类型别名和扩展函数。
*/
package fuookami.ospf.kotlin.utils.functional

/**
 * 二维列表类型别名
 *
 * Type alias for a 2-dimensional list (List of List).
 * 二维列表的类型别名（列表的列表）。
*/
typealias List2<T> = List<List<T>>

/**
 * 二维可变列表类型别名
 *
 * Type alias for a 2-dimensional mutable list.
 * 二维可变列表的类型别名。
*/
typealias MutableList2<T> = MutableList<MutableList<T>>

/**
 * 三维列表类型别名
 *
 * Type alias for a 3-dimensional list.
 * 三维列表的类型别名。
*/
typealias List3<T> = List<List<List<T>>>

/**
 * 三维可变列表类型别名
 *
 * Type alias for a 3-dimensional mutable list.
 * 三维可变列表的类型别名。
*/
typealias MutableList3<T> = MutableList<MutableList<MutableList<T>>>

/**
 * 二维列表的双索引获取操作
 *
 * Dual-index access operator for 2-dimensional lists.
 * 二维列表的双索引获取操作符。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param i 第一维索引 / The first dimension index
 * @param j 第二维索引 / The second dimension index
 * @return 位于 (i, j) 位置的元素 / The element at position (i, j)
*/
operator fun <T> List<List<T>>.get(i: Int, j: Int): T {
    return this[i][j]
}

/**
 * 二维列表的安全双索引获取操作
 *
 * Safe dual-index access for 2-dimensional lists, returning null if indices are out of bounds.
 * 二维列表的安全双索引获取，如果索引越界则返回 null。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param i 第一维索引 / The first dimension index
 * @param j 第二维索引 / The second dimension index
 * @return 位于 (i, j) 位置的元素，如果索引越界则返回 null / The element at position (i, j), or null if indices are out of bounds
*/
fun <T> List<List<T>>.getOrNull(i: Int, j: Int): T? {
    return this.getOrNull(i)?.getOrNull(j)
}

/**
 * 二维可变列表的双索引设置操作
 *
 * Dual-index set operator for 2-dimensional mutable lists.
 * 二维可变列表的双索引设置操作符。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param i 第一维索引 / The first dimension index
 * @param j 第二维索引 / The second dimension index
 * @param value 要设置的值 / The value to set
*/
operator fun <T> List<MutableList<T>>.set(i: Int, j: Int, value: T) {
    this[i][j] = value
}

/**
 * 三维列表的三索引获取操作
 *
 * Triple-index access operator for 3-dimensional lists.
 * 三维列表的三索引获取操作符。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param i 第一维索引 / The first dimension index
 * @param j 第二维索引 / The second dimension index
 * @param k 第三维索引 / The third dimension index
 * @return 位于 (i, j, k) 位置的元素 / The element at position (i, j, k)
*/
operator fun <T> List3<T>.get(i: Int, j: Int, k: Int): T {
    return this[i][j][k]
}

/**
 * 三维列表的安全三索引获取操作
 *
 * Safe triple-index access for 3-dimensional lists, returning null if indices are out of bounds.
 * 三维列表的安全三索引获取，如果索引越界则返回 null。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param i 第一维索引 / The first dimension index
 * @param j 第二维索引 / The second dimension index
 * @param k 第三维索引 / The third dimension index
 * @return 位于 (i, j, k) 位置的元素，如果索引越界则返回 null / The element at position (i, j, k), or null if indices are out of bounds
*/
fun <T> List3<T>.getOrNull(i: Int, j: Int, k: Int): T? {
    return this.getOrNull(i)?.getOrNull(j)?.getOrNull(k)
}

/**
 * 三维可变列表的三索引设置操作
 *
 * Triple-index set operator for 3-dimensional mutable lists.
 * 三维可变列表的三索引设置操作符。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param i 第一维索引 / The first dimension index
 * @param j 第二维索引 / The second dimension index
 * @param k 第三维索引 / The third dimension index
 * @param value 要设置的值 / The value to set
*/
operator fun <T> List<List<MutableList<T>>>.set(i: Int, j: Int, k: Int, value: T) {
    this[i][j][k] = value
}

/**
 * 列表查找结果
 *
 * Result of finding an element in a mutable list, supporting default value handling.
 * 在可变列表中查找元素的结果，支持默认值处理。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param self 源可变列表 / The source mutable list
 * @param result 查找到的元素，如果没有找到则为 null / The found element, or null if not found
*/
data class ListFindResult<T>(
    val self: MutableList<T>,
    val result: T?
) {

    /**
     * 如果找到则返回结果，否则添加默认值并返回
     *
     * Returns the found result if present, otherwise adds a new value from the default provider and returns it.
     * 如果找到结果则返回，否则添加来自默认提供者的新值并返回。
     *
     * @param default 默认值提供者 / The default value provider
     * @return 找到的元素或新添加的默认值 / The found element or the newly added default value
    */
    fun add(default: () -> T): T {
        return if (result != null) {
            result
        } else {
            val newValue = default()
            self.add(newValue)
            newValue
        }
    }

    /**
     * 如果找到则返回结果，否则返回默认值
     *
     * Returns the found result if present, otherwise returns a value from the default provider.
     * 如果找到结果则返回，否则返回来自默认提供者的值。
     *
     * @param default 默认值提供者 / The default value provider
     * @return 找到的元素或默认值 / The found element or the default value
    */
    fun default(default: () -> T): T {
        return result ?: default()
    }
}

/**
 * 查找元素或返回查找结果对象
 *
 * Finds an element in the mutable list matching the predicate, returning a ListFindResult.
 * 在可变列表中查找匹配谓词的元素，返回 ListFindResult。
 *
 * @param T 列表元素的类型 / The type of list elements
 * @param predicate 查找谓词 / The search predicate
 * @return 包含列表和查找结果的 ListFindResult / A ListFindResult containing the list and search result
*/
fun <T> MutableList<T>.findOr(predicate: (T) -> Boolean): ListFindResult<T> {
    return ListFindResult(this, this.find(predicate))
}
