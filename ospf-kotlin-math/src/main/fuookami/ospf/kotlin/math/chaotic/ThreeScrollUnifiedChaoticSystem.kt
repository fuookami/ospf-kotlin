/**
 * 三涡卷统一混沌系统
 * Three-Scroll Unified Chaotic System (TSUCS)
*/
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 三涡卷统一混沌系统 TSUCS1
 * Three-Scroll Unified Chaotic System TSUCS1
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property epsilon 系统参数 epsilon / System parameter epsilon
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property rho 系统参数 rho / System parameter rho
 * @property h 时间步长 / Time step size
*/
data class ThreeScrollUnifiedChaoticSystemTsucs1Attractor<V : FloatingNumber<V>>(
    val alpha: V, val beta: V, val delta: V, val epsilon: V, val zeta: V, val rho: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = alpha * (y - x) + delta * x * z
        val dy = rho * x - x * z + zeta * y
        val dz = beta * z + x * y - epsilon * x * x
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(40.0),
            beta: Flt64 = Flt64(0.833),
            delta: Flt64 = Flt64(0.5),
            epsilon: Flt64 = Flt64(0.65),
            zeta: Flt64 = Flt64(20.0),
            rho: Flt64 = Flt64(55.0),
            h: Flt64 = Flt64(0.001)
        ): ThreeScrollUnifiedChaoticSystemTsucs1Attractor<Flt64> =
            ThreeScrollUnifiedChaoticSystemTsucs1Attractor(alpha, beta, delta, epsilon, zeta, rho, h)
    }
}

/**
 * 三涡卷统一混沌系统 TSUCS1 吸引子生成器
 * Three-Scroll Unified Chaotic System TSUCS1 attractor generator
 *
 * @property attractor TSUCS1 吸引子实例 / TSUCS1 attractor instance
*/
data class ThreeScrollUnifiedChaoticSystemTsucs1AttractorGenerator(
    val attractor: ThreeScrollUnifiedChaoticSystemTsucs1Attractor<Flt64> = ThreeScrollUnifiedChaoticSystemTsucs1Attractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        )
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}

/**
 * 三涡卷统一混沌系统 TSUCS2
 * Three-Scroll Unified Chaotic System TSUCS2
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property rho 系统参数 rho / System parameter rho
 * @property h 时间步长 / Time step size
*/
data class ThreeScrollUnifiedChaoticSystemTsucs2Attractor<V : FloatingNumber<V>>(
    val alpha: V, val beta: V, val delta: V, val zeta: V, val rho: V, val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val three = x.constants.two + x.constants.one
        val dx = alpha * (y - x) + delta * x * z
        val dy = rho * x - x * z + zeta * y
        val dz = beta * z + x * y / three
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(40.0),
            beta: Flt64 = Flt64(0.833),
            delta: Flt64 = Flt64(0.5),
            zeta: Flt64 = Flt64(20.0),
            rho: Flt64 = Flt64(55.0),
            h: Flt64 = Flt64(0.001)
        ): ThreeScrollUnifiedChaoticSystemTsucs2Attractor<Flt64> =
            ThreeScrollUnifiedChaoticSystemTsucs2Attractor(alpha, beta, delta, zeta, rho, h)
    }
}

/**
 * 三涡卷统一混沌系统 TSUCS2 吸引子生成器
 * Three-Scroll Unified Chaotic System TSUCS2 attractor generator
 *
 * @property attractor TSUCS2 吸引子实例 / TSUCS2 attractor instance
*/
data class ThreeScrollUnifiedChaoticSystemTsucs2AttractorGenerator(
    val attractor: ThreeScrollUnifiedChaoticSystemTsucs2Attractor<Flt64> = ThreeScrollUnifiedChaoticSystemTsucs2Attractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        )
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
