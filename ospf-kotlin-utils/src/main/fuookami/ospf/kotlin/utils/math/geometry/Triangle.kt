package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*

class Triangle<P : Point>(
    val p1: P,
    val p2: P,
    val p3: P
) {
    val e1: Edge<P> get() = Edge(p1, p2)
    val e2: Edge<P> get() = Edge(p2, p3)
    val e3: Edge<P> get() = Edge(p3, p1)

    // Heron's formula
    val area: Flt64 get() {
        val a = e1.length
        val b = e2.length
        val c = e3.length
        val p = (a + b + c) / Flt64.two
        val s = p * (p - a) * (p - b) * (p - c)
        return s.sqr().toFlt64()
    }
}

typealias Triangle2 = Triangle<Point2>
typealias Triangle3 = Triangle<Point3>
