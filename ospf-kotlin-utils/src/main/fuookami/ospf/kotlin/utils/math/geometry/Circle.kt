package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import kotlin.math.sqrt

/**
 * Circle - 泛型圆（2D）或球（3D）
 * Circle - Generic circle (2D) or sphere (3D)
 *
 * 表示 N 维空间中的圆（2D）或球（3D+）。
 * Represents a circle (2D) or sphere (3D+) in N-dimensional space.
 *
 * @param center 圆心 / Center
 * @param direction 方向向量 / Direction vector
 * @param radius 半径 / Radius
 */
data class Circle<P : Point<D>, V : Vector<D>, D : Dimension>(
    val center: P,
    val direction: V,
    val radius: Flt64
) {
    companion object {}

    @Suppress("UNCHECKED_CAST")
    constructor(center: P, radius: V) : this(
        center = center,
        direction = radius.unit as V,
        radius = radius.norm
    )
}

typealias Circle2 = Circle<Point2, Vector2, Dim2>
typealias Circle3 = Circle<Point3, Vector3, Dim3>

/** Sphere3 类型别名 / Sphere3 type alias */
typealias Sphere3 = Circle3

// ============================================================================
// Circle2 属性 / Circle2 properties
// ============================================================================

val Circle2.x: Flt64 get() = center.x
val Circle2.y: Flt64 get() = center.y

/** 计算圆的面积 / Calculate the area of the circle */
val Circle2.area: Flt64 get() = Flt64.pi * radius * radius

/** 计算圆的周长 / Calculate the circumference of the circle */
val Circle2.circumference: Flt64 get() = Flt64.two * Flt64.pi * radius

/** 计算直径 / Calculate the diameter */
val Circle2.diameter: Flt64 get() = Flt64.two * radius

// ============================================================================
// Circle2 方法 / Circle2 methods
// ============================================================================

/**
 * 判断点是否在圆内（包含边界）
 * Check if a point is inside or on the circle
 *
 * @param point 待检查的点 / The point to check
 * @return 点是否在圆内 / Whether the point is inside
 */
@JvmName("containsPoint2D")
infix fun Circle2.containsPoint(point: Point2): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq leq radius * radius
}

/**
 * 判断点是否严格在圆内（不包含边界）
 * Check if a point is strictly inside the circle (not on boundary)
 *
 * @param point 待检查的点 / The point to check
 * @return 点是否严格在圆内 / Whether the point is strictly inside
 */
infix fun Circle2.containsPointStrict(point: Point2): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq ls radius * radius
}

/**
 * 判断两圆是否相交
 * Check if two circles intersect
 *
 * @param other 另一个圆 / The other circle
 * @return 是否相交 / Whether they intersect
 */
infix fun Circle2.intersects(other: Circle2): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist <= (radius + other.radius).toDouble()
}

/**
 * 判断两圆是否包含关系
 * Check if one circle contains another
 *
 * @param other 另一个圆 / The other circle
 * @return 是否包含 / Whether this circle contains the other
 */
infix fun Circle2.containsCircle(other: Circle2): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist + other.radius.toDouble() <= radius.toDouble()
}

/**
 * 判断点是否在圆上
 * Check if a point is on the circle boundary
 *
 * @param point 待检查的点 / The point to check
 * @param epsilon 容差 / Tolerance
 * @return 点是否在圆上 / Whether the point is on the boundary
 */
fun Circle2.pointOnBoundary(point: Point2, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()
    return (dist - radius).abs() <= epsilon
}

/**
 * 判断两圆是否相切
 * Check if two circles are tangent
 *
 * 两圆相切有两种情况：外切（距离等于半径之和）或内切（距离等于半径之差）。
 * Two circles are tangent when they touch at exactly one point:
 * externally tangent (distance equals sum of radii) or internally tangent (distance equals difference of radii).
 *
 * @param other 另一个圆 / The other circle
 * @param epsilon 容差 / Tolerance
 * @return 是否相切 / Whether the circles are tangent
 */
fun Circle2.isTangent(other: Circle2, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()

    // 同心圆不相切（要么完全重合，要么不相交）
    // Concentric circles are not tangent (either identical or non-intersecting)
    if (dist ls epsilon) return false

    // 外切：距离等于半径之和 / External tangency: distance equals sum of radii
    val externalTangent = (dist - (radius + other.radius)).abs() <= epsilon

    // 内切：距离等于半径之差 / Internal tangency: distance equals difference of radii
    val internalTangent = (dist - (radius - other.radius).abs()).abs() <= epsilon

    return externalTangent || internalTangent
}

