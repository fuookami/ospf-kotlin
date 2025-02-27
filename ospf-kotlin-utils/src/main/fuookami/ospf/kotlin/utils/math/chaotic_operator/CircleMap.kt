package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class CircleMap(
    val alpha: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val beta: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, pi2)
) : Extractor<Flt64, Flt64> {
    companion object {
        val pi2 = Flt64.pi * Flt64.two
    }

    override fun invoke(x: Flt64): Flt64 {
        return (x + alpha - beta * (x * pi2).sin() / pi2) mod Flt64.one
    }
}

data class CircleMapGenerator(
    val circleMap: CircleMap = CircleMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            beta: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, CircleMap.pi2),
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): CircleMapGenerator {
            return CircleMapGenerator(
                CircleMap(alpha, beta),
                x
            )
        }
    }

    val x by ::_x

    override fun invoke(): Flt64 {
        val x = _x.copy()
        _x = circleMap(x)
        return x
    }
}
