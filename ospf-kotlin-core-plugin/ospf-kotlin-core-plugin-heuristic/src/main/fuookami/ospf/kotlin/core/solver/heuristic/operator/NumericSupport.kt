/**
 * 运算级数值辅助 / Numeric support for operators
 *
 * 这些是各算子与骨架共用的样板：读变量上下界、拆包数值、夹紧并拦截非有限值、按概率触发。
 * 它们不属于任何单一算子族（`update/` 与 `mutation/` 各用一部分，`runner/` 也用），因此与
 * `Ordering` 一起放在算子包根级。 /
 * These are the shared boilerplate of every operator and skeleton: reading variable bounds,
 * unwrapping numeric values, clamping while rejecting non-finite values, and firing by probability.
 * They belong to no single family (`update/` and `mutation/` each use a part, and `runner/` does too),
 * so they sit at the root of the operator package next to `Ordering`.
 *
 * 原名 `GeneSupport` 是遗传算法还持有染色体概念时的遗留；`Chromosome` 删除后，这些函数里已没有
 * 任何与基因相关的内容。 / The old name `GeneSupport` dates from when the GA held chromosomes; after
 * `Chromosome` was removed nothing gene-related is left in these functions.
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 读取变量的下界 / Read the lower bound of a variable
 *
 * @param index 变量下标 / Variable index
 * @return 下界，不可用时返回 null / Lower bound, or null when unavailable
*/
internal fun <V> AbstractCallBackModelInterface<*, *, V>.lowerBoundOf(index: Int): Double?
    where V : RealNumber<V>, V : NumberField<V> {
    return tokens[index].lowerBound?.value?.unwrap()?.toDouble()
}

/**
 * 读取变量的上界 / Read the upper bound of a variable
 *
 * @param index 变量下标 / Variable index
 * @return 上界，不可用时返回 null / Upper bound, or null when unavailable
*/
internal fun <V> AbstractCallBackModelInterface<*, *, V>.upperBoundOf(index: Int): Double?
    where V : RealNumber<V>, V : NumberField<V> {
    return tokens[index].upperBound?.value?.unwrap()?.toDouble()
}

/**
 * 把任意数值解包为 Double / Unwrap any numeric value into a Double
 *
 * @return 双精度值 / Double value
*/
internal fun <V> V.asDouble(): Double where V : RealNumber<V>, V : NumberField<V> {
    return toFlt64().toDouble()
}

/**
 * 采样 [0, 1) 上的均匀随机数 / Sample a uniform random number in [0, 1)
 *
 * @return 均匀随机数 / Uniform random number
*/
internal fun Generator<Flt64>.unit(): Double {
    return (this()?.toDouble() ?: 0.5).coerceIn(0.0, 1.0)
}

/**
 * 从父代列表中取两个父代，只有一个时复用该父代 / Take two parents, reusing the only one when there is just one
 *
 * @param T 父代类型 / Parent type
 * @param parents 父代列表 / Parent list
 * @return 父代对，列表为空时返回 null / Parent pair, or null when the list is empty
*/
internal fun <T> pickTwo(parents: List<T>): Pair<T, T>? {
    return when {
        parents.size >= 2 -> parents[0] to parents[1]
        parents.size == 1 -> parents[0] to parents[0]
        else -> null
    }
}

/**
 * 把值规约到变量上下界之间 / Clamp a value between the variable bounds
 *
 * 非有限值（`NaN`、`±∞`）一律替换为可用边界或 0：这类值一旦进入种群，目标值会溢出为 `∞`，
 * 而 `∞` 之间的比较不满足全序，会让环境选择的排序直接抛 `Comparison method violates its general contract`。 /
 * Non-finite values (`NaN`, `±∞`) are replaced by an available bound or 0: once such a value enters the population the
 * objective overflows to `∞`, and comparisons between `∞` values fail to form a total order, which makes the survivor
 * sort throw `Comparison method violates its general contract`.
 *
 * @param value 待规约值 / Value to clamp
 * @param lower 下界 / Lower bound
 * @param upper 上界 / Upper bound
 * @return 规约后的值 / Clamped value
*/
internal fun clampTo(value: Double, lower: Double?, upper: Double?): Double {
    if (!value.isFinite()) {
        return lower ?: upper ?: 0.0
    }
    var result = value
    if (lower != null && result < lower) {
        result = lower
    }
    if (upper != null && result > upper) {
        result = upper
    }
    return result
}

/**
 * 把 Double 转回解值类型 / Convert a Double back into the solution value type
 *
 * @param V 值类型 / Value type
 * @param converter 值转换器 / Value converter
 * @return 解值 / Solution value
*/
internal fun <V> Double.asValue(converter: IntoValue<V>): V where V : RealNumber<V>, V : NumberField<V> {
    return converter.intoValue(Flt64(this))
}

/**
 * 按概率判断是否触发 / Decide whether an event fires at the given probability
 *
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param probability 触发概率 / Trigger probability
 * @return 是否触发 / Whether it fires
*/
internal fun shouldFire(entropy: Generator<Flt64>, probability: Double): Boolean {
    return probability >= 1.0 || entropy.unit() < probability
}
