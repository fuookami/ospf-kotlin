package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ChuaCircuit(
    val a: Flt64 = Flt64(15.6),
    val b: Flt64 = Flt64(28.0),
    val c: Flt64 = Flt64(-0.71),
    val d: Flt64 = Flt64(-1.14),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val f = c * x[0] + Flt64(0.5) * (d - c) * ((x[0] + Flt64.one).abs() - (x[0] - Flt64.one).abs())
        val dx = a * (x[1] - x[0] - f)
        val dy = x[0] - x[1] + x[2]
        val dz = -b * x[1]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChuaCircuitGenerator(
    val chuaCircuit: ChuaCircuit = ChuaCircuit(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(15.6),
            b: Flt64 = Flt64(28.0),
            c: Flt64 = Flt64(-0.71),
            d: Flt64 = Flt64(-1.14),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChuaCircuitGenerator {
            return ChuaCircuitGenerator(
                ChuaCircuit(a, b, c, d, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chuaCircuit(x)
        return x
    }
}
