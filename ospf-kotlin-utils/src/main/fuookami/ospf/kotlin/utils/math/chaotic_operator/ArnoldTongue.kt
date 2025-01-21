package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class ArnoldTongue(
    val omega: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
    val kappa: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), (Flt64.pi * Flt64.two).toDouble())),
) : Extractor<Flt64, Flt64> {
    override operator fun invoke(x: Flt64): Flt64 {
        return x + omega - kappa / (Flt64.pi * Flt64.two) * (Flt64.pi * Flt64.two * x).sin()
    }
}

data class ArnoldTongueGenerator(
    val arnoldTongue: ArnoldTongue = ArnoldTongue(),
    private var _x: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            omega: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0)),
            kappa: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), (Flt64.pi * Flt64.two).toDouble())),
            x: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
        ): ArnoldTongueGenerator {
            return ArnoldTongueGenerator(
                ArnoldTongue(omega, kappa),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = arnoldTongue(x)
        return x
    }
}
