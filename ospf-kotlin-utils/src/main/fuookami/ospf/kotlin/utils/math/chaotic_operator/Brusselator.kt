package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class Brusselator(
    val a: Flt64 = Flt64.one,
    val b: Flt64 = Flt64.three,
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val temp1 = a * x[0].sqr() * x[1]
        val temp2 = b * x[0]
        val dx = temp1 - temp2 - x[0] + Flt64.one
        val dy = temp2 - temp1
        return point2(
            x[0] + h * dx,
            x[1] + h * dy
        )
    }
}

data class BrusselatorGenerator(
    val brusselator: Brusselator = Brusselator(),
    private var _x: Point2 = point2(
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64.one,
            b: Flt64 = Flt64.three,
            h: Flt64 = Flt64(0.01),
            x: Point2 = point2(
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
            )
        ): BrusselatorGenerator {
            return BrusselatorGenerator(
                Brusselator(a, b, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = brusselator(x)
        return x
    }
}
