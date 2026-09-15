/** Objective-target feasibility analysis. / ObjectiveTarget 可行性分析。 */
package fuookami.ospf.kotlin.core.analysis

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Feasibility result for one objective target.
 * 一个 ObjectiveTarget 的可行性结果。
 */
data class TargetFeasibilityReport(
    /** Requested target. / 请求的目标条件。 */
    val target: ObjectiveTarget,
    /** Exact integer RHS used by the CP target constraint. / CP 目标约束使用的精确整数 RHS。 */
    val effectiveIntegerBound: Int64? = null,
    /** Stable target evidence source. / 稳定目标证据来源。 */
    val source: DiagnosticSource.ObjectiveTarget = DiagnosticSource.ObjectiveTarget(target),
    /** Reachability conclusion. / 可达性结论。 */
    val status: AnalysisStatus,
    /** Witness solution when target is reachable. / 目标可达时的见证解。 */
    val solution: ConstraintProgrammingSolution? = null,
    /** Exact objective value from the CP output. / CP 输出中的精确目标值。 */
    val exactObjective: Int64? = null,
    /** Compatibility objective value. / 兼容浮点目标值。 */
    val objectiveValue: Flt64? = null,
    /** Proof status of the target conclusion. / 目标结论的证明状态。 */
    val proofStatus: ProofStatus = ProofStatus.None,
    /** Backend termination reason. / 后端终止原因。 */
    val terminationReason: TerminationReason? = null,
    /** Backend solve duration. / 后端求解耗时。 */
    val solveTime: Duration? = null,
    /** Whether the target was inserted into a derived model. / 目标是否已插入派生模型。 */
    val targetFixed: Boolean = true,
    /** Human-readable diagnostic detail. / 面向诊断的详情。 */
    val message: String? = null
) {
    /** Compatibility alias used by feasibility callers. / 可行性调用方使用的兼容别名。 */
    val reachable: Boolean?
        get() = when (status) {
            AnalysisStatus.Reachable -> true
            AnalysisStatus.Unreachable -> false
            AnalysisStatus.Unknown,
            AnalysisStatus.Unsupported -> null
        }

    /** Whether the conclusion is a proven target result. / 是否为已证明的目标结论。 */
    val isProven: Boolean
        get() = status.isProven
}

/** Internal model plus target identity used by the snapshot builder. / snapshot builder 使用的内部模型及 target 身份。 */
internal data class DerivedConstraintProgrammingModel(
    val model: ConstraintProgrammingModel,
    val targetConstraintId: ConstraintId?
)

/**
 * Rebuilds a mutable CP model from an immutable snapshot and adds a target.
 * 从不可变 snapshot 重建可变 CP 模型并追加 target。
 *
 * The builder is shared by target feasibility and conflict deletion checks. It
 * keeps stable IDs and original global constraints while making domain-member
 * activation explicit for conflict analysis.
 * 该 builder 由 target 可行性与 conflict 删除复验共享；保留稳定 ID 和原始 global constraint，
 * 并在 conflict 分析中显式处理值域成员 activation。
 */
internal object ConstraintProgrammingSnapshotBuilder {
    /** Build a model with all original members active. / 构造所有原始成员均激活的模型。 */
    fun build(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget
    ): Ret<DerivedConstraintProgrammingModel> {
        return build(snapshot, target, null)
    }

