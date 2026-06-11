/**
 * PWL 半径平方近似函数。
 * PWL radius-squared approximation function.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

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
    val breakpoints: List<InfraNumber>,
    val slopes: List<InfraNumber>,
    val intercepts: List<InfraNumber>,
    val maxRelativeError: InfraNumber,
    val maxAbsoluteError: InfraNumber
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
         * 从半径区间构建 PWL 近似函数。
         * Build PWL approximation from radius interval.
         *
         * @param rMin 半径下界 / radius lower bound
         * @param rMax 半径上界 / radius upper bound
         * @param config PWL 配置 / PWL config
         * @return PWL 近似函数 / PWL approximation
         */
        fun fromRadiusInterval(
            rMin: InfraNumber,
            rMax: InfraNumber,
            config: PWLRadiusApproximationConfig
        ): PWLRadiusSquaredApproximation {
            require(rMin > InfraNumber.zero) { "rMin must be positive" }
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

            val slopes = ArrayList<InfraNumber>()
            val intercepts = ArrayList<InfraNumber>()
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

        private fun validateCustomBreakpoints(
            breakpoints: List<InfraNumber>,
            rMin: InfraNumber,
            rMax: InfraNumber
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

        private fun generateBreakpoints(
            rMin: InfraNumber,
            rMax: InfraNumber,
            config: PWLRadiusApproximationConfig
        ): List<InfraNumber> {
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

        private fun generateUniformBreakpoints(
            rMin: InfraNumber,
            rMax: InfraNumber,
            numSegments: Int
        ): List<InfraNumber> {
            val delta = (rMax - rMin) / infraScalar(numSegments.toDouble())
            return (0..numSegments).map { i -> rMin + delta * infraScalar(i.toDouble()) }
        }

        /**
         * Chebyshev-like 断点分布：在 r 小处（r² 的相对误差更敏感）集中更多断点。
         * Chebyshev-like breakpoint distribution: concentrate more breakpoints near rMin
         * where the relative error of r² is more sensitive.
         */
        private fun generateAdaptiveBreakpoints(
            rMin: InfraNumber,
            rMax: InfraNumber,
            numSegments: Int
        ): List<InfraNumber> {
            val result = ArrayList<InfraNumber>()
            for (i in 0..numSegments) {
                val t = infraScalar(i.toDouble()) / infraScalar(numSegments.toDouble())
                // Chebyshev node: map t to [0, 1] with concentration near 0 (rMin side)
                // theta = pi * t, Chebyshev node = 0.5 * (1 - cos(theta))
                // This places more nodes near t=0 (mapped to rMin) and t=1 (mapped to rMax)
                // For r², relative error is worse at small r, so we bias toward rMin.
                val chebyshev = infraScalar(0.5) * (infraScalar(1.0) - infraScalar(kotlin.math.cos(kotlin.math.PI * t.toDouble())))
                val r = rMin + (rMax - rMin) * chebyshev
                result.add(r)
            }
            return result
        }

        /**
         * 误差驱动断点生成：从均匀分布开始，迭代地在相对误差最大的段添加中点。
         * Error-driven breakpoint generation: start from uniform, iteratively add midpoints
         * to segments with the highest relative error.
         */
        private fun generateErrorDrivenBreakpoints(
            rMin: InfraNumber,
            rMax: InfraNumber,
            relativeTolerance: InfraNumber,
            maxSegments: Int
        ): List<InfraNumber> {
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
                val mid = (breakpoints[worstSegment] + breakpoints[worstSegment + 1]) / infraScalar(2.0)
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
         */
        private fun computeSegmentRelativeErrors(
            breakpoints: List<InfraNumber>,
            slopes: List<InfraNumber>,
            intercepts: List<InfraNumber>
        ): Pair<InfraNumber, List<InfraNumber>> {
            var globalMaxRelError = InfraNumber.zero
            val segmentErrors = ArrayList<InfraNumber>()
            val numSamples = 50
            for (i in 0 until breakpoints.size - 1) {
                val r0 = breakpoints[i]
                val r1 = breakpoints[i + 1]
                val slope = slopes[i]
                val intercept = intercepts[i]
                var segMaxRelError = InfraNumber.zero
                for (j in 0..numSamples) {
                    val t = infraScalar(j.toDouble()) / infraScalar(numSamples.toDouble())
                    val r = r0 + (r1 - r0) * t
                    val actualRSquared = r * r
                    val approxRSquared = slope * r + intercept
                    val absError = (approxRSquared - actualRSquared).abs()
                    if (actualRSquared > infraScalar(1e-12)) {
                        val relError = absError / actualRSquared
                        if (relError > segMaxRelError) segMaxRelError = relError
                    }
                }
                segmentErrors.add(segMaxRelError)
                if (segMaxRelError > globalMaxRelError) globalMaxRelError = segMaxRelError
            }
            return Pair(globalMaxRelError, segmentErrors)
        }

        private fun computeMaxErrors(
            breakpoints: List<InfraNumber>,
            slopes: List<InfraNumber>,
            intercepts: List<InfraNumber>
        ): Pair<InfraNumber, InfraNumber> {
            var maxRelError = InfraNumber.zero
            var maxAbsError = InfraNumber.zero
            val numSamples = 100
            for (i in 0 until breakpoints.size - 1) {
                val r0 = breakpoints[i]
                val r1 = breakpoints[i + 1]
                val slope = slopes[i]
                val intercept = intercepts[i]
                for (j in 0..numSamples) {
                    val t = infraScalar(j.toDouble()) / infraScalar(numSamples.toDouble())
                    val r = r0 + (r1 - r0) * t
                    val actualRSquared = r * r
                    val approxRSquared = slope * r + intercept
                    val absError = (approxRSquared - actualRSquared).abs()
                    if (absError > maxAbsError) maxAbsError = absError
                    if (actualRSquared > infraScalar(1e-12)) {
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
     */
    fun evaluate(r: InfraNumber): InfraNumber {
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
     */
    fun actualError(r: InfraNumber): InfraNumber {
        val q = evaluate(r)
        val actualRSquared = r * r
        return (q - actualRSquared).abs()
    }

    /**
     * 计算实际相对误差 |q - r²| / r²。
     * Compute actual relative error |q - r²| / r².
     */
    fun actualRelativeError(r: InfraNumber): InfraNumber {
        val absError = actualError(r)
        val actualRSquared = r * r
        if (actualRSquared < infraScalar(1e-12)) return InfraNumber.zero
        return absError / actualRSquared
    }
}
