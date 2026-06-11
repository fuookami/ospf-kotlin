package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ContinuousCylinderRadiusSolverPrototype
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ConservativeRadiusEnvelope
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusApproximationConfig
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusSquaredApproximation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * 连续半径 solver 变量注册计划。
 * Continuous-radius solver variable registration plan.
 *
 * @property variableNames 变量名列表 / variable names
 * @property boundDescriptions 变量上下界描述 / variable bound descriptions
 * @property selectedRadiusDescriptions 已选择半径描述 / selected-radius descriptions
 * @property gapDescriptions 缺口描述 / gap descriptions
 * @property productionReadyVariables 已具备生产回写闭环的变量 / variables with production selection closure
 * @property modelRegistrationBlockedVariables 暂未注册进 solver model 的变量 / variables not yet registered into solver model
 * @property registeredVariables 已注册进 solver model 的变量（native path）/ variables registered into solver model (native path)
 * @property pwlRegisteredVariables 已通过 PWL 路径注册进 solver model 的变量 / variables registered via PWL path
 */
internal data class ContinuousRadiusSolverVariableRegistrationPlan(
    val variableNames: List<String>,
    val boundDescriptions: List<String>,
    val selectedRadiusDescriptions: List<String>,
    val gapDescriptions: List<String>,
    val productionReadyVariables: List<String>,
    val modelRegistrationBlockedVariables: List<String>,
    val registeredVariables: List<ContinuousRadiusSolverVariable> = emptyList(),
    val pwlRegisteredVariables: List<PWLContinuousRadiusSolverVariable> = emptyList()
) {
    /**
     * 转为诊断信息。
     * Convert to diagnostic info.
     *
     * @return 诊断信息 / diagnostic info
     */
    fun info(): Map<String, String> {
        val blockedReason = if (modelRegistrationBlockedVariables.isEmpty()) {
            ""
        } else {
            "solver numeric variable bound conversion requires explicit constants or core token-bound support; footprint/volume/support/renderer remain unbound"
        }
        val pwlInfo = if (pwlRegisteredVariables.isEmpty()) {
            ""
        } else {
            pwlRegisteredVariables.joinToString("|") { "${it.prototype.variableName}:segments=${it.pwlApproximation.numSegments},maxRelErr=${it.pwlApproximation.maxRelativeError.toDouble().formatScientific()}" }
        }
        // Mutual exclusion classification: each variable belongs to exactly one path
        val nativeCount = registeredVariables.size
        val pwlCount = pwlRegisteredVariables.size
        val productionReadyCount = productionReadyVariables.size
        val blockedCount = modelRegistrationBlockedVariables.size
        val mutualExclusionSummary = "native=$nativeCount, pwl=$pwlCount, productionReady=$productionReadyCount, blocked=$blockedCount"
        return mapOf(
            "continuous_radius_solver_registration_plan_count" to variableNames.size.toString(),
            "continuous_radius_solver_registration_plan_variables" to variableNames.joinToString("|"),
            "continuous_radius_solver_registration_plan_bounds" to boundDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_selected_radii" to selectedRadiusDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_gap_variables" to gapDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_production_ready_variables" to productionReadyVariables.joinToString("|"),
            "continuous_radius_solver_model_registration_blocked_variables" to modelRegistrationBlockedVariables.joinToString("|"),
            "continuous_radius_solver_model_registration_blocked_reason" to blockedReason,
            "continuous_radius_solver_pwl_registered_variables" to pwlInfo,
            "continuous_radius_solver_mutual_exclusion_summary" to mutualExclusionSummary
        )
    }

    private fun Double.formatScientific(): String {
        return if (this == 0.0) "0" else String.format("%.4e", this)
    }
}

/**
 * 生成连续半径 solver 变量注册计划。
 * Build the continuous-radius solver variable registration plan.
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param solverVariables solver 变量列表（native path）/ solver variable list (native path)
 * @param pwlSolverVariables PWL solver 变量列表 / PWL solver variable list
 * @return 注册计划 / registration plan
 */