/**
 * 计算两圆交点
 * Calculate intersection points of two circles
 *
 * 返回 0、1 或 2 个交点。
 * Returns 0, 1, or 2 intersection points.
 *
 * @param other 另一个圆 / The other circle
 * @return 交点列表 / List of intersection points
 */
infix fun Circle2.intersectionPoints(other: Circle2): List<Point2> {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val d = center distance other.center

    val r1 = radius.toDouble()
    val r2 = other.radius.toDouble()
    val dVal = d.toDouble()

    // 无交点 / No intersection
    if (dVal > r1 + r2 || dVal < kotlin.math.abs(r1 - r2)) {
        return emptyList()
    }

    // 同一个圆 / Same circle
    if (d eq Flt64.zero && radius eq other.radius) {
        return emptyList()
    }

    val a = (r1 * r1 - r2 * r2 + dVal * dVal) / (2.0 * dVal)
    val hSquared = r1 * r1 - a * a

    if (hSquared < 0) {
        return emptyList()
    }

    val h = sqrt(hSquared)

    if (h < 1e-10) {
        // 相切，一个交点 / Tangent, one intersection
        val px = center.x.toDouble() + a * dx.toDouble() / dVal
        val py = center.y.toDouble() + a * dy.toDouble() / dVal
        return listOf(point2(Flt64(px), Flt64(py)))
    }

    // 两个交点 / Two intersections
    val px = center.x.toDouble() + a * dx.toDouble() / dVal
    val py = center.y.toDouble() + a * dy.toDouble() / dVal

    val hDyD = h * dy.toDouble() / dVal
    val hDxD = h * dx.toDouble() / dVal

    val p1 = point2(Flt64(px + hDyD), Flt64(py - hDxD))
    val p2 = point2(Flt64(px - hDyD), Flt64(py + hDxD))

    return listOf(p1, p2)
}

// ============================================================================
// Circle3 (Sphere3) 属性 / Circle3 (Sphere3) properties
// ============================================================================

/** 计算球的体积 / Calculate the volume of the sphere */
val Sphere3.volume: Flt64
    get() {
        val fourThirds = Flt64(4.0) / Flt64(3.0)
        return fourThirds * Flt64.pi * radius * radius * radius
    }

/** 计算球的表面积 / Calculate the surface area of the sphere */
val Sphere3.surfaceArea: Flt64 get() = Flt64(4.0) * Flt64.pi * radius * radius

// ============================================================================
// Circle3 (Sphere3) 方法 / Circle3 (Sphere3) methods
// ============================================================================

/**
 * 判断点是否在球内（包含边界）
 * Check if a point is inside or on the sphere
 *
 * @param point 待检查的点 / The point to check
 * @return 点是否在球内 / Whether the point is inside
 */
@JvmName("containsPoint3D")
infix fun Sphere3.containsPoint(point: Point3): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dz = point.z - center.z
    val distSq = dx * dx + dy * dy + dz * dz
    return distSq leq radius * radius
}

// ============================================================================
// 外接圆计算 / Circumcircle calculation
// ============================================================================

fun Circle.Companion.circumcircleOf(triangle: Triangle2): Circle2 {
    val ax = triangle.p2.x - triangle.p1.x
    val ay = triangle.p2.y - triangle.p1.y
    val bx = triangle.p3.x - triangle.p1.x
    val by = triangle.p3.y - triangle.p1.y

    val m = triangle.p2.x.sqr() - triangle.p1.x.sqr() + triangle.p2.y.sqr() - triangle.p1.y.sqr()
    val u = triangle.p3.x.sqr() - triangle.p1.x.sqr() + triangle.p3.y.sqr() - triangle.p1.y.sqr()
    val s = Flt64.one / (Flt64.two * (ax * by - ay * bx))

    val x = ((triangle.p3.y - triangle.p1.y) * m + (triangle.p1.y - triangle.p2.y) * u) * s
    val y = ((triangle.p1.x - triangle.p3.x) * m + (triangle.p2.x - triangle.p1.x) * u) * s
    val r = ((triangle.p1.x - x).sqr() + (triangle.p1.y - y).sqr()).sqrt()

    return Circle2(Point2(x, y), Vector2(r, Flt64.zero))
}







