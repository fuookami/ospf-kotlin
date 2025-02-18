package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data object BakersMap : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        return point2(
            (Flt64.two * x[0]) mod Flt64.one,
            ((Flt64.two * x[0]).floor() + x[1]) / Flt64.two
        )
    }
}

data class BakersMapGenerator(
    val bakersMap: BakersMap = BakersMap,
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BakersMapGenerator {
            return BakersMapGenerator(BakersMap, x)
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = bakersMap(x)
        return x
    }
}
