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

import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

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

    /**
     * 边的长度（欧几里得距离）
     * The length of the edge (Euclidean distance)
    */
    val length by lazy { from distance to }

    /**
     * 使用指定距离度量计算长度
     * Compute length using the specified distance metric
     *
     * @param distance 距离度量策略，默认为欧几里得距离 / The distance metric strategy, defaults to Euclidean
     * @return 边的长度 / The length of the edge
    */
    fun length(distance: Distance = Distance.Euclidean): V {
        return distance(from, to)
    }

    /**
     * The squared length of the edge.
     * 边的长度平方。
    */
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

    /**
     * Cast a coordinate list to the point type.
     * 将坐标列表转换为点类型。
     *
     * @param position the coordinate list / 坐标列表
     * @return the casted point / 转换后的点
    */
    @Suppress("UNCHECKED_CAST")
    private fun castPoint(position: List<V>): P {
        // 安全不变量：P 约束为 Point<D, V>，此处只改变具体子类型视图，不改变坐标维度与数值类型。
        // Safety invariant: P is constrained as Point<D, V>; this only reuses concrete subtype view with same D/V.
        return Point(position, from.dim) as P
    }

    /**
     * 计算边的中点
     * Compute the midpoint of the edge
     *
     * @return 中点 / The midpoint
    */
    fun midpoint(): P {
        val v = from[0]
        val two = v.constants.two
        return castPoint(from.indices.map { (from[it] + to[it]) / two })
    }

    /**
     * 计算边上参数 t 处的点
     * Compute the point at parameter t on the edge
     *
     * @param t 参数值（0 为起点，1 为终点） / The parameter value (0 for start, 1 for end)
     * @return 边上对应位置的点 / The point at the corresponding position on the edge
    */
    fun pointAt(t: V): P {
        return castPoint(from.indices.map { from[it] + t * (to[it] - from[it]) })
    }

    /**
     * 判断点是否在边上（含容差）
     * Check whether a point is on the edge (with tolerance)
     *
     * @param point 待检测的点 / The point to check
     * @param epsilon 容差值，默认为 decimalPrecision / The tolerance value, defaults to decimalPrecision
     * @return 点是否在边上 / Whether the point is on the edge
    */
    fun containsPoint(point: P, epsilon: V = from[0].constants.decimalPrecision): Boolean {
        val distToFrom = point distance from
        val distToTo = point distance to
        return (distToFrom + distToTo - length).abs() <= epsilon
    }

    /**
     * 使用默认精度判断两条边是否近似相等（有向）
     * Check approximate equality with default precision (directed)
     *
     * @param other 另一条边 / The other edge
     * @return 是否近似相等 / Whether approximately equal
    */
    infix fun approxEq(other: Edge<P, D, V>): Boolean {
        return from.approxEq(other.from) && to.approxEq(other.to)
    }

    /**
     * 使用指定精度判断两条边是否近似相等（有向）
     * Check approximate equality with specified precision (directed)
     *
     * @param other 另一条边 / The other edge
     * @param epsilon 容差值 / The tolerance value
     * @return 是否近似相等 / Whether approximately equal
    */
    fun approxEq(other: Edge<P, D, V>, epsilon: V): Boolean {
        return from.approxEq(other.from, epsilon) && to.approxEq(other.to, epsilon)
    }

    /**
     * 使用默认精度判断两条边是否近似相等（无向）
     * Check approximate equality with default precision (undirected)
     *
     * @param other 另一条边 / The other edge
     * @return 是否近似相等 / Whether approximately equal
    */
    infix fun approxEqUndirected(other: Edge<P, D, V>): Boolean {
        return (from.approxEq(other.from) && to.approxEq(other.to))
            || (from.approxEq(other.to) && to.approxEq(other.from))
    }

    /**
     * 使用指定精度判断两条边是否近似相等（无向）
     * Check approximate equality with specified precision (undirected)
     *
     * @param other 另一条边 / The other edge
     * @param epsilon 容差值 / The tolerance value
     * @return 是否近似相等 / Whether approximately equal
    */
    fun approxEqUndirected(other: Edge<P, D, V>, epsilon: V): Boolean {
        return (from.approxEq(other.from, epsilon) && to.approxEq(other.to, epsilon))
            || (from.approxEq(other.to, epsilon) && to.approxEq(other.from, epsilon))
    }

    override fun toString() = "$from -> $to"
}

/**
 * 判断两条二维边是否相交
 * Check whether two 2D edges intersect
 *
 * @param other 另一条边 / The other edge
 * @return 是否相交 / Whether they intersect
*/
infix fun Edge<Point<Dim2, Flt64>, Dim2, Flt64>.intersects(other: Edge<Point<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    return intersectionPoint(other) != null
}

/**
 * 计算两条二维边的交点，无交点返回 null
 * Compute intersection point of two 2D edges, returns null if none
 *
 * @param other 另一条边 / The other edge
 * @return 交点，无交点返回 null / The intersection point, or null if none
*/
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

/**
 * 计算边上离给定点最近的点
 * Compute the closest point on the edge to the given point
 *
 * @param point 给定的点 / The given point
 * @return 边上最近的点 / The closest point on the edge
*/
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

/**
 * 计算边到给定点的距离
 * Compute the distance from the edge to the given point
 *
 * @param point 给定的点 / The given point
 * @return 边到该点的距离 / The distance from the edge to the point
*/
infix fun Edge<Point<Dim2, Flt64>, Dim2, Flt64>.distanceToPoint(point: Point<Dim2, Flt64>): Flt64 {
    val closest = closestPoint(point)
    return point distance closest
}
