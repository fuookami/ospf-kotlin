package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.functional.sumOf

class Triangle<P : Point<D, V>, D : Dimension, V : FloatingNumber<V>>(
    val p1: P,
    val p2: P,
    val p3: P
) {
    init {
        assert(p1.size == p2.size)
        assert(p2.size == p3.size)
    }

    val e1: Edge<P, D, V> get() = Edge(p1, p2)
    val e2: Edge<P, D, V> get() = Edge(p2, p3)
    val e3: Edge<P, D, V> get() = Edge(p3, p1)

    val edges: List<Edge<P, D, V>> get() = listOf(e1, e2, e3)

    val vertices: List<P> get() = listOf(p1, p2, p3)

    val perimeter: V by lazy { e1.length + e2.length + e3.length }

    val centroid: P by lazy {
        val v = p1[0]
        val three = v.constants.three
        p1.indices.map { (p1[it] + p2[it] + p3[it]) / three }.let {
            Point(it, p1.dim) as P
        }
    }

    val isDegenerate: Boolean by lazy {
        p1.approxEq(p2) || p2.approxEq(p3) || p3.approxEq(p1)
    }

    val illegal: Boolean by lazy {
        for (i in p1.indices) {
            if (p1[i] eq p2[i] && p2[i] eq p3[i]) {
                return@lazy true
            }
        }
        false
    }

    val area: V by lazy {
        val a = e1.length
        val b = e2.length
        val c = e3.length
        val v = p1[0]
        val two = v.constants.two
        val p = (a + b + c) / two
        val s = p * (p - a) * (p - b) * (p - c)
        s.sqrt() as V
    }

    override fun toString() = "Triangle($p1, $p2, $p3)"
}

typealias Triangle2 = Triangle<Point2, Dim2, Flt64>
typealias Triangle3 = Triangle<Point3, Dim3, Flt64>

fun Triangle2.area2D(): Flt64 {
    val v1x = p2.x - p1.x
    val v1y = p2.y - p1.y
    val v2x = p3.x - p1.x
    val v2y = p3.y - p1.y
    val cross = v1x * v2y - v1y * v2x
    return cross.abs() / Flt64.two
}

infix fun Triangle2.containsPoint(point: Point2): Boolean {
    val v0x = p3.x - p1.x
    val v0y = p3.y - p1.y
    val v1x = p2.x - p1.x
    val v1y = p2.y - p1.y
    val v2x = point.x - p1.x
    val v2y = point.y - p1.y

    val dot00 = v0x * v0x + v0y * v0y
    val dot01 = v0x * v1x + v0y * v1y
    val dot02 = v0x * v2x + v0y * v2y
    val dot11 = v1x * v1x + v1y * v1y
    val dot12 = v1x * v2x + v1y * v2y

    val denom = dot00 * dot11 - dot01 * dot01
    if (denom eq Flt64.zero) {
        return false
    }

    val u = (dot11 * dot02 - dot01 * dot12) / denom
    val v = (dot00 * dot12 - dot01 * dot02) / denom

    return u geq Flt64.zero && v geq Flt64.zero && u + v leq Flt64.one
}

fun Triangle2.circumcircle(): Circle2 {
    return Circle.circumcircleOf(this)
}

fun Triangle2.circumcenter(): Point2 {
    val cc = circumcircle()
    return cc.center
}

fun Triangle2.incenter(): Point2 {
    val a = e2.length
    val b = e3.length
    val c = e1.length
    val perimeter = a + b + c

    if (perimeter eq Flt64.zero) {
        return p1
    }

    return point2(
        (a * p1.x + b * p2.x + c * p3.x) / perimeter,
        (a * p1.y + b * p2.y + c * p3.y) / perimeter
    )
}

fun Triangle3.area3D(): Flt64 {
    val v1 = vector3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
    val v2 = vector3(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)
    val cross = v1 cross v2
    return cross.norm / Flt64.two
}

fun Triangle3.normal(): Vector3? {
    val v1 = vector3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
    val v2 = vector3(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)
    val cross = v1 cross v2

    if (cross.norm eq Flt64.zero) {
        return null
    }

    return cross.unit
}
