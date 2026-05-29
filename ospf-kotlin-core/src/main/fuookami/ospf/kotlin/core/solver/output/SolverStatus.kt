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
    /** 最优解 / Optimal solution */
    Optimal,
    /** 可行解 / Feasible solution */
    Feasible,
    /** 不可行 / Infeasible */
    Infeasible {
        override val errCode = ErrorCode.ORModelInfeasible
    },
    /** 无界 / Unbounded */
    Unbounded {
        override val errCode = ErrorCode.ORModelUnbounded
    },
    /** 不可行或无界 / Infeasible or unbounded */
    InfeasibleOrUnbounded {
        override val errCode = ErrorCode.ORModelInfeasibleOrUnbounded
    },
    /** 求解异常 / Solving exception */
    SolvingException {
        override val errCode = ErrorCode.OREngineSolvingException
    };

    /** 是否成功 / Whether succeeded */
    open val succeeded: Boolean get() = errCode == null
    /** 是否失败 / Whether failed */
    open val failed: Boolean get() = errCode != null
    /** 错误码 / Error code */
    open val errCode: ErrorCode? = null
}
