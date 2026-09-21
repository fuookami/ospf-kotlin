package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.math.abs
import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel

/** 选择可直接使用 Gurobi 11 ABS 的结构。 / Select structures eligible for direct Gurobi 11 ABS. */
internal fun selectGurobiNativeAbs(
    model: LinearMechanismModel<Flt64>
): List<AbsStructure<*>> {
    if (!gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.GeneralAbs)) {
        return emptyList()
    }
    val boundKeys = model.deferredFunctionConstraintRegions.mapNotNull {
        (it.structure as? AbsStructure<*>)?.resultVariable?.key
    }.toSet()
    val tokens = model.tokens.tokens.associateBy { it.key }
    return model.deferredFunctionStructures.filterIsInstance<AbsStructure<*>>().filter { structure ->
        if (structure.resultVariable.key in boundKeys) {
            return@filter false
        }
        val data = when (val prepared = prepareGurobiNativeAbs(structure)) {
            is Ok -> prepared.value
            is Failed, is Fatal -> return@filter false
        }
        val input = tokens[data.inputKey] ?: return@filter false
        if (data.resultKey !in tokens) {
            return@filter false
        }
        val lower = input.lowerBound?.value?.unwrap()?.toDouble() ?: return@filter false
        val upper = input.upperBound?.value?.unwrap()?.toDouble() ?: return@filter false
        lower.isFinite() && upper.isFinite() && lower <= upper &&
            abs(lower) < GRB.INFINITY && abs(upper) < GRB.INFINITY &&
            lower >= -data.negativeBigM && upper <= data.positiveBigM
    }
}
