package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

interface Point {}

data class Point2(
    val x: Flt64,
    val y: Flt64
) : Point, Plus<Point2, Point2>, Minus<Point2, Point2> {
    override fun plus(rhs: Point2) = Point2(x + rhs.x, y + rhs.y)
    override fun minus(rhs: Point2) = Point2(x - rhs.x, y - rhs.y)
}

data class Point3(
    val x: Flt64,
    val y: Flt64,
    val z: Flt64
) : Point, Plus<Point3, Point3>, Minus<Point3, Point3> {
    override fun plus(rhs: Point3) = Point3(x + rhs.x, y + rhs.y, z + rhs.z)
    override fun minus(rhs: Point3) = Point3(x - rhs.x, y - rhs.y, z - rhs.z)
}
