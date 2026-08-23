/** Logic-Based Benders contracts and engine. / Logic-Based Benders 契约与迭代引擎。 */
package fuookami.ospf.kotlin.framework.solver

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.TimeSource
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveIssue
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.BinVariable

/** Engine proof mode. / 引擎证明模式。 */
enum class BendersProofMode {
    /** Every accepted terminal certificate must be verified. / 所有接受的终态证书都必须已验证。 */
    Exact,

    /** Feasible incumbents may be used without a global optimality proof. / 允许使用未完成全局最优证明的可行解。 */
    Heuristic
}

/** Master-cut category. / 主问题割类别。 */
enum class BendersCutKind {
    Feasibility,
    Conflict,
    NoGood,
    Optimality,
    Heuristic
}

/** Cut validity scope. / 割的有效性范围。 */
enum class BendersCutValidity {
    /** Valid for the complete master feasible region. / 对整个主问题可行域全局有效。 */
    Global,

    /** Valid only at the current assignment. / 仅对当前赋值有效。 */
    Assignment,

    /** Heuristic and not suitable for exact mode. / 启发式有效性，不能用于精确模式。 */
    Heuristic
}

/** Stable value source used by variable bindings. / 变量绑定使用的稳定值源。 */
interface ConstraintProgrammingValueSource {
    /** Read a value by a stable domain key. / 按稳定领域键读取值。
     *
     * @param key Stable domain key. / 稳定领域键。
     * @return Value or a missing-value error. / 变量值或缺失值错误。
     */
    fun value(key: String): Ret<Flt64>

    /** Return all values for diagnostics and trace output. / 返回全部值以供诊断和轨迹使用。
     *
     * @return Snapshot of all known values. / 全部已知值的快照。
     */
    fun values(): Map<String, Flt64>

    /** Read a value by an OSPF variable identity. / 按 OSPF 变量身份读取值。
     *
     * @param variable OSPF variable whose stable key is used. / 使用其稳定键的 OSPF 变量。
     * @return Value or a missing-value error. / 变量值或缺失值错误。
     */
    fun value(variable: AbstractVariableItem<*, *>): Ret<Flt64> {
        return value(variable.bendersStableKey())
    }

    companion object {
        /** Create a map-backed source. / 创建基于映射的值源。
         *
         * @param values Values keyed by stable domain key. / 按稳定领域键索引的变量值。
         * @return Map-backed value source. / 基于映射的值源。
         */
        fun of(values: Map<String, Flt64>): ConstraintProgrammingValueSource {
            return MapConstraintProgrammingValueSource(values)
        }
    }
}

/** Map-backed value source. / 基于映射的值源。
 *
 * @property assignment Values keyed by stable domain key. / 按稳定领域键索引的变量值。
 */
data class MapConstraintProgrammingValueSource(
    private val assignment: Map<String, Flt64>
) : ConstraintProgrammingValueSource {
    override fun value(key: String): Ret<Flt64> {
        return assignment[key]?.let(::ok) ?: Failed(
            ErrorCode.DataNotFound,
            "缺少主问题变量值：$key / Missing master variable value: $key"
        )
    }

    override fun values(): Map<String, Flt64> {
        return assignment.toMap()
    }
}

/** Build a source from a legacy solver vector and optional variable list. / 从旧求解器向量和可选变量列表创建值源。
 *
 * @param output Feasible master output containing the solver vector. / 包含求解器向量的主问题可行输出。
 * @param variables Optional master variables to receive stable keys. / 用于补充稳定键的可选主问题变量列表。
 * @return Value source backed by the solver vector. / 由求解器向量支持的值源。
 */
fun masterSolutionValueSource(
    output: SolveReport<Flt64>,
    variables: List<AbstractVariableItem<*, *>> = emptyList()
): ConstraintProgrammingValueSource {
    val values = LinkedHashMap<String, Flt64>()
    output.values.forEachIndexed { index, value ->
        values["index:$index"] = value
        values[index.toString()] = value
    }
    variables.forEach { variable ->
        output.values.getOrNull(variable.index)?.let { value ->
            values[variable.bendersStableKey()] = value
        }
    }
    return MapConstraintProgrammingValueSource(values)
}

/** One bound master value and its CP projection. / 一个主问题值及其 CP 投影。
 *
 * @property key Stable master binding key. / 稳定主问题绑定键。
 * @property variable Master variable. / 主问题变量。
 * @property value Master value. / 主问题值。
 */
data class BendersMasterAssignment(
    val key: String,
    val variable: AbstractVariableItem<*, *>,
    val value: Flt64
)

/** Master assignment fixed for one CP subproblem solve. / 一轮 CP 子问题固定的主问题赋值。
 *
 * @property masterValues Master values keyed by binding key. / 按绑定键索引的主问题值。
 * @property masterVariables Master variables keyed by binding key. / 按绑定键索引的主问题变量。
 * @property fixedValues CP values fixed for this solve. / 本次求解固定的 CP 变量值。
 * @property assumptions Boolean assumptions passed to the CP solver. / 传给 CP 求解器的布尔假设。
 * @property assumptionToMaster Reverse mapping from CP variable ID to master key. / 从 CP 变量 ID 到主问题键的反向映射。
 */
data class BendersSubproblemAssignment(
    val masterValues: Map<String, Flt64>,
    val masterVariables: Map<String, AbstractVariableItem<*, *>>,
    val fixedValues: Map<VariableId, Int64>,
    val assumptions: List<BooleanLiteral>,
    val assumptionToMaster: Map<String, String> = emptyMap()
) {
    /** Compatibility alias for callers using the plan terminology. / 计划术语兼容别名。 */
    val masterAssignment: Map<String, Flt64>
        get() = masterValues

    /** Compatibility alias for CP fixed values. / CP 固定值兼容别名。 */
    val cpValues: Map<VariableId, Int64>
        get() = fixedValues

    /** Reverse assumption mapping keyed by CP variable ID. / 按 CP 变量 ID 索引的反向映射。 */
    val assumptionOrigins: Map<String, String>
        get() = assumptionToMaster
}

/** Explicit master-to-CP binding contract. / 显式主问题到 CP 绑定契约。 */
interface BendersVariableBinding {
    /** Bind one master solution to one static CP model. / 将一轮主问题解绑定到静态 CP 模型。
     *
     * @param masterSolution Master values to bind. / 待绑定的主问题值。
     * @param subproblem Static CP model receiving the binding. / 接收绑定的静态 CP 模型。
     * @return Binding assignment or a structured error. / 绑定赋值或结构化错误。
     */
    fun bind(
        masterSolution: ConstraintProgrammingValueSource,
        subproblem: ConstraintProgrammingModel
    ): Ret<BendersSubproblemAssignment>
}

/**
 * A binary master variable mapped to a binary CP variable. / 主问题二值变量到 CP 二值变量的映射。
 *
 * @property key Stable binding key. / 稳定绑定键。
 * @property masterVariable Binary master variable. / 二值主问题变量。
 * @property subproblemVariable Binary CP variable. / 二值 CP 变量。
 * @property subproblemVariableId Explicit stable CP variable ID; when absent the legacy model-local ID is used. /
 * 显式稳定 CP 变量 ID；为空时使用旧的模型局部 ID。
 */
data class BinaryBendersVariable(
    val key: String,
    val masterVariable: AbstractVariableItem<*, *>,
    val subproblemVariable: BinVariable,
    val subproblemVariableId: String? = null
)

/** A bounded integer master variable mapped to an integer CP variable. / 有界整数主问题变量到 CP 整数变量的映射。
 *
 * @property key Stable binding key. / 稳定绑定键。
 * @property masterVariable Integer master variable. / 整数主问题变量。
 * @property subproblemVariable Integer CP variable. / 整数 CP 变量。
 * @property domain Master assignment domain. / 主问题赋值值域。
 * @property subproblemVariableId Explicit stable CP variable ID; when absent the legacy model-local ID is used. /
 * 显式稳定 CP 变量 ID；为空时使用旧的模型局部 ID。
 */
data class IntegerBendersVariable(
    val key: String,
    val masterVariable: AbstractVariableItem<*, *>,
    val subproblemVariable: AbstractVariableItem<*, *>,
    val domain: IntegerDomain,
    val subproblemVariableId: String? = null
)

/** Default binding for binary master assignments. / 二值主问题赋值的默认绑定。
 *
 * @property variables Binary master-to-CP bindings. / 二值主问题到 CP 的绑定集合。
 */
