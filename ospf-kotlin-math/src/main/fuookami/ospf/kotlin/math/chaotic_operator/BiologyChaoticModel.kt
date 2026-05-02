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
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Point3
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random

data class BiologyChaoticModel<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val r: V
) : Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(x: Point<Dim3, V>): Point<Dim3, V> {
        val v = r
        return Point<Dim3, V>(listOf(
            r * x[0] * (v.constants.one - a * x[0] - b * x[1] - c * x[2]),
            x[0],
            x[1]
        ), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            b: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            c: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
            r: Flt64 = Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
        ): BiologyChaoticModel<Flt64> {
            return BiologyChaoticModel(a, b, c, r)
        }
    }
}

data class BiologyChaoticModelGenerator(
    val biologyChaoticModel: BiologyChaoticModel<Flt64> = BiologyChaoticModel(),
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
                BiologyChaoticModel(a, b, c, r),
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
