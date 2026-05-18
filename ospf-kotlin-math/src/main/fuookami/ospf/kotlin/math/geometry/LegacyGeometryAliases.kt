package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64

typealias Point2 = Point<Dim2, Flt64>
typealias Point3 = Point<Dim3, Flt64>

typealias Vector2 = Vector<Dim2, Flt64>
typealias Vector3 = Vector<Dim3, Flt64>

typealias Rectangle2 = Rectangle<Point2, Dim2, Flt64>

@Suppress("FunctionName")
fun Point2(x: Flt64, y: Flt64): Point2 = point2(x, y)

@Suppress("FunctionName")
fun Point3(x: Flt64, y: Flt64, z: Flt64): Point3 = point3(x, y, z)
