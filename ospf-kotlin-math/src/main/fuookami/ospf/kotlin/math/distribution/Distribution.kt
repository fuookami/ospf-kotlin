/**
 * 概率分布采样接口 / Probability distribution sampling interface
 *
 * 所有分布以「熵源注入」的方式采样：调用方提供一个 [Generator] 作为均匀随机源，
 * 从而让随机性可替换（伪随机 / 混沌序列 / 固定种子可复现）。 / All distributions sample through an injected entropy source: the caller supplies a [Generator] as the uniform random source,
 * which makes randomness replaceable (pseudo-random / chaotic sequence / reproducible with a fixed seed).
*/
package fuookami.ospf.kotlin.math.distribution

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 概率分布接口 / Probability distribution interface
 *
 * 参数校验通过 [validate] 显式返回，采样结果通过 [invoke] 返回 [Ret]，
 * 全程不抛出异常。 / Parameter validation is returned explicitly by [validate] and sampling returns [Ret] through [invoke],
 * without throwing exceptions anywhere.
*/
interface Distribution {

    /**
     * 校验分布参数 / Validate distribution parameters
     *
     * @return 校验结果 / Validation result
    */
    fun validate(): Ret<Unit>

    /**
     * 执行数值采样，假定参数已通过 [validate] 校验 / Perform numeric sampling, assuming parameters passed [validate]
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 采样值 / Sampled value
    */
    fun sample(entropy: Generator<Flt64>): Flt64

    /**
     * 校验后采样 / Validate then sample
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 采样结果 / Sampling result
    */
    operator fun invoke(entropy: Generator<Flt64>): Ret<Flt64> {
        return validate().then { sample(entropy) }
    }
}

/** 规避 0 与 1 端点的极小正数 / Tiny positive value used to avoid the 0 and 1 endpoints */
internal const val UNIT_EPSILON: Double = 1.0e-12

/**
 * 读取 [0, 1) 区间的均匀随机数，越界时裁剪 / Read a uniform random number in [0, 1), clamped when out of range
 *
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @return [0, 1) 内的均匀值 / Uniform value in [0, 1)
*/
internal fun uniformUnit(entropy: Generator<Flt64>): Double {
    val value = entropy()?.value ?: 0.0
    return when {
        value.isNaN() -> 0.0
        value <= 0.0 -> 0.0
        value >= 1.0 -> 1.0 - UNIT_EPSILON
        else -> value
    }
}

/**
 * 校验通过后执行后续计算，失败时透传错误 / Run the follow-up computation after successful validation, propagating failures
 *
 * @param T 后续计算的结果类型 / Result type of the follow-up computation
 * @param block 后续计算闭包 / Follow-up computation closure
 * @return 后续计算结果 / Follow-up computation result
*/
internal inline fun <T> Ret<Unit>.then(block: () -> T): Ret<T> {
    return when (this) {
        is Ok -> ok(block())
        is Failed -> failed(error)
        is Fatal -> Fatal(errors)
    }
}