    /** Build a model with the requested original members active. / 构造指定原始成员激活的模型。 */
    fun build(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        activeSources: Set<DiagnosticSource>?
    ): Ret<DerivedConstraintProgrammingModel> {
        val modelResult = buildModel(snapshot, activeSources, target.objectiveId)
        if (modelResult !is Ok) {
            return propagateBuilderFailure(modelResult)
        }
        val model = modelResult.value!!
        val objective = snapshot.objective(target.objectiveId)
            ?: run {
                model.close()
                return Failed(
                    ErrorCode.DataNotFound,
                    "目标引用了未注册 Objective：${target.objectiveId} / Target references an unregistered objective: ${target.objectiveId}"
                )
            }
        val integerBound = integerBound(target)
        if (integerBound.failed) {
            model.close()
            return propagateBuilderFailure(integerBound)
        }
        val domains = activeSources?.let { activeDomains(snapshot, it) }.orEmpty()
        // Variable expressions carry their own domain. Rebind the target expression as well as
        // the registered model domain, otherwise deleting a bound would leave that bound hidden
        // inside the AST and make the deletion check unsound.
        // 变量表达式自身携带值域。除了重建 model domain 外，target expression 也必须重绑；否则删除
        // bound 后该限制仍会藏在 AST 内，导致删除复验不可靠。
        val targetExpression = rebindExpression(objective.expression, domains)
        val targetConstraint = when (target.relation) {
            ObjectiveTargetRelation.AtLeast -> ConstraintProgrammingConstraint.greaterOrEqual(
                targetExpression,
                integerBound.value!!
            )
            ObjectiveTargetRelation.AtMost -> ConstraintProgrammingConstraint.lessOrEqual(
                targetExpression,
                integerBound.value!!
            )
        }
        if (targetConstraint.failed) {
            model.close()
            return propagateBuilderFailure(targetConstraint)
        }
        val targetId = targetConstraintId(target)
        val targetAdded = model.addConstraint(
            constraint = targetConstraint.value!!,
            id = targetId,
            name = targetId.value
        )
        if (targetAdded.failed) {
            model.close()
            return propagateBuilderFailure(targetAdded)
        }
        return ok(DerivedConstraintProgrammingModel(model, targetId))
    }

    /**
     * 从不可变 snapshot 重建 CP 模型，不追加任何 target。
     *
     * 供扰动与 removal 分析复用：这些阶段的目标条件已经由调用方写入派生 snapshot，
     * 因此这里只做"忠实重建"，不引入额外语义。
     *
     * Rebuild a CP model from an immutable snapshot without appending any target. Perturbation and
     * removal analysis reuse this: their objective condition is already baked into the derived
     * snapshot by the caller, so this performs a faithful rebuild only and adds no semantics.
     *
     * `objectiveFilter` 为 `null` 时注册 snapshot 的全部目标（忠实重建）；指定时只注册该目标，
     * 保持 target 可行性分析原有的单目标语义。
     * When `objectiveFilter` is `null` every snapshot objective is registered (faithful rebuild);
     * when set, only that objective is registered, preserving target-feasibility analysis's
     * original single-objective semantics.
     */
    fun buildModel(
        snapshot: ConstraintProgrammingModelSnapshot,
        activeSources: Set<DiagnosticSource>? = null,
        objectiveFilter: ObjectiveId? = null
    ): Ret<ConstraintProgrammingModel> {
        // Keep the snapshot AST unchanged for the ordinary all-members-active path. Domain
        // rebinding is required only when conflict checks selectively deactivate evidence.
        // 全部成员激活的普通路径保持 snapshot AST 不变；只有 conflict 选择性停用证据时才需要重绑值域。
        val domains = activeSources?.let { activeDomains(snapshot, it) }.orEmpty()
        val sourceVariables = LinkedHashMap<VariableId, fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>>()
        collectExpressionVariables(snapshot.expressions.map { it.expression }, sourceVariables)
        collectExpressionVariables(snapshot.objectives.map { it.expression }, sourceVariables)
        snapshot.intervals.forEach { interval ->
            collectExpressionVariables(listOf(interval.start, interval.size, interval.end), sourceVariables)
            collectLiteralVariable(interval.presence, sourceVariables)
        }
        snapshot.constraints.forEach { entry ->
            collectConstraintVariables(entry.constraint, sourceVariables)
        }

        val model = ConstraintProgrammingModel(
            name = snapshot.name,
            objectCategory = snapshot.objectCategory,
            identityNamespace = snapshot.identityNamespace,
            identitySchemaVersion = snapshot.identitySchemaVersion
        )
        fun fail(result: Ret<*>): Ret<ConstraintProgrammingModel> {
            model.close()
            return propagateBuilderFailure(result)
        }

        for (definition in snapshot.variables) {
            val variable = sourceVariables[definition.id]
            if (variable == null) {
                // A completely unused variable has no effect on feasibility and
                // cannot be reconstructed from the immutable snapshot alone.
                // 完全未被引用的变量不影响可行性，且 snapshot 无法单独恢复其对象。
                continue
            }
            val domain = effectiveDomain(definition.domain, definition.id, activeSources)
            val registered = model.registerVariable(
                id = definition.id,
                variable = variable,
                domain = domain,
                scope = definition.scope,
                origin = definition.origin,
                identityProvenance = definition.identityProvenance
            )
            if (registered.failed) {
                return fail(registered)
            }
        }
        for (expression in snapshot.expressions) {
            val registered = model.registerExpression(
                expression.name,
                rebindExpression(expression.expression, domains)
            )
            if (registered.failed) {
                return fail(registered)
            }
        }
        for (interval in snapshot.intervals) {
            val registered = model.registerInterval(rebindInterval(interval, domains))
            if (registered.failed) {
                return fail(registered)
            }
        }
        val groups = LinkedHashMap<String, SnapshotConstraintGroup>()
        for (entry in snapshot.constraints) {
            val source = DiagnosticSource.Constraint(entry.id)
            if (activeSources != null && source !in activeSources) {
                continue
            }
            val group = entry.groupName?.let { groups.getOrPut(it) { SnapshotConstraintGroup(it) } }
            val added = model.addConstraint(
                constraint = rebindConstraint(entry.constraint, domains),
                id = entry.id,
                name = entry.name,
                group = group,
                scope = entry.scope,
                origin = entry.origin,
                identityProvenance = entry.identityProvenance
            )
            if (added.failed) {
                return fail(added)
            }
        }
        for (objective in snapshot.objectives) {
            if (objectiveFilter != null && objective.id != objectiveFilter) {
                continue
            }
            val objectiveAdded = model.addObjective(
                category = objective.category,
                expression = rebindExpression(objective.expression, domains),
                id = objective.id,
                name = objective.name,
                scope = objective.scope,
                origin = objective.origin,
                identityProvenance = objective.identityProvenance
            )
            if (objectiveAdded.failed) {
                return fail(objectiveAdded)
            }
        }
        return ok(model)
    }

