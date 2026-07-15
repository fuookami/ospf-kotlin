/**
 * 圆形与球体
 * Circle and Sphere
 *
 * 定义几何空间中的圆形/球体数据结构，由圆心、方向和半径定义。
 * 支持面积、周长、包含检测、相交检测、外接圆计算等操作。
 * Defines circle/sphere data structure in geometric space, defined by center, direction, and radius.
 * Supports area, circumference, containment detection, intersection detection, circumcircle computation, etc.
*/
package fuookami.ospf.kotlin.math.geometry

import kotlin.math.sqrt
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 将向量转换为单位向量并保持类型
 * Convert a vector to a unit vector while preserving its type
 *
 * @param D 维度类型 / The dimension type
 * @param Va 数值类型 / The numeric type
 * @param Vec 向量类型 / The vector type
 * @param radiusVec 半径向量 / The radius vector
 * @return 单位向量（保持原类型） / The unit vector (preserving the original type)
*/
@Suppress("UNCHECKED_CAST")
private fun <D : Dimension, Va : FloatingNumber<Va>, Vec : Vector<D, Va>> unitAsSameVectorType(radiusVec: Vec): Vec {
    // 安全不变量：unit 保持 Vector<D, Va> 数域与维度不变，仅回收为调用方 Vec 视图。
    // Safety invariant: unit keeps Vector<D, Va> domain/dimension unchanged; cast only restores caller Vec view.
    return radiusVec.unit as Vec
}

/**
 * 通用圆形/球体，由圆心、方向和半径定义。
 * General circle/sphere defined by center, direction, and radius.
 *
 * @param P 点类型 / The point type
 * @param Vec 向量类型 / The vector type
 * @param D 维度类型 / The dimension type
 * @param Va 数值类型 / The numeric type
 * @property center 圆心 / The center point
 * @property direction 方向向量（单位向量） / The direction vector (unit vector)
 * @property radius 半径 / The radius
*/
data class Circle<P : Point<D, Va>, Vec : Vector<D, Va>, D : Dimension, Va : FloatingNumber<Va>>(
    val center: P,
    val direction: Vec,
    val radius: Va
) {
    companion object {}

    /**
     * 通过圆心和半径向量构造
     * Construct from center and radius vector
     *
     * @param center 圆心 / The center point
     * @param radiusVec 半径向量（方向和长度） / The radius vector (direction and length)
    */
    constructor(center: P, radiusVec: Vec) : this(
        center = center,
        direction = unitAsSameVectorType(radiusVec),
        radius = radiusVec.norm
    )
}

/** 圆心 X 坐标 / Center X coordinate */
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.x: Flt64 get() = center.x

/** 圆心 Y 坐标 / Center Y coordinate */
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.y: Flt64 get() = center.y

/** 面积 / Area */
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.area: Flt64 get() = Flt64.pi * radius * radius

/** 周长 / Circumference */
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.circumference: Flt64 get() = Flt64.two * Flt64.pi * radius

/** 直径 / Diameter */
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.diameter: Flt64 get() = Flt64.two * radius

/**
 * Check whether a 2D point is inside the circle (inclusive).
 * 判断二维点是否在圆内（含边界）。
 *
 * @param point the point to check / 待检测的点
 * @return whether the point is inside the circle / 点是否在圆内
*/
@JvmName("containsPoint2D")
infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.containsPoint(point: Point<Dim2, Flt64>): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq leq radius * radius
}

/**
 * 判断二维点是否严格在圆内（不含边界）
 * Check whether a 2D point is strictly inside the circle (exclusive)
 *
 * @param point 待检测的点 / The point to check
 * @return 点是否严格在圆内 / Whether the point is strictly inside the circle
*/
infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.containsPointStrict(point: Point<Dim2, Flt64>): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq ls radius * radius
}

/**
 * 判断两个二维圆是否相交
 * Check whether two 2D circles intersect
 *
 * @param other 另一个圆 / The other circle
 * @return 是否相交 / Whether they intersect
*/
infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.intersects(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist <= (radius + other.radius).toDouble()
}

