package fuookami.ospf.kotlin.framework.solver

import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*

/** 局部决策到配置块的显式映射，无装箱业务语义。 / Explicit local-decision ownership, without packing semantics.
 * @property blockId 配置块身份 / Configuration-block identity
 * @property variable 原始模型变量 / Source-model variable
 */
data class ColumnDecision(
    val id: String,
    val blockId: String,
    val variable: AbstractVariableItem<*, *>
)

/** 配置列的数值投影。 / Numeric projection of a configuration column.
 * @property blockId 配置块 / Configuration block
 * @property values 全部局部决策值 / Complete local-decision values
 */
data class ProjectedColumn<V>(
    val id: String,
    val blockId: String,
    val values: Map<String, V>
) where V : RealNumber<V>, V : NumberField<V>

/** 原始模型及列投影版本。 / Source-model and column-projection revision.
 * @property source 原始结构版本 / Source structural revision
 * @property owner 投影会话 / Projection session
 * @property sequence 列池或投影变更版本 / Column-pool or projection revision
 */
data class ColumnProjectionRevision(
    val source: LinearModelRevision,
    val owner: Long,
    val sequence: Long
)

/** 带完整同源系数和版本的定价请求。 / Versioned pricing request with coefficients from the same row definitions.
 * @property blockId 定价配置块 / Block being priced
 * @property coefficients 决策检验数系数 / Decision reduced-cost coefficients
 * @property convexityPrice 恰选一列的对偶 / Exactly-one-column dual
 * @property rowPrices 原始行身份到对偶 / Source-row identities to duals
 * @property revision 产生请求的模型及列池版本 / Source-model and pool revision
 */
data class ColumnPricingRequest<V>(
    val blockId: String,
    val coefficients: Map<String, V>,
    val convexityPrice: V,
    val rowPrices: Map<String, V>,
    val revision: ColumnProjectionRevision
) where V : RealNumber<V>, V : NumberField<V> {
    /** 计算完整检验数，不将缺失值当作零。 / Evaluate reduced cost without treating missing values as zero.
     * @param values 完整局部向量 / Complete local vector
     * @return 检验数或结构化错误 / Reduced cost or structured error
     */
    fun reducedCost(values: Map<String, V>): Ret<V> {
        if (values.keys != coefficients.keys || values.values.any { !it.isFinite() } || coefficients.values.any { !it.isFinite() } || !convexityPrice.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "定价向量必须完整且有限 / Pricing vector must be complete and finite")
        }
        val cost = coefficients.entries.fold(-convexityPrice) { total, (id, coefficient) -> total + coefficient * values.getValue(id) }
        return if (cost.isFinite()) Ok(cost) else Failed(ErrorCode.IllegalArgument, "检验数溢出 / Reduced cost overflow")
    }
}

/** 同一已校验投影快照的局部定价请求与模型。 / Pricing request and local model from one validated projection snapshot.
 * @property request 当前块的定价请求 / Pricing request for this block
 * @property model 当前块的局部模型 / Local model for this block
 */
data class ColumnPricingEntry<V>(
    val request: ColumnPricingRequest<V>,
    val model: LinearMetaModel<V>
) where V : RealNumber<V>, V : NumberField<V>

/** 一次同步准备生成的同源定价批次；异步定价后仍须重新校验。 / Same-source pricing batch prepared synchronously; revalidate after asynchronous pricing.
 * @property revision 批次共用的源模型及列池版本 / Shared source-model and pool revision
 * @property entries 按输入块顺序生成的请求与局部模型 / Requests and local models in input block order
 */
class ColumnPricingBatch<V> internal constructor(
    internal val owner: Any,
    val revision: ColumnProjectionRevision,
    val entries: List<ColumnPricingEntry<V>>
) where V : RealNumber<V>, V : NumberField<V>

