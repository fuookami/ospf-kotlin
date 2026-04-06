/**
 * Bogdanov 映射
 * Bogdanov Map
 *
 * Bogdanov 映射是一个二维离散混沌映射，由 Bogdanov 提出。
 * 该映射与 Bogdanov-Takens 分岔相关，是研究二维映射分岔行为的重要模型。
 * 常用于分岔理论研究、混沌边界分析和非线性动力学研究。
 *
 * The Bogdanov map is a two-dimensional discrete chaotic map proposed by Bogdanov.
 * This map is related to the Bogdanov-Takens bifurcation and serves as an important model for studying bifurcation behavior in two-dimensional maps.
 * Commonly used for bifurcation theory research, chaos boundary analysis, and nonlinear dynamics research.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

data class BogdanovMap(
    val epsilon: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val mu: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val temp = x[1] + epsilon * x[1] + kappa * x[0] * (Flt64.one - x[0]) + mu * x[0] * x[1]
        return point2(
            x[0] + temp,
            temp
        )
    }
}

data class BogdanovMapGenerator(
    val bogdanovMap: BogdanovMap = BogdanovMap(),
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            epsilon: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            kappa: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            mu: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BogdanovMapGenerator {
            return BogdanovMapGenerator(
                BogdanovMap(
                    epsilon = epsilon,
                    kappa = kappa,
                    mu = mu
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = bogdanovMap(x)
        return x
    }
}






