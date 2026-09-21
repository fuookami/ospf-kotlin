package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.GRB
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.prepareNativeMasking
import fuookami.ospf.kotlin.core.solver.prepareNativeIndicator
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 不写入 SDK 的函数展开候选。 / A function-lowering candidate without SDK writes.
 *
 * @property structure 原始结构快照 / Original structure snapshot
 * @property decision 适用性决策 / Eligibility decision
 */
data class GurobiFunctionLoweringPlan(
    val structure: DeferredFunctionStructure,
    val decision: FunctionLoweringDecision
)

/**
 * Plan native lowering without mutating a Gurobi model. /
 * 只规划原生展开，不修改 Gurobi 模型。
 *
 * Preserve the result relation even for objective-only uses. /
 * 即使仅用于目标，也保留可引用的结果关系。
 *
 * @param model 中间模型 / Intermediate model
 * @param capabilities 当前 SDK 能力 / Current SDK capabilities
 * @return 每个结构的候选决策 / Candidate decisions for each structure
 */
fun planGurobiFunctionLowering(
    model: LinearTriadModelView,
    capabilities: FunctionSolverCapabilities = gurobiFunctionSolverCapabilities()
): List<GurobiFunctionLoweringPlan> {
    return model.deferredFunctionStructures.map { structure ->
        val decision = when (structure) {
            is MaskingStructure<*> -> {
                val candidate = decideFunctionLowering(
                    policy = model.functionExpansionPolicy,
                    usage = structure.usage,
                    capabilities = capabilities,
                    nativeCapability = FunctionNativeCapability.Indicator
                )
                if (model.dual || model.deferredFunctionConstraintRegions.any { it.structure == structure } ||
                    listOf(structure.resultVariable, structure.mask).any { retained -> model.variables.none { it.origin?.key == retained.key } } ||
                    prepareNativeMasking(structure, GRB.INFINITY) !is Ok
                ) FunctionLoweringDecision(false, reason = "masking columns, bounds or fallback region are incompatible")
                else candidate
            }
            is IndicatorStructure<*> -> {
                val candidate = decideFunctionLowering(
                    policy = model.functionExpansionPolicy,
                    usage = structure.usage,
                    capabilities = capabilities,
                    nativeCapability = FunctionNativeCapability.Indicator
                )
                if (model.dual || model.deferredFunctionConstraintRegions.any { it.structure == structure } ||
                    model.variables.none { it.origin?.key == structure.resultVariable.key } ||
                    prepareNativeIndicator(structure, GRB.INFINITY) !is Ok
                ) {
                    FunctionLoweringDecision(false, reason = "indicator columns, bounds or fallback region are incompatible")
                } else candidate
            }
            is UnivariateLinearPiecewiseStructure<*> -> planPiecewiseLowering(
                model = model,
                structure = structure,
                capabilities = capabilities
            )
            is SemiStructure<*> -> decideFunctionLowering(
                policy = model.functionExpansionPolicy,
                usage = structure.usage,
                capabilities = capabilities,
                nativeCapability = FunctionNativeCapability.SemiContinuous
            ).takeIf { it.useNative && structure.usage.location != FunctionUsageLocation.External }
                ?: FunctionLoweringDecision(false, reason = "SEMICONT requires an unreferenced indicator helper" )
            is BinaryLogicStructure<*> -> decideFunctionLowering(
                policy = model.functionExpansionPolicy,
                usage = structure.usage,
                capabilities = capabilities,
                nativeCapability = FunctionNativeCapability.BinaryLogic
            ).takeIf { it.useNative && structure.operation != BinaryLogicOperation.Xor }
                ?: FunctionLoweringDecision(false, reason = "XOR has exact-one semantics and is not a parity constraint")
            else -> FunctionLoweringDecision(false, reason = "Gurobi planner does not support this structure")
        }
        GurobiFunctionLoweringPlan(structure, decision)
    }
}

private fun <V> planPiecewiseLowering(
    model: LinearTriadModelView,
    structure: UnivariateLinearPiecewiseStructure<V>,
    capabilities: FunctionSolverCapabilities
): FunctionLoweringDecision where V : RealNumber<V>, V : NumberField<V> {
    val candidate = decideFunctionLowering(
        policy = model.functionExpansionPolicy,
        usage = structure.usage,
        capabilities = capabilities,
        nativeCapability = FunctionNativeCapability.PiecewiseLinearConstraint
    )
    if (!candidate.useNative) {
        return candidate
    }
    fun fallback(reason: String) = FunctionLoweringDecision(false, reason = reason)
    if (model.dual || model.deferredFunctionConstraintRegions.any {
            (it.structure as? UnivariateLinearPiecewiseStructure<*>)?.resultVariable?.key == structure.resultVariable.key
        }
    ) {
        return fallback("model already contains fallback rows or is a transformed dual")
    }
    val converter = structure.converter ?: return fallback("numeric converter is unavailable")
    val input = structure.input.monomials.singleOrNull() ?: return fallback("input is not a single variable")
    val inputVariable = input.symbol as? AbstractVariableItem<*, *> ?: return fallback("input is not a solver variable")
    val solverInput = model.variables.singleOrNull { it.origin?.key == inputVariable.key }
        ?: return fallback("input variable is not present in the solver model")
    if (model.variables.none { it.origin?.key == structure.resultVariable.key }) {
        return fallback("result variable is not present in the solver model")
    }
    val selectorKeys = structure.selectorVariables.map { it.key }.toSet()
    if (model.variables.any { it.origin?.key in selectorKeys }) {
        return fallback("fallback selector variables have already been indexed")
    }
    return try {
        if (converter.fromValue(input.coefficient).toDouble() != 1.0 ||
            converter.fromValue(structure.input.constant).toDouble() != 0.0
        ) {
            return fallback("affine input requires a separate input variable")
        }
        val points = structure.breakpoints.map { converter.fromValue(it).toDouble() }
        val slopes = structure.slopes.map { converter.fromValue(it).toDouble() }
        val intercepts = structure.intercepts.map { converter.fromValue(it).toDouble() }
        fun usable(value: Double) = value.isFinite() && kotlin.math.abs(value) < GRB.INFINITY
        if (points.size < 2 || slopes.size != points.size - 1 || intercepts.size != slopes.size ||
            (points + slopes + intercepts).any { !usable(it) } ||
            points.zipWithNext().any { (lower, upper) -> lower >= upper }
        ) {
            return fallback("invalid or non-finite piecewise data")
        }
        val inputLower = solverInput.lowerBound.toDouble()
        val inputUpper = solverInput.upperBound.toDouble()
        if (!usable(inputLower) || !usable(inputUpper) || inputLower > inputUpper ||
            inputLower < points.first() || inputUpper > points.last()
        ) {
            return fallback("input bounds permit extrapolation beyond the piecewise domain")
        }
        for (index in slopes.indices) {
            val lowerValue = slopes[index] * points[index] + intercepts[index]
            val upperValue = slopes[index] * points[index + 1] + intercepts[index]
            if (!usable(lowerValue) || !usable(upperValue)) {
                return fallback("piecewise endpoint is not finite")
            }
            if (index > 0 && lowerValue != slopes[index - 1] * points[index] + intercepts[index - 1]) {
                return fallback("piecewise relation is discontinuous")
            }
        }
        candidate
    } catch (_: RuntimeException) {
        fallback("numeric conversion failed")
    }
}
