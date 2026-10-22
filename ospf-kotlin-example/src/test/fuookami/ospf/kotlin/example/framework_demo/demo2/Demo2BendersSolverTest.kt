package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.solver.report.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.*

/** 验证 Demo2 Benders 主问题解会传递给子问题。 */
class Demo2BendersSolverTest {
    @Test
    fun `benders subproblem receives current master variable values`() = runBlocking {
        val fixedVariable = URealVar("assignment")
        val objectVariable = URealVar("theta")
        val masterModel = LinearMetaModel<Flt64>(
            name = "benders_master_value_propagation",
            converter = IntoValue.Identity
        )
        masterModel.add(fixedVariable)
        masterModel.add(objectVariable)
        val subModel = LinearMetaModel<Flt64>(
            name = "benders_sub_value_propagation",
            converter = IntoValue.Identity
        )
        val solver = CapturingBendersSolver()

        val result = BendersSolver.solve(
            solver = solver,
            masterModel = masterModel,
            subModel = subModel,
            fixedVariables = mapOf(fixedVariable to Flt64.zero),
            objectVariable = objectVariable,
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 1,
                tolerance = 1e-6
            ),
            notes = mutableListOf()
        )

        assertTrue(result is Ok)
        assertEquals(Flt64(0.75), solver.fixedValues[fixedVariable])
    }

    @Test
    fun `invalid iteration budget is returned as structured failure`() = runBlocking {
        val master = LinearMetaModel<Flt64>(name = "invalid-budget", converter = IntoValue.Identity)
        val theta = URealVar("theta")
        master.add(theta)
        val result = BendersSolver.solve(
            solver = CapturingBendersSolver(), masterModel = master,
            subModel = LinearMetaModel<Flt64>(name = "sub", converter = IntoValue.Identity),
            fixedVariables = emptyMap(), objectVariable = theta,
            config = EffectiveBendersAdaptiveConfig(minBinaryVariables = 1, maxIterations = 0, tolerance = 1e-6), notes = mutableListOf()
        )
        assertTrue(result is Failed)
    }

    private class CapturingBendersSolver : LinearBendersDecompositionSolver {
        override val name: String = "capturing-benders"
        val fixedValues = mutableMapOf<AbstractVariableItem<*, *>, Flt64>()

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolverOutput> {
            return Ok(output())
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
            fixedValues.putAll(fixedVariables)
            return Ok(
                LinearBendersDecompositionSolver.LinearFeasibleResult(
                    result = output(),
                    dualSolution = emptyMap(),
                    cuts = null
                )
            )
        }

        private fun output(): SolveReport<Flt64> {
            return SolverStatus.Optimal.toSolveReport(
                objective = Flt64.zero,
                values = listOf(Flt64(0.75), Flt64.zero),
                solveTime = Duration.ZERO,
                bestBound = Flt64.zero,
                gap = Flt64.zero
            )
        }
    }

}
