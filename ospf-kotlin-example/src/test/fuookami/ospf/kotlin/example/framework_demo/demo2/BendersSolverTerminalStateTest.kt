package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.solver.report.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Assertions.assertEquals
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Assertions.assertTrue
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*

class BendersSolverTerminalStateTest {
    @Test
    fun nonOptimalMasterIsRejectedBeforeSubproblem() = runBlocking {
        val solver = NonOptimalMasterSolver()
        val result = BendersSolver.solve(
            solver = solver,
            masterModel = LinearMetaModel(name = "master", converter = IntoValue.Identity),
            subModel = LinearMetaModel(name = "sub", converter = IntoValue.Identity),
            fixedVariables = emptyMap(),
            objectVariable = RealVar("theta"),
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 1,
                tolerance = 1e-6
            ),
            notes = mutableListOf()
        )

        assertTrue(result is Failed)
        assertEquals(0, solver.subSolveCount)
    }

    private class NonOptimalMasterSolver : LinearBendersDecompositionSolver {
        override val name: String = "non-optimal-master"
        var subSolveCount: Int = 0

        private val output = SolverStatus.Feasible.toSolveReport(
            objective = Flt64.zero,
            values = emptyList(),
            solveTime = Duration.ZERO,
            bestBound = Flt64.zero,
            gap = Flt64.infinity
        )

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolverOutput> {
            return Ok(output)
        }

        override suspend fun solveSub(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            objectVariable: AbstractVariableItem<*, *>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
            subSolveCount++
            return Failed(Err(ErrorCode.ApplicationError, "Subproblem should not be called"))
        }
    }
}
