/**
 * CP snapshot 到精确线性模型的降阶器。 / Exact lowerer from a CP snapshot to a linear model.
 */
package fuookami.ospf.kotlin.core.solver.constraint_programming.lowering

import java.math.BigInteger
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.model.constraint_programming.ReificationDirection
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * 降阶后的线性模型及 CP 变量映射。[variables] 只包含原 CP 变量；降阶器生成的辅助变量不会暴露到 CP 解中。 / Lowered linear model and CP-variable mapping. / [variables] contains only original CP variables; generated auxiliaries are not exposed in CP solutions.
 *
 * @property model 降阶后的线性模型 / Lowered linear model
 * @property variables 原 CP 变量映射 / Original CP variable mapping
 * @property variableIds 原 CP 变量 ID 顺序 / Original CP variable ID order
 * @property intervals 原 interval 映射 / Original interval mapping
 * @property domains 原 CP 值域 / Original CP domains
 * @property artifacts 降阶 artifact 与源 CP 元素的映射 / Lowered artifacts and their CP source mappings
 */
data class ConstraintProgrammingLoweredLinearModel(
    val model: LinearMetaModel<Flt64>,
    val variables: Map<VariableId, AbstractVariableItem<*, *>>,
    val variableIds: List<VariableId>,
    val intervals: Map<IntervalId, IntervalVariable> = emptyMap(),
    val domains: Map<VariableId, IntegerDomain> = emptyMap(),
    val artifacts: Map<String, ConstraintProgrammingLoweredArtifact> = emptyMap()
) : AutoCloseable {
    override fun close() {
        model.close()
    }
}

/**
 * MIP lowerer artifact 与源 CP 元素的可回查映射。 / Stable reverse mapping from a MIP lowering artifact to its CP source.
 *
 * 辅助变量和约束拥有独立 ID，不会被误认为原始 CP 成员。 / Auxiliary variables and constraints have independent IDs and are never mistaken for source CP members.
 *
 * @property artifactId 降阶 artifact 稳定标识 / Stable lowered-artifact identifier
 * @property role artifact 角色 / Artifact role
 * @property originId 源 CP 元素 ID（无法唯一归属时为空） / Source CP element ID, or null when no unique source exists
 */
data class ConstraintProgrammingLoweredArtifact(
    val artifactId: String,
    val role: String,
    val originId: String? = null
)

/**
 * 将 CP 模型等价降为 OSPF 线性元模型。该实现只接受整数且具有有限、可精确转换为 IEEE double 的边界；任何近似或弱化线性化 / Lower a CP model equivalently to an OSPF linear meta model.
 * 都会返回 `Unsupported` 风格的结构化错误。 / The implementation accepts only bounded integer expressions whose coefficients and bounds are exactly representable by IEEE doubles; approximate or weakened linearizations return a structured unsupported error.
 *
 * @property policy 精确降阶策略 / Exact lowering policy
 */
