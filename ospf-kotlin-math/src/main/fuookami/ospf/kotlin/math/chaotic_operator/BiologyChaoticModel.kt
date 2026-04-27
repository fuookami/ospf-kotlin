/**
 * 生物混沌模型
 * Biological Chaotic Model
 *
 * 生物混沌模型是模拟生态系统中种群动态变化的混沌系统。
 * 该模型反映了多物种之间的竞争关系，参数变化可导致系统从稳定状态过渡到混沌状态。
 * 常用于生态动力学研究、种群演化模拟和复杂系统分析。
 *
 * The biological chaotic model simulates chaotic dynamics of population changes in ecosystems.
 * This model reflects competitive relationships among multiple species, with parameter changes causing the system to transition from stable states to chaotic states.
 * Commonly used for ecological dynamics research, population evolution simulation, and complex systems analysis.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

/**
 * 生物混沌模型
 * Biological Chaotic Model
 */
data class BiologyChaoticModel(
    val a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
    val r: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
) : Extractor<Point3, Point3> {
    override operator fun invoke(x: Point3): Point3 {
        return point3(
            r * x[0] * (Flt64.one - a * x[0] - b * x[1] - c * x[2]),
            x[0],
            x[1]
        )
    }
}

/**
 * 生物混沌模型生成器
 * Biological Chaotic Model Generator
 */
data class BiologyChaoticModelGenerator(
    val biologyChaoticModel: BiologyChaoticModel = BiologyChaoticModel(),
    private var _x: Point3 = point3(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point3> {
    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            r: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            x: Point3 = point3(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BiologyChaoticModelGenerator {
            return BiologyChaoticModelGenerator(
                BiologyChaoticModel(
                    a = a,
                    b = b,
                    c = c,
                    r = r
                ),
                x
            )
        }
    }

    val x by ::_x

    override operator fun invoke(): Point3 {
        val x = _x
        _x = biologyChaoticModel(x)
        return x
    }
}






