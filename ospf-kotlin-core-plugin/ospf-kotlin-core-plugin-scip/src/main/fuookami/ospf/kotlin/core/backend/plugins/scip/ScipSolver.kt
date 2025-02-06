package fuookami.ospf.kotlin.core.backend.plugins.scip

import jscip.*
import kotlin.time.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.utils.math.UInt64
import kotlinx.datetime.Clock

abstract class ScipSolver {
    companion object {
        init {
            System.loadLibrary("jscip")
        }
    }

    protected lateinit var scip: Scip
    protected lateinit var status: SolverStatus
    protected var solvingTime: Duration? = null

    protected open fun finalize() {
        scip.free()
    }

    protected suspend fun init(name: String): Try {
        scip = Scip()
        scip.create(name)
        return ok
    }

    protected suspend fun solve(threadNum: UInt64): Try {
        val begin = Clock.System.now()
        if (threadNum gr UInt64.one) {
            scip.solveConcurrent()
            val stage = scip.stage
            if (stage.swigValue() < SCIP_Stage.SCIP_STAGE_INITPRESOLVE.swigValue()) {
                scip.solve()
            }
        } else {
            scip.solve()
        }
        solvingTime = Clock.System.now() - begin

        return ok
    }

    protected suspend fun analyzeStatus(): Try {
        val solution = scip.bestSol
        status = when (scip.status) {
            SCIP_Status.SCIP_STATUS_OPTIMAL -> {
                SolverStatus.Optimal
            }

            SCIP_Status.SCIP_STATUS_INFEASIBLE -> {
                SolverStatus.Infeasible
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
