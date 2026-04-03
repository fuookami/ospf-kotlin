package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.operator.Minus
import fuookami.ospf.kotlin.utils.math.operator.Plus
import kotlin.math.acos

private fun normOf(vector: List<Flt64>): Flt64 {
    return (vector.sumOf(Flt64) { it.sqr() }).sqrt()
}

private fun unitOf(vector: List<Flt64>): List<Flt64> {
    val norm = normOf(vector)
    return vector.map { it / norm }
}

private fun timesBetween(lhs: List<Flt64>, rhs: List<Flt64>): Flt64 {
    assert(lhs.size == rhs.size)
    return lhs.indices.sumOf(Flt64) { lhs[it] * rhs[it] }
}

open class Vector<D : Dimension>(
    val vector: List<Flt64>,
    val dim: D
) : InnerProductSpace<Vector<D>, Flt64> {
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
    override val norm: Flt64 by lazy { normOf(vector) }
    override val unit: Vector<D> by lazy { Vector(unitOf(vector), dim) }

    init {
        assert(vector.size == dim.size)
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): Flt64 {
        return vector[i]
    }

    override operator fun plus(rhs: Vector<D>) = Vector(indices.map { this[it] + rhs[it] }, dim)
    override operator fun minus(rhs: Vector<D>) = Vector(indices.map { this[it] - rhs[it] }, dim)
    override fun scale(rhs: Flt64) = Vector(indices.map { this[it] * rhs }, dim)
    override infix fun dot(rhs: Vector<D>) = timesBetween(vector, rhs.vector)

    operator fun times(rhs: Vector<D>) = dot(rhs)
    operator fun times(rhs: Flt64) = scale(rhs)

    operator fun plus(rhs: Point<D>) = Point(indices.map { this[it] + rhs[it] }, dim)

    fun angle(rhs: Vector<D>): Flt64? {
        if (norm eq Flt64.zero || rhs.norm eq Flt64.zero) {
            return null
        }
        val cosine = ((this dot rhs) / (norm * rhs.norm)).toDouble().coerceIn(-1.0, 1.0)
        return Flt64(acos(cosine))
    }

    fun projectionOn(rhs: Vector<D>): Vector<D>? {
        val denominator = rhs dot rhs
        if (denominator eq Flt64.zero) {
            return null
        }
        return rhs * ((this dot rhs) / denominator)
    }

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

operator fun <D : Dimension> Flt64.times(rhs: Vector<D>): Vector<D> {
    return rhs * this
}

infix fun Vector2.cross(rhs: Vector2): Flt64 {
    return this.x * rhs.y - this.y * rhs.x
}

infix fun Vector3.cross(rhs: Vector3): Vector3 {
    return vector3(
        x = this.y * rhs.z - this.z * rhs.y,
        y = this.z * rhs.x - this.x * rhs.z,
        z = this.x * rhs.y - this.y * rhs.x
    )
}







