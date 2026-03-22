@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.plugins.mosek

import fuookami.ospf.kotlin.core.backend.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import mosek.*

abstract class MosekSolver : AutoCloseable {
    protected lateinit var env: Env
    protected lateinit var mosekModel: Task
    protected lateinit var status: SolverStatus

    override fun close() {
        mosekModel.close()
        env.close()
    }

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

                solsta.prim_infeas_cer -> {
                    SolverStatus.Infeasible
                }

                else -> {
                    SolverStatus.SolvingException
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
