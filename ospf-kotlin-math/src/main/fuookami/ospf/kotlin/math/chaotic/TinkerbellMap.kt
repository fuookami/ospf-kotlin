/**
 * 丁克贝尔映射
 * Tinkerbell Map
 *
 * 丁克贝尔映射是一个具有混沌吸引子的二维映射。
 * 该映射以 Peter Pan 故事中的小精灵 Tinker Bell 命名，展现出美丽的混沌轨迹。
 * 常用于混沌理论研究和混沌图形可视化。
 *
 * The Tinkerbell map is a two-dimensional map with a chaotic attractor.
 * This map is named after Tinker Bell from the Peter Pan story, exhibiting beautiful chaotic trajectories.
 * Commonly used for chaos theory research and chaotic graphics visualization.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * 丁克贝尔映射
 * Tinkerbell Map
 *
 * 公式 / Formula:
 * x_{n+1} = x^2 - y^2 + a*x + b*y
 * y_{n+1} = 2*x*y + c*x + d*y
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 */
data class TinkerbellMap<V : FloatingNumber<V>>(
    val a: V,
    val b: V,
    val c: V,
    val d: V
) : Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0]
        val y = p[1]
        val two = x.constants.two
        return Point<Dim2, V>(
            listOf(
                x * x - y * y + a * x + b * y,
                two * x * y + c * x + d * y
            ),
            Dim2
        )
    }

    companion object {
        /**
         * 创建一个默认参数的丁克贝尔映射 / Create a Tinkerbell map with default parameters.
         *
         * @param a 系统参数 a / System parameter a
         * @param b 系统参数 b / System parameter b
         * @param c 系统参数 c / System parameter c
         * @param d 系统参数 d / System parameter d
         * @return 一个新的丁克贝尔映射 / A new Tinkerbell map
         */
        operator fun invoke(
            a: Flt64 = Flt64(0.9),
            b: Flt64 = Flt64(-0.6013),
            c: Flt64 = Flt64(2.0),
            d: Flt64 = Flt64(0.5)
        ): TinkerbellMap<Flt64> {
            return TinkerbellMap(a, b, c, d)
        }
    }
}

/**
 * 丁克贝尔映射生成器
 * Tinkerbell Map Generator
 *
 * @property tinkerbellMap 用于生成的丁克贝尔映射实例 / The Tinkerbell map instance used for generation
 * @property _x 当前状态点 / The current state point
 */
data class TinkerbellMapGenerator(
    val tinkerbellMap: TinkerbellMap<Flt64> = TinkerbellMap(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    companion object {
        /**
         * 创建一个 Tinkerbell 映射生成器 / Create a Tinkerbell map generator.
         *
         * @param a 系统参数 a / System parameter a
         * @param b 系统参数 b / System parameter b
         * @param c 系统参数 c / System parameter c
         * @param d 系统参数 d / System parameter d
         * @param x 初始状态点 / Initial state point
         * @return 一个新的 Tinkerbell 映射生成器 / A new Tinkerbell map generator
         */
        operator fun invoke(
            a: Flt64,
            b: Flt64,
            c: Flt64,
            d: Flt64,
            x: Point<Dim2, Flt64> = point2(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            )
        ): TinkerbellMapGenerator {
            return TinkerbellMapGenerator(
                TinkerbellMap(a, b, c, d),
                x
            )
        }
    }

    /** 当前状态点 / Current state point */
    val x by ::_x

    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy()
        _x = tinkerbellMap(x)
        return x
    }
}
