package fuookami.ospf.kotlin.core.backend.solver.output

import fuookami.ospf.kotlin.utils.error.*

enum class SolvingStatus {
    Optimal {
        override val succeeded = true
    },
    Feasible {
        override val succeeded = true
    },
    NoSolution {
        override val errCode: ErrorCode = ErrorCode.ORModelNoSolution
    },
    Unbounded {
        override val errCode: ErrorCode = ErrorCode.ORModelUnbounded
    },
    SolvingException {
        override val errCode: ErrorCode = ErrorCode.OREngineSolvingException
    };

    open val succeeded: Boolean = false
    open val failed: Boolean get() = !succeeded
    open val errCode: ErrorCode? = null
}
