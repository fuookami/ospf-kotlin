package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class GaussMap(
    val mu: Flt64 = Flt64(Random.nextDouble(1.0, 10.0))
) : Extractor<Flt64, Flt64> {
    override operator fun invoke(x: Flt64): Flt64 {
        return if (x eq Flt64.zero) {
            Flt64.zero
        } else {
            mu / x
        }
    }
}

data class GaussMapGenerator(
    val gaussMap: GaussMap = GaussMap(),
    private var _x: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            mu: Flt64,
            x: Flt64 = Flt64(Random.nextDouble(Flt64.decimalPrecision.toDouble(), 1.0))
        ): GaussMapGenerator {
            return GaussMapGenerator(
                GaussMap(mu),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = gaussMap(x)
        return x
    }
}