    /** Compute the effective domain represented by the active original evidence. */
    private fun activeDomains(
        snapshot: ConstraintProgrammingModelSnapshot,
        activeSources: Set<DiagnosticSource>?
    ): Map<VariableId, IntegerDomain> {
        return snapshot.variables.associate { definition ->
            definition.id to effectiveDomain(definition.domain, definition.id, activeSources)
        }
    }

    /** Rebind variable expressions so domain deactivation is reflected in the AST itself. */
    private fun rebindExpression(
        expression: ConstraintProgrammingExpression,
        domains: Map<VariableId, IntegerDomain>
    ): ConstraintProgrammingExpression {
        return when (expression) {
            is ConstraintProgrammingExpression.Constant,
            is ConstraintProgrammingExpression.Invalid -> expression

            is ConstraintProgrammingExpression.Variable -> {
                ConstraintProgrammingExpression.Variable(
                    variable = expression.variable,
                    domain = domains[expression.variableId] ?: expression.domain,
                    id = expression.id
                )
            }

            // Linear terms intentionally do not validate variable domains during evaluation, so
            // preserving their source variables is sufficient; only Variable nodes carry a
            // domain restriction in the CP AST.
            // Linear term 求值本身不校验变量值域，因此保留其 source variable 即可；CP AST 中只有
            // Variable 节点携带值域限制。
            is ConstraintProgrammingExpression.Linear -> expression
        }
    }

    private fun rebindInterval(
        interval: IntervalVariable,
        domains: Map<VariableId, IntegerDomain>
    ): IntervalVariable {
        return interval.copy(
            start = rebindExpression(interval.start, domains),
            size = rebindExpression(interval.size, domains),
            end = rebindExpression(interval.end, domains)
        )
    }

