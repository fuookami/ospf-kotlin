package fuookami.ospf.kotlin.core.backend.solver.output

import fuookami.ospf.kotlin.utils.error.ErrorCode

enum class SolvingStatus {
    Optimal {
        override fun succeeded(): Boolean {
            return true
        }
    },
    Feasible {
        override fun succeeded(): Boolean {
            return true
        }
    },
    NoSolution {
        override fun errCode(): ErrorCode {
            return ErrorCode.ORModelNoSolution
        }
    },
    Unbounded {
        override fun errCode(): ErrorCode {
            return ErrorCode.ORModelUnbounded
        }
    },
    SolvingException {
        override fun errCode(): ErrorCode {
            return ErrorCode.OREngineSolvingException
        }
    };

    open fun succeeded(): Boolean {
        return false
    }

    open fun failed(): Boolean {
        return !succeeded()
    }

    open fun errCode(): ErrorCode? {
        return null
    }
}
