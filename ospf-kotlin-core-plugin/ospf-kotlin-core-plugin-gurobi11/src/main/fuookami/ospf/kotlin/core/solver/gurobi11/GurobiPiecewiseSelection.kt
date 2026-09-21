package fuookami.ospf.kotlin.core.solver.gurobi11

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.*

/** 在 solver 编号前筛选可使用 Gurobi 11 原生 PWL 的结构。 / Select Gurobi 11 native PWL structures before solver indexing. */
internal fun selectGurobiNativePiecewise(
    model: LinearMechanismModel<Flt64>
): List<UnivariateLinearPiecewiseStructure<*>> {
    if (!gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.PiecewiseLinearConstraint)) {
        return emptyList()
    }
    val boundKeys = model.deferredFunctionConstraintRegions.mapNotNull {
        (it.structure as? UnivariateLinearPiecewiseStructure<*>)?.resultVariable?.key
    }.toSet()
    val tokens = model.tokens.tokens.associateBy { it.key }
    return model.deferredFunctionStructures.filterIsInstance<UnivariateLinearPiecewiseStructure<*>>().filter { structure ->
        if (structure.resultVariable.key in boundKeys) {
            return@filter false
        }
        val data = when (val prepared = prepareGurobiNativePiecewise(structure)) {
            is Ok -> prepared.value
            else -> return@filter false
        }
        val input = tokens[data.inputKey] ?: return@filter false
        if (data.resultKey !in tokens) {
            return@filter false
        }
        val lower = input.lowerBound?.value?.unwrap()?.toDouble() ?: return@filter false
        val upper = input.upperBound?.value?.unwrap()?.toDouble() ?: return@filter false
        lower.isFinite() && upper.isFinite() && lower <= upper &&
            lower >= data.xPoints.first() && upper <= data.xPoints.last()
    }
}
