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
 *
 * @property lowerBounds 变量下界数组 / Variable lower bounds array
 * @property upperBounds 变量上界数组 / Variable upper bounds array
 * @property names 变量名称数组 / Variable names array
 * @property initialResults 初始解列表（列索引, 值）/ Initial solution list (column index, value)
*/
data class VariableDumpingData(
    val lowerBounds: DoubleArray,
    val upperBounds: DoubleArray,
    val names: Array<String>,
    val initialResults: List<Pair<Int, Double>>
)

/**
 * 准备变量转储数据，将变量列表转换为求解器所需的数组格式。
 * Prepare variable dumping data, converting variable list to array format required by solver.
 *
 * @param variables 变量列表 / Variable list
 * @param scopeName 作用域名称（用于错误信息）/ Scope name (for error messages)
 * @return 变量转储数据 / Variable dumping data
*/
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

/**
 * 计算约束分段大小，用于并发转储时的任务划分。
 * Compute constraint segment size for task partitioning during concurrent dumping.
 *
 * @param constraintSize 约束总数 / Total constraint count
 * @param availableProcessors 可用处理器数 / Available processor count
 * @return 分段大小 / Segment size
*/
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
