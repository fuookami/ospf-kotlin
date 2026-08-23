/**
 * 用于 CP 契约测试的确定性穷举求解器。 / Deterministic exhaustive solver for CP contract tests.
 */
package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.time.Duration.Companion.nanoseconds
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.evaluateInteger
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.output.toCompatibilityFlt64
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolveStatistics
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverProvenance
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId

/**
 * 不依赖具体 backend 的 CP 测试求解器。它只适合小模型和契约测试，不作为生产搜索引擎。 / / Backend-independent CP test solver. It is intended for small models and contract tests only.
 *
 * @property enumerationLimit 单个变量值域的枚举上限 / Enumeration limit per variable domain
 */
class FakeConstraintProgrammingSolver(
    private val enumerationLimit: Int = DEFAULT_ENUMERATION_LIMIT
) : ConstraintProgrammingSolver {
    override val descriptor: SolverDescriptor = SolverDescriptor(
        solverId = "fake-cp",
        backendName = "OSPF exhaustive CP test solver",
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.CP),
            interrupt = true,
            constraintProgrammingFeatures = ConstraintProgrammingFeature.entries.associateWith {
                ConstraintProgrammingSupportLevel.Native
            }
        )
    )

    override suspend fun solve(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSolverOutput> {
        return createSession(model, options).flatMapSuspendResult { session ->
            try {
                session.solve()
            } finally {
                session.close()
            }
        }
    }

    override fun createSession(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSession> {
        if (enumerationLimit <= 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "穷举规模上限必须为正 / Enumeration limit must be positive"
            )
        }
        return ok(FakeConstraintProgrammingSession(model, options, enumerationLimit))
    }

    private companion object {
        const val DEFAULT_ENUMERATION_LIMIT = 100_000
    }
}

