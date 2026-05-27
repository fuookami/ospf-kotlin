/**
 * 求解值转换上下文管理
 * Solve value conversion context management
 */
package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val solveValueConversionPolicyThreadLocal = ThreadLocal<SolveValueConversionPolicy?>()

/**
 * 获取当前线程的求解值转换策略。
 * Get the current thread's solve value conversion policy.
 *
 * @return 当前转换策略 / Current conversion policy
 */
fun currentSolveValueConversionPolicy(): SolveValueConversionPolicy {
    return solveValueConversionPolicyThreadLocal.get() ?: SolveValueConversionPolicy.AllowRounding
}

/**
 * 在指定的转换策略下执行代码块。
 * Execute a code block under the specified conversion policy.
 *
 * @param T 返回值类型 / Return value type
 * @param policy 转换策略 / Conversion policy
 * @param block 要执行的代码块 / Code block to execute
 * @return 代码块的返回值 / Return value of the code block
 */
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

/**
 * 使用当前线程的转换策略将 Flt64 转换为 Double。
 * Convert Flt64 to Double using the current thread's conversion policy.
 *
 * @param fieldName 字段名称（用于错误信息）/ Field name (for error messages)
 * @return 转换后的 Double 值 / Converted Double value
 */
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

/**
 * 使用指定策略将 Flt64 转换为 Double。
 * Convert Flt64 to Double using the specified policy.
 *
 * @param policy 转换策略 / Conversion policy
 * @param fieldName 字段名称（用于错误信息）/ Field name (for error messages)
 * @param rejectInfinity 是否拒绝无穷大 / Whether to reject infinity
 * @param nanMessage NaN 错误信息 / NaN error message
 * @param infinityMessage 无穷大错误信息 / Infinity error message
 * @return 转换后的 Double 值 / Converted Double value
 */
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
