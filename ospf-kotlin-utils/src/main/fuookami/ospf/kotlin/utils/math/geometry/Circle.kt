package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*

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

val Circle2.x: Flt64 get() = center.x
val Circle2.y: Flt64 get() = center.y

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
