package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.math.abs
import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** 选择可直接使用 Gurobi 11 MAX 的结构。 / Select structures eligible for direct Gurobi 11 MAX. */
internal fun selectGurobiNativeMax(
    model: LinearMechanismModel<Flt64>
): List<MaxStructure<*>> {
    if (!gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.GeneralMinMax)) {
        return emptyList()
    }
    val boundKeys = model.deferredFunctionConstraintRegions.mapNotNull {
        (it.structure as? MaxStructure<*>)?.resultVariable?.key
    }.toSet()
    val tokens = model.tokens.tokens.associateBy { it.key }
    return model.deferredFunctionStructures.filterIsInstance<MaxStructure<*>>().filter { structure ->
        if (structure.resultVariable.key in boundKeys) {
            return@filter false
        }
        val data = when (val prepared = prepareGurobiNativeMax(structure)) {
            is Ok -> prepared.value
            is Failed, is Fatal -> return@filter false
        }
        if (data.resultKey !in tokens || data.selectorKeys.any { it !in tokens }) {
            return@filter false
        }
        if (!maxInputBoundsRemainCovered(data, tokens)) {
            return@filter false
        }
        true
    }
}

private fun maxInputBoundsRemainCovered(
    data: GurobiNativeMaxData,
    tokens: Map<VariableItemKey, Token<Flt64>>
): Boolean {
    fun usable(value: Double): Boolean = value.isFinite() && abs(value) < GRB.INFINITY
    for (input in data.inputs) {
        var lower = input.constant
        var upper = input.constant
        for ((key, coefficient) in input.terms) {
            val token = tokens[key]
                ?: return false
            val tokenLower = token.lowerBound?.value?.unwrap()?.toDouble() ?: return false
            val tokenUpper = token.upperBound?.value?.unwrap()?.toDouble() ?: return false
            if (!usable(tokenLower) || !usable(tokenUpper) || tokenLower > tokenUpper) {
                return false
            }
            if (coefficient >= 0.0) {
                lower += coefficient * tokenLower
                upper += coefficient * tokenUpper
            } else {
                lower += coefficient * tokenUpper
                upper += coefficient * tokenLower
            }
        }
        if (!usable(lower) || !usable(upper) || lower < input.lowerBound || upper > input.upperBound) {
            return false
        }
    }
    return true
}
