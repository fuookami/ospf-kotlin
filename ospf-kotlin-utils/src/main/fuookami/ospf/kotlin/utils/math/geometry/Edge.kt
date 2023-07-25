package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*

data class Edge<P : Point>(
    val from: P,
    val to: P
) {
    init {
        assert(from.size == to.size)
    }

    val length = (0 until from.size).sumOf(Flt64) { (to[it] - from[it]).sqrt() }
}

typealias Edge2 = Edge<Point2>
typealias Edge3 = Edge<Point3>

val Edge2.vector get() = Vector2(to.x - from.x, to.y - from.y)
val Edge3.vector get() = Vector3(to.x - from.x, to.y - from.y, to.z - from.z)