class ConstraintProgrammingToLinearModelLowerer(
    private val policy: ConstraintProgrammingLoweringPolicy = ConstraintProgrammingLoweringPolicy.Strict
) {
    /**
     * 从可变 CP 模型生成线性模型。 / Lower a mutable CP model.
     *
     * @param model 可变 CP 模型 / Mutable CP model
     * @param assumptions 激活文字 / Assumption literals
     * @param fixedValues 固定变量值 / Fixed variable values
     * @return 降阶模型或结构化错误 / Lowered model or a structured error
     */
    fun lower(
        model: ConstraintProgrammingModel,
        assumptions: List<BooleanLiteral> = emptyList(),
        fixedValues: Map<VariableId, Int64> = emptyMap()
    ): Ret<ConstraintProgrammingLoweredLinearModel> {
        val snapshot = model.snapshot()
        if (snapshot.failed) {
            return propagate(snapshot)
        }
        return lower(snapshot.value!!, assumptions, fixedValues)
    }

    /**
     * 从 immutable snapshot 生成线性模型。 / Lower an immutable snapshot.
     *
     * @param snapshot CP 模型 snapshot / CP model snapshot
     * @param assumptions 激活文字 / Assumption literals
     * @param fixedValues 固定变量值 / Fixed variable values
     * @return 降阶模型或结构化错误 / Lowered model or a structured error
     */
    fun lower(
        snapshot: ConstraintProgrammingModelSnapshot,
        assumptions: List<BooleanLiteral> = emptyList(),
        fixedValues: Map<VariableId, Int64> = emptyMap()
    ): Ret<ConstraintProgrammingLoweredLinearModel> {
        val policyResult = policy.validate()
        if (policyResult.failed) {
            return propagate(policyResult)
        }
        val linear = LinearMetaModel(
            name = "${snapshot.name}-mip",
            objectCategory = snapshot.objectCategory,
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true)
        )
        val compiler = Compiler(linear, snapshot, policy)
        val result = compiler.compile(assumptions, fixedValues)
        if (result.failed) {
            linear.close()
            return propagate(result)
        }
        return ok(
            ConstraintProgrammingLoweredLinearModel(
                model = linear,
                variables = compiler.variables.toMap(),
                variableIds = snapshot.variables.map { it.id },
                intervals = snapshot.intervals.associateBy { it.id },
                domains = snapshot.variables.associate { it.id to it.domain },
                artifacts = compiler.artifacts.toMap()
            )
        )
    }

    private class Compiler(
        private val linear: LinearMetaModel<Flt64>,
        private val snapshot: ConstraintProgrammingModelSnapshot,
        private val policy: ConstraintProgrammingLoweringPolicy
    ) {
        val variables = LinkedHashMap<VariableId, AbstractVariableItem<*, *>>()
        private val domains = LinkedHashMap<AbstractVariableItem<*, *>, Pair<BigInteger, BigInteger>>()
        private val sourceReferences = LinkedHashMap<VariableId, AbstractVariableItem<*, *>>()
        private val intervals = LinkedHashMap<IntervalId, LoweredInterval>()
        val artifacts = LinkedHashMap<String, ConstraintProgrammingLoweredArtifact>()
        private var auxiliaryVariables = 0

        fun compile(
            assumptions: List<BooleanLiteral>,
            fixedValues: Map<VariableId, Int64>
        ): Try {
            collectReferences()
            val variablesResult = compileVariables()
            if (variablesResult.failed) {
                return propagate(variablesResult)
            }
            val intervalsResult = compileIntervals()
            if (intervalsResult.failed) {
                return propagate(intervalsResult)
            }
            for (entry in snapshot.constraints) {
                val result = compileConstraint(entry.constraint, entry.id.value)
                if (result.failed) {
                    return propagate(result)
                }
            }
            for ((index, assumption) in assumptions.withIndex()) {
                val form = literalForm(assumption)
                if (form.failed) {
                    return propagate(form)
                }
                val result = addConstraint(
                    form = form.value!!,
                    comparison = Comparison.EQ,
                    rhs = ONE,
                    name = "cp-assumption-$index"
                )
                if (result.failed) {
                    return result
                }
            }
            for ((id, value) in fixedValues) {
                val definition = snapshot.variable(id)
                    ?: return Failed(
                        ErrorCode.DataNotFound,
                        "固定值引用了未注册 CP 变量：$id / Fixed value references an unregistered CP variable: $id"
                    )
                if (!definition.domain.contains(value)) {
                    return Failed(
                        ErrorCode.ORSolutionInvalid,
                        "固定值超出 CP 变量值域：$id=$value / Fixed value is outside the CP variable domain: $id=$value"
                    )
                }
                val variable = variables[id]
                    ?: return Failed(
                        ErrorCode.DataNotFound,
                        "固定值缺少降阶变量：$id / Lowered model misses fixed CP variable: $id"
                    )
                val result = addConstraint(
                    form = form(variable),
                    comparison = Comparison.EQ,
                    rhs = BigInteger.valueOf(value.toLong()),
                    name = "cp-fixed-${sanitize(id.value)}"
                )
                if (result.failed) {
                    return result
                }
            }
            return compileObjectives()
        }

        private fun collectReferences() {
            snapshot.expressions.forEach { collectExpression(it.expression) }
            snapshot.objectives.forEach { collectExpression(it.expression) }
            snapshot.intervals.forEach { collectInterval(it) }
            snapshot.constraints.forEach { collectConstraint(it.constraint) }
        }

        private fun collectExpression(expression: ConstraintProgrammingExpression) {
            when (expression) {
                is ConstraintProgrammingExpression.Constant,
                is ConstraintProgrammingExpression.Invalid -> {}
                is ConstraintProgrammingExpression.Variable -> {
                    sourceReferences.putIfAbsent(expression.variableId, expression.variable)
                }
                is ConstraintProgrammingExpression.Linear -> expression.terms.forEach {
                    sourceReferences.putIfAbsent(it.variableId, it.variable)
                }
            }
        }

        private fun collectLiteral(literal: BooleanLiteral) {
            literal.variableId?.let { id -> literal.variable?.let { sourceReferences.putIfAbsent(id, it) } }
        }

        private fun collectInterval(interval: IntervalVariable) {
            collectExpression(interval.start)
            collectExpression(interval.size)
            collectExpression(interval.end)
            interval.presence?.let(::collectLiteral)
        }

        private fun collectConstraint(constraint: ConstraintProgrammingConstraint) {
            when (constraint) {
                is ConstraintProgrammingConstraint.IntegerComparison -> collectExpression(constraint.expression)
                is ConstraintProgrammingConstraint.BoolAnd -> constraint.literals.forEach(::collectLiteral)
                is ConstraintProgrammingConstraint.BoolOr -> constraint.literals.forEach(::collectLiteral)
                is ConstraintProgrammingConstraint.BoolXor -> constraint.literals.forEach(::collectLiteral)
                is ConstraintProgrammingConstraint.Literal -> collectLiteral(constraint.literal)
                is ConstraintProgrammingConstraint.Implication -> {
                    collectLiteral(constraint.enforcement)
                    collectConstraint(constraint.constraint)
                }
                is ConstraintProgrammingConstraint.Reified -> {
                    collectLiteral(constraint.literal)
                    collectConstraint(constraint.constraint)
                }
                is ConstraintProgrammingConstraint.AllDifferent -> constraint.expressions.forEach(::collectExpression)
                is ConstraintProgrammingConstraint.Element -> {
                    collectExpression(constraint.index)
                    collectExpression(constraint.target)
                    constraint.values.filterIsInstance<ConstraintProgrammingExpression>().forEach(::collectExpression)
                }
                is ConstraintProgrammingConstraint.AllowedAssignments -> constraint.expressions.forEach(::collectExpression)
                is ConstraintProgrammingConstraint.ForbiddenAssignments -> constraint.expressions.forEach(::collectExpression)
                is NoOverlap -> constraint.intervals.forEach(::collectInterval)
                is Cumulative -> {
                    constraint.intervals.forEach(::collectInterval)
                    constraint.demands.forEach(::collectExpression)
                    collectExpression(constraint.capacity)
                }
                is ConstraintProgrammingConstraint.Circuit -> constraint.successors.forEach(::collectExpression)
                is ConstraintProgrammingConstraint.Automaton -> constraint.expressions.forEach(::collectExpression)
                is ConstraintProgrammingConstraint.Reservoir -> constraint.events.forEach {
                    collectExpression(it.time)
                    collectExpression(it.levelChange)
                }
            }
        }

        private fun compileVariables(): Try {
            for (definition in snapshot.variables) {
                val variable = if (definition.domain == IntegerDomain.boolean) {
                    BinVar("cp-${sanitize(definition.id.value)}")
                } else {
                    IntVar("cp-${sanitize(definition.id.value)}")
                }
                if (variable is IntVar &&
                    !variable.range.intersectWith(
                        definition.domain.lowerBound,
                        definition.domain.upperBound
                    )
                ) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "CP 变量值域为空：${definition.id} / CP variable domain is empty: ${definition.id}"
                    )
                }
                val added = linear.add(variable)
                if (added.failed) {
                    return propagate(added)
                }
                variables[definition.id] = variable
                registerArtifact(
                    artifactId = "variable:${definition.id.value}",
                    role = "source-variable",
                    originId = definition.id.value
                )
                val bounds = domainBounds(definition.domain)
                val lowerBound = safeFlt64(bounds.first, "CP variable lower bound")
                if (lowerBound.failed) return propagate(lowerBound)
                val upperBound = safeFlt64(bounds.second, "CP variable upper bound")
                if (upperBound.failed) return propagate(upperBound)
                domains[variable] = bounds
                val sparse = definition.domain as? IntegerDomain.Values
                if (sparse != null && definition.domain != IntegerDomain.boolean) {
                    if (sparse.values.size > policy.sparseDomainLimit) {
                        return unsupported("稀疏值域超出降阶规模上限 / Sparse domain exceeds the lowering limit")
                    }
                    val result = compileSparseDomain(variable, sparse.values, definition.id.value)
                    if (result.failed) {
                        return propagate(result)
                    }
                }
            }
            return ok
        }

        private fun compileSparseDomain(
            variable: AbstractVariableItem<*, *>,
            values: List<Int64>,
            name: String
        ): Try {
            val selectors = ArrayList<AbstractVariableItem<*, *>>(values.size)
            for (index in values.indices) {
                val selector = auxiliaryBinary("domain-${sanitize(name)}-$index")
                if (selector.failed) {
                    return propagate(selector)
                }
                selectors += selector.value!!
            }
            val one = addConstraint(
                form = form(selectors.associateWith { ONE_BI }),
                comparison = Comparison.EQ,
                rhs = ONE,
                name = "cp-domain-${sanitize(name)}-one"
            )
            if (one.failed) {
                return one
            }
            val linkTerms = LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>()
            linkTerms[variable] = ONE_BI
            selectors.forEachIndexed { index, selector ->
                linkTerms[selector] = -BigInteger.valueOf(values[index].toLong())
            }
            return addConstraint(
                form = form(linkTerms),
                comparison = Comparison.EQ,
                rhs = ZERO,
                name = "cp-domain-${sanitize(name)}-link"
            )
        }

        private fun compileIntervals(): Try {
            for (interval in snapshot.intervals) {
                val start = linearForm(interval.start)
                val end = linearForm(interval.end)
                if (start.failed) return propagate(start)
                if (end.failed) return propagate(end)
                val size = linearForm(interval.size)
                if (size.failed) return propagate(size)
                val sizeBounds = formBounds(size.value!!)
                    ?: return unsupported("变量 duration 需要有限值域 / Variable duration requires finite bounds")
                val presence = interval.presence?.let { literalForm(it) }
                if (presence != null && presence.failed) return propagate(presence)
                val intervalName = sanitize(interval.id.value)
                val linkForm = plus(
                    combine(end.value!!, start.value!!, ONE_BI, -ONE_BI),
                    size.value!!.negate()
                )
                val linkBounds = formBounds(linkForm)
                    ?: return unsupported("interval link 需要有限值域 / Interval link requires finite bounds")
                if (presence == null) {
                    val nonNegative = addConstraint(
                        form = size.value!!,
                        comparison = Comparison.GE,
                        rhs = ZERO,
                        name = "cp-interval-$intervalName-non-negative"
                    )
                    if (nonNegative.failed) return nonNegative
                    val link = addConstraint(
                        form = linkForm,
                        comparison = Comparison.EQ,
                        rhs = ZERO,
                        name = "cp-interval-$intervalName-link"
                    )
                    if (link.failed) return link
                } else {
                    // Optional intervals impose these semantics only when presence is true.
                    // 可选 interval 仅在 presence 为真时施加这些语义。
                    val gatedNonNegative = addConstraint(
                        form = combine(size.value!!, presence.value!!, ONE_BI, sizeBounds.first),
                        comparison = Comparison.GE,
                        rhs = sizeBounds.first,
                        name = "cp-interval-$intervalName-non-negative-gated"
                    )
                    if (gatedNonNegative.failed) return gatedNonNegative
                    val gatedUpper = addConstraint(
                        form = combine(linkForm, presence.value!!, ONE_BI, linkBounds.second),
                        comparison = Comparison.LE,
                        rhs = linkBounds.second,
                        name = "cp-interval-$intervalName-link-upper"
                    )
                    if (gatedUpper.failed) return gatedUpper
                    val gatedLower = addConstraint(
                        form = combine(linkForm, presence.value!!, ONE_BI, linkBounds.first),
                        comparison = Comparison.GE,
                        rhs = linkBounds.first,
                        name = "cp-interval-$intervalName-link-lower"
                    )
                    if (gatedLower.failed) return gatedLower
                }
                val startVariable = asVariable(interval.start)
                    ?: return unsupported("interval start 必须是标量变量 / Interval start must be a scalar variable")
                val endVariable = asVariable(interval.end)
                    ?: return unsupported("interval end 必须是标量变量 / Interval end must be a scalar variable")
                intervals[interval.id] = LoweredInterval(
                    start = startVariable,
                    end = endVariable,
                    size = size.value!!,
                    presence = presence?.value
                )
            }
            return ok
        }

        private fun compileConstraint(constraint: ConstraintProgrammingConstraint, name: String): Try {
            return when (constraint) {
                is ConstraintProgrammingConstraint.IntegerComparison -> {
                    val form = linearForm(constraint.expression)
                    if (form.failed) propagate(form) else addConstraint(
                        form = form.value!!,
                        comparison = comparison(constraint.comparison),
                        rhs = BigInteger.valueOf(constraint.rhs.toLong()),
                        name = name
                    )
                }
                is ConstraintProgrammingConstraint.Literal -> {
                    val form = literalForm(constraint.literal)
                    if (form.failed) propagate(form) else addConstraint(form.value!!, Comparison.EQ, ONE, name)
                }
                is ConstraintProgrammingConstraint.BoolAnd -> {
                    for ((index, literal) in constraint.literals.withIndex()) {
                        val form = literalForm(literal)
                        if (form.failed) return propagate(form)
                        val result = addConstraint(form.value!!, Comparison.EQ, ONE, "$name-and-$index")
                        if (result.failed) return result
                    }
                    ok
                }
                is ConstraintProgrammingConstraint.BoolOr -> {
                    val forms = constraint.literals.map(::literalForm)
                    val failed = forms.firstOrNull { it.failed }
                    if (failed != null) propagate(failed) else addConstraint(
                        forms.map { it.value!! }.reduceOrNull(::plus)
                            ?: form(constant = ZERO),
                        Comparison.GE,
                        ONE,
                        name
                    )
                }
                is ConstraintProgrammingConstraint.BoolXor -> {
                    val forms = constraint.literals.map(::literalForm)
                    val failed = forms.firstOrNull { it.failed }
                    if (failed != null) propagate(failed) else addConstraint(
                        forms.map { it.value!! }.reduceOrNull(::plus)
                            ?: form(constant = ZERO),
                        Comparison.EQ,
                        ONE,
                        name
                    )
                }
                is ConstraintProgrammingConstraint.Implication -> compileImplication(constraint, name)
                is ConstraintProgrammingConstraint.Reified -> compileReified(constraint, name)
                is ConstraintProgrammingConstraint.AllDifferent -> compileAllDifferent(constraint, name)
                is ConstraintProgrammingConstraint.Element -> compileElement(constraint, name)
                is ConstraintProgrammingConstraint.AllowedAssignments -> compileAllowedAssignments(constraint, name)
                is ConstraintProgrammingConstraint.ForbiddenAssignments -> compileForbiddenAssignments(constraint, name)
                is NoOverlap -> compileNoOverlap(constraint, name)
                is Cumulative -> if (policy.allowCumulative) {
                    unsupported("time-indexed cumulative 降阶尚未启用 / Time-indexed cumulative lowering is not enabled")
                } else {
                    unsupported("MIP-backed CP 首版不支持 cumulative / Cumulative is unsupported by the first MIP-backed CP compiler")
                }
                is ConstraintProgrammingConstraint.Circuit -> unsupported(
                    "MIP-backed CP 首版不支持 Circuit / Circuit is unsupported by the first MIP-backed CP compiler"
                )
                is ConstraintProgrammingConstraint.Automaton -> unsupported(
                    "MIP-backed CP 首版不支持 Automaton / Automaton is unsupported by the first MIP-backed CP compiler"
                )
                is ConstraintProgrammingConstraint.Reservoir -> unsupported(
                    "MIP-backed CP 首版不支持 Reservoir / Reservoir is unsupported by the first MIP-backed CP compiler"
                )
            }
        }

        private fun compileImplication(
            implication: ConstraintProgrammingConstraint.Implication,
            name: String
        ): Try {
            val enforcement = literalForm(implication.enforcement)
            if (enforcement.failed) return propagate(enforcement)
            return when (val child = implication.constraint) {
                is ConstraintProgrammingConstraint.IntegerComparison -> compileGatedComparison(
                    enforcement.value!!,
                    child.expression,
                    child.comparison,
                    BigInteger.valueOf(child.rhs.toLong()),
                    name
                )
                is ConstraintProgrammingConstraint.Literal -> {
                    val consequence = literalForm(child.literal)
                    if (consequence.failed) propagate(consequence) else addConstraint(
                        combine(consequence.value!!, enforcement.value!!, ONE_BI, -ONE_BI),
                        Comparison.GE,
                        ZERO,
                        name
                    )
                }
                else -> unsupported("复杂逻辑 implication 不支持精确降阶 / Complex logical implications are unsupported by exact lowering")
            }
        }

        private fun compileReified(
            reified: ConstraintProgrammingConstraint.Reified,
            name: String
        ): Try {
            val literal = literalForm(reified.literal)
            if (literal.failed) return propagate(literal)
            val child = reified.constraint as? ConstraintProgrammingConstraint.IntegerComparison
                ?: (reified.constraint as? ConstraintProgrammingConstraint.Literal)?.let { literalConstraint ->
                    return compileReifiedLiteral(literal.value!!, literalConstraint.literal, reified.direction, name)
                }
                ?: return unsupported("仅整数比较和文字支持精确 reification / Exact reification supports integer comparisons and literals only")
            val expression = linearForm(child.expression)
            if (expression.failed) return propagate(expression)
            val rhs = BigInteger.valueOf(child.rhs.toLong())
            return when (reified.direction) {
                ReificationDirection.Implies -> compileGatedComparison(literal.value!!, child.expression, child.comparison, rhs, name)
                ReificationDirection.ImpliedBy -> {
                    val negatedLiteral = literalForm(reified.literal.negate())
                    if (negatedLiteral.failed) {
                        propagate(negatedLiteral)
                    } else {
                        compileGatedNegatedComparison(
                            negatedLiteral.value!!,
                            expression.value!!,
                            child.comparison,
                            rhs,
                            name
                        )
                    }
                }
                ReificationDirection.Equivalent -> {
                    val forward = compileGatedComparison(literal.value!!, child.expression, child.comparison, rhs, "$name-forward")
                    if (forward.failed) {
                        forward
                    } else {
                        val negatedLiteral = literalForm(reified.literal.negate())
                        if (negatedLiteral.failed) {
                            propagate(negatedLiteral)
                        } else {
                            compileGatedNegatedComparison(
                                negatedLiteral.value!!,
                                expression.value!!,
                                child.comparison,
                                rhs,
                                "$name-reverse"
                            )
                        }
                    }
                }
            }
        }

        private fun compileReifiedLiteral(
            literal: LinearForm,
            child: BooleanLiteral,
            direction: ReificationDirection,
            name: String
        ): Try {
            val consequence = literalForm(child)
            if (consequence.failed) return propagate(consequence)
            return when (direction) {
                ReificationDirection.Implies -> addConstraint(combine(consequence.value!!, literal, ONE_BI, -ONE_BI), Comparison.GE, ZERO, name)
                ReificationDirection.ImpliedBy -> addConstraint(combine(literal, consequence.value!!, ONE_BI, -ONE_BI), Comparison.GE, ZERO, name)
                ReificationDirection.Equivalent -> {
                    val forward = addConstraint(combine(consequence.value!!, literal, ONE_BI, -ONE_BI), Comparison.EQ, ZERO, "$name-forward")
                    if (forward.failed) forward else addConstraint(combine(literal, consequence.value!!, ONE_BI, -ONE_BI), Comparison.EQ, ZERO, "$name-reverse")
                }
            }
        }

        private fun compileGatedComparison(
            enforcement: LinearForm,
            expression: ConstraintProgrammingExpression,
            comparison: ConstraintProgrammingComparison,
            rhs: BigInteger,
            name: String
        ): Try {
            val form = linearForm(expression)
            if (form.failed) return propagate(form)
            return compileGatedComparison(enforcement, form.value!!, comparison, rhs, name)
        }

        private fun compileGatedComparison(
            enforcement: LinearForm,
            form: LinearForm,
            comparison: ConstraintProgrammingComparison,
            rhs: BigInteger,
            name: String
        ): Try {
            val bounds = formBounds(form)
                ?: return unsupported("indicator/reification 需要有限表达式界 / Indicators and reification require finite expression bounds")
            return when (comparison) {
                ConstraintProgrammingComparison.LessOrEqual -> {
                    val m = maxOf(ONE_BI, bounds.second - rhs)
                    addConstraint(combine(form, enforcement, ONE_BI, m), Comparison.LE, rhs + m, name)
                }
                ConstraintProgrammingComparison.GreaterOrEqual -> {
                    val m = maxOf(ONE_BI, rhs - bounds.first)
                    addConstraint(combine(form, enforcement, ONE_BI, -m), Comparison.GE, rhs - m, name)
                }
                ConstraintProgrammingComparison.Equal -> {
                    val upper = compileGatedComparison(enforcement, form, ConstraintProgrammingComparison.LessOrEqual, rhs, "$name-le")
                    if (upper.failed) upper else compileGatedComparison(enforcement, form, ConstraintProgrammingComparison.GreaterOrEqual, rhs, "$name-ge")
                }
            }
        }

        private fun compileGatedNegatedComparison(
            enforcement: LinearForm,
            form: LinearForm,
            comparison: ConstraintProgrammingComparison,
            rhs: BigInteger,
            name: String
        ): Try {
            return when (comparison) {
                ConstraintProgrammingComparison.LessOrEqual -> compileGatedComparison(
                    enforcement,
                    form,
                    ConstraintProgrammingComparison.GreaterOrEqual,
                    rhs + ONE_BI,
                    name
                )
                ConstraintProgrammingComparison.GreaterOrEqual -> compileGatedComparison(
                    enforcement,
                    form,
                    ConstraintProgrammingComparison.LessOrEqual,
                    rhs - ONE_BI,
                    name
                )
                ConstraintProgrammingComparison.Equal -> compileGatedNotEqual(enforcement, form, rhs, name)
            }
        }

        private fun compileGatedNotEqual(
            enforcement: LinearForm,
            form: LinearForm,
            rhs: BigInteger,
            name: String
        ): Try {
            val bounds = formBounds(form)
                ?: return unsupported("不等式 reification 需要有限表达式界 / Not-equal reification requires finite expression bounds")
            val left = auxiliaryBinary("$name-left")
            if (left.failed) return propagate(left)
            val right = auxiliaryBinary("$name-right")
            if (right.failed) return propagate(right)
            val atLeast = addConstraint(
                combine(form(left.value!!, right.value!!), enforcement, ONE_BI, -ONE_BI),
                Comparison.GE,
                ZERO,
                "$name-branch"
            )
            if (atLeast.failed) return atLeast
            val leftM = maxOf(ONE_BI, bounds.second - (rhs - ONE_BI))
            val leftResult = addConstraint(
                combine(form, form(left.value!!), ONE_BI, leftM),
                Comparison.LE,
                rhs - ONE_BI + leftM,
                "$name-left-link"
            )
            if (leftResult.failed) return leftResult
            val rightM = maxOf(ONE_BI, (rhs + ONE_BI) - bounds.first)
            return addConstraint(
                combine(form, form(right.value!!), ONE_BI, -rightM),
                Comparison.GE,
                rhs + ONE_BI - rightM,
                "$name-right-link"
            )
        }

        private fun compileAllDifferent(
            constraint: ConstraintProgrammingConstraint.AllDifferent,
            name: String
        ): Try {
            if (constraint.expressions.size > policy.decompositionLimit) {
                return unsupported("AllDifferent 分解规模超限 / AllDifferent decomposition exceeds the limit")
            }
            for (i in constraint.expressions.indices) {
                for (j in i + 1 until constraint.expressions.size) {
                    val left = linearForm(constraint.expressions[i])
                    val right = linearForm(constraint.expressions[j])
                    if (left.failed) return propagate(left)
                    if (right.failed) return propagate(right)
                    val difference = combine(left.value!!, right.value!!, ONE_BI, -ONE_BI)
                    val bounds = formBounds(difference)
                        ?: return unsupported("AllDifferent 需要有限表达式界 / AllDifferent requires finite expression bounds")
                    val m = maxOf(bounds.first.abs(), bounds.second.abs()) + ONE_BI
                    val order = auxiliaryBinary("$name-order-$i-$j")
                    if (order.failed) return propagate(order)
                    val upper = addConstraint(
                        combine(difference, form(order.value!!), ONE_BI, m),
                        Comparison.LE,
                        m - ONE_BI,
                        "$name-upper-$i-$j"
                    )
                    if (upper.failed) return upper
                    val lower = addConstraint(
                        combine(difference.negate(), form(order.value!!), ONE_BI, -m),
                        Comparison.LE,
                        -ONE_BI,
                        "$name-lower-$i-$j"
                    )
                    if (lower.failed) return lower
                }
            }
            return ok
        }

        private fun compileElement(
            constraint: ConstraintProgrammingConstraint.Element,
            name: String
        ): Try {
            if (constraint.values.size > policy.decompositionLimit) {
                return unsupported("Element 分解规模超限 / Element decomposition exceeds the limit")
            }
            val constants = constraint.values.map { value ->
                when (value) {
                    is Int64 -> value
                    is Long -> Int64(value)
                    is Int -> Int64(value.toLong())
                    else -> return unsupported("Element 首版只支持常量数组 / The first Element lowerer supports constant arrays only")
                }
            }
            val selectors = constants.indices.map { index -> auxiliaryBinary("$name-select-$index") }
            val failed = selectors.firstOrNull { it.failed }
            if (failed != null) return propagate(failed)
            val selectorValues = selectors.map { it.value!! }
            val one = addConstraint(form(selectorValues.associateWith { ONE_BI }), Comparison.EQ, ONE, "$name-one")
            if (one.failed) return one
            val index = linearForm(constraint.index)
            val target = linearForm(constraint.target)
            if (index.failed) return propagate(index)
            if (target.failed) return propagate(target)
            val indexTerms = LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>()
            indexTerms.putAll(index.value!!.terms)
            selectorValues.forEachIndexed { position, selector ->
                indexTerms[selector] = (indexTerms[selector] ?: ZERO_BI) - BigInteger.valueOf(position.toLong())
            }
            val targetTerms = LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>()
            targetTerms.putAll(target.value!!.terms)
            selectorValues.forEachIndexed { position, selector ->
                targetTerms[selector] = (targetTerms[selector] ?: ZERO_BI) - BigInteger.valueOf(constants[position].toLong())
            }
            val indexResult = addConstraint(index.value!!.copy(terms = indexTerms), Comparison.EQ, ZERO, "$name-index")
            if (indexResult.failed) return indexResult
            return addConstraint(target.value!!.copy(terms = targetTerms), Comparison.EQ, ZERO, "$name-target")
        }

        private fun compileAllowedAssignments(
            constraint: ConstraintProgrammingConstraint.AllowedAssignments,
            name: String
        ): Try {
            if (constraint.tuples.size > policy.decompositionLimit) {
                return unsupported("Allowed table 分解规模超限 / Allowed table decomposition exceeds the limit")
            }
            val selectors = constraint.tuples.indices.map { auxiliaryBinary("$name-tuple-$it") }
            val failed = selectors.firstOrNull { it.failed }
            if (failed != null) return propagate(failed)
            val selectorValues = selectors.map { it.value!! }
            val one = addConstraint(form(selectorValues.associateWith { ONE_BI }), Comparison.EQ, ONE, "$name-one")
            if (one.failed) return one
            for (column in constraint.expressions.indices) {
                val expression = linearForm(constraint.expressions[column])
                if (expression.failed) return propagate(expression)
                val terms = LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>()
                terms.putAll(expression.value!!.terms)
                selectorValues.forEachIndexed { row, selector ->
                    terms[selector] = (terms[selector] ?: ZERO_BI) - BigInteger.valueOf(constraint.tuples[row][column].toLong())
                }
                val link = addConstraint(expression.value!!.copy(terms = terms), Comparison.EQ, ZERO, "$name-link-$column")
                if (link.failed) return link
            }
            return ok
        }

        private fun compileForbiddenAssignments(
            constraint: ConstraintProgrammingConstraint.ForbiddenAssignments,
            name: String
        ): Try {
            if (!policy.allowForbiddenAssignments) {
                return unsupported("策略禁止 forbidden table / The policy disables forbidden tables")
            }
            if (constraint.tuples.size > policy.decompositionLimit) {
                return unsupported("Forbidden table 分解规模超限 / Forbidden table decomposition exceeds the limit")
            }
            for ((row, tuple) in constraint.tuples.withIndex()) {
                val differences = ArrayList<AbstractVariableItem<*, *>>()
                for (column in constraint.expressions.indices) {
                    val expression = linearForm(constraint.expressions[column])
                    if (expression.failed) return propagate(expression)
                    val bounds = formBounds(expression.value!!)
                        ?: return unsupported("Forbidden table 需要有限表达式界 / Forbidden table requires finite expression bounds")
                    val value = BigInteger.valueOf(tuple[column].toLong())
                    val left = auxiliaryBinary("$name-$row-$column-left")
                    if (left.failed) return propagate(left)
                    val right = auxiliaryBinary("$name-$row-$column-right")
                    if (right.failed) return propagate(right)
                    differences += left.value!!
                    differences += right.value!!
                    val leftM = maxOf(ONE_BI, bounds.second - (value - ONE_BI))
                    val leftResult = addConstraint(
                        combine(expression.value!!, form(left.value!!), ONE_BI, leftM),
                        Comparison.LE,
                        value - ONE_BI + leftM,
                        "$name-$row-$column-left-link"
                    )
                    if (leftResult.failed) return leftResult
                    val rightM = maxOf(ONE_BI, value + ONE_BI - bounds.first)
                    val rightResult = addConstraint(
                        combine(expression.value!!, form(right.value!!), ONE_BI, -rightM),
                        Comparison.GE,
                        value + ONE_BI - rightM,
                        "$name-$row-$column-right-link"
                    )
                    if (rightResult.failed) return rightResult
                }
                val result = addConstraint(
                    form(differences.associateWith { ONE_BI }),
                    Comparison.GE,
                    ONE,
                    "$name-$row-difference"
                )
                if (result.failed) return result
            }
            return ok
        }

        private fun compileNoOverlap(noOverlap: NoOverlap, name: String): Try {
            if (noOverlap.intervals.size > policy.decompositionLimit) {
                return unsupported("NoOverlap 分解规模超限 / NoOverlap decomposition exceeds the limit")
            }
            for (i in noOverlap.intervals.indices) {
                for (j in i + 1 until noOverlap.intervals.size) {
                    val first = intervals[noOverlap.intervals[i].id]
                        ?: return Failed(ErrorCode.IllegalArgument, "NoOverlap 引用了未注册 interval / NoOverlap references an unknown interval")
                    val second = intervals[noOverlap.intervals[j].id]
                        ?: return Failed(ErrorCode.IllegalArgument, "NoOverlap 引用了未注册 interval / NoOverlap references an unknown interval")
                    val firstBounds = formBounds(form(first.start))
                        ?: return unsupported("NoOverlap 需要有限 start 值域 / NoOverlap requires finite start bounds")
                    val secondBounds = formBounds(form(second.start))
                        ?: return unsupported("NoOverlap 需要有限 start 值域 / NoOverlap requires finite start bounds")
                    val firstSizeBounds = formBounds(first.size)
                        ?: return unsupported("NoOverlap 需要有限 duration 值域 / NoOverlap requires finite duration bounds")
                    val secondSizeBounds = formBounds(second.size)
                        ?: return unsupported("NoOverlap 需要有限 duration 值域 / NoOverlap requires finite duration bounds")
                    val m = maxOf(
                        ONE_BI,
                        firstBounds.second + firstSizeBounds.second - secondBounds.first,
                        secondBounds.second + secondSizeBounds.second - firstBounds.first
                    )
                    val order = auxiliaryBinary("$name-order-$i-$j")
                    if (order.failed) return propagate(order)
                    val firstPresence = first.presence ?: form(constant = ONE_BI)
                    val secondPresence = second.presence ?: form(constant = ONE_BI)
                    val firstBefore = addConstraint(
                                plus(
                                    plus(
                                        plus(combine(form(first.start), first.size, ONE_BI, ONE_BI), form(second.start).negate()),
                                        form(order.value!!).let { combine(it, firstPresence, m, m) }
                                    ),
                                    combine(secondPresence, form(constant = ZERO_BI), m, ZERO_BI)
                                ),
                        Comparison.LE,
                        m * BigInteger.valueOf(3),
                        "$name-first-before-$i-$j"
                    )
                    if (firstBefore.failed) return firstBefore
                    val secondBefore = addConstraint(
                                plus(
                                    plus(
                                        plus(combine(form(second.start), second.size, ONE_BI, ONE_BI), form(first.start).negate()),
                                        combine(form(order.value!!), firstPresence, -m, m)
                                    ),
                                    combine(secondPresence, form(constant = ZERO_BI), m, ZERO_BI)
                                ),
                        Comparison.LE,
                        m * BigInteger.valueOf(2),
                        "$name-second-before-$i-$j"
                    )
                    if (secondBefore.failed) return secondBefore
                }
            }
            return ok
        }

        private fun compileObjectives(): Try {
            if (snapshot.objectives.size > 1) {
                return unsupported("MIP-backed CP 首版只支持单一目标 / The first MIP-backed CP lowerer supports one objective")
            }
            val objective = snapshot.objectives.firstOrNull() ?: return ok
            if (objective.category != snapshot.objectCategory) {
                return unsupported("目标方向与模型方向不一致 / Objective category does not match the model category")
            }
            val expression = linearForm(objective.expression)
            if (expression.failed) return propagate(expression)
            val polynomial = expression.value!!.toPolynomial()
            if (polynomial.failed) return propagate(polynomial)
            return try {
                linear.addObject(
                    category = objective.category,
                    polynomial = polynomial.value!!,
                    name = objective.name,
                    displayName = null
                )
            } catch (error: IllegalArgumentException) {
                Failed(ErrorCode.Other, error.message)
            }
        }

        private fun linearForm(expression: ConstraintProgrammingExpression): Ret<LinearForm> {
            return when (expression) {
                is ConstraintProgrammingExpression.Constant -> safeConstant(expression.value)
                    .map { value -> form(constant = value) }
                is ConstraintProgrammingExpression.Invalid -> Failed(ErrorCode.IllegalArgument, expression.message)
                is ConstraintProgrammingExpression.Variable -> variables[expression.variableId]?.let {
                    ok(form(terms = linkedMapOf(it to ONE_BI)))
                } ?: Failed(ErrorCode.IllegalArgument, "CP 表达式变量未注册 / CP expression variable is not registered")
                is ConstraintProgrammingExpression.Linear -> {
                    val terms = LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>()
                    var constant = ZERO_BI
                    for (term in expression.terms) {
                        val variable = variables[term.variableId]
                            ?: return Failed(ErrorCode.IllegalArgument, "CP 线性项变量未注册 / CP linear term variable is not registered")
                        val coefficient = BigInteger.valueOf(term.coefficient.toLong())
                        terms[variable] = (terms[variable] ?: ZERO_BI) + coefficient
                    }
                    constant += BigInteger.valueOf(expression.constant.toLong())
                    ok(form(terms, constant))
                }
            }
        }

        private fun literalForm(literal: BooleanLiteral): Ret<LinearForm> {
            val constant = literal.constant
            if (constant != null) {
                return ok(form(constant = if (constant) ONE_BI else ZERO_BI))
            }
            val variable = literal.variableId?.let(variables::get)
                ?: return Failed(ErrorCode.IllegalArgument, "布尔文字变量未注册 / Boolean literal variable is not registered")
            return if (literal.negated) {
                ok(form(terms = linkedMapOf(variable to -ONE_BI), constant = ONE_BI))
            } else {
                ok(form(terms = linkedMapOf(variable to ONE_BI)))
            }
        }

        private fun auxiliaryBinary(name: String): Ret<AbstractVariableItem<*, *>> {
            if (++auxiliaryVariables > policy.auxiliaryVariableLimit) {
                return unsupported("辅助变量数量超出降阶规模上限 / Auxiliary variable count exceeds the lowering limit")
            }
            val variable = BinVar("cp-aux-${sanitize(name)}-$auxiliaryVariables")
            val added = linear.add(variable)
            if (added.failed) {
                return propagate(added)
            }
            domains[variable] = ZERO_BI to ONE_BI
            registerArtifact(
                artifactId = "variable:${variable.identifier}:${variable.index}",
                role = "auxiliary-variable",
                originId = sourceOriginId(name)
            )
            return ok(variable)
        }

        private fun registerArtifact(
            artifactId: String,
            role: String,
            originId: String?
        ) {
            var resolvedId = artifactId
            var collision = 1
            while (resolvedId in artifacts) {
                resolvedId = "$artifactId#$collision"
                collision++
            }
            artifacts[resolvedId] = ConstraintProgrammingLoweredArtifact(
                artifactId = resolvedId,
                role = role,
                originId = originId
            )
        }

        private fun sourceOriginId(name: String): String? {
            val constraintMatches = snapshot.constraints.filter { entry ->
                val id = entry.id.value
                val sanitizedId = sanitize(id)
                containsToken(name, id) || containsToken(name, sanitizedId)
            }
            if (constraintMatches.size == 1) {
                return constraintMatches.single().id.value
            }
            val intervalMatches = snapshot.intervals.filter { interval ->
                val id = interval.id.value
                val sanitizedId = sanitize(id)
                containsToken(name, id) || containsToken(name, sanitizedId)
            }
            return intervalMatches.singleOrNull()?.id?.value
        }

        private fun containsToken(value: String, token: String): Boolean {
            if (token.isBlank()) {
                return false
            }
            return value == token ||
                value.startsWith("$token-") ||
                value.endsWith("-$token") ||
                value.contains("-$token-") ||
                value.startsWith("$token:") ||
                value.endsWith(":$token") ||
                value.contains(":$token:") ||
                value.startsWith("${token}_") ||
                value.endsWith("_${token}") ||
                value.contains("_${token}_")
        }

        private fun form(
            terms: Map<AbstractVariableItem<*, *>, BigInteger> = emptyMap(),
            constant: BigInteger = ZERO_BI
        ): LinearForm {
            return LinearForm(
                terms = LinkedHashMap(terms.filterValues { it != ZERO_BI }),
                constant = constant
            )
        }

        private fun form(variable: AbstractVariableItem<*, *>): LinearForm {
            return form(terms = linkedMapOf(variable to ONE_BI))
        }

        private fun form(left: AbstractVariableItem<*, *>, right: AbstractVariableItem<*, *>): LinearForm {
            return form(terms = linkedMapOf(left to ONE_BI, right to ONE_BI))
        }

        private fun combine(left: LinearForm, right: LinearForm, leftScale: BigInteger, rightScale: BigInteger): LinearForm {
            val terms = LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>()
            left.terms.forEach { (variable, coefficient) -> terms[variable] = coefficient * leftScale }
            right.terms.forEach { (variable, coefficient) -> terms[variable] = (terms[variable] ?: ZERO_BI) + coefficient * rightScale }
            return form(terms, left.constant * leftScale + right.constant * rightScale)
        }

        private fun plus(left: LinearForm, right: LinearForm): LinearForm {
            return combine(left, right, ONE_BI, ONE_BI)
        }

        private fun LinearForm.negate(): LinearForm {
            return form(terms.mapValues { -it.value }, -constant)
        }

        private fun LinearForm.plusConstant(value: BigInteger): LinearForm {
            return copy(constant = constant + value)
        }

        private fun addConstraint(
            form: LinearForm,
            comparison: Comparison,
            rhs: BigInteger,
            name: String
        ): Try {
            val polynomial = form.toPolynomial()
            if (polynomial.failed) return propagate(polynomial)
            val right = safeFlt64(rhs, "CP rhs")
            if (right.failed) return propagate(right)
            return try {
                val added = linear.addConstraint(
                    relation = LinearInequality(
                        polynomial.value!!,
                        LinearPolynomial(emptyList(), right.value!!),
                        comparison,
                        name = name
                    ),
                    name = name
                )
                if (added.failed) {
                    return added
                }
                registerArtifact(
                    artifactId = "constraint:$name",
                    role = "compiled-constraint",
                    originId = sourceOriginId(name)
                )
                added
            } catch (error: Throwable) {
                Failed(
                    ErrorCode.OREngineModelingException,
                    "CP 线性约束注册失败：${error.message ?: error::class.simpleName} / " +
                        "CP linear constraint registration failed: ${error.message ?: error::class.simpleName}"
                )
            }
        }

        private fun LinearForm.toPolynomial(): Ret<LinearPolynomial<Flt64>> {
            val monomials = ArrayList<LinearMonomial<Flt64>>()
            for ((variable, coefficient) in terms) {
                val converted = safeFlt64(coefficient, "CP coefficient")
                if (converted.failed) return propagate(converted)
                monomials += LinearMonomial(converted.value!!, variable)
            }
            val convertedConstant = safeFlt64(constant, "CP constant")
            if (convertedConstant.failed) return propagate(convertedConstant)
            return ok(
                LinearPolynomial(
                    monomials = monomials,
                    constant = convertedConstant.value!!
                )
            )
        }

        private fun formBounds(form: LinearForm): Pair<BigInteger, BigInteger>? {
            var lower = form.constant
            var upper = form.constant
            for ((variable, coefficient) in form.terms) {
                val bounds = domains[variable] ?: return null
                if (coefficient.signum() >= 0) {
                    lower += coefficient * bounds.first
                    upper += coefficient * bounds.second
                } else {
                    lower += coefficient * bounds.second
                    upper += coefficient * bounds.first
                }
            }
            return lower to upper
        }

        private fun asVariable(expression: ConstraintProgrammingExpression): AbstractVariableItem<*, *>? {
            return (expression as? ConstraintProgrammingExpression.Variable)?.variableId?.let(variables::get)
        }

        private fun constantValue(expression: ConstraintProgrammingExpression): Int64? {
            return (expression as? ConstraintProgrammingExpression.Constant)?.value
        }

        private fun safeConstant(value: Int64): Ret<BigInteger> {
            return ok(BigInteger.valueOf(value.toLong()))
        }

        private fun comparison(value: ConstraintProgrammingComparison): Comparison {
            return when (value) {
                ConstraintProgrammingComparison.Equal -> Comparison.EQ
                ConstraintProgrammingComparison.LessOrEqual -> Comparison.LE
                ConstraintProgrammingComparison.GreaterOrEqual -> Comparison.GE
            }
        }

        private fun domainBounds(domain: IntegerDomain): Pair<BigInteger, BigInteger> {
            return BigInteger.valueOf(domain.lowerBound.toLong()) to BigInteger.valueOf(domain.upperBound.toLong())
        }

        private fun unsupported(message: String): Ret<Nothing> {
            return Failed(ErrorCode.Other, message)
        }

        private fun sanitize(value: String): String {
            return value.map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }.joinToString("")
        }

        private data class LoweredInterval(
            val start: AbstractVariableItem<*, *>,
            val end: AbstractVariableItem<*, *>,
            val size: LinearForm,
            val presence: LinearForm?
        )

        private data class LinearForm(
            val terms: LinkedHashMap<AbstractVariableItem<*, *>, BigInteger>,
            val constant: BigInteger
        )

        private companion object {
            val ZERO_BI: BigInteger = BigInteger.ZERO
            val ONE_BI: BigInteger = BigInteger.ONE
            val ZERO: BigInteger = BigInteger.ZERO
            val ONE: BigInteger = BigInteger.ONE
            val MAX_EXACT_DOUBLE: BigInteger = BigInteger.valueOf(9_007_199_254_740_991L)
        }

        private fun safeFlt64(value: BigInteger, context: String): Ret<Flt64> {
            if (value.abs() > MAX_EXACT_DOUBLE) {
                return Failed(
                    ErrorCode.Other,
                    "$context 超出 Flt64 精确整数范围 / $context exceeds the exact Flt64 integer range"
                )
            }
            return ok(Flt64(java.lang.Double.valueOf(value.toString())))
        }
    }
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "CP 降阶结果状态无效 / Invalid CP lowering result state")
    }
}