class BinaryBendersVariableBinding(
    private val variables: List<BinaryBendersVariable>
) : BendersVariableBinding {
    override fun bind(
        masterSolution: ConstraintProgrammingValueSource,
        subproblem: ConstraintProgrammingModel
    ): Ret<BendersSubproblemAssignment> {
        val snapshot = subproblem.snapshot()
        if (snapshot.failed) {
            return propagate(snapshot)
        }
        val current = snapshot.value!!
        val masterValues = LinkedHashMap<String, Flt64>()
        val masterVariables = LinkedHashMap<String, AbstractVariableItem<*, *>>()
        val fixedValues = LinkedHashMap<VariableId, Int64>()
        val assumptions = ArrayList<BooleanLiteral>()
        val assumptionToMaster = LinkedHashMap<String, String>()

        for (binding in variables) {
            if (!binding.masterVariable.type.isBinaryType || !binding.subproblemVariable.type.isBinaryType) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders 绑定要求主问题和 CP 变量均为二值：${binding.key} / " +
                        "Benders binding requires binary master and CP variables: ${binding.key}"
                )
            }
            val value = masterSolution.value(binding.key)
            if (value.failed) {
                return propagate(value)
            }
            val raw = value.value!!
            if (raw != Flt64.zero && raw != Flt64.one) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders 主问题变量必须为 0 或 1：${binding.key}=$raw / " +
                        "Benders master variable must be 0 or 1: ${binding.key}=$raw"
                )
            }
            val cpId = binding.cpVariableId()
            if (cpId.value.isBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders CP 变量稳定 ID 不能为空：${binding.key} / Benders CP variable stable ID must not be blank: ${binding.key}"
                )
            }
            if (current.variable(cpId) == null) {
                return Failed(
                    ErrorCode.DataNotFound,
                    "CP 子问题缺少绑定变量：$cpId / CP subproblem is missing bound variable: $cpId"
                )
            }
            val integer = if (raw == Flt64.one) Int64.one else Int64.zero
            val assumption = BooleanLiteral(
                variable = binding.subproblemVariable,
                negated = integer == Int64.zero,
                id = cpId
            )
            masterValues[binding.key] = raw
            masterVariables[binding.key] = binding.masterVariable
            fixedValues[cpId] = integer
            assumptions += assumption
            assumptionToMaster[cpId.value] = binding.key
        }

        return ok(
            BendersSubproblemAssignment(
                masterValues = masterValues,
                masterVariables = masterVariables,
                fixedValues = fixedValues,
                assumptions = assumptions,
                assumptionToMaster = assumptionToMaster
            )
        )
    }
}

/** Binding for bounded integer master assignments. / 有界整数主问题赋值绑定。
 *
 * @property variables Bounded integer master-to-CP bindings. / 有界整数主问题到 CP 的绑定集合。
 */
class IntegerBendersVariableBinding(
    private val variables: List<IntegerBendersVariable>
) : BendersVariableBinding {
    override fun bind(
        masterSolution: ConstraintProgrammingValueSource,
        subproblem: ConstraintProgrammingModel
    ): Ret<BendersSubproblemAssignment> {
        val snapshot = subproblem.snapshot()
        if (snapshot.failed) {
            return propagate(snapshot)
        }
        val current = snapshot.value!!
        val masterValues = LinkedHashMap<String, Flt64>()
        val masterVariables = LinkedHashMap<String, AbstractVariableItem<*, *>>()
        val fixedValues = LinkedHashMap<VariableId, Int64>()

        for (binding in variables) {
            if (!binding.masterVariable.type.isIntegerType || !binding.subproblemVariable.type.isIntegerType) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders 绑定要求主问题和 CP 变量均为整数：${binding.key} / " +
                        "Benders binding requires integer master and CP variables: ${binding.key}"
                )
            }
            val rawResult = masterSolution.value(binding.key)
            if (rawResult.failed) {
                return propagate(rawResult)
            }
            val raw = rawResult.value!!
            if (!raw.toDouble().isFinite() || raw != raw.round()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders 主问题变量必须为整数：${binding.key}=$raw / " +
                        "Benders master variable must have an integral value: ${binding.key}=$raw"
                )
            }
            val integer = raw.toInt64()
            if (!binding.domain.contains(integer)) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "Benders 主问题赋值超出声明值域：${binding.key}=$integer / " +
                        "Benders master assignment is outside the declared domain: ${binding.key}=$integer"
                )
            }
            val cpId = binding.cpVariableId()
            if (cpId.value.isBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders CP 变量稳定 ID 不能为空：${binding.key} / Benders CP variable stable ID must not be blank: ${binding.key}"
                )
            }
            val cpDefinition = current.variable(cpId)
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "CP 子问题缺少绑定变量：$cpId / CP subproblem is missing bound variable: $cpId"
                )
            if (!cpDefinition.domain.contains(integer)) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "绑定值超出 CP 子问题值域：$cpId=$integer / " +
                        "Binding value is outside the CP subproblem domain: $cpId=$integer"
                )
            }
            masterValues[binding.key] = raw
            masterVariables[binding.key] = binding.masterVariable
            fixedValues[cpId] = integer
        }

        return ok(
            BendersSubproblemAssignment(
                masterValues = masterValues,
                masterVariables = masterVariables,
                fixedValues = fixedValues,
                assumptions = emptyList(),
                assumptionToMaster = emptyMap()
            )
        )
    }
}

/** Subproblem result contract. / 子问题结果契约。
 *
 * @property assignment Master assignment used for the subproblem solve. / 子问题求解使用的主问题赋值。
 */
sealed interface LogicBasedBendersSubproblemResult {
    val assignment: BendersSubproblemAssignment
}

/** Proven or accepted feasible CP result. / 已证明或已接受的 CP 可行结果。
 *
 * @property assignment Master assignment used for the solve. / 本次求解使用的主问题赋值。
 * @property output Feasible CP solver output. / CP 求解器可行输出。
 */
data class FeasibleSubproblemResult(
    override val assignment: BendersSubproblemAssignment,
    val output: ConstraintProgrammingFeasibleOutput
) : LogicBasedBendersSubproblemResult

/** Proven CP infeasibility result. / 已证明 CP 不可行结果。
 *
 * @property assignment Master assignment used for the solve. / 本次求解使用的主问题赋值。
 * @property conflict Optional infeasibility conflict evidence. / 可选的不可行冲突证据。
 * @property proofStatus Status of the infeasibility proof. / 不可行证明状态。
 */
data class InfeasibleSubproblemResult(
    override val assignment: BendersSubproblemAssignment,
    val conflict: ConstraintProgrammingConflict?,
    val proofStatus: ProofStatus = ProofStatus.Verified
) : LogicBasedBendersSubproblemResult

/** Unknown, limited, or cancelled CP result. / 未知、受限或取消的 CP 结果。
 *
 * @property assignment Master assignment used for the solve. / 本次求解使用的主问题赋值。
 * @property terminationReason Reason the CP solve stopped. / CP 求解停止原因。
 */
data class UnknownSubproblemResult(
    override val assignment: BendersSubproblemAssignment,
    val terminationReason: TerminationReason
) : LogicBasedBendersSubproblemResult

/** Context supplied to cut oracles. / 提供给割预言器的上下文。
 *
 * @property master Mutable master model. / 可变主问题模型。
 * @property subproblem Static CP subproblem. / 静态 CP 子问题。
 * @property assignment Current master-to-CP assignment. / 当前主问题到 CP 的赋值。
 * @property iteration Current Benders iteration. / 当前 Benders 迭代编号。
 * @property proofMode Active proof mode. / 当前证明模式。
 */
data class BendersCutContext(
    val master: LinearMetaModel<Flt64>,
    val subproblem: ConstraintProgrammingModel,
    val assignment: BendersSubproblemAssignment,
    val iteration: Int,
    val proofMode: BendersProofMode
)

/** Master cut contract. / 主问题割契约。
 *
 * @property inequality Primary linear inequality. / 主线性不等式。
 * @property kind Cut category. / 割类别。
 * @property validity Validity scope. / 有效性范围。
 * @property proofStatus Evidence status supporting the cut. / 支持该割的证据状态。
 * @property source Stable source identifier. / 稳定来源标识。
 * @property name Optional model name. / 可选模型名称。
 * @property additionalInequalities Additional inequalities in the same cut. / 同一割中的附加不等式。
 * @property auxiliaryVariables Auxiliary variables required by the cut. / 该割所需的辅助变量。
 */
data class BendersMasterCut(
    val inequality: LinearInequality<Flt64>,
    val kind: BendersCutKind,
    val validity: BendersCutValidity,
    val proofStatus: ProofStatus,
    val source: String,
    val name: String? = null,
    val additionalInequalities: List<LinearInequality<Flt64>> = emptyList(),
    val auxiliaryVariables: List<BinVar> = emptyList()
) {
    /** Stable deduplication key. / 稳定去重键。 */
    val key: String
        get() = buildString {
            append(source)
            append('|')
            append(kind)
            append('|')
            append(inequality)
            additionalInequalities.forEach {
                append('|')
                append(it)
            }
            auxiliaryVariables.forEach {
                append('|')
                append(it.identifier)
                append(':')
                append(it.index)
            }
        }
}

