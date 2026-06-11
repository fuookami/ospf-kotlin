/**
 * PWL 连续半径 solver 变量注册（基于 core UnivariateLinearPiecewiseFunction）。
 * PWL continuous-radius solver variable registration (based on core UnivariateLinearPiecewiseFunction).
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ConservativeRadiusEnvelope
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusSquaredApproximation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 注册 PWL 连续半径 solver 变量及其约束到模型。
 * Register PWL continuous-radius solver variables and their constraints into the model.
 *
 * 使用 core [UnivariateLinearPiecewiseFunction] 的数据结构实现分段线性近似 q ≈ r²：
 * - 注册 radius 变量 r 及其上下界 [rMin, rMax]
 * - 注册 PWL 函数的辅助变量（resultVar、selectorVars）到 model
 * - 注册 Big-M 约束（段选择、函数等式）到 model
 * - 保守 envelope 使用 rMax 保障几何安全
 *
 * @param model 线性元模型 / linear meta model
 * @param pwlVariables PWL solver 变量列表 / PWL solver variable list
 * @param ensureTry 错误处理函数 / error handling function
 */
internal fun registerPWLContinuousRadiusVariables(
    model: LinearMetaModel<InfraNumber>,
    pwlVariables: List<PWLContinuousRadiusSolverVariable>,
    ensureTry: (Try, String) -> Unit
) {
    for (pwlVar in pwlVariables) {
        val variableName = pwlVar.variableName
        val r = pwlVar.radiusVariable
        val envelope = pwlVar.envelope
        val pwlFunction = pwlVar.pwlFunction

        // Register the radius variable into model
        model.add(r)

        // Lower bound: r >= rMin
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(InfraNumber.one, r)), InfraNumber.zero),
                    LinearPolynomial(emptyList(), envelope.rMin),
                    Comparison.GE
                ),
                name = "${variableName}_pwl_r_lb"
            ),
            "register PWL r lower bound for $variableName"
        )

        // Upper bound: r <= rMax
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(InfraNumber.one, r)), InfraNumber.zero),
                    LinearPolynomial(emptyList(), envelope.rMax),
                    Comparison.LE
                ),
                name = "${variableName}_pwl_r_ub"
            ),
            "register PWL r upper bound for $variableName"
        )

        // Register PWL function's helper variables into model
        for (helperVar in pwlFunction.helperVariables) {
            model.add(helperVar as AbstractVariableItem<InfraNumber, *>)
        }

        // Register PWL constraints via the core function's registerConstraints
        // We use the model's tokens for auxiliary registration and then
        // register the Big-M constraints directly through LinearMetaModel.addConstraint
        ensureTry(
            pwlFunction.registerAuxiliaryTokens(model.tokens),
            "register PWL auxiliary tokens for $variableName"
        )

        // Register the PWL function's constraints
        // Since LinearMetaModel is not directly an AbstractLinearMechanismModel,
        // we register constraints by extracting them from the function and
        // adding them individually to the LinearMetaModel
        registerPWLFunctionConstraints(model, pwlFunction, variableName, ensureTry)
    }
}

/**
 * 将 UnivariateLinearPiecewiseFunction 的 Big-M 约束注册到 LinearMetaModel。
 * Register UnivariateLinearPiecewiseFunction's Big-M constraints into LinearMetaModel.
 *
 * This mirrors the logic in UnivariateLinearPiecewiseFunction.registerConstraints
 * but targets LinearMetaModel.addConstraint instead of AbstractLinearMechanismModel.addConstraint.
 */
private fun registerPWLFunctionConstraints(
    model: LinearMetaModel<InfraNumber>,
    pwlFunction: fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction<InfraNumber>,
    variableName: String,
    ensureTry: (Try, String) -> Unit
) {
    val name = pwlFunction.name
    val breakpoints = pwlFunction.breakpoints
    val slopes = pwlFunction.slopes
    val intercepts = pwlFunction.intercepts
    val numSegments = breakpoints.size - 1
    val selectorVars = pwlFunction.selectorVars
    val resultVar = pwlFunction.resultVar
    val x = pwlFunction.x

    val one = InfraNumber.one
    val zero = InfraNumber.zero

    // Compute Big-M from radius bounds and output range
    // For q ≈ r², the maximum possible output value is rMax²
    // The Big-M must be large enough to deactivate constraints when s[i]=0
    val rMinValue = breakpoints.first()
    val rMaxValue = breakpoints.last()
    val rMaxSquared = rMaxValue * rMaxValue
    // Big-M = max(rMax - rMin, rMax²) * safety factor
    val bigMValue = (rMaxValue - rMinValue + rMaxSquared) * infraScalar(2.0)

    // Exactly one segment must be active: sum(s[i]) = 1
    val sumMonos = selectorVars.map { LinearMonomial(one, it) }
    ensureTry(
        model.addConstraint(
            relation = LinearInequality(
                LinearPolynomial(sumMonos, zero),
                LinearPolynomial(emptyList(), one),
                Comparison.EQ
            ),
            name = "${name}_select_one"
        ),
        "register PWL select-one constraint for $variableName"
    )

    for (i in 0 until numSegments) {
        val sVar = selectorVars[i]
        val bpLow = breakpoints[i]
        val bpHigh = breakpoints[i + 1]
        val slope = slopes[i]
        val intercept = intercepts[i]

        val bigMValue = bigMValue  // Use the pre-computed Big-M from radius bounds

        // Lower bound: x >= bpLow - M*(1 - s[i])
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(
                        x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } + LinearMonomial(-bigMValue, sVar),
                        x.constant + bigMValue
                    ),
                    LinearPolynomial(emptyList(), bpLow),
                    Comparison.GE
                ),
                name = "${name}_seg_${i}_lb"
            ),
            "register PWL segment $i lower bound for $variableName"
        )

        // Upper bound: x <= bpHigh + M*(1 - s[i])
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(
                        x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } + LinearMonomial(bigMValue, sVar),
                        x.constant
                    ),
                    LinearPolynomial(emptyList(), bpHigh + bigMValue),
                    Comparison.LE
                ),
                name = "${name}_seg_${i}_ub"
            ),
            "register PWL segment $i upper bound for $variableName"
        )

        // y - slope*x - intercept <= M*(1 - s[i])
        val negSlopeXMonos = x.monomials.map { LinearMonomial(-it.coefficient * slope, it.symbol) }
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(
                        listOf(LinearMonomial(one, resultVar)) + negSlopeXMonos + LinearMonomial(bigMValue, sVar),
                        -intercept
                    ),
                    LinearPolynomial(emptyList(), bigMValue),
                    Comparison.LE
                ),
                name = "${name}_seg_${i}_eq_ub"
            ),
            "register PWL segment $i equality upper for $variableName"
        )

        // y - slope*x - intercept >= -M*(1 - s[i])
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(
                        listOf(LinearMonomial(one, resultVar)) + negSlopeXMonos + LinearMonomial(-bigMValue, sVar),
                        -intercept
                    ),
                    LinearPolynomial(emptyList(), -bigMValue),
                    Comparison.GE
                ),
                name = "${name}_seg_${i}_eq_lb"
            ),
            "register PWL segment $i equality lower for $variableName"
        )
    }
}

