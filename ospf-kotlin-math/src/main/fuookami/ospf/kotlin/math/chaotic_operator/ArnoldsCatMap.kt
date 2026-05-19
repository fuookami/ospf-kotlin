/**
 * Arnold 猫映射
 * Arnold's Cat Map
 *
 * Arnold 猫映射是由俄罗斯数学家 Vladimir Arnold 于 1960 年提出的经典混沌映射。
 * 该映射在单位正方形上定义，将图像进行拉伸和折叠，展现出典型的混沌特性。
 * 是混沌理论和拓扑学研究中的经典示例，常用于图像加密和混沌研究教学。
 *
 * Arnold's cat map is a classic chaotic map proposed by Russian mathematician Vladimir Arnold in 1960.
 * This map is defined on the unit square, stretching and folding images, exhibiting typical chaotic properties.
 * A classic example in chaos theory and topology research, commonly used for image encryption and chaos research education.
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
 * Arnold 猫映射
 * Arnold's Cat Map
 */
data object ArnoldsCatMap : Extractor<Point<Dim2, Flt64>, Point<Dim2, Flt64>> {
    override operator fun invoke(x: Point<Dim2, Flt64>): Point<Dim2, Flt64> {
        return point2(
            (Flt64.two * x[0] + x[1]) mod Flt64.one,
            (x[0] + x[1]) mod Flt64.one
        )
    }
}

/**
 * Arnold 猫映射生成器
 * Arnold's Cat Map Generator
 */
data class ArnoldsCatMapGenerator(
    val arnoldsCatMap: ArnoldsCatMap = ArnoldsCatMap,
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
        ): ArnoldsCatMapGenerator {
            return ArnoldsCatMapGenerator(ArnoldsCatMap, x)
        }
    }

    val x by ::_x

    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy()
        _x = arnoldsCatMap(x)
        return x
    }
}






