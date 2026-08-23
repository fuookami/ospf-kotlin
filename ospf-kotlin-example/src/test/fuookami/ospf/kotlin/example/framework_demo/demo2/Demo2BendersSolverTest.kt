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
    fun `benders returns Failed after multiple non-converging iterations`() = runBlocking {
        val fixedVariable = URealVar("non_converging_assignment")
        val objectVariable = URealVar("non_converging_theta")
        val masterModel = LinearMetaModel<Flt64>(
            name = "benders_non_converging_master",
            converter = IntoValue.Identity
        )
        masterModel.add(fixedVariable)
        masterModel.add(objectVariable)
        val subModel = LinearMetaModel<Flt64>(
            name = "benders_non_converging_sub",
            converter = IntoValue.Identity
        )

        val result = BendersSolver.solve(
            solver = NonConvergingBendersSolver(),
            masterModel = masterModel,
            subModel = subModel,
            fixedVariables = mapOf(fixedVariable to Flt64.zero),
            objectVariable = objectVariable,
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 2,
                tolerance = 1e-6
            ),
            notes = mutableListOf()
        )

        when (result) {
            is Ok -> fail("non-converging Benders must not return an empty successful solution")
            is Failed -> {
                assertTrue(result.error.message.contains("未在 2 次迭代内收敛"))
                assertTrue(result.error.message.contains("最终主子问题 gap=1.0"))
            }
            is Fatal -> fail("non-convergence is a recoverable Benders failure")
        }
    }

    @Test
    fun `benders fails when the objective variable token is missing`() = runBlocking {
        val masterModel = LinearMetaModel<Flt64>(
            name = "benders_missing_objective_token_master",
            converter = IntoValue.Identity
        )
        val result = BendersSolver.solve(
            solver = CapturingBendersSolver(),
            masterModel = masterModel,
            subModel = LinearMetaModel(
                name = "benders_missing_objective_token_sub",
                converter = IntoValue.Identity
            ),
            fixedVariables = emptyMap(),
            objectVariable = URealVar("missing_theta"),
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 1,
                tolerance = 1e-6
            ),
            notes = mutableListOf()
        )

        assertTrue(result is Failed)
        assertTrue((result as Failed<*, *, *>).error.message.contains("缺少目标变量"))
    }

    @Test
    fun `benders records the real master-subproblem gap for every iteration`() = runBlocking {
        val fixedVariable = URealVar("multi_round_assignment")
        val objectVariable = URealVar("multi_round_theta")
        val masterModel = LinearMetaModel<Flt64>(
            name = "benders_multi_round_master",
            converter = IntoValue.Identity
        )
        masterModel.add(fixedVariable)
        masterModel.add(objectVariable)
        val subModel = LinearMetaModel<Flt64>(
            name = "benders_multi_round_sub",
            converter = IntoValue.Identity
        )

        val result = BendersSolver.solve(
            solver = MultiRoundBendersSolver(),
            masterModel = masterModel,
            subModel = subModel,
            fixedVariables = mapOf(fixedVariable to Flt64.zero),
            objectVariable = objectVariable,
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 2,
                tolerance = 0.01
            ),
            notes = mutableListOf()
        )

        val bendersResult = when (result) {
            is Ok -> result.value ?: fail("Benders result must not be null")
            is Failed -> fail("Benders should converge in the second iteration: ${result.error.message}")
            is Fatal -> fail("Benders should converge in the second iteration: ${result.firstError?.message}")
        }
        assertEquals(10.0, bendersResult.obj, 1e-9)
        assertEquals(0.0, bendersResult.gap, 1e-9)
        assertEquals(2, bendersResult.bendersIterations)
        val snapshots = bendersResult.runtimeMetrics!!.iterationSnapshots
        assertEquals(2, snapshots.size)
        assertEquals(10.0, snapshots[0].masterObj, 1e-9)
        assertEquals(8.0, snapshots[0].subObj!!, 1e-9)
        assertEquals(9.0, snapshots[0].objectValue!!, 1e-9)
        assertEquals(1.0 / 9.0, snapshots[0].gap, 1e-9)
        assertEquals(10.0, snapshots[1].masterObj, 1e-9)
        assertEquals(9.99, snapshots[1].subObj!!, 1e-9)
        assertEquals(9.99, snapshots[1].objectValue!!, 1e-9)
        assertEquals(0.0, snapshots[1].gap, 1e-9)
    }

    @Test
    fun `objective stall detection is independent of minimization direction`() = runBlocking {
        val fixedVariable = URealVar("descending_assignment")
        val objectVariable = URealVar("descending_theta")
        val masterModel = LinearMetaModel<Flt64>(
            name = "benders_descending_master",
            converter = IntoValue.Identity
        )
        masterModel.add(fixedVariable)
        masterModel.add(objectVariable)
        val subModel = LinearMetaModel<Flt64>(
            name = "benders_descending_sub",
            converter = IntoValue.Identity
        )
        val solver = DescendingObjectiveBendersSolver()

        val result = BendersSolver.solve(
            solver = solver,
            masterModel = masterModel,
            subModel = subModel,
            fixedVariables = mapOf(fixedVariable to Flt64.zero),
            objectVariable = objectVariable,
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 4,
                tolerance = 1e-6,
                maxStallIterations = 10,
                objectiveStallIterations = 2
            ),
            notes = mutableListOf()
        )

        assertTrue(result is Failed)
        assertEquals(4, solver.masterCalls)
        assertTrue((result as Failed<*, *, *>).error.message.contains("未在 4 次迭代内收敛"))
    }

    @Test
    fun `objective stall does not stop iterations while new cuts are added`() = runBlocking {
        val fixedVariable = URealVar("cut_stall_assignment")
        val objectVariable = URealVar("cut_stall_theta")
        val masterModel = LinearMetaModel<Flt64>(
            name = "benders_cut_stall_master",
            converter = IntoValue.Identity
        )
        masterModel.add(fixedVariable)
        masterModel.add(objectVariable)

        val solver = AddingCutBendersSolver()
        val result = BendersSolver.solve(
            solver = solver,
            masterModel = masterModel,
            subModel = LinearMetaModel(
                name = "benders_cut_stall_sub",
                converter = IntoValue.Identity
            ),
            fixedVariables = mapOf(fixedVariable to Flt64.zero),
            objectVariable = objectVariable,
            config = EffectiveBendersAdaptiveConfig(
                minBinaryVariables = 1,
                maxIterations = 3,
                tolerance = 1e-6,
                maxStallIterations = 10,
                objectiveStallIterations = 1
            ),
            notes = mutableListOf()
        )

        assertTrue(result is Failed)
        assertEquals(3, solver.masterCalls)
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

    private class NonConvergingBendersSolver : LinearBendersDecompositionSolver {
        override val name: String = "non-converging-benders"

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolverOutput> {
            return Ok(output(Flt64.one, Flt64.one))
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
            return Ok(
                LinearBendersDecompositionSolver.LinearFeasibleResult(
                    result = output(Flt64.zero, Flt64.zero),
                    dualSolution = emptyMap(),
                    cuts = null
                )
            )
        }

        private fun output(obj: Flt64, objectValue: Flt64): SolveReport<Flt64> {
            return SolverStatus.Optimal.toSolveReport(
                objective = obj,
                values = listOf(Flt64.zero, objectValue),
                solveTime = Duration.ZERO,
                bestBound = obj,
                gap = Flt64.zero
            )
        }
    }

    private class MultiRoundBendersSolver : LinearBendersDecompositionSolver {
        override val name: String = "multi-round-benders"
        private var iteration = 0

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolverOutput> {
            iteration++
            return Ok(output(Flt64(10.0), Flt64(0.75)))
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
            val subObj = if (iteration == 1) Flt64(8.0) else Flt64(9.99)
            return Ok(
                LinearBendersDecompositionSolver.LinearFeasibleResult(
                    result = output(subObj, Flt64(0.75)),
                    dualSolution = emptyMap(),
                    cuts = null
                )
            )
        }

        private fun output(obj: Flt64, gap: Flt64): SolveReport<Flt64> {
            return SolverStatus.Optimal.toSolveReport(
                objective = obj,
                values = listOf(Flt64(0.75), if (iteration == 1) Flt64(9.0) else Flt64(9.99)),
                solveTime = Duration.ZERO,
                bestBound = obj,
                gap = gap
            )
        }
    }

    private class AddingCutBendersSolver : LinearBendersDecompositionSolver {
        override val name: String = "adding-cut-benders"
        var masterCalls: Int = 0

        private val cut = LinearInequality(
            lhs = LinearPolynomial(emptyList(), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64.one),
            comparison = Comparison.LE
        )

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolverOutput> {
            masterCalls++
            return Ok(output(Flt64(10.0)))
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
            return Ok(
                LinearBendersDecompositionSolver.LinearFeasibleResult(
                    result = output(Flt64.zero),
                    dualSolution = emptyMap(),
                    cuts = listOf(cut)
                )
            )
        }

        private fun output(obj: Flt64): SolveReport<Flt64> {
            return SolverStatus.Optimal.toSolveReport(
                objective = obj,
                values = listOf(Flt64.zero, Flt64(10.0)),
                solveTime = Duration.ZERO,
                bestBound = obj,
                gap = Flt64.zero
            )
        }
    }

    private class DescendingObjectiveBendersSolver : LinearBendersDecompositionSolver {
        override val name: String = "descending-objective-benders"
        var masterCalls: Int = 0

        override suspend fun solveMaster(
            name: String,
            metaModel: LinearMetaModel<Flt64>,
            toLogModel: Boolean,
            registrationStatusCallBack: RegistrationStatusCallBack?,
            solvingStatusCallBack: SolvingStatusCallBack?
        ): Ret<SolverOutput> {
            masterCalls++
            return Ok(output(Flt64(11 - masterCalls)))
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
            return Ok(
                LinearBendersDecompositionSolver.LinearFeasibleResult(
                    result = output(Flt64.zero),
                    dualSolution = emptyMap(),
                    cuts = null
                )
            )
        }

        private fun output(obj: Flt64): SolveReport<Flt64> {
            return SolverStatus.Optimal.toSolveReport(
                objective = obj,
                values = listOf(Flt64(0.75), Flt64.one),
                solveTime = Duration.ZERO,
                bestBound = obj,
                gap = Flt64.zero
            )
        }
    }
}
