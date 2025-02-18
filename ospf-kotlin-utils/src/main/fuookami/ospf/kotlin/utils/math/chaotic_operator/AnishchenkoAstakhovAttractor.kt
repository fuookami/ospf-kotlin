package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class AnishchenkoAstakhovAttractor(
    val mu: Flt64 = Flt64(1.2),
    val eta: Flt64 = Flt64(0.5),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val i = if (x[0] geq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
        val dx = mu * x[0] + x[1] - x[0] * x[2]
        val dy = -x[0]
        val dz = -eta * x[2] + eta * i * x[0].sqr()
        return point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class AnishchenkoAstakhovAttractorGenerator(
    val anishchenkoAstakhovAttractor: AnishchenkoAstakhovAttractor = AnishchenkoAstakhovAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            mu: Flt64 = Flt64(1.2),
            eta: Flt64 = Flt64(0.5),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): AnishchenkoAstakhovAttractorGenerator {
            return AnishchenkoAstakhovAttractorGenerator(
                AnishchenkoAstakhovAttractor(mu, eta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = anishchenkoAstakhovAttractor(x)
        return x
    }
}
