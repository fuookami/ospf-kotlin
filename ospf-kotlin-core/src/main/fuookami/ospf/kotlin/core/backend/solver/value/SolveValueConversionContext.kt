package fuookami.ospf.kotlin.core.backend.solver.value

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

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

fun Flt64.toSolverDouble(fieldName: String = "solver.value"): Double {
    val converted = this.toDouble()
    if (currentSolveValueConversionPolicy() == SolveValueConversionPolicy.Strict) {
        require(!converted.isNaN()) {
            "Strict conversion rejected NaN at $fieldName."
        }
        require(!converted.isInfinite()) {
            "Strict conversion rejected infinity at $fieldName."
        }
    }
    return converted
}
