/**
 * PWL 半径平方近似函数。
 * PWL radius-squared approximation function.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX

/**
 * PWL 段数推导结果。
 * PWL segment count derivation result.
 *
 * 从误差预算推导所需的段数，不静默放宽精度目标。
 * Derive required segment count from error budget; never silently relax precision target.
 *
 * @property recommendedSegments 推荐段数 / recommended segment count
 * @property achievedMaxRelativeError 达到的最大相对误差 / achieved maximum relative error
 * @property meetsTolerance 是否满足误差容限 / whether tolerance is met
 * @property iterations 推导迭代次数 / derivation iteration count
*/
data class SegmentCountDerivation(
    val recommendedSegments: Int,
    val achievedMaxRelativeError: FltX,
    val meetsTolerance: Boolean,
    val iterations: Int
) {

    /**
     * 转为诊断信息。
     * Convert to diagnostic info.
     *
     * @return diagnostic key-value map / 诊断键值映射
    */
    fun info(): Map<String, String> = mapOf(
        "pwl_derived_segments" to recommendedSegments.toString(),
        "pwl_derived_achieved_max_rel_error" to achievedMaxRelativeError.toDouble().toString(),
        "pwl_derived_meets_tolerance" to meetsTolerance.toString(),
        "pwl_derived_iterations" to iterations.toString()
    )
}

