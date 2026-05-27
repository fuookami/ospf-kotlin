/**
 * 求解器状态枚举
 * Solver status enum
 */
package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.utils.error.ErrorCode

/**
 * 求解器状态枚举，表示求解过程的各种结果。
 * Solver status enum, representing various results of the solving process.
 */
enum class SolverStatus {
    Optimal,
    Feasible,
    Infeasible {
        override val errCode = ErrorCode.ORModelInfeasible
    },
    Unbounded {
        override val errCode = ErrorCode.ORModelUnbounded
    },
    InfeasibleOrUnbounded {
        override val errCode = ErrorCode.ORModelInfeasibleOrUnbounded
    },
    SolvingException {
        override val errCode = ErrorCode.OREngineSolvingException
    };

    open val succeeded: Boolean get() = errCode == null
    open val failed: Boolean get() = errCode != null
    open val errCode: ErrorCode? = null
}
