/**
 * PWL 连续半径 solver 变量注册（基于 core UnivariateLinearPiecewiseFunction）。
 * PWL continuous-radius solver variable registration (based on core UnivariateLinearPiecewiseFunction).
 *
 * 注意：PWLExtractedRadius 和 extractPWLRadiusValues 已迁移到 domain-item-context 的
 * ContinuousRadiusModelComponent.kt。注册逻辑将在阶段三迁移到 ContinuousRadiusModelComponent。
 *
 * Note: PWLExtractedRadius and extractPWLRadiusValues have been moved to
 * ContinuousRadiusModelComponent.kt in domain-item-context.
 * Registration logic will be migrated to ContinuousRadiusModelComponent in Phase 3.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PWLContinuousRadiusSolverVariable
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
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
