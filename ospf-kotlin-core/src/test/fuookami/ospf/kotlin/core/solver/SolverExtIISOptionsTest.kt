@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.intermediate_model.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.intermediate_model.BasicQuadraticTetradModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearConstraintBatch
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticConstraintBatch
import fuookami.ospf.kotlin.core.intermediate_model.SparseMatrixF64
import fuookami.ospf.kotlin.core.intermediate_model.SparseQuadraticMatrix
import fuookami.ospf.kotlin.core.intermediate_model.LinearObjective
import fuookami.ospf.kotlin.core.intermediate_model.LinearTriadModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticConstraint
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticObjective
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticTetradModel
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.intermediate_model.Variable
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.LinearInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.QuadraticInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.intermediate_model.ObjectCategory
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class SolverExtIISOptionsTest {
    @Test
    fun linearIisSolutionPoolShouldForwardOptionsAndCallback() = runBlocking {
        val solver = RecordingLinearSolver()
        val callbackStates = ArrayList<SolvingStatus>()
        val callback: SolvingStatusCallBack = { status ->
            callbackStates.add(status)
            ok
        }

        val result = solver.solveWithOptionsAndIISForSolutionPool(
            model = emptyLinearModel(),
            options = SolveOptions(
                solutionAmount = UInt64(3),
                solvingStatusCallBack = callback
            ),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val value = assertNotNull(result.value)
                assertEquals(UInt64(3), solver.lastSolutionAmount)
                assertEquals(1, callbackStates.size)
                assertEquals(1, value.second.size)
            }

            else -> {
                fail("expected solveWithOptionsAndIISForSolutionPool to succeed, got $result")
            }
        }
    }

    @Test
    fun linearIisSolutionPoolShouldFallbackToSingleSolveWhenSolutionAmountMissing() = runBlocking {
        val solver = RecordingLinearSolver()

        val result = solver.solveWithOptionsAndIISForSolutionPool(
            model = emptyLinearModel(),
            options = SolveOptions(),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val value = assertNotNull(result.value)
                assertNull(solver.lastSolutionAmount)
                assertEquals(0, value.second.size)
            }

            else -> {
                fail("expected solveWithOptionsAndIISForSolutionPool fallback to succeed, got $result")
            }
        }
    }

    @Test
    fun quadraticIisSingleSolveShouldForwardCallback() = runBlocking {
        val solver = RecordingQuadraticSolver()
        var callbackInvoked = 0
        val callback: SolvingStatusCallBack = {
            callbackInvoked += 1
            ok
        }

        val result = solver.solveWithOptionsAndIIS(
            model = emptyQuadraticModel(),
            options = SolveOptions(
                solvingStatusCallBack = callback
            ),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                assertNotNull(result.value)
                assertNull(solver.lastSolutionAmount)
                assertEquals(1, callbackInvoked)
            }

            else -> {
                fail("expected solveWithOptionsAndIIS to succeed, got $result")
            }
        }
    }

    @Test
    fun linearIisSingleSolveShouldFillUnifiedFieldsFromLatestStatus() = runBlocking {
        val latestStatus = dummyStatus("infeasible-linear").copy(
            iterations = UInt64(12),
            nodeCount = UInt64(7),
            bestBound = Flt64(8),
            mipGap = Flt64(0.2),
            solveTime = 4.seconds
        )
        val solver = InfeasibleThenFeasibleLinearSolver(
            statusOnFailure = latestStatus,
            emitStatusOnFailure = true
        )

        val result = solver.solveWithOptionsAndIIS(
            model = emptyLinearModel(),
            options = SolveOptions(
                solvingStatusCallBack = { ok }
            ),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val output = result.value
                assertTrue(output is LinearInfeasibleSolverOutput)
                assertEquals(UInt64(12), output.iterations)
                assertEquals(UInt64(7), output.nodeCount)
                assertEquals(Flt64(8), output.bestBound)
                assertEquals(Flt64(0.2), output.mipGap)
                assertEquals(4.seconds, output.solveTime)
            }

            else -> {
                fail("expected linear IIS output, got $result")
            }
        }
    }

    @Test
    fun linearIisSingleSolveShouldFallbackSolveTimeWhenNoLatestStatus() = runBlocking {
        val solver = InfeasibleThenFeasibleLinearSolver(
            statusOnFailure = null,
            emitStatusOnFailure = false
        )

        val result = solver.solveWithOptionsAndIIS(
            model = emptyLinearModel(),
            options = SolveOptions(),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val output = result.value
                assertTrue(output is LinearInfeasibleSolverOutput)
                assertNull(output.iterations)
                assertNull(output.nodeCount)
                assertNull(output.bestBound)
                assertNull(output.mipGap)
                assertTrue(output.solveTime != null)
            }

            else -> {
                fail("expected linear IIS output with fallback solveTime, got $result")
            }
        }
    }

    @Test
    fun linearIisShouldUseDeletionFilteringWhenElasticFilteringFails() = runBlocking {
        val solver = InfeasibleThenDeletionFilteringLinearSolver()

        val result = solver.solveWithOptionsAndIIS(
            model = boundedLinearModel(),
            options = SolveOptions(),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val output = result.value
                assertTrue(output is LinearInfeasibleSolverOutput)
                assertEquals(6, solver.invokeCount)
                assertEquals(1, output.iis.variables.size)
                assertEquals(0, output.iis.constraints.size)
                assertEquals(Flt64.zero, output.iis.variables[0].lowerBound)
                assertEquals(Flt64.infinity, output.iis.variables[0].upperBound)
            }

            else -> {
                fail("expected linear IIS output from deletion filtering, got $result")
            }
        }
    }

    @Test
    fun quadraticIisSingleSolveShouldFallbackToSnapshotModelAndFillUnifiedFields() = runBlocking {
        val latestStatus = dummyStatus("infeasible-quadratic").copy(
            iterations = UInt64(21),
            nodeCount = UInt64(13),
            bestBound = Flt64(6),
            mipGap = Flt64(0.4),
            solveTime = 9.seconds
        )
        val solver = InfeasibleQuadraticSolver(
            statusOnFailure = latestStatus,
            emitStatusOnFailure = true
        )

        val result = solver.solveWithOptionsAndIIS(
            model = emptyQuadraticModel(),
            options = SolveOptions(
                solvingStatusCallBack = { ok }
            ),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val output = result.value
                assertTrue(output is QuadraticInfeasibleSolverOutput)
                assertEquals(UInt64(21), output.iterations)
                assertEquals(UInt64(13), output.nodeCount)
                assertEquals(Flt64(6), output.bestBound)
                assertEquals(Flt64(0.4), output.mipGap)
                assertEquals(9.seconds, output.solveTime)
            }

            else -> {
                fail("expected quadratic IIS output fallback, got $result")
            }
        }
    }

    @Test
    fun quadraticIisSingleSolveShouldUseElasticFilteringResultWhenAvailable() = runBlocking {
        val solver = InfeasibleThenElasticFeasibleQuadraticSolver()

        val result = solver.solveWithOptionsAndIIS(
            model = boundedQuadraticModel(),
            options = SolveOptions(),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val output = result.value
                assertTrue(output is QuadraticInfeasibleSolverOutput)
                assertEquals(2, solver.invokeCount)
                assertEquals(1, output.iis.variables.size)
                assertEquals(0, output.iis.constraints.size)
                assertEquals(Flt64.zero, output.iis.variables[0].lowerBound)
                assertEquals(Flt64.infinity, output.iis.variables[0].upperBound)
            }

            else -> {
                fail("expected quadratic IIS output from elastic filtering, got $result")
            }
        }
    }

    @Test
    fun quadraticIisShouldUseDeletionFilteringWhenElasticFilteringFails() = runBlocking {
        val solver = InfeasibleThenDeletionFilteringQuadraticSolver()

        val result = solver.solveWithOptionsAndIIS(
            model = boundedQuadraticModel(),
            options = SolveOptions(),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                val output = result.value
                assertTrue(output is QuadraticInfeasibleSolverOutput)
                assertEquals(6, solver.invokeCount)
                assertEquals(1, output.iis.variables.size)
                assertEquals(0, output.iis.constraints.size)
                assertEquals(Flt64.zero, output.iis.variables[0].lowerBound)
                assertEquals(Flt64.infinity, output.iis.variables[0].upperBound)
            }

            else -> {
                fail("expected quadratic IIS output from deletion filtering, got $result")
            }
        }
    }

    @Test
    fun quadraticIisSolutionPoolShouldReturnInfeasibleOutputAndEmptySolutions() = runBlocking {
        val solver = InfeasibleQuadraticSolver(
            statusOnFailure = null,
            emitStatusOnFailure = false
        )

        val result = solver.solveWithOptionsAndIISForSolutionPool(
            model = emptyQuadraticModel(),
            options = SolveOptions(
                solutionAmount = UInt64(2)
            ),
            iisConfig = IISConfig()
        )

        when (result) {
            is Ok -> {
                assertTrue(result.value.first is QuadraticInfeasibleSolverOutput)
                assertEquals(0, result.value.second.size)
            }

            else -> {
                fail("expected quadratic IIS solution pool fallback output, got $result")
            }
        }
    }
}

