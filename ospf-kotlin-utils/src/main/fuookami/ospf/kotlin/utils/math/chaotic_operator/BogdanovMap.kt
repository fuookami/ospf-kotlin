package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class BogdanovMap(
    val epsilon: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val kappa: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val mu: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
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
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            epsilon: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            kappa: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            mu: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            x: Point2 = point2(
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
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
