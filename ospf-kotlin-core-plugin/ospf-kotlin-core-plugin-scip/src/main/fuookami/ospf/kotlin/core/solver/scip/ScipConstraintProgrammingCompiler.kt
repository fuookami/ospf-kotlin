/**
 * CP snapshot 到 JSCIP 的编译器。 / Compiler from a CP snapshot to JSCIP.
 */
package fuookami.ospf.kotlin.core.solver.scip

import java.math.BigInteger
import jscip.Constraint
import jscip.Scip
import jscip.SCIP_Vartype
import jscip.Variable
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.model.constraint_programming.ReificationDirection
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/** 编译后可求解的 SCIP 模型句柄集合。 / Compiled SCIP model handles.
 *
 * @property variables Compiled CP variable handles keyed by stable ID. / 按稳定 ID 索引的已编译 CP 变量句柄。
 * @property intervals Compiled interval handles keyed by stable ID. / 按稳定 ID 索引的已编译 interval 句柄。
 * @property constraints Native constraints grouped by stable constraint ID. / 按稳定约束 ID 分组的原生约束。
 * @property activations Diagnostic activation handles keyed by stable ID. / 按稳定 ID 索引的诊断 activation 句柄。
 * @property artifacts Compiled auxiliary artifacts and their source mapping. / 编译辅助 artifact 及其源映射。
 */
class ScipConstraintProgrammingCompiledModel internal constructor(
    val variables: Map<VariableId, Variable>,
    val intervals: Map<IntervalId, ScipConstraintProgrammingInterval>,
    val constraints: Map<ConstraintId, List<Constraint>>,
    val activations: Map<String, ScipConstraintProgrammingActivation>,
    val artifacts: Map<String, ScipConstraintProgrammingArtifact>,
    internal val allVariables: List<Variable>,
    internal val allConstraints: List<Constraint>
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        allConstraints.asReversed().forEach { constraint ->
            try {
                owner?.releaseCons(constraint)
            } catch (_: Throwable) {
                // Native cleanup is best effort at the adapter boundary. / 原生资源清理在 adapter 边界尽力完成。
            }
        }
        allVariables.asReversed().forEach { variable ->
            try {
                owner?.releaseVar(variable)
            } catch (_: Throwable) {
                // See the comment above. / 同上。
            }
        }
        owner = null
    }

    private var owner: Scip? = null

    internal fun attachOwner(scip: Scip): ScipConstraintProgrammingCompiledModel {
        owner = scip
        return this
    }
}

/** 编译后的 interval 引用。 / Compiled interval reference.
 *
 * @property start Compiled start variable. / 已编译的开始变量。
 * @property size Constant interval size. / interval 的常量长度。
 * @property end Compiled end variable. / 已编译的结束变量。
 */
data class ScipConstraintProgrammingInterval(
    val start: Variable,
    val size: Int,
    val end: Variable
)

/**
 * 编译 artifact 与源模型元素的可回查映射。 / Stable reverse mapping from a compiled artifact to its source element.
 *
 * 辅助元素使用独立的 artifact ID，不会伪装成原始 CP 变量或约束。 /
 * Auxiliary elements use their own artifact IDs and never masquerade as source CP members.
 *
 * @property artifactId 编译 artifact 稳定标识 / Stable compiled-artifact identifier
 * @property role artifact 角色 / Artifact role
 * @property originId 源 CP 元素 ID（无法唯一归属时为空） / Source CP element ID, or null when no unique source exists
 */
data class ScipConstraintProgrammingArtifact(
    val artifactId: String,
    val role: String,
    val originId: String? = null
)

/**
 * 原始模型成员到 SCIP activation 变量的映射。 / Mapping from an original model member to a SCIP activation variable.
 *
 * @property id Stable activation identifier. / 稳定 activation 标识。
 * @property member Original model member represented by the activation. / activation 表示的原始模型成员。
 * @property variable SCIP activation variable. / SCIP activation 变量。
 */
data class ScipConstraintProgrammingActivation(
    val id: String,
    val member: InfeasibilityMember,
    val variable: Variable
)

/**
 * 受约束的 CP 编译器。所有辅助变量和约束都由该对象持有并可统一释放。 / Restricted CP compiler. All auxiliary handles are owned and released together.
 *
 * @property scip Native SCIP instance receiving the compilation. / 接收编译结果的原生 SCIP 实例。
 * @property snapshot Immutable CP model snapshot. / 不可变 CP 模型快照。
 * @property sparseDomainLimit Maximum sparse-domain expansion size. / 稀疏值域展开规模上限。
 * @property decompositionLimit Maximum decomposition size. / 分解规模上限。
 */
