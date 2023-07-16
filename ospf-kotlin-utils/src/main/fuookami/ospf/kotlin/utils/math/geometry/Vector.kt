package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

interface Vector {
    val size: Int

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): Flt64
}

data class Vector2(
    val x: Flt64 = Flt64.zero,
    val y: Flt64 = Flt64.zero
) : Vector, Plus<Vector2, Vector2>, Minus<Vector2, Vector2> {
    companion object {
        operator fun invoke(edge: Edge2): Vector2 = edge.vector
    }

    override val size = 2

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun get(i: Int): Flt64 {
        return when (i) {
            0 -> x
            1 -> y
            else -> throw ArrayIndexOutOfBoundsException(i)
        }
    }

    override fun plus(rhs: Vector2) = Vector2(x + rhs.x, y + rhs.y)
    override fun minus(rhs: Vector2) = Vector2(x - rhs.x, y - rhs.y)

    operator fun plus(rhs: Point2) = Point2(x + rhs.x, y + rhs.y)
}

data class Vector3(
    val x: Flt64 = Flt64.zero,
    val y: Flt64 = Flt64.zero,
    val z: Flt64 = Flt64.zero
) : Vector, Plus<Vector3, Vector3>, Minus<Vector3, Vector3> {
    companion object {
        operator fun invoke(edge: Edge3): Vector3 = edge.vector
    }

    override val size = 3

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun get(i: Int): Flt64 {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw ArrayIndexOutOfBoundsException(i)
        }
    }

    override fun plus(rhs: Vector3) = Vector3(x + rhs.x, y + rhs.y, z + rhs.z)
    override fun minus(rhs: Vector3) = Vector3(x - rhs.x, y - rhs.y, z - rhs.z)

    operator fun plus(rhs: Point3) = Point3(x + rhs.x, y + rhs.y, z + rhs.z)
}