private class InfeasibleThenDeletionFilteringLinearSolver : AbstractLinearSolver {
    override val name: String = "infeasible-then-deletion-filtering-linear"
    var invokeCount: Int = 0

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        invokeCount += 1
        return when (invokeCount) {
            1, 2, 3, 4, 5 -> {
                Failed(ErrorCode.ORModelInfeasible)
            }

            else -> {
                Ok(
                    FeasibleSolverOutput(
                        obj = Flt64.zero,
                        solution = MutableList(model.variables.size) { Flt64.zero },
                        time = 1.seconds,
                        possibleBestObj = Flt64.zero,
                        gap = Flt64.zero
                    )
                )
            }
        }
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return Ok(dummyFeasibleOutput() to emptyList())
    }
}

private class InfeasibleThenDeletionFilteringQuadraticSolver : AbstractQuadraticSolver {
    override val name: String = "infeasible-then-deletion-filtering-quadratic"
    var invokeCount: Int = 0

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        invokeCount += 1
        return when (invokeCount) {
            1 -> {
                Failed(ErrorCode.ORModelInfeasible)
            }

            2, 3, 4, 5 -> {
                Failed(ErrorCode.ORModelInfeasible)
            }

            else -> {
                Ok(
                    FeasibleSolverOutput(
                        obj = Flt64.zero,
                        solution = MutableList(model.variables.size) { Flt64.zero },
                        time = 1.seconds,
                        possibleBestObj = Flt64.zero,
                        gap = Flt64.zero
                    )
                )
            }
        }
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return Ok(dummyFeasibleOutput() to emptyList())
    }
}