class ScipConstraintProgrammingCompiler(
    private val scip: Scip,
    private val snapshot: ConstraintProgrammingModelSnapshot,
    private val sparseDomainLimit: Int = DEFAULT_SPARSE_DOMAIN_LIMIT,
    private val decompositionLimit: Int = DEFAULT_DECOMPOSITION_LIMIT
) {
    private val variables = LinkedHashMap<VariableId, Variable>()
    private val intervals = LinkedHashMap<IntervalId, ScipConstraintProgrammingInterval>()
    private val constraintMap = LinkedHashMap<ConstraintId, List<Constraint>>()
    private val activations = LinkedHashMap<String, ScipConstraintProgrammingActivation>()
    private val artifacts = LinkedHashMap<String, ScipConstraintProgrammingArtifact>()
    private val allVariables = ArrayList<Variable>()
    private val allConstraints = ArrayList<Constraint>()
    private var auxiliaryIndex = 0
    private var activeActivation: Variable? = null

    /** 编译 snapshot。 / Compile the snapshot。
     *
     * @param assumptions Boolean assumptions fixed for this compilation. / 本次编译固定的布尔假设。
     * @param fixedValues Integer values fixed for this compilation. / 本次编译固定的整数值。
     * @param diagnosticMode Whether diagnostic activations are emitted. / 是否生成诊断 activation。
     * @param activeActivationIds Optional activation IDs enabled in diagnostic mode. / 诊断模式下可选启用的 activation ID。
     * @return Compiled SCIP model or a structured compilation error. / 已编译 SCIP 模型或结构化编译错误。
     */
    fun compile(
        assumptions: List<BooleanLiteral> = emptyList(),
        fixedValues: Map<VariableId, Int64> = emptyMap(),
        diagnosticMode: Boolean = false,
        activeActivationIds: Set<String>? = null
    ): Ret<ScipConstraintProgrammingCompiledModel> {
        return try {
            compileVariables(diagnosticMode).flatMapResult {
                compileIntervals().flatMapResult {
                    compileConstraints(diagnosticMode).flatMapResult {
                        compileAssumptions(assumptions).flatMapResult {
                            compileFixedValues(fixedValues).flatMapResult {
                                compileActivationStates(diagnosticMode, activeActivationIds).flatMapResult {
                                    compileObjectives().map {
                                        ScipConstraintProgrammingCompiledModel(
                                            variables = variables.toMap(),
                                            intervals = intervals.toMap(),
                                            constraints = constraintMap.toMap(),
                                            activations = activations.toMap(),
                                            artifacts = artifacts.toMap(),
                                            allVariables = allVariables.toList(),
                                            allConstraints = allConstraints.toList()
                                        ).attachOwner(scip)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            cleanup()
            Failed(
                ErrorCode.OREngineModelingException,
                "SCIP CP 编译失败：${error.message ?: error::class.simpleName} / " +
                    "SCIP CP compilation failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }

    /** 编译本轮固定 assumptions。 / Compile fixed assumptions for this solve. */
    private fun compileAssumptions(assumptions: List<BooleanLiteral>): Ret<Unit> {
        for ((index, literal) in assumptions.withIndex()) {
            val form = literalForm(literal)
            if (form.failed) {
                return propagate(form)
            }
            val constraint = addFormConstraint("cp-assumption-$index", form.value!!, 1.0, 1.0)
            if (constraint.failed) {
                return propagate(constraint)
            }
        }
        return ok(Unit)
    }

    /** 编译本轮必须固定的整数值。 / Compile integer values fixed for this solve. */
    private fun compileFixedValues(fixedValues: Map<VariableId, Int64>): Ret<Unit> {
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
                    "固定值缺少 SCIP 变量：$id / SCIP model misses fixed CP variable: $id"
                )
            val exact = value.toLong().toDouble()
            if (kotlin.math.abs(exact) > MAX_EXACT_DOUBLE_INTEGER) {
                return Failed(
                    ErrorCode.Other,
                    "固定值超出 SCIP double 精度范围：$id=$value / Fixed value exceeds SCIP double precision: $id=$value"
                )
            }
            val result = addLinearConstraint(
                name = "cp-fixed-${id.value.replace(Regex("[^A-Za-z0-9_]+"), "_")}",
                variables = arrayOf(variable),
                coefficients = doubleArrayOf(1.0),
                lowerBound = exact,
                upperBound = exact
            )
            if (result.failed) {
                return propagate(result)
            }
        }
        return ok(Unit)
    }

    /** 失败时释放已创建的 native 句柄。 / Release native handles after an unsuccessful compilation. */
    fun cleanup() {
        allConstraints.asReversed().forEach { constraint ->
            try {
                scip.releaseCons(constraint)
            } catch (_: Throwable) {
            }
        }
        allVariables.asReversed().forEach { variable ->
            try {
                scip.releaseVar(variable)
            } catch (_: Throwable) {
            }
        }
        allConstraints.clear()
        allVariables.clear()
    }

    private fun compileVariables(diagnosticMode: Boolean): Ret<Unit> {
        for (definition in snapshot.variables) {
            val bounds = if (diagnosticMode) {
                diagnosticBaseBounds(definition)
            } else {
                solverBounds(definition.domain)
            }
            if (bounds.failed) {
                return propagate(bounds)
            }
            val type = if (definition.domain == IntegerDomain.boolean) {
                SCIP_Vartype.SCIP_VARTYPE_BINARY
            } else {
                SCIP_Vartype.SCIP_VARTYPE_INTEGER
            }
            val variable = scip.createVar(
                definition.name.ifBlank { definition.id.value },
                bounds.value!!.first,
                bounds.value!!.second,
                0.0,
                type
            )
            variables[definition.id] = variable
            allVariables += variable
            registerArtifact(
                artifactId = "variable:${definition.id.value}",
                role = "source-variable",
                originId = definition.id.value
            )
            if (diagnosticMode) {
                compileDiagnosticBounds(definition, variable).onFailure { return it }
            }
            val sparseDomain = definition.domain as? IntegerDomain.Values
            if (sparseDomain != null && sparseDomain != IntegerDomain.boolean) {
                val values = sparseDomain.values
                if (values.size > sparseDomainLimit) {
                    return Failed(
                        ErrorCode.Other,
                        "稀疏值域超出 SCIP 编译规模上限 / Sparse domain exceeds the SCIP compilation limit"
                    )
                }
                compileSparseDomain(definition.id, variable, values, diagnosticMode).onFailure { return it }
            }
        }
        return ok(Unit)
    }

    private fun compileSparseDomain(
        id: VariableId,
        variable: Variable,
        values: List<Int64>,
        diagnosticMode: Boolean
    ): Ret<Unit> {
        val selectors = values.mapIndexed { index, value ->
            val selector = scip.createVar(
                "cp-domain-${id.value}-$index",
                0.0,
                1.0,
                0.0,
                SCIP_Vartype.SCIP_VARTYPE_BINARY
            )
            allVariables += selector
            selector to value
        }
        val activation = if (diagnosticMode) {
            activationFor("variable:${id.value}:domain", InfeasibilityMember.VariableDomain(
                fuookami.ospf.kotlin.core.solver.report.VariableDomainRef(id)
            ))
        } else {
            null
        }
        val one = withActivation(activation?.variable) {
            addLinearConstraint(
                name = "cp-domain-${id.value}-exactly-one",
                variables = selectors.map { it.first }.toTypedArray(),
                coefficients = DoubleArray(selectors.size) { 1.0 },
                lowerBound = 1.0,
                upperBound = 1.0
            )
        }
        if (one.failed) {
            return propagate(one)
        }
        val vars = ArrayList<Variable>(selectors.size + 1)
        val coefficients = ArrayList<Double>(selectors.size + 1)
        vars += variable
        coefficients += 1.0
        for ((selector, value) in selectors) {
            val converted = safeDouble(value, "CP sparse domain value")
            if (converted.failed) {
                return propagate(converted)
            }
            vars += selector
            coefficients += -converted.value!!
        }
        val link = withActivation(activation?.variable) {
            addLinearConstraint(
                name = "cp-domain-${id.value}-link",
                variables = vars.toTypedArray(),
                coefficients = coefficients.toDoubleArray(),
                lowerBound = 0.0,
                upperBound = 0.0
            )
        }
        if (link.failed) {
            return propagate(link)
        }
        return ok(Unit)
    }

    /**
     * 在诊断模型中为原始上下界创建可关闭的 activation 约束。 / / Create switchable activation constraints for original bounds in diagnostic mode.
     */
    private fun compileDiagnosticBounds(
        definition: fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingVariableSnapshot,
        variable: Variable
    ): Ret<Unit> {
        val base = diagnosticBaseBounds(definition)
        if (base.failed) {
            return propagate(base)
        }
        val original = solverBounds(definition.domain)
        if (original.failed) {
            return propagate(original)
        }
        val (baseLower, baseUpper) = base.value!!
        val (lower, upper) = original.value!!
        if (lower > baseLower) {
            val activation = activationFor(
                id = "variable:${definition.id.value}:lower",
                member = InfeasibilityMember.VariableBound(
                    fuookami.ospf.kotlin.core.solver.report.VariableBoundRef(
                        definition.id,
                        fuookami.ospf.kotlin.core.solver.report.BoundSide.Lower
                    )
                )
            )
            val result = withActivation(activation.variable) {
                addLinearConstraint(
                    name = "cp-bound-${definition.id.value}-lower",
                    variables = arrayOf(variable),
                    coefficients = doubleArrayOf(1.0),
                    lowerBound = lower,
                    upperBound = scip.infinity()
                )
            }
            if (result.failed) {
                return propagate(result)
            }
        }
        if (upper < baseUpper) {
            val activation = activationFor(
                id = "variable:${definition.id.value}:upper",
                member = InfeasibilityMember.VariableBound(
                    fuookami.ospf.kotlin.core.solver.report.VariableBoundRef(
                        definition.id,
                        fuookami.ospf.kotlin.core.solver.report.BoundSide.Upper
                    )
                )
            )
            val result = withActivation(activation.variable) {
                addLinearConstraint(
                    name = "cp-bound-${definition.id.value}-upper",
                    variables = arrayOf(variable),
                    coefficients = doubleArrayOf(1.0),
                    lowerBound = -scip.infinity(),
                    upperBound = upper
                )
            }
            if (result.failed) {
                return propagate(result)
            }
        }
        return ok(Unit)
    }

    /**
     * 计算诊断专用的安全基础值域；所有原始边界都必须包含在其中。 / / Compute a safe diagnostic base domain containing every original bound.
     */
    private fun diagnosticBaseBounds(
        definition: fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingVariableSnapshot
    ): Ret<Pair<Double, Double>> {
        val original = solverBounds(definition.domain)
        if (original.failed) {
            return propagate(original)
        }
        val lower = original.value!!.first
        val upper = original.value!!.second
        return if (lower < -MAX_EXACT_DOUBLE_INTEGER || upper > MAX_EXACT_DOUBLE_INTEGER) {
            Failed(
                ErrorCode.Other,
                "无法为变量构造安全诊断基础值域：${definition.id.value} / " +
                    "Cannot construct a safe diagnostic base domain for variable: ${definition.id.value}"
            )
        } else {
            val bounds = if (definition.domain == IntegerDomain.boolean) {
                0.0 to 1.0
            } else if (lower >= 0.0) {
                0.0 to MAX_EXACT_DOUBLE_INTEGER
            } else {
                -MAX_EXACT_DOUBLE_INTEGER to MAX_EXACT_DOUBLE_INTEGER
            }
            ok(bounds)
        }
    }

    private fun compileIntervals(): Ret<Unit> {
        for (interval in snapshot.intervals) {
            if (interval.optional) {
                return Failed(
                    ErrorCode.Other,
                    "SCIP CP 首版不支持 optional interval / Optional intervals are unsupported in the first SCIP CP compiler"
                )
            }
            val start = asVariable(interval.start)
            val end = asVariable(interval.end)
            val size = constantValue(interval.size)
            if (start == null || end == null || size == null || size < Int64.zero) {
                return Failed(
                    ErrorCode.Other,
                    "SCIP CP 首版只支持固定 duration 的标量 interval / " +
                    "The first SCIP CP compiler only supports scalar fixed-duration intervals"
                )
            }
            val sizeDouble = safeDouble(size, "interval size")
            if (sizeDouble.failed) {
                return propagate(sizeDouble)
            }
            val link = linearForm(interval.end).flatMapResult { endForm ->
                linearForm(interval.start).flatMapResult { startForm ->
                    addFormConstraint(
                        name = "cp-interval-${interval.id.value}-link",
                        form = combine(
                            endForm,
                            startForm,
                            1.0,
                            -1.0
                        ).copy(constant = endForm.constant - startForm.constant - sizeDouble.value!!),
                        lowerBound = 0.0,
                        upperBound = 0.0
                    )
                }
            }
            if (link.failed) {
                return propagate(link)
            }
            val sizeLong = size.toLong()
            if (sizeLong > Int.MAX_VALUE.toLong()) {
                return Failed(
                    ErrorCode.Other,
                    "interval duration 超出 JSCIP Int 范围 / Interval duration exceeds the JSCIP Int range"
                )
            }
            intervals[interval.id] = ScipConstraintProgrammingInterval(start, sizeLong.toInt(), end)
        }
        return ok(Unit)
    }

    private fun compileConstraints(diagnosticMode: Boolean): Ret<Unit> {
        for (entry in snapshot.constraints) {
            val activation = if (diagnosticMode) {
                activationFor(
                    id = "constraint:${entry.id.value}",
                    member = InfeasibilityMember.Constraint(entry.id)
                )
            } else {
                null
            }
            val compiled = withActivation(activation?.variable) {
                compileConstraint(entry.constraint, entry.id.value)
            }
            if (compiled.failed) {
                return propagate(compiled)
            }
            constraintMap[entry.id] = compiled.value!!
        }
        return ok(Unit)
    }

    /** 固定本轮 activation 状态；关闭的成员不会以隐藏基础约束保留。 / Fix activation states for this round. */
    private fun compileActivationStates(
        diagnosticMode: Boolean,
        activeActivationIds: Set<String>?
    ): Ret<Unit> {
        if (!diagnosticMode) {
            return ok(Unit)
        }
        val active = activeActivationIds ?: activations.keys
        val unknown = activeActivationIds?.filterNot { it in activations.keys }.orEmpty()
        if (unknown.isNotEmpty()) {
            return Failed(
                ErrorCode.DataNotFound,
                "SCIP CP conflict activation ID 未知：${unknown.joinToString(",")} / " +
                    "SCIP CP conflict activation IDs are unknown: ${unknown.joinToString(",")}"
            )
        }
        for ((id, activation) in activations) {
            val result = addLinearConstraint(
                name = "cp-activation-state-$id",
                variables = arrayOf(activation.variable),
                coefficients = doubleArrayOf(1.0),
                lowerBound = if (id in active) 1.0 else 0.0,
                upperBound = if (id in active) 1.0 else 0.0
            )
            if (result.failed) {
                return propagate(result)
            }
        }
        return ok(Unit)
    }

    private fun compileConstraint(
        constraint: ConstraintProgrammingConstraint,
        name: String
    ): Ret<List<Constraint>> {
        return when (constraint) {
            is ConstraintProgrammingConstraint.IntegerComparison -> {
                linearForm(constraint.expression).flatMapResult { form ->
                    safeDouble(constraint.rhs, "comparison rhs").flatMapResult { rhs ->
                        val bounds = when (constraint.comparison) {
                            fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison.Equal -> 0.0 to 0.0
                            fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison.LessOrEqual -> -scip.infinity() to 0.0
                            fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison.GreaterOrEqual -> 0.0 to scip.infinity()
                        }
                        addFormConstraint(
                            name = name,
                            form = form,
                            lowerBound = bounds.first + rhs,
                            upperBound = bounds.second + rhs
                        )
                    }
                }
            }

            is ConstraintProgrammingConstraint.Literal -> {
                literalForm(constraint.literal).flatMapResult { form ->
                    addFormConstraint(name, form, 1.0, 1.0)
                }
            }

            is ConstraintProgrammingConstraint.BoolAnd -> {
                val result = ArrayList<Constraint>()
                for ((index, literal) in constraint.literals.withIndex()) {
                    val compiled = literalForm(literal).flatMapResult {
                        addFormConstraint("$name-and-$index", it, 1.0, 1.0)
                    }
                    if (compiled.failed) {
                        return propagate(compiled)
                    }
                    result += compiled.value!!
                }
                ok(result)
            }

            is ConstraintProgrammingConstraint.BoolOr -> {
                val forms = constraint.literals.map { literalForm(it) }
                val failed = forms.firstOrNull { it.failed }
                if (failed != null) {
                    propagate(failed)
                } else {
                    val form = forms.map { it.value!! }.reduceOrNull(::plusForm)
                        ?: LinearForm(emptyMap(), 0.0)
                    addFormConstraint(name, form, 1.0, scip.infinity())
                }
            }

            is ConstraintProgrammingConstraint.BoolXor -> {
                val forms = constraint.literals.map { literalForm(it) }
                val failed = forms.firstOrNull { it.failed }
                if (failed != null) {
                    propagate(failed)
                } else {
                    val form = forms.map { it.value!! }.reduceOrNull(::plusForm)
                        ?: LinearForm(emptyMap(), 0.0)
                    addFormConstraint(name, form, 1.0, 1.0)
                }
            }

            is ConstraintProgrammingConstraint.Implication -> compileImplication(
                constraint.enforcement,
                constraint.constraint,
                name
            )

            is ConstraintProgrammingConstraint.Reified -> compileReified(constraint, name)

            is ConstraintProgrammingConstraint.AllDifferent -> compileAllDifferent(constraint, name)

            is ConstraintProgrammingConstraint.Element -> compileElement(constraint, name)

            is ConstraintProgrammingConstraint.AllowedAssignments -> compileAllowedAssignments(constraint, name)

            is ConstraintProgrammingConstraint.ForbiddenAssignments -> compileForbiddenAssignments(constraint, name)

            is NoOverlap -> compileNoOverlap(constraint, name)

            is Cumulative -> compileCumulative(constraint, name)

            is ConstraintProgrammingConstraint.Circuit -> compileCircuit(constraint, name)

            is ConstraintProgrammingConstraint.Automaton -> compileAutomaton(constraint, name)

            is ConstraintProgrammingConstraint.Reservoir -> compileReservoir(constraint, name)
        }
    }

    private fun unsupportedConstraint(message: String): Ret<List<Constraint>> {
        return Failed(ErrorCode.Other, message)
    }

    /**
     * 将 Circuit 分解为 successor 选择、排列和 MTZ 约束。 / Decompose Circuit into successor selectors, a permutation, and MTZ constraints.
     *
     * 该分解保留从节点 0 出发访问全部节点的语义；所有 Big-M 均由模型值域推导并受精度门禁。 /
     * The decomposition preserves the semantics of visiting every node from node 0; every Big-M is derived from model bounds and precision-gated.
     */
    private fun compileCircuit(
        circuit: ConstraintProgrammingConstraint.Circuit,
        name: String
    ): Ret<List<Constraint>> {
        val size = circuit.successors.size
        if (size == 0) {
            return ok(emptyList())
        }
        if (size.toLong() * size.toLong() > decompositionLimit.toLong() * decompositionLimit.toLong()) {
            return Failed(
                ErrorCode.Other,
                "Circuit 分解规模超限 / Circuit decomposition exceeds the limit"
            )
        }
        val result = ArrayList<Constraint>()
        val selectors = ArrayList<List<Variable>>(size)
        for ((index, successor) in circuit.successors.withIndex()) {
            val expression = linearForm(successor)
            if (expression.failed) {
                return propagate(expression)
            }
            val row = (0 until size).map { target ->
                auxiliaryBinary("$name-successor-$index-$target")
            }
            selectors += row
            val exactlyOne = addLinearConstraint(
                "$name-successor-one-$index",
                row.toTypedArray(),
                DoubleArray(row.size) { 1.0 },
                1.0,
                1.0
            )
            if (exactlyOne.failed) {
                return propagate(exactlyOne)
            }
            result += exactlyOne.value!!
            val encoded = selectorsToForm(row, (0 until size).map { Int64(it.toLong()) })
            if (encoded.failed) {
                return propagate(encoded)
            }
            val linked = addFormConstraint(
                "$name-successor-link-$index",
                combine(expression.value!!, encoded.value!!, 1.0, -1.0),
                0.0,
                0.0
            )
            if (linked.failed) {
                return propagate(linked)
            }
            result += linked.value!!
        }
        for (target in 0 until size) {
            val column = selectors.map { it[target] }
            val exactlyOne = addLinearConstraint(
                "$name-target-one-$target",
                column.toTypedArray(),
                DoubleArray(column.size) { 1.0 },
                1.0,
                1.0
            )
            if (exactlyOne.failed) {
                return propagate(exactlyOne)
            }
            result += exactlyOne.value!!
        }
        if (size == 1) {
            return ok(result)
        }
        for (index in 0 until size) {
            val selfLoop = addLinearConstraint(
                "$name-no-self-$index",
                arrayOf(selectors[index][index]),
                doubleArrayOf(1.0),
                0.0,
                0.0
            )
            if (selfLoop.failed) {
                return propagate(selfLoop)
            }
            result += selfLoop.value!!
        }
        val order = LinkedHashMap<Int, Variable>()
        for (index in 1 until size) {
            val variable = auxiliaryInteger(
                "$name-order-$index",
                1.0,
                (size - 1).toDouble()
            )
            if (variable.failed) {
                return propagate(variable)
            }
            order[index] = variable.value!!
        }
        for (from in 1 until size) {
            for (to in 1 until size) {
                if (from == to) {
                    continue
                }
                val form = addVariable(
                    combine(
                        LinearForm(linkedMapOf(order[from]!! to 1.0), 0.0),
                        LinearForm(linkedMapOf(order[to]!! to 1.0), 0.0),
                        1.0,
                        -1.0
                    ),
                    selectors[from][to],
                    size.toDouble()
                )
                val constraint = addFormConstraint(
                    "$name-mtz-$from-$to",
                    form,
                    -scip.infinity(),
                    (size - 1).toDouble()
                )
                if (constraint.failed) {
                    return propagate(constraint)
                }
                result += constraint.value!!
            }
        }
        return ok(result)
    }

    /** 将确定性自动机展开为每层状态和转移的流平衡。 / Expand a deterministic automaton into layered state and transition flow. */
    private fun compileAutomaton(
        automaton: ConstraintProgrammingConstraint.Automaton,
        name: String
    ): Ret<List<Constraint>> {
        val duplicate = automaton.transitions
            .groupBy { it.fromState to it.value }
            .entries
            .firstOrNull { it.value.size > 1 }
        if (duplicate != null) {
            val (fromState, value) = duplicate.key
            return Failed(
                ErrorCode.IllegalArgument,
                "Automaton 转移不确定：($fromState, $value) 存在重复转移 / " +
                    "Automaton transition is non-deterministic: duplicate key ($fromState, $value)"
            )
        }
        val uniqueTransitions = automaton.transitions
        val states = linkedSetOf<Int>().apply {
            add(automaton.initialState)
            addAll(automaton.finalStates)
            uniqueTransitions.forEach {
                add(it.fromState)
                add(it.toState)
            }
        }.toList()
        val expressionCount = BigInteger.valueOf(automaton.expressions.size.toLong())
        val stateCount = BigInteger.valueOf(states.size.toLong())
        val transitionCount = BigInteger.valueOf(uniqueTransitions.size.toLong())
        val stateLayerCount = expressionCount.add(BigInteger.ONE)
        val auxiliaryVariableCount = stateLayerCount.multiply(stateCount)
            .add(expressionCount.multiply(transitionCount))
        val finalStateCount = automaton.finalStates.count { it in states }
        val finalStateConstraintCount = states.size - finalStateCount
        val constraintCount = stateLayerCount
            .add(BigInteger.valueOf(states.size.toLong()))
            .add(BigInteger.valueOf(finalStateConstraintCount.toLong()))
            .add(BigInteger.ONE)
            .add(expressionCount.multiply(BigInteger.valueOf(2L + 2L * states.size.toLong())))
        val decompositionBudget = BigInteger.valueOf(decompositionLimit.toLong())
            .multiply(BigInteger.valueOf(4L))
        if (states.isEmpty() ||
            auxiliaryVariableCount > decompositionBudget ||
            constraintCount > decompositionBudget
        ) {
            return Failed(ErrorCode.Other, "Automaton 分解规模超限 / Automaton decomposition exceeds the limit")
        }
        val result = ArrayList<Constraint>()
        val stateLayers = (0..automaton.expressions.size).map { layer ->
            states.associateWith { state -> auxiliaryBinary("$name-state-$layer-$state") }
        }
        for ((layer, statesAtLayer) in stateLayers.withIndex()) {
            val one = addLinearConstraint(
                "$name-state-one-$layer",
                statesAtLayer.values.toTypedArray(),
                DoubleArray(statesAtLayer.size) { 1.0 },
                1.0,
                1.0
            )
            if (one.failed) {
                return propagate(one)
            }
            result += one.value!!
        }
        for (state in states) {
            val initial = addLinearConstraint(
                "$name-initial-$state",
                arrayOf(stateLayers.first()[state]!!),
                doubleArrayOf(1.0),
                if (state == automaton.initialState) 1.0 else 0.0,
                if (state == automaton.initialState) 1.0 else 0.0
            )
            if (initial.failed) {
                return propagate(initial)
            }
            result += initial.value!!
            val final = if (state in automaton.finalStates) {
                null
            } else {
                addLinearConstraint(
                    "$name-final-$state",
                    arrayOf(stateLayers.last()[state]!!),
                    doubleArrayOf(1.0),
                    0.0,
                    0.0
                )
            }
            if (final != null) {
                if (final.failed) {
                    return propagate(final)
                }
                result += final.value!!
            }
        }
        if (automaton.finalStates.isEmpty()) {
            val impossible = addFormConstraint(
                "$name-no-final",
                LinearForm(emptyMap(), 0.0),
                1.0,
                1.0
            )
            if (impossible.failed) {
                return propagate(impossible)
            }
            result += impossible.value!!
        } else {
            val finals = automaton.finalStates.mapNotNull { stateLayers.last()[it] }
            val finalOne = addLinearConstraint(
                "$name-final-one",
                finals.toTypedArray(),
                DoubleArray(finals.size) { 1.0 },
                1.0,
                1.0
            )
            if (finalOne.failed) {
                return propagate(finalOne)
            }
            result += finalOne.value!!
        }
        if (automaton.expressions.isEmpty()) {
            return ok(result)
        }
        if (uniqueTransitions.isEmpty()) {
            val impossible = addFormConstraint(
                "$name-no-transition",
                LinearForm(emptyMap(), 0.0),
                1.0,
                1.0
            )
            if (impossible.failed) {
                return propagate(impossible)
            }
            result += impossible.value!!
            return ok(result)
        }
        for ((layer, expression) in automaton.expressions.withIndex()) {
            val transitionVariables = uniqueTransitions.mapIndexed { index, _ ->
                auxiliaryBinary("$name-transition-$layer-$index")
            }
            val one = addLinearConstraint(
                "$name-transition-one-$layer",
                transitionVariables.toTypedArray(),
                DoubleArray(transitionVariables.size) { 1.0 },
                1.0,
                1.0
            )
            if (one.failed) {
                return propagate(one)
            }
            result += one.value!!
            val expressionForm = linearForm(expression)
            if (expressionForm.failed) {
                return propagate(expressionForm)
            }
            val encoded = selectorsToForm(
                transitionVariables,
                uniqueTransitions.map { it.value }
            )
            if (encoded.failed) {
                return propagate(encoded)
            }
            val link = addFormConstraint(
                "$name-expression-$layer",
                combine(expressionForm.value!!, encoded.value!!, 1.0, -1.0),
                0.0,
                0.0
            )
            if (link.failed) {
                return propagate(link)
            }
            result += link.value!!
            for (state in states) {
                val outgoing = uniqueTransitions.mapIndexedNotNull { index, transition ->
                    transitionVariables[index].takeIf { transition.fromState == state }
                }
                val incoming = uniqueTransitions.mapIndexedNotNull { index, transition ->
                    transitionVariables[index].takeIf { transition.toState == state }
                }
                val outgoingForm = LinearForm(
                    outgoing.associateWith { 1.0 },
                    0.0
                )
                val incomingForm = LinearForm(
                    incoming.associateWith { 1.0 },
                    0.0
                )
                val outgoingLink = addFormConstraint(
                    "$name-outgoing-$layer-$state",
                    combine(outgoingForm, LinearForm(linkedMapOf(stateLayers[layer][state]!! to 1.0), 0.0), 1.0, -1.0),
                    0.0,
                    0.0
                )
                val incomingLink = addFormConstraint(
                    "$name-incoming-$layer-$state",
                    combine(incomingForm, LinearForm(linkedMapOf(stateLayers[layer + 1][state]!! to 1.0), 0.0), 1.0, -1.0),
                    0.0,
                    0.0
                )
                if (outgoingLink.failed) return propagate(outgoingLink)
                if (incomingLink.failed) return propagate(incomingLink)
                result += outgoingLink.value!!
                result += incomingLink.value!!
            }
        }
        return ok(result)
    }

    /** 通过整数事件排序与乘积线性化编译 Reservoir。 / Compile Reservoir with integer event ordering and product linearization. */
    private fun compileReservoir(
        reservoir: ConstraintProgrammingConstraint.Reservoir,
        name: String
    ): Ret<List<Constraint>> {
        val events = reservoir.events
        val eventCount = events.size
        if (eventCount.toLong() * eventCount.toLong() > decompositionLimit.toLong() * decompositionLimit.toLong()) {
            return Failed(ErrorCode.Other, "Reservoir 分解规模超限 / Reservoir decomposition exceeds the limit")
        }
        val minimum = safeDouble(reservoir.minimumLevel, "reservoir minimum level")
        val maximum = safeDouble(reservoir.maximumLevel, "reservoir maximum level")
        val initial = safeDouble(reservoir.initialLevel, "reservoir initial level")
        if (minimum.failed) return propagate(minimum)
        if (maximum.failed) return propagate(maximum)
        if (initial.failed) return propagate(initial)
        if (minimum.value!! > maximum.value!! || initial.value!! < minimum.value!! || initial.value!! > maximum.value!!) {
            val impossible = addFormConstraint("$name-invalid-level", LinearForm(emptyMap(), 0.0), 1.0, 1.0)
            if (impossible.failed) return propagate(impossible)
            return ok(impossible.value!!)
        }
        val times = events.map { linearForm(it.time) }
        val changes = events.map { linearForm(it.levelChange) }
        times.firstOrNull { it.failed }?.let { return propagate(it) }
        changes.firstOrNull { it.failed }?.let { return propagate(it) }
        val timeForms = times.map { it.value!! }
        val changeForms = changes.map { it.value!! }
        val result = ArrayList<Constraint>()
        val pairProducts = HashMap<Pair<Int, Int>, ReservoirPairProduct>()
        for (first in 0 until eventCount) {
            for (second in first + 1 until eventCount) {
                val firstTime = formBounds(timeForms[first])
                    ?: return Failed(ErrorCode.Other, "Reservoir 事件时间需要有限值域 / Reservoir event times require finite bounds")
                val secondTime = formBounds(timeForms[second])
                    ?: return Failed(ErrorCode.Other, "Reservoir 事件时间需要有限值域 / Reservoir event times require finite bounds")
                val bigM = maxOf(firstTime.second - secondTime.first, secondTime.second - firstTime.first) + 1.0
                if (!bigM.isFinite() || bigM > MAX_EXACT_DOUBLE_INTEGER) {
                    return Failed(ErrorCode.Other, "Reservoir 排序 Big-M 超出精度范围 / Reservoir ordering Big-M exceeds exact precision")
                }
                val before = auxiliaryBinary("$name-before-$first-$second")
                val firstOrder = addFormConstraint(
                    "$name-order-first-$first-$second",
                    addVariable(combine(timeForms[first], timeForms[second], 1.0, -1.0), before, bigM),
                    -scip.infinity(),
                    bigM
                )
                val secondOrder = addFormConstraint(
                    "$name-order-second-$first-$second",
                    addVariable(combine(timeForms[second], timeForms[first], 1.0, -1.0), before, -bigM),
                    -scip.infinity(),
                    -1.0
                )
                if (firstOrder.failed) return propagate(firstOrder)
                if (secondOrder.failed) return propagate(secondOrder)
                result += firstOrder.value!!
                result += secondOrder.value!!
                val firstProduct = product(changeForms[first], before, "$name-product-first-$first-$second")
                if (firstProduct.failed) return propagate(firstProduct)
                val secondProduct = product(changeForms[second], before, "$name-product-second-$first-$second")
                if (secondProduct.failed) return propagate(secondProduct)
                result += firstProduct.value!!.constraints
                result += secondProduct.value!!.constraints
                pairProducts[first to second] = ReservoirPairProduct(
                    before = before,
                    first = firstProduct.value!!.variable,
                    second = secondProduct.value!!.variable
                )
            }
        }
        for (event in 0 until eventCount) {
            var level = LinearForm(linkedMapOf(), initial.value!!)
            level = plusForm(level, changeForms[event])
            for (other in 0 until eventCount) {
                if (other == event) continue
                if (other < event) {
                    level = addVariable(level, pairProducts[other to event]!!.first, 1.0)
                } else {
                    val pair = pairProducts[event to other]!!
                    level = plusForm(level, changeForms[other])
                    level = addVariable(level, pair.second, -1.0)
                }
            }
            val bound = addFormConstraint(
                "$name-level-$event",
                level,
                minimum.value!!,
                maximum.value!!
            )
            if (bound.failed) return propagate(bound)
            result += bound.value!!
        }
        return ok(result)
    }

    private fun compileImplication(
        enforcement: BooleanLiteral,
        child: ConstraintProgrammingConstraint,
        name: String
    ): Ret<List<Constraint>> {
        if (enforcement.constant == false) {
            return ok(emptyList())
        }
        if (enforcement.constant == true) {
            return compileConstraint(child, name)
        }
        val enforcementVariable = enforcementVariable(enforcement, name)
        if (enforcementVariable.failed) {
            return propagate(enforcementVariable)
        }
        val indicator = enforcementVariable.value!!
        if (child !is ConstraintProgrammingConstraint.IntegerComparison) {
            val nested = compileConstraint(child, "$name-child")
            if (nested.failed || nested.value!!.size != 1) {
                return Failed(
                    ErrorCode.Other,
                    "复杂 indicator 约束必须可编译为单个 SCIP constraint / " +
                        "Complex indicators must compile to one SCIP constraint"
                )
            }
            val wrapped = scip.createConsSuperindicator(name, indicator, nested.value!!.single())
            val registered = registerConstraint(name, wrapped)
            return if (registered.failed) {
                propagate(registered)
            } else {
                ok(listOf(registered.value!!))
            }
        }
        val formResult = linearForm(child.expression)
        if (formResult.failed) {
            return propagate(formResult)
        }
        val form = formResult.value!!
        val rhs = safeDouble(child.rhs, "indicator rhs")
        if (rhs.failed) {
            return propagate(rhs)
        }
        val result = ArrayList<Constraint>()
        when (child.comparison) {
            fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison.LessOrEqual -> {
                val constraint = createIndicator(name, indicator, form, -scip.infinity(), rhs.value!!)
                if (constraint.failed) return propagate(constraint)
                result += constraint.value!!
            }
            fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison.GreaterOrEqual -> {
                val constraint = createIndicator(name, indicator, negateForm(form), -scip.infinity(), -rhs.value!!)
                if (constraint.failed) return propagate(constraint)
                result += constraint.value!!
            }
            fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison.Equal -> {
                val lower = createIndicator("$name-ge", indicator, negateForm(form), -scip.infinity(), -rhs.value!!)
                val upper = createIndicator("$name-le", indicator, form, -scip.infinity(), rhs.value!!)
                if (lower.failed) return propagate(lower)
                if (upper.failed) return propagate(upper)
                result += lower.value!!
                result += upper.value!!
            }
        }
        return ok(result)
    }

    private fun compileReified(
        constraint: ConstraintProgrammingConstraint.Reified,
        name: String
    ): Ret<List<Constraint>> {
        if (constraint.direction == ReificationDirection.Implies) {
            return compileImplication(constraint.literal, constraint.constraint, name)
        }
        return Failed(
            ErrorCode.Other,
            "SCIP CP 首版只支持单向 reification / The first SCIP CP compiler only supports one-way reification"
        )
    }

    private fun compileAllDifferent(
        constraint: ConstraintProgrammingConstraint.AllDifferent,
        name: String
    ): Ret<List<Constraint>> {
        if (constraint.expressions.size > decompositionLimit) {
            return Failed(ErrorCode.Other, "AllDifferent 分解规模超限 / AllDifferent decomposition exceeds the limit")
        }
        val result = ArrayList<Constraint>()
        for (i in constraint.expressions.indices) {
            for (j in i + 1 until constraint.expressions.size) {
                val left = linearForm(constraint.expressions[i])
                val right = linearForm(constraint.expressions[j])
                if (left.failed) return propagate(left)
                if (right.failed) return propagate(right)
                val difference = combine(left.value!!, right.value!!, 1.0, -1.0)
                val bounds = formBounds(difference)
                    ?: return Failed(ErrorCode.Other, "AllDifferent 需要有限表达式界 / AllDifferent requires finite expression bounds")
                val bigM = maxOf(kotlin.math.abs(bounds.first), kotlin.math.abs(bounds.second)) + 1.0
                if (!bigM.isFinite() || bigM > MAX_EXACT_DOUBLE_INTEGER) {
                    return Failed(
                        ErrorCode.Other,
                        "AllDifferent Big-M 超出 SCIP 精确整数范围 / AllDifferent Big-M exceeds SCIP's exact integer range"
                    )
                }
                val order = auxiliaryBinary("$name-order-$i-$j")
                val upperForm = difference.copy(
                    variables = difference.variables + (order to bigM)
                )
                val lowerForm = negateForm(difference).copy(
                    variables = negateForm(difference).variables + (order to -bigM)
                )
                val upper = addFormConstraint(
                    "$name-upper-$i-$j",
                    upperForm,
                    -scip.infinity(),
                    bigM - 1.0
                )
                val lower = addFormConstraint(
                    "$name-lower-$i-$j",
                    lowerForm,
                    -scip.infinity(),
                    -1.0
                )
                if (upper.failed) return propagate(upper)
                if (lower.failed) return propagate(lower)
                result += upper.value!!
                result += lower.value!!
            }
        }
        return ok(result)
    }

    private fun compileElement(
        constraint: ConstraintProgrammingConstraint.Element,
        name: String
    ): Ret<List<Constraint>> {
        val constants = constraint.values.map { value ->
            when (value) {
                is Int64 -> value
                is Long -> Int64(value)
                is Int -> Int64(value.toLong())
                else -> return Failed(ErrorCode.Other, "Element 首版只支持常量数组 / First Element compiler supports constants only")
            }
        }
        if (constants.size > decompositionLimit) {
            return Failed(ErrorCode.Other, "Element 分解规模超限 / Element decomposition exceeds the limit")
        }
        val selectors = constants.indices.map { index -> auxiliaryBinary("$name-select-$index") }
        val result = ArrayList<Constraint>()
        val one = addLinearConstraint("$name-one", selectors.toTypedArray(), DoubleArray(selectors.size) { 1.0 }, 1.0, 1.0)
        if (one.failed) return propagate(one)
        result += one.value!!
        val indexForm = linearForm(constraint.index)
        val targetForm = linearForm(constraint.target)
        if (indexForm.failed) return propagate(indexForm)
        if (targetForm.failed) return propagate(targetForm)
        val indexSelectors = selectorsToForm(selectors, constants.indices.map { Int64(it.toLong()) })
        if (indexSelectors.failed) return propagate(indexSelectors)
        val targetSelectors = selectorsToForm(selectors, constants)
        if (targetSelectors.failed) return propagate(targetSelectors)
        val indexLink = combine(indexForm.value!!, indexSelectors.value!!, 1.0, -1.0)
        val targetLink = combine(targetForm.value!!, targetSelectors.value!!, 1.0, -1.0)
        val indexConstraint = addFormConstraint("$name-index", indexLink, 0.0, 0.0)
        val targetConstraint = addFormConstraint("$name-target", targetLink, 0.0, 0.0)
        if (indexConstraint.failed) return propagate(indexConstraint)
        if (targetConstraint.failed) return propagate(targetConstraint)
        result += indexConstraint.value!!
        result += targetConstraint.value!!
        return ok(result)
    }

    private fun compileAllowedAssignments(
        constraint: ConstraintProgrammingConstraint.AllowedAssignments,
        name: String
    ): Ret<List<Constraint>> {
        if (constraint.tuples.size > decompositionLimit) {
            return Failed(ErrorCode.Other, "Allowed table 分解规模超限 / Allowed table decomposition exceeds the limit")
        }
        val selectors = constraint.tuples.indices.map { auxiliaryBinary("$name-tuple-$it") }
        val result = ArrayList<Constraint>()
        val one = addLinearConstraint("$name-one", selectors.toTypedArray(), DoubleArray(selectors.size) { 1.0 }, 1.0, 1.0)
        if (one.failed) return propagate(one)
        result += one.value!!
        for (column in constraint.expressions.indices) {
            val expression = linearForm(constraint.expressions[column])
            if (expression.failed) return propagate(expression)
            val values = selectorsToForm(selectors, constraint.tuples.map { it[column] })
            if (values.failed) return propagate(values)
            val link = addFormConstraint(
                "$name-link-$column",
                combine(expression.value!!, values.value!!, 1.0, -1.0),
                0.0,
                0.0
            )
            if (link.failed) return propagate(link)
            result += link.value!!
        }
        return ok(result)
    }

    private fun compileForbiddenAssignments(
        constraint: ConstraintProgrammingConstraint.ForbiddenAssignments,
        name: String
    ): Ret<List<Constraint>> {
        if (constraint.tuples.size > decompositionLimit) {
            return Failed(
                ErrorCode.Other,
                "Forbidden table 分解规模超限 / Forbidden table decomposition exceeds the limit"
            )
        }
        val result = ArrayList<Constraint>()
        for ((row, tuple) in constraint.tuples.withIndex()) {
            val differences = ArrayList<Variable>()
            for (column in constraint.expressions.indices) {
                val expression = linearForm(constraint.expressions[column])
                if (expression.failed) {
                    return propagate(expression)
                }
                val bounds = formBounds(expression.value!!)
                    ?: return Failed(
                        ErrorCode.Other,
                        "Forbidden table 需要有限表达式界 / Forbidden table requires finite expression bounds"
                    )
                val value = safeDouble(tuple[column], "forbidden table tuple value")
                if (value.failed) {
                    return propagate(value)
                }
                val left = auxiliaryBinary("$name-$row-$column-left")
                val right = auxiliaryBinary("$name-$row-$column-right")

                val leftBigM = maxOf(1.0, bounds.second - (value.value!! - 1.0))
                if (!leftBigM.isFinite() || leftBigM > MAX_EXACT_DOUBLE_INTEGER) {
                    return Failed(
                        ErrorCode.Other,
                        "Forbidden table Big-M 超出 SCIP 精确整数范围 / Forbidden table Big-M exceeds SCIP's exact integer range"
                    )
                }
                val leftForm = expression.value!!.copy(
                    variables = expression.value!!.variables + (left to leftBigM)
                )
                val leftConstraint = addFormConstraint(
                    "$name-$row-$column-left-link",
                    leftForm,
                    -scip.infinity(),
                    value.value!! - 1.0 + leftBigM
                )
                if (leftConstraint.failed) {
                    return propagate(leftConstraint)
                }
                result += leftConstraint.value!!

                val rightBigM = maxOf(1.0, value.value!! + 1.0 - bounds.first)
                if (!rightBigM.isFinite() || rightBigM > MAX_EXACT_DOUBLE_INTEGER) {
                    return Failed(
                        ErrorCode.Other,
                        "Forbidden table Big-M 超出 SCIP 精确整数范围 / Forbidden table Big-M exceeds SCIP's exact integer range"
                    )
                }
                val rightForm = expression.value!!.copy(
                    variables = expression.value!!.variables + (right to -rightBigM)
                )
                val rightConstraint = addFormConstraint(
                    "$name-$row-$column-right-link",
                    rightForm,
                    value.value!! + 1.0 - rightBigM,
                    scip.infinity()
                )
                if (rightConstraint.failed) {
                    return propagate(rightConstraint)
                }
                result += rightConstraint.value!!
                differences += left
                differences += right
            }
            val differenceConstraint = addLinearConstraint(
                "$name-$row-difference",
                differences.toTypedArray(),
                DoubleArray(differences.size) { 1.0 },
                1.0,
                scip.infinity()
            )
            if (differenceConstraint.failed) {
                return propagate(differenceConstraint)
            }
            result += differenceConstraint.value!!
        }
        return ok(result)
    }

    private fun compileNoOverlap(noOverlap: NoOverlap, name: String): Ret<List<Constraint>> {
        val result = ArrayList<Constraint>()
        for (i in noOverlap.intervals.indices) {
            for (j in i + 1 until noOverlap.intervals.size) {
                val first = intervals[noOverlap.intervals[i].id]
                    ?: return Failed(ErrorCode.IllegalArgument, "未编译 interval / Interval was not compiled")
                val second = intervals[noOverlap.intervals[j].id]
                    ?: return Failed(ErrorCode.IllegalArgument, "未编译 interval / Interval was not compiled")
                val order = auxiliaryBinary("$name-order-$i-$j")
                val firstStartBounds = formBounds(LinearForm(linkedMapOf(first.start to 1.0), 0.0))
                    ?: return Failed(ErrorCode.Other, "NoOverlap 需要有限 start 值域 / NoOverlap requires finite start bounds")
                val secondStartBounds = formBounds(LinearForm(linkedMapOf(second.start to 1.0), 0.0))
                    ?: return Failed(ErrorCode.Other, "NoOverlap 需要有限 start 值域 / NoOverlap requires finite start bounds")
                val bigM = maxOf(firstStartBounds.second - secondStartBounds.first + first.size, secondStartBounds.second - firstStartBounds.first + second.size) + 1.0
                if (!bigM.isFinite() || bigM > MAX_EXACT_DOUBLE_INTEGER) {
                    return Failed(
                        ErrorCode.Other,
                        "NoOverlap Big-M 超出 SCIP 精确整数范围 / NoOverlap Big-M exceeds SCIP's exact integer range"
                    )
                }
                val firstBefore = addLinearConstraint(
                    "$name-first-before-$i-$j",
                    arrayOf(first.start, second.start, order),
                    doubleArrayOf(1.0, -1.0, bigM),
                    -scip.infinity(),
                    bigM - first.size
                )
                if (firstBefore.failed) return propagate(firstBefore)
                val secondBefore = addLinearConstraint(
                    "$name-second-before-$i-$j",
                    arrayOf(second.start, first.start, order),
                    doubleArrayOf(1.0, -1.0, -bigM),
                    -scip.infinity(),
                    -second.size.toDouble()
                )
                if (secondBefore.failed) return propagate(secondBefore)
                result += firstBefore.value!!
                result += secondBefore.value!!
            }
        }
        return ok(result)
    }

    private fun compileCumulative(cumulative: Cumulative, name: String): Ret<List<Constraint>> {
        val compiled = cumulative.intervals.map { intervals[it.id] }
        if (compiled.any { it == null }) {
            return Failed(ErrorCode.IllegalArgument, "Cumulative 引用了未注册 interval / Cumulative references an unknown interval")
        }
        val nonNullCompiled = compiled.filterNotNull()
        val starts = nonNullCompiled.map { it.start }
        val durations = nonNullCompiled.map { it.size }.toIntArray()
        val demandValues = cumulative.demands.map { constantValue(it)?.toLong() }
        val capacityValue = constantValue(cumulative.capacity)?.toLong()
        if (demandValues.any { it == null } ||
            capacityValue == null ||
            capacityValue < 0L ||
            demandValues.any { it!! < 0L } ||
            demandValues.any { it!! > Int.MAX_VALUE.toLong() } ||
            capacityValue > Int.MAX_VALUE.toLong()
        ) {
            return Failed(
                ErrorCode.Other,
                "SCIP native cumulative 需要固定非负 duration、demand 和 capacity / " +
                    "SCIP native cumulative requires fixed non-negative duration, demand, and capacity"
            )
        }
        val demands = demandValues.map { it!!.toInt() }.toIntArray()
        val capacity = capacityValue.toInt()
        return try {
            val constraint = scip.createConsCumulative(
                name,
                starts.toTypedArray(),
                durations,
                demands,
                capacity
            )
            registerConstraint(name, constraint).map { listOf(it) }
        } catch (error: Throwable) {
            Failed(ErrorCode.OREngineModelingException, "SCIP cumulative 编译失败 / SCIP cumulative compilation failed: ${error.message}")
        }
    }

    private fun compileObjectives(): Ret<Unit> {
        if (!snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "SCIP CP 只支持与模型方向一致的单一目标 / SCIP CP supports at most one objective matching the model category"
            )
        }
        val objective = snapshot.objectives.firstOrNull() ?: return ok(Unit)
        val form = linearForm(objective.expression)
        if (form.failed) return propagate(form)
        form.value!!.variables.forEach { (variable, coefficient) ->
            scip.changeVarObj(variable, coefficient)
        }
        if (form.value!!.constant != 0.0) {
            val objectiveConstantName = "cp-objective-constant-${auxiliaryIndex++}"
            val constant = scip.createVar(
                objectiveConstantName,
                1.0,
                1.0,
                form.value!!.constant,
                SCIP_Vartype.SCIP_VARTYPE_INTEGER
            )
            allVariables += constant
            registerArtifact(
                artifactId = "variable:$objectiveConstantName",
                role = "objective-constant",
                originId = objective.id.value
            )
        }
        when (objective.category) {
            fuookami.ospf.kotlin.core.model.basic.ObjectCategory.Minimum -> scip.setMinimize()
            fuookami.ospf.kotlin.core.model.basic.ObjectCategory.Maximum -> scip.setMaximize()
        }
        return ok(Unit)
    }

    private fun createIndicator(
        name: String,
        indicator: Variable,
        form: LinearForm,
        lowerBound: Double,
        upperBound: Double
    ): Ret<Constraint> {
        if (lowerBound != -scip.infinity()) {
            return Failed(ErrorCode.Other, "indicator lower bound must be converted to upper-bound form")
        }
        return try {
            val constraint = scip.createConsIndicator(
                name,
                indicator,
                form.variables.keys.toTypedArray(),
                form.variables.values.toDoubleArray(),
                upperBound - form.constant
            )
            registerConstraint(name, constraint)
        } catch (error: Throwable) {
            Failed(ErrorCode.OREngineModelingException, "SCIP indicator 编译失败 / SCIP indicator compilation failed: ${error.message}")
        }
    }

    private fun enforcementVariable(literal: BooleanLiteral, name: String): Ret<Variable> {
        if (literal.constant != null) {
            return Failed(ErrorCode.IllegalArgument, "布尔常量不应请求 solver variable / Boolean constant has no solver variable")
        }
        val variable = literal.variableId?.let(variables::get)
            ?: return Failed(ErrorCode.IllegalArgument, "assumption 变量未注册 / Assumption variable is not registered")
        if (!literal.negated) {
            return ok(variable)
        }
        val negated = auxiliaryBinary("$name-negated")
        val link = addLinearConstraint(
            "$name-negated-link",
            arrayOf(variable, negated),
            doubleArrayOf(1.0, 1.0),
            1.0,
            1.0
        )
        return if (link.failed) propagate(link) else ok(negated)
    }

    private fun auxiliaryBinary(name: String): Variable {
        val index = auxiliaryIndex++
        val variableName = "cp-aux-$index-$name"
        val variable = scip.createVar(
            variableName,
            0.0,
            1.0,
            0.0,
            SCIP_Vartype.SCIP_VARTYPE_BINARY
        )
        allVariables += variable
        registerArtifact(
            artifactId = "variable:$variableName",
            role = if (name.startsWith("activation-")) "activation-variable" else "auxiliary-variable",
            originId = sourceOriginId(name)
        )
        return variable
    }

    private fun auxiliaryInteger(
        name: String,
        lowerBound: Double,
        upperBound: Double
    ): Ret<Variable> {
        if (!lowerBound.isFinite() || !upperBound.isFinite() || lowerBound > upperBound ||
            lowerBound < -MAX_EXACT_DOUBLE_INTEGER || upperBound > MAX_EXACT_DOUBLE_INTEGER
        ) {
            return Failed(
                ErrorCode.Other,
                "SCIP CP 辅助整数值域超出精度范围 / SCIP CP auxiliary integer domain exceeds exact precision"
            )
        }
        return try {
            val index = auxiliaryIndex++
            val variableName = "cp-aux-$index-$name"
            val variable = scip.createVar(
                variableName,
                lowerBound,
                upperBound,
                0.0,
                SCIP_Vartype.SCIP_VARTYPE_INTEGER
            )
            allVariables += variable
            registerArtifact(
                artifactId = "variable:$variableName",
                role = "auxiliary-variable",
                originId = sourceOriginId(name)
            )
            ok(variable)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineModelingException,
                "SCIP CP 辅助整数变量创建失败：${error.message} / " +
                    "SCIP CP auxiliary integer creation failed: ${error.message}"
            )
        }
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
        artifacts[resolvedId] = ScipConstraintProgrammingArtifact(
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

    private fun sanitize(value: String): String {
        return value.map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }.joinToString("")
    }

    private fun addVariable(
        form: LinearForm,
        variable: Variable,
        coefficient: Double
    ): LinearForm {
        if (coefficient == 0.0) {
            return form
        }
        val variables = LinkedHashMap(form.variables)
        variables[variable] = (variables[variable] ?: 0.0) + coefficient
        return LinearForm(variables.filterValues { it != 0.0 }, form.constant)
    }

    /** 线性化 y = binary * expression；表达式上下界必须有限。 / Linearize y = binary * expression; finite expression bounds are required. */
    private fun product(
        expression: LinearForm,
        binary: Variable,
        name: String
    ): Ret<ProductResult> {
        val bounds = formBounds(expression)
            ?: return Failed(
                ErrorCode.Other,
                "Reservoir 事件变化需要有限值域 / Reservoir event changes require finite bounds"
            )
        val lower = minOf(0.0, bounds.first)
        val upper = maxOf(0.0, bounds.second)
        if (!lower.isFinite() || !upper.isFinite() ||
            lower < -MAX_EXACT_DOUBLE_INTEGER || upper > MAX_EXACT_DOUBLE_INTEGER
        ) {
            return Failed(
                ErrorCode.Other,
                "Reservoir 事件变化超出 SCIP 精度范围 / Reservoir event changes exceed SCIP exact precision"
            )
        }
        val variable = auxiliaryInteger(name, lower, upper)
        if (variable.failed) {
            return propagate(variable)
        }
        val productVariable = variable.value!!
        val constraints = ArrayList<Constraint>()
        val lowerBound = addVariable(
            addVariable(LinearForm(linkedMapOf(), 0.0), productVariable, 1.0),
            binary,
            -bounds.first
        )
        val lowerConstraint = addFormConstraint(
            "$name-lower-active",
            lowerBound,
            0.0,
            scip.infinity()
        )
        if (lowerConstraint.failed) return propagate(lowerConstraint)
        constraints += lowerConstraint.value!!
        val upperBound = addVariable(
            addVariable(LinearForm(linkedMapOf(), 0.0), productVariable, 1.0),
            binary,
            -bounds.second
        )
        val upperConstraint = addFormConstraint(
            "$name-upper-active",
            upperBound,
            -scip.infinity(),
            0.0
        )
        if (upperConstraint.failed) return propagate(upperConstraint)
        constraints += upperConstraint.value!!
        val lowerInactive = addVariable(
            addVariable(combine(
                LinearForm(linkedMapOf(), 0.0),
                expression,
                1.0,
                -1.0
            ), productVariable, 1.0),
            binary,
            -bounds.second
        )
        val lowerInactiveConstraint = addFormConstraint(
            "$name-lower-inactive",
            lowerInactive,
            -bounds.second,
            scip.infinity()
        )
        if (lowerInactiveConstraint.failed) return propagate(lowerInactiveConstraint)
        constraints += lowerInactiveConstraint.value!!
        val upperInactive = addVariable(
            addVariable(combine(
                LinearForm(linkedMapOf(), 0.0),
                expression,
                1.0,
                -1.0
            ), productVariable, 1.0),
            binary,
            -bounds.first
        )
        val upperInactiveConstraint = addFormConstraint(
            "$name-upper-inactive",
            upperInactive,
            -scip.infinity(),
            -bounds.first
        )
        if (upperInactiveConstraint.failed) return propagate(upperInactiveConstraint)
        constraints += upperInactiveConstraint.value!!
        return ok(ProductResult(productVariable, constraints))
    }

    private fun addFormConstraint(
        name: String,
        form: LinearForm,
        lowerBound: Double,
        upperBound: Double
    ): Ret<List<Constraint>> {
        return addLinearConstraint(
            name,
            form.variables.keys.toTypedArray(),
            form.variables.values.toDoubleArray(),
            lowerBound - form.constant,
            upperBound - form.constant
        ).map { listOf(it) }
    }

    private fun addLinearConstraint(
        name: String,
        variables: Array<Variable>,
        coefficients: DoubleArray,
        lowerBound: Double,
        upperBound: Double
    ): Ret<Constraint> {
        return try {
            val constraint = scip.createConsLinear(name, variables, coefficients, lowerBound, upperBound)
            registerArtifact(
                artifactId = "constraint:$name",
                role = "compiled-constraint",
                originId = sourceOriginId(name)
            )
            registerConstraint(name, constraint)
        } catch (error: Throwable) {
            Failed(ErrorCode.OREngineModelingException, "SCIP linear constraint 编译失败 / SCIP linear constraint compilation failed: ${error.message}")
        }
    }

    /** 将约束注册为普通约束或 activation superindicator。 / Register a plain constraint or an activation superindicator. */
    private fun registerConstraint(name: String, constraint: Constraint): Ret<Constraint> {
        return try {
            val activation = activeActivation
            if (activation == null) {
                scip.addCons(constraint)
                allConstraints += constraint
                ok(constraint)
            } else {
                val wrapped = scip.createConsSuperindicator(name, activation, constraint)
                scip.addCons(wrapped)
                allConstraints += wrapped
                try {
                    scip.releaseCons(constraint)
                } catch (_: Throwable) {
                    // The wrapper owns the child constraint at the native boundary.
                    // superindicator 在原生边界持有子约束引用。
                }
                ok(wrapped)
            }
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineModelingException,
                "SCIP activation 约束注册失败：${error.message} / " +
                    "SCIP activation constraint registration failed: ${error.message}"
            )
        }
    }

    /** 临时设置当前 activation；嵌套编译结束后恢复外层状态。 / Set a scoped activation for nested compilation. */
    private inline fun <T> withActivation(
        activation: Variable?,
        block: () -> Ret<T>
    ): Ret<T> {
        val previous = activeActivation
        activeActivation = activation
        return try {
            block()
        } finally {
            activeActivation = previous
        }
    }

    /** 创建或复用稳定 activation 变量。 / Create or reuse a stable activation variable. */
    private fun activationFor(
        id: String,
        member: InfeasibilityMember
    ): ScipConstraintProgrammingActivation {
        return activations[id] ?: ScipConstraintProgrammingActivation(
            id = id,
            member = member,
            variable = auxiliaryBinary("activation-$id")
        ).also { activations[id] = it }
    }

    private fun linearForm(expression: ConstraintProgrammingExpression): Ret<LinearForm> {
        return when (expression) {
            is ConstraintProgrammingExpression.Constant -> safeDouble(expression.value, "CP constant")
                .map { LinearForm(emptyMap(), it) }
            is ConstraintProgrammingExpression.Invalid -> Failed(ErrorCode.IllegalArgument, expression.message)
            is ConstraintProgrammingExpression.Variable -> {
                val variable = variables[expression.variableId]
                    ?: return Failed(ErrorCode.IllegalArgument, "CP 表达式变量未注册 / CP expression variable is not registered")
                ok(LinearForm(linkedMapOf(variable to 1.0), 0.0))
            }
            is ConstraintProgrammingExpression.Linear -> {
                val terms = LinkedHashMap<Variable, Double>()
                for (term in expression.terms) {
                    val variable = variables[term.variableId]
                        ?: return Failed(ErrorCode.IllegalArgument, "CP 线性项变量未注册 / CP linear term variable is not registered")
                    val coefficient = safeDouble(term.coefficient, "CP coefficient")
                    if (coefficient.failed) {
                        return propagate(coefficient)
                    }
                    terms[variable] = (terms[variable] ?: 0.0) + coefficient.value!!
                }
                safeDouble(expression.constant, "CP linear constant")
                    .map { constant -> LinearForm(terms, constant) }
            }
        }
    }

    private fun literalForm(literal: BooleanLiteral): Ret<LinearForm> {
        val constant = literal.constant
        if (constant != null) {
            return ok(LinearForm(emptyMap(), if (constant) 1.0 else 0.0))
        }
        val variable = literal.variableId?.let(variables::get)
            ?: return Failed(ErrorCode.IllegalArgument, "布尔文字变量未注册 / Boolean literal variable is not registered")
        return if (literal.negated) {
            ok(LinearForm(linkedMapOf(variable to -1.0), 1.0))
        } else {
            ok(LinearForm(linkedMapOf(variable to 1.0), 0.0))
        }
    }

    private fun asVariable(expression: ConstraintProgrammingExpression): Variable? {
        return (expression as? ConstraintProgrammingExpression.Variable)?.variableId?.let(variables::get)
    }

    private fun constantValue(expression: ConstraintProgrammingExpression): Int64? {
        return (expression as? ConstraintProgrammingExpression.Constant)?.value
    }

    private fun selectorsToForm(selectors: List<Variable>, values: List<Int64>): Ret<LinearForm> {
        val variables = LinkedHashMap<Variable, Double>()
        for ((index, variable) in selectors.withIndex()) {
            val value = safeDouble(values[index], "selector value")
            if (value.failed) {
                return propagate(value)
            }
            variables[variable] = value.value!!
        }
        return ok(LinearForm(variables, 0.0))
    }

    private fun formBounds(form: LinearForm): Pair<Double, Double>? {
        var lower = form.constant
        var upper = form.constant
        for ((variable, coefficient) in form.variables) {
            val definition = snapshot.variables.firstOrNull { variables[it.id] == variable } ?: return null
            val bounds = solverBounds(definition.domain)
            if (bounds.failed) return null
            if (coefficient >= 0.0) {
                lower += coefficient * bounds.value!!.first
                upper += coefficient * bounds.value!!.second
            } else {
                lower += coefficient * bounds.value!!.second
                upper += coefficient * bounds.value!!.first
            }
        }
        return lower to upper
    }

    private fun combine(left: LinearForm, right: LinearForm, leftScale: Double, rightScale: Double): LinearForm {
        val result = LinkedHashMap<Variable, Double>()
        left.variables.forEach { (variable, coefficient) -> result[variable] = coefficient * leftScale }
        right.variables.forEach { (variable, coefficient) -> result[variable] = (result[variable] ?: 0.0) + coefficient * rightScale }
        return LinearForm(result.filterValues { it != 0.0 }, left.constant * leftScale + right.constant * rightScale)
    }

    private fun plusForm(left: LinearForm, right: LinearForm): LinearForm {
        return combine(left, right, 1.0, 1.0)
    }

    private fun negateForm(form: LinearForm): LinearForm {
        return LinearForm(form.variables.mapValues { -it.value }, -form.constant)
    }

    private fun solverBounds(domain: IntegerDomain): Ret<Pair<Double, Double>> {
        return safeDouble(domain.lowerBound, "CP domain lower bound").flatMapResult { lower ->
            safeDouble(domain.upperBound, "CP domain upper bound").map { upper ->
                lower to upper
            }
        }
    }

    private fun safeDouble(value: Int64, context: String): Ret<Double> {
        val long = value.toLong()
        if (kotlin.math.abs(long.toDouble()) > MAX_EXACT_DOUBLE_INTEGER) {
            return Failed(
                ErrorCode.IllegalArgument,
                "$context 超出 SCIP double 精确整数范围 / $context exceeds SCIP's exact integer range"
            )
        }
        return ok(long.toDouble())
    }

    private data class LinearForm(
        val variables: Map<Variable, Double>,
        val constant: Double
    )

    private data class ProductResult(
        val variable: Variable,
        val constraints: List<Constraint>
    )

    private data class ReservoirPairProduct(
        val before: Variable,
        val first: Variable,
        val second: Variable
    )

    private companion object {
        const val DEFAULT_SPARSE_DOMAIN_LIMIT = 128
        const val DEFAULT_DECOMPOSITION_LIMIT = 256
        const val MAX_EXACT_DOUBLE_INTEGER = 9_007_199_254_740_991.0
    }
}

private inline fun <T> Ret<T>.onFailure(block: (Ret<T>) -> Nothing): Ret<T> {
    if (failed) {
        block(this)
    }
    return this
}

private inline fun <T, U> Ret<T>.flatMapResult(transform: (T) -> Ret<U>): Ret<U> {
    return when (this) {
        is fuookami.ospf.kotlin.utils.functional.Ok -> transform(value)
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "SCIP CP 编译结果状态无效 / Invalid SCIP CP compiler result state")
    }
}
