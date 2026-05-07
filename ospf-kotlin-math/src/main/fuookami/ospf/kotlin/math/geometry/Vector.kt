package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.concept.InnerProductSpace
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus
private fun <V : FloatingNumber<V>> normOf(vector: List<V>): V {
    val v = vector[0]
    return (vector.indices.sumOf(v.constants) { i -> vector[i].sqr() }).sqrt() as V
}

private fun <V : FloatingNumber<V>> unitOf(vector: List<V>): List<V> {
    val norm = normOf(vector)
    return vector.map { it / norm }
}

private fun <V : FloatingNumber<V>> timesBetween(lhs: List<V>, rhs: List<V>): V {
    assert(lhs.size == rhs.size)
    val v = lhs[0]
    return lhs.indices.sumOf(v.constants) { lhs[it] * rhs[it] }
}

open class Vector<D : Dimension, V : FloatingNumber<V>>(
    val vector: List<V>,
    val dim: D
) : InnerProductSpace<Vector<D, V>, V> {
    companion object {
        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(x: V, y: V): Vector<D, V> {
            return Vector(listOf(x, y), Dim2 as D)
        }

        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(x: V, y: V, z: V): Vector<D, V> {
            return Vector(listOf(x, y, z), Dim3 as D)
        }

        operator fun invoke(x: Flt64, y: Flt64): Vector<Dim2, Flt64> {
            return Vector(listOf(x, y), Dim2)
        }

        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Vector<Dim3, Flt64> {
            return Vector(listOf(x, y, z), Dim3)
        }
    }

    val size by vector::size
    val indices by vector::indices
    override val norm: V by lazy { normOf(vector) }
    override val unit: Vector<D, V> by lazy { Vector(unitOf(vector), dim) }

    init {
        assert(vector.size == dim.size)
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): V {
        return vector[i]
    }

    override operator fun plus(rhs: Vector<D, V>) = Vector(indices.map { this[it] + rhs[it] }, dim)

    override operator fun minus(rhs: Vector<D, V>) = Vector(indices.map { this[it] - rhs[it] }, dim)

    override fun scale(rhs: V) = Vector(indices.map { this[it] * rhs }, dim)

    override infix fun dot(rhs: Vector<D, V>) = timesBetween(vector, rhs.vector)

    operator fun times(rhs: Vector<D, V>) = dot(rhs)

    operator fun times(rhs: V) = scale(rhs)

    operator fun plus(rhs: Point<D, V>) = Point(indices.map { this[it] + rhs[it] }, dim)

    override fun angle(rhs: Vector<D, V>): FloatingNumber<*>? {
        return super<InnerProductSpace>.angle(rhs) as? V
    }

    fun projectionOn(rhs: Vector<D, V>): Vector<D, V>? {
        return project(rhs)
    }

    fun orthogonalComponentTo(rhs: Vector<D, V>): Vector<D, V>? {
        return orthogonalComponent(rhs)
    }

    override fun toString() = vector.joinToString(",", "[", "]")
}


@get:JvmName("Vector2X")
val Vector<Dim2, Flt64>.x get() = this[0]

@get:JvmName("Vector2Y")
val Vector<Dim2, Flt64>.y get() = this[1]

fun vector2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Vector<Dim2, Flt64> {
    return Vector<Dim2, Flt64>(x, y)
}

@get:JvmName("Vector3X")
val Vector<Dim3, Flt64>.x get() = this[0]

@get:JvmName("Vector3Y")
val Vector<Dim3, Flt64>.y get() = this[1]

@get:JvmName("Vector3Z")
val Vector<Dim3, Flt64>.z get() = this[2]

fun vector3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Vector<Dim3, Flt64> {
    return Vector<Dim3, Flt64>(x, y, z)
}

operator fun <D : Dimension, V : FloatingNumber<V>> V.times(rhs: Vector<D, V>): Vector<D, V> {
    return rhs * this
}

infix fun Vector<Dim2, Flt64>.cross(rhs: Vector<Dim2, Flt64>): Flt64 {
    return this.x * rhs.y - this.y * rhs.x
}

infix fun Vector<Dim3, Flt64>.cross(rhs: Vector<Dim3, Flt64>): Vector<Dim3, Flt64> {
    return vector3(
        x = this.y * rhs.z - this.z * rhs.y,
        y = this.z * rhs.x - this.x * rhs.z,
        z = this.x * rhs.y - this.y * rhs.x
    )
}
