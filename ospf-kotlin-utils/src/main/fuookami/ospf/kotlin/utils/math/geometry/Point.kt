package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

data class Point<D : Dimension>(
    val position: List<Flt64>,
    val dim: D
) : Plus<Point<D>, Point<D>>, Minus<Point<D>, Point<D>>, Eq<Point<D>> {
    companion object {
        operator fun invoke(x: Flt64, y: Flt64): Point2 {
            return Point(listOf(x, y), Dim2)
        }

        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Point3 {
            return Point(listOf(x, y, z), Dim3)
        }

        operator fun <D : Dimension> invoke(vector: Vector<D>): Point<D> {
            return Point(vector.vector, vector.dim)
        }
    }

    init {
        assert(position.size == dim.size)
    }

    val size by dim::size
    val indices by dim::indices

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): Flt64 {
        return position[i]
    }

    infix fun distance(rhs: Point<D>): Flt64 = Distance.Euclidean(this, rhs)
    fun distanceBetween(rhs: Point<D>, type: Distance = Distance.Euclidean): Flt64 = type(this, rhs)

    override fun plus(rhs: Point<D>) = Point(indices.map { this[it] + rhs[it] }, dim)
    override fun minus(rhs: Point<D>) = Point(indices.map { this[it] - rhs[it] }, dim)

    operator fun plus(rhs: Vector<D>) = Point(indices.map { this[it] + rhs[it] }, dim)
    operator fun minus(rhs: Vector<D>) = Point(indices.map { this[it] - rhs[it] }, dim)

    override fun partialEq(rhs: Point<D>): Boolean {
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

    override fun toString() = position.joinToString(",", "[", "]")
}

typealias Point2 = Point<Dim2>

@get:JvmName("Point2X")
val Point2.x get() = this[0]

@get:JvmName("Point2Y")
val Point2.y get() = this[1]

val Point2.pair get() = Pair(x, y)

val originPoint2 = point2()

fun point2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Point2 {
    return Point2(x, y)
}

typealias Point3 = Point<Dim3>

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
