package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.math.sqrt
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.geometry.vector2

@Suppress("UNCHECKED_CAST")
private fun <D : Dimension, Va : FloatingNumber<Va>, Vec : Vector<D, Va>> unitAsSameVectorType(radiusVec: Vec): Vec {
    // 安全不变量：unit 保持 Vector<D, Va> 数域与维度不变，仅回收为调用方 Vec 视图。
    // Safety invariant: unit keeps Vector<D, Va> domain/dimension unchanged; cast only restores caller Vec view.
    return radiusVec.unit as Vec
}

data class Circle<P : Point<D, Va>, Vec : Vector<D, Va>, D : Dimension, Va : FloatingNumber<Va>>(
    val center: P,
    val direction: Vec,
    val radius: Va
) {
    companion object {}
    constructor(center: P, radiusVec: Vec) : this(
        center = center,
        direction = unitAsSameVectorType(radiusVec),
        radius = radiusVec.norm
    )
}


val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.x: Flt64 get() = center.x
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.y: Flt64 get() = center.y

val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.area: Flt64 get() = Flt64.pi * radius * radius
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.circumference: Flt64 get() = Flt64.two * Flt64.pi * radius
val Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.diameter: Flt64 get() = Flt64.two * radius

@JvmName("containsPoint2D")
infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.containsPoint(point: Point<Dim2, Flt64>): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq leq radius * radius
}

infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.containsPointStrict(point: Point<Dim2, Flt64>): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq ls radius * radius
}

infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.intersects(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist <= (radius + other.radius).toDouble()
}

infix fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.containsCircle(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist + other.radius.toDouble() <= radius.toDouble()
}

fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.pointOnBoundary(point: Point<Dim2, Flt64>, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()
    return (dist - radius).abs() <= epsilon
}

fun Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>.isTangent(other: Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()

    if (dist ls epsilon) return false

    val externalTangent = (dist - (radius + other.radius)).abs() <= epsilon
    val internalTangent = (dist - (radius - other.radius).abs()).abs() <= epsilon

    return externalTangent || internalTangent
}

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

val Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>.volume: Flt64
    get() {
        val fourThirds = Flt64(4.0) / Flt64(3.0)
        return fourThirds * Flt64.pi * radius * radius * radius
    }

val Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>.surfaceArea: Flt64 get() = Flt64(4.0) * Flt64.pi * radius * radius

@JvmName("containsPoint3D")
infix fun Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>.containsPoint(point: Point<Dim3, Flt64>): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dz = point.z - center.z
    val distSq = dx * dx + dy * dy + dz * dz
    return distSq leq radius * radius
}

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
