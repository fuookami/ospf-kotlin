package fuookami.ospf.kotlin.core.backend.plugins.cplex

import ilog.cplex.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class CplexSolver {
    lateinit var cplex: IloCplex
    lateinit var status: SolvingStatus

    protected fun init(name: String): Try {
        cplex = IloCplex()
        cplex.name = name
        return ok
    }

    protected fun analyzeStatus(): Try {
        status = when (cplex.status) {
            IloCplex.Status.Optimal -> {
                SolvingStatus.Optimal
            }

            IloCplex.Status.Feasible -> {
                SolvingStatus.Feasible
            }

            IloCplex.Status.Unbounded -> {
                SolvingStatus.Unbounded
            }

            IloCplex.Status.Infeasible -> {
                SolvingStatus.NoSolution
            }

            IloCplex.Status.InfeasibleOrUnbounded, IloCplex.Status.Error, IloCplex.Status.Unknown -> {
                SolvingStatus.SolvingException
            }

            else -> {
                SolvingStatus.SolvingException
            }
        }
        return ok
    }
}
