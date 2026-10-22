package fuookami.ospf.kotlin.core.model.mechanism

import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/** 模型会话及其结构版本；不能跨模型误用。 / Model session and structural revision, never interchangeable across models.
 * @property owner 模型会话身份 / Model-session identity
 * @property sequence 结构版本 / Structural revision
 */
data class LinearModelRevision(val owner: Long, val sequence: Long)

/** 不可变的已展开线性行。未显式绑定的 ID 仅在当前模型会话内有效。
 * / Immutable expanded linear row; unbound IDs are local to this model session.
 * @property sourceName 原始显示名称，不用作身份 / Original display name, not an identity
 * @property coefficients 按变量身份索引的系数 / Coefficients keyed by variable identity
 * @property rhs 规范化右端 / Normalized right-hand side
 * @property comparison 比较方向 / Comparison direction
 */
data class LinearModelRow<V>(
    val id: String,
    val sourceName: String,
    val coefficients: Map<AbstractVariableItem<*, *>, V>,
    val rhs: V,
    val comparison: Comparison
) where V : RealNumber<V>, V : NumberField<V>

/** 变量范围快照，不保留可变范围对象。 / Variable-bound snapshot without retaining mutable range objects.
 * @property variable 变量身份 / Variable identity
 * @property lowerBound 下界 / Lower bound
 * @property upperBound 上界 / Upper bound
 */
data class LinearVariableState<V>(
    val variable: AbstractVariableItem<*, *>,
    val lowerBound: V?,
    val upperBound: V?
) where V : RealNumber<V>, V : NumberField<V>

/** 从原始模型派生的结构快照；目标统一为最小化。 / Structural snapshot derived from the source, with a minimizing objective.
 * @property revision 会话及版本 / Session and revision
 * @property variables 包含展开辅助变量的范围 / Bounds including expanded auxiliary variables
 * @property rows 完整已展开行 / Complete expanded rows
 * @property objective 规范化目标系数 / Normalized objective coefficients
 * @property objectiveConstant 只计算一次的目标常数 / Objective constant counted once
 * @property objectCategory 原始目标方向 / Original objective direction
 */
data class LinearModelSnapshot<V>(
    val revision: LinearModelRevision,
    val variables: List<LinearVariableState<V>>,
    val rows: List<LinearModelRow<V>>,
    val objective: Map<AbstractVariableItem<*, *>, V>,
    val objectiveConstant: V,
    val objectCategory: ObjectCategory
) where V : RealNumber<V>, V : NumberField<V>

/** 模型自有的结构观察器；原始 MetaModel 是唯一模型定义来源。
 * / Model-owned structural observer; the original MetaModel remains the sole model definition.
 * 在显式刷新边界展开并比较结构，包含直接修改的约束、目标和变量范围；不声称监听可变对象。
 * / Expansion and comparison at explicit refresh boundaries include direct edits, without claiming mutation listeners.
 * @property model 原始模型 / Source model
 */
