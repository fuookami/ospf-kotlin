package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus

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

        operator fun invoke(x: Flt64, y: Flt64): Point2 {
            return Point(listOf(x, y), Dim2)
        }

        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Point3 {
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

typealias Point2 = Point<Dim2, Flt64>
typealias Point3 = Point<Dim3, Flt64>

@get:JvmName("Point2X")
val Point2.x get() = this[0]

@get:JvmName("Point2Y")
val Point2.y get() = this[1]

val Point2.pair get() = Pair(x, y)

val originPoint2 = point2()

fun point2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Point2 {
    return Point2(x, y)
}

@get:JvmName("Point3X")
val Point3.x get() = this[0]

@get:JvmName("Point3Y")
val Point3.y get() = this[1]

@get:JvmName("Point3Z")
val Point3.z get() = this[2]

val Point3.triple get() = Triple(x, y, z)

val originPoint3 = point3()

fun point3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Point3 {
    return Point3(x, y, z)
}