/**
 * PWL 半径平方近似函数：q ≈ r²。
 * PWL radius-squared approximation function: q ≈ r².
 *
 * 对 f(r) = r² 在区间 [rMin, rMax] 上进行分段线性近似。
 * 每段 [r_i, r_{i+1}] 上的近似函数为 q(r) = slope_i * r + intercept_i，
 * 其中 slope_i = r_i + r_{i+1}，intercept_i = -r_i * r_{i+1}（弦线方程）。
 *
 * 对于 LP/MILP solver，q 作为辅助变量，通过 Big-M + 二值选择变量约束（基于 core UnivariateLinearPiecewiseFunction）与 r 关联。
 *
 * @property breakpoints 断点列表 [r_0, r_1, ..., r_n] / breakpoints
 * @property slopes 各段斜率 / slopes per segment
 * @property intercepts 各段截距 / intercepts per segment
 * @property maxRelativeError 最大相对误差 / maximum relative error
 * @property maxAbsoluteError 最大绝对误差 / maximum absolute error
*/
data class PWLRadiusSquaredApproximation(
    val breakpoints: List<FltX>,
    val slopes: List<FltX>,
    val intercepts: List<FltX>,
    val maxRelativeError: FltX,
    val maxAbsoluteError: FltX
) {
    init {
        require(breakpoints.size >= 2) { "Need at least 2 breakpoints" }
        require(slopes.size == breakpoints.size - 1) { "slopes size must be breakpoints.size - 1" }
        require(intercepts.size == breakpoints.size - 1) { "intercepts size must be breakpoints.size - 1" }
    }

    /** 分段数 / number of segments */
    val numSegments: Int get() = breakpoints.size - 1

    companion object {
        /**
         * 从误差预算推导所需段数。
         * Derive required segment count from error budget.
         *
         * 从 1 段开始，逐步加倍直到满足 tolerance 或达到 maxSegments。
         * 不静默放宽精度目标：若 maxSegments 内无法满足，返回 meetsTolerance=false。
         *
         * Start from 1 segment, double until tolerance is met or maxSegments reached.
         * Never silently relax precision: if tolerance cannot be met within maxSegments,
         * returns meetsTolerance=false with diagnostics.
         *
         * @param rMin 半径下界 / radius lower bound
         * @param rMax 半径上界 / radius upper bound
         * @param relativeErrorTolerance 目标相对误差容限 / target relative error tolerance
         * @param maxSegments 最大段数 / maximum segment count
         * @return 段数推导结果 / segment count derivation result
        */
        fun deriveSegmentCount(
            rMin: FltX,
            rMax: FltX,
            relativeErrorTolerance: FltX,
            maxSegments: Int = 64
        ): SegmentCountDerivation {
            require(rMin > FltX.zero) { "rMin must be positive" }
            require(rMax > rMin) { "rMax must be greater than rMin" }
            require(maxSegments >= 1) { "maxSegments must be at least 1" }

            var segments = 1
            var iterations = 0
            while (segments <= maxSegments) {
                val pwl = fromRadiusInterval(
                    rMin = rMin,
                    rMax = rMax,
                    config = PWLRadiusApproximationConfig(
                        maxSegments = segments,
                        breakpointStrategy = PWLBreakpointStrategy.Uniform
                    )
                )
                iterations++
                if (pwl.maxRelativeError <= relativeErrorTolerance) {
                    return SegmentCountDerivation(
                        recommendedSegments = segments,
                        achievedMaxRelativeError = pwl.maxRelativeError,
                        meetsTolerance = true,
                        iterations = iterations
                    )
                }
                if (segments >= maxSegments) break
                segments = minOf(segments * 2, maxSegments)
            }
            // Could not meet tolerance within maxSegments — report honestly
            val finalPwl = fromRadiusInterval(
                rMin = rMin,
                rMax = rMax,
                config = PWLRadiusApproximationConfig(
                    maxSegments = maxSegments,
                    breakpointStrategy = PWLBreakpointStrategy.Uniform
                )
            )
            return SegmentCountDerivation(
                recommendedSegments = maxSegments,
                achievedMaxRelativeError = finalPwl.maxRelativeError,
                meetsTolerance = false,
                iterations = iterations
            )
        }

        /**
         * 从半径区间构建 PWL 近似函数。
         * Build PWL approximation from radius interval.
         *
         * @param rMin 半径下界 / radius lower bound
         * @param rMax 半径上界 / radius upper bound
         * @param config PWL 配置 / PWL config
         * @return PWL 近似函数 / PWL approximation
        */
        fun fromRadiusInterval(
            rMin: FltX,
            rMax: FltX,
            config: PWLRadiusApproximationConfig
        ): PWLRadiusSquaredApproximation {
            require(rMin > FltX.zero) { "rMin must be positive" }
            require(rMax > rMin) { "rMax must be greater than rMin" }

            val breakpoints = when {
                config.customBreakpoints != null && config.customBreakpoints.size >= 2 -> {
                    validateCustomBreakpoints(config.customBreakpoints, rMin, rMax)
                    config.customBreakpoints
                }
                else -> {
                    generateBreakpoints(rMin, rMax, config)
                }
            }

            val slopes = ArrayList<FltX>()
            val intercepts = ArrayList<FltX>()
            for (i in 0 until breakpoints.size - 1) {
                val r0 = breakpoints[i]
                val r1 = breakpoints[i + 1]
                // chord slope: (r1^2 - r0^2) / (r1 - r0) = r0 + r1
                val slope = r0 + r1
                // chord intercept: r0^2 - slope * r0 = r0^2 - r0*(r0 + r1) = -r0*r1
                val intercept = -(r0 * r1)
                slopes.add(slope)
                intercepts.add(intercept)
            }

            val errors = computeMaxErrors(breakpoints, slopes, intercepts)
            return PWLRadiusSquaredApproximation(
                breakpoints = breakpoints,
                slopes = slopes,
                intercepts = intercepts,
                maxRelativeError = errors.first,
                maxAbsoluteError = errors.second
            )
        }

/**
 * Validates customBreakpoints.
 * 验证CustomBreakpoints。
 * @param breakpoints custom breakpoint list to validate / 待验证的自定义断点列表
 * @param rMin radius lower bound / 半径下界
 * @param rMax radius upper bound / 半径上界
*/
        private fun validateCustomBreakpoints(
            breakpoints: List<FltX>,
            rMin: FltX,
            rMax: FltX
        ) {
            require(breakpoints.first() <= rMin) {
                "Custom breakpoints must start at or below rMin"
            }
            require(breakpoints.last() >= rMax) {
                "Custom breakpoints must end at or above rMax"
            }
            for (i in 0 until breakpoints.size - 1) {
                require(breakpoints[i] <= breakpoints[i + 1]) {
                    "Custom breakpoints must be non-decreasing"
                }
            }
        }

        /**
         * 根据策略生成断点。
         * Generate breakpoints based on the chosen strategy.
         *
         * @param rMin radius lower bound / 半径下界
         * @param rMax radius upper bound / 半径上界
         * @param config PWL approximation configuration / PWL 近似配置
         * @return generated breakpoint list / 生成的断点列表
        */
        private fun generateBreakpoints(
            rMin: FltX,
            rMax: FltX,
            config: PWLRadiusApproximationConfig
        ): List<FltX> {
            return when (config.breakpointStrategy) {
                PWLBreakpointStrategy.Uniform -> {
                    generateUniformBreakpoints(rMin, rMax, config.maxSegments)
                }
                PWLBreakpointStrategy.Adaptive -> {
                    generateAdaptiveBreakpoints(rMin, rMax, config.maxSegments)
                }
                PWLBreakpointStrategy.ErrorDriven -> {
                    generateErrorDrivenBreakpoints(rMin, rMax, config.relativeErrorTolerance, config.maxSegments)
                }
            }
        }

        /**
         * 生成均匀分布断点。
         * Generate uniformly distributed breakpoints.
         *
         * @param rMin radius lower bound / 半径下界
         * @param rMax radius upper bound / 半径上界
         * @param numSegments number of segments / 分段数
         * @return uniformly distributed breakpoint list / 均匀分布的断点列表
        */
        private fun generateUniformBreakpoints(
            rMin: FltX,
            rMax: FltX,
            numSegments: Int
        ): List<FltX> {
            val delta = (rMax - rMin) / FltX(numSegments.toDouble())
            return (0..numSegments).map { i -> rMin + delta * FltX(i.toDouble()) }
        }

        /**
         * Chebyshev-like 断点分布：在 r 小处（r² 的相对误差更敏感）集中更多断点。
         * Chebyshev-like breakpoint distribution: concentrate more breakpoints near rMin
         * where the relative error of r² is more sensitive.
         *
         * @param rMin radius lower bound / 半径下界
         * @param rMax radius upper bound / 半径上界
         * @param numSegments number of segments / 分段数
         * @return Chebyshev-like distributed breakpoint list / Chebyshev 类分布的断点列表
        */
        private fun generateAdaptiveBreakpoints(
            rMin: FltX,
            rMax: FltX,
            numSegments: Int
        ): List<FltX> {
            val result = ArrayList<FltX>()
            for (i in 0..numSegments) {
                val t = FltX(i.toDouble()) / FltX(numSegments.toDouble())
                // Chebyshev node: map t to [0, 1] with concentration near 0 (rMin side)
                // theta = pi * t, Chebyshev node = 0.5 * (1 - cos(theta))
                // This places more nodes near t=0 (mapped to rMin) and t=1 (mapped to rMax)
                // For r², relative error is worse at small r, so we bias toward rMin.
                val chebyshev = FltX(0.5) * (FltX(1.0) - FltX(kotlin.math.cos(kotlin.math.PI * t.toDouble())))
                val r = rMin + (rMax - rMin) * chebyshev
                result.add(r)
            }
            return result
        }

        /**
         * 误差驱动断点生成：从均匀分布开始，迭代地在相对误差最大的段添加中点。
         * Error-driven breakpoint generation: start from uniform, iteratively add midpoints
         * to segments with the highest relative error.
         *
         * @param rMin radius lower bound / 半径下界
         * @param rMax radius upper bound / 半径上界
         * @param relativeTolerance target relative error tolerance / 目标相对误差容限
         * @param maxSegments maximum segment count / 最大段数
         * @return error-driven breakpoint list / 误差驱动生成的断点列表
        */
        private fun generateErrorDrivenBreakpoints(
            rMin: FltX,
            rMax: FltX,
            relativeTolerance: FltX,
            maxSegments: Int
        ): List<FltX> {
            var breakpoints = generateUniformBreakpoints(rMin, rMax, maxSegments)
            val maxIterations = 5
            var iterations = 0
            while (iterations < maxIterations) {
                val slopes = (0 until breakpoints.size - 1).map { i ->
                    breakpoints[i] + breakpoints[i + 1]
                }
                val intercepts = (0 until breakpoints.size - 1).map { i ->
                    -(breakpoints[i] * breakpoints[i + 1])
                }
                val (maxRelError, segmentErrors) = computeSegmentRelativeErrors(breakpoints, slopes, intercepts)
                if (maxRelError <= relativeTolerance) break

                // Find the segment with highest relative error and bisect it
                val worstSegment = segmentErrors.indices.maxByOrNull { segmentErrors[it].toDouble() } ?: break
                val mid = (breakpoints[worstSegment] + breakpoints[worstSegment + 1]) / FltX(2.0)
                val newBreakpoints = breakpoints.toMutableList()
                newBreakpoints.add(worstSegment + 1, mid)
                if (newBreakpoints.size - 1 > maxSegments * 2) break // Safety limit
                breakpoints = newBreakpoints
                iterations++
            }
            return breakpoints
        }

        /**
         * 计算各段最大相对误差。
         * Compute maximum relative errors per segment.
         *
         * @param breakpoints breakpoint list defining the PWL segments / 定义 PWL 分段的断点列表
         * @param slopes per-segment chord slopes / 各段弦斜率
         * @param intercepts per-segment chord intercepts / 各段弦截距
         * @return global maximum relative error and per-segment relative error list / 全局最大相对误差与各段相对误差列表
        */
        private fun computeSegmentRelativeErrors(
            breakpoints: List<FltX>,
            slopes: List<FltX>,
            intercepts: List<FltX>
        ): Pair<FltX, List<FltX>> {
            var globalMaxRelError = FltX.zero
            val segmentErrors = ArrayList<FltX>()
            val numSamples = 50
            for (i in 0 until breakpoints.size - 1) {
                val r0 = breakpoints[i]
                val r1 = breakpoints[i + 1]
                val slope = slopes[i]
                val intercept = intercepts[i]
                var segMaxRelError = FltX.zero
                for (j in 0..numSamples) {
                    val t = FltX(j.toDouble()) / FltX(numSamples.toDouble())
                    val r = r0 + (r1 - r0) * t
                    val actualRSquared = r * r
                    val approxRSquared = slope * r + intercept
                    val absError = (approxRSquared - actualRSquared).abs()
                    if (actualRSquared > FltX(1e-12)) {
                        val relError = absError / actualRSquared
                        if (relError > segMaxRelError) segMaxRelError = relError
                    }
                }
                segmentErrors.add(segMaxRelError)
                if (segMaxRelError > globalMaxRelError) globalMaxRelError = segMaxRelError
            }
            return Pair(globalMaxRelError, segmentErrors)
        }

        /**
         * 计算所有段的最大相对误差和绝对误差。
         * Compute maximum relative and absolute errors across all segments.
         *
         * @param breakpoints breakpoint list defining the PWL segments / 定义 PWL 分段的断点列表
         * @param slopes per-segment chord slopes / 各段弦斜率
         * @param intercepts per-segment chord intercepts / 各段弦截距
         * @return maximum relative error and maximum absolute error across all segments / 所有段的最大相对误差和最大绝对误差
        */
        private fun computeMaxErrors(
            breakpoints: List<FltX>,
            slopes: List<FltX>,
            intercepts: List<FltX>
        ): Pair<FltX, FltX> {
            var maxRelError = FltX.zero
            var maxAbsError = FltX.zero
            val numSamples = 100
            for (i in 0 until breakpoints.size - 1) {
                val r0 = breakpoints[i]
                val r1 = breakpoints[i + 1]
                val slope = slopes[i]
                val intercept = intercepts[i]
                for (j in 0..numSamples) {
                    val t = FltX(j.toDouble()) / FltX(numSamples.toDouble())
                    val r = r0 + (r1 - r0) * t
                    val actualRSquared = r * r
                    val approxRSquared = slope * r + intercept
                    val absError = (approxRSquared - actualRSquared).abs()
                    if (absError > maxAbsError) maxAbsError = absError
                    if (actualRSquared > FltX(1e-12)) {
                        val relError = absError / actualRSquared
                        if (relError > maxRelError) maxRelError = relError
                    }
                }
            }
            return Pair(maxRelError, maxAbsError)
        }
    }

    /**
     * 分段线性求值 q ≈ r²。
     * Piecewise linear evaluation q ≈ r².
     *
     * 对 r 在 [breakpoints.first, breakpoints.last] 范围内使用分段线性近似；
     * 超出范围时使用最近段外推。
     *
     * @param r 半径值 / radius value
     * @return 近似 r² 的结果 / approximated r² value
    */
    fun evaluate(r: FltX): FltX {
        // Binary search for the correct segment
        val bp = breakpoints
        if (r <= bp.first()) {
            return slopes.first() * r + intercepts.first()
        }
        if (r >= bp.last()) {
            return slopes.last() * r + intercepts.last()
        }
        // Linear search is fine for small n (max ~16 segments)
        for (i in 0 until numSegments) {
            if (r <= bp[i + 1]) {
                return slopes[i] * r + intercepts[i]
            }
        }
        // Should not reach here
        return slopes.last() * r + intercepts.last()
    }

    /**
     * 计算实际误差 |q - r²|。
     * Compute actual error |q - r²|.
     *
     * @param r 半径值 / radius value
     * @return 实际误差 / actual error
    */
    fun actualError(r: FltX): FltX {
        val q = evaluate(r)
        val actualRSquared = r * r
        return (q - actualRSquared).abs()
    }

    /**
     * 计算实际相对误差 |q - r²| / r²。
     * Compute actual relative error |q - r²| / r².
     *
     * @param r 半径值 / radius value
     * @return 实际相对误差 / actual relative error
    */
    fun actualRelativeError(r: FltX): FltX {
        val absError = actualError(r)
        val actualRSquared = r * r
        if (actualRSquared < FltX(1e-12)) return FltX.zero
        return absError / actualRSquared
    }
}