class LinearModelState<V> internal constructor(val model: LinearMetaModel<V>)
    where V : RealNumber<V>, V : NumberField<V> {
    private val owner = owners.incrementAndGet()
    private val rowIds = IdentityHashMap<MathConstraint, String>()
    private val namedRows = linkedMapOf<String, Pair<MathConstraint, LinearModelRow<V>>>()
    private var latest: LinearModelSnapshot<V>? = null

    companion object {
        private val owners = AtomicLong()
    }

    /** 注册已有变量上的不可变线性行；同 ID 同内容幂等，冲突则失败。
     * / Register an immutable linear row over existing variables; identical IDs/contents are idempotent.
     * @param id 显式稳定行身份 / Explicit stable row identity
     * @param relation 变量级线性表达式，不接受未展开符号 / Variable-level linear expression, without unexpanded symbols
     * @return 注册结果 / Registration result
     */
    fun registerRow(id: ConstraintId, relation: LinearInequality<V>): Try {
        val identityCheck = ModelElementIdentityRegistry().registerConstraint(Any(), id)
        if (identityCheck !is Ok) return identityCheck
        if (relation.comparison !in setOf(Comparison.LE, Comparison.EQ, Comparison.GE)) {
            return Failed(ErrorCode.IllegalArgument, "动态线性行只支持非严格比较 / Dynamic linear rows require non-strict comparisons")
        }
        val difference = relation.lhs - relation.rhs
        val coefficients = linkedMapOf<AbstractVariableItem<*, *>, V>()
        for (term in difference.monomials) {
            val variable = term.symbol as? AbstractVariableItem<*, *>
                ?: return Failed(ErrorCode.IllegalArgument, "动态行必须引用已展开变量 / Dynamic rows require expanded variables")
            if (model.tokens.find(variable) == null || !term.coefficient.isFinite()) {
                return Failed(ErrorCode.IllegalArgument, "动态行引用未知变量或非有限系数 / Unknown dynamic-row variable or nonfinite coefficient")
            }
            coefficients[variable] = (coefficients[variable] ?: model.converter.zero) + term.coefficient
        }
        val normalized = coefficients.filterValues { it.compareTo(model.converter.zero) != 0 }
        if (!difference.constant.isFinite() || normalized.values.any { !it.isFinite() }) {
            return Failed(ErrorCode.IllegalArgument, "动态行包含非有限值 / Dynamic row contains nonfinite values")
        }
        val definition = LinearModelRow(
            id = id.value,
            sourceName = id.value,
            coefficients = normalized,
            rhs = -difference.constant,
            comparison = relation.comparison
        )
        namedRows[id.value]?.let { (origin, previous) ->
            return if (origin in model.constraints && previous == definition) ok
                else Failed(ErrorCode.IllegalArgument, "行身份已被不同内容占用 / Row identity already has different content: ${id.value}")
        }
        if (latest?.rows?.any { it.id == id.value } == true || model.identityRegistry?.entries()?.any { it.id.value == id.value } == true) {
            return Failed(ErrorCode.IllegalArgument, "行身份已存在 / Row identity already exists: ${id.value}")
        }
        val frozen = LinearInequality(
            lhs = LinearPolynomial(normalized.map { LinearMonomial(it.value, it.key) }, model.converter.zero),
            rhs = LinearPolynomial(constant = definition.rhs),
            comparison = definition.comparison
        )
        val added = model.addConstraint(relation = frozen, name = id.value)
        if (added !is Ok) return added
        val origin = model.constraints.last()
        val registered = model.identityRegistry?.registerConstraint(origin, id)
        if (registered is Failed) return Failed(registered.error)
        if (registered is Fatal) return Fatal(registered.errors)
        rowIds[origin] = id.value
        namedRows[id.value] = origin to definition
        return ok
    }

    /** 刷新不可变快照，检测直接模型修改；调用期间不得并发修改模型。
     * / Refresh an immutable snapshot, detecting direct edits; concurrent model mutation is unsupported.
     * @return 当前结构及版本 / Current structure and revision
     */
    suspend fun refresh(): Ret<LinearModelSnapshot<V>> {
        val expanded = when (val result = LinearMechanismModel(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return expanded.use { full ->
            // 快照必须包含延迟函数的完整线性语义，而不是后端待处理的结构。
            // Snapshots require full linear semantics, not deferred backend structures.
            if (full.functionExpansionPolicy != FunctionExpansionPolicy.EAGER) {
                for (symbol in full.tokens.symbols) {
                    val function = symbol as? MathFunctionSymbolBase<*> ?: continue
                    if (function.deferredStructure()?.supportsDeferredFallback() != true) continue
                    when (val result = function.registerConstraintsUnchecked(full)) {
                        is Ok -> {}
                        is Failed -> return@use Failed(result.error)
                        is Fatal -> return@use Fatal(result.errors)
                    }
                }
            }
            val variables = full.tokens.tokensInSolver.map { token ->
                LinearVariableState(
                    variable = token.variable,
                    lowerBound = token.variable.lowerBound?.value?.unwrapOrNull()?.let(model.converter::intoValue),
                    upperBound = token.variable.upperBound?.value?.unwrapOrNull()?.let(model.converter::intoValue)
                )
            }
            for (state in variables.filter { model.tokens.find(it.variable) == null }) {
                when (val added = model.add(state.variable)) {
                    is Ok -> {}
                    is Failed -> return@use Failed(added.error)
                    is Fatal -> return@use Fatal(added.errors)
                }
            }
            var derivedIndex = 0
            val rows = full.constraints.map { row ->
                val coefficients = linkedMapOf<AbstractVariableItem<*, *>, V>()
                for (cell in row.lhs) {
                    cell as LinearCell<V>
                    coefficients[cell.token.variable] = (coefficients[cell.token.variable] ?: model.converter.zero) + cell.coefficient
                }
                val origin = row.origin
                val id = origin?.let {
                    model.identityRegistry?.identity(it)?.id?.value ?: rowIds.getOrPut(it) { "model-local-row:$owner:${rowIds.size}" }
                } ?: "model-local-derived:$owner:${derivedIndex++}"
                LinearModelRow(
                    id = id,
                    sourceName = row.name,
                    coefficients = coefficients.filterValues { it.compareTo(model.converter.zero) != 0 },
                    rhs = row.rhs,
                    comparison = when (row.sign) {
                        ConstraintRelation.LessEqual -> Comparison.LE
                        ConstraintRelation.GreaterEqual -> Comparison.GE
                        else -> Comparison.EQ
                    }
                )
            }
            if (rows.map { it.id }.distinct().size != rows.size) {
                return@use Failed(ErrorCode.IllegalArgument, "已展开行身份重复 / Duplicate expanded row identities")
            }
            val objective = linkedMapOf<AbstractVariableItem<*, *>, V>()
            var constant = model.converter.zero
            for (part in full.objectFunction.subObjects) {
                val sign = if (part.category == ObjectCategory.Minimum) model.converter.one else -model.converter.one
                constant += sign * part.constant
                for ((coefficient, variable) in part.linearTerms()) {
                    objective[variable] = (objective[variable] ?: model.converter.zero) + sign * coefficient
                }
            }
            if (rows.any { row -> !row.rhs.isFinite() || row.coefficients.values.any { !it.isFinite() } } ||
                !constant.isFinite() || objective.values.any { !it.isFinite() }) {
                return@use Failed(ErrorCode.IllegalArgument, "模型快照包含非有限系数 / Model snapshot contains nonfinite coefficients")
            }
            val previous = latest
            var snapshot = LinearModelSnapshot(
                revision = previous?.revision ?: LinearModelRevision(owner, 0),
                variables = variables,
                rows = rows,
                objective = objective.filterValues { it.compareTo(model.converter.zero) != 0 },
                objectiveConstant = constant,
                objectCategory = model.objectCategory
            )
            if (previous != null && previous != snapshot) snapshot = snapshot.copy(revision = LinearModelRevision(owner, previous.revision.sequence + 1))
            latest = snapshot
            Ok(snapshot)
        }
    }

    /** 拒绝不同模型或过期版本。 / Reject foreign-model or stale revisions.
     * @param revision 被使用的模型版本 / Model revision being consumed
     * @return 当前版本验证 / Current-revision validation
     */
    suspend fun requireCurrent(revision: LinearModelRevision): Try {
        return when (val result = refresh()) {
            is Ok -> if (result.value.revision == revision) ok else Failed(ErrorCode.IllegalArgument, "模型版本已过期或属于其他模型 / Stale or foreign model revision")
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}
