package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class ChenCelikovskyAttractor(
    val alpha: Flt64 = Flt64(36.0),
    val beta: Flt64 = Flt64(3.0),
    val delta: Flt64 = Flt64(20.0),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = alpha * (x[1] - x[0])
        val dy = -x[0] * x[2] + delta * x[1]
        val dz = x[0] * x[1] - beta * x[2]
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class ChenCelikovskyAttractorGenerator(
    val chenCelikovskyAttractor: ChenCelikovskyAttractor = ChenCelikovskyAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(36.0),
            beta: Flt64 = Flt64(3.0),
            delta: Flt64 = Flt64(20.0),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            )
        ): ChenCelikovskyAttractorGenerator {
            return ChenCelikovskyAttractorGenerator(
                ChenCelikovskyAttractor(alpha, beta, delta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = chenCelikovskyAttractor(x)
        return x
    }
}
