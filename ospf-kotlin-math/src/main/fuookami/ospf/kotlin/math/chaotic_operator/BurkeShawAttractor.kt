/**
 * Burke-Shaw 吸引孌
 * Burke-Shaw Attractor
 *
 * Burke-Shaw 吸引子是一个三维连续时间混沌系统，甌Burke 和Shaw 提出。
 * 该系统通过非线性项产生混沌行为，轨迹在三维空间中形成独特的吸引子结构。
 * 常用于混沌系统研究、非线性动力学分析和混沌信号处理。
 *
 * The Burke-Shaw attractor is a three-dimensional continuous-time chaotic system proposed by Burke and Shaw.
 * This system generates chaotic behavior through nonlinear terms, with trajectories forming unique attractor structures in three-dimensional space.
 * Commonly used for chaotic system research, nonlinear dynamics analysis, and chaotic signal processing.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

data class BurkeShawAttractor<V : FloatingNumber<V>>(
    val zeta: V,
    val nu: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val dx = -zeta * (x[0] + x[1])
        val dy = -x[1] - zeta * x[0] * x[2]
        val dz = zeta * x[0] * x[1] + nu
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            zeta: Flt64 = Flt64(10.0),
            nu: Flt64 = Flt64(4.272),
            h: Flt64 = Flt64(0.01)
        ): BurkeShawAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return BurkeShawAttractor(zeta, nu, h)
        }
    }
}

data class BurkeShawAttractorGenerator(
    val burkeShawAttractor: BurkeShawAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> = BurkeShawAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            zeta: Flt64 = Flt64(10.0),
            nu: Flt64 = Flt64(4.272),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BurkeShawAttractorGenerator {
            return BurkeShawAttractorGenerator(
                BurkeShawAttractor(zeta, nu, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = burkeShawAttractor(x)
        return x
    }
}
