/**
 * 帐篷映射
 * Tent Map
 *
 * 帐篷映射是一个分段线性的一维混沌映射，因其图像形似帐篷而得名。
 * 该映射具有均匀的不变密度分布，常用于混沌理论研究和伪随机数生成。
 *
 * The tent map is a piecewise linear one-dimensional chaotic map, named for its tent-shaped graph.
 * This map has a uniform invariant density distribution, commonly used for chaos theory research and pseudo-random number generation.
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 帐篷映射
 * Tent Map
 *
 * 公式 / Formula:
 * x_{n+1} = mu * x,         if x < 0.5
 * x_{n+1} = mu * (1 - x),   if x >= 0.5
 *
 * @property mu 系统参数 mu，取值范围 (0, 2] / System parameter mu, range (0, 2]
*/
data class TentMap<V : FloatingNumber<V>>(
    val mu: V
) : Extractor<V, V> {
    override operator fun invoke(x: V): V {
        val half = x.constants.half
        return if (x ls half) {
            mu * x
        } else {
            mu * (x.constants.one - x)
        }
    }

    companion object {
        operator fun invoke(
            mu: Flt64 = Flt64(1.5)
        ): TentMap<Flt64> {
            return TentMap(mu)
        }
    }
}

/**
 * 帐篷映射生成器
 * Tent Map Generator
*/
data class TentMapGenerator(
    val tentMap: TentMap<Flt64> = TentMap(),
    private var _x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Generator<Flt64> {
    companion object {
        operator fun invoke(
            mu: Flt64,
            x: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): TentMapGenerator {
            return TentMapGenerator(
                TentMap(mu),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Flt64 {
        val x = _x.copy()
        _x = tentMap(x)
        return x
    }
}
