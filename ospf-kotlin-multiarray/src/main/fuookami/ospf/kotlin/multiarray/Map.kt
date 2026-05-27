/**
 * Map 扩展操作符函数
 * Map extension operator functions
 *
 * 为 Map 类型提供多维数组访问的扩展操作符。
 * Provides extension operators for Map types to access multi-dimensional arrays.
 *
 * 支持的功能：
 * Supported features:
 * - 使用 All 索引（_a）获取所有值
 *   Get all values using All index (_a)
 * - 通过键和索引访问嵌套的 MultiArray 值
 *   Access nested MultiArray values by key and index
 */
package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Map 获取操作符
 * Map get operators
 *
 * 通过键和索引从 Map 中的 MultiArray 值获取元素。
 * Get elements from MultiArray values in Map by key and index.
 */

/** 使用 All 索引获取所有值 / Get all values using All index */
operator fun <K, T : Any> Map<K, T>.get(k: DummyIndex.All): Iterable<T> {
    return this.values
}

/** 通过键和 Int 索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and Int index */
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, i: Int): T? {
    return this[k]?.get(i)
}

/** 通过键和 ULong 索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and ULong index */
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, i: ULong): T? {
    return this[k]?.get(i)
}

/** 通过键和 Indexed 索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and Indexed index */
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, e: Indexed): T? {
    return this[k]?.get(e)
}

/** 通过键和 IntArray 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and IntArray vector index */
@JvmName("mapGetByIntArray")
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, v: IntArray): T? {
    return this[k]?.get(v)
}

/** 通过键和 vararg Int 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and vararg Int vector index */
@JvmName("mapGetByInts")
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, vararg v: Int): T? {
    return this[k]?.get(v)
}

/** 通过键和 ULong 迭代索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and ULong iterable index */
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, v: Iterable<ULong>): T? {
    return this[k]?.get(v)
}

/** 通过键和 vararg Indexed 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by key and vararg Indexed vector index */
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, vararg v: Indexed): T? {
    return this[k]?.get(v.map { it.index }.toIntArray())
}

/** 通过键和 vararg Any 创建视图 / Create view by key and vararg Any indices */
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, vararg v: Any): MultiArrayView<T, S>? {
    return this[k]?.get(*v)
}

/**
 * Map 设置操作符
 * Map set operators
 *
 * 通过键和索引设置 Map 中的 MutableMultiArray 值的元素。
 * Set elements in MutableMultiArray values in Map by key and index.
 */

/** 通过键和 Int 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and Int index */
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, i: Int, value: T) {
    this[k]!![i] = value
}

/** 通过键和 ULong 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and ULong index */
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, i: ULong, value: T) {
    this[k]!![i] = value
}

/** 通过键和 Indexed 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and Indexed index */
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, e: Indexed, value: T) {
    this[k]!![e] = value
}

/** 通过键和 IntArray 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and IntArray vector index */
@JvmName("setByIntArray")
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, v: IntArray, value: T) {
    this[k]!![v] = value
}

/** 通过键和 vararg Int 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and vararg Int vector index */
@JvmName("setByInts")
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, vararg v: Int, value: T) {
    this[k]!![v] = value
}

/** 通过键和 ULong 迭代索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and ULong iterable index */
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, v: Iterable<ULong>, value: T) {
    this[k]!![v] = value
}

/** 通过键和 vararg Indexed 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by key and vararg Indexed vector index */
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, vararg v: Indexed, value: T) {
    this[k]!![v.map { it.index }.toIntArray()] = value
}

/**
 * MultiMap2 获取操作符
 * MultiMap2 get operators
 *
 * 通过双键和索引从 MultiMap2 中的 MultiArray 值获取元素。
 * Get elements from MultiArray values in MultiMap2 by dual keys and index.
 */

/** 使用 All 索引获取所有第一维值 / Get all first-dimension values using All index */
operator fun <K1, K2, T : Any> MultiMap2<K1, K2, T>.get(k1: DummyIndex.All, k2: K2): Iterable<T> {
    return this.values.mapNotNull { it[k2] }
}

/** 获取指定第一维的所有第二维值 / Get all second-dimension values for specified first key */
operator fun <K1, K2, T : Any> MultiMap2<K1, K2, T>.get(k1: K1, k2: DummyIndex.All): Iterable<T> {
    return this[k1]?.values ?: emptyList()
}

