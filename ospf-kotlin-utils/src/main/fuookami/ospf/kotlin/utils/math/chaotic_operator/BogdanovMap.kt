package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class BogdanovMap(
    val epsilon: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val mu: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val temp = x[1] + epsilon * x[1] + kappa * x[0] * (Flt64.one - x[0]) + mu * x[0] * x[1]
        return point2(
            x[0] + temp,
            temp
        )
    }
}

data class BogdanovMapGenerator(
    val bogdanovMap: BogdanovMap = BogdanovMap(),
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            epsilon: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            mu: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BogdanovMapGenerator {
            return BogdanovMapGenerator(
                BogdanovMap(epsilon, kappa, mu),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = bogdanovMap(x)
        return x
    }
}
