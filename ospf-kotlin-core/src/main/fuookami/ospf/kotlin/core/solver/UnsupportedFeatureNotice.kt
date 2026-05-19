package fuookami.ospf.kotlin.core.solver

import org.apache.logging.log4j.kotlin.logger

private val unsupportedFeatureLogger = logger("UnsupportedFeatureNotice")

fun warnIgnoredConstraintPriority(
    solverName: String,
    priorityAmount: Int
) {
    if (priorityAmount > 0) {
        unsupportedFeatureLogger.warn("Solver $solverName does not support constraint priority; ignored $priorityAmount constraint priority hints.")
    }
}