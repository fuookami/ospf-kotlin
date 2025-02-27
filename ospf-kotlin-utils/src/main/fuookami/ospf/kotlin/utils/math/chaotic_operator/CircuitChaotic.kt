package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class CircuitChaotic(
    val a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
    val b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
    val c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
    val d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
) : Extractor<Point2, Point2> {
    override fun invoke(x: Point2): Point2 {
        return point2(
            a * x[1] - d * x[1].sqr(),
            -b * x[0] + c * x[1]
        )
    }
}

data class CircuitChaoticGenerator(
    val circuitChaotic: CircuitChaotic = CircuitChaotic(),
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.ten),
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ) : CircuitChaoticGenerator {
            return CircuitChaoticGenerator(
                CircuitChaotic(a, b, c, d),
                x
            )
        }
    }

    val x by ::_x

    override fun invoke(): Point2 {
        val x = _x.copy()
        _x = circuitChaotic(x)
        return x
    }
}