internal fun continuousRadiusSolverVariableRegistrationPlan(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    solverVariables: List<ContinuousRadiusSolverVariable> = emptyList(),
    pwlSolverVariables: List<PWLContinuousRadiusSolverVariable> = emptyList()
): ContinuousRadiusSolverVariableRegistrationPlan {
    val variableNames = ArrayList<String>()
    val boundDescriptions = ArrayList<String>()
    val selectedRadiusDescriptions = ArrayList<String>()
    val gapDescriptions = ArrayList<String>()
    val productionReadyVariables = ArrayList<String>()
    val modelRegistrationBlockedVariables = ArrayList<String>()
    val registeredVariables = ArrayList<ContinuousRadiusSolverVariable>()
    val pwlRegisteredVariables = ArrayList<PWLContinuousRadiusSolverVariable>()
    for (prototype in prototypes) {
        variableNames.add(prototype.variableName)
        boundDescriptions.add(prototype.registrationBoundDescription())
        prototype.registrationSelectedRadiusDescription()?.let { description ->
            selectedRadiusDescriptions.add(description)
        }
        if (prototype.gaps.isNotEmpty()) {
            gapDescriptions.add("${prototype.variableName}:${prototype.gaps.joinToString("+") { it.name }}")
        }
        if (prototype.isProductionReady) {
            productionReadyVariables.add(prototype.variableName)
        }
        when {
            prototype.isSolverRegisterable -> {
                val solverVar = solverVariables.firstOrNull { it.prototype.variableName == prototype.variableName }
                if (solverVar != null) {
                    registeredVariables.add(solverVar)
                }
            }
            prototype.isPWLRegisterable -> {
                val pwlVar = pwlSolverVariables.firstOrNull { it.prototype.variableName == prototype.variableName }
                if (pwlVar != null) {
                    pwlRegisteredVariables.add(pwlVar)
                }
            }
            else -> {
                modelRegistrationBlockedVariables.add(prototype.variableName)
            }
        }
    }
    return ContinuousRadiusSolverVariableRegistrationPlan(
        variableNames = variableNames,
        boundDescriptions = boundDescriptions,
        selectedRadiusDescriptions = selectedRadiusDescriptions,
        gapDescriptions = gapDescriptions,
        productionReadyVariables = productionReadyVariables,
        modelRegistrationBlockedVariables = modelRegistrationBlockedVariables,
        registeredVariables = registeredVariables,
        pwlRegisteredVariables = pwlRegisteredVariables
    )
}

/**
 * 连续半径 solver 变量（持有真实 solver 变量和对应的原型）。
 * Continuous-radius solver variable (holds a real solver variable and its prototype).
 *
 * @property prototype 连续半径 solver 变量原型 / continuous-radius solver variable prototype
 * @property variable 真实 solver 连续变量 / real solver continuous variable
 */
internal data class ContinuousRadiusSolverVariable(
    val prototype: ContinuousCylinderRadiusSolverPrototype,
    val variable: RealVar
)

/**
 * PWL 连续半径 solver 变量（持有 core UnivariateLinearPiecewiseFunction、PWL 近似函数和保守 envelope）。
 * PWL continuous-radius solver variable (holds a core UnivariateLinearPiecewiseFunction, PWL approximation, and conservative envelope).
 *
 * 使用 core 提供的 [UnivariateLinearPiecewiseFunction] 实现分段线性近似 q ≈ r²：
 * - r: solver 连续变量，代表半径
 * - q = f(r): UnivariateLinearPiecewiseFunction 自动创建结果变量、二值选择变量和 Big-M 约束
 * - envelope: 使用 rMax 保守建模所有几何计算
 *
 * @property prototype 连续半径 solver 变量原型 / continuous-radius solver variable prototype
 * @property radiusVariable 半径连续变量 r / radius continuous variable r
 * @property pwlFunction core 单变量分段线性函数 q ≈ r² / core univariate piecewise linear function q ≈ r²
 * @property pwlApproximation PWL 近似函数（断点/误差）/ PWL approximation function (breakpoints/errors)
 * @property envelope 保守半径 envelope / conservative radius envelope
 * @property config PWL 配置 / PWL config
 */
