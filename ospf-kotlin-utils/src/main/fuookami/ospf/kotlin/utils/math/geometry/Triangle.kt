package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*

class Triangle<P : Point<D>, D : Dimension>(
    val p1: P,
    val p2: P,
    val p3: P
) {
    init {
        assert(p1.size == p2.size)
        assert(p2.size == p3.size)
    }

    val e1: Edge<P, D> get() = Edge(p1, p2)
    val e2: Edge<P, D> get() = Edge(p2, p3)
    val e3: Edge<P, D> get() = Edge(p3, p1)

    // Heron's formula
    val area: Flt64 by lazy {
        val a = e1.length
        val b = e2.length
        val c = e3.length
        val p = (a + b + c) / Flt64.two
        val s = p * (p - a) * (p - b) * (p - c)
        s.sqrt().toFlt64()
    }

    val illegal: Boolean by lazy {
        for (i in (0 until p1.size)) {
            if (p1[i] eq p2[i] && p2[i] eq p3[i]) {
                return@lazy true
            }
        }
        false
    }
}

typealias Triangle2 = Triangle<Point2, Dim2>
typealias Triangle3 = Triangle<Point3, Dim3>
