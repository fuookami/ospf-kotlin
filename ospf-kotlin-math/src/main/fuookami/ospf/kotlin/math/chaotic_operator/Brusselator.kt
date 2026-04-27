/**
 * 布鲁塞尔振子
 * Brusselator
 *
 * 布鲁塞尔振子是由 Ilya Prigogine 在布鲁塞尔自由大学提出的化学反应动力学模型。
 * 该模型描述了自催化化学反应中的振荡行为，是非平衡态热力学和化学反应动力学研究的重要范例。
 * 常用于化学反应动力学研究、自组织现象分析和非线性动力学教学。
 *
 * The Brusselator is a chemical reaction kinetics model proposed by Ilya Prigogine at the Free University of Brussels.
 * This model describes oscillatory behavior in autocatalytic chemical reactions, serving as an important example for nonequilibrium thermodynamics and chemical reaction kinetics research.
 * Commonly used for chemical reaction kinetics research, self-organization phenomena analysis, and nonlinear dynamics education.
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

/**
 * 布鲁塞尔振子
 * Brusselator
 */
data class Brusselator(
    val a: Flt64 = Flt64.one,
    val b: Flt64 = Flt64.three,
    val h: Flt64 = Flt64(0.01)
) : Extractor<Point2, Point2> {
    override operator fun invoke(x: Point2): Point2 {
        val temp1 = a * x[0].sqr() * x[1]
        val temp2 = b * x[0]
        val dx = temp1 - temp2 - x[0] + Flt64.one
        val dy = temp2 - temp1
        return point2(
            x[0] + h * dx,
            x[1] + h * dy
        )
    }
}

/**
 * 布鲁塞尔振子生成器
 * Brusselator Generator
 */
data class BrusselatorGenerator(
    val brusselator: Brusselator = Brusselator(),
    private var _x: Point2 = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point2> {
    companion object {
        operator fun invoke(
            a: Flt64 = Flt64.one,
            b: Flt64 = Flt64.three,
            h: Flt64 = Flt64(0.01),
            x: Point2 = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BrusselatorGenerator {
            return BrusselatorGenerator(
                Brusselator(
                    a = a,
                    b = b,
                    h = h
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point2 {
        val x = _x.copy()
        _x = brusselator(x)
        return x
    }
}






