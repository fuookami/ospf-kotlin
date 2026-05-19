/**
 * 面包师映射
 * Baker's Map
 *
 * 面包师映射是一个经典的二维混沌映射，其操作类似于面包师揉面团的过程。
 * 该映射将单位正方形拉伸、压缩并折叠，展现出典型的混沌拉伸-折叠特性。
 * 是混沌理论中的基础模型，常用于混沌动力学研究和教学演示。
 *
 * Baker's map is a classic two-dimensional chaotic map, whose operation resembles a baker kneading dough.
 * This map stretches, compresses, and folds the unit square, exhibiting typical chaotic stretching-folding properties.
 * A fundamental model in chaos theory, commonly used for chaos dynamics research and educational demonstrations.
 */
package fuookami.ospf.kotlin.math.chaotic_operator

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.nextFlt64
import kotlin.random.Random
import fuookami.ospf.kotlin.math.geometry.point2

/**
 * 面包师映射
 * Baker's Map
 */
data object BakersMap : Extractor<Point<Dim2, Flt64>, Point<Dim2, Flt64>> {
    override operator fun invoke(x: Point<Dim2, Flt64>): Point<Dim2, Flt64> {
        return point2(
            (Flt64.two * x[0]) mod Flt64.one,
            ((Flt64.two * x[0]).floor() + x[1]) / Flt64.two
        )
    }
}

/**
 * 面包师映射生成器
 * Baker's Map Generator
 */
data class BakersMapGenerator(
    val bakersMap: BakersMap = BakersMap,
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    companion object {
        operator fun invoke(
            x: Point<Dim2, Flt64> = point2(
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
                Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
            )
        ): BakersMapGenerator {
            return BakersMapGenerator(BakersMap, x)
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy()
        _x = bakersMap(x)
        return x
    }
}