/** 获取所有元素 / Get all elements */
operator fun <K1, K2, T : Any> MultiMap2<K1, K2, T>.get(k1: DummyIndex.All, k2: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it.values }
}

/** 通过双键和 Int 索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and Int index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, i: Int): T? {
    return this[k1]?.get(k2)?.get(i)
}

/** 通过双键和 ULong 索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and ULong index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, i: ULong): T? {
    return this[k1]?.get(k2)?.get(i)
}

/** 通过双键和 Indexed 索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and Indexed index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, e: Indexed): T? {
    return this[k1]?.get(k2)?.get(e)
}

/** 通过双键和 IntArray 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and IntArray vector index */
@JvmName("multiMap2GetByIntArray")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, v: IntArray): T? {
    return this[k1]?.get(k2)?.get(v)
}

/** 通过双键和 vararg Int 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and vararg Int vector index */
@JvmName("multiMap2GetByInts")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, vararg v: Int): T? {
    return this[k1]?.get(k2)?.get(v)
}

/** 通过双键和 ULong 迭代索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and ULong iterable index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, v: Iterable<ULong>): T? {
    return this[k1]?.get(k2)?.get(v)
}

/** 通过双键和 vararg Indexed 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by dual keys and vararg Indexed vector index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, vararg v: Indexed): T? {
    return this[k1]?.get(k2)?.get(v.map { it.index }.toIntArray())
}

/** 通过双键和 vararg Any 创建视图 / Create view by dual keys and vararg Any indices */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, vararg v: Any): MultiArrayView<T, S>? {
    return this[k1]?.get(k2)?.get(*v)
}

/**
 * MultiMap2 设置操作符
 * MultiMap2 set operators
 *
 * 通过双键和索引设置 MultiMap2 中的 MutableMultiArray 值的元素。
 * Set elements in MutableMultiArray values in MultiMap2 by dual keys and index.
 */

/** 通过双键和 Int 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and Int index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, i: Int, value: T) {
    this[k1, k2]!![i] = value
}

/** 通过双键和 ULong 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and ULong index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, i: ULong, value: T) {
    this[k1, k2]!![i] = value
}

/** 通过双键和 Indexed 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and Indexed index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, e: Indexed, value: T) {
    this[k1, k2]!![e] = value
}

/** 通过双键和 IntArray 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and IntArray vector index */
@JvmName("multiMap2SetByIntArray")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, v: IntArray, value: T) {
    this[k1, k2]!![v] = value
}

/** 通过双键和 vararg Int 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and vararg Int vector index */
@JvmName("multiMap2SetByInt")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, vararg v: Int, value: T) {
    this[k1, k2]!![v] = value
}

/** 通过双键和 ULong 迭代索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and ULong iterable index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, v: Iterable<ULong>, value: T) {
    this[k1, k2]!![v] = value
}

/** 通过双键和 vararg Indexed 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by dual keys and vararg Indexed vector index */
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, vararg v: Indexed, value: T) {
    this[k1, k2]!![v.map { it.index }.toIntArray()] = value
}

/**
 * MultiMap3 获取操作符
 * MultiMap3 get operators
 *
 * 通过三键和索引从 MultiMap3 中的 MultiArray 值获取元素。
 * Get elements from MultiArray values in MultiMap3 by triple keys and index.
 */

/** 使用 All 索引获取所有第一维值 / Get all first-dimension values using All index */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: DummyIndex.All, k2: K2, k3: K3): Iterable<T> {
    return this.values.mapNotNull { it[k2, k3] }
}

/** 获取指定第一维的所有第二维值 / Get all second-dimension values for specified first key */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: K1, k2: DummyIndex.All, k3: K3): Iterable<T> {
    return this[k1]?.get(k2, k3) ?: emptyList()
}

/** 获取指定第一、二维的所有第三维值 / Get all third-dimension values for specified first and second keys */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: K1, k2: K2, k3: DummyIndex.All): Iterable<T> {
    return this[k1]?.get(k2, k3) ?: emptyList()
}

/** 获取指定第三维的所有第一、二维值 / Get all first and second dimension values for specified third key */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: DummyIndex.All, k2: DummyIndex.All, k3: K3): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

