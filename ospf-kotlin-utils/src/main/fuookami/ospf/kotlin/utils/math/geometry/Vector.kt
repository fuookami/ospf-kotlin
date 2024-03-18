package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

private fun normOf(vector: List<Flt64>): Flt64 {
    return (vector.sumOf { it.sqr() }).sqrt()
}

private fun unitOf(vector: List<Flt64>): List<Flt64> {
    val norm = normOf(vector)
    return vector.map { it / norm }
}

private fun timesBetween(lhs: List<Flt64>, rhs: List<Flt64>): Flt64 {
    assert(lhs.size == rhs.size)
    return lhs.indices.sumOf { lhs[it] * rhs[it] }
}

open class Vector<D : Dimension>(
    val vector: List<Flt64>,
    val dim: D
) : Plus<Vector<D>, Vector<D>>, Minus<Vector<D>, Vector<D>>, Times<Vector<D>, Flt64> {
    companion object {
        operator fun invoke(x: Flt64, y: Flt64): Vector2 {
            return Vector2(listOf(x, y), Dim2)
        }

        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Vector3 {
            return Vector3(listOf(x, y, z), Dim3)
        }
    }

    val size by vector::size
    val indices by vector::indices
    val norm: Flt64 by lazy { normOf(vector) }
    open val unit: Vector<D> by lazy { Vector(unitOf(vector), dim) }

    init {
        assert(vector.size == dim.size)
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): Flt64 {
        return vector[i]
    }

    override operator fun plus(rhs: Vector<D>) = Vector(indices.map { this[it] + rhs[it] }, dim)
    override operator fun minus(rhs: Vector<D>) = Vector(indices.map { this[it] + rhs[it] }, dim)
    override operator fun times(rhs: Vector<D>) = timesBetween(vector, rhs.vector)

    operator fun plus(rhs: Point<D>) = Point(indices.map { this[it] + rhs[it] }, dim)

    override fun toString() = vector.joinToString(",", "[", "]")
}

typealias Vector2 = Vector<Dim2>

@get:JvmName("Vector2X")
val Vector2.x get() = this[0]

@get:JvmName("Vector2Y")
val Vector2.y get() = this[1]

fun vector2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Vector2 {
    return Vector2(x, y)
}

typealias Vector3 = Vector<Dim3>

@get:JvmName("Vector3X")
val Vector3.x get() = this[0]

@get:JvmName("Vector3Y")
val Vector3.y get() = this[1]

@get:JvmName("Vector3Z")
val Vector3.z get() = this[2]

fun vector3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Vector3 {
    return Vector3(x, y, z)
}