private class InfeasibleThenElasticFeasibleQuadraticSolver : AbstractQuadraticSolver {
    override val name: String = "infeasible-then-elastic-feasible-quadratic"
    var invokeCount: Int = 0

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        invokeCount += 1
        return if (invokeCount == 1) {
            solvingStatusCallBack?.invoke(dummyStatus(name))
            Failed(ErrorCode.ORModelInfeasible)
        } else {
            val solution = MutableList(model.variables.size) { Flt64.zero }
            model.variables.firstOrNull { it.slack?.lowerBound != null }?.let { slack ->
                solution[slack.index] = Flt64.one
            }
            Ok(
                FeasibleSolverOutput(
                    obj = Flt64.zero,
                    solution = solution,
                    time = 1.seconds,
                    possibleBestObj = Flt64.zero,
                    gap = Flt64.zero
                )
            )
        }
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return Ok(dummyFeasibleOutput() to emptyList())
    }
}

private class RecordingLinearSolver : AbstractLinearSolver {
    override val name: String = "recording-linear"
    var lastSolutionAmount: UInt64? = null

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        lastSolutionAmount = null
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(dummyFeasibleOutput())
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        lastSolutionAmount = solutionAmount
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(dummyFeasibleOutput() to listOf(listOf(Flt64(2.0))))
    }
}

private class RecordingQuadraticSolver : AbstractQuadraticSolver {
    override val name: String = "recording-quadratic"
    var lastSolutionAmount: UInt64? = null

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        lastSolutionAmount = null
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(dummyFeasibleOutput())
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        lastSolutionAmount = solutionAmount
        solvingStatusCallBack?.invoke(dummyStatus(name))
        return Ok(dummyFeasibleOutput() to listOf(listOf(Flt64(3.0))))
    }
}

private class InfeasibleThenFeasibleLinearSolver(
    private val statusOnFailure: SolvingStatus?,
    private val emitStatusOnFailure: Boolean
) : AbstractLinearSolver {
    override val name: String = "infeasible-then-feasible-linear"
    private var invokeCount: Int = 0

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        invokeCount += 1
        return if (invokeCount == 1) {
            if (emitStatusOnFailure) {
                statusOnFailure?.let { solvingStatusCallBack?.invoke(it) }
            }
            Failed(ErrorCode.ORModelInfeasible)
        } else {
            Ok(
                FeasibleSolverOutput(
                    obj = Flt64.zero,
                    solution = emptyList(),
                    time = 1.seconds,
                    possibleBestObj = Flt64.zero,
                    gap = Flt64.zero
                )
            )
        }
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return Ok(dummyFeasibleOutput() to emptyList())
    }
}