/** 获取指定第一、三维的所有第二维值 / Get all second-dimension values for specified first and third keys */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: DummyIndex.All, k2: K2, k3: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

/** 获取指定第一维的所有第二、三维值 / Get all second and third dimension values for specified first key */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: K1, k2: DummyIndex.All, k3: DummyIndex.All): Iterable<T> {
    return this[k1]?.get(k2, k3) ?: emptyList()
}

/** 获取所有元素 / Get all elements */
operator fun <K1, K2, K3, T : Any> MultiMap3<K1, K2, K3, T>.get(k1: DummyIndex.All, k2: DummyIndex.All, k3: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

/** 通过三键和 Int 索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and Int index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, i: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(i)
}

/** 通过三键和 ULong 索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and ULong index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, i: ULong): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(i)
}

/** 通过三键和 Indexed 索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and Indexed index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, e: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(e)
}

/** 通过三键和 IntArray 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and IntArray vector index */
@JvmName("multiMap3GetByIntArray")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, v: IntArray): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v)
}

/** 通过三键和 vararg Int 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and vararg Int vector index */
@JvmName("multiMap3GetByInts")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, vararg v: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v)
}

/** 通过三键和 ULong 迭代索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and ULong iterable index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, v: Iterable<ULong>): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v)
}

/** 通过三键和 vararg Indexed 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by triple keys and vararg Indexed vector index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, vararg v: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v.map { it.index }.toIntArray())
}

/** 通过三键和 vararg Any 创建视图 / Create view by triple keys and vararg Any indices */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, vararg v: Any): MultiArrayView<T, S>? {
    return this[k1]?.get(k2)?.get(k3)?.get(*v)
}

/**
 * MultiMap3 设置操作符
 * MultiMap3 set operators
 *
 * 通过三键和索引设置 MultiMap3 中的 MutableMultiArray 值的元素。
 * Set elements in MutableMultiArray values in MultiMap3 by triple keys and index.
 */

/** 通过三键和 Int 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and Int index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, i: Int, value: T) {
    this[k1, k2, k3]!![i] = value
}

/** 通过三键和 ULong 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and ULong index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, i: ULong, value: T) {
    this[k1, k2, k3]!![i] = value
}

/** 通过三键和 Indexed 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and Indexed index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, e: Indexed, value: T) {
    this[k1, k2, k3]!![e] = value
}

/** 通过三键和 IntArray 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and IntArray vector index */
@JvmName("multiMap3SetByIntArray")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, v: IntArray, value: T) {
    this[k1, k2, k3]!![v] = value
}

/** 通过三键和 vararg Int 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and vararg Int vector index */
@JvmName("multiMap3SetByInts")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, vararg v: Int, value: T) {
    this[k1, k2, k3]!![v] = value
}

/** 通过三键和 ULong 迭代索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and ULong iterable index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, v: Iterable<ULong>, value: T) {
    this[k1, k2, k3]!![v] = value
}

/** 通过三键和 vararg Indexed 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by triple keys and vararg Indexed vector index */
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, vararg v: Indexed, value: T) {
    this[k1, k2, k3]!![v.map { it.index }.toIntArray()] = value
}

/**
 * MultiMap4 获取操作符
 * MultiMap4 get operators
 *
 * 通过四键和索引从 MultiMap4 中的 MultiArray 值获取元素。
 * Get elements from MultiArray values in MultiMap4 by quadruple keys and index.
 */

/** 使用 All 索引获取所有第一维值 / Get all first-dimension values using All index */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: K2, k3: K3, k4: K4): Iterable<T> {
    return this.values.mapNotNull { it[k2, k3, k4] }
}

