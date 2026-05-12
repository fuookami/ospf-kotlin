/**
 * 点
 * Point
 *
 * 定义几何空间中的点数据结构，支持任意维度和数值类型。
 * 点是几何空间中的基本元素，表示空间中的一个位置。
 * Defines point data structure in geometric space, supporting arbitrary dimensions and number types.
 * A point is a fundamental element in geometric space, representing a position.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus

/**
 * 点数据类
 * Point Data Class
 *
 * 表示几何空间中的点，支持任意维度和浮点数类型。
 * 点可以与向量进行加减运算，支持距离计算和相等性比较。
 * Represents a point in geometric space, supporting arbitrary dimensions and floating-point types.
 * Points can be added to or subtracted by vectors, supporting distance calculation and equality comparison.
 *
 * @param D 维度类型 / Dimension type
 * @param V 数值类型 / Number type
 * @property position 点的坐标列表 / List of coordinates of the point
 * @property dim 维度信息 / Dimension information
 */
data class Point<D : Dimension, V : FloatingNumber<V>>(
    val position: List<V>,
    val dim: D
) : Plus<Point<D, V>, Point<D, V>>, Minus<Point<D, V>, Point<D, V>>, Eq<Point<D, V>> {
    companion object {
        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(x: V, y: V): Point<D, V> {
            return Point(listOf(x, y), Dim2 as D)
        }

        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(x: V, y: V, z: V): Point<D, V> {
            return Point(listOf(x, y, z), Dim3 as D)
        }

        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(vector: Vector<D, V>): Point<D, V> {
            return Point(vector.vector, vector.dim)
        }

        operator fun invoke(x: Flt64, y: Flt64): Point<Dim2, Flt64> {
            return Point(listOf(x, y), Dim2)
        }

        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Point<Dim3, Flt64> {
            return Point(listOf(x, y, z), Dim3)
        }
    }

    init {
        assert(position.size == dim.size)
    }

    val size by dim::size
    val indices by dim::indices

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): V {
        return position[i]
    }

    infix fun distance(rhs: Point<D, V>): V = Distance.Euclidean(this, rhs)

    fun distanceBetween(rhs: Point<D, V>, type: Distance = Distance.Euclidean): V = type(this, rhs)

    override fun plus(rhs: Point<D, V>) = Point(indices.map { this[it] + rhs[it] }, dim)

    override fun minus(rhs: Point<D, V>) = Point(indices.map { this[it] - rhs[it] }, dim)

    operator fun plus(rhs: Vector<D, V>) = Point(indices.map { this[it] + rhs[it] }, dim)

    operator fun minus(rhs: Vector<D, V>) = Point(indices.map { this[it] - rhs[it] }, dim)

    override fun partialEq(rhs: Point<D, V>): Boolean {
        if (dim != rhs.dim) {
            return false
        }

        for (i in indices) {
            if (this[i] neq rhs[i]) {
                return false
            }
        }

        return true
    }

    infix fun approxEq(rhs: Point<D, V>): Boolean {
        if (dim != rhs.dim) {
            return false
        }
        val v = this[0]
        for (i in indices) {
            if ((this[i] - rhs[i]).abs() gr v.constants.decimalPrecision) {
                return false
            }
        }
        return true
    }

    fun approxEq(rhs: Point<D, V>, epsilon: V): Boolean {
        if (dim != rhs.dim) {
            return false
        }
        for (i in indices) {
            if ((this[i] - rhs[i]).abs() gr epsilon) {
                return false
            }
        }
        return true
    }

    infix fun midpoint(rhs: Point<D, V>): Point<D, V> {
        val v = this[0]
        val two = v.constants.two
        return Point(indices.map { (this[it] + rhs[it]) / two }, dim)
    }

    override fun toString() = position.joinToString(",", "[", "]")
}


@get:JvmName("Point2X")
val Point<Dim2, Flt64>.x get() = this[0]

@get:JvmName("Point2Y")
val Point<Dim2, Flt64>.y get() = this[1]

val Point<Dim2, Flt64>.pair get() = Pair(x, y)

val originPoint2 = point2()

fun point2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Point<Dim2, Flt64> {
    return Point<Dim2, Flt64>(x, y)
}

@get:JvmName("Point3X")
val Point<Dim3, Flt64>.x get() = this[0]

@get:JvmName("Point3Y")
val Point<Dim3, Flt64>.y get() = this[1]

@get:JvmName("Point3Z")
val Point<Dim3, Flt64>.z get() = this[2]

val Point<Dim3, Flt64>.triple get() = Triple(x, y, z)

val originPoint3 = point3()

fun point3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Point<Dim3, Flt64> {
    return Point<Dim3, Flt64>(x, y, z)
}
