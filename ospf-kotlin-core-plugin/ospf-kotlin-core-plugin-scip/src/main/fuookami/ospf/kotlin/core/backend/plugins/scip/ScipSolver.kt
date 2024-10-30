package fuookami.ospf.kotlin.core.backend.plugins.scip

import jscip.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class ScipSolver {
    companion object {
        init {
            System.loadLibrary("jscip")
        }
    }

    lateinit var scip: Scip
    lateinit var status: SolverStatus

    protected open fun finalize() {
        scip.free()
    }

    protected suspend fun init(name: String): Try {
        scip = Scip()
        scip.create(name)
        return ok
    }

    protected suspend fun analyzeStatus(): Try {
        val solution = scip.bestSol
        status = when (scip.status) {
            SCIP_Status.SCIP_STATUS_OPTIMAL -> {
                SolverStatus.Optimal
            }

            SCIP_Status.SCIP_STATUS_INFEASIBLE -> {
                SolverStatus.NoSolution
            }

            SCIP_Status.SCIP_STATUS_UNBOUNDED -> {
                SolverStatus.Unbounded
            }

            SCIP_Status.SCIP_STATUS_INFORUNBD -> {
                SolverStatus.SolvingException
            }

            else -> {
                if (solution != null) {
                    SolverStatus.Feasible
                } else {
                    SolverStatus.SolvingException
                }
            }
        }
        return ok
    }
}
