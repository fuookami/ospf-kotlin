package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class CapacitanceEquation(
    val a: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val b: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val c: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val d: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val e: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
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
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
        Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            b: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            c: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            d: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            e: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
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
