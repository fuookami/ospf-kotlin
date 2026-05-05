package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.math.sqrt

data class Circle<P : Point<D, Va>, Vec : Vector<D, Va>, D : Dimension, Va : FloatingNumber<Va>>(
    val center: P,
    val direction: Vec,
    val radius: Va
) {
    companion object {}
    constructor(center: P, radiusVec: Vec) : this(
        center = center,
        direction = radiusVec.unit as Vec,
        radius = radiusVec.norm
    )
}

typealias Circle2 = Circle<Point2, Vector2, Dim2, Flt64>
typealias Circle3 = Circle<Point3, Vector3, Dim3, Flt64>

typealias Sphere3 = Circle3

val Circle2.x: Flt64 get() = center.x
val Circle2.y: Flt64 get() = center.y

val Circle2.area: Flt64 get() = Flt64.pi * radius * radius
val Circle2.circumference: Flt64 get() = Flt64.two * Flt64.pi * radius
val Circle2.diameter: Flt64 get() = Flt64.two * radius

@JvmName("containsPoint2D")
infix fun Circle2.containsPoint(point: Point2): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq leq radius * radius
}

infix fun Circle2.containsPointStrict(point: Point2): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distSq = dx * dx + dy * dy
    return distSq ls radius * radius
}

infix fun Circle2.intersects(other: Circle2): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist <= (radius + other.radius).toDouble()
}

infix fun Circle2.containsCircle(other: Circle2): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = sqrt((dx * dx + dy * dy).toDouble())
    return dist + other.radius.toDouble() <= radius.toDouble()
}

fun Circle2.pointOnBoundary(point: Point2, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()
    return (dist - radius).abs() <= epsilon
}

fun Circle2.isTangent(other: Circle2, epsilon: Flt64 = Flt64.decimalPrecision): Boolean {
    val dx = other.center.x - center.x
    val dy = other.center.y - center.y
    val dist = (dx * dx + dy * dy).sqrt()

    if (dist ls epsilon) return false

    val externalTangent = (dist - (radius + other.radius)).abs() <= epsilon
    val internalTangent = (dist - (radius - other.radius).abs()).abs() <= epsilon

    return externalTangent || internalTangent
}

infix fun Circle2.intersectionPoints(other: Circle2): List<Point2> {
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

val Sphere3.volume: Flt64
    get() {
        val fourThirds = Flt64(4.0) / Flt64(3.0)
        return fourThirds * Flt64.pi * radius * radius * radius
    }

val Sphere3.surfaceArea: Flt64 get() = Flt64(4.0) * Flt64.pi * radius * radius

@JvmName("containsPoint3D")
infix fun Sphere3.containsPoint(point: Point3): Boolean {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dz = point.z - center.z
    val distSq = dx * dx + dy * dy + dz * dz
    return distSq leq radius * radius
}

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
