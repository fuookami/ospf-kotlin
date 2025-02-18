package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ChenLeeAttractor(
    val alpha: Flt64 = Flt64(5.0),
    val beta: Flt64 = Flt64(-10.0),
    val delta: Flt64 = Flt64(0.38),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = alpha * x[0] - x[1] * x[2]
        val dy = beta * x[1] + x[0] * x[2]
        val dz = delta * x[2] + x[0] * x[1] / Flt64(3.0)
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChenLeeAttractorGenerator(
    val chenLeeAttractor: ChenLeeAttractor = ChenLeeAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(5.0),
            beta: Flt64 = Flt64(-10.0),
            delta: Flt64 = Flt64(0.38),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChenLeeAttractorGenerator {
            return ChenLeeAttractorGenerator(
                ChenLeeAttractor(alpha, beta, delta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chenLeeAttractor(x)
        return x
    }
}
