/**
 * 方向采样器 / Direction sampler
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 单位超球面方向采样器 / Unit hypersphere direction sampler
 *
 * 逐维采样标准正态后归一化，得到单位超球面上均匀分布的方向向量；
 * 与 [Levy] 组合即可得到完整的高维莱维飞行步长。 / Samples per-dimension standard normals and normalizes them into a uniformly distributed direction on the unit hypersphere;
 * combining it with [Levy] yields a complete high-dimensional Lévy flight step.
 *
 * @property dimension 维度 / Dimensionality
*/
class DirectionSampler(
    val dimension: Int
) {
    /**
     * 校验采样维度 / Validate the sampling dimension
     *
     * @return 校验结果 / Validation result
    */
    fun validate(): Ret<Unit> {
        return if (dimension > 0) {
            ok(Unit)
        } else {
            Failed(ErrorCode.IllegalArgument, "dimension must be positive / 维度必须为正")
        }
    }

    /**
     * 采样一个单位方向向量，假定维度已校验 / Sample a unit direction vector, assuming the dimension has been validated
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 长度为 [dimension] 的单位向量 / Unit vector whose length is [dimension]
    */
    fun sample(entropy: Generator<Flt64>): List<Flt64> {
        val raw = DoubleArray(dimension) { standardGaussian(entropy) }
        val norm = sqrt(raw.fold(0.0) { acc, value -> acc + value * value })
        return if (norm <= UNIT_EPSILON) {
            List(dimension) { index -> if (index == 0) Flt64.one else Flt64.zero }
        } else {
            raw.map { Flt64(it / norm) }
        }
    }

    /**
     * 校验后采样 / Validate then sample
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 方向向量采样结果 / Direction vector sampling result
    */
    operator fun invoke(entropy: Generator<Flt64>): Ret<List<Flt64>> {
        return validate().then { sample(entropy) }
    }
}
