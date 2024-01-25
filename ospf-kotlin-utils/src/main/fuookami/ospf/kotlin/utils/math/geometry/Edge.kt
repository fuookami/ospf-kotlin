package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*

data class Edge<P : Point<D>, D : Dimension>(
    val from: P,
    val to: P
) {
    init {
        assert(from.size == to.size)
    }

    val length by lazy { from distance to }
    fun length(distance: Distance = Distance.Euclidean): Flt64 {
        return distance(from, to)
    }

    val vector by lazy { Vector(from.indices.map { to[it] - from[it] }, from.dim) }

    override fun toString() = "$from -> $to"
}

typealias Edge2 = Edge<Point2, Dim2>
typealias Edge3 = Edge<Point3, Dim3>
