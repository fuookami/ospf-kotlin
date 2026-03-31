package fuookami.ospf.kotlin.utils.math.chaotic_operator

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.geometry.Point2
import fuookami.ospf.kotlin.utils.math.geometry.point2
import fuookami.ospf.kotlin.utils.math.nextFlt64
import kotlin.random.Random

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
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64.one,
            b: Flt64 = Flt64.three,
            h: Flt64 = Flt64(0.01),
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BrusselatorGenerator {
            return BrusselatorGenerator(
                Brusselator(
                    a = a,
                    b = b,
                    h = h
                ),
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






