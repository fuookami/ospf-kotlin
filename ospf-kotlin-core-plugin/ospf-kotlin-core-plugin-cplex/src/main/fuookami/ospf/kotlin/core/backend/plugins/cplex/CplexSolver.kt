package fuookami.ospf.kotlin.core.backend.plugins.cplex

import ilog.cplex.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class CplexSolver {
    protected lateinit var cplex: IloCplex
    protected lateinit var status: SolverStatus

    protected suspend fun init(name: String): Try {
        cplex = IloCplex()
        cplex.name = name
        return ok
    }

    protected suspend fun analyzeStatus(): Try {
        status = when (cplex.status) {
            IloCplex.Status.Optimal -> {
                SolverStatus.Optimal
            }

            IloCplex.Status.Feasible -> {
                SolverStatus.Feasible
            }

            IloCplex.Status.Unbounded -> {
                SolverStatus.Unbounded
            }

            IloCplex.Status.Infeasible -> {
                SolverStatus.Infeasible
            }

            IloCplex.Status.InfeasibleOrUnbounded, IloCplex.Status.Error, IloCplex.Status.Unknown -> {
                SolverStatus.SolvingException
            }

            else -> {
                SolverStatus.SolvingException
            }
        }
        return ok
    }
}
