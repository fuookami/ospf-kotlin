/**
 * Aizawa 吸引子
 * Aizawa Attractor
 *
 * Aizawa 吸引子是一个三维连续时间混沌系统，由 Hiroshi Aizawa 于 1982 年提出。
 * 该系统展现出独特的混沌行为，其轨迹在三维空间中形成优美的螺旋状吸引子结构。
 * 常用于混沌信号生成、非线性动力学研究和混沌同步应用。
 *
 * The Aizawa attractor is a three-dimensional continuous-time chaotic system proposed by Hiroshi Aizawa in 1982.
 * This system exhibits unique chaotic behavior, with trajectories forming beautiful spiral-like attractor structures in 3D space.
 * Commonly used for chaotic signal generation, nonlinear dynamics research, and chaos synchronization applications.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

/**
 * Aizawa 吸引子
 * Aizawa Attractor
 */
data class AizawaAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val gamma: V,
    val delta: V,
    val epsilon: V,
    val zeta: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = alpha
        val dy = delta * x[0] + (x[2] - beta) * x[1]
        val dx = (x[2] - beta) * x[0] - dy
        val dz = gamma + alpha * x[0] - x[2].cub() / v.constants.three - (x[0].sqr() + x[1].sqr()) * (v.constants.one + epsilon * x[2]) + zeta * x[2] * x[0].cub()
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dz, x[2] + h * dy), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.95),
            beta: Flt64 = Flt64(0.7),
            gamma: Flt64 = Flt64(0.6),
            delta: Flt64 = Flt64(3.5),
            epsilon: Flt64 = Flt64(0.25),
            zeta: Flt64 = Flt64(0.1),
            h: Flt64 = Flt64(0.01)
        ): AizawaAttractor<Flt64> {
            return AizawaAttractor(alpha, beta, gamma, delta, epsilon, zeta, h)
        }
    }
}

/**
 * Aizawa 吸引子生成器
 * Aizawa Attractor Generator
 */
data class AizawaAttractorGenerator(
    val aizawaAttractor: AizawaAttractor<Flt64> = AizawaAttractor(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.95),
            beta: Flt64 = Flt64(0.7),
            gamma: Flt64 = Flt64(0.6),
            delta: Flt64 = Flt64(3.5),
            epsilon: Flt64 = Flt64(0.25),
            zeta: Flt64 = Flt64(0.1),
            h: Flt64 = Flt64(0.01),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): AizawaAttractorGenerator {
            return AizawaAttractorGenerator(
                AizawaAttractor(
                    alpha = alpha,
                    beta = beta,
                    gamma = gamma,
                    delta = delta,
                    epsilon = epsilon,
                    zeta = zeta,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x.copy()
        _x = aizawaAttractor(x)
        return x
    }
}
