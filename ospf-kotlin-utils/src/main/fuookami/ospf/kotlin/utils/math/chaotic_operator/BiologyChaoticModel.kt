package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*

data class BiologyChaoticModel(
    val a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val r: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
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
            r: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
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