private class InfeasibleQuadraticSolver(
    private val statusOnFailure: SolvingStatus?,
    private val emitStatusOnFailure: Boolean
) : AbstractQuadraticSolver {
    override val name: String = "infeasible-quadratic"

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        if (emitStatusOnFailure) {
            statusOnFailure?.let { solvingStatusCallBack?.invoke(it) }
        }
        return Failed(ErrorCode.ORModelInfeasible)
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        if (emitStatusOnFailure) {
            statusOnFailure?.let { solvingStatusCallBack?.invoke(it) }
        }
        return Failed(ErrorCode.ORModelInfeasible)
    }
}

private fun emptyLinearModel(): LinearTriadModel {
    val constraints = LinearConstraintBatch(
        sparseLhs = SparseMatrixF64(),
        signs = emptyList(),
        rhs = emptyList(),
        names = emptyList(),
        sources = emptyList()
    )
    val basicModel = BasicLinearTriadModel(
        variables = emptyList(),
        constraints = constraints,
        name = "empty-linear"
    )
    return LinearTriadModel(
        impl = basicModel,
        tokensInSolver = emptyList(),
        objective = LinearObjective(
            category = ObjectCategory.Minimum,
            objective = emptyList()
        )
    )
}

private fun boundedLinearModel(): LinearTriadModel {
    val variable = Variable(
        index = 0,
        lowerBound = Flt64.zero,
        upperBound = Flt64.one,
        type = Continuous,
        origin = null,
        name = "x"
    )
    val constraints = LinearConstraintBatch(
        sparseLhs = SparseMatrixF64(),
        signs = emptyList(),
        rhs = emptyList(),
        names = emptyList(),
        sources = emptyList()
    )
    val basicModel = BasicLinearTriadModel(
        variables = listOf(variable),
        constraints = constraints,
        name = "bounded-linear"
    )
    return LinearTriadModel(
        impl = basicModel,
        tokensInSolver = emptyList(),
        objective = LinearObjective(
            category = ObjectCategory.Minimum,
            objective = emptyList()
        )
    )
}

private fun emptyQuadraticModel(): QuadraticTetradModel {
    val constraints = QuadraticConstraintBatch(
        sparseLhs = SparseQuadraticMatrix(),
        signs = emptyList(),
        rhs = emptyList(),
        names = emptyList(),
        sources = emptyList()
    )
    val basicModel = BasicQuadraticTetradModel(
        variables = emptyList(),
        constraints = constraints,
        name = "empty-quadratic"
    )
    return QuadraticTetradModel(
        impl = basicModel,
        tokensInSolver = emptyList(),
        objective = QuadraticObjective(
            category = ObjectCategory.Minimum,
            objective = emptyList()
        )
    )
}

private fun boundedQuadraticModel(): QuadraticTetradModel {
    val variable = Variable(
        index = 0,
        lowerBound = Flt64.zero,
        upperBound = Flt64.one,
        type = Continuous,
        origin = null,
        name = "x"
    )
    val constraints = QuadraticConstraintBatch(
        sparseLhs = SparseQuadraticMatrix(),
        signs = emptyList(),
        rhs = emptyList(),
        names = emptyList(),
        sources = emptyList()
    )
    val basicModel = BasicQuadraticTetradModel(
        variables = listOf(variable),
        constraints = constraints,
        name = "bounded-quadratic"
    )
    return QuadraticTetradModel(
        impl = basicModel,
        tokensInSolver = emptyList(),
        objective = QuadraticObjective(
            category = ObjectCategory.Minimum,
            objective = emptyList()
        )
    )
}

private fun dummyFeasibleOutput(): FeasibleSolverOutput {
    return FeasibleSolverOutput(
        obj = Flt64.one,
        solution = listOf(Flt64.one),
        time = 1.seconds,
        possibleBestObj = Flt64.one,
        gap = Flt64.zero
    )
}

private fun dummyStatus(solver: String): SolvingStatus {
    return SolvingStatus(
        solver = solver,
        solverConfig = SolverConfig(),
        objectCategory = ObjectCategory.Minimum,
        time = 1.seconds,
        obj = Flt64.one,
        possibleBestObj = Flt64.one,
        initialBestObj = Flt64.one,
        gap = Flt64.zero
    )
}
