/**
 * 求解器建模数据准备工具
 * Solver modeling data preparation utilities
 */
package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble

/**
 * solver 边界公共 helper（仅用于 solver dump 前数据准备）。
 * Shared helper at solver boundary (only for pre-dump data preparation).
 */
data class VariableDumpingData(
    val lowerBounds: DoubleArray,
    val upperBounds: DoubleArray,
    val names: Array<String>,
    val initialResults: List<Pair<Int, Double>>
)

fun prepareVariableDumpingData(
    variables: List<Variable>,
    scopeName: String
): VariableDumpingData {
    val variableAmount = variables.size
    val lowerBounds = DoubleArray(variableAmount)
    val upperBounds = DoubleArray(variableAmount)
    val names = Array(variableAmount) { "" }
    val initialResults = ArrayList<Pair<Int, Double>>()
    for ((col, variable) in variables.withIndex()) {
        lowerBounds[col] = variable.lowerBound.toSolverDouble("$scopeName.variables[$col].lowerBound")
        upperBounds[col] = variable.upperBound.toSolverDouble("$scopeName.variables[$col].upperBound")
        names[col] = variable.name
        variable.initialResult?.let {
            initialResults.add(col to it.toSolverDouble("$scopeName.variables[$col].initialResult"))
        }
    }
    return VariableDumpingData(
        lowerBounds = lowerBounds,
        upperBounds = upperBounds,
        names = names,
        initialResults = initialResults
    )
}

fun computeConstraintSegmentSize(
    constraintSize: Int,
    availableProcessors: Int = Runtime.getRuntime().availableProcessors()
): Int {
    if (constraintSize <= 0) {
        return 10
    }
    val workerCount = (availableProcessors - 1).coerceAtLeast(1)
    var ratio = constraintSize / workerCount
    if (ratio < 10) {
        return 10
    }
    var segment = 1
    while (ratio >= 10) {
        ratio /= 10
        segment *= 10
    }
    return segment
}