/**
 * 从 solver 结果中提取 PWL 变量值。
 * Extract PWL variable values from solver results.
 *
 * @param pwlVariables PWL solver 变量列表 / PWL solver variable list
 * @param resultMap solver 变量结果映射 / solver variable result map
 * @return 提取的半径值列表 / extracted radius values
 */
internal fun extractPWLRadiusValues(
    pwlVariables: List<PWLContinuousRadiusSolverVariable>,
    resultMap: Map<RealVar, InfraNumber>
): List<PWLExtractedRadius> {
    return pwlVariables.map { pwlVar ->
        val rValue = resultMap[pwlVar.radiusVariable] ?: InfraNumber.zero
        val qValue = resultMap[pwlVar.pwlFunction.resultVar] ?: InfraNumber.zero
        val actualRSquared = rValue * rValue
        val pwlError = (qValue - actualRSquared).abs()

        PWLExtractedRadius(
            variableName = pwlVar.variableName,
            solverRadius = rValue,
            solverRadiusSquared = qValue,
            actualRadiusSquared = actualRSquared,
            pwlAbsoluteError = pwlError,
            pwlRelativeError = if (actualRSquared > infraScalar(1e-12)) pwlError / actualRSquared else InfraNumber.zero,
            isWithinEnvelope = pwlVar.envelope.isRadiusValid(rValue),
            envelope = pwlVar.envelope,
            pwlApproximation = pwlVar.pwlApproximation
        )
    }
}

/**
 * PWL 提取的半径结果。
 * PWL extracted radius result.
 *
 * @property variableName 变量名 / variable name
 * @property solverRadius solver 选择的半径 r / solver-selected radius r
 * @property solverRadiusSquared solver 的近似 r² 值 q / solver's approximate r² value q
 * @property actualRadiusSquared 真实 r² 值 / actual r² value
 * @property pwlAbsoluteError PWL 绝对误差 |q - r²| / PWL absolute error
 * @property pwlRelativeError PWL 相对误差 |q - r²| / r² / PWL relative error
 * @property isWithinEnvelope 是否在 envelope 范围内 / whether within envelope range
 * @property envelope 保守半径 envelope / conservative radius envelope
 * @property pwlApproximation PWL 近似函数 / PWL approximation function
 */
internal data class PWLExtractedRadius(
    val variableName: String,
    val solverRadius: InfraNumber,
    val solverRadiusSquared: InfraNumber,
    val actualRadiusSquared: InfraNumber,
    val pwlAbsoluteError: InfraNumber,
    val pwlRelativeError: InfraNumber,
    val isWithinEnvelope: Boolean,
    val envelope: ConservativeRadiusEnvelope,
    val pwlApproximation: PWLRadiusSquaredApproximation
) {
    /**
     * 计算真实圆柱体积（使用 solver 选择的半径）。
     * Compute actual cylinder volume using solver-selected radius.
     */
    fun actualVolume(height: InfraNumber, pi: InfraNumber): InfraNumber {
        return pi * actualRadiusSquared * height
    }

    /**
     * 计算 PWL 近似体积（使用 q ≈ r²）。
     * Compute PWL approximate volume using q ≈ r².
     */
    fun pwlVolume(height: InfraNumber, pi: InfraNumber): InfraNumber {
        return pi * solverRadiusSquared * height
    }

    /**
     * 转为诊断信息。
     * Convert to diagnostic info.
     */
    fun info(): Map<String, String> {
        return mapOf(
            "pwl_radius_${variableName}_r" to solverRadius.toDouble().toString(),
            "pwl_radius_${variableName}_q" to solverRadiusSquared.toDouble().toString(),
            "pwl_radius_${variableName}_r_squared" to actualRadiusSquared.toDouble().toString(),
            "pwl_radius_${variableName}_abs_error" to pwlAbsoluteError.toDouble().toString(),
            "pwl_radius_${variableName}_rel_error" to pwlRelativeError.toDouble().toString(),
            "pwl_radius_${variableName}_within_envelope" to isWithinEnvelope.toString(),
            "pwl_radius_${variableName}_pwl_max_rel_error" to pwlApproximation.maxRelativeError.toDouble().toString()
        )
    }
}
