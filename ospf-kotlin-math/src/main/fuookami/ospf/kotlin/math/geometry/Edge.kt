/**
 * 边
 * Edge
 *
 * 定义几何空间中的边数据结构，表示连接两个点的线段。
 * 边是几何图形的基本元素，支持长度计算、中点查找、相交检测等操作。
 * Defines edge data structure in geometric space, representing a line segment connecting two points.
 * An edge is a fundamental element of geometric shapes, supporting length calculation, midpoint finding, intersection detection, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 边数据类
 * Edge Data Class
 *
 * 表示连接两个点的线段，支持任意维度和浮点数类型。
 * 提供长度、方向向量、中点等计算功能。
 * Represents a line segment connecting two points, supporting arbitrary dimensions and floating-point types.
 * Provides functionality for length, direction vector, midpoint calculations, etc.
 *
 * @param P 点类型 / Point type
 * @param D 维度类型 / Dimension type
 * @param V 数值类型 / Number type
 * @property from 起点 / Starting point
 * @property to 终点 / Ending point
 */
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

    @Suppress("UNCHECKED_CAST")
    private fun castPoint(position: List<V>): P {
        // 安全不变量：P 约束为 Point<D, V>，此处只改变具体子类型视图，不改变坐标维度与数值类型。
        // Safety invariant: P is constrained as Point<D, V>; this only reuses concrete subtype view with same D/V.
        return Point(position, from.dim) as P
    }

    fun midpoint(): P {
        val v = from[0]
        val two = v.constants.two
        return castPoint(from.indices.map { (from[it] + to[it]) / two })
    }

    fun pointAt(t: V): P {
        return castPoint(from.indices.map { from[it] + t * (to[it] - from[it]) })
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

/** 判断两条二维边是否相交 / Check whether two 2D edges intersect */
infix fun Edge<Point<Dim2, Flt64>, Dim2, Flt64>.intersects(other: Edge<Point<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    return intersectionPoint(other) != null
}

/** 计算两条二维边的交点，无交点返回 null / Compute intersection point of two 2D edges, returns null if none */
infix fun Edge<Point<Dim2, Flt64>, Dim2, Flt64>.intersectionPoint(other: Edge<Point<Dim2, Flt64>, Dim2, Flt64>): Point<Dim2, Flt64>? {
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

/** 计算边上离给定点最近的点 / Compute the closest point on the edge to the given point */
infix fun Edge<Point<Dim2, Flt64>, Dim2, Flt64>.closestPoint(point: Point<Dim2, Flt64>): Point<Dim2, Flt64> {
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

/** 计算边到给定点的距离 / Compute the distance from the edge to the given point */
infix fun Edge<Point<Dim2, Flt64>, Dim2, Flt64>.distanceToPoint(point: Point<Dim2, Flt64>): Flt64 {
    val closest = closestPoint(point)
    return point distance closest
}