    private fun rebindConstraint(
        constraint: ConstraintProgrammingConstraint,
        domains: Map<VariableId, IntegerDomain>
    ): ConstraintProgrammingConstraint {
        return when (constraint) {
            is ConstraintProgrammingConstraint.IntegerComparison -> constraint.copy(
                expression = rebindExpression(constraint.expression, domains)
            )
            is ConstraintProgrammingConstraint.BoolAnd,
            is ConstraintProgrammingConstraint.BoolOr,
            is ConstraintProgrammingConstraint.BoolXor,
            is ConstraintProgrammingConstraint.Literal -> constraint
            is ConstraintProgrammingConstraint.Implication -> constraint.copy(
                constraint = rebindConstraint(constraint.constraint, domains)
            )
            is ConstraintProgrammingConstraint.Reified -> constraint.copy(
                constraint = rebindConstraint(constraint.constraint, domains)
            )
            is ConstraintProgrammingConstraint.AllDifferent -> constraint.copy(
                expressions = constraint.expressions.map { rebindExpression(it, domains) }
            )
            is ConstraintProgrammingConstraint.Element -> constraint.copy(
                index = rebindExpression(constraint.index, domains),
                values = constraint.values.map { value ->
                    if (value is ConstraintProgrammingExpression) {
                        rebindExpression(value, domains)
                    } else {
                        value
                    }
                },
                target = rebindExpression(constraint.target, domains)
            )
            is ConstraintProgrammingConstraint.AllowedAssignments -> constraint.copy(
                expressions = constraint.expressions.map { rebindExpression(it, domains) }
            )
            is ConstraintProgrammingConstraint.ForbiddenAssignments -> constraint.copy(
                expressions = constraint.expressions.map { rebindExpression(it, domains) }
            )
            is NoOverlap -> constraint.copy(
                intervals = constraint.intervals.map { rebindInterval(it, domains) }
            )
            is Cumulative -> constraint.copy(
                intervals = constraint.intervals.map { rebindInterval(it, domains) },
                demands = constraint.demands.map { rebindExpression(it, domains) },
                capacity = rebindExpression(constraint.capacity, domains)
            )
            is ConstraintProgrammingConstraint.Circuit -> constraint.copy(
                successors = constraint.successors.map { rebindExpression(it, domains) }
            )
            is ConstraintProgrammingConstraint.Automaton -> constraint.copy(
                expressions = constraint.expressions.map { rebindExpression(it, domains) }
            )
            is ConstraintProgrammingConstraint.Reservoir -> constraint.copy(
                events = constraint.events.map { event ->
                    ConstraintProgrammingConstraint.Reservoir.Event(
                        time = rebindExpression(event.time, domains),
                        levelChange = rebindExpression(event.levelChange, domains)
                    )
                }
            )
        }
    }

    private fun effectiveDomain(
        domain: IntegerDomain,
        variableId: VariableId,
        activeSources: Set<DiagnosticSource>?
    ): IntegerDomain {
        if (activeSources == null) {
            return copyDomain(domain)
        }
        if (domain == IntegerDomain.boolean) {
            // Binary-ness is a variable type invariant, not a removable
            // numeric bound. / 二值语义是变量类型不变量，不是可移除的数值边界。
            return copyDomain(domain)
        }
        val lowerActive = DiagnosticSource.VariableLowerBound(variableId) in activeSources
        val upperActive = DiagnosticSource.VariableUpperBound(variableId) in activeSources
        val sparseActive = DiagnosticSource.SparseDomain(variableId) in activeSources
        return when (domain) {
            is IntegerDomain.Interval -> IntegerDomain.Interval(
                lowerBound = if (lowerActive) domain.lowerBound else Int64.minimum,
                upperBound = if (upperActive) domain.upperBound else Int64.maximum
            )
            is IntegerDomain.Values -> if (sparseActive) {
                copyDomain(domain)
            } else {
                IntegerDomain.Interval(
                    lowerBound = if (lowerActive) domain.lowerBound else Int64.minimum,
                    upperBound = if (upperActive) domain.upperBound else Int64.maximum
                )
            }
        }
    }

    private fun copyDomain(domain: IntegerDomain): IntegerDomain {
        return when (domain) {
            is IntegerDomain.Interval -> IntegerDomain.Interval(domain.lowerBound, domain.upperBound)
            is IntegerDomain.Values -> IntegerDomain.Values(domain.values.toList())
        }
    }

    private fun integerBound(target: ObjectiveTarget): Ret<Int64> {
        val rounding = when (target.relation) {
            ObjectiveTargetRelation.AtLeast -> RoundingMode.CEILING
            ObjectiveTargetRelation.AtMost -> RoundingMode.FLOOR
        }
        return try {
            val value = BigDecimal.valueOf(target.value.toSolverDouble("objectiveTarget.value"))
                .setScale(0, rounding)
                .longValueExact()
            ok(Int64(value))
        } catch (_: ArithmeticException) {
            Failed(
                ErrorCode.IllegalArgument,
                "ObjectiveTarget 超出 CP Int64 范围：${target.value} / ObjectiveTarget exceeds the CP Int64 range: ${target.value}"
            )
        }
    }

    private fun targetConstraintId(target: ObjectiveTarget): ConstraintId {
        return ConstraintId("analysis-target:${target.stableId}")
    }

    private fun collectExpressionVariables(
        expressions: Iterable<ConstraintProgrammingExpression>,
        target: MutableMap<VariableId, fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>>
    ) {
        expressions.forEach { expression ->
            when (expression) {
                is ConstraintProgrammingExpression.Constant,
                is ConstraintProgrammingExpression.Invalid -> {}
                is ConstraintProgrammingExpression.Variable -> target[expression.variableId] = expression.variable
                is ConstraintProgrammingExpression.Linear -> expression.terms.forEach { term ->
                    target[term.variableId] = term.variable
                }
            }
        }
    }

