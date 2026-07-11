/**
 * 多维数组扩展
 * MultiArray Extensions
 *
 * 为多维数组提侌UInt64 索引支持和形状工厂方法扩展。
 * 这些扩展函数允许使用 UInt64 类型作为索引，并提供了便捷的形状创建方法。
 *
 * Provides UInt64 index support and shape factory method extensions for multi-dimensional arrays.
 * These extension functions allow using UInt64 type as indices and provide convenient shape creation methods.
 *
 * 主要功能 / Main features:
 * - UInt64 索引访问操作笌/ UInt64 index access operators
 * - UInt64 形状工厂方法 / UInt64 shape factory methods
 * - Map 扩展支持 / Map extension support
*/
package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Ret

// ============================================================================
// UInt64 extensions for AbstractMultiArray
// ============================================================================

/**
 * 通过 UInt64 线性索引获取元素
 * Get element by UInt64 linear index
 *
 * @param i UInt64 线性索引 / The UInt64 linear index
 * @return 该索引处的元素 / The element at the given index
*/
operator fun <T : Any, S : Shape> AbstractMultiArray<T, S>.get(i: UInt64): T {
    return this[i.toInt()]
}

/**
 * 通过 UInt64 迭代获取元素
 * Get element by UInt64 iterable
 *
 * @param v UInt64 索引迭代器 / The UInt64 index iterable
 * @return 该索引处的元素 / The element at the given indices
*/
@JvmName("getByUInt64Iterable")
operator fun <T : Any, S : Shape> AbstractMultiArray<T, S>.get(v: Iterable<UInt64>): T {
    return this[v.map { it.toInt() }.toIntArray()]
}

// ============================================================================
// UInt64 extensions for MutableMultiArray
// ============================================================================

/**
 * 通过 UInt64 线性索引设置元素
 * Set element by UInt64 linear index
 *
 * @param i UInt64 线性索引 / The UInt64 linear index
 * @param value 要设置的值 / The value to set
*/
operator fun <T : Any, S : Shape> MutableMultiArray<T, S>.set(i: UInt64, value: T) {
    this[i.toInt()] = value
}

/**
 * 通过 UInt64 迭代设置元素
 * Set element by UInt64 iterable
 *
 * @param v UInt64 索引迭代器 / The UInt64 index iterable
 * @param value 要设置的值 / The value to set
*/
@JvmName("setByUInt64Iterable")
operator fun <T : Any, S : Shape> MutableMultiArray<T, S>.set(v: Iterable<UInt64>, value: T) {
    this[v.map { it.toInt() }.toIntArray()] = value
}

// ============================================================================
// UInt64 extensions for MultiArrayView
// ============================================================================

/**
 * 通过 UInt64 线性索引获取元素
 * Get element by UInt64 linear index
 *
 * @param i UInt64 线性索引 / The UInt64 linear index
 * @return 该索引处的元素 / The element at the given index
*/
operator fun <T : Any, S : Shape> MultiArrayView<T, S>.get(i: UInt64): T {
    return this[i.toInt()]
}

/**
 * 通过 UInt64 迭代获取元素
 * Get element by UInt64 iterable
 *
 * @param v UInt64 索引迭代器 / The UInt64 index iterable
 * @return 该索引处的元素 / The element at the given indices
*/
@JvmName("viewGetByUInt64Iterable")
operator fun <T : Any, S : Shape> MultiArrayView<T, S>.get(v: Iterable<UInt64>): Ret<T> {
    return this[v.map { it.toInt() }.toIntArray()]
}

// ============================================================================
// UInt64 extensions for MappedMultiArrayView
// ============================================================================

/**
 * 通过 UInt64 线性索引获取元素
 * Get element by UInt64 linear index
 *
 * @param i UInt64 线性索引 / The UInt64 linear index
 * @return 该索引处的元素 / The element at the given index
*/
operator fun <T : Any, S : Shape> MappedMultiArrayView<T, S>.get(i: UInt64): T {
    return this[i.toInt()]
}

// ============================================================================
// UInt64 extensions for Shape
// ============================================================================

/**
 * 维度数量（UInt64）
 * Number of dimensions (UInt64)
*/
val Shape.udimensionUInt64: UInt64 get() = UInt64(dimension.toULong())

/**
 * 元素总数（UInt64）
 * Total number of elements (UInt64)
*/
val Shape.usizeUInt64: UInt64 get() = UInt64(size.toULong())

// ============================================================================
// UInt64 extensions for Shape factory methods
// ============================================================================

/**
 * 使用 UInt64 创建一维形状
 * Create one-dimensional shape with UInt64
 *
 * @param d1 第一维度大小 / First dimension size
 * @return 一维形状 / One-dimensional shape
*/
fun Shape1(d1: UInt64): Shape1 = Shape1(d1.toInt())

/**
 * 使用 UInt64 创建二维形状
 * Create two-dimensional shape with UInt64
 *
 * @param d1 第一维度大小 / First dimension size
 * @param d2 第二维度大小 / Second dimension size
 * @return 二维形状 / Two-dimensional shape
*/
fun Shape2(d1: UInt64, d2: UInt64): Shape2 = Shape2(d1.toInt(), d2.toInt())

/**
 * 使用 UInt64 创建三维形状
 * Create three-dimensional shape with UInt64
 *
 * @param d1 第一维度大小 / First dimension size
 * @param d2 第二维度大小 / Second dimension size
 * @param d3 第三维度大小 / Third dimension size
 * @return 三维形状 / Three-dimensional shape
*/
fun Shape3(d1: UInt64, d2: UInt64, d3: UInt64): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt())

/**
 * 使用 UInt64 创建四维形状
 * Create four-dimensional shape with UInt64
 *
 * @param d1 第一维度大小 / First dimension size
 * @param d2 第二维度大小 / Second dimension size
 * @param d3 第三维度大小 / Third dimension size
 * @param d4 第四维度大小 / Fourth dimension size
 * @return 四维形状 / Four-dimensional shape
*/
fun Shape4(d1: UInt64, d2: UInt64, d3: UInt64, d4: UInt64): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt())

/**
 * 使用 UInt64 列表创建动态形状
 * Create dynamic shape with UInt64 list
 *
 * @param shape UInt64 维度大小列表 / List of UInt64 dimension sizes
 * @return 动态形状 / Dynamic shape
*/
fun DynShape(shape: List<UInt64>): DynShape = DynShape(shape.map { it.toInt() }.toIntArray())

// ============================================================================
// Map extensions with UInt64
// ============================================================================

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, i: UInt64): T? {
    return this[k]?.get(i.toInt())
}

@JvmName("mapGetByUInt64Iterable")
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, v: Iterable<UInt64>): T? {
    return this[k]?.get(v.map { it.toInt() }.toIntArray())
}

operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, i: UInt64, value: T) {
    this[k]?.set(i.toInt(), value)
}

@JvmName("mapSetByUInt64Iterable")
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, v: Iterable<UInt64>, value: T) {
    this[k]?.set(v.map { it.toInt() }.toIntArray(), value)
}
