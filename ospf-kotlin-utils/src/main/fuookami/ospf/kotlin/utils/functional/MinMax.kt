/**
 * 最小最大值计算
 *
 * Extension functions for finding minimum and maximum values simultaneously.
 * Provides efficient single-pass min/max computation.
 * 同时查找最小值和最大值的扩展函数。
 * 提供高效的单次遍历最小/最大值计算。
 */
package fuookami.ospf.kotlin.utils.functional

/**
 * 使用比较器同时查找最小和最大值
 *
 * Finds both minimum and maximum values in the iterable using a comparator.
 * Returns a Pair where first is minimum and second is maximum.
 * 使用比较器在迭代器中同时查找最小值和最大值。
 * 返回一个 Pair，其中 first 是最小值，second 是最大值。
 *
 * @param T 比较值的类型 / The type of values to compare
 * @param U 迭代器元素的类型 / The type of iterable elements
 * @param comparator 用于比较值的比较器 / The comparator for comparing values
 * @param extractor 从元素中提取比较值的函数 / The function to extract comparison value from element
 * @return 包含最小值和最大值的 Pair / A Pair containing minimum and maximum values
 * @throws NoSuchElementException 如果迭代器为空 / If the iterable is empty
 */
inline fun <T, U> Iterable<U>.minMaxOfWith(
    comparator: kotlin.Comparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    val iterator = this.iterator()
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}

/**
 * 使用比较器同时查找最小和最大值（可空版本）
 *
 * Finds both minimum and maximum values in the iterable using a comparator.
 * Returns a Pair where first is minimum and second is maximum, or null if empty.
 * 使用比较器在迭代器中同时查找最小值和最大值。
 * 返回一个 Pair，其中 first 是最小值，second 是最大值；如果为空则返回 null。
 *
 * @param T 比较值的类型 / The type of values to compare
 * @param U 迭代器元素的类型 / The type of iterable elements
 * @param comparator 用于比较值的比较器 / The comparator for comparing values
 * @param extractor 从元素中提取比较值的函数 / The function to extract comparison value from element
 * @return 包含最小值和最大值的 Pair，如果迭代器为空则返回 null / A Pair containing minimum and maximum values, or null if iterable is empty
 */
inline fun <T, U> Iterable<U>.minMaxOfWithOrNull(
    comparator: kotlin.Comparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}