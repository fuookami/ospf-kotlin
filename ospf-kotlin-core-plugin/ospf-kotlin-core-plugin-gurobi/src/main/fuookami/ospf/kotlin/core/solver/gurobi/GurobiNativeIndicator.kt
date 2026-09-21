package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.NativeIndicatorData
import fuookami.ospf.kotlin.core.solver.prepareNativeIndicator
import fuookami.ospf.kotlin.core.variable.VariableItemKey

internal fun selectGurobiNativeIndicator(model: LinearMechanismModel<Flt64>): List<IndicatorStructure<*>> {
    if (!gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.Indicator)) return emptyList()
    return model.deferredFunctionStructures.filterIsInstance<IndicatorStructure<*>>().filter { structure ->
        model.deferredFunctionConstraintRegions.none { it.structure == structure } &&
            prepareNativeIndicator(structure, GRB.INFINITY) is Ok
    }
}

internal fun addGurobiNativeIndicator(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<IndicatorStructure<*>>
): Try {
    fun invalid(): Try = Failed(ErrorCode.IllegalArgument, "Gurobi 指示列或范围无效。 / Invalid Gurobi indicator columns or bounds.")
    return try {
        val prepared = mutableListOf<NativeIndicatorData>()
        model.update()
        for (structure in structures) {
            val root = when (val result = prepareNativeIndicator(structure, GRB.INFINITY)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            for (data in listOf(root) + listOfNotNull(root.conjunction?.condition, root.difference?.condition)) {
                if (data.difference?.let { it.resultKey !in variables } == true) return invalid()
                if (data.conjunction?.let { variables[it.resultKey]?.get(GRB.CharAttr.VType) != GRB.BINARY } == true) return invalid()
                val result = variables[data.resultKey] ?: return invalid()
                if (result.get(GRB.CharAttr.VType) != GRB.BINARY) return invalid()
                if (data.equivalentResultKeys.any { variables[it]?.get(GRB.CharAttr.VType) != GRB.BINARY }) return invalid()
                if (data.zeroBand?.let { variables[it.sideKey]?.get(GRB.CharAttr.VType) != GRB.BINARY } == true) return invalid()
                var lower = data.constant
                var upper = data.constant
                for ((key, coefficient) in data.terms) {
                    val variable = variables[key] ?: return invalid()
                    val lb = variable.get(GRB.DoubleAttr.LB)
                    val ub = variable.get(GRB.DoubleAttr.UB)
                    if (!lb.isFinite() || !ub.isFinite() || lb <= -GRB.INFINITY || ub >= GRB.INFINITY || lb > ub) return invalid()
                    lower += coefficient * if (coefficient > 0) lb else ub
                    upper += coefficient * if (coefficient > 0) ub else lb
                }
                if (!lower.isFinite() || !upper.isFinite() || lower < data.lowerBound || upper > data.upperBound) return invalid()
                if (data.impliedCondition?.let { variables[it.resultKey]?.get(GRB.CharAttr.VType) != GRB.BINARY } == true) return invalid()
                for (value in listOfNotNull(data.conditionalValue, data.impliedCondition)) {
                    if (value.resultKey !in variables) return invalid()
                    var valueLower = value.constant
                    var valueUpper = value.constant
                    for ((key, coefficient) in value.terms) {
                        val variable = variables[key] ?: return invalid()
                        val lb = variable.get(GRB.DoubleAttr.LB)
                        val ub = variable.get(GRB.DoubleAttr.UB)
                        if (!lb.isFinite() || !ub.isFinite() || lb <= -GRB.INFINITY || ub >= GRB.INFINITY || lb > ub) return invalid()
                        valueLower += coefficient * if (coefficient > 0) lb else ub
                        valueUpper += coefficient * if (coefficient > 0) ub else lb
                    }
                    if (!valueLower.isFinite() || !valueUpper.isFinite() || valueLower < value.lowerBound || valueUpper > value.upperBound) return invalid()
                }
                prepared += data
            }
        }
        for (data in prepared) {
            val expression = GRBLinExpr()
            for ((key, coefficient) in data.terms) expression.addTerm(coefficient, variables.getValue(key))
            val result = variables.getValue(data.resultKey)
            data.difference?.let { combined ->
                val second = variables.getValue(combined.condition.resultKey)
                val equality = GRBLinExpr()
                equality.addTerm(1.0, variables.getValue(combined.resultKey))
                equality.addTerm(-1.0, result)
                equality.addTerm(1.0, second)
                model.addConstr(
                    equality,
                    GRB.EQUAL,
                    0.0,
                    "${combined.name}_bter_result"
                )
                val exclusive = GRBLinExpr()
                exclusive.addTerm(1.0, result)
                exclusive.addTerm(1.0, second)
                model.addConstr(
                    exclusive,
                    GRB.LESS_EQUAL,
                    1.0,
                    "${combined.name}_bter_exclusive"
                )
            }
            data.conjunction?.let { combined ->
                val output = variables.getValue(combined.resultKey)
                val second = variables.getValue(combined.condition.resultKey)
                val firstLink = GRBLinExpr()
                firstLink.addTerm(1.0, output)
                firstLink.addTerm(-1.0, result)
                model.addConstr(
                    firstLink,
                    GRB.LESS_EQUAL,
                    0.0,
                    "${combined.name}_link_ge"
                )
                val secondLink = GRBLinExpr()
                secondLink.addTerm(1.0, output)
                secondLink.addTerm(-1.0, second)
                model.addConstr(
                    secondLink,
                    GRB.LESS_EQUAL,
                    0.0,
                    "${combined.name}_link_le"
                )
                val lowerLink = GRBLinExpr()
                lowerLink.addTerm(1.0, output)
                lowerLink.addTerm(-1.0, result)
                lowerLink.addTerm(-1.0, second)
                model.addConstr(
                    lowerLink,
                    GRB.GREATER_EQUAL,
                    -1.0,
                    "${combined.name}_link_lb"
                )
            }
            val positiveValue = if (data.positiveOnZero) 0 else 1
            val band = data.zeroBand
            if (band == null) {
                model.addGenConstrIndicator(
                    result,
                    positiveValue,
                    expression,
                    GRB.GREATER_EQUAL,
                    data.tolerance - data.constant,
                    "${data.name}_positive"
                )
                model.addGenConstrIndicator(
                    result,
                    1 - positiveValue,
                    expression,
                    GRB.LESS_EQUAL,
                    -data.constant,
                    "${data.name}_nonpositive"
                )
            } else {
                model.addGenConstrIndicator(
                    result,
                    1 - positiveValue,
                    expression,
                    GRB.LESS_EQUAL,
                    band.tolerance - data.constant,
                    "${data.name}_inside_upper"
                )
                model.addGenConstrIndicator(
                    result,
                    1 - positiveValue,
                    expression,
                    GRB.GREATER_EQUAL,
                    -band.tolerance - data.constant,
                    "${data.name}_inside_lower"
                )
                val lower = GRBLinExpr()
                val upper = GRBLinExpr()
                for ((key, coefficient) in data.terms) {
                    lower.addTerm(coefficient, variables.getValue(key))
                    upper.addTerm(coefficient, variables.getValue(key))
                }
                val threshold = data.tolerance + band.tolerance
                lower.addTerm(if (data.positiveOnZero) threshold else -threshold, result)
                upper.addTerm(if (data.positiveOnZero) -threshold else threshold, result)
                val lowerBound = if (data.positiveOnZero) data.tolerance else -band.tolerance
                val upperBound = if (data.positiveOnZero) -data.tolerance else band.tolerance
                val side = variables.getValue(band.sideKey)
                model.addGenConstrIndicator(
                    side,
                    1,
                    lower,
                    GRB.GREATER_EQUAL,
                    lowerBound - data.constant,
                    "${data.name}_side_positive"
                )
                model.addGenConstrIndicator(
                    side,
                    0,
                    upper,
                    GRB.LESS_EQUAL,
                    upperBound - data.constant,
                    "${data.name}_side_negative"
                )
            }
            data.impliedCondition?.let { value ->
                val consequent = GRBLinExpr()
                for ((key, coefficient) in value.terms) consequent.addTerm(coefficient, variables.getValue(key))
                model.addGenConstrIndicator(
                    result,
                    1,
                    consequent,
                    GRB.GREATER_EQUAL,
                    data.tolerance - value.constant,
                    "${value.name}_con_implied"
                )
                val link = GRBLinExpr()
                link.addTerm(1.0, result)
                link.addTerm(-1.0, variables.getValue(value.resultKey))
                model.addConstr(
                    link,
                    GRB.LESS_EQUAL,
                    0.0,
                    "${value.name}_imply_link"
                )
            }
            data.conditionalValue?.let { value ->
                val output = variables.getValue(value.resultKey)
                val trueExpression = GRBLinExpr()
                trueExpression.addTerm(1.0, output)
                for ((key, coefficient) in value.terms) trueExpression.addTerm(-coefficient, variables.getValue(key))
                model.addGenConstrIndicator(
                    result,
                    1,
                    trueExpression,
                    GRB.EQUAL,
                    value.constant,
                    "${value.name}_then"
                )
                val falseExpression = GRBLinExpr()
                falseExpression.addTerm(1.0, output)
                model.addGenConstrIndicator(
                    result,
                    0,
                    falseExpression,
                    GRB.EQUAL,
                    0.0,
                    "${value.name}_zero"
                )
            }
            for ((index, key) in data.equivalentResultKeys.withIndex()) {
                val equality = GRBLinExpr()
                equality.addTerm(1.0, result)
                equality.addTerm(-1.0, variables.getValue(key))
                model.addConstr(
                    equality,
                    GRB.EQUAL,
                    0.0,
                    "${data.name}_equivalent_$index"
                )
            }
        }
        model.update()
        ok
    } catch (error: Exception) {
        Failed(ErrorCode.OREngineModelingException, "Gurobi 指示写入失败：${error.message} / Gurobi indicator write failed")
    }
}
