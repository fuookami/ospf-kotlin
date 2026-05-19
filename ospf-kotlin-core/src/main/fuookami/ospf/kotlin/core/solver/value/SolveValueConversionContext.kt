package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val solveValueConversionPolicyThreadLocal = ThreadLocal<SolveValueConversionPolicy?>()

fun currentSolveValueConversionPolicy(): SolveValueConversionPolicy {
    return solveValueConversionPolicyThreadLocal.get() ?: SolveValueConversionPolicy.AllowRounding
}

suspend fun <T> withSolveValueConversionPolicy(
    policy: SolveValueConversionPolicy,
    block: suspend () -> T
): T {
    val previous = solveValueConversionPolicyThreadLocal.get()
    solveValueConversionPolicyThreadLocal.set(policy)
    try {
        return block()
    } finally {
        solveValueConversionPolicyThreadLocal.set(previous)
    }
}

fun Flt64.toSolverDouble(
    fieldName: String = "solver.value"
): Double {
    return toSolverDouble(
        policy = currentSolveValueConversionPolicy(),
        fieldName = fieldName,
        rejectInfinity = true,
        nanMessage = "Strict conversion rejected NaN at $fieldName.",
        infinityMessage = "Strict conversion rejected infinity at $fieldName."
    )
}

fun Flt64.toSolverDouble(
    policy: SolveValueConversionPolicy,
    fieldName: String,
    rejectInfinity: Boolean = true,
    nanMessage: String = "Strict conversion rejected NaN at $fieldName.",
    infinityMessage: String = "Strict conversion rejected infinity at $fieldName."
): Double {
    val converted = this.toDouble()
    if (policy == SolveValueConversionPolicy.Strict) {
        require(!converted.isNaN()) {
            nanMessage
        }
        if (rejectInfinity) {
            require(!converted.isInfinite()) {
                infinityMessage
            }
        }
    }
    return converted
}