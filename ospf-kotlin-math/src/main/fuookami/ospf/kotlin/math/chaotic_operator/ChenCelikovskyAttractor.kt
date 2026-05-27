/**
 * Chen-Celikovsky 吸引孌
 * Chen-Celikovsky Attractor
 *
 * Chen-Celikovsky 吸引子是 Chen 系统的一种变体，甌Chen 和Celikovsky 提出。
 * 该系统在 Lorenz 系统和Chen 系统之间建立起桥梁，展现出独特的混沌动力学特性。
 * 常用于混沌系统研究、控制理论和非线性动力学分析。
 *
 * The Chen-Celikovsky attractor is a variant of the Chen system proposed by Chen and Celikovsky.
 * This system builds a bridge between the Lorenz system and the Chen system, exhibiting unique chaotic dynamical properties.
 * Commonly used for chaotic system research, control theory, and nonlinear dynamics analysis.
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
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.geometry.point3
import kotlin.random.Random

/**
 * Chen-Celikovsky 吸引子
 * Chen-Celikovsky Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property delta 系统参数 delta / System parameter delta
 * @property h 时间步长 / Time step size
 */
data class ChenCelikovskyAttractor<V : FloatingNumber<V>>(
    val alpha: V,
    val beta: V,
    val delta: V,
    val h: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val dx = alpha * (x[1] - x[0])
        val dy = -x[0] * x[2] + delta * x[1]
        val dz = x[0] * x[1] - beta * x[2]
        return Point<Dim3, V>(listOf(x[0] + h * dx, x[1] + h * dy, x[2] + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(36.0),
            beta: Flt64 = Flt64(3.0),
            delta: Flt64 = Flt64(20.0),
            h: Flt64 = Flt64(0.01)
        ): ChenCelikovskyAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            return ChenCelikovskyAttractor(alpha, beta, delta, h)
        }
    }
}

data class ChenCelikovskyAttractorGenerator(
    val chenCelikovskyAttractor: ChenCelikovskyAttractor<fuookami.ospf.kotlin.math.algebra.number.Flt64> = ChenCelikovskyAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(36.0),
            beta: Flt64 = Flt64(3.0),
            delta: Flt64 = Flt64(20.0),
            h: Flt64 = Flt64(0.01),
            x: Point<Dim3, Flt64> = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): ChenCelikovskyAttractorGenerator {
            return ChenCelikovskyAttractorGenerator(
                ChenCelikovskyAttractor(alpha, beta, delta, h),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy()
        _x = chenCelikovskyAttractor(x)
        return x
    }
}