    private fun collectLiteralVariable(
        literal: BooleanLiteral?,
        target: MutableMap<VariableId, fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>>
    ) {
        val variable = literal?.variable ?: return
        val id = literal.variableId ?: return
        target[id] = variable
    }

    private fun collectConstraintVariables(
        constraint: ConstraintProgrammingConstraint,
        target: MutableMap<VariableId, fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>>
    ) {
        when (constraint) {
            is ConstraintProgrammingConstraint.IntegerComparison -> collectExpressionVariables(listOf(constraint.expression), target)
            is ConstraintProgrammingConstraint.BoolAnd -> constraint.literals.forEach { collectLiteralVariable(it, target) }
            is ConstraintProgrammingConstraint.BoolOr -> constraint.literals.forEach { collectLiteralVariable(it, target) }
            is ConstraintProgrammingConstraint.BoolXor -> constraint.literals.forEach { collectLiteralVariable(it, target) }
            is ConstraintProgrammingConstraint.Literal -> collectLiteralVariable(constraint.literal, target)
            is ConstraintProgrammingConstraint.Implication -> {
                collectLiteralVariable(constraint.enforcement, target)
                collectConstraintVariables(constraint.constraint, target)
            }
            is ConstraintProgrammingConstraint.Reified -> {
                collectLiteralVariable(constraint.literal, target)
                collectConstraintVariables(constraint.constraint, target)
            }
            is ConstraintProgrammingConstraint.AllDifferent -> collectExpressionVariables(constraint.expressions, target)
            is ConstraintProgrammingConstraint.Element -> {
                collectExpressionVariables(listOf(constraint.index, constraint.target), target)
                collectExpressionVariables(
                    constraint.values.filterIsInstance<ConstraintProgrammingExpression>(),
                    target
                )
            }
            is ConstraintProgrammingConstraint.AllowedAssignments -> collectExpressionVariables(constraint.expressions, target)
            is ConstraintProgrammingConstraint.ForbiddenAssignments -> collectExpressionVariables(constraint.expressions, target)
            is NoOverlap -> constraint.intervals.forEach { interval ->
                collectExpressionVariables(listOf(interval.start, interval.size, interval.end), target)
                collectLiteralVariable(interval.presence, target)
            }
            is Cumulative -> {
                constraint.intervals.forEach { interval ->
                    collectExpressionVariables(listOf(interval.start, interval.size, interval.end), target)
                    collectLiteralVariable(interval.presence, target)
                }
                collectExpressionVariables(constraint.demands + constraint.capacity, target)
            }
            is ConstraintProgrammingConstraint.Circuit -> collectExpressionVariables(constraint.successors, target)
            is ConstraintProgrammingConstraint.Automaton -> collectExpressionVariables(constraint.expressions, target)
            is ConstraintProgrammingConstraint.Reservoir -> constraint.events.forEach { event ->
                collectExpressionVariables(listOf(event.time, event.levelChange), target)
            }
        }
    }

    private class SnapshotConstraintGroup(
        override val name: String
    ) : MetaConstraintGroup
}

/**
 * Runs target feasibility using a real CP solver on a derived model.
 * 使用真实 CP solver 在派生模型上执行目标可行性求解。
 */
