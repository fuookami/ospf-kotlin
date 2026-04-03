package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64

class Quadrilateral<P : Point<D>, D : Dimension>(
    val p1: P,
    val p2: P,
    val p3: P,
    val p4: P
) {
    init {
        assert(p1.size == p2.size)
        assert(p2.size == p3.size)
        assert(p3.size == p4.size)
    }

    val e1: Edge<P, D> get() = Edge(p1, p2)
    val e2: Edge<P, D> get() = Edge(p2, p3)
    val e3: Edge<P, D> get() = Edge(p3, p4)
    val e4: Edge<P, D> get() = Edge(p4, p1)

    val edges: List<Edge<P, D>> get() = listOf(e1, e2, e3, e4)
    val diagonals: List<Edge<P, D>> get() = listOf(Edge(p1, p3), Edge(p2, p4))

    val perimeter: Flt64 by lazy { e1.length + e2.length + e3.length + e4.length }

    val centroid: Point<D> by lazy {
        Point(
            p1.indices.map { (p1[it] + p2[it] + p3[it] + p4[it]) / Flt64(4.0) },
            p1.dim
        )
    }

    val areaByTriangles: Flt64 by lazy {
        Triangle(p1, p2, p3).area + Triangle(p1, p3, p4).area
    }
}

typealias Quadrilateral2 = Quadrilateral<Point2, Dim2>
typealias Quadrilateral3 = Quadrilateral<Point3, Dim3>

val Quadrilateral2.area: Flt64
    get() {
        val sum1 = p1.x * p2.y + p2.x * p3.y + p3.x * p4.y + p4.x * p1.y
        val sum2 = p1.y * p2.x + p2.y * p3.x + p3.y * p4.x + p4.y * p1.x
        val doubleArea = sum1 - sum2
        val absolute = if (doubleArea < Flt64.zero) {
            -doubleArea
        } else {
            doubleArea
        }
        return absolute / Flt64.two
    }

fun Quadrilateral2.isConvex(): Boolean {
    fun crossSign(a: Point2, b: Point2, c: Point2): Flt64 {
        val v1 = vector2(b.x - a.x, b.y - a.y)
        val v2 = vector2(c.x - b.x, c.y - b.y)
        return v1 cross v2
    }

    val cross1 = crossSign(p1, p2, p3)
    val cross2 = crossSign(p2, p3, p4)
    val cross3 = crossSign(p3, p4, p1)
    val cross4 = crossSign(p4, p1, p2)

    val allPositive = cross1 > Flt64.zero && cross2 > Flt64.zero && cross3 > Flt64.zero && cross4 > Flt64.zero
    val allNegative = cross1 < Flt64.zero && cross2 < Flt64.zero && cross3 < Flt64.zero && cross4 < Flt64.zero
    return allPositive || allNegative
}

val Quadrilateral2.illegal: Boolean
    get() = area eq Flt64.zero

fun quadrilateral2(p1: Point2, p2: Point2, p3: Point2, p4: Point2): Quadrilateral2 {
    return Quadrilateral(p1, p2, p3, p4)
}

