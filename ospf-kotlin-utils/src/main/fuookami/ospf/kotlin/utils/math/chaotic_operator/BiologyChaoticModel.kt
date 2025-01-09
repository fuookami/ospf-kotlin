package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class BiologyChaoticModel(
    val a: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val b: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val c: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val r: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        return point3(
            r * x[0] * (Flt64.one - a * x[0] - b * x[1] - c * x[2]),
            x[0],
            x[1]
        )
    }
}

data class BiologyChaoticModelGenerator(
    val biologyChaoticModel: BiologyChaoticModel = BiologyChaoticModel(),
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
            r: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            x: Point3 = point3(
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
                Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
            )
        ): BiologyChaoticModelGenerator {
            return BiologyChaoticModelGenerator(
                BiologyChaoticModel(a, b, c, r),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x
        _x = biologyChaoticModel(x)
        return x
    }
}