/** 一次主问题注册的变量、行身份和投影版本。 / Variables, row bindings and projection revision of one master registration.
 * @property model 当前主问题 / Current master
 * @property columnVariables 配置列到当前变量 / Column IDs to model-local variables
 * @property refinedResources 独立补全的局部资源 / Independently completed local resources
 * @property rows 原始行 ID 到当前实际约束 / Source-row IDs to current constraint objects
 * @property convexities 配置块到恰选一列约束 / Blocks to exactly-one constraints
 * @property revision 注册版本 / Registration revision
 * @property modelRevision 主问题结构版本 / Master structural revision
 * @property phaseOne 是否人工变量阶段 / Artificial-variable phase
 * @property integer 是否整数列选择 / Integer-column selection
 * @property columns 本次注册冻结的列向量 / Column vectors frozen for this registration
 */
data class ColumnMasterRegistration<V>(
    val model: LinearMetaModel<V>,
    val columnVariables: Map<String, URealVar>,
    val refinedResources: Set<AbstractVariableItem<*, *>>,
    val rows: Map<String, MathConstraint>,
    val convexities: Map<String, MathConstraint>,
    val revision: ColumnProjectionRevision,
    val modelRevision: LinearModelRevision,
    val phaseOne: Boolean,
    val integer: Boolean,
    val columns: List<ProjectedColumn<V>>
) where V : RealNumber<V>, V : NumberField<V>

/** 共享线性列映射与定价投影；不负责配置可行性、定价算法或联合迭代。
 * / Shared linear column/pricing projection, without configuration feasibility, pricing algorithms or joint iteration.
 * @property source 唯一原始模型 / Sole source model
 * @property decisions 显式局部变量映射 / Explicit local-variable mapping
 * @property blockIds 需要恰选一列的配置块 / Blocks requiring exactly one column
 */
