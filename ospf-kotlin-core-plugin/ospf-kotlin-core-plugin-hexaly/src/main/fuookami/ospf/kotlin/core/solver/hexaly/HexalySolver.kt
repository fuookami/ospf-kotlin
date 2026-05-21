@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.hexaly

import com.hexaly.optimizer.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private fun <T> executeCreatingEnvironmentCallback(
    target: T,
    callBack: ((T) -> Try)?
): Try {
    return when (val result = callBack?.invoke(target)) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> ok
    }
}

private fun environmentLost(message: String?): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost, message))
private fun environmentLost(): Try = Failed(Err(ErrorCode.OREngineEnvironmentLost))
private fun solvingException(message: String?): Try = Failed(Err(ErrorCode.OREngineSolvingException, message))
private fun solvingException(): Try = Failed(Err(ErrorCode.OREngineSolvingException))
private fun terminated(): Try = Failed(Err(ErrorCode.OREngineTerminated))

@OptIn(ExperimentalTime::class)
abstract class HexalySolver : AutoCloseable {
    protected lateinit var optimizer: HexalyOptimizer
    protected lateinit var hexalyModel: HxModel
    protected lateinit var hexalySolution: HxSolution
    protected lateinit var status: SolverStatus
    protected var beginTime: Instant? = null
    protected var solvingTime: Duration? = null

    override fun close() {
        hexalyModel.close()
        optimizer.close()
    }

    protected suspend fun init(
        name: String,
        callBack: CreatingEnvironmentFunction? = null
    ): Try {
        return try {
            optimizer = HexalyOptimizer()
            when (val callbackResult = executeCreatingEnvironmentCallback(optimizer, callBack)) {
                is Failed -> return callbackResult
                is Fatal -> return callbackResult
                else -> {}
            }
            hexalyModel = optimizer.model

            ok
        } catch (e: HxException) {
            environmentLost(e.message)
        } catch (e: Exception) {
            environmentLost()
        }
    }

    protected suspend fun solve(): Try {
        return try {
            beginTime = Clock.System.now()
            hexalyModel.close()
            optimizer.solve()
            solvingTime = Clock.System.now() - beginTime!!

            ok
        } catch (e: HxException) {
            solvingException(e.message)
        } catch (e: Exception) {
            terminated()
        }
    }

    protected suspend fun analyzeStatus(): Try {
        return try {
            hexalySolution = optimizer.solution
            status = when (hexalySolution.status) {
                HxSolutionStatus.Optimal -> {
                    SolverStatus.Optimal
                }

                HxSolutionStatus.Feasible -> {
                    SolverStatus.Feasible
                }

                HxSolutionStatus.Infeasible -> {
                    SolverStatus.Infeasible
                }

                HxSolutionStatus.Inconsistent -> {
                    SolverStatus.Unbounded
                }

                else -> {
                    SolverStatus.SolvingException
                }
            }

            ok
        } catch (e: HxException) {
            solvingException(e.message)
        } catch (e: Exception) {
            solvingException()
        }
    }
}