class TargetFeasibilityAnalyzer(
    /** CP solver used for every target check. / 每次 target 检查使用的 CP solver。 */
    private val solver: ConstraintProgrammingSolver,
    /** Default solve options. / 默认求解选项。 */
    private val defaultOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
) {
    /** Analyze a session target and cache the immutable report. / 分析 session target 并缓存不可变报告。 */
    suspend fun analyze(
        session: CriticalConstraintAnalysisSession,
        target: ObjectiveTarget,
        options: ConstraintProgrammingSolveOptions = defaultOptions
    ): Ret<TargetFeasibilityReport> {
        invalidSessionResult<TargetFeasibilityReport>(session)?.let { return it }
        if (session.isClosed) {
            return Failed(
                ErrorCode.ApplicationStopped,
                "目标可行性分析会话已关闭 / Target feasibility analysis session is closed"
            )
        }
        val key = CacheKey(this, target, options)
        session.cached<TargetFeasibilityReport>(AnalysisCacheKind.Target, key)?.let { return ok(it) }
        return analyze(
            snapshot = session.baselineSnapshot,
            target = target,
            options = options,
            capabilityMatrix = session.capabilityMatrix
        ).map { report ->
            session.cache(AnalysisCacheKind.Target, key, report)
            report
        }
    }

    /** Analyze a snapshot target with solver-backed derived-model solving. / 使用 solver 在派生模型上分析 snapshot target。 */
    suspend fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        options: ConstraintProgrammingSolveOptions = defaultOptions,
        capabilityMatrix: CapabilityMatrix = CapabilityMatrix.from(solver.descriptor)
    ): Ret<TargetFeasibilityReport> {
        if (!snapshot.validateIdentity() || !snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份或目标语义无效 / CP snapshot identity or objective semantics is invalid"
            )
        }
        val explicitCapability = capabilityMatrix.analysisCapabilities[AnalysisCapability.ObjectiveTarget]
        if (explicitCapability == CapabilitySupport.Unsupported) {
            return ok(
                TargetFeasibilityReport(
                    target = target,
                    status = AnalysisStatus.Unsupported,
                    targetFixed = false,
                    message = "当前求解器未声明 ObjectiveTarget 能力 / Solver does not declare ObjectiveTarget support"
                )
            )
        }
        capabilityMatrix.unsupportedDeclaredCpFeatures(snapshot).takeIf { it.isNotEmpty() }?.let { unsupported ->
            return ok(unsupportedTargetReport(target, unsupported))
        }
        val built = ConstraintProgrammingSnapshotBuilder.build(snapshot, target)
        if (built !is Ok) {
            return when (built) {
                is Failed -> Failed(built.error)
                is Fatal -> Fatal(built.errors)
                is Ok -> Failed(ErrorCode.ApplicationError, "CP 派生模型构造结果无效 / Invalid derived CP model result")
            }
        }
        val derived = built.value!!
        return try {
            solve(derived.model, target, options)
        } finally {
            derived.model.close()
        }
    }

    /** Internal overload used by conflict deletion checks. / conflict 删除复验使用的内部重载。 */
    internal suspend fun analyzeActiveSources(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        activeSources: Set<DiagnosticSource>,
        options: ConstraintProgrammingSolveOptions = defaultOptions,
        capabilityMatrix: CapabilityMatrix = CapabilityMatrix.from(solver.descriptor)
    ): Ret<TargetFeasibilityReport> {
        capabilityMatrix.unsupportedDeclaredCpFeatures(snapshot, activeSources).takeIf { it.isNotEmpty() }?.let { unsupported ->
            return ok(unsupportedTargetReport(target, unsupported))
        }
        val built = ConstraintProgrammingSnapshotBuilder.build(snapshot, target, activeSources)
        if (built !is Ok) {
            return when (built) {
                is Failed -> Failed(built.error)
                is Fatal -> Fatal(built.errors)
                is Ok -> Failed(ErrorCode.ApplicationError, "CP 派生模型构造结果无效 / Invalid derived CP model result")
            }
        }
        val derived = built.value!!
        return try {
            solve(derived.model, target, options)
        } finally {
            derived.model.close()
        }
    }

    private suspend fun solve(
        model: ConstraintProgrammingModel,
        target: ObjectiveTarget,
        options: ConstraintProgrammingSolveOptions
    ): Ret<TargetFeasibilityReport> {
        val integerBound = targetIntegerBound(target)
        if (integerBound == null) {
            return ok(
                TargetFeasibilityReport(
                    target = target,
                    status = AnalysisStatus.Unsupported,
                    message = "ObjectiveTarget 无法精确映射为 CP Int64 边界 / ObjectiveTarget cannot be mapped exactly to a CP Int64 bound"
                )
            )
        }
        // Witness validation must use the model actually sent to the backend. Conflict deletion
        // checks solve a selectively rebuilt model whose domains and active constraints may differ
        // from the baseline snapshot.
        // witness 校验必须使用实际提交给后端的模型。conflict 删除复验求解的是选择性重建模型，
        // 其值域和激活约束可能不同于基线 snapshot。
        val derivedSnapshot = when (val snapshotResult = model.snapshot()) {
            is Ok -> snapshotResult.value!!
            is Failed -> return Failed(snapshotResult.error)
            is Fatal -> return Fatal(snapshotResult.errors)
        }
        val output = try {
            solver.solve(model, options)
        } catch (error: Throwable) {
            return ok(
                TargetFeasibilityReport(
                    target = target,
                    effectiveIntegerBound = integerBound,
                    status = AnalysisStatus.Unknown,
                    message = "CP target 求解失败：${error.message ?: error::class.simpleName} / CP target solve failed: ${error.message ?: error::class.simpleName}"
                )
            )
        }
        return when (output) {
            is Ok -> mapOutput(output.value!!, target, integerBound, derivedSnapshot)
            is Failed -> if (output.error.code == ErrorCode.Other) {
                ok(
                    TargetFeasibilityReport(
                        target = target,
                        effectiveIntegerBound = integerBound,
                        status = AnalysisStatus.Unsupported,
                        targetFixed = false,
                        message = "CP target 降阶不支持：${output.error.message} / " +
                            "CP target lowering is unsupported: ${output.error.message}"
                    )
                )
            } else {
                Failed(output.error)
            }
            is Fatal -> Fatal(output.errors)
        }
    }

    private fun unsupportedTargetReport(
        target: ObjectiveTarget,
        features: Set<fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature>
    ): TargetFeasibilityReport {
        val names = features.joinToString { it.name }
        return TargetFeasibilityReport(
            target = target,
            status = AnalysisStatus.Unsupported,
            targetFixed = false,
            message = "当前求解器未声明 CP 特性支持：$names / " +
                "Solver explicitly declares these CP features unsupported: $names"
        )
    }

    private fun mapOutput(
        output: ConstraintProgrammingSolverOutput,
        target: ObjectiveTarget,
        integerBound: Int64,
        snapshot: ConstraintProgrammingModelSnapshot
    ): Ret<TargetFeasibilityReport> {
        return when (output) {
            is ConstraintProgrammingFeasibleOutput -> {
                val witnessError = validateTargetWitness(snapshot, target, integerBound, output.solution)
                ok(
                    if (witnessError == null) {
                        TargetFeasibilityReport(
                            target = target,
                            effectiveIntegerBound = integerBound,
                            status = AnalysisStatus.Reachable,
                            solution = output.solution,
                            exactObjective = output.exactObjective,
                            objectiveValue = output.objective,
                            proofStatus = output.proofStatus,
                            terminationReason = output.report?.terminationReason,
                            solveTime = output.report?.statistics?.solveTime,
                            message = if (output.report?.solutionPresence?.name == "Incumbent") {
                                "目标找到可行见证，但未证明最优 / A feasible target witness was found without an optimality proof"
                            } else {
                                null
                            }
                        )
                    } else {
                        TargetFeasibilityReport(
                            target = target,
                            effectiveIntegerBound = integerBound,
                            status = AnalysisStatus.Unknown,
                            proofStatus = output.proofStatus,
                            terminationReason = output.report?.terminationReason,
                            solveTime = output.report?.statistics?.solveTime,
                            message = "CP 后端返回的 target 见证无效：$witnessError / " +
                                "CP backend returned an invalid target witness: $witnessError"
                        )
                    }
                )
            }
            is ConstraintProgrammingInfeasibleOutput -> {
                val report = output.report
                val verified = output.proofStatus == ProofStatus.Verified ||
                    report?.proof?.status == ProofStatus.Verified
                ok(
                    TargetFeasibilityReport(
                        target = target,
                        effectiveIntegerBound = integerBound,
                        status = if (verified) AnalysisStatus.Unreachable else AnalysisStatus.Unknown,
                        proofStatus = if (verified) ProofStatus.Verified else output.proofStatus,
                        terminationReason = report?.terminationReason,
                        solveTime = report?.statistics?.solveTime,
                        message = if (verified) {
                            null
                        } else {
                            "CP 后端未提供不可达证明 / CP backend did not provide a proof of target infeasibility"
                        }
                    )
                )
            }
            is ConstraintProgrammingUnknownOutput -> {
                val report = output.report
                val witness = report?.solution
                val mappedWitness = if (witness == null) {
                    null
                } else {
                    ConstraintProgrammingSolution(
                        values = snapshot.variables.mapIndexedNotNull { index, variable ->
                            witness.values.getOrNull(index)?.let { variable.id to it }
                        }.toMap()
                    )
                }
                val witnessError = mappedWitness?.let {
                    validateTargetWitness(snapshot, target, integerBound, it)
                }
                val reachable = mappedWitness != null && witnessError == null
                ok(
                    TargetFeasibilityReport(
                        target = target,
                        effectiveIntegerBound = integerBound,
                        status = if (reachable) AnalysisStatus.Reachable else AnalysisStatus.Unknown,
                        solution = if (reachable) mappedWitness else null,
                        exactObjective = if (reachable) {
                            snapshot.objective(target.objectiveId)?.expression
                                ?.evaluate(mappedWitness!!.values)?.value
                        } else {
                            null
                        },
                        objectiveValue = if (reachable) {
                            snapshot.objective(target.objectiveId)?.expression
                                ?.evaluate(mappedWitness!!.values)?.value
                                ?.toFlt64()
                        } else {
                            null
                        },
                        proofStatus = report?.proof?.status ?: ProofStatus.None,
                        terminationReason = output.terminationReason,
                        solveTime = report?.statistics?.solveTime,
                        message = if (reachable) {
                            "目标找到可行见证，但求解未形成完成证明 / A feasible target witness was found without a completed proof"
                        } else if (witnessError != null) {
                            "CP 后端返回的 target 见证无效：$witnessError / " +
                                "CP backend returned an invalid target witness: $witnessError"
                        } else {
                            "CP target 求解未形成证明：${output.terminationReason} / " +
                                "CP target solve did not form a proof: ${output.terminationReason}"
                        }
                    )
                )
            }
        }
    }

    /**
     * Validate a backend witness against the original snapshot and the temporary target.
     *
     * A backend status alone is insufficient here: a malformed or partially mapped witness must
     * not turn an unverified target check into `Reachable`.
     */
    private fun validateTargetWitness(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        integerBound: Int64,
        solution: ConstraintProgrammingSolution
    ): String? {
        val unknown = solution.values.keys.firstOrNull { snapshot.variable(it) == null }
        if (unknown != null) {
            return "见证解包含未知变量：$unknown / witness contains an unknown variable: $unknown"
        }
        val outside = solution.values.entries.firstOrNull { (id, value) ->
            snapshot.variable(id)?.domain?.contains(value) != true
        }
        if (outside != null) {
            return "见证解超出变量值域：${outside.key} / witness is outside a variable domain: ${outside.key}"
        }
        for (entry in snapshot.constraints) {
            val satisfied = entry.constraint.isSatisfied(solution.values)
            when (satisfied) {
                is Ok -> if (satisfied.value != true) {
                    return "见证解违反原始约束：${entry.id} / witness violates original constraint: ${entry.id}"
                }
                is Failed -> return "原始约束无法求值：${entry.id} / original constraint could not be evaluated: ${entry.id}"
                is Fatal -> return "原始约束求值失败：${entry.id} / original constraint evaluation failed: ${entry.id}"
            }
        }
        val objective = snapshot.objective(target.objectiveId)
            ?: return "目标引用了未注册 Objective / target references an unregistered objective"
        val objectiveValue = objective.expression.evaluate(solution.values)
        val exact = when (objectiveValue) {
            is Ok -> objectiveValue.value!!
            is Failed -> return "目标表达式无法求值 / objective expression could not be evaluated"
            is Fatal -> return "目标表达式求值失败 / objective expression evaluation failed"
        }
        val targetSatisfied = when (target.relation) {
            ObjectiveTargetRelation.AtLeast -> exact >= integerBound
            ObjectiveTargetRelation.AtMost -> exact <= integerBound
        }
        if (!targetSatisfied) {
            return "见证解不满足目标边界 / witness does not satisfy the target bound"
        }
        return null
    }

    private fun targetIntegerBound(target: ObjectiveTarget): Int64? {
        return try {
            val rounding = when (target.relation) {
                ObjectiveTargetRelation.AtLeast -> RoundingMode.CEILING
                ObjectiveTargetRelation.AtMost -> RoundingMode.FLOOR
            }
            Int64(
                BigDecimal.valueOf(target.value.toSolverDouble("objectiveTarget.value"))
                    .setScale(0, rounding)
                    .longValueExact()
            )
        } catch (_: ArithmeticException) {
            null
        }
    }

    private data class CacheKey(
        val analyzer: TargetFeasibilityAnalyzer,
        val target: ObjectiveTarget,
        val options: ConstraintProgrammingSolveOptions
    )
}

private fun <T> propagateBuilderFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Ok -> Failed(
            ErrorCode.ApplicationError,
            "CP snapshot builder returned an invalid result state / CP snapshot builder returned an invalid result state"
        )
    }
}
