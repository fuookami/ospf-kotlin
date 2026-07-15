/**
 * 不支持特性警告通知
 * Unsupported feature warning notices
*/
package fuookami.ospf.kotlin.core.solver

import org.apache.logging.log4j.kotlin.logger

private val unsupportedFeatureLogger = logger("UnsupportedFeatureNotice")

/**
 * 当求解器不支持约束优先级时发出警告。
 * Warn when a solver does not support constraint priority.
 *
 * @param solverName 求解器名称 / Solver name
 * @param priorityAmount 被忽略的优先级提示数量 / Number of ignored priority hints
*/
fun warnIgnoredConstraintPriority(
    solverName: String,
    priorityAmount: Int
) {
    if (priorityAmount > 0) {
        unsupportedFeatureLogger.warn("Solver $solverName does not support constraint priority; ignored $priorityAmount constraint priority hints.")
    }
}
