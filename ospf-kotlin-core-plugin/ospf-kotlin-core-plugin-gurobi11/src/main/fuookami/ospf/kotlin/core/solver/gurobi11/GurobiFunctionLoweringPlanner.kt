package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.math.abs
import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*

/** 不修改 Gurobi 模型的函数原生展开候选。 / A native function-lowering candidate that does not mutate a Gurobi model. */
data class GurobiFunctionLoweringPlan(
    val structure: DeferredFunctionStructure,
    val decision: FunctionLoweringDecision
)

/**
 * 只规划原生展开，不写入 Gurobi 模型。 / Plan native lowering without writing to a Gurobi model.
 *
 * 即使函数只出现在目标中，也保留结果变量关系。 / Preserve the result-variable relation even for objective-only usage.
 *
 * @param model 中间模型 / Intermediate model
 * @param capabilities 当前 SDK 能力 / Current SDK capabilities
 * @return 每个函数结构的原生决策 / Native decision for each function structure
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
                    fuookami.ospf.kotlin.core.solver.prepareNativeMasking(structure, GRB.INFINITY) !is fuookami.ospf.kotlin.utils.functional.Ok
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
                    fuookami.ospf.kotlin.core.solver.prepareNativeIndicator(structure, GRB.INFINITY) !is fuookami.ospf.kotlin.utils.functional.Ok
                ) {
                    FunctionLoweringDecision(false, reason = "indicator columns, bounds or fallback region are incompatible")
                } else candidate
            }
            is UnivariateLinearPiecewiseStructure<*> -> planPiecewiseLowering(
                model = model,
                structure = structure,
                capabilities = capabilities
            )
            is SemiStructure<*> -> planSemiLowering(
                model = model,
                structure = structure,
                capabilities = capabilities
            )
            is BinaryLogicStructure<*> -> decideFunctionLowering(
                policy = model.functionExpansionPolicy,
                usage = structure.usage,
                capabilities = capabilities,
                nativeCapability = FunctionNativeCapability.BinaryLogic
            ).takeIf { it.useNative && structure.operation != BinaryLogicOperation.Xor }
                ?: FunctionLoweringDecision(false, reason = "XOR has exact-one semantics and is not a parity constraint")
            else -> FunctionLoweringDecision(false, reason = "Gurobi 11 planner does not support this structure")
        }
        GurobiFunctionLoweringPlan(structure, decision)
    }
}

private fun planSemiLowering(
    model: LinearTriadModelView,
    structure: SemiStructure<*>,
    capabilities: FunctionSolverCapabilities
): FunctionLoweringDecision {
    val candidate = decideFunctionLowering(
        policy = model.functionExpansionPolicy,
        usage = structure.usage,
        capabilities = capabilities,
        nativeCapability = FunctionNativeCapability.SemiContinuous
    )
    if (!candidate.useNative) {
        return candidate
    }
    return try {
        val lower = structure.converter.fromValue(structure.lowerBound).toDouble()
        val upper = structure.converter.fromValue(structure.upperBound).toDouble()
        when {
            !lower.isFinite() || !upper.isFinite() || lower <= 0.0 || lower > upper ->
                FunctionLoweringDecision(false, reason = "SEMICONT requires finite 0 < lb <= ub")
            model.dual || model.deferredFunctionConstraintRegions.any {
                (it.structure as? SemiStructure<*>)?.resultVariable?.key == structure.resultVariable.key
            } -> FunctionLoweringDecision(false, reason = "model already contains fallback rows or is a transformed dual")
            model.variables.none { it.origin?.key == structure.resultVariable.key } ->
                FunctionLoweringDecision(false, reason = "semi result variable is not present in the solver model")
            else -> candidate
        }
    } catch (_: RuntimeException) {
        FunctionLoweringDecision(false, reason = "SEMICONT bounds are not representable")
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
    val data = when (val prepared = prepareGurobiNativePiecewise(structure)) {
        is Ok -> prepared.value
        is Failed -> return fallback("piecewise data is invalid")
        is Fatal -> return fallback("piecewise data is invalid")
    }
    val inputVariable = structure.input.monomials.singleOrNull()?.symbol as? AbstractVariableItem<*, *>
        ?: return fallback("input is not a solver variable")
    if (inputVariable.key != data.inputKey) {
        return fallback("input variable does not match prepared native data")
    }
    val solverInput = model.variables.singleOrNull { it.origin?.key == data.inputKey }
        ?: return fallback("input variable is not present in the solver model")
    if (model.variables.none { it.origin?.key == data.resultKey }) {
        return fallback("result variable is not present in the solver model")
    }
    val selectorKeys = structure.selectorVariables.map { it.key }.toSet()
    if (model.variables.any { it.origin?.key in selectorKeys }) {
        return fallback("fallback selector variables have already been indexed")
    }
    val inputLower = solverInput.lowerBound.toDouble()
    val inputUpper = solverInput.upperBound.toDouble()
    fun usable(value: Double): Boolean = value.isFinite() && abs(value) < GRB.INFINITY
    if (!usable(inputLower) || !usable(inputUpper) || inputLower > inputUpper ||
        inputLower < data.xPoints.first() || inputUpper > data.xPoints.last()
    ) {
        return fallback("input bounds permit extrapolation beyond the piecewise domain")
    }
    return candidate
}
