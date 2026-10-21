/**
 * 初始化算子 / Initialization operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 初始化算子接口，决定初始解如何产生。 / Initialization operator interface deciding how initial solutions are produced.
*/
interface Initializer {

    /**
     * 生成初始解 / Generate initial solutions
     *
     * @param V 值类型 / Value type
     * @param iteration 当前迭代 / Current iteration
     * @param model 回调模型接口 / Callback model interface
     * @param amount 需要的解数量 / Required solution amount
     * @param converter 值转换器 / Value converter
     * @return 初始解列表 / Initial solution list
    */
    operator fun <V> invoke(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, *, V>,
        amount: UInt64,
        converter: IntoValue<V>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>
}

/** 直接使用模型提供的初始解 / Uses the initial solutions provided by the model */
data object ModelInitializer : Initializer {
    override operator fun <V> invoke(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, *, V>,
        amount: UInt64,
        converter: IntoValue<V>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        return model.initialSolutions(amount)
    }
}

/**
 * 均匀随机初始化，在变量上下界内均匀采样。 / Uniform random initialization sampling within variable bounds.
 *
 * @property entropy 均匀分布熵源 / Uniform entropy source
*/
class RandomUniformInitializer(
    private val entropy: Generator<Flt64>
) : Initializer {
    override operator fun <V> invoke(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, *, V>,
        amount: UInt64,
        converter: IntoValue<V>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        val tokenAmount = model.tokens.tokens.size
        // 只有全部变量都有有限上下界时才能按区间均匀采样；否则回退到模型自带的初始解生成器。
        // Uniform sampling over the interval is only possible when every variable has finite bounds; otherwise fall back to the model's own initial solution generator.
        val bounded = (0 until tokenAmount).all { index ->
            val token = model.tokens[index]
            val lowerBound = token.lowerBound?.value?.unwrap()?.toDouble()
            val upperBound = token.upperBound?.value?.unwrap()?.toDouble()
            lowerBound != null && upperBound != null && lowerBound.isFinite() && upperBound.isFinite()
        }
        if (!bounded) {
            return ModelInitializer(
                iteration = iteration,
                model = model,
                amount = amount,
                converter = converter
            )
        }

        return List(amount.toInt()) {
            List(tokenAmount) { index ->
                val token = model.tokens[index]
                val lowerBound = token.lowerBound!!.value.unwrap().toDouble()
                val upperBound = token.upperBound!!.value.unwrap().toDouble()
                val unit = (entropy()?.toDouble() ?: 0.0).coerceIn(0.0, 1.0)
                converter.intoValue(Flt64(lowerBound + unit * (upperBound - lowerBound)))
            }
        }
    }
}

/**
 * 反对立学习初始化，在基础初始化之上补充镜像解。 / Opposition-based learning initialization augmenting a base initializer with mirrored solutions.
 *
 * @property base 基础初始化算子 / Base initializer
*/
class OppositionBasedInitializer(
    private val base: Initializer = ModelInitializer
) : Initializer {
    override operator fun <V> invoke(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, *, V>,
        amount: UInt64,
        converter: IntoValue<V>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        val originals = base(
            iteration = iteration,
            model = model,
            amount = amount,
            converter = converter
        )
        val mirrored = originals.map { solution ->
            solution.mapIndexed { index, value ->
                val token = model.tokens[index]
                val lowerBound = token.lowerBound!!.value.unwrap().toDouble()
                val upperBound = token.upperBound!!.value.unwrap().toDouble()
                converter.intoValue(Flt64(lowerBound + upperBound - converter.fromValue(value).toDouble()))
            }
        }
        return originals + mirrored
    }
}
