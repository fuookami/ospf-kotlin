package fuookami.ospf.kotlin.core.backend.plugins.mosek

import kotlin.time.*
import mosek.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

abstract class MosekSolver {
    protected lateinit var env: Env
    protected lateinit var mosekModel: Task
    protected lateinit var status: SolverStatus

    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        TODO("not implemented yet")
    }

    protected suspend fun solve(): Try {
        TODO("not implemented yet")
    }

    protected suspend fun analyzeStatus(): Try {
        return try {
            status = when (val result = mosekModel.getsolsta(soltype.bas)) {
                solsta.optimal -> {
                    SolverStatus.Optimal
                }

                solsta.prim_and_dual_feas -> {
                    SolverStatus.Feasible
                }

                solsta.dual_infeas_cer -> {
                    SolverStatus.Unbounded
                }

                solsta.prim_infeas_cer ->{
                    SolverStatus.Infeasible
                }

                else -> {
                    SolverStatus.Infeasible
                }
            }

            ok
        } catch (e: Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException, e.message))
        } catch (e: java.lang.Exception) {
            Failed(Err(ErrorCode.OREngineSolvingException))
        }
    }
}