/**
 * 判断另一个圆是否完全在本圆内
 * Check whether another circle is entirely inside this circle
 *
 * @param other 另一个圆 / The other circle
 * @return 另一个圆是否完全在本圆内 / Whether the other circle is entirely inside this circle
*/
infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.containsCircle(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist + other.radius.toDouble() <= radius.toDouble()
}

/**
 * 判断点是否在圆的边界上
 * Check whether a point is on the circle's boundary
 *
 * @param point 待检测的点 / The point to check
 * @param epsilon 容差值，默认为 decimalPrecision / The tolerance value, defaults to decimalPrecision
 * @return 点是否在边界上 / Whether the point is on the boundary
*/
fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.pointOnBoundary(point: Point<Dim2, Flt64>, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()
    return (dist - radius).abs() <= epsilon
}

/**
 * 判断两个圆是否相切
 * Check whether two circles are tangent
 *
 * @param other 另一个圆 / The other circle
 * @param epsilon 容差值，默认为 decimalPrecision / The tolerance value, defaults to decimalPrecision
 * @return 是否相切 / Whether they are tangent
*/
fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.isTangent(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()

    if (dist ls epsilon) return false

    val externalTangent = (dist - (radius + other.radius)).abs() <= epsilon
    val internalTangent = (dist - (radius - other.radius).abs()).abs() <= epsilon

    return externalTangent || internalTangent
}

/**
 * 计算两个圆的交点
 * Compute intersection points of two circles
 *
 * @param other 另一个圆 / The other circle
 * @return 交点列表（0、1 或 2 个） / List of intersection points (0, 1, or 2)
*/
infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.intersectionPoints(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>): List<Point<Dim2, Flt64>> {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val d = center distance other.center

    val r1 = radius.toDouble()
    val r2 = other.radius.toDouble()
    val dVal = d.toDouble()

    if (dVal > r1 + r2 || dVal < kotlin.math.abs(r1 - r2)) {
        return emptyList()
    }

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
        val px = center.x.toDouble() + a * dx.toDouble() / dVal
        val py = center.y.toDouble() + a * dy.toDouble() / dVal
        return listOf(point2(Flt64(px), Flt64(py)))
    }

    val px = center.x.toDouble() + a * dx.toDouble() / dVal
    val py = center.y.toDouble() + a * dy.toDouble() / dVal

    val hDyD = h * dy.toDouble() / dVal
    val hDxD = h * dx.toDouble() / dVal

    val p1 = point2(Flt64(px + hDyD), Flt64(py - hDxD))
    val p2 = point2(Flt64(px - hDyD), Flt64(py + hDxD))

    return listOf(p1, p2)
}

/** 球体积 / Sphere volume */
val Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>.volume: Flt64
    get() {
        val fourThirds = Flt64(4.0) / Flt64(3.0)
        return fourThirds * Flt64.pi * radius * radius * radius
    }

/** 球表面积 / Sphere surface area */
val Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>.surfaceArea: Flt64 get() = Flt64(4.0) * Flt64.pi * radius * radius

/**
 * Check whether a 3D point is inside the sphere (inclusive).
 * 判断三维点是否在球内（含边界）。
 *
 * @param point the point to check / 待检测的点
 * @return whether the point is inside the sphere / 点是否在球内
*/
@JvmName("containsPoint3D")
infix fun Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>.containsPoint(point: Point<Dim3, Flt64>): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dz = point.z - center.z
    val distSq = dx * dx + dy * dy + dz * dz
    return distSq leq radius * radius
}

/**
 * 计算三角形的外接圆
 * Compute the circumcircle of a triangle
 *
 * @param triangle 三角形 / The triangle
 * @return 外接圆 / The circumcircle
*/
fun Circle.Companion.circumcircleOf(triangle: Triangle<Point<Dim2, Flt64>, Dim2, Flt64>): Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64> {
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

    return Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(x, y), vector2(r, Flt64.zero))
}