/** 获取指定第一维的所有第二维值 / Get all second-dimension values for specified first key */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: DummyIndex.All, k3: K3, k4: K4): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取指定第一、二维的所有第三维值 / Get all third-dimension values for specified first and second keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: K2, k3: DummyIndex.All, k4: K4): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取指定第一、二、三维的所有第四维值 / Get all fourth-dimension values for specified first, second and third keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: K2, k3: K3, k4: DummyIndex.All): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取指定第三、四维的所有第一、二维值 / Get all first and second dimension values for specified third and fourth keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: DummyIndex.All, k3: K3, k4: K4): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 获取指定第二、四维的所有第一、三维值 / Get all first and third dimension values for specified second and fourth keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: K2, k3: DummyIndex.All, k4: K4): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 获取指定第二、三维的所有第一、四维值 / Get all first and fourth dimension values for specified second and third keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: K2, k3: K3, k4: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 获取指定第一维的所有第二、三、四维值 / Get all second, third and fourth dimension values for specified first key */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: DummyIndex.All, k3: DummyIndex.All, k4: K4): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取指定第一、三维的所有第二、四维值 / Get all second and fourth dimension values for specified first and third keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: DummyIndex.All, k3: K3, k4: DummyIndex.All): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取指定第一、四维的所有第二、三维值 / Get all second and third dimension values for specified first and fourth keys */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: K2, k3: DummyIndex.All, k4: DummyIndex.All): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取指定第四维的所有第一、二、三维值 / Get all first, second and third dimension values for specified fourth key */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: DummyIndex.All, k3: DummyIndex.All, k4: K4): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 获取指定第三维的所有第一、二、四维值 / Get all first, second and fourth dimension values for specified third key */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: DummyIndex.All, k3: K3, k4: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 获取指定第二维的所有第一、三、四维值 / Get all first, third and fourth dimension values for specified second key */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: K2, k3: DummyIndex.All, k4: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 获取指定第一维的所有第二、三、四维值 / Get all second, third and fourth dimension values for specified first key */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: DummyIndex.All, k3: DummyIndex.All, k4: DummyIndex.All): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

/** 获取所有元素 / Get all elements */
operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: DummyIndex.All, k2: DummyIndex.All, k3: DummyIndex.All, k4: DummyIndex.All): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

/** 通过四键和 Int 索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and Int index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, i: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(i)
}

/** 通过四键和 ULong 索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and ULong index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, i: ULong): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(i)
}

/** 通过四键和 Indexed 索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and Indexed index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, e: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(e)
}

/** 通过四键和 IntArray 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and IntArray vector index */
@JvmName("multiMap4GetByIntArray")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, v: IntArray): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v)
}

/** 通过四键和 vararg Int 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and vararg Int vector index */
@JvmName("multiMap4GetByInts")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v)
}

/** 通过四键和 ULong 迭代索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and ULong iterable index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, v: Iterable<ULong>): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v)
}

/** 通过四键和 vararg Indexed 向量索引从 MultiArray 值获取元素 / Get element from MultiArray value by quadruple keys and vararg Indexed vector index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v.map { it.index }.toIntArray())
}

/** 通过四键和 vararg Any 创建视图 / Create view by quadruple keys and vararg Any indices */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Any): MultiArrayView<T, S>? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(*v)
}

/**
 * MultiMap4 设置操作符
 * MultiMap4 set operators
 *
 * 通过四键和索引设置 MultiMap4 中的 MutableMultiArray 值的元素。
 * Set elements in MutableMultiArray values in MultiMap4 by quadruple keys and index.
 */

/** 通过四键和 Int 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and Int index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, i: Int, value: T) {
    this[k1, k2, k3, k4]!![i] = value
}

/** 通过四键和 ULong 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and ULong index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, i: ULong, value: T) {
    this[k1, k2, k3, k4]!![i] = value
}

/** 通过四键和 Indexed 索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and Indexed index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, e: Indexed, value: T) {
    this[k1, k2, k3, k4]!![e] = value
}

/** 通过四键和 IntArray 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and IntArray vector index */
@JvmName("multiMap4SetByIntArray")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, v: IntArray, value: T) {
    this[k1, k2, k3, k4]!![v] = value
}

/** 通过四键和 vararg Int 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and vararg Int vector index */
@JvmName("multiMap4SetByInts")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Int, value: T) {
    this[k1, k2, k3, k4]!![v] = value
}

/** 通过四键和 ULong 迭代索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and ULong iterable index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, v: Iterable<ULong>, value: T) {
    this[k1, k2, k3, k4]!![v] = value
}

/** 通过四键和 vararg Indexed 向量索引设置 MutableMultiArray 值的元素 / Set element in MutableMultiArray value by quadruple keys and vararg Indexed vector index */
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Indexed, value: T) {
    this[k1, k2, k3, k4]!![v.map { it.index }.toIntArray()] = value
}
