package fuookami.ospf.kotlin.core.solver

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue

class LinearSolverValidationPropagationTest {
    @Test
    fun failedAndFatalValidationNeverReachAnyGenericLinearBackendEntry() = runBlocking {
        val validations: List<Try> = listOf(
            Failed(ErrorCode.IllegalArgument, "failed identity"),
            Fatal(ErrorCode.IllegalArgument, "fatal identity")
        )

        for (validation in validations) {
            val model = invalidTriad(validation)
            val solver = ValidationRecordingLinearSolver()

            val solve = solver.solve(model, IntoValue.Identity)
            val solvePool = solver.solve(model, UInt64(2), IntoValue.Identity)
            val report = solver.solveReport(model, IntoValue.Identity)
            val reportPool = solver.solveReport(model, UInt64(2), IntoValue.Identity)

            assertValidation(solve, validation)
            assertValidation(solvePool, validation)
            assertValidation(report, validation)
            assertValidation(reportPool, validation)
            assertEquals(0, solver.singleCalls)
            assertEquals(0, solver.poolCalls)
            model.close()
        }
    }

    private fun assertValidation(result: Ret<*>, expected: Try) {
        when (expected) {
            is Failed -> when (val actual = result) {
                is Failed -> assertEquals(expected.error, actual.error)
                else -> assertTrue(false, "expected Failed but got ${actual::class.simpleName}")
            }

            is Fatal -> when (val actual = result) {
                is Fatal -> assertEquals(expected.errors, actual.errors)
                else -> assertTrue(false, "expected Fatal but got ${actual::class.simpleName}")
            }

            else -> error("unexpected validation result: ${expected::class.simpleName}")
        }
    }

    private fun invalidTriad(validation: Try): LinearTriadModel {
        val constraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList()
        )
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = constraints,
                name = "invalid-identity"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(ObjectCategory.Minimum, emptyList()),
            identityValidation = validation
        )
    }
}

private class ValidationRecordingLinearSolver : AbstractLinearSolver {
    override val name: String = "identity-propagation-recording"

    var singleCalls: Int = 0
    var poolCalls: Int = 0

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        singleCalls += 1
        return Failed(ErrorCode.ApplicationError, "single backend should not be called")
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        poolCalls += 1
        return Failed(ErrorCode.ApplicationError, "pool backend should not be called")
    }
}
