package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class BurkeShawAttractor(
    val zeta: Flt64 = Flt64(10.0),
    val nu: Flt64 = Flt64(4.272),
    val h: Flt64 = Flt64(0.01),
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dx = -zeta * (x[0] + x[1])
        val dy = -x[1] - zeta * x[0] * x[2]
        val dz = zeta * x[0] * x[1] + nu
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class BurkeShawAttractorGenerator(
    val burkeShawAttractor: BurkeShawAttractor = BurkeShawAttractor(),
    private var _x: Point3 = point3(
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            zeta: Flt64 = Flt64(10.0),
            nu: Flt64 = Flt64(4.272),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
            )
        ): BurkeShawAttractorGenerator {
            return BurkeShawAttractorGenerator(
                BurkeShawAttractor(zeta, nu, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = burkeShawAttractor(x)
        return x
    }
}
