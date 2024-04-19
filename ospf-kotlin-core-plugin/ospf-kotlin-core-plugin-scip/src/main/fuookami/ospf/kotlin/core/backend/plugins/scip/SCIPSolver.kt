package fuookami.ospf.kotlin.core.backend.plugins.scip

import kotlin.time.*
import kotlinx.datetime.*
import jscip.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class SCIPSolver {
    companion object {
        init {
            System.loadLibrary("jscip")
        }
    }

    lateinit var scip: Scip
    lateinit var status: SolvingStatus

    protected open fun finalize() {
        scip.free()
    }

    protected fun init(name: String): Try {
        scip = Scip()
        scip.create(name)
        return ok
    }

    protected fun analyzeStatus(): Try {
        val solution = scip.bestSol
        status = when (scip.status) {
            SCIP_Status.SCIP_STATUS_OPTIMAL -> {
                SolvingStatus.Optimal
            }

            SCIP_Status.SCIP_STATUS_INFEASIBLE -> {
                SolvingStatus.NoSolution
            }

            SCIP_Status.SCIP_STATUS_UNBOUNDED -> {
                SolvingStatus.Unbounded
            }

            SCIP_Status.SCIP_STATUS_INFORUNBD -> {
                SolvingStatus.SolvingException
            }

            else -> {
                if (solution != null) {
                    SolvingStatus.Feasible
                } else {
                    SolvingStatus.SolvingException
                }
            }
        }
        return ok
    }
}
