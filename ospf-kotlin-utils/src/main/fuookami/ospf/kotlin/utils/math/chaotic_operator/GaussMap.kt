package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class GaussMap(
    val mu: Flt64 = Random.nextFlt64(Flt64.one, Flt64.ten)
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
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            mu: Flt64,
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
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
