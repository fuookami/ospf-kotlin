package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*

class Triangle<P : Point>(
    val p1: P,
    val p2: P,
    val p3: P
) {
}

typealias Triangle2 = Triangle<Point2>
typealias Triangle3 = Triangle<Point3>

//tex:$$
//S = \frac{1}{2} \cdot (-y_{1} \cdot x_{2} + y_{0} \cdot (-x_{1} + x_{2}) + x_{0} \cdot (y_{1} - y_{2}) + x_{1} \cdot y_{2})
//$$
fun Triangle2.area(): Flt64 {
    return Flt64(0.5) * (-p2.y * p3.x + p1.y * (-p2.x + p3.x) + p1.x * (p2.y - p3.y) + p2.x * p3.y)
}