/** Domain cut oracle. / 领域割预言器。 */
interface BendersCutOracle {
    /** Generate feasibility/conflict cuts. / 生成可行性或冲突割。
     *
     * @param result Proven infeasible subproblem result. / 已证明不可行的子问题结果。
     * @param context Current master, subproblem, and iteration context. / 当前主问题、子问题和迭代上下文。
     * @return Candidate cuts or a generation error. / 候选割或生成错误。
     */
    fun feasibilityCuts(
        result: InfeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>>

    /** Generate optional optimality cuts. / 生成可选最优性割。
     *
     * @param result Feasible subproblem result. / 子问题可行结果。
     * @param context Current master, subproblem, and iteration context. / 当前主问题、子问题和迭代上下文。
     * @return Candidate cuts or a generation error. / 候选割或生成错误。
     */
    fun optimalityCuts(
        result: FeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>>
}

/** Default binary no-good/conflict cut oracle. / 默认二值 no-good/conflict 割预言器。 */
class BinaryNoGoodCutOracle : BendersCutOracle {
    override fun feasibilityCuts(
        result: InfeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>> {
        val candidateKeys = linkedSetOf<String>()
        val conflict = result.conflict
        // A shrunk core is only safe to project when its final revalidation was
        // verified.  Otherwise fall back to the complete assignment: the
        // proven infeasibility of that fixed assignment still yields a valid
        // global binary no-good cut.
        val useVerifiedCore = conflict?.validity == fuookami.ospf.kotlin.core.solver.report.EvidenceValidity.Verified
        if (conflict != null && useVerifiedCore) {
            conflict.assumptions.forEach { assumption ->
                assumption.variableId?.value?.let { id ->
                    result.assignment.assumptionToMaster[id]?.let(candidateKeys::add)
                }
            }
            conflict.variableIds.forEach { id ->
                result.assignment.assumptionToMaster[id.value]?.let(candidateKeys::add)
            }
        }
        if (candidateKeys.isEmpty()) {
            candidateKeys += result.assignment.masterValues.keys
        }
        if (candidateKeys.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "无法为无变量赋值生成 no-good cut / Cannot generate a no-good cut without an assignment"
            )
        }
        return noGoodCut(result.assignment, candidateKeys, conflict != null && useVerifiedCore)
    }

    override fun optimalityCuts(
        result: FeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>> {
        // A CP point objective is not globally valid without a domain oracle. / CP 点目标没有领域证明时不具备全局有效性。
        return ok(emptyList())
    }

    private fun noGoodCut(
        assignment: BendersSubproblemAssignment,
        keys: Set<String>,
        conflict: Boolean
    ): Ret<List<BendersMasterCut>> {
        val terms = ArrayList<LinearMonomial<Flt64>>()
        var constant = Flt64.zero
        for (key in keys) {
            val variable = assignment.masterVariables[key]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "no-good cut 缺少主问题变量：$key / No-good cut is missing master variable: $key"
                )
            val value = assignment.masterValues[key]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "no-good cut 缺少主问题赋值：$key / No-good cut is missing master assignment: $key"
                )
            if (value != Flt64.zero && value != Flt64.one) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "no-good cut 只支持二值赋值：$key=$value / No-good cut only supports binary assignments: $key=$value"
                )
            }
            if (value == Flt64.one) {
                terms += LinearMonomial(-Flt64.one, variable)
                constant += Flt64.one
            } else {
                terms += LinearMonomial(Flt64.one, variable)
            }
        }
        val lhs = LinearPolynomial(terms, constant)
        val rhs = LinearPolynomial<Flt64>(emptyList(), Flt64.one)
        return ok(
            listOf(
                BendersMasterCut(
                    inequality = LinearInequality(lhs, rhs, Comparison.GE),
                    kind = if (conflict) BendersCutKind.Conflict else BendersCutKind.NoGood,
                    validity = BendersCutValidity.Global,
                    proofStatus = ProofStatus.Verified,
                    source = if (conflict) "binary-conflict-core" else "binary-no-good"
                )
            )
        )
    }
}

/** Exact no-good oracle for bounded integer master assignments. / 有界整数主问题的精确 no-good 割预言器。
 *
 * @property variables Integer master-to-CP bindings used to build the encoding. / 用于构造编码的主问题到 CP 整数绑定。
 */
class IntegerNoGoodCutOracle(
    private val variables: List<IntegerBendersVariable>
) : BendersCutOracle {
    override fun feasibilityCuts(
        result: InfeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>> {
        if (variables.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "整数 no-good 绑定不能为空 / Integer no-good binding must not be empty"
            )
        }
        val items = ArrayList<IntegerNoGoodVariable>(variables.size)
        for (binding in variables) {
            val value = result.assignment.masterValues[binding.key]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "整数 no-good 缺少主问题赋值：${binding.key} / Integer no-good is missing master assignment: ${binding.key}"
                )
            if (value != value.round() || !value.toDouble().isFinite()) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "整数 no-good 收到非整数赋值：${binding.key}=$value / " +
                        "Integer no-good received a non-integral assignment: ${binding.key}=$value"
                )
            }
            items += IntegerNoGoodVariable(
                key = binding.key,
                variable = binding.masterVariable,
                value = value.toInt64(),
                domain = binding.domain
            )
        }
        val encoding = IntegerNoGoodCutEncoder.encode(
            variables = items,
            name = "integer-no-good-${context.iteration}"
        )
        if (encoding.failed) {
            return propagate(encoding)
        }
        val value = encoding.value!!
        val constraints = value.constraints
        if (constraints.isEmpty()) {
            return Failed(
                ErrorCode.ApplicationError,
                "整数 no-good 编码未生成约束 / Integer no-good encoding produced no constraints"
            )
        }
        return ok(
            listOf(
                BendersMasterCut(
                    inequality = constraints.first(),
                    additionalInequalities = constraints.drop(1),
                    auxiliaryVariables = value.auxiliaryVariables,
                    kind = BendersCutKind.NoGood,
                    validity = BendersCutValidity.Global,
                    proofStatus = ProofStatus.Verified,
                    source = "integer-no-good"
                )
            )
        )
    }

    override fun optimalityCuts(
        result: FeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>> {
        return ok(emptyList())
    }
}

/** Iteration-level Benders trace. / Benders 迭代轨迹。
 *
 * @property iteration Iteration number. / 迭代编号。
 * @property masterObjective Master incumbent objective. / 主问题当前目标值。
 * @property masterBestBound Master solver best bound. / 主问题求解器最佳界。
 * @property masterStatus Master solver status. / 主问题求解状态。
 * @property subproblemStatus CP subproblem status. / CP 子问题状态。
 * @property subproblemObjective CP objective at the current assignment. / 当前赋值下的 CP 目标值。
 * @property cutCount Number of newly accepted cuts. / 新接受的割数量。
 * @property conflictCoreSize Size of the conflict core. / 冲突核心大小。
 * @property elapsed Elapsed engine time. / 引擎耗时。
 * @property proofMode Proof mode used for the iteration. / 本次迭代使用的证明模式。
 * @property convergenceGap Master incumbent/bound gap. / 主问题 incumbent 与最佳界间隙。
 */
data class BendersIterationTrace(
    val iteration: Int,
    val masterObjective: Flt64? = null,
    val masterBestBound: Flt64? = null,
    val masterStatus: SolverStatus? = null,
    val subproblemStatus: BendersSubproblemStatus,
    val subproblemObjective: Flt64? = null,
    val cutCount: Int = 0,
    val conflictCoreSize: Int = 0,
    val elapsed: Duration = ZERO,
    val proofMode: BendersProofMode,
    val convergenceGap: Flt64? = null
)

/**
 * Evaluates the complete Benders incumbent objective for an assignment. /
 * 计算给定赋值下完整 Benders incumbent 目标值。
 *
 * The returned value must use the same objective sense and constant convention as the master output. /
 * 返回值必须与主问题输出使用相同的优化方向和常数项口径。
 *
 * @param masterOutput Current master feasible output. / 当前主问题可行输出。
 * @param assignment Master-to-CP assignment. / 主问题到 CP 的赋值。
 * @param subproblemOutput Feasible CP output. / CP 可行输出。
 * @return Complete incumbent objective or an evaluation error. / 完整当前解目标值或求值错误。
 */
typealias BendersCompleteObjectiveEvaluator = (
    masterOutput: SolveReport<Flt64>,
    assignment: BendersSubproblemAssignment,
    subproblemOutput: ConstraintProgrammingFeasibleOutput
) -> Ret<Flt64>

/** Evidence returned after applying persisted master state. / 应用持久化主问题状态后返回的复验证据。
 *
 * @property masterFingerprint Fingerprint of the master actually used by the adapter. /
 * 适配器实际使用的主问题指纹。
 * @property incumbent Applied master incumbent. / 已应用的主问题 incumbent。
 * @property bestBound Applied master bound. / 已应用的主问题最佳界。
 * @property assumptions Applied assumption identifiers. / 已应用的 assumption 标识。
 * @property fixedBindings Applied fixed bindings. / 已应用的固定绑定。
 * @property conflicts Applied conflict evidence. / 已应用的冲突证据。
 * @property convergenceVerified Whether the convergence marker was applied. / 是否应用了收敛标记。
 */
