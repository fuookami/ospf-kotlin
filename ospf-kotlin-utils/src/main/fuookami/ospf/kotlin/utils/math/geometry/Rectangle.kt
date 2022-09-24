package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*

class Rectangle<P : Point>(
    val p1: P,
    val p2: P,
    val p3: P,
    val p4: P
) {
}

typealias Rectangle2 = Rectangle<Point2>
typealias Rectangle3 = Rectangle<Point3>

fun Rectangle2.area(): Flt64 {
    val (minX, maxX) = minMaxOf(p1.x, p2.x, p3.x, p4.x)
    val (minY, maxY) = minMaxOf(p1.y, p2.y, p3.y, p4.y)
    val length = maxX - minX
    val width = maxY - minY
    return length * width
}
