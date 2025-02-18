package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class CapacitanceEquation(
    val a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val e: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        val g = if (x[0] gr Flt64.one) {
            e * x[0] - (e - d)
        } else if (x[0] ls -Flt64.one) {
            e * x[1] + (e - d)
        } else {
            d * x[0]
        }
        val dx = a * ((c - Flt64.one) * g + x[1])
        val dy = g - x[1] + x[2]
        val dz = -b * x[1]
        return Point3(
            x[0] + h * dx,
            x[1] + h * dy,
            x[2] + h * dz
        )
    }
}

data class CapacitanceEquationGenerator(
    val capacitanceEquation: CapacitanceEquation = CapacitanceEquation(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            d: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            e: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ) : CapacitanceEquationGenerator {
            return CapacitanceEquationGenerator(
                CapacitanceEquation(a, b, c, d, e, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = capacitanceEquation(x)
        return x
    }
}
