package fuookami.ospf.kotlin.core.backend.solver.output

import fuookami.ospf.kotlin.utils.error.*

enum class SolverStatus {
    Optimal,
    Feasible,
    Infeasible {
        override val errCode = ErrorCode.ORModelNoSolution
    },
    Unbounded {
        override val errCode = ErrorCode.ORModelUnbounded
    },
    SolvingException {
        override val errCode = ErrorCode.OREngineSolvingException
    };

    open val succeeded: Boolean get() = errCode == null
    open val failed: Boolean get() = errCode != null
    open val errCode: ErrorCode? = null
}
