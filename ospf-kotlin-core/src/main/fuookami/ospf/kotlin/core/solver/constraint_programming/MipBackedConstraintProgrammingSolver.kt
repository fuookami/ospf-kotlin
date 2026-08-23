/**
 * 基于现有线性求解器的 CP 精确降阶适配器。 / CP adapter backed by an existing linear solver.
 */
package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.output.toCompatibilityFlt64
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolveStatistics
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.VariableId

/**
 * 将 CP 能力子集精确降为线性 MIP 后调用 [linearSolver]。 / / Lower the exact CP subset to a linear MIP and invoke [linearSolver].
 *
 * 适配器不把线性辅助变量暴露到 CP 解中，也不把线性模型的对象引用泄露到 session之外。每次 solve 都重新 lower，因此 assumptions 不会污染后续求解。 / / Auxiliary linear variables are not exposed in CP solutions, and backend model references do not escape the session. Every solve lowers again so assumptions cannot contaminate a later solve.
 *
 * @property linearSolver 后端线性求解器 / Backend linear solver
 * @property lowerer CP 到线性模型的 lowerer / CP-to-linear lowerer
 */
class MipBackedConstraintProgrammingSolver(
    private val linearSolver: LinearSolver,
    private val lowerer: ConstraintProgrammingToLinearModelLowerer = ConstraintProgrammingToLinearModelLowerer()
) : ConstraintProgrammingSolver {
    override val descriptor: SolverDescriptor = SolverDescriptor(
        solverId = "mip-backed-cp-${linearSolver.name}",
        backendName = "MIP-backed CP",
        backendVersion = linearSolver.descriptor.backendVersion,
        pluginVersion = linearSolver.descriptor.pluginVersion,
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.CP),
            warmStart = linearSolver.descriptor.capabilities.warmStart,
            interrupt = linearSolver.descriptor.capabilities.interrupt,
            constraintProgrammingFeatures = mapOf(
                ConstraintProgrammingFeature.BooleanLogic to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.Reification to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.SparseDomain to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.AllDifferent to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.Element to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.Table to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.Interval to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.OptionalInterval to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.NoOverlap to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.Assumption to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.SolutionHint to if (linearSolver.descriptor.capabilities.warmStart) {
                    ConstraintProgrammingSupportLevel.ExactLowering
                } else {
                    ConstraintProgrammingSupportLevel.Unsupported
                },
                ConstraintProgrammingFeature.Cumulative to ConstraintProgrammingSupportLevel.Unsupported,
                ConstraintProgrammingFeature.ConflictCore to ConstraintProgrammingSupportLevel.Unsupported,
                ConstraintProgrammingFeature.IncrementalSolve to ConstraintProgrammingSupportLevel.ExactLowering
            )
        )
    )

    override suspend fun solve(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSolverOutput> {
        val session = createSession(model, options)
        if (session.failed) {
            return propagate(session)
        }
        val opened = session.value!!
        return try {
            opened.solve()
        } finally {
            opened.close()
        }
    }

    override fun createSession(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSession> {
        if (model.isClosed) {
            return Failed(ErrorCode.ApplicationStopped, "CP 模型已关闭 / CP model is closed")
        }
        validateOptions(options)?.let { return Failed(ErrorCode.IllegalArgument, it) }
        return ok(Session(model, options))
    }

    private inner class Session(
        override val model: ConstraintProgrammingModel,
        override val options: ConstraintProgrammingSolveOptions
    ) : ConstraintProgrammingSession {
        override var isClosed: Boolean = false
            private set

    override suspend fun solve(
        assumptions: List<BooleanLiteral>,
        fixedValues: Map<VariableId, Int64>,
        hints: ConstraintProgrammingSolution?
        ): Ret<ConstraintProgrammingSolverOutput> {
            if (isClosed) {
                return Failed(ErrorCode.ApplicationStopped, "CP session 已关闭 / CP session is closed")
            }
            if (options.cancellationToken?.isCancellationRequested == true) {
                return ok(ConstraintProgrammingUnknownOutput(TerminationReason.Cancelled))
            }

            val snapshotResult = model.snapshot()
            if (snapshotResult.failed) {
                return propagate(snapshotResult)
            }
            val loweredResult = lowerer.lower(model, assumptions, fixedValues)
            if (loweredResult.failed) {
                return propagate(loweredResult)
            }
            val lowered = loweredResult.value!!
            return try {
                val hintResult = applyHints(lowered, hints)
                if (hintResult.failed) {
                    return propagate(hintResult)
                }
                if (options.cancellationToken?.isCancellationRequested == true) {
                    return ok(ConstraintProgrammingUnknownOutput(TerminationReason.Cancelled))
                }

                val mechanismResult = linearSolver.dump(lowered.model, null, null)
                if (mechanismResult.failed) {
                    return propagate(mechanismResult)
                }
                val mechanism = mechanismResult.value!!
                try {
                    val triad = linearSolver.dump(mechanism)
                    try {
                        val reportResult = linearSolver.solveReport(triad, options.progressContext)
                        if (options.cancellationToken?.isCancellationRequested == true) {
                            ok(ConstraintProgrammingUnknownOutput(TerminationReason.Cancelled))
                        } else {
                            when (reportResult) {
                                is Ok -> mapReport(
                                    report = reportResult.value,
                                    triad = triad,
                                    lowered = lowered,
                                    snapshot = snapshotResult.value!!
                                )
                                is Failed -> mapLinearFailure(reportResult)
                                is Fatal -> Fatal(reportResult.errors)
                            }
                        }
                    } finally {
                        triad.close()
                    }
                } finally {
                    mechanism.close()
                }
            } finally {
                lowered.close()
            }
        }

        override fun close() {
            isClosed = true
        }
    }

    private fun applyHints(
        lowered: ConstraintProgrammingLoweredLinearModel,
        hints: ConstraintProgrammingSolution?
    ): Ret<Unit> {
        if (hints == null || (hints.values.isEmpty() && hints.intervals.isEmpty())) {
            return ok(Unit)
        }
        val initial = LinkedHashMap<fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>, Flt64>()
        val hintValues = LinkedHashMap<VariableId, fuookami.ospf.kotlin.math.algebra.number.Int64>()
        for ((id, value) in hints.values) {
            val variable = lowered.variables[id]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "CP hint 引用了未知变量：$id / CP hint references an unknown variable: $id"
                )
            val domain = lowered.domains[id]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "CP hint 缺少变量值域：$id / CP hint is missing the variable domain: $id"
                )
            if (!domain.contains(value)) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "CP hint 超出变量值域：$id=$value / CP hint is outside the variable domain: $id=$value"
                )
            }
            hintValues[id] = value
            initial[variable] = value.toFlt64()
        }
        for ((id, intervalValue) in hints.intervals) {
            val interval = lowered.intervals[id]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "CP hint 引用了未知 interval：$id / CP hint references an unknown interval: $id"
                )
            interval.presence?.variableId?.let { presenceId ->
                val presenceValue = if (intervalValue.present) {
                    fuookami.ospf.kotlin.math.algebra.number.Int64.one
                } else {
                    fuookami.ospf.kotlin.math.algebra.number.Int64.zero
                }
                val existing = hintValues[presenceId]
                if (existing != null && existing != presenceValue) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "CP interval hint 与 presence literal 冲突：$id / " +
                            "CP interval hint conflicts with its presence literal: $id"
                    )
                }
                if (existing == null) {
                    val variable = lowered.variables[presenceId]
                        ?: return Failed(
                            ErrorCode.DataNotFound,
                            "CP interval presence 变量未注册：$presenceId / " +
                                "CP interval presence variable is not registered: $presenceId"
                        )
                    hintValues[presenceId] = presenceValue
                    initial[variable] = presenceValue.toFlt64()
                }
            }
            if (intervalValue.present) {
                val startHint = addIntervalVariableHint(interval.start, intervalValue.start, id, hintValues, initial, lowered)
                if (startHint.failed) return propagate(startHint)
                val endHint = addIntervalVariableHint(interval.end, intervalValue.end, id, hintValues, initial, lowered)
                if (endHint.failed) return propagate(endHint)
                if (interval.size is ConstraintProgrammingExpression.Variable) {
                    val sizeHint = addIntervalVariableHint(interval.size, intervalValue.size, id, hintValues, initial, lowered)
                    if (sizeHint.failed) return propagate(sizeHint)
                }
            }
            val evaluated = interval.evaluate(hintValues)
            if (evaluated.failed) {
                return propagate(evaluated)
            }
            if (evaluated.value!! != intervalValue) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "CP interval hint 不满足 interval 定义：$id / CP interval hint does not satisfy the interval definition: $id"
                )
            }
        }
        lowered.model.tokens.setSolverSolution(initial)
        return ok(Unit)
    }

    private fun addIntervalVariableHint(
        expression: ConstraintProgrammingExpression,
        value: fuookami.ospf.kotlin.math.algebra.number.Int64,
        intervalId: fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId,
        hintValues: MutableMap<VariableId, fuookami.ospf.kotlin.math.algebra.number.Int64>,
        initial: MutableMap<fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>, Flt64>,
        lowered: ConstraintProgrammingLoweredLinearModel
    ): Ret<Unit> {
        val variableExpression = expression as? ConstraintProgrammingExpression.Variable
            ?: return Failed(
                ErrorCode.ORSolutionInvalid,
                "CP interval hint 只能注入标量变量起止点：$intervalId / " +
                    "CP interval hints can only inject scalar start/end variables: $intervalId"
            )
        val id = variableExpression.variableId
        val domain = lowered.domains[id]
            ?: return Failed(
                ErrorCode.DataNotFound,
                "CP interval hint 缺少变量值域：$id / CP interval hint is missing the variable domain: $id"
            )
        if (!domain.contains(value)) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "CP interval hint 超出变量值域：$id=$value / CP interval hint is outside the variable domain: $id=$value"
            )
        }
        val existing = hintValues[id]
        if (existing != null && existing != value) {
            return Failed(
                ErrorCode.ORSolutionInvalid,
                "CP interval hint 变量值冲突：$id / CP interval hint has conflicting values: $id"
            )
        }
        if (existing == null) {
            val variable = lowered.variables[id]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "CP interval hint 引用了未知变量：$id / CP interval hint references an unknown variable: $id"
                )
            hintValues[id] = value
            initial[variable] = value.toFlt64()
        }
        return ok(Unit)
    }

    private fun mapLinearFailure(
        result: Failed<*, ErrorCode, fuookami.ospf.kotlin.utils.error.Error<ErrorCode>>
    ): Ret<ConstraintProgrammingSolverOutput> {
        return when (result.error.code) {
            ErrorCode.ORModelInfeasible -> ok(
                ConstraintProgrammingInfeasibleOutput(
                    proofStatus = ProofStatus.Verified
                )
            )

            ErrorCode.ORModelInfeasibleOrUnbounded -> ok(
                ConstraintProgrammingUnknownOutput(TerminationReason.BackendFailure)
            )

            else -> fuookami.ospf.kotlin.utils.functional.Failed(result.error)
        }
    }

    private fun mapReport(
        report: SolveReport<Flt64>,
        triad: LinearTriadModel,
        lowered: ConstraintProgrammingLoweredLinearModel,
        snapshot: ConstraintProgrammingModelSnapshot
    ): Ret<ConstraintProgrammingSolverOutput> {
        return when (report.problemStatus) {
            ProblemStatus.Infeasible -> ok(
                ConstraintProgrammingInfeasibleOutput(
                    proofStatus = report.proof.status,
                    report = report.toConstraintProgrammingReport(null)
                )
            )

            ProblemStatus.Feasible -> {
                val source = report.solution
                    ?: return Failed(ErrorCode.ORSolutionInvalid, "线性报告缺少可行解 / Linear report has no feasible solution")
                val solution = extractSolution(source.values, triad, lowered)
                if (solution.failed) {
                    return propagate(solution)
                }
                val cpSolution = solution.value!!
                val exactObjective = snapshot.objectives.firstOrNull()?.let { objective ->
                    when (val evaluated = objective.expression.evaluate(cpSolution.values)) {
                        is Ok -> evaluated.value
                        is Failed -> return propagate(evaluated)
                        is Fatal -> return Fatal(evaluated.errors)
                    }
                }
                val status = if (report.solutionPresence == SolutionPresence.Optimal) {
                    SolverStatus.Optimal
                } else {
                    SolverStatus.Feasible
                }
                ok(
                    ConstraintProgrammingFeasibleOutput(
                        solution = cpSolution,
                        objective = exactObjective?.toCompatibilityFlt64(),
                        bestBound = report.statistics.bestBound,
                        status = status,
                        proofStatus = report.proof.status,
                        report = report.toConstraintProgrammingReport(cpSolution, exactObjective),
                        exactObjective = exactObjective
                    )
                )
            }

            ProblemStatus.Unknown -> ok(
                ConstraintProgrammingUnknownOutput(
                    terminationReason = report.terminationReason,
                    report = report.toConstraintProgrammingReport(null)
                )
            )

            ProblemStatus.Unbounded,
            ProblemStatus.InfeasibleOrUnbounded -> ok(
                ConstraintProgrammingUnknownOutput(
                    terminationReason = report.terminationReason,
                    report = report.toConstraintProgrammingReport(null)
                )
            )
        }
    }

    private fun extractSolution(
        values: List<Flt64>,
        triad: LinearTriadModel,
        lowered: ConstraintProgrammingLoweredLinearModel
    ): Ret<ConstraintProgrammingSolution> {
        val result = LinkedHashMap<VariableId, Int64>()
        for (id in lowered.variableIds) {
            val variable = lowered.variables[id]
                ?: return Failed(ErrorCode.DataNotFound, "降阶结果缺少 CP 变量：$id / Lowered result misses CP variable: $id")
            val index = triad.tokensInSolver.indexOfFirst { it.variable == variable }
            if (index < 0 || index >= values.size) {
                return Failed(ErrorCode.ORSolutionInvalid, "线性解缺少 CP 变量值：$id / Linear solution misses CP variable value: $id")
            }
            val value = values[index]
            if (value != value.round()) {
                return Failed(ErrorCode.ORSolutionInvalid, "CP 变量解不是整数：$id=$value / CP variable solution is not integral: $id=$value")
            }
            if (!value.isFinite() || value.abs() > MAX_EXACT_DOUBLE_INTEGER) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "MIP-backed CP 变量解超出 Int64 精确传输范围：$id=$value / " +
                        "MIP-backed CP variable exceeds the exact Int64 transport range: $id=$value"
                )
            }
            result[id] = value.toInt64()
        }
        val intervals = LinkedHashMap<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, IntervalValue>()
        for ((id, interval) in lowered.intervals) {
            val evaluated = interval.evaluate(result)
            if (evaluated.failed) {
                return propagate(evaluated)
            }
            intervals[id] = evaluated.value!!
        }
        return ok(ConstraintProgrammingSolution(values = result, intervals = intervals))
    }

    private fun SolveReport<Flt64>.toConstraintProgrammingReport(
        solution: ConstraintProgrammingSolution?,
        objective: Int64? = null
    ): SolveReport<Int64> {
        return SolveReport(
            schemaVersion = schemaVersion,
            runId = runId,
            problemStatus = problemStatus,
            terminationReason = terminationReason,
            solutionPresence = solutionPresence,
            solution = solution?.let {
                SolveSolution(
                    values = it.values.values.toList(),
                    objective = objective
                )
            },
            proof = proof,
            statistics = SolveStatistics(
                solveTime = statistics.solveTime,
                iterations = statistics.iterations,
                nodes = statistics.nodes,
                bestBound = statistics.bestBound,
                gap = statistics.gap
            ),
            diagnostics = SolveDiagnostics(
                infeasibilityEvidence = diagnostics.infeasibilityEvidence,
                warnings = diagnostics.warnings,
                errors = diagnostics.errors
            ),
            provenance = provenance,
            fingerprints = fingerprints
        )
    }

    private fun validateOptions(options: ConstraintProgrammingSolveOptions): String? {
        if (options.timeLimit != null && options.timeLimit < ZERO) {
            return "CP timeLimit 不得为负 / CP timeLimit must not be negative"
        }
        if (options.nodeLimit != null && options.nodeLimit == UInt64.zero) {
            return "CP nodeLimit 必须为正 / CP nodeLimit must be positive"
        }
        if (options.solutionLimit != null && options.solutionLimit == UInt64.zero) {
            return "CP solutionLimit 必须为正 / CP solutionLimit must be positive"
        }
        return null
    }
}

private val MAX_EXACT_DOUBLE_INTEGER = Flt64(9_007_199_254_740_991.0)

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "求解结果状态无效 / Invalid solver result state")
    }
}
