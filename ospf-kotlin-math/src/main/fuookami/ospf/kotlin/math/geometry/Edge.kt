package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.functional.sumOf

data class Edge<P : Point<D, V>, D : Dimension, V : FloatingNumber<V>>(
    val from: P,
    val to: P
) {
    init {
        assert(from.size == to.size)
    }

    val length by lazy { from distance to }

    fun length(distance: Distance = Distance.Euclidean): V {
        return distance(from, to)
    }

    val lengthSquared: V by lazy {
        val v = from[0]
        from.indices.sumOf(v.constants) { i ->
            (to[i] - from[i]).sqr()
        }
    }

    val vector by lazy { Vector(from.indices.map { to[it] - from[it] }, from.dim) }

    val direction by lazy { vector }

    val unitDirection: Vector<D, V>? by lazy {
        if (lengthSquared eq from[0].constants.zero) {
            null
        } else {
            vector.unit
        }
    }

    fun midpoint(): P {
        val v = from[0]
        val two = v.constants.two
        return from.indices.map { (from[it] + to[it]) / two }.let {
            Point(it, from.dim) as P
        }
    }

    fun pointAt(t: V): P {
        return from.indices.map { from[it] + t * (to[it] - from[it]) }.let {
            Point(it, from.dim) as P
        }
    }

    fun containsPoint(point: P, epsilon: V = from[0].constants.decimalPrecision): Boolean {
        val distToFrom = point distance from
        val distToTo = point distance to
        return (distToFrom + distToTo - length).abs() <= epsilon
    }

    infix fun approxEq(other: Edge<P, D, V>): Boolean {
        return from.approxEq(other.from) && to.approxEq(other.to)
    }

    fun approxEq(other: Edge<P, D, V>, epsilon: V): Boolean {
        return from.approxEq(other.from, epsilon) && to.approxEq(other.to, epsilon)
    }

    infix fun approxEqUndirected(other: Edge<P, D, V>): Boolean {
        return (from.approxEq(other.from) && to.approxEq(other.to))
            || (from.approxEq(other.to) && to.approxEq(other.from))
    }

    fun approxEqUndirected(other: Edge<P, D, V>, epsilon: V): Boolean {
        return (from.approxEq(other.from, epsilon) && to.approxEq(other.to, epsilon))
            || (from.approxEq(other.to, epsilon) && to.approxEq(other.from, epsilon))
    }

    override fun toString() = "$from -> $to"
}

typealias Edge2 = Edge<Point2, Dim2, Flt64>
typealias Edge3 = Edge<Point3, Dim3, Flt64>

infix fun Edge2.intersects(other: Edge2): Boolean {
    return intersectionPoint(other) != null
}

infix fun Edge2.intersectionPoint(other: Edge2): Point2? {
    val p1 = from
    val p2 = to
    val p3 = other.from
    val p4 = other.to

    val d1x = p2.x - p1.x
    val d1y = p2.y - p1.y
    val d2x = p4.x - p3.x
    val d2y = p4.y - p3.y

    val denom = d1x * d2y - d1y * d2x

    if (denom eq Flt64.zero) {
        return null
    }

    val dx = p3.x - p1.x
    val dy = p3.y - p1.y

    val t = (dx * d2y - dy * d2x) / denom
    val s = (dx * d1y - dy * d1x) / denom

    if (t geq Flt64.zero && t leq Flt64.one && s geq Flt64.zero && s leq Flt64.one) {
        return pointAt(t)
    }

    return null
}

infix fun Edge2.closestPoint(point: Point2): Point2 {
    val direction = this.direction
    val dx = point.x - from.x
    val dy = point.y - from.y

    val lengthSq = this.lengthSquared

    if (lengthSq eq Flt64.zero) {
        return from
    }

    val t = (dx * direction.x + dy * direction.y) / lengthSq

    val tClamped = when {
        t ls Flt64.zero -> Flt64.zero
        t gr Flt64.one -> Flt64.one
        else -> t
    }

    return pointAt(tClamped)
}

infix fun Edge2.distanceToPoint(point: Point2): Flt64 {
    val closest = closestPoint(point)
    return point distance closest
}
