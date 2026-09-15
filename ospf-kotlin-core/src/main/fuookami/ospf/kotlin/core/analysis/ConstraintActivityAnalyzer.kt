/** Baseline CP activity analyzer. / CP 基线活动性分析器。 */
package fuookami.ospf.kotlin.core.analysis

import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.max
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Computes activity at one immutable CP snapshot and baseline assignment.
 *
 * Integer comparisons expose an exact scalar slack metric. Global constraints
 * and sparse domains are evaluated through their semantic contract and never
 * receive a fabricated LP-style slack.
 *
 * @property tolerance absolute active tolerance / Active 的绝对容差
 * @property nearlyActiveTolerance upper threshold for NearlyActive / NearlyActive 的上限
 * @property normalizationFloor denominator floor for normalized slack / 归一化松弛的分母下限
 */
class ConstraintActivityAnalyzer(
    val tolerance: Double = DEFAULT_TOLERANCE,
    val nearlyActiveTolerance: Double = tolerance * DEFAULT_NEARLY_ACTIVE_FACTOR,
    val normalizationFloor: Double = DEFAULT_NORMALIZATION_FLOOR
) {
    init {
        require(tolerance.isFinite() && tolerance >= 0.0) {
            "Activity tolerance must be finite and non-negative"
        }
        require(nearlyActiveTolerance.isFinite() && nearlyActiveTolerance >= tolerance) {
            "Nearly-active tolerance must be finite and no smaller than activity tolerance"
        }
        require(normalizationFloor.isFinite() && normalizationFloor > 0.0) {
            "Activity normalization floor must be finite and positive"
        }
    }

    /** Analyze the baseline held by a session, caching the immutable report. */
    fun analyze(session: CriticalConstraintAnalysisSession): Ret<ConstraintActivityReport> {
        return analyze(session, session.baselineSolution)
    }

    /** Analyze an explicit assignment within a session. */
    fun analyze(
        session: CriticalConstraintAnalysisSession,
        solution: ConstraintProgrammingSolution?
    ): Ret<ConstraintActivityReport> {
        invalidSessionResult<ConstraintActivityReport>(session)?.let { return it }
        if (session.isClosed) {
            return Failed(
                ErrorCode.ApplicationStopped,
                "活动性分析会话已关闭 / Activity analysis session is closed"
            )
        }
        val key = ActivityCacheKey(solution, tolerance, nearlyActiveTolerance, normalizationFloor)
        session.cached<ConstraintActivityReport>(AnalysisCacheKind.Activity, key)?.let {
            return ok(it)
        }
        return analyze(session.baselineSnapshot, solution).map { report ->
            session.cache(AnalysisCacheKind.Activity, key, report)
            report
        }
    }

    /** Analyze a snapshot; a null solution yields Unknown records. */
    fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        solution: ConstraintProgrammingSolution? = null
    ): Ret<ConstraintActivityReport> {
        if (!snapshot.validateIdentity()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份清单无效 / CP snapshot identity manifest is invalid"
            )
        }
        if (!snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 目标语义无效 / CP snapshot objective semantics are invalid"
            )
        }
        return ok(buildReport(snapshot, solution))
    }

    /** Analyze a snapshot with values keyed by stable variable ID. */
    fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        values: Map<VariableId, Int64>
    ): Ret<ConstraintActivityReport> {
        return analyze(snapshot, ConstraintProgrammingSolution(values = values))
    }

    /** Snapshot a mutable CP model and analyze its baseline assignment. */
    fun analyze(
        model: ConstraintProgrammingModel,
        solution: ConstraintProgrammingSolution? = null
    ): Ret<ConstraintActivityReport> {
        return when (val snapshot = model.snapshot()) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> analyze(snapshot.value!!, solution)
            is Failed -> Failed(snapshot.error)
            is Fatal -> Fatal(snapshot.errors)
        }
    }

    private fun buildReport(
        snapshot: ConstraintProgrammingModelSnapshot,
        solution: ConstraintProgrammingSolution?
    ): ConstraintActivityReport {
        val activities = ArrayList<ConstraintActivity>(
            snapshot.constraints.size + snapshot.variables.size * 2
        )
        snapshot.constraints.forEach { definition ->
            activities += activityForConstraint(definition, solution)
        }
        snapshot.variables.forEach { variable ->
            activities += activityForBound(
                variableId = variable.id,
                group = null,
                domain = variable.domain,
                side = BoundSide.Lower,
                solution = solution
            )
            activities += activityForBound(
                variableId = variable.id,
                group = null,
                domain = variable.domain,
                side = BoundSide.Upper,
                solution = solution
            )
            if (variable.domain is IntegerDomain.Values && variable.domain != IntegerDomain.boolean) {
                activities += activityForSparseDomain(variable.id, variable.domain, solution)
            }
        }
        return ConstraintActivityReport(
            activities = activities,
            groupSummaries = summarizeGroups(activities),
            tolerance = tolerance,
            nearlyActiveTolerance = nearlyActiveTolerance
        )
    }

    private fun activityForConstraint(
        definition: fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraintSnapshot,
        solution: ConstraintProgrammingSolution?
    ): ConstraintActivity {
        val source = DiagnosticSource.Constraint(definition.id)
        if (solution == null) {
            return unknownActivity(
                constraintId = definition.id,
                group = definition.groupName,
                source = source,
                kind = ActivityEvidenceKind.Unknown,
                message = "缺少基线 CP 解 / Baseline CP solution is missing"
            )
        }
        val values = solution.values
        val constraint = definition.constraint
        if (constraint is ConstraintProgrammingConstraint.IntegerComparison) {
            val evaluated = constraint.expression.evaluate(values)
            if (evaluated.failed || evaluated.value == null) {
                return unknownActivity(
                    constraintId = definition.id,
                    group = definition.groupName,
                    source = source,
                    kind = ActivityEvidenceKind.Unknown,
                    message = resultMessage(evaluated)
                )
            }
            val lhs = evaluated.value!!
            val exactSlack = comparisonSlack(lhs, constraint.rhs, constraint.comparison)
            val slack = reportMetric(exactSlack)
            val denominator = max(
                normalizationFloor,
                max(
                    abs(lhs.toFlt64().toSolverDouble("activity.lhs")),
                    abs(constraint.rhs.toFlt64().toSolverDouble("activity.rhs"))
                )
            )
            val normalizedSlack = slack / denominator
            val satisfied = when (constraint.comparison) {
                ConstraintProgrammingComparison.Equal -> lhs == constraint.rhs
                ConstraintProgrammingComparison.LessOrEqual -> lhs <= constraint.rhs
                ConstraintProgrammingComparison.GreaterOrEqual -> lhs >= constraint.rhs
            }
            val status = when (constraint.comparison) {
                ConstraintProgrammingComparison.Equal -> if (lhs == constraint.rhs) {
                    ActivityStatus.Active
                } else {
                    ActivityStatus.Violated
                }

                ConstraintProgrammingComparison.LessOrEqual,
                ConstraintProgrammingComparison.GreaterOrEqual -> classifySlack(slack)
            }
            return ConstraintActivity(
                constraintId = definition.id,
                group = definition.groupName,
                status = status,
                slack = slack,
                normalizedSlack = normalizedSlack,
                tolerance = tolerance,
                evidence = ActivityEvidence(
                    source = source,
                    kind = ActivityEvidenceKind.Comparison,
                    lhs = lhs.toFlt64().toSolverDouble("activity.lhs"),
                    rhs = constraint.rhs.toFlt64().toSolverDouble("activity.rhs"),
                    relation = constraint.comparison,
                    exactLhs = lhs.toLong().toString(),
                    exactRhs = constraint.rhs.toLong().toString(),
                    satisfied = satisfied
                )
            )
        }
        val evaluated = constraint.isSatisfied(values)
        if (evaluated.failed || evaluated.value == null) {
            return unknownActivity(
                constraintId = definition.id,
                group = definition.groupName,
                source = source,
                kind = ActivityEvidenceKind.Unknown,
                message = resultMessage(evaluated)
            )
        }
        val satisfied = evaluated.value!!
        // 只有在语义上存在无歧义标量"边界距离"的 global constraint 才给出 margin；其余按计划 2.2
        // 保持 `SatisfiedWithoutSlackMetric`，绝不伪造 slack。两端（Kotlin / Rust）语义一致。
        //
        // Only global constraints with an unambiguous scalar boundary distance receive a margin. The
        // rest keep `SatisfiedWithoutSlackMetric` per plan 2.2 and never receive fabricated slack. The
        // semantics match the Rust implementation.
        val margin = allDifferentMargin(constraint, values)
        if (margin != null) {
            return ConstraintActivity(
                constraintId = definition.id,
                group = definition.groupName,
                status = classifySlack(margin),
                slack = margin,
                normalizedSlack = margin,
                tolerance = tolerance,
                evidence = ActivityEvidence(
                    source = source,
                    kind = ActivityEvidenceKind.Semantic,
                    satisfied = satisfied
                )
            )
        }
        return ConstraintActivity(
            constraintId = definition.id,
            group = definition.groupName,
            status = if (satisfied) {
                ActivityStatus.SatisfiedWithoutSlackMetric
            } else {
                ActivityStatus.Violated
            },
            slack = null,
            normalizedSlack = null,
            tolerance = null,
            evidence = ActivityEvidence(
                source = source,
                kind = ActivityEvidenceKind.Semantic,
                satisfied = satisfied,
                message = if (satisfied) null else {
                    "CP 语义约束未满足 / CP semantic constraint is not satisfied"
                }
            )
        )
    }

    private fun activityForBound(
        variableId: VariableId,
        group: String?,
        domain: IntegerDomain,
        side: BoundSide,
        solution: ConstraintProgrammingSolution?
    ): ConstraintActivity {
        val source = when (side) {
            BoundSide.Lower -> DiagnosticSource.VariableLowerBound(variableId)
            BoundSide.Upper -> DiagnosticSource.VariableUpperBound(variableId)
        }
        val value = solution?.values?.get(variableId)
        if (value == null) {
            return unknownActivity(
                constraintId = null,
                group = group,
                source = source,
                kind = ActivityEvidenceKind.Unknown,
                message = "缺少变量赋值：$variableId / Variable assignment is missing: $variableId"
            )
        }
        val boundary = when (side) {
            BoundSide.Lower -> domain.lowerBound
            BoundSide.Upper -> domain.upperBound
        }
        val exactSlack = when (side) {
            BoundSide.Lower -> difference(value, boundary)
            BoundSide.Upper -> difference(boundary, value)
        }
        val slack = reportMetric(exactSlack)
        val denominator = max(
            normalizationFloor,
            max(
                abs(value.toFlt64().toSolverDouble("activity.value")),
                abs(boundary.toFlt64().toSolverDouble("activity.boundary"))
            )
        )
        return ConstraintActivity(
            constraintId = null,
            group = group,
            status = classifySlack(slack),
            slack = slack,
            normalizedSlack = slack / denominator,
            tolerance = tolerance,
            evidence = ActivityEvidence(
                source = source,
                kind = ActivityEvidenceKind.VariableBound,
                value = value.toFlt64().toSolverDouble("activity.value"),
                boundary = boundary.toFlt64().toSolverDouble("activity.boundary"),
                exactValue = value.toLong().toString(),
                exactBoundary = boundary.toLong().toString(),
                satisfied = slack >= 0.0
            )
        )
    }

    private fun activityForSparseDomain(
        variableId: VariableId,
        domain: IntegerDomain.Values,
        solution: ConstraintProgrammingSolution?
    ): ConstraintActivity {
        val source = DiagnosticSource.SparseDomain(variableId)
        val value = solution?.values?.get(variableId)
        if (value == null) {
            return unknownActivity(
                constraintId = null,
                group = null,
                source = source,
                kind = ActivityEvidenceKind.Unknown,
                message = "缺少变量赋值：$variableId / Variable assignment is missing: $variableId"
            )
        }
        val satisfied = value in domain
        return ConstraintActivity(
            constraintId = null,
            group = null,
            status = if (satisfied) ActivityStatus.SatisfiedWithoutSlackMetric else ActivityStatus.Violated,
            slack = null,
            normalizedSlack = null,
            tolerance = null,
            evidence = ActivityEvidence(
                source = source,
                kind = ActivityEvidenceKind.SparseDomain,
                value = value.toFlt64().toSolverDouble("activity.value"),
                exactValue = value.toLong().toString(),
                satisfied = satisfied,
                message = if (satisfied) null else {
                    "变量值不在稀疏值域中 / Variable value is outside the sparse domain"
                }
            )
        )
    }

    /**
     * CP global constraint 的自然 margin。
     *
     * 只有在**语义上无歧义**时才返回非 null：目前只有 `AllDifferent`。整数取值下它的自然标量
     * 距离是"最小的两两间距减一"——即还需要缩小多少间距才会违反约束：全异时 `minGap ≥ 1`，
     * margin `≥ 0`（`0` 表示已贴边，即存在相邻取值）；出现重复时 `minGap = 0`，margin `= -1`。
     * 该度量与 [classifySlack] 的有符号 slack 语义一致，因此可直接复用同一套
     * Active / NearlyActive / Inactive 判定。
     *
     * Natural margin for a CP global constraint. Returns non-null only when the metric is
     * **semantically unambiguous**, which today means `AllDifferent` alone. For integer values its
     * natural scalar distance is "smallest pairwise gap minus one", i.e. how much the gap would have to
     * shrink before the constraint is violated: when all values differ, `minGap ≥ 1` and the margin is
     * `≥ 0` (`0` means already at the boundary, i.e. adjacent values); with a duplicate, `minGap = 0`
     * and the margin is `-1`. This matches the signed slack semantics of [classifySlack], so the same
     * classification applies.
     */
    private fun allDifferentMargin(
        constraint: ConstraintProgrammingConstraint,
        values: Map<VariableId, Int64>
    ): Double? {
        val expressions = (constraint as? ConstraintProgrammingConstraint.AllDifferent)?.expressions
            ?: return null
        if (expressions.size < 2) {
            // 少于两个表达式时不存在两两间距。 / Fewer than two expressions means no pairwise gap.
            return null
        }
        // Keep the pairwise distance in BigInteger: two valid Int64 values can be separated by
        // more than Long.MAX_VALUE, and computing the gap as Long would wrap to a negative margin.
        // 使用 BigInteger 保存两两距离：两个合法 Int64 值的差距可能超过 Long.MAX_VALUE，若先用
        // Long 计算会回绕成负 margin。
        val evaluated = ArrayList<BigInteger>(expressions.size)
        for (expression in expressions) {
            val result = expression.evaluate(values)
            if (result.failed || result.value == null) {
                return null
            }
            evaluated += BigInteger.valueOf(result.value!!.toLong())
        }
        evaluated.sort()
        val minGap = evaluated.zipWithNext { left, right -> right.subtract(left) }.min()
        // 经受控转换边界把整数间距投影为标量 margin。 / Project the integer gap onto the scalar
        // margin through the controlled conversion boundary.
        return reportMetric(minGap.subtract(BigInteger.ONE))
    }

    private fun unknownActivity(
        constraintId: ConstraintId?,
        group: String?,
        source: DiagnosticSource,
        kind: ActivityEvidenceKind,
        message: String?
    ): ConstraintActivity {
        return ConstraintActivity(
            constraintId = constraintId,
            group = group,
            status = ActivityStatus.Unknown,
            slack = null,
            normalizedSlack = null,
            tolerance = null,
            evidence = ActivityEvidence(
                source = source,
                kind = kind,
                message = message ?: "CP 成员无法求值 / CP member could not be evaluated"
            )
        )
    }

    private fun classifySlack(slack: Double): ActivityStatus {
        val magnitude = abs(slack)
        if (slack < -tolerance) {
            return ActivityStatus.Violated
        }
        return when {
            magnitude <= tolerance -> ActivityStatus.Active
            magnitude <= nearlyActiveTolerance -> ActivityStatus.NearlyActive
            else -> ActivityStatus.Inactive
        }
    }

    private fun summarizeGroups(activities: List<ConstraintActivity>): List<ActivityGroupSummary> {
        val grouped = LinkedHashMap<String?, MutableList<ConstraintActivity>>()
        activities.filter { it.constraintId != null }.forEach { activity ->
            grouped.getOrPut(activity.group) { ArrayList() } += activity
        }
        return grouped.map { (group, members) ->
            ActivityGroupSummary(
                group = group,
                total = members.size,
                active = members.count { it.status == ActivityStatus.Active },
                nearlyActive = members.count { it.status == ActivityStatus.NearlyActive },
                inactive = members.count { it.status == ActivityStatus.Inactive },
                satisfiedWithoutSlackMetric = members.count {
                    it.status == ActivityStatus.SatisfiedWithoutSlackMetric
                },
                violated = members.count { it.status == ActivityStatus.Violated },
                unknown = members.count { it.status == ActivityStatus.Unknown }
            )
        }
    }

    private fun comparisonSlack(
        lhs: Int64,
        rhs: Int64,
        comparison: ConstraintProgrammingComparison
    ): BigInteger {
        val left = BigInteger.valueOf(lhs.toLong())
        val right = BigInteger.valueOf(rhs.toLong())
        return when (comparison) {
            // Equality has no one-sided slack: a non-zero residual is a violation.
            // 等式没有单侧 slack：非零残差必须作为违反处理。
            ConstraintProgrammingComparison.Equal -> right.subtract(left).let {
                if (it.signum() == 0) it else it.negate().abs()
            }
            ConstraintProgrammingComparison.LessOrEqual -> right.subtract(left)
            ConstraintProgrammingComparison.GreaterOrEqual -> left.subtract(right)
        }
    }

    private fun difference(left: Int64, right: Int64): BigInteger {
        return BigInteger.valueOf(left.toLong()).subtract(BigInteger.valueOf(right.toLong()))
    }

    /**
     * Project an exact slack onto the report's scalar metric.
     *
     * Classification always uses the exact value; this conversion only exists so
     * the public report can expose a scalar without inventing a second tolerance
     * boundary.
     * 将精确松弛投影为报告使用的标量度量。分类始终使用精确值，此转换仅用于让公开报告
     * 暴露标量，不引入第二套容差边界。
     */
    private fun reportMetric(value: BigInteger): Double {
        return java.lang.Double.valueOf(value.toString())
    }

    private fun resultMessage(result: Ret<*>): String {
        return when (result) {
            is Failed -> result.error.message
            is Fatal -> result.errors.joinToString(separator = "; ") { it.message }
            else -> "CP 求值结果无效 / CP evaluation returned an invalid result"
        }
    }

    private data class ActivityCacheKey(
        val solution: ConstraintProgrammingSolution?,
        val tolerance: Double,
        val nearlyActiveTolerance: Double,
        val normalizationFloor: Double
    )

    companion object {
        /** Default absolute tolerance. / 默认绝对容差。 */
        const val DEFAULT_TOLERANCE: Double = 1e-6

        /** Default multiplier used for NearlyActive. / NearlyActive 默认阈值倍数。 */
        const val DEFAULT_NEARLY_ACTIVE_FACTOR: Double = 10.0

        /** Default normalization floor. / 默认归一化分母下限。 */
        const val DEFAULT_NORMALIZATION_FLOOR: Double = 1.0
    }
}
