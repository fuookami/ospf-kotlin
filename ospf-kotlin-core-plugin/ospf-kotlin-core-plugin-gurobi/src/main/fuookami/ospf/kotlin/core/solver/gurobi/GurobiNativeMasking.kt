package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.solver.NativeMaskingData
import fuookami.ospf.kotlin.core.solver.prepareNativeMasking
import fuookami.ospf.kotlin.core.variable.VariableItemKey

internal fun selectGurobiNativeMasking(model: LinearMechanismModel<Flt64>): List<MaskingStructure<*>> {
    if (!gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.Indicator)) return emptyList()
    return model.deferredFunctionStructures.filterIsInstance<MaskingStructure<*>>().filter { structure ->
        model.deferredFunctionConstraintRegions.none { it.structure == structure } &&
            prepareNativeMasking(structure, GRB.INFINITY) is Ok
    }
}

internal fun addGurobiNativeMasking(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<MaskingStructure<*>>
): Try {
    fun invalid(): Try = Failed(ErrorCode.IllegalArgument, "Gurobi 门控列或范围无效。 / Invalid Gurobi masking columns or bounds.")
    return try {
        val prepared = mutableListOf<NativeMaskingData>()
        model.update()
        for (structure in structures) {
            val data = when (val result = prepareNativeMasking(structure, GRB.INFINITY)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (variables[data.maskKey]?.get(GRB.CharAttr.VType) != GRB.BINARY || data.value.resultKey !in variables) return invalid()
            if (data.definition?.terms?.keys?.any { it !in variables } == true) return invalid()
            var lower = data.value.constant
            var upper = data.value.constant
            for ((key, coefficient) in data.value.terms) {
                val variable = variables[key] ?: return invalid()
                val lb = variable.get(GRB.DoubleAttr.LB)
                val ub = variable.get(GRB.DoubleAttr.UB)
                if (!lb.isFinite() || !ub.isFinite() || lb <= -GRB.INFINITY || ub >= GRB.INFINITY || lb > ub) return invalid()
                lower += coefficient * if (coefficient > 0) lb else ub
                upper += coefficient * if (coefficient > 0) ub else lb
            }
            if (!lower.isFinite() || !upper.isFinite() || lower < data.value.lowerBound || upper > data.value.upperBound) return invalid()
            prepared += data
        }
        for (data in prepared) {
            val mask = variables.getValue(data.maskKey)
            val output = variables.getValue(data.value.resultKey)
            data.definition?.let { definition ->
                val equality = GRBLinExpr()
                equality.addTerm(1.0, mask)
                for ((key, coefficient) in definition.terms) equality.addTerm(-coefficient, variables.getValue(key))
                model.addConstr(equality, GRB.EQUAL, definition.constant, "${data.value.name}_mask_eq")
            }
            val active = GRBLinExpr()
            active.addTerm(1.0, output)
            for ((key, coefficient) in data.value.terms) active.addTerm(-coefficient, variables.getValue(key))
            model.addGenConstrIndicator(mask, 1, active, GRB.EQUAL, data.value.constant, "${data.value.name}_active")
            val inactive = GRBLinExpr()
            inactive.addTerm(1.0, output)
            model.addGenConstrIndicator(mask, 0, inactive, GRB.EQUAL, 0.0, "${data.value.name}_inactive")
        }
        model.update()
        ok
    } catch (error: Exception) {
        Failed(ErrorCode.OREngineModelingException, "Gurobi 门控写入失败：${error.message} / Gurobi masking write failed")
    }
}
