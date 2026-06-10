package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ContinuousCylinderRadiusSolverPrototype

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
 * @property registeredVariables 已注册进 solver model 的变量 / variables registered into solver model
 */
internal data class ContinuousRadiusSolverVariableRegistrationPlan(
    val variableNames: List<String>,
    val boundDescriptions: List<String>,
    val selectedRadiusDescriptions: List<String>,
    val gapDescriptions: List<String>,
    val productionReadyVariables: List<String>,
    val modelRegistrationBlockedVariables: List<String>,
    val registeredVariables: List<ContinuousRadiusSolverVariable> = emptyList()
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
        return mapOf(
            "continuous_radius_solver_registration_plan_count" to variableNames.size.toString(),
            "continuous_radius_solver_registration_plan_variables" to variableNames.joinToString("|"),
            "continuous_radius_solver_registration_plan_bounds" to boundDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_selected_radii" to selectedRadiusDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_gap_variables" to gapDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_production_ready_variables" to productionReadyVariables.joinToString("|"),
            "continuous_radius_solver_model_registration_blocked_variables" to modelRegistrationBlockedVariables.joinToString("|"),
            "continuous_radius_solver_model_registration_blocked_reason" to blockedReason
        )
    }
}

/**
 * 生成连续半径 solver 变量注册计划。
 * Build the continuous-radius solver variable registration plan.
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param solverVariables solver 变量列表 / solver variable list
 * @return 注册计划 / registration plan
 */
internal fun continuousRadiusSolverVariableRegistrationPlan(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    solverVariables: List<ContinuousRadiusSolverVariable> = emptyList()
): ContinuousRadiusSolverVariableRegistrationPlan {
    val variableNames = ArrayList<String>()
    val boundDescriptions = ArrayList<String>()
    val selectedRadiusDescriptions = ArrayList<String>()
    val gapDescriptions = ArrayList<String>()
    val productionReadyVariables = ArrayList<String>()
    val modelRegistrationBlockedVariables = ArrayList<String>()
    val registeredVariables = ArrayList<ContinuousRadiusSolverVariable>()
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
        if (prototype.isSolverRegisterable) {
            val solverVar = solverVariables.firstOrNull { it.prototype.variableName == prototype.variableName }
            if (solverVar != null) {
                registeredVariables.add(solverVar)
            }
        } else {
            modelRegistrationBlockedVariables.add(prototype.variableName)
        }
    }
    return ContinuousRadiusSolverVariableRegistrationPlan(
        variableNames = variableNames,
        boundDescriptions = boundDescriptions,
        selectedRadiusDescriptions = selectedRadiusDescriptions,
        gapDescriptions = gapDescriptions,
        productionReadyVariables = productionReadyVariables,
        modelRegistrationBlockedVariables = modelRegistrationBlockedVariables,
        registeredVariables = registeredVariables
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
 * 从原型列表创建 solver 变量列表。
 * Create solver variable list from prototype list.
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