data class BendersMasterResumeEvidence(
    val masterFingerprint: String,
    val incumbent: String? = null,
    val bestBound: String? = null,
    val assumptions: List<String> = emptyList(),
    val fixedBindings: Map<String, Long> = emptyMap(),
    val conflicts: List<fuookami.ospf.kotlin.core.solver.constraint_programming.PortableConstraintProgrammingConflict> = emptyList(),
    val convergenceVerified: Boolean = false
)

/** Applies persisted Benders master state before the first resumed solve. / 在首次恢复求解前应用持久化主问题状态。
 *
 * @param master Mutable master model / 可变主问题模型
 * @param state Validated portable resume state / 已复验的可移植恢复状态
 * @return Applied state evidence or a structured error. / 已应用状态证据或结构化错误。
 */
typealias BendersMasterResumeApplier = (
    master: LinearMetaModel<Flt64>,
    state: BendersResumeState
) -> Ret<BendersMasterResumeEvidence>

/** CP subproblem trace status. / CP 子问题轨迹状态。 */
enum class BendersSubproblemStatus {
    Feasible,
    Infeasible,
    Unknown
}

/** Engine options. / 引擎选项。
 *
 * @property proofMode Proof mode controlling accepted certificates. / 控制可接受证书的证明模式。
 * @property maxIterations Maximum Benders iterations. / Benders 最大迭代次数。
 * @property stallIterationLimit Maximum consecutive stalled iterations. / 连续停滞迭代上限。
 * @property optimalityTolerance Absolute objective and convergence tolerance. / 目标和收敛绝对容差。
 * @property completeObjectiveEvaluator Evaluates the complete Benders incumbent objective. / 计算完整 Benders 当前解目标值的函数。
 * @property masterVariables Optional variables used for stable value lookup. / 用于稳定值查找的可选主问题变量。
 * @property masterSolutionSource Optional custom master-output value source. / 可选的自定义主问题输出值源。
 * @property constraintProgrammingOptions CP solver options. / CP 求解器选项。
 * @property cancellationToken Optional cancellation token. / 可选取消令牌。
 * @property progressReporter Optional iteration progress callback. / 可选迭代进度回调。
 * @property resumeState Previously validated portable state to seed the master. /
 * 用于初始化主问题的已校验可移植状态。
 * @property resumeStateApplier Applies persisted master incumbent/bound and binding state. /
 * 应用持久化的主问题 incumbent/bound 及绑定状态。
 */
data class LogicBasedBendersOptions(
    val proofMode: BendersProofMode = BendersProofMode.Exact,
    val maxIterations: Int = 100,
    val stallIterationLimit: Int = 1,
    /**
     * Absolute objective and master incumbent/bound gaps accepted by Exact mode. The master objective
     * must represent the complete Benders objective at the current assignment. / Exact 模式接受的
     * 子问题目标与主问题目标差值、以及主问题 incumbent/bound 绝对间隙；主问题目标必须表示当前赋值下的完整 Benders 目标。
     */
    val optimalityTolerance: Flt64 = Flt64.zero,
    /**
     * Evaluates first-stage cost plus the subproblem contribution. Required by Exact mode for an
     * optimizing CP subproblem because the master objective may contain theta and other terms. /
     * 计算第一阶段成本与子问题贡献之和。Exact 模式下带目标 CP 子问题必须提供该契约，
     * 因为主问题目标可能包含 theta 和其他项。
     */
    val completeObjectiveEvaluator: BendersCompleteObjectiveEvaluator? = null,
    val masterVariables: List<AbstractVariableItem<*, *>> = emptyList(),
    val masterSolutionSource: ((SolveReport<Flt64>) -> Ret<ConstraintProgrammingValueSource>)? = null,
    val constraintProgrammingOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions(),
    val cancellationToken: CancellationToken? = null,
    val progressReporter: ((BendersIterationTrace) -> Try)? = null,
    val resumeState: BendersResumeState? = null,
    val resumeStateApplier: BendersMasterResumeApplier? = null
)

/** Final Logic-Based Benders report. / Logic-Based Benders 最终报告。
 *
 * @property problemStatus Final problem status. / 最终问题状态。
 * @property terminationReason Final termination reason. / 最终终止原因。
 * @property proof Final proof certificate. / 最终证明证书。
 * @property masterOutput Last master solver output. / 主问题求解器最后输出。
 * @property assignment Last master-to-CP assignment. / 最后的主问题到 CP 赋值。
 * @property subproblemResult Last CP subproblem result. / CP 子问题最后结果。
 * @property cuts All accepted Benders cuts. / 所有已接受的 Benders 割。
 * @property iterations Iteration traces. / 迭代轨迹。
 * @property diagnostics Structured diagnostics. / 结构化诊断信息。
 */
data class LogicBasedBendersReport(
    val problemStatus: ProblemStatus,
    val terminationReason: TerminationReason,
    val proof: SolveProof,
    val masterOutput: SolveReport<Flt64>? = null,
    val assignment: BendersSubproblemAssignment? = null,
    val subproblemResult: LogicBasedBendersSubproblemResult? = null,
    val cuts: List<BendersMasterCut> = emptyList(),
    val iterations: List<BendersIterationTrace> = emptyList(),
    val diagnostics: List<SolveIssue> = emptyList()
) {
    /** Compatibility alias. / 兼容别名。 */
    val status: ProblemStatus
        get() = problemStatus

    /** Convert to the core report contract. / 转换为 core 统一报告契约。
     *
     * @return Core solve report. / Core 统一求解报告。
     */
    fun toSolveReport(): SolveReport<Flt64> {
        val output = masterOutput
        return SolveReport(
            problemStatus = problemStatus,
            terminationReason = terminationReason,
            solutionPresence = when {
                output == null -> SolutionPresence.None
                proof.status == ProofStatus.Verified && problemStatus == ProblemStatus.Feasible -> SolutionPresence.Optimal
                else -> SolutionPresence.Incumbent
            },
            solution = output?.let {
                SolveSolution(
                    values = it.values,
                    objective = it.solution?.objective
                )
            },
            proof = proof,
            statistics = fuookami.ospf.kotlin.core.solver.report.SolveStatistics(
                solveTime = output?.solveTime,
                iterations = iterations.size.toULong()
            ),
            diagnostics = SolveDiagnostics(errors = diagnostics)
        )
    }
}

/** Master-solve adapter used by the engine. / 引擎使用的主问题求解适配器。 */
fun interface BendersMasterProblemSolver {
    /** Solve the current mutable master model. / 求解当前可变主问题模型。
     *
     * @param master Current mutable master model. / 当前可变主问题模型。
     * @return Feasible master output or a solver error. / 主问题可行输出或求解错误。
     */
    suspend fun solve(master: LinearMetaModel<Flt64>): Ret<SolveReport<Flt64>>
}

/** Logic-Based Benders engine. / Logic-Based Benders 迭代引擎。
 *
 * @property masterSolver Adapter that solves the mutable master model. / 求解可变主问题模型的适配器。
 * @property subproblemSolver CP subproblem solver. / CP 子问题求解器。
 * @property binding Master-to-CP variable binding. / 主问题到 CP 的变量绑定。
 * @property cutOracle Domain cut generator. / 领域割生成器。
 * @property options Engine options. / 引擎选项。
 */
