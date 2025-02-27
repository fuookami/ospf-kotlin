package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ChuaAttractor(
    val alpha: Flt64 = Flt64(15.6),
    val beta: Flt64 = Flt64(1.0),
    val delta: Flt64 = Flt64(-1.0),
    val epsilon: Flt64 = Flt64(0.0),
    val zeta: Flt64 = Flt64(25.58),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val g = epsilon * x[0] + (delta - epsilon) * ((x[0] + Flt64.one).abs() - (x[0] - Flt64.one).abs())
        val dx = alpha * (x[1] - x[0] - g)
        val dy = beta * (x[0] - x[1] + x[2])
        val dz = -zeta * x[1]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChuaAttractorGenerator(
    val chuaAttractor: ChuaAttractor = ChuaAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(15.6),
            beta: Flt64 = Flt64(1.0),
            delta: Flt64 = Flt64(-1.0),
            epsilon: Flt64 = Flt64(0.0),
            zeta: Flt64 = Flt64(25.58),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChuaAttractorGenerator {
            return ChuaAttractorGenerator(
                ChuaAttractor(alpha, beta, delta, epsilon, zeta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chuaAttractor(x)
        return _x
    }
}
