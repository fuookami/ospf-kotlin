package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

/**
 * Edge - 泛型边（线段）
 * Edge - Generic edge (line segment)
 *
 * 表示连接两个点的边（线段）。
 * Represents an edge (line segment) connecting two points.
 *
 * @param from 起点 / Start point
 * @param to 终点 / End point
 */
data class Edge<P : Point<D>, D : Dimension>(
    val from: P,
    val to: P
) {
    init {
        assert(from.size == to.size)
    }

    // ============================================================================
    // 几何属性 / Geometric properties
    // ============================================================================

    /** 计算边的长度 / Calculate the length of the edge */
    val length by lazy { from distance to }

    /** 使用指定距离度量计算边的长度 / Calculate the length using a specified distance metric */
    fun length(distance: Distance = Distance.Euclidean): Flt64 {
        return distance(from, to)
    }

    /** 计算长度平方（避免开方）/ Calculate squared length (avoiding square root) */
    val lengthSquared: Flt64 by lazy {
        from.indices.sumOf(Flt64) { i ->
            (to[i] - from[i]).sqr()
        }
    }

    /** 获取方向向量（从起点指向终点）/ Get the direction vector (from start to end) */
    val vector by lazy { Vector(from.indices.map { to[it] - from[it] }, from.dim) }

    /** 获取方向向量（别名）/ Get the direction vector (alias) */
    val direction by lazy { vector }

    /** 获取单位方向向量，若边长度为零则返回 null / Get the unit direction vector, returns null if length is zero */
    val unitDirection: Vector<D>? by lazy {
        if (lengthSquared eq Flt64.zero) {
            null
        } else {
            vector.unit
        }
    }

    // ============================================================================
    // 参数化方法 / Parametric methods
    // ============================================================================

    /**
     * 计算边的中点 / Calculate the midpoint of the edge
     *
     * @return 边的中点 / The midpoint of the edge
     */
    fun midpoint(): P {
        @Suppress("UNCHECKED_CAST")
        return from.indices.map { (from[it] + to[it]) / Flt64(2.0) }.let {
            Point(it, from.dim) as P
        }
    }

    /**
     * 计算边上的点（参数化）/ Calculate a point on the edge (parametric)
     *
     * @param t 参数，0.0 返回起点，1.0 返回终点 / Parameter, 0.0 returns start, 1.0 returns end
     * @return 边上的点 / Point on the edge
     */
    fun pointAt(t: Flt64): P {
        @Suppress("UNCHECKED_CAST")
        return from.indices.map { from[it] + t * (to[it] - from[it]) }.let {
            Point(it, from.dim) as P
        }
    }

    /**
     * 判断点是否在边上 / Check if a point lies on the edge
     *
     * 使用距离之和判断：点到起点和终点的距离之和是否等于边长。
     * Uses sum of distances: check if sum of distances from point to start and end equals edge length.
     *
     * @param point 待检查的点 / The point to check
     * @param epsilon 容差 / Tolerance
     * @return 点是否在边上 / Whether the point lies on the edge
     */
    fun containsPoint(point: P, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
        val distToFrom = point distance from
        val distToTo = point distance to
        return (distToFrom + distToTo - length).abs() <= epsilon
    }

    // ============================================================================
    // 容差比较 / Tolerance comparison
    // ============================================================================

    /**
     * 使用容差判断两条边是否近似相等 / Check if two edges are approximately equal using tolerance
     *
     * @param other 另一条边 / The other edge
     * @param epsilon 容差 / Tolerance
     * @return 是否近似相等 / Whether approximately equal
     */
    infix fun approxEq(other: Edge<P, D>): Boolean {
        return from.approxEq(other.from) && to.approxEq(other.to)
    }

    fun approxEq(other: Edge<P, D>, epsilon: Flt64): Boolean {
        return from.approxEq(other.from, epsilon) && to.approxEq(other.to, epsilon)
    }

    /**
     * 判断两条边是否近似相等（忽略方向）/ Check if two edges are approximately equal (ignoring direction)
     *
     * @param other 另一条边 / The other edge
     * @param epsilon 容差 / Tolerance
     * @return 是否近似相等（忽略方向）/ Whether approximately equal (ignoring direction)
     */
    infix fun approxEqUndirected(other: Edge<P, D>): Boolean {
        return (from.approxEq(other.from) && to.approxEq(other.to))
            || (from.approxEq(other.to) && to.approxEq(other.from))
    }

    fun approxEqUndirected(other: Edge<P, D>, epsilon: Flt64): Boolean {
        return (from.approxEq(other.from, epsilon) && to.approxEq(other.to, epsilon))
            || (from.approxEq(other.to, epsilon) && to.approxEq(other.from, epsilon))
    }

    override fun toString() = "$from -> $to"
}

