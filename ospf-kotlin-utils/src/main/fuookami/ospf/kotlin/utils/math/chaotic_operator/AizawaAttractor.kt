package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class AizawaAttractor(
    val alpha: Flt64 = Flt64(0.95),
    val beta: Flt64 = Flt64(0.7),
    val gamma: Flt64 = Flt64(0.6),
    val delta: Flt64 = Flt64(3.5),
    val epsilon: Flt64 = Flt64(0.25),
    val zeta: Flt64 = Flt64(0.1),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val dy = delta * x[0] + (x[2] - beta) * x[1]
        val dx = (x[2] - beta) * x[0] - dy
        val dz = gamma + alpha * x[0] - x[2].cub() / Flt64.three - (x[0].sqr() + x[1].sqr()) * (Flt64.one + epsilon * x[2]) + zeta * x[2] * x[0].cub()
        return point3(
            x[0] + h * dx,
            x[1] + h * dz,
            x[2] + h * dy
        )
    }
}

data class AizawaAttractorGenerator(
    val aizawaAttractor: AizawaAttractor = AizawaAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.95),
            beta: Flt64 = Flt64(0.7),
            gamma: Flt64 = Flt64(0.6),
            delta: Flt64 = Flt64(3.5),
            epsilon: Flt64 = Flt64(0.25),
            zeta: Flt64 = Flt64(0.1),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): AizawaAttractorGenerator {
            return AizawaAttractorGenerator(
                AizawaAttractor(alpha, beta, gamma, delta, epsilon, zeta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = aizawaAttractor(x)
        return x
    }
}
