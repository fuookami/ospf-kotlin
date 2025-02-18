package fuookami.ospf.kotlin.utils.math.chaotic_operator

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class ChebyshevMap(
    val a: Flt64 = Random.nextFlt64(Flt64.two, Flt64.ten),
) : Extractor<Flt64, Flt64> {
    override operator fun invoke(x: Flt64): Flt64 {
        return if (x geq -Flt64.one && x leq Flt64.one) {
            (a * x.acos()!!).cos()
        } else {
            Flt64.zero
        }
    }
}

data class ChebyshevMapGenerator(
    val chebyshevMap: ChebyshevMap = ChebyshevMap(),
    private var _x: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.two, Flt64.ten),
            x: Flt64 = Random.nextFlt64(-Flt64.one, Flt64.one)
        ): ChebyshevMapGenerator {
            return ChebyshevMapGenerator(
                ChebyshevMap(a),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = chebyshevMap(x)
        return x
    }
}
