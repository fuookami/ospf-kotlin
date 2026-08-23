package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

class MipBackedConstraintProgrammingSolverTest {
    @Test
    fun shouldReevaluateLargeIntegerObjectiveFromExactCpSolution() = runBlocking {
        val coefficient = 4_600_000_000_000_000L
        val exactObjective = coefficient * 2L
        val model = ConstraintProgrammingModel("mip-large-objective", ObjectCategory.Minimum)
        try {
            val first = fuookami.ospf.kotlin.core.variable.IntVar("x")
            val second = fuookami.ospf.kotlin.core.variable.IntVar("y")
            model.registerVariable(first, IntegerDomain.interval(0, 1).value!!)
            model.registerVariable(second, IntegerDomain.interval(0, 1).value!!)
            model.minimize(
                ConstraintProgrammingExpression.linear(
                    mapOf(
                        first to Int64(coefficient),
                        second to Int64(coefficient)
                    )
                ).value!!
            )

            val result = MipBackedConstraintProgrammingSolver(
                LargeObjectiveLinearSolver(exactObjective)
            ).solve(model)
            val failureMessage = (result as? fuookami.ospf.kotlin.utils.functional.Failed<*, *, *>)
                ?.error
                ?.message
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<fuookami.ospf.kotlin.utils.functional.Ok<*, *, *>>(result, failureMessage ?: result.toString()).value
            )
            assertEquals(Int64(exactObjective), output.report!!.solution!!.objective)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldMapLinearReportAndForwardHintsAndAssumptions() = runBlocking {
        val model = ConstraintProgrammingModel("mip-backed", ObjectCategory.Minimum)
        try {
            val x = fuookami.ospf.kotlin.core.variable.IntVar("x")
            model.registerVariable(x, IntegerDomain.interval(0, 5).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.zero).value!!)
            model.minimize(expression)

            val linear = RecordingLinearSolver()
            val solver = MipBackedConstraintProgrammingSolver(linear)
            val session = assertIs<ConstraintProgrammingSession>(solver.createSession(model).value)
            try {
                val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<fuookami.ospf.kotlin.utils.functional.Ok<*, *, *>>(session.solve()).value
                )
                assertEquals(Int64(4), feasible.solution.value(x).value)
                assertEquals(SolverStatus.Optimal, feasible.status)

                val hinted = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<fuookami.ospf.kotlin.utils.functional.Ok<*, *, *>>(
                        session.solve(hints = ConstraintProgrammingSolution(mapOf(
                            fuookami.ospf.kotlin.core.solver.report.VariableId("${x.identifier}:${x.index}") to Int64(2)
                        )))
                    ).value
                )
                assertEquals(Int64(4), hinted.solution.value(x).value)
                assertTrue(linear.sawHint)

                val infeasible = assertIs<ConstraintProgrammingInfeasibleOutput>(
                    assertIs<fuookami.ospf.kotlin.utils.functional.Ok<*, *, *>>(
                        session.solve(assumptions = listOf(BooleanLiteral.False))
                    ).value
                )
                assertEquals(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified, infeasible.proofStatus)
                assertEquals(3, linear.solveCalls)
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldMapUnknownLinearReport() = runBlocking {
        val model = ConstraintProgrammingModel("mip-unknown")
        try {
            val x = fuookami.ospf.kotlin.core.variable.IntVar("x")
            model.registerVariable(x, IntegerDomain.interval(0, 1).value!!)
            val linear = RecordingLinearSolver(unknown = true)
            val output = MipBackedConstraintProgrammingSolver(linear).solve(model)
            val unknown = assertIs<ConstraintProgrammingUnknownOutput>(
                assertIs<fuookami.ospf.kotlin.utils.functional.Ok<*, *, *>>(output).value
            )
            assertEquals(TerminationReason.TimeLimit, unknown.terminationReason)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldValidateAndRestoreOptionalIntervalHints() = runBlocking {
        val model = ConstraintProgrammingModel("mip-interval-hint")
        try {
            val start = fuookami.ospf.kotlin.core.variable.IntVar("hint-start")
            val end = fuookami.ospf.kotlin.core.variable.IntVar("hint-end")
            val present = fuookami.ospf.kotlin.core.variable.BinVar("hint-present")
            model.registerVariable(start, IntegerDomain.interval(0, 5).value!!)
            model.registerVariable(end, IntegerDomain.interval(0, 5).value!!)
            model.registerVariable(present, IntegerDomain.boolean)
            val interval = IntervalVariable.fixed(
                id = IntervalId("hinted"),
                start = ConstraintProgrammingExpression.Variable(start),
                size = Int64(2),
                end = ConstraintProgrammingExpression.Variable(end),
                presence = BooleanLiteral(present)
            ).value!!
            model.registerInterval(interval)

            val session = assertIs<ConstraintProgrammingSession>(
                MipBackedConstraintProgrammingSolver(HintEchoLinearSolver()).createSession(model).value
            )
            try {
                val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<fuookami.ospf.kotlin.utils.functional.Ok<*, *, *>>(
                        session.solve(
                            hints = ConstraintProgrammingSolution(
                                intervals = mapOf(
                                    IntervalId("hinted") to IntervalValue(
                                        start = Int64(1),
                                        size = Int64(2),
                                        end = Int64(3),
                                        present = true
                                    )
                                )
                            )
                        )
                    ).value
                )
                assertEquals(
                    IntervalValue(Int64(1), Int64(2), Int64(3), present = true),
                    output.solution.interval(IntervalId("hinted")).value
                )
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }
}

private class RecordingLinearSolver(
    private val unknown: Boolean = false
) : LinearSolver {
    override val config: SolverConfig = SolverConfig()
    override val name: String = "recording-mip"
    var solveCalls: Int = 0
    var sawHint: Boolean = false

    override suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext?
    ): Ret<SolveReport<Flt64>> {
        ++solveCalls
        sawHint = sawHint || model.variables.any { it.initialResult == Flt64(2) }
        if (unknown) {
            return ok(
                SolveReport(
                    problemStatus = ProblemStatus.Unknown,
                    terminationReason = TerminationReason.TimeLimit,
                    solutionPresence = SolutionPresence.None
                )
            )
        }
        if (model.constraints.names.any { it.startsWith("cp-assumption") }) {
            return ok(
                SolveReport(
                    problemStatus = ProblemStatus.Infeasible,
                    terminationReason = TerminationReason.Completed,
                    solutionPresence = SolutionPresence.None,
                    proof = SolveProof(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified)
                )
            )
        }
        return ok(
            SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                solution = SolveSolution(
                    values = model.variables.indices.map { index -> if (index == 0) Flt64(4) else Flt64.zero },
                    objective = Flt64(4)
                ),
                proof = SolveProof(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified)
            )
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        error("solveReport is the test boundary")
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        error("solveReport is the test boundary")
    }
}

private class HintEchoLinearSolver : LinearSolver {
    override val config: SolverConfig = SolverConfig()
    override val name: String = "hint-echo-mip"

    override suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext?
    ): Ret<SolveReport<Flt64>> {
        return ok(
            SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                solution = SolveSolution(
                    values = model.variables.map { it.initialResult ?: Flt64.zero }
                ),
                proof = SolveProof(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified)
            )
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        error("solveReport is the test boundary")
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        error("solveReport is the test boundary")
    }
}

private class LargeObjectiveLinearSolver(
    private val exactObjective: Long
) : LinearSolver {
    override val config: SolverConfig = SolverConfig()
    override val name: String = "large-objective-mip"

    override suspend fun solveReport(
        model: LinearTriadModelView,
        progressContext: fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext?
    ): Ret<SolveReport<Flt64>> {
        return ok(
            SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                solution = SolveSolution(
                    values = model.variables.map { Flt64.one },
                    objective = Flt64(exactObjective.toDouble())
                ),
                proof = SolveProof(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified)
            )
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        error("solveReport is the test boundary")
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        error("solveReport is the test boundary")
    }
}