typealias Edge2 = Edge<Point2, Dim2>
typealias Edge3 = Edge<Point3, Dim3>

// ============================================================================
// 2D 边特有方法 / 2D edge specific methods
// ============================================================================

/**
 * 判断两条 2D 边是否相交 / Check if two 2D edges intersect
 *
 * @param other 另一条边 / The other edge
 * @return 是否相交 / Whether they intersect
 */
infix fun Edge2.intersects(other: Edge2): Boolean {
    return intersectionPoint(other) != null
}

/**
 * 计算两条 2D 边的交点 / Calculate the intersection point of two 2D edges
 *
 * 返回 null 如果边不相交或重合。
 * Returns null if edges don't intersect or are collinear.
 *
 * @param other 另一条边 / The other edge
 * @return 交点，若不相交则返回 null / Intersection point, or null if no intersection
 */
infix fun Edge2.intersectionPoint(other: Edge2): Point2? {
    val p1 = from
    val p2 = to
    val p3 = other.from
    val p4 = other.to

    val d1x = p2.x - p1.x
    val d1y = p2.y - p1.y
    val d2x = p4.x - p3.x
    val d2y = p4.y - p3.y

    // 计算行列式 / Calculate determinant
    val denom = d1x * d2y - d1y * d2x

    if (denom eq Flt64.zero) {
        // 平行或重合 / Parallel or collinear
        return null
    }

    val dx = p3.x - p1.x
    val dy = p3.y - p1.y

    val t = (dx * d2y - dy * d2x) / denom
    val s = (dx * d1y - dy * d1x) / denom

    // 检查 t 和 s 是否在 [0, 1] 范围内 / Check if t and s are in [0, 1] range
    if (t geq Flt64.zero && t leq Flt64.one && s geq Flt64.zero && s leq Flt64.one) {
        return pointAt(t)
    }

    return null
}

/**
 * 计算边到点的最近点 / Calculate the closest point on the edge to a given point
 *
 * @param point 给定的点 / The given point
 * @return 边上的最近点 / The closest point on the edge
 */
infix fun Edge2.closestPoint(point: Point2): Point2 {
    val direction = this.direction
    val dx = point.x - from.x
    val dy = point.y - from.y

    val lengthSq = this.lengthSquared

    if (lengthSq eq Flt64.zero) {
        return from
    }

    val t = (dx * direction.x + dy * direction.y) / lengthSq

    // 限制 t 在 [0, 1] 范围内 / Clamp t to [0, 1] range
    val tClamped = when {
        t ls Flt64.zero -> Flt64.zero
        t gr Flt64.one -> Flt64.one
        else -> t
    }

    return pointAt(tClamped)
}

/**
 * 计算点到边的距离 / Calculate the distance from a point to the edge
 *
 * @param point 给定的点 / The given point
 * @return 点到边的距离 / Distance from point to edge
 */
infix fun Edge2.distanceToPoint(point: Point2): Flt64 {
    val closest = closestPoint(point)
    return point distance closest
}