internal data class PWLContinuousRadiusSolverVariable(
    val prototype: ContinuousCylinderRadiusSolverPrototype,
    val radiusVariable: RealVar,
    val pwlFunction: UnivariateLinearPiecewiseFunction<fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber>,
    val pwlApproximation: PWLRadiusSquaredApproximation,
    val envelope: ConservativeRadiusEnvelope,
    val config: PWLRadiusApproximationConfig
) {
    /** 变量名 / variable name */
    val variableName: String get() = prototype.variableName

    /** PWL 结果变量 q ≈ r² / PWL result variable q ≈ r² */
    val radiusSquaredVariable get() = pwlFunction.resultVar

    /**
     * 生成 PWL 约束描述（用于诊断和调试）。
     * Generate PWL constraint descriptions (for diagnostics and debugging).
     */
    fun constraintDescriptions(): List<String> {
        val descriptions = ArrayList<String>()
        descriptions.add("PWL($variableName): q = f(r) via UnivariateLinearPiecewiseFunction, r in [${envelope.rMin.toDouble()}, ${envelope.rMax.toDouble()}]")
        descriptions.add("PWL($variableName): numSegments=${pwlApproximation.numSegments}, Big-M + binary selector")
        if (config.enableDebugInfo) {
            descriptions.add("PWL($variableName): maxRelError = ${pwlApproximation.maxRelativeError.toDouble()}")
            descriptions.add("PWL($variableName): maxAbsError = ${pwlApproximation.maxAbsoluteError.toDouble()}")
            for (i in 0 until pwlApproximation.numSegments) {
                descriptions.add("PWL($variableName): seg[$i] slope=${pwlApproximation.slopes[i].toDouble()}, intercept=${pwlApproximation.intercepts[i].toDouble()}")
            }
        }
        return descriptions
    }
}

/**
 * 从原型列表创建 solver 变量列表（native path）。
 * Create solver variable list from prototype list (native path).
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @return solver 变量列表 / solver variable list
 */
internal fun continuousRadiusSolverVariables(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>
): List<ContinuousRadiusSolverVariable> {
    return prototypes
        .filter { it.isSolverRegisterable }
        .map { prototype ->
            ContinuousRadiusSolverVariable(
                prototype = prototype,
                variable = RealVar(prototype.variableName)
            )
        }
}

/**
 * 从原型列表创建 PWL solver 变量列表。
 * Create PWL solver variable list from prototype list.
 *
 * 使用 core [UnivariateLinearPiecewiseFunction] 实现分段线性近似 q ≈ r²。
 * 仅处理 isPWLRegisterable 且非 isSolverRegisterable 的原型。
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param config PWL 配置 / PWL config
 * @return PWL solver 变量列表 / PWL solver variable list
 */
internal fun pwlContinuousRadiusSolverVariables(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    config: PWLRadiusApproximationConfig = PWLRadiusApproximationConfig()
): List<PWLContinuousRadiusSolverVariable> {
    return prototypes
        .filter { it.isPWLRegisterable && !it.isSolverRegisterable }
        .mapNotNull { prototype ->
            val rMin = prototype.radiusLowerBound ?: return@mapNotNull null
            val rMax = prototype.radiusUpperBound ?: return@mapNotNull null
            val rMinValue = rMin.value
            val rMaxValue = rMax.value

            val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = rMinValue,
                rMax = rMaxValue,
                config = config
            )

            val envelope = ConservativeRadiusEnvelope(
                rMin = rMinValue,
                rMax = rMaxValue
            )

            val variableName = prototype.variableName
            val radiusVariable = RealVar("${variableName}_r")

            // Build core UnivariateLinearPiecewiseFunction: q ≈ r²
            val x = LinearPolynomial(
                listOf(LinearMonomial(infraScalar(1.0), radiusVariable)),
                infraScalar(0.0)
            )
            val pwlFunction = UnivariateLinearPiecewiseFunction(
                x = x,
                breakpoints = pwlApproximation.breakpoints,
                slopes = pwlApproximation.slopes,
                intercepts = pwlApproximation.intercepts,
                converter = IntoValue.fromConverter(FltX),
                name = "${variableName}_pwl_r_squared"
            )

            PWLContinuousRadiusSolverVariable(
                prototype = prototype,
                radiusVariable = radiusVariable,
                pwlFunction = pwlFunction,
                pwlApproximation = pwlApproximation,
                envelope = envelope,
                config = config
            )
        }
}

private fun ContinuousCylinderRadiusSolverPrototype.registrationBoundDescription(): String {
    val selectedRadius = initialRadius
    if (selectedRadius != null && gaps.isEmpty()) {
        val selectedText = "${selectedRadius.value.toDouble()} ${selectedRadius.unit.symbol}"
        return "$variableName:$selectedText..$selectedText"
    }
    val lowerText = radiusLowerBound
        ?.let { "${it.value.toDouble()} ${it.unit.symbol}" }
        ?: "-inf"
    val upperText = radiusUpperBound
        ?.let { "${it.value.toDouble()} ${it.unit.symbol}" }
        ?: "+inf"
    return "$variableName:$lowerText..$upperText"
}

private fun ContinuousCylinderRadiusSolverPrototype.registrationSelectedRadiusDescription(): String? {
    val selectedRadius = initialRadius ?: return null
    return "$variableName:${selectedRadius.value.toDouble()} ${selectedRadius.unit.symbol}"
}
