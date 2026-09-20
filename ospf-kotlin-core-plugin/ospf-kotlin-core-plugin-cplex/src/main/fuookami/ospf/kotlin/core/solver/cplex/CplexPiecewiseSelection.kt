package fuookami.ospf.kotlin.core.solver.cplex

import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.solver.prepareNativePiecewise

/** 在变量编号前选择可由 CPLEX SOS2 表达的连续一元 PWL。 / Select continuous univariate PWLs expressible by CPLEX SOS2 before indexing. */
internal fun selectCplexNativePiecewise(
    model: LinearMechanismModel<Flt64>
): List<UnivariateLinearPiecewiseStructure<*>> {
    if (!cplexFunctionSolverCapabilities().supports(FunctionNativeCapability.SOS2)) {
        return emptyList()
    }
    val boundKeys = model.deferredFunctionConstraintRegions.mapNotNull {
        (it.structure as? UnivariateLinearPiecewiseStructure<*>)?.resultVariable?.key
    }.toSet()
    val tokens = model.tokens.tokens.associateBy { it.key }
    return model.deferredFunctionStructures
        .filterIsInstance<UnivariateLinearPiecewiseStructure<*>>()
        .filter { structure ->
            if (structure.resultVariable.key in boundKeys) {
                return@filter false
            }
            val data = when (val prepared = prepareNativePiecewise(structure)) {
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
