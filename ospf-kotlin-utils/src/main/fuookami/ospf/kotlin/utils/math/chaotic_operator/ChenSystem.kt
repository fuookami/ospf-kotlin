package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ChenSystem(
    val a: Flt64 = Flt64(10.0),
    val b: Flt64 = Flt64(8.0 / 3.0),
    val c: Flt64 = Flt64(137.0 / 5.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = a * (x[1] - x[0])
        val dy = (c - a) * x[0] - x[0] * x[2] + c * x[1]
        val dz = x[0] * x[1] - b * x[2]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChenSystemGenerator(
    val chenSystem: ChenSystem = ChenSystem(),
    private var _x: Point3 = point3(
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(10.0),
            b: Flt64 = Flt64(8.0 / 3.0),
            c: Flt64 = Flt64(137.0 / 5.0),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            )
        ): ChenSystemGenerator {
            return ChenSystemGenerator(
                ChenSystem(a, b, c, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chenSystem(x)
        return x
    }
}