private class FakeConstraintProgrammingSession(
    override val model: ConstraintProgrammingModel,
    override val options: ConstraintProgrammingSolveOptions,
    private val enumerationLimit: Int
) : ConstraintProgrammingSession {
    private var closed = false

    override val isClosed: Boolean
        get() = closed

    override suspend fun solve(
        assumptions: List<fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral>,
        fixedValues: Map<VariableId, Int64>,
        hints: ConstraintProgrammingSolution?
    ): Ret<ConstraintProgrammingSolverOutput> {
        if (closed) {
            return Failed(ErrorCode.ApplicationStopped, "CP 会话已关闭 / CP session is closed")
        }
        val started = System.nanoTime()
        val snapshot = model.snapshot()
        if (snapshot.failed) {
            return propagate(snapshot)
        }
        val checked = validateOptions(options)
        if (checked.failed) {
            return propagate(checked)
        }
        val current = snapshot.value!!
        val assumptionError = assumptions.firstOrNull { it.variableId != null && current.variable(it.variableId!!) == null }
        if (assumptionError != null) {
            return Failed(
                ErrorCode.IllegalArgument,
                "assumption 引用了未注册变量 / Assumption references an unregistered variable"
            )
        }

        val completedHintValues = LinkedHashMap<VariableId, Int64>()
        for ((id, value) in fixedValues) {
            val variable = current.variable(id)
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "固定值引用了未知变量：$id / Fixed value references an unknown variable: $id"
                )
            if (!variable.domain.contains(value)) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "固定值超出变量值域：$id=$value / Fixed value is outside the variable domain: $id=$value"
                )
            }
            completedHintValues[id] = value
        }
        if (hints != null) {
            for ((id, value) in hints.values) {
                val variable = current.variable(id)
                    ?: return Failed(
                        ErrorCode.DataNotFound,
                        "CP hint 引用了未知变量：$id / CP hint references an unknown variable: $id"
                    )
                if (!variable.domain.contains(value)) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "CP hint 超出变量值域：$id=$value / CP hint is outside the variable domain: $id=$value"
                    )
                }
                val existing = completedHintValues[id]
                if (existing != null && existing != value) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "CP hint 与固定值冲突：$id / CP hint conflicts with a fixed value: $id"
                    )
                }
                completedHintValues[id] = value
            }
            for ((id, intervalValue) in hints.intervals) {
                val interval = current.intervals.firstOrNull { it.id == id }
                    ?: return Failed(
                        ErrorCode.DataNotFound,
                        "CP hint 引用了未知 interval：$id / CP hint references an unknown interval: $id"
                    )
                interval.presence?.variableId?.let { presenceId ->
                    val presenceValue = if (intervalValue.present) Int64.one else Int64.zero
                    val existing = completedHintValues[presenceId]
                    if (existing != null && existing != presenceValue) {
                        return Failed(
                            ErrorCode.ORSolutionInvalid,
                            "CP interval hint 与 presence literal 冲突：$id / " +
                                "CP interval hint conflicts with its presence literal: $id"
                        )
                    }
                    if (existing == null) {
                        completedHintValues[presenceId] = presenceValue
                    }
                }
                if (intervalValue.present) {
                    val start = interval.start as? ConstraintProgrammingExpression.Variable
                        ?: return Failed(
                            ErrorCode.ORSolutionInvalid,
                            "CP interval hint 只能注入标量起点：$id / CP interval hints require a scalar start variable: $id"
                        )
                    val end = interval.end as? ConstraintProgrammingExpression.Variable
                        ?: return Failed(
                            ErrorCode.ORSolutionInvalid,
                            "CP interval hint 只能注入标量终点：$id / CP interval hints require a scalar end variable: $id"
                        )
                    val size = interval.size as? ConstraintProgrammingExpression.Variable
                    val startResult = putHint(completedHintValues, start.variableId, intervalValue.start, current)
                    if (startResult.failed) return propagate(startResult)
                    val endResult = putHint(completedHintValues, end.variableId, intervalValue.end, current)
                    if (endResult.failed) return propagate(endResult)
                    if (size != null) {
                        val sizeResult = putHint(completedHintValues, size.variableId, intervalValue.size, current)
                        if (sizeResult.failed) return propagate(sizeResult)
                    }
                }
                val evaluated = interval.evaluate(completedHintValues)
                if (evaluated.failed) return propagate(evaluated)
                if (evaluated.value!! != intervalValue) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "CP interval hint 不满足 interval 定义：$id / CP interval hint does not satisfy the interval definition: $id"
                    )
                }
            }
        }

        val domains = ArrayList<Pair<VariableId, List<Int64>>>(current.variables.size)
        for (variable in current.variables) {
            val values = fixedValues[variable.id]?.let(::listOf)
                ?: variable.domain.enumerate(enumerationLimit)
                ?: return ok(
                    ConstraintProgrammingUnknownOutput(
                        terminationReason = TerminationReason.NodeLimit,
                        report = report(
                            current,
                            started,
                            ProblemStatus.Unknown,
                            TerminationReason.NodeLimit,
                            SolutionPresence.None,
                            null,
                            null,
                            ProofStatus.None
                        )
                    )
                )
            domains += variable.id to values
        }
        var assignmentCount = 0
        var best: Candidate? = null
        var stopped = false
        var evaluationFailure: Ret<*>? = null

        fun recordEvaluationFailure(result: Ret<*>) {
            evaluationFailure = result
            stopped = true
        }

        fun visit(index: Int, values: LinkedHashMap<VariableId, Int64>) {
            if (stopped) {
                return
            }
            if (options.cancellationToken?.isCancellationRequested == true) {
                stopped = true
                return
            }
            if (options.timeLimit != null &&
                System.nanoTime() - started >= options.timeLimit.inWholeNanoseconds
            ) {
                stopped = true
                return
            }
            if (index == domains.size) {
                ++assignmentCount
                if (options.solutionLimit != null && assignmentCount > options.solutionLimit.toLong()) {
                    stopped = true
                    return
                }
                for (literal in assumptions) {
                    when (val evaluated = literal.evaluateInteger(values)) {
                        is Ok -> if (evaluated.value != true) return
                        is Failed,
                        is Fatal -> {
                            recordEvaluationFailure(evaluated)
                            return
                        }
                    }
                }
                val intervalValues = LinkedHashMap<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue>()
                for (interval in current.intervals) {
                    when (val evaluated = interval.evaluate(values)) {
                        is Ok -> intervalValues[interval.id] = evaluated.value!!
                        is Failed,
                        is Fatal -> {
                            recordEvaluationFailure(evaluated)
                            return
                        }
                    }
                }
                for (entry in current.constraints) {
                    when (val evaluated = entry.constraint.isSatisfied(values)) {
                        is Ok -> if (evaluated.value != true) return
                        is Failed,
                        is Fatal -> {
                            recordEvaluationFailure(evaluated)
                            return
                        }
                    }
                }
                val objective = objectiveValue(current, values)
                if (objective.failed) {
                    recordEvaluationFailure(objective)
                    stopped = true
                    return
                }
                val candidate = Candidate(LinkedHashMap(values), intervalValues, objective.value)
                if (best == null || better(current, candidate, best!!)) {
                    best = candidate
                }
                return
            }
            val (id, domain) = domains[index]
            for (value in domain) {
                values[id] = value
                visit(index + 1, values)
                values.remove(id)
                if (stopped) {
                    return
                }
            }
        }

        if (completedHintValues.isNotEmpty() && completedHintValues.keys.containsAll(domains.map { it.first })) {
            val hintAssignment = LinkedHashMap(completedHintValues)
            val hintIntervals = LinkedHashMap<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue>()
            var validHint = true
            for (interval in current.intervals) {
                when (val evaluated = interval.evaluate(hintAssignment)) {
                    is Ok -> hintIntervals[interval.id] = evaluated.value!!
                    is Failed,
                    is Fatal -> {
                        return propagate(evaluated)
                    }
                }
            }
            for (literal in assumptions) {
                when (val evaluated = literal.evaluateInteger(hintAssignment)) {
                    is Ok -> if (evaluated.value != true) validHint = false
                    is Failed,
                    is Fatal -> return propagate(evaluated)
                }
            }
            for (entry in current.constraints) {
                when (val evaluated = entry.constraint.isSatisfied(hintAssignment)) {
                    is Ok -> if (evaluated.value != true) validHint = false
                    is Failed,
                    is Fatal -> return propagate(evaluated)
                }
            }
            if (validHint) {
                val objective = objectiveValue(current, hintAssignment)
                when (objective) {
                    is Ok -> best = Candidate(hintAssignment, hintIntervals, objective.value)
                    is Failed,
                    is Fatal -> return propagate(objective)
                }
            }
        }
        visit(0, LinkedHashMap())

        evaluationFailure?.let { return propagate(it) }

        if (best != null) {
            val output = ConstraintProgrammingFeasibleOutput(
                solution = ConstraintProgrammingSolution(best!!.values, best!!.intervals),
                    objective = best!!.objective?.toCompatibilityFlt64(),
                    bestBound = best!!.objective?.toCompatibilityFlt64(),
                status = if (stopped) SolverStatus.Feasible else SolverStatus.Optimal,
                proofStatus = if (stopped) ProofStatus.None else ProofStatus.Verified,
                exactObjective = best!!.objective,
                report = report(
                    current,
                    started,
                    if (stopped) ProblemStatus.Feasible else ProblemStatus.Feasible,
                    if (stopped) TerminationReason.TimeLimit else TerminationReason.Completed,
                    if (stopped) SolutionPresence.Incumbent else SolutionPresence.Optimal,
                    ConstraintProgrammingSolution(best!!.values, best!!.intervals),
                    best!!.objective,
                    if (stopped) ProofStatus.None else ProofStatus.Verified
                )
            )
            return ok(output)
        }
        if (stopped) {
            return ok(
                ConstraintProgrammingUnknownOutput(
                    terminationReason = if (options.cancellationToken?.isCancellationRequested == true) {
                        TerminationReason.Cancelled
                    } else {
                        TerminationReason.TimeLimit
                    },
                    report = report(
                        current,
                        started,
                        ProblemStatus.Unknown,
                        if (options.cancellationToken?.isCancellationRequested == true) {
                            TerminationReason.Cancelled
                        } else {
                            TerminationReason.TimeLimit
                        },
                        SolutionPresence.None,
                        null,
                        null,
                        ProofStatus.None
                    )
                )
            )
        }
        val conflict = ConstraintProgrammingConflict(
            constraintIds = current.constraints.mapTo(linkedSetOf()) { it.id },
            message = "所有 CP 赋值均违反约束 / Every CP assignment violates the constraints"
        )
        return ok(
            ConstraintProgrammingInfeasibleOutput(
                conflict = conflict,
                proofStatus = ProofStatus.Verified,
                report = report(
                    current,
                    started,
                    ProblemStatus.Infeasible,
                    TerminationReason.Completed,
                    SolutionPresence.None,
                    null,
                    null,
                    ProofStatus.Verified
                )
            )
        )
    }

    override fun close() {
        closed = true
    }

    private fun objectiveValue(
        snapshot: ConstraintProgrammingModelSnapshot,
        values: Map<VariableId, Int64>
    ): Ret<Int64?> {
        val objective = snapshot.objectives.firstOrNull() ?: return ok(null)
        return objective.expression.evaluate(values).map { it }
    }

    private fun better(
        snapshot: ConstraintProgrammingModelSnapshot,
        candidate: Candidate,
        incumbent: Candidate
    ): Boolean {
        if (candidate.objective == null) {
            return false
        }
        if (incumbent.objective == null) {
            return true
        }
        return when (snapshot.objectCategory) {
            fuookami.ospf.kotlin.core.model.basic.ObjectCategory.Minimum -> candidate.objective < incumbent.objective
            fuookami.ospf.kotlin.core.model.basic.ObjectCategory.Maximum -> candidate.objective > incumbent.objective
        }
    }

    private fun report(
        snapshot: ConstraintProgrammingModelSnapshot,
        started: Long,
        status: ProblemStatus,
        termination: TerminationReason,
        presence: SolutionPresence,
        solution: ConstraintProgrammingSolution?,
        objective: Int64?,
        proof: ProofStatus
    ): SolveReport<Int64> {
        return SolveReport(
            problemStatus = status,
            terminationReason = termination,
            solutionPresence = presence,
            solution = solution?.let {
                SolveSolution(
                    values = it.asList(snapshot.variables.map { variable -> variable.id }),
                    objective = objective
                )
            },
            proof = SolveProof(status = proof, kind = "exhaustive-enumeration"),
            statistics = SolveStatistics(solveTime = (System.nanoTime() - started).nanoseconds),
            diagnostics = SolveDiagnostics(),
            provenance = SolverProvenance(
                descriptor = FakeConstraintProgrammingSolver().descriptor,
                deterministic = true
            )
        )
    }

    private fun validateOptions(options: ConstraintProgrammingSolveOptions): Ret<Unit> {
        if (options.timeLimit != null && options.timeLimit.isNegative()) {
            return Failed(ErrorCode.IllegalArgument, "timeLimit 不得为负 / timeLimit must not be negative")
        }
        if (options.solutionLimit != null && options.solutionLimit == fuookami.ospf.kotlin.math.algebra.number.UInt64.zero) {
            return Failed(ErrorCode.IllegalArgument, "solutionLimit 必须为正 / solutionLimit must be positive")
        }
        return ok(Unit)
    }

    private fun putHint(
        values: MutableMap<VariableId, Int64>,
        id: VariableId,
        value: Int64,
        snapshot: ConstraintProgrammingModelSnapshot
    ): Ret<Unit> {
        val variable = snapshot.variable(id)
            ?: return Failed(
                ErrorCode.DataNotFound,
                "CP hint 引用了未知变量：$id / CP hint references an unknown variable: $id"
            )
        if (!variable.domain.contains(value)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "CP hint 超出变量值域：$id=$value / CP hint is outside the variable domain: $id=$value"
            )
        }
        val existing = values[id]
        if (existing != null && existing != value) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "CP hint 变量值冲突：$id / CP hint has conflicting values: $id"
            )
        }
        values[id] = value
        return ok(Unit)
    }

    private data class Candidate(
        val values: LinkedHashMap<VariableId, Int64>,
        val intervals: LinkedHashMap<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue>,
        val objective: Int64?
    )
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "CP fake solver result state invalid")
    }
}

private suspend fun <T> Ret<T>.flatMapSuspendResult(
    block: suspend (T) -> Ret<ConstraintProgrammingSolverOutput>
): Ret<ConstraintProgrammingSolverOutput> {
    return when (this) {
        is fuookami.ospf.kotlin.utils.functional.Ok -> block(value)
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}
