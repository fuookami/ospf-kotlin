package fuookami.ospf.kotlin.math.chaotic

import fuookami.ospf.kotlin.utils.math.*

/**
 * Hopfractional 混沌映射，用于生成混沌序列。
 * Hopfractional chaotic map, used for generating chaotic sequences.
 *
 * @property p 控制参数 p，影响混沌行为。
 * @property q 控制参数 q，影响混沌行为。
 * @property k 控制参数 k，影响混沌行为。
 * @property s 控制参数 s，影响混沌行为。
 * @property iterations 迭代次数，决定生成的混沌序列长度。
 */
data class HopfractionalMap(
    val p: Flt64,
    val q: Flt64,
    val k: Flt64,
    val s: Flt64,
    val iterations: UInt64
) {
    companion object {
        /**
         * 使用默认初始值构造 Hopfractional 混沌映射。
         * Construct a Hopfractional chaotic map with the default initial value.
         *
         * @param p 控制参数 p。
         * @param q 控制参数 q。
         * @param k 控制参数 k。
         * @param s 控制参数 s。
         * @param iterations 迭代次数。
         * @param initialValue 初始值，默认为零。
         * @return 新的 [HopfractionalMap] 实例。
         */
        fun apply(
            p: Flt64,
            q: Flt64,
            k: Flt64,
            s: Flt64,
            iterations: UInt64,
            initialValue: Flt64 = Flt64.zero
        ): HopfractionalMap = HopfractionalMap(p, q, k, s, iterations)

        /**
         * 从现有的 [HopfractionalMap] 实例创建副本。
         * Create a copy from an existing [HopfractionalMap] instance.
         *
         * @param p 控制参数 p。
         * @param q 控制参数 q。
         * @param k 控制参数 k。
         * @param s 控制参数 s。
         * @param iterations 迭代次数。
         * @param map 已有的 [HopfractionalMap] 实例。
         * @return 新的 [HopfractionalMap] 实例。
         */
        fun apply(
            p: Flt64,
            q: Flt64,
            k: Flt64,
            s: Flt64,
            iterations: UInt64,
            map: HopfractionalMap
        ): HopfractionalMap = HopfractionalMap(p, q, k, s, iterations)
    }

    /**
     * 执行一次 Hopfractional 映射迭代。
     * Perform one iteration of the Hopfractional map.
     *
     * @param x 输入值。
     * @return 映射后的输出值。
     */
    operator fun invoke(x: Flt64): Flt64 {
        val a = x * p
        val b = (Flt64.one + (k * x).pow(q)) / (Flt64.one + (s * x).pow(q))
        return a / b
    }

    /**
     * 生成混沌序列。
     * Generate the chaotic sequence.
     */
    val chaoticSequence: Sequence<Flt64>
        get() = sequence {
            var x = Flt64.zero
            for (i in 0L until iterations.value) {
                x = invoke(x)
                yield(x)
            }
        }
}