class ColumnProjection<V>(
    val source: LinearMetaModel<V>,
    decisions: List<ColumnDecision>,
    blockIds: Set<String>
) where V : RealNumber<V>, V : NumberField<V> {
    val decisions = decisions.toList()
    val blockIds = blockIds.toSet()
    private val owner = owners.incrementAndGet()
    private var sequence = 0L
    private var current: LinearModelSnapshot<V>? = null
    private var projectionIndex: ColumnProjectionIndex<V>? = null
    private var prices: Pair<ColumnProjectionRevision, Map<String, V>>? = null
    private var convexityPrices = emptyMap<String, V>()
    private var active: ColumnMasterRegistration<V>? = null
    private val zero get() = source.converter.zero
    private val one get() = source.converter.one

    companion object {
        private val owners = AtomicLong()
    }

    private fun currentRevision(snapshot: LinearModelSnapshot<V>) = ColumnProjectionRevision(
        source = snapshot.revision,
        owner = owner,
        sequence = sequence
    )

    /** 重新读取模型基础层快照，原始模型变化使旧对偶失效。
     * / Read the model-layer snapshot; source changes invalidate old duals.
     * @return 当前结构或映射错误 / Current structure or mapping error
     */
    suspend fun snapshot(): Ret<LinearModelSnapshot<V>> {
        if (blockIds.isEmpty() || blockIds.any { it.isBlank() } || decisions.any { it.id.isBlank() || it.blockId !in blockIds } ||
            decisions.map { it.id }.distinct().size != decisions.size || decisions.map { it.variable }.distinct().size != decisions.size) {
            return Failed(ErrorCode.IllegalArgument, "列决策映射无效 / Invalid column-decision mapping")
        }
        val snapshot = when (val result = source.state.refresh()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val variables = snapshot.variables.map { it.variable }.toSet()
        if (decisions.any { it.variable !in variables }) return Failed(ErrorCode.IllegalArgument, "列决策引用未知变量 / Column decision references an unknown variable")
        if (current?.revision != snapshot.revision) prices = null
        if (projectionIndex?.revision != snapshot.revision) {
            projectionIndex = ColumnProjectionIndex(
                decisions = decisions,
                blockIds = blockIds,
                snapshot = snapshot
            )
        }
        current = snapshot
        return Ok(snapshot)
    }

    /** 列生命周期发生变化时立即撤销旧注册和对偶。 / Immediately invalidate old registrations and duals on column changes. */
    fun columnsChanged() {
        sequence++
        prices = null
        active = null
    }

    /** 检查请求或注册是否仍对应当前模型。 / Check whether a request or registration still belongs to the current model.
     * @param revision 被消费的版本 / Revision being consumed
     * @return 验证结果 / Validation result
     */
    suspend fun requireCurrent(revision: ColumnProjectionRevision): Try {
        val snapshot = when (val result = snapshot()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (revision != currentRevision(snapshot)) {
            return Failed(ErrorCode.IllegalArgument, "列投影注册或定价版本已过期 / Stale column-projection registration or pricing revision")
        }
        val registration = active
        return if (registration != null) registration.model.state.requireCurrent(registration.modelRevision) else ok
    }

    /** 校验定价批次仍对应当前模型；应在任何异步定价结果被接受前调用。
     * / Validate that a pricing batch still belongs to the current model before accepting asynchronous results.
     * @param batch 待消费的定价批次 / Pricing batch to consume
     * @return freshness 验证结果 / Freshness validation result
     */
    suspend fun requireCurrent(batch: ColumnPricingBatch<V>): Try {
        if (batch.owner !== this || batch.entries.isEmpty() || batch.entries.any { it.request.revision != batch.revision }) {
            return Failed(ErrorCode.IllegalArgument, "定价批次无效 / Invalid pricing batch")
        }
        return requireCurrent(batch.revision)
    }

    /** 同一原始行对配置列的贡献。 / Contribution of the same source row to a column.
     * @param terms 原始行按块分组的快照项 / Captured source-row terms grouped by block
     * @param column 配置列 / Configuration column
     * @param excluded 改为独立变量的资源 / Resources represented as independent variables
     * @return 配置列系数 / Column coefficient
     */
    private fun coefficient(
        terms: Map<String, List<Pair<ColumnDecision, V>>>,
        column: ProjectedColumn<V>,
        excluded: Set<AbstractVariableItem<*, *>> = emptySet()
    ): V {
        return terms[column.blockId].orEmpty().fold(zero) { total, (decision, coefficient) ->
            if (decision.variable in excluded) total else total + coefficient * column.values.getValue(decision.id)
        }
    }

    /** 重建主问题；全部行及目标来自原始模型快照，不维护第二份模型定义。
     * / Rebuild a master from source rows/objective, without a second model definition.
     * @param columns 当前完整列池 / Current complete column pool
     * @param phaseOne 是否使用人工变量可行性目标 / Whether using an artificial feasibility objective
     * @param integer 是否整数列选择 / Whether selecting integer columns
     * @param refinedResources 改为独立补全的已映射资源 / Mapped resources completed independently
     * @param tolerance 列值与当前变量范围比较的数值容差 / Numeric tolerance for current column bounds
     * @return 带实际行绑定的注册 / Registration with actual row bindings
     */
    suspend fun master(
        columns: List<ProjectedColumn<V>>,
        phaseOne: Boolean,
        integer: Boolean,
        refinedResources: Set<AbstractVariableItem<*, *>> = emptySet(),
        tolerance: V = source.converter.zero
    ): Ret<ColumnMasterRegistration<V>> {
        val snapshot = when (val result = snapshot()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val index = projectionIndex!!
        val mapped = index.mappedVariables
        if (!tolerance.isFinite() || tolerance < zero) return Failed(ErrorCode.IllegalArgument, "列范围容差无效 / Invalid column-bound tolerance")
        if (!mapped.containsAll(refinedResources) || columns.map { it.id }.distinct().size != columns.size || columns.any { column ->
                column.id.isBlank() || column.blockId !in blockIds || column.values.keys != index.idsByBlock[column.blockId] || column.values.values.any { !it.isFinite() }
            }) return Failed(ErrorCode.IllegalArgument, "列向量或资源映射无效 / Invalid column vector or resource mapping")
        val bounds = snapshot.variables.associateBy { it.variable }
        val localRows = index.localRows.mapValues { (_, rows) ->
            rows.filter { it.coefficients.keys.none { variable -> variable in refinedResources } }
        }
        val columnsByBlock = columns.groupBy { it.blockId }
        val projectedRows = if (columns.isEmpty() || snapshot.rows.isEmpty()) emptyMap() else index.rowTerms
        for (column in columns) {
            for (decision in index.decisionsByBlock[column.blockId].orEmpty()) {
                if (decision.variable in refinedResources) continue
                val value = column.values.getValue(decision.id)
                val state = bounds.getValue(decision.variable)
                if (state.lowerBound?.let { value < it - tolerance } == true || state.upperBound?.let { value > it + tolerance } == true) {
                    return Failed(ErrorCode.IllegalArgument, "旧列不满足当前变量范围，需重新定价或删列 / Column violates current bounds; reprice or remove it: ${column.id}")
                }
            }
            for (row in localRows.getValue(column.blockId)) {
                val value = coefficient(projectedRows.getValue(row.id), column)
                val feasible = when (row.comparison) {
                    Comparison.LE -> value <= row.rhs + tolerance
                    Comparison.GE -> value >= row.rhs - tolerance
                    else -> value >= row.rhs - tolerance && value <= row.rhs + tolerance
                }
                if (!value.isFinite() || !feasible) {
                    return Failed(ErrorCode.IllegalArgument, "旧列不满足当前局部行，需重新定价或删列 / Column violates a current local row; reprice or remove it: ${column.id}, ${row.id}")
                }
            }
        }
        columnsChanged()
        val model = LinearMetaModel<V>(name = "column_master_${phaseOne}_$integer", converter = source.converter)
        val lambdas = linkedMapOf<String, URealVar>()
        for ((index, column) in columns.withIndex()) {
            val variable = URealVar("partition_column_$index")
            lambdas[column.id] = variable
            when (val added = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(added.error)
                is Fatal -> return Fatal(added.errors)
            }
            if (integer) {
                val selected = BinVar("partition_selected_$index")
                val added = model.add(selected)
                if (added is Failed) return Failed(added.error)
                if (added is Fatal) return Fatal(added.errors)
                val relation = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(one, variable)), zero),
                    rhs = LinearPolynomial(listOf(LinearMonomial(one, selected)), zero),
                    comparison = Comparison.EQ
                )
                when (val result = model.addConstraint(relation = relation, name = "column_integral_$index")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        for (variable in snapshot.variables.map { it.variable }.filter { it !in mapped || it in refinedResources }) {
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val convexities = linkedMapOf<String, MathConstraint>()
        for ((index, block) in blockIds.withIndex()) {
            val lhs = LinearPolynomial(columnsByBlock[block].orEmpty().map { LinearMonomial(one, lambdas.getValue(it.id)) }, zero)
            when (val result = model.addConstraint(relation = LinearInequality(
                lhs = lhs,
                rhs = LinearPolynomial(constant = one),
                comparison = Comparison.EQ
            ), name = "column_choose_$index")) {
                is Ok -> convexities[block] = model.constraints.last()
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        var objective = LinearPolynomial(constant = if (phaseOne) zero else snapshot.objectiveConstant)
        if (!phaseOne) {
            objective += LinearPolynomial(snapshot.objective.filterKeys { it !in mapped || it in refinedResources }.map { LinearMonomial(it.value, it.key) }, zero)
            val objectiveTerms = if (columns.isEmpty()) emptyMap() else index.objectiveTerms
            for (column in columns) {
                val coefficient = objectiveTerms[column.blockId].orEmpty().fold(zero) { total, (decision, coefficient) ->
                    if (decision.variable in refinedResources) total else total + coefficient * column.values.getValue(decision.id)
                }
                objective += LinearPolynomial(listOf(LinearMonomial(coefficient, lambdas.getValue(column.id))), zero)
            }
        }
        val rows = linkedMapOf<String, MathConstraint>()
        for ((index, row) in snapshot.rows.withIndex()) {
            var lhs = LinearPolynomial(row.coefficients.filterKeys { it !in mapped || it in refinedResources }.map { LinearMonomial(it.value, it.key) }, zero)
            val terms = projectedRows[row.id].orEmpty()
            for (column in columns) {
                val value = coefficient(
                    terms = terms,
                    column = column,
                    excluded = refinedResources
                )
                lhs += LinearPolynomial(listOf(LinearMonomial(value, lambdas.getValue(column.id))), zero)
            }
            if (phaseOne) {
                if (row.comparison != Comparison.GE) {
                    val slack = URealVar("excess_$index")
                    when (val added = model.add(slack)) {
                        is Ok -> {}
                        is Failed -> return Failed(added.error)
                        is Fatal -> return Fatal(added.errors)
                    }
                    lhs -= LinearPolynomial(listOf(LinearMonomial(one, slack)), zero)
                    objective += LinearPolynomial(listOf(LinearMonomial(one, slack)), zero)
                }
                if (row.comparison != Comparison.LE) {
                    val slack = URealVar("shortage_$index")
                    when (val added = model.add(slack)) {
                        is Ok -> {}
                        is Failed -> return Failed(added.error)
                        is Fatal -> return Fatal(added.errors)
                    }
                    lhs += LinearPolynomial(listOf(LinearMonomial(one, slack)), zero)
                    objective += LinearPolynomial(listOf(LinearMonomial(one, slack)), zero)
                }
            }
            when (val result = model.addConstraint(relation = LinearInequality(
                lhs = lhs,
                rhs = LinearPolynomial(constant = row.rhs),
                comparison = row.comparison
            ), name = row.id)) {
                is Ok -> rows[row.id] = model.constraints.last()
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        when (val result = model.minimize(objective)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val modelRevision = when (val result = model.state.refresh()) {
            is Ok -> result.value.revision
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val registration = ColumnMasterRegistration(
            model = model,
            columnVariables = lambdas.toMap(),
            refinedResources = refinedResources.toSet(),
            rows = rows.toMap(),
            convexities = convexities.toMap(),
            revision = currentRevision(snapshot),
            modelRevision = modelRevision,
            phaseOne = phaseOne,
            integer = integer,
            columns = columns.map { it.copy(values = it.values.toMap()) }
        )
        active = registration
        return Ok(registration)
    }

    /** 验证源模型、列池及主问题本身均未变化。 / Validate source, pool and master structure.
     * @param registration 被消费的注册 / Registration being consumed
     * @return 当前注册验证 / Current-registration validation
     */
    suspend fun requireCurrent(registration: ColumnMasterRegistration<V>): Try {
        val valid = requireCurrent(registration.revision)
        if (valid !is Ok) return valid
        if (active !== registration) return Failed(ErrorCode.IllegalArgument, "主问题注册已失效 / Master registration is no longer active")
        return ok
    }

    /** 按实际约束来源绑定对偶，不使用显示名或跨模型列表顺序。
     * / Bind duals through actual constraint origins, never display names or cross-model order.
     * @param registration 产生 LP 的注册 / Registration that produced the LP
     * @param dual 当前最优 LP 的完整原始行对偶 / Complete original-row duals of the current optimal LP
     * @return 验证结果；缺失对偶不默认成零 / Validation result; missing duals are not silently zero
     */
    suspend fun refreshShadowPrice(registration: ColumnMasterRegistration<V>, dual: Map<Constraint<V, Linear>, V>): Try {
        prices = null
        convexityPrices = emptyMap()
        val valid = requireCurrent(registration)
        if (valid !is Ok) return valid
        if (registration.integer || registration.refinedResources.isNotEmpty()) return Failed(ErrorCode.IllegalArgument, "整数或资源补全主问题不能提供定价对偶 / Integer or resource-completion masters cannot supply pricing duals")
        val byOrigin = IdentityHashMap<MathConstraint, V>()
        for ((row, value) in dual) {
            val origin = row.origin ?: return Failed(ErrorCode.IllegalArgument, "对偶行缺少来源 / Dual row has no origin")
            byOrigin[origin] = value
        }
        val expected = registration.rows.values + registration.convexities.values
        if (dual.size != expected.size || byOrigin.size != expected.size || expected.any { !byOrigin.containsKey(it) } || byOrigin.values.any { !it.isFinite() }) {
            return Failed(ErrorCode.IllegalArgument, "对偶不属于当前注册或行不完整 / Duals belong to another registration or omit rows")
        }
        prices = registration.revision to registration.rows.mapValues { byOrigin.getValue(it.value) }
        convexityPrices = registration.convexities.mapValues { byOrigin.getValue(it.value) }
        return ok
    }

    private suspend fun validatePricingState(
        snapshot: LinearModelSnapshot<V>,
        phaseOne: Boolean,
        seed: Boolean
    ): Try {
        val revision = currentRevision(snapshot)
        if (!seed && prices?.first != revision) return Failed(ErrorCode.IllegalArgument, "定价需要当前注册的对偶 / Pricing requires current-registration duals")
        if (!seed) {
            val registration = active ?: return Failed(ErrorCode.IllegalArgument, "缺少当前主问题 / Missing active master")
            val valid = registration.model.state.requireCurrent(registration.modelRevision)
            if (valid !is Ok) return when (valid) {
                is Failed -> Failed(valid.error)
                is Fatal -> Fatal(valid.errors)
                else -> Failed(ErrorCode.ApplicationError)
            }
            if (registration.phaseOne != phaseOne) return Failed(ErrorCode.IllegalArgument, "定价阶段与对偶不一致 / Pricing phase disagrees with dual phase")
        }
        return ok
    }

    private fun pricingRequest(
        snapshot: LinearModelSnapshot<V>,
        blockId: String,
        phaseOne: Boolean,
        seed: Boolean
    ): Ret<ColumnPricingRequest<V>> {
        val revision = currentRevision(snapshot)
        val dual = if (seed) emptyMap() else prices!!.second
        val coefficients = projectionIndex!!.decisionsByBlock[blockId].orEmpty().associate { decision ->
            var value = if (phaseOne) zero else snapshot.objective[decision.variable] ?: zero
            for (row in snapshot.rows) value -= (dual[row.id] ?: zero) * (row.coefficients[decision.variable] ?: zero)
            decision.id to value
        }
        if (coefficients.values.any { !it.isFinite() }) return Failed(ErrorCode.IllegalArgument, "定价投影系数溢出 / Pricing projection coefficient overflow")
        return Ok(ColumnPricingRequest(
            blockId = blockId,
            coefficients = coefficients,
            convexityPrice = if (seed) zero else convexityPrices.getValue(blockId),
            rowPrices = dual,
            revision = revision
        ))
    }

    /** 从同源行生成当前定价目标；只有种子阶段允许不提供对偶。
     * / Generate pricing from the same source rows; only seeding permits absent duals.
     * @param blockId 配置块 / Configuration block
     * @param phaseOne 是否人工变量阶段 / Artificial-variable phase
     * @param seed 是否零对偶种子定价 / Whether pricing seeds with zero duals
     * @return 带版本定价请求 / Versioned pricing request
     */
    suspend fun pricingRequest(blockId: String, phaseOne: Boolean, seed: Boolean = false): Ret<ColumnPricingRequest<V>> {
        val snapshot = when (val result = snapshot()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (blockId !in blockIds) return Failed(ErrorCode.IllegalArgument, "未知配置块 / Unknown configuration block")
        when (val result = validatePricingState(
            snapshot = snapshot,
            phaseOne = phaseOne,
            seed = seed
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return pricingRequest(
            snapshot = snapshot,
            blockId = blockId,
            phaseOne = phaseOne,
            seed = seed
        )
    }

    /** 一次刷新后为多个块准备同版本请求和局部模型。
     * / Refresh once and prepare same-revision requests and local models for multiple blocks.
     *
     * 此 API 仅合并同步准备阶段的快照工作；定价可能跨越异步求解边界，调用方必须在接收候选前
     * 使用 [requireCurrent] 重新完整检查源模型及主问题。局部模型仍可按块追加扩展约束。
     * This API only shares the synchronous preparation snapshot. Pricing can cross an async solver boundary,
     * so callers must revalidate the source and master with [requireCurrent] before accepting candidates.
     * Local models remain extensible with block-specific constraints.
     *
     * @param blockIds 按所需输出顺序排列的不同配置块 / Distinct blocks in desired output order
     * @param phaseOne 是否人工变量阶段 / Artificial-variable phase
     * @param seed 是否零对偶种子定价 / Whether pricing seeds with zero duals
     * @return 同一快照下按输入顺序生成的定价批次 / Pricing batch in input order from one snapshot
     */
    suspend fun preparePricingBatch(
        blockIds: List<String>,
        phaseOne: Boolean,
        seed: Boolean = false
    ): Ret<ColumnPricingBatch<V>> {
        val snapshot = when (val result = snapshot()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (blockIds.isEmpty() || blockIds.distinct().size != blockIds.size) {
            return Failed(ErrorCode.IllegalArgument, "定价批次必须包含不同配置块 / Pricing batch must contain distinct blocks")
        }
        if (blockIds.any { it !in this.blockIds }) return Failed(ErrorCode.IllegalArgument, "未知配置块 / Unknown configuration block")
        when (val result = validatePricingState(
            snapshot = snapshot,
            phaseOne = phaseOne,
            seed = seed
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val index = projectionIndex!!
        val entries = mutableListOf<ColumnPricingEntry<V>>()
        for (blockId in blockIds) {
            val request = when (val result = pricingRequest(
                snapshot = snapshot,
                blockId = blockId,
                phaseOne = phaseOne,
                seed = seed
            )) {
                is Ok -> result.value
                is Failed -> {
                    entries.forEach { it.model.close() }
                    return Failed(result.error)
                }
                is Fatal -> {
                    entries.forEach { it.model.close() }
                    return Fatal(result.errors)
                }
            }
            val model = when (val result = pricingModel(request, index)) {
                is Ok -> result.value
                is Failed -> {
                    entries.forEach { it.model.close() }
                    return Failed(result.error)
                }
                is Fatal -> {
                    entries.forEach { it.model.close() }
                    return Fatal(result.errors)
                }
            }
            entries.add(ColumnPricingEntry(request = request, model = model))
        }
        return Ok(ColumnPricingBatch(
            owner = this,
            revision = currentRevision(snapshot),
            entries = entries.toList()
        ))
    }

    /** 创建局部线性定价模型；只有完全属于当前块的行作为局部硬约束。
     * / Build local linear pricing; only wholly local rows become local hard constraints.
     * @param request 当前定价请求 / Current pricing request
     * @return 局部模型，供领域继续追加约束 / Local model for additional domain constraints
     */
    suspend fun pricingModel(request: ColumnPricingRequest<V>): Ret<LinearMetaModel<V>> {
        val valid = requireCurrent(request.revision)
        if (valid is Failed) return Failed(valid.error)
        if (valid is Fatal) return Fatal(valid.errors)
        return pricingModel(request, projectionIndex!!)
    }

    private fun pricingModel(request: ColumnPricingRequest<V>, index: ColumnProjectionIndex<V>): Ret<LinearMetaModel<V>> {
        val local = index.decisionsByBlock[request.blockId].orEmpty()
        if (request.blockId !in blockIds || index.idsByBlock[request.blockId] != request.coefficients.keys ||
            request.coefficients.values.any { !it.isFinite() } || !request.convexityPrice.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "定价请求结构无效 / Invalid pricing-request structure")
        }
        val model = LinearMetaModel<V>(name = "column_pricing", converter = source.converter)
        for (decision in local) {
            when (val result = model.add(decision.variable)) {
                is Ok -> {}
                is Failed -> {
                    model.close()
                    return Failed(result.error)
                }
                is Fatal -> {
                    model.close()
                    return Fatal(result.errors)
                }
            }
        }
        for (row in index.localRows.getValue(request.blockId)) {
            val relation = LinearInequality(
                lhs = LinearPolynomial(row.coefficients.map { LinearMonomial(it.value, it.key) }, zero),
                rhs = LinearPolynomial(constant = row.rhs),
                comparison = row.comparison
            )
            when (val result = model.addConstraint(relation = relation, name = row.id)) {
                is Ok -> {}
                is Failed -> {
                    model.close()
                    return Failed(result.error)
                }
                is Fatal -> {
                    model.close()
                    return Fatal(result.errors)
                }
            }
        }
        val objective = LinearPolynomial(local.map { LinearMonomial(request.coefficients.getValue(it.id), it.variable) }, -request.convexityPrice)
        when (val result = model.minimize(objective)) {
            is Ok -> {}
            is Failed -> {
                model.close()
                return Failed(result.error)
            }
            is Fatal -> {
                model.close()
                return Fatal(result.errors)
            }
        }
        return Ok(model)
    }

    /** 按当前注册的列向量和原变量身份合并解；不假设跨模型 token 顺序一致。
     * / Merge registered columns and source-variable identities without cross-model token-order assumptions.
     * @param registration 当前整数主问题 / Current integer master
     * @param values 当前主问题 token 顺序的值 / Values in the current master's token order
     * @param tolerance 整数列选择的数值容差 / Numeric tolerance for integer column selection
     * @return 原始模型自身 token 顺序的完整值；调用方仍需复核原模型 / Complete source-token values; callers still verify the original model
     */
    suspend fun extractSolution(registration: ColumnMasterRegistration<V>, values: List<V>, tolerance: V): Ret<List<V>> {
        val valid = requireCurrent(registration)
        if (valid is Failed) return Failed(valid.error)
        if (valid is Fatal) return Fatal(valid.errors)
        if (!registration.integer || registration.phaseOne || !tolerance.isFinite() || tolerance < zero || tolerance + tolerance >= one) {
            return Failed(ErrorCode.IllegalArgument, "提取需要无人工变量的整数主问题及有效容差 / Extraction requires an integer master without artificials and a valid tolerance")
        }
        if (values.size != registration.model.tokens.tokensInSolver.size || values.any { !it.isFinite() }) {
            return Failed(ErrorCode.IllegalArgument, "主问题解向量不完整或非有限 / Incomplete or nonfinite master solution")
        }
        fun value(variable: AbstractVariableItem<*, *>): V? = registration.model.tokens.indexOf(variable)?.let { values.getOrNull(it) }
        val index = projectionIndex!!
        val mapped = index.mappedVariables
        val full = linkedMapOf<AbstractVariableItem<*, *>, V>()
        for (state in current!!.variables.filter { it.variable !in mapped || it.variable in registration.refinedResources }) {
            full[state.variable] = value(state.variable) ?: return Failed(ErrorCode.DataNotFound, "缺少全局变量值 / Missing global variable value")
        }
        val columnsByBlock = registration.columns.groupBy { it.blockId }
        for (block in blockIds) {
            val selected = arrayListOf<ProjectedColumn<V>>()
            for (column in columnsByBlock[block].orEmpty()) {
                val amount = value(registration.columnVariables.getValue(column.id))
                    ?: return Failed(ErrorCode.DataNotFound, "缺少列选择变量值 / Missing column-selection value")
                when {
                    amount >= one - tolerance && amount <= one + tolerance -> selected.add(column)
                    amount >= -tolerance && amount <= tolerance -> {}
                    else -> return Failed(ErrorCode.IllegalArgument, "列选择值不是整数 / Nonintegral column-selection value")
                }
            }
            if (selected.size != 1) return Failed(ErrorCode.IllegalArgument, "每个配置块必须恰选一列 / Each configuration block must select exactly one column")
            for (decision in index.decisionsByBlock[block].orEmpty()) {
                if (decision.variable in registration.refinedResources) continue
                full[decision.variable] = selected.single().values.getValue(decision.id)
            }
        }
        return Ok(source.tokens.tokensInSolver.map {
            full[it.variable] ?: return Failed(ErrorCode.DataNotFound, "缺少原始模型变量 / Missing source-model variable")
        })
    }
}