class LogicBasedBendersEngine(
    private val masterSolver: BendersMasterProblemSolver,
    private val subproblemSolver: ConstraintProgrammingSolver,
    private val binding: BendersVariableBinding,
    private val cutOracle: BendersCutOracle = BinaryNoGoodCutOracle(),
    private val options: LogicBasedBendersOptions = LogicBasedBendersOptions()
) {
    /**
     * Adapt the existing framework Benders solver contract. /
     * 适配现有 framework Benders 求解器契约。
     *
     * @param masterSolver Existing framework master solver. / 现有 framework 主问题求解器。
     * @param subproblemSolver CP subproblem solver. / CP 子问题求解器。
     * @param binding Master-to-CP variable binding. / 主问题到 CP 的变量绑定。
     * @param cutOracle Domain cut generator. / 领域割生成器。
     * @param options Engine options. / 引擎选项。
     */
    constructor(
        masterSolver: LinearBendersDecompositionSolver,
        subproblemSolver: ConstraintProgrammingSolver,
        binding: BendersVariableBinding,
        cutOracle: BendersCutOracle = BinaryNoGoodCutOracle(),
        options: LogicBasedBendersOptions = LogicBasedBendersOptions()
    ) : this(
        masterSolver = BendersMasterProblemSolver { master ->
            when (val result = masterSolver.solveMaster(master, FrameworkSolveOptions())) {
                is Ok -> {
                    val output = result.value as? SolveReport<Flt64>
                    output?.let(::ok) ?: Failed(
                        ErrorCode.Other,
                        "主问题求解器未返回可行输出 / Master solver did not return a feasible output"
                    )
                }

                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        },
        subproblemSolver = subproblemSolver,
        binding = binding,
        cutOracle = cutOracle,
        options = options
    )

    /** Solve a linear master and static CP subproblem. / 求解线性主问题和静态 CP 子问题。
     *
     * @param master Mutable linear master model. / 可变线性主问题模型。
     * @param subproblem Static CP subproblem model. / 静态 CP 子问题模型。
     * @return Benders report or a structured solver error. / Benders 报告或结构化求解错误。
     */
    suspend fun solve(
        master: LinearMetaModel<Flt64>,
        subproblem: ConstraintProgrammingModel
    ): Ret<LogicBasedBendersReport> {
        val optionError = validateOptions()
        if (optionError != null) {
            return Failed(ErrorCode.IllegalArgument, optionError)
        }
        val session = subproblemSolver.createSession(
            subproblem,
            options.constraintProgrammingOptions
        )
        if (session.failed) {
            return propagate(session)
        }
        return try {
            iterate(master, subproblem, session.value!!)
        } finally {
            session.value?.close()
        }
    }

    /** Solve and directly return the unified core report. / 求解并直接返回 core 统一报告。
     *
     * @param master Mutable linear master model. / 可变线性主问题模型。
     * @param subproblem Static CP subproblem model. / 静态 CP 子问题模型。
     * @return Core solve report or a structured solver error. / Core 统一求解报告或结构化求解错误。
     */
    suspend fun solveReport(
        master: LinearMetaModel<Flt64>,
        subproblem: ConstraintProgrammingModel
    ): Ret<SolveReport<Flt64>> {
        return solve(master, subproblem).map { it.toSolveReport() }
    }

    private suspend fun iterate(
        master: LinearMetaModel<Flt64>,
        subproblem: ConstraintProgrammingModel,
        session: ConstraintProgrammingSession
    ): Ret<LogicBasedBendersReport> {
        val subproblemSnapshot = subproblem.snapshot()
        if (subproblemSnapshot.failed) {
            return propagate(subproblemSnapshot)
        }
        options.resumeState?.let { resumeState ->
            val evidence = validateResumeEvidence(resumeState, subproblemSnapshot.value!!)
            if (evidence.failed) {
                return propagate(evidence)
            }
            val hasPersistedMasterState = resumeState.masterIncumbent != null ||
                resumeState.masterBestBound != null ||
                resumeState.masterFingerprint != null ||
                resumeState.assumptions.isNotEmpty() ||
                resumeState.fixedBindings.isNotEmpty() ||
                resumeState.conflicts.isNotEmpty() ||
                resumeState.convergenceVerified
            if (hasPersistedMasterState) {
                val applier = options.resumeStateApplier
                    ?: return Failed(
                        ErrorCode.IllegalArgument,
                        "Benders checkpoint 包含主问题状态但未提供恢复适配器 / " +
                            "Benders checkpoint contains master state without a resume applier"
                    )
                when (val applied = applier(master, resumeState)) {
                    is Ok -> {
                        val evidence = applied.value
                        if (evidence.masterFingerprint.isBlank() ||
                            evidence.masterFingerprint != resumeState.masterFingerprint ||
                            evidence.incumbent != resumeState.masterIncumbent ||
                            evidence.bestBound != resumeState.masterBestBound ||
                            evidence.assumptions != resumeState.assumptions ||
                            evidence.fixedBindings != resumeState.fixedBindings ||
                            evidence.conflicts != resumeState.conflicts ||
                            evidence.convergenceVerified != resumeState.convergenceVerified
                        ) {
                            return Failed(
                                ErrorCode.ORSolutionInvalid,
                                "Benders 主问题恢复证据与 checkpoint 不一致 / Benders master resume evidence disagrees with the checkpoint"
                            )
                        }
                    }
                    is Failed -> return Failed(applied.error)
                    is Fatal -> return Fatal(applied.errors)
                }
            }
        }
        val hasSubproblemObjective = subproblemSnapshot.value!!.objectives.isNotEmpty()
        val started = TimeSource.Monotonic.markNow()
        val cuts = ArrayList<BendersMasterCut>()
        val traces = ArrayList<BendersIterationTrace>()
        val knownCuts = HashSet<String>()
        val resumeState = options.resumeState
        if (resumeState != null) {
            val validated = validateCuts(resumeState.cuts, knownCuts)
            if (validated.failed) {
                return propagate(validated)
            }
            val seeded = addCuts(master, validated.value!!, cuts, knownCuts)
            if (seeded.failed) {
                return propagate(seeded)
            }
            for (encoded in resumeState.trace) {
                when (val decoded = BendersCheckpointCodec.decodeTrace(encoded)) {
                    is Ok -> traces += decoded.value
                    is Failed -> return Failed(decoded.error)
                    is Fatal -> return Fatal(decoded.errors)
                }
            }
        }
        var lastMaster: SolveReport<Flt64>? = null
        var lastAssignment: BendersSubproblemAssignment? = null
        var lastSubproblem: LogicBasedBendersSubproblemResult? = null
        var stallIterations = 0

        val firstIteration = resumeState?.iteration ?: 0
        if (firstIteration < 0 || firstIteration >= options.maxIterations && resumeState != null) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint iteration 超出当前引擎范围 / Benders checkpoint iteration is outside the active engine range"
            )
        }
        for (iteration in firstIteration until options.maxIterations) {
            if (options.cancellationToken?.isCancellationRequested == true) {
                return ok(
                    report(
                        status = ProblemStatus.Unknown,
                        termination = TerminationReason.Cancelled,
                        proof = ProofStatus.None,
                        master = lastMaster,
                        assignment = lastAssignment,
                        subproblem = lastSubproblem,
                        cuts = cuts,
                        traces = traces,
                        diagnostics = listOf(cancelledIssue())
                    )
                )
            }
            val masterResult = try {
                masterSolver.solve(master)
            } catch (error: Throwable) {
                return Failed(
                    ErrorCode.OREngineSolvingException,
                    "Benders 主问题求解失败：${error.message ?: error::class.simpleName} / " +
                        "Benders master solve failed: ${error.message ?: error::class.simpleName}"
                )
            }
            when (masterResult) {
                is Failed -> {
                    return if (masterResult.error.code == ErrorCode.ORModelInfeasible) {
                        ok(
                            report(
                                status = ProblemStatus.Infeasible,
                                termination = TerminationReason.Completed,
                                proof = ProofStatus.Verified,
                                master = lastMaster,
                                assignment = lastAssignment,
                                subproblem = lastSubproblem,
                                cuts = cuts,
                                traces = traces
                            )
                        )
                    } else {
                        Failed(masterResult.error)
                    }
                }

                is Fatal -> return Fatal(masterResult.errors)
                is Ok -> {}
            }
            val masterOutput = (masterResult as Ok).value
            lastMaster = masterOutput
            if (options.proofMode == BendersProofMode.Exact && masterOutput.toSolverStatus() != SolverStatus.Optimal) {
                return ok(
                    report(
                        status = ProblemStatus.Unknown,
                        termination = TerminationReason.BackendFailure,
                        proof = ProofStatus.None,
                        master = masterOutput,
                        assignment = lastAssignment,
                        subproblem = lastSubproblem,
                        cuts = cuts,
                        traces = traces,
                        diagnostics = listOf(
                            SolveIssue(
                                code = "benders-master-not-proven-optimal",
                                category = SolveIssueCategory.Backend,
                                message = "Exact 模式要求主问题最优证明 / Exact mode requires a proven master optimum"
                            )
                        )
                    )
                )
            }

            val source = options.masterSolutionSource?.invoke(masterOutput)
                ?: ok(masterSolutionValueSource(masterOutput, options.masterVariables))
            if (source.failed) {
                return propagate(source)
            }
            val assignmentResult = binding.bind(source.value!!, subproblem)
            if (assignmentResult.failed) {
                return propagate(assignmentResult)
            }
            val assignment = assignmentResult.value!!
            lastAssignment = assignment
            val subproblemOutput = try {
                session.solve(
                    assumptions = assignment.assumptions,
                    fixedValues = assignment.fixedValues
                )
            } catch (error: Throwable) {
                return Failed(
                    ErrorCode.OREngineSolvingException,
                    "Benders CP 子问题求解失败：${error.message ?: error::class.simpleName} / " +
                        "Benders CP subproblem solve failed: ${error.message ?: error::class.simpleName}"
                )
            }
            if (subproblemOutput.failed) {
                return propagate(subproblemOutput)
            }
            val result = classifySubproblem(subproblemOutput.value!!, assignment)
            lastSubproblem = result
            when (result) {
                is FeasibleSubproblemResult -> {
                    if (options.proofMode == BendersProofMode.Exact &&
                        (result.output.proofStatus != ProofStatus.Verified || result.output.status != SolverStatus.Optimal)
                    ) {
                        return ok(
                            report(
                                status = ProblemStatus.Unknown,
                                termination = TerminationReason.BackendFailure,
                                proof = ProofStatus.None,
                                master = masterOutput,
                                assignment = assignment,
                                subproblem = result,
                                cuts = cuts,
                                traces = traces,
                                diagnostics = listOf(
                                    SolveIssue(
                                        code = "benders-subproblem-not-proven",
                                        category = SolveIssueCategory.Backend,
                                        message = "Exact 模式要求 CP 子问题已证明最优或不可行 / " +
                                            "Exact mode requires the CP subproblem to be proven optimal or infeasible"
                                    )
                                )
                            )
                        )
                    }
                    if (options.proofMode == BendersProofMode.Exact &&
                        hasSubproblemObjective &&
                        result.output.compatibleObjective() == null &&
                        options.completeObjectiveEvaluator == null
                    ) {
                        return ok(
                            report(
                                status = ProblemStatus.Unknown,
                                termination = TerminationReason.BackendFailure,
                                proof = ProofStatus.None,
                                master = masterOutput,
                                assignment = assignment,
                                subproblem = result,
                                cuts = cuts,
                                traces = traces,
                                diagnostics = listOf(
                                    SolveIssue(
                                        code = "benders-subproblem-objective-missing",
                                        category = SolveIssueCategory.Backend,
                                        message = "CP 模型声明了目标但后端未返回可兼容 objective，且未提供完整目标求值契约 / " +
                                            "The CP model declares an objective but the backend returned no compatible objective and no complete objective evaluator was provided"
                                    )
                                )
                            )
                        )
                    }
                    val context = BendersCutContext(master, subproblem, assignment, iteration, options.proofMode)
                    val optimalityCuts = cutOracle.optimalityCuts(result, context)
                    if (optimalityCuts.failed) {
                        return propagate(optimalityCuts)
                    }
                    val newCuts = validateCuts(optimalityCuts.value!!, knownCuts)
                    if (newCuts.failed) {
                        return propagate(newCuts)
                    }
                    val trace = trace(
                        iteration = iteration,
                        masterModel = master,
                        master = masterOutput,
                        subproblem = BendersSubproblemStatus.Feasible,
                        objective = result.output.compatibleObjective(),
                        cutCount = newCuts.value!!.size,
                        conflictSize = 0,
                        started = started
                    )
                    traces += trace
                    when (val notification = notify(trace)) {
                        is Ok -> {}
                        is Failed -> return Failed(notification.error)
                        is Fatal -> return Fatal(notification.errors)
                    }
                    if (newCuts.value!!.isEmpty()) {
                        val hasVerifiedOptimalityCut = cuts.any {
                            it.kind == BendersCutKind.Optimality &&
                                it.validity == BendersCutValidity.Global &&
                                it.proofStatus == ProofStatus.Verified
                        }
                        if (options.proofMode == BendersProofMode.Exact && hasSubproblemObjective) {
                            if (!hasVerifiedOptimalityCut) {
                                return ok(
                                    report(
                                        status = ProblemStatus.Feasible,
                                        termination = TerminationReason.IterationLimit,
                                        proof = ProofStatus.None,
                                        master = masterOutput,
                                        assignment = assignment,
                                        subproblem = result,
                                        cuts = cuts,
                                        traces = traces,
                                        diagnostics = listOf(
                                            SolveIssue(
                                                code = "benders-no-optimality-proof",
                                                category = SolveIssueCategory.Unsupported,
                                                message = "优化型 CP 子问题没有全局有效 optimality proof / " +
                                                    "The optimization CP subproblem has no globally valid optimality proof"
                                            )
                                        )
                                    )
                                )
                            }
                            val objectiveEvaluator = options.completeObjectiveEvaluator
                            if (objectiveEvaluator == null) {
                                return ok(
                                    report(
                                        status = ProblemStatus.Feasible,
                                        termination = TerminationReason.IterationLimit,
                                        proof = ProofStatus.None,
                                        master = masterOutput,
                                        assignment = assignment,
                                        subproblem = result,
                                        cuts = cuts,
                                        traces = traces,
                                        diagnostics = listOf(
                                            SolveIssue(
                                                code = "benders-objective-contract-missing",
                                                category = SolveIssueCategory.Unsupported,
                                                message = "Exact 模式缺少完整 Benders 目标评估契约 / " +
                                                    "Exact mode requires a complete Benders objective evaluator",
                                                details = mapOf(
                                                    "masterObjective" to (masterOutput.solution?.objective ?: Flt64.zero).toString(),
                                                    "subproblemObjective" to (result.output.compatibleObjective()?.toString() ?: "missing")
                                                )
                                            )
                                        )
                                    )
                                )
                            }
                            val completeObjective = objectiveEvaluator(masterOutput, assignment, result.output)
                            if (completeObjective.failed) {
                                return propagate(completeObjective)
                            }
                            val expectedObjective = completeObjective.value!!
                            val convergenceGap = masterConvergenceGap(master, masterOutput)
                            val objectiveGap = (expectedObjective - (masterOutput.solution?.objective ?: Flt64.zero)).abs()
                            if (objectiveGap > options.optimalityTolerance) {
                                return ok(
                                    report(
                                        status = ProblemStatus.Feasible,
                                        termination = TerminationReason.IterationLimit,
                                        proof = ProofStatus.None,
                                        master = masterOutput,
                                        assignment = assignment,
                                        subproblem = result,
                                        cuts = cuts,
                                        traces = traces,
                                        diagnostics = listOf(
                                            SolveIssue(
                                                code = "benders-objective-inconsistent",
                                                category = SolveIssueCategory.Unsupported,
                                                message = "Exact 模式要求主问题目标与完整 Benders 目标一致 / " +
                                                    "Exact mode requires the master objective to match the complete Benders objective",
                                                details = mapOf(
                                                    "masterObjective" to (masterOutput.solution?.objective ?: Flt64.zero).toString(),
                                                    "expectedCompleteObjective" to expectedObjective.toString(),
                                                    "subproblemObjective" to (result.output.compatibleObjective()?.toString() ?: "missing"),
                                                    "gap" to objectiveGap.toString(),
                                                    "tolerance" to options.optimalityTolerance.toString()
                                                )
                                            )
                                        )
                                    )
                                )
                            }
                            if (convergenceGap == null || convergenceGap > options.optimalityTolerance) {
                                return ok(
                                    report(
                                        status = ProblemStatus.Feasible,
                                        termination = TerminationReason.IterationLimit,
                                        proof = ProofStatus.None,
                                        master = masterOutput,
                                        assignment = assignment,
                                        subproblem = result,
                                        cuts = cuts,
                                        traces = traces,
                                        diagnostics = listOf(
                                            SolveIssue(
                                                code = "benders-no-convergence-proof",
                                                category = SolveIssueCategory.Unsupported,
                                                message = "Exact 模式缺少满足容差的主问题上下界收敛证明 / " +
                                                    "Exact mode lacks a master incumbent/bound convergence proof within tolerance",
                                                details = mapOf(
                                                    "gap" to (convergenceGap?.toString() ?: "missing"),
                                                    "tolerance" to options.optimalityTolerance.toString()
                                                )
                                            )
                                        )
                                    )
                                )
                            }
                        }
                        val proof = if (options.proofMode == BendersProofMode.Exact) {
                            ProofStatus.Verified
                        } else {
                            ProofStatus.Claimed
                        }
                        return ok(
                            report(
                                status = ProblemStatus.Feasible,
                                termination = TerminationReason.Completed,
                                proof = proof,
                                master = masterOutput,
                                assignment = assignment,
                                subproblem = result,
                                cuts = cuts,
                                traces = traces
                            )
                        )
                    }
                    val added = addCuts(master, newCuts.value!!, cuts, knownCuts)
                    if (added.failed) {
                        return propagate(added)
                    }
                    stallIterations = 0
                }

                is InfeasibleSubproblemResult -> {
                    if (options.proofMode == BendersProofMode.Exact &&
                        result.proofStatus != ProofStatus.Verified
                    ) {
                        return ok(
                            report(
                                status = ProblemStatus.Unknown,
                                termination = TerminationReason.BackendFailure,
                                proof = ProofStatus.None,
                                master = masterOutput,
                                assignment = assignment,
                                subproblem = result,
                                cuts = cuts,
                                traces = traces,
                                diagnostics = listOf(
                                    SolveIssue(
                                        code = "benders-subproblem-not-proven-infeasible",
                                        category = SolveIssueCategory.Backend,
                                        message = "Exact 模式要求 CP 子问题不可行证明 / " +
                                            "Exact mode requires a proven CP subproblem infeasibility certificate"
                                    )
                                )
                            )
                        )
                    }
                    val context = BendersCutContext(master, subproblem, assignment, iteration, options.proofMode)
                    val generated = cutOracle.feasibilityCuts(result, context)
                    if (generated.failed) {
                        return propagate(generated)
                    }
                    val newCuts = validateCuts(generated.value!!, knownCuts)
                    if (newCuts.failed) {
                        return propagate(newCuts)
                    }
                    if (newCuts.value!!.isEmpty()) {
                        return ok(
                            report(
                                status = ProblemStatus.Unknown,
                                termination = TerminationReason.IterationLimit,
                                proof = ProofStatus.None,
                                master = masterOutput,
                                assignment = assignment,
                                subproblem = result,
                                cuts = cuts,
                                traces = traces,
                                diagnostics = listOf(
                                    SolveIssue(
                                        code = "benders-no-feasibility-cut",
                                        category = SolveIssueCategory.Unsupported,
                                        message = "不可行子问题没有可用的全局有效 cut / No globally valid cut is available for the infeasible subproblem"
                                    )
                                )
                            )
                        )
                    }
                    val before = cuts.size
                    val addedResult = addCuts(master, newCuts.value!!, cuts, knownCuts)
                    if (addedResult.failed) {
                        return propagate(addedResult)
                    }
                    val added = cuts.size - before
                    val trace = trace(
                        iteration = iteration,
                        masterModel = master,
                        master = masterOutput,
                        subproblem = BendersSubproblemStatus.Infeasible,
                        objective = null,
                        cutCount = added,
                        conflictSize = result.conflict?.members?.size ?: result.conflict?.constraintIds?.size ?: 0,
                        started = started
                    )
                    traces += trace
                    when (val notification = notify(trace)) {
                        is Ok -> {}
                        is Failed -> return Failed(notification.error)
                        is Fatal -> return Fatal(notification.errors)
                    }
                    if (added == 0) {
                        ++stallIterations
                        if (stallIterations >= options.stallIterationLimit) {
                            return ok(
                                report(
                                    status = ProblemStatus.Unknown,
                                    termination = TerminationReason.IterationLimit,
                                    proof = ProofStatus.None,
                                    master = masterOutput,
                                    assignment = assignment,
                                    subproblem = result,
                                    cuts = cuts,
                                    traces = traces,
                                    diagnostics = listOf(
                                        SolveIssue(
                                            code = "benders-cut-stalled",
                                            category = SolveIssueCategory.Backend,
                                            message = "Benders 割添加后未产生新约束 / Benders cut generation stalled without a new constraint"
                                        )
                                    )
                                )
                            )
                        }
                    } else {
                        stallIterations = 0
                    }
                }

                is UnknownSubproblemResult -> {
                    val trace = trace(
                        iteration = iteration,
                        masterModel = master,
                        master = masterOutput,
                        subproblem = BendersSubproblemStatus.Unknown,
                        objective = null,
                        cutCount = 0,
                        conflictSize = 0,
                        started = started
                    )
                    traces += trace
                    when (val notification = notify(trace)) {
                        is Ok -> {}
                        is Failed -> return Failed(notification.error)
                        is Fatal -> return Fatal(notification.errors)
                    }
                    return ok(
                        report(
                            status = ProblemStatus.Unknown,
                            termination = result.terminationReason,
                            proof = ProofStatus.None,
                            master = masterOutput,
                            assignment = assignment,
                            subproblem = result,
                            cuts = cuts,
                            traces = traces,
                            diagnostics = listOf(
                                SolveIssue(
                                    code = "benders-subproblem-unknown",
                                    category = SolveIssueCategory.Backend,
                                    message = "CP 子问题未得到证明终态 / CP subproblem did not reach a proven terminal state"
                                )
                            )
                        )
                    )
                }
            }
        }
        return ok(
            report(
                status = ProblemStatus.Unknown,
                termination = TerminationReason.IterationLimit,
                proof = ProofStatus.None,
                master = lastMaster,
                assignment = lastAssignment,
                subproblem = lastSubproblem,
                cuts = cuts,
                traces = traces,
                diagnostics = listOf(
                    SolveIssue(
                        code = "benders-iteration-limit",
                        category = SolveIssueCategory.Backend,
                        message = "Benders 达到迭代次数上限 / Benders reached the iteration limit"
                    )
                )
            )
        )
    }

    private fun classifySubproblem(
        output: ConstraintProgrammingSolverOutput,
        assignment: BendersSubproblemAssignment
    ): LogicBasedBendersSubproblemResult {
        return when (output) {
            is ConstraintProgrammingFeasibleOutput -> FeasibleSubproblemResult(assignment, output)
            is ConstraintProgrammingInfeasibleOutput ->
                InfeasibleSubproblemResult(assignment, output.conflict, output.proofStatus)
            is ConstraintProgrammingUnknownOutput -> UnknownSubproblemResult(assignment, output.terminationReason)
        }
    }

    private fun validateCuts(
        cuts: List<BendersMasterCut>,
        knownCuts: Set<String>
    ): Ret<List<BendersMasterCut>> {
        val accepted = ArrayList<BendersMasterCut>()
        for (cut in cuts) {
            if (cut.source.isBlank()) {
                return Failed(ErrorCode.IllegalArgument, "Benders cut source 不能为空 / Benders cut source must not be blank")
            }
            if (cut.inequality.comparison == Comparison.NE ||
                cut.inequality.comparison == Comparison.LT ||
                cut.inequality.comparison == Comparison.GT
            ) {
                return Failed(
                    ErrorCode.Other,
                    "Benders 主问题首版不支持严格或不等关系 cut / Strict or not-equal Benders cuts are unsupported"
                )
            }
            if (options.proofMode == BendersProofMode.Exact &&
                (cut.validity != BendersCutValidity.Global || cut.proofStatus != ProofStatus.Verified)
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Exact 模式只能接受已验证的全局有效 cut / Exact mode only accepts verified globally valid cuts"
                )
            }
            if (cut.key !in knownCuts && accepted.none { it.key == cut.key }) {
                accepted += cut
            }
        }
        return ok(accepted)
    }

    private fun validateResumeEvidence(
        state: BendersResumeState,
        snapshot: fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
    ): Try {
        if (state.iteration < 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint iteration 不能为负 / Benders checkpoint iteration must not be negative"
            )
        }
        val hasMasterState = state.masterIncumbent != null || state.masterBestBound != null ||
            state.assumptions.isNotEmpty() || state.fixedBindings.isNotEmpty() ||
            state.conflicts.isNotEmpty() || state.convergenceVerified
        if (hasMasterState && state.masterFingerprint.isNullOrBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders 主问题状态缺少模型指纹 / Benders master state is missing a model fingerprint"
            )
        }
        listOf(
            "masterIncumbent" to state.masterIncumbent,
            "masterBestBound" to state.masterBestBound
        ).forEach { (field, value) ->
            if (value != null && (value.toDoubleOrNull()?.isFinite() != true)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint 主问题字段无效：$field / Invalid Benders checkpoint master field: $field"
                )
            }
        }
        state.subproblemModelFingerprint?.let { expectedFingerprint ->
            val actualFingerprint = computeBendersSubproblemModelFingerprint(snapshot)
            when (actualFingerprint) {
                is Ok -> {
                    if (actualFingerprint.value != expectedFingerprint) {
                        return Failed(
                            ErrorCode.ORSolutionInvalid,
                            "Benders checkpoint 子问题模型指纹不匹配 / Benders checkpoint subproblem model fingerprint mismatch"
                        )
                    }
                }

                is Failed -> return Failed(actualFingerprint.error)
                is Fatal -> return Fatal(actualFingerprint.errors)
            }
        }
        val variableIds = snapshot.variables.mapTo(linkedSetOf()) { it.id.value }
        val constraintIds = snapshot.constraints.mapTo(linkedSetOf()) { it.id.value }
        if (state.assumptions.any { it !in variableIds }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Benders checkpoint assumption 引用了未知变量 / Benders checkpoint assumption references an unknown variable"
            )
        }
        for ((id, raw) in state.fixedBindings) {
            val definition = snapshot.variable(VariableId(id))
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint fixed binding 引用了未知变量：$id / Benders checkpoint fixed binding references an unknown variable: $id"
                )
            if (!definition.domain.contains(Int64(raw))) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Benders checkpoint fixed binding 超出值域：$id=$raw / Benders checkpoint fixed binding is outside the domain: $id=$raw"
                )
            }
        }
        for (conflict in state.conflicts) {
            if (conflict.validity !in setOf("Verified", "Heuristic", "Unknown") ||
                conflict.minimality !in setOf("Irreducible", "Partial", "NotChecked")
            ) {
                return Failed(ErrorCode.IllegalArgument, "Benders checkpoint conflict 证据枚举无效 / Benders checkpoint conflict evidence enum is invalid")
            }
            if (conflict.assumptionIds.any { it !in variableIds }) {
                return Failed(ErrorCode.IllegalArgument, "Benders checkpoint conflict assumption 无效 / Benders checkpoint conflict assumption is invalid")
            }
            for (member in conflict.memberIds) {
                when {
                    member.startsWith("constraint:") -> {
                        if (member.removePrefix("constraint:") !in constraintIds) {
                            return Failed(ErrorCode.IllegalArgument, "Benders checkpoint conflict 约束无效 / Benders checkpoint conflict constraint is invalid")
                        }
                    }
                    member.startsWith("domain:") -> {
                        if (member.removePrefix("domain:") !in variableIds) {
                            return Failed(ErrorCode.IllegalArgument, "Benders checkpoint conflict 域成员无效 / Benders checkpoint conflict domain member is invalid")
                        }
                    }
                    member.startsWith("bound:") -> {
                        val encoded = member.removePrefix("bound:")
                        val separator = encoded.lastIndexOf(':')
                        if (separator <= 0 ||
                            encoded.substring(0, separator) !in variableIds ||
                            runCatching { BoundSide.valueOf(encoded.substring(separator + 1)) }.isFailure
                        ) {
                            return Failed(ErrorCode.IllegalArgument, "Benders checkpoint conflict bound 成员无效 / Benders checkpoint conflict bound member is invalid")
                        }
                    }
                    else -> return Failed(ErrorCode.IllegalArgument, "Benders checkpoint conflict 成员类型未知 / Benders checkpoint conflict member type is unknown")
                }
            }
        }
        return ok
    }

    private fun addCuts(
        master: LinearMetaModel<Flt64>,
        newCuts: List<BendersMasterCut>,
        allCuts: MutableList<BendersMasterCut>,
        knownCuts: MutableSet<String>
    ): Ret<Int> {
        var added = 0
        newCuts.forEachIndexed { index, cut ->
            val name = cut.name ?: "benders-${allCuts.size + index}-${cut.kind.name.lowercase()}"
            for (auxiliary in cut.auxiliaryVariables) {
                val result = master.add(auxiliary)
                if (result.failed) {
                    return propagate(result)
                }
            }
            val inequalities = listOf(cut.inequality) + cut.additionalInequalities
            for ((constraintIndex, inequality) in inequalities.withIndex()) {
                val constraintName = if (inequalities.size == 1) name else "$name-$constraintIndex"
                val result = master.addConstraint(
                    relation = inequality,
                    name = constraintName,
                    displayName = constraintName
                )
                if (result.failed) {
                    return propagate(result)
                }
            }
            knownCuts += cut.key
            allCuts += cut.copy(name = name)
            ++added
        }
        return ok(added)
    }

    private fun masterConvergenceGap(
        master: LinearMetaModel<Flt64>,
        output: SolveReport<Flt64>
    ): Flt64? {
        val bestBound = output.bestBound ?: return null
        val signedGap = when (master.objectCategory) {
            fuookami.ospf.kotlin.core.model.basic.ObjectCategory.Minimum ->
                (output.solution?.objective ?: Flt64.zero) - bestBound
            fuookami.ospf.kotlin.core.model.basic.ObjectCategory.Maximum ->
                bestBound - (output.solution?.objective ?: Flt64.zero)
        }
        return if (signedGap < Flt64.zero) null else signedGap.abs()
    }

    private fun trace(
        iteration: Int,
        masterModel: LinearMetaModel<Flt64>,
        master: SolveReport<Flt64>,
        subproblem: BendersSubproblemStatus,
        objective: Flt64?,
        cutCount: Int,
        conflictSize: Int,
        started: TimeSource.Monotonic.ValueTimeMark
    ): BendersIterationTrace {
        val convergenceGap = masterConvergenceGap(masterModel, master)
        return BendersIterationTrace(
            iteration = iteration,
            masterObjective = master.solution?.objective,
            masterBestBound = master.bestBound,
            masterStatus = master.toSolverStatus(),
            subproblemStatus = subproblem,
            subproblemObjective = objective,
            cutCount = cutCount,
            conflictCoreSize = conflictSize,
            elapsed = started.elapsedNow(),
            proofMode = options.proofMode,
            convergenceGap = convergenceGap
        )
    }

    private fun notify(trace: BendersIterationTrace): Try {
        return when (val result = options.progressReporter?.invoke(trace)) {
            null -> ok
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun report(
        status: ProblemStatus,
        termination: TerminationReason,
        proof: ProofStatus,
        master: SolveReport<Flt64>?,
        assignment: BendersSubproblemAssignment?,
        subproblem: LogicBasedBendersSubproblemResult?,
        cuts: List<BendersMasterCut>,
        traces: List<BendersIterationTrace>,
        diagnostics: List<SolveIssue> = emptyList()
    ): LogicBasedBendersReport {
        return LogicBasedBendersReport(
            problemStatus = status,
            terminationReason = termination,
            proof = SolveProof(
                status = proof,
                kind = "logic-based-benders"
            ),
            masterOutput = master,
            assignment = assignment,
            subproblemResult = subproblem,
            cuts = cuts.toList(),
            iterations = traces.toList(),
            diagnostics = diagnostics
        )
    }

    private fun validateOptions(): String? {
        if (options.maxIterations <= 0) {
            return "Benders maxIterations 必须为正 / Benders maxIterations must be positive"
        }
        if (options.stallIterationLimit <= 0) {
            return "Benders stallIterationLimit 必须为正 / Benders stallIterationLimit must be positive"
        }
        if (options.optimalityTolerance < Flt64.zero) {
            return "Benders optimalityTolerance 不得为负 / Benders optimalityTolerance must not be negative"
        }
        return null
    }

    private fun cancelledIssue(): SolveIssue {
        return SolveIssue(
            code = "benders-cancelled",
            category = SolveIssueCategory.Backend,
            message = "Benders 求解已取消 / Benders solve was cancelled"
        )
    }
}

/** CP registration pipeline contract. / CP 注册管线契约。 */
interface ConstraintProgrammingPipeline {
    /** Stable pipeline name. / 稳定管线名称。 */
    val name: String

    /** Register constraints and variables. / 注册变量和约束。
     *
     * @param model CP model receiving the registration. / 接收注册内容的 CP 模型。
     * @return Registration result. / 注册结果。
     */
    fun register(model: ConstraintProgrammingModel): Try

    /** Execute pipeline registration. / 执行管线注册。
     *
     * @param model CP model receiving the registration. / 接收注册内容的 CP 模型。
     * @return Registration result. / 注册结果。
     */
    operator fun invoke(model: ConstraintProgrammingModel): Try {
        return register(model)
    }
}

/** Apply a list of CP pipelines. / 执行 CP 管线列表。
 *
 * @param model CP model receiving all registrations. / 接收全部注册内容的 CP 模型。
 * @return Registration result. / 注册结果。
 */
fun List<ConstraintProgrammingPipeline>.registerConstraintProgramming(
    model: ConstraintProgrammingModel
): Try {
    for (pipeline in this) {
        when (val result = pipeline.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

/** Benders subproblem pipeline contract. / Benders 子问题管线契约。 */
interface BendersSubproblemPipeline {
    /** Register a CP subproblem with the binding context. / 使用绑定上下文注册 CP 子问题。
     *
     * @param model CP subproblem model. / CP 子问题模型。
     * @param binding Master-to-CP binding context. / 主问题到 CP 的绑定上下文。
     * @return Registration result. / 注册结果。
     */
    fun register(
        model: ConstraintProgrammingModel,
        binding: BendersVariableBinding
    ): Try

    /** Extract a typed solution into domain state. / 将类型化解提取回领域状态。
     *
     * @param solution CP solution to extract. / 待提取的 CP 解。
     * @return Extraction result. / 提取结果。
     */
    fun extractSolution(
        solution: fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
    ): Try {
        return ok
    }
}

private fun BinaryBendersVariable.cpVariableId(): VariableId {
    return VariableId(subproblemVariableId ?: "${subproblemVariable.identifier}:${subproblemVariable.index}")
}

private fun IntegerBendersVariable.cpVariableId(): VariableId {
    return VariableId(subproblemVariableId ?: "${subproblemVariable.identifier}:${subproblemVariable.index}")
}

private fun AbstractVariableItem<*, *>.bendersStableKey(): String {
    return "${identifier}:${index}"
}

private fun ConstraintProgrammingFeasibleOutput.compatibleObjective(): Flt64? {
    // Do not reconstruct a floating-point Benders objective from an exact Int64 value. The
    // compatibility field is intentionally optional and cannot safely represent the full CP
    // domain; optimizing subproblems must provide a complete objective evaluator when it is
    // absent. / 不要从精确 Int64 目标重新构造 Benders 浮点目标；兼容字段是可选的，无法安全覆盖完整
    // CP 值域；缺少该字段时，优化子问题必须提供完整目标求值契约。
    return exactObjective?.let { null } ?: objective
}

private fun String.toFlt64OrNull(): Flt64? {
    return toDoubleOrNull()?.takeIf { it.isFinite() }?.let(::Flt64)
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "Benders 结果状态无效 / Invalid Benders result state")
    }
}
