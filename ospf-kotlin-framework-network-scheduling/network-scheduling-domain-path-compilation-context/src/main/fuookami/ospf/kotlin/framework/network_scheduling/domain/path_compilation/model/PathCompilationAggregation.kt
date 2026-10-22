package fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.service.*

/** 当前模型/修订/业务范围的对偶快照，不是最优性证书。 / Dual snapshot bound to model/revision/scope, not an optimality certificate.
 * @property scope 调用方提供的定价范围版本（含分支及固定状态） / Caller's pricing-scope version including branch/fixing state
 * @property prices 按规范行单位的有符号对偶 / Signed duals in normalized row units
 */
class PathCompilationDuals internal constructor(
    internal val owner: Any,
    internal val revision: Long,
    val scope: String,
    prices: Map<String, Flt64>
) {
    val prices = prices.toMap()
}

/** 通用路径主问题的独立注册态。失败后禁止继续使用部分装配模型。 / Independent master registration state; partial failures poison the context.
 * @property problem 不可变问题 / Immutable problem
 * @property adapter solver 边界 / Solver boundary
 * @property name 命名空间 / Namespace
 * @property compilation 变量及行组件 / Variable and row component
 */
class PathCompilationAggregation<V : RealNumber<V>, C>(
    val problem: PathCompilationProblem<V, C>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String
) {
    val compilation = PathCompilation(problem.rows.map { it.id }, name)
    private val columns = linkedMapOf<String, CompiledPathColumn<V, C>>()
    private val pipelines = mutableListOf<PathCompilationRows>()
    private var model: LinearMetaModel<Flt64>? = null
    private var attempted = false
    private var usable = false
    private var revision = 0L
    private var serial = 0L
    private var currentDuals: PathCompilationDuals? = null
    private var dualModelSignature: List<Any?> = emptyList()

    private fun validateObjective(target: LinearMetaModel<Flt64>): Try {
        if (target.objectCategory != ObjectCategory.Minimum ||
            (target.hasFlattenedObjectives && target.subObjects.isNotEmpty())) {
            return networkSchedulingFailure("路径主问题需要可审计的最小化目标 / Path master requires an auditable minimization objective")
        }
        val objectives = target.objectiveSnapshot()
        for ((id, compiled) in columns) {
            val variable = compilation.variables.getValue(id)
            var coefficient = Flt64.zero
            for (objective in objectives) {
                val sign = if (objective.category == ObjectCategory.Minimum) Flt64.one else -Flt64.one
                for ((amount, symbol) in objective.linearTerms()) {
                    if (symbol == variable) coefficient += sign * amount
                }
            }
            if (!coefficient.isFinite() || coefficient != compiled.cost) {
                return networkSchedulingFailure("路径目标与完整列费用不一致：$id / Path objective differs from complete-column cost: $id")
            }
        }
        return ok
    }

    private fun normalize(quantity: Quantity<V>, unit: PhysicalUnit): Ret<Flt64> {
        if (quantity.unit.isAffine || unit.isAffine) {
            return networkSchedulingFailure("线性路径模型不支持仿射单位 / Linear path models do not support affine units")
        }
        return when (val result = adapter.normalize(quantity, unit)) {
            is Ok -> if (result.value.isFinite()) {
                result
            } else {
                networkSchedulingFailure("规范化数值必须有限 / Normalized values must be finite")
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun modelSignature(): List<Any?> {
        val target = model ?: return emptyList()
        return listOf(target.objectCategory) + target.constraints.map { it.toString() } +
            target.subObjects.map { Triple(it.category, it.name, it.polynomial.toString()) } +
            target.objectiveSnapshot().map { objective ->
                Triple(objective.category, objective.constant, objective.linearTerms().map { (amount, variable) -> variable.key to amount })
            } +
            target.tokens.tokensInSolver.map { token ->
                // Bound 转换会创建新对象，快照比较数值及开闭性。 / Bound conversion allocates; compare values and inclusivity.
                Triple(
                    first = token.variable.key,
                    second = token.variable.lowerBound?.let { it.value.toString() to it.interval },
                    third = token.variable.upperBound?.let { it.value.toString() to it.interval }
                )
            }
    }

    /** 在注册边界归一化完整列，费用与定价使用相同函数。 / Normalize a complete column for both registration and pricing.
     * @param column 业务候选 / Business candidate
     * @return 规范化列 / Normalized column
     */
    fun compile(column: PathColumn<V, C>): Ret<CompiledPathColumn<V, C>> {
        if (column.id.isBlank() || column.coefficients.keys.any { key -> problem.rows.none { it.id == key } }) {
            return networkSchedulingFailure("列标识或行引用无效 / Invalid column identity or row reference")
        }
        val cost = when (val result = normalize(column.cost, problem.costUnit)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val coefficients = linkedMapOf<String, Flt64>()
        for (row in problem.rows) {
            coefficients[row.id] = column.coefficients[row.id]?.let { quantity ->
                when (val result = normalize(quantity, row.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } ?: Flt64.zero
        }
        return ok(CompiledPathColumn(column, cost, coefficients))
    }

    /** 注册变量、标准行管线和完整列目标。 / Register variables, standard-row pipelines and full-column objectives.
     * @param target 当前模型 / Current model
     * @return 注册结果 / Registration result
     */
    fun register(target: LinearMetaModel<Flt64>): Try {
        if (usable && model === target) return ok
        if (attempted) return networkSchedulingFailure("路径上下文不可跨模型或失败后复用 / Path context cannot be reused across models or after failure")
        attempted = true
        if (target.objectCategory != ObjectCategory.Minimum) {
            return networkSchedulingFailure("路径费用主问题必须最小化 / Path-cost master must minimize")
        }
        if (problem.costUnit.isAffine || problem.rows.any { it.unit.isAffine }) {
            return networkSchedulingFailure("线性路径模型不支持仿射单位 / Linear path models do not support affine units")
        }
        if (problem.rows.any { it.id.isBlank() || (it.lower == null && it.upper == null) } ||
            problem.rows.map { it.id }.toSet().size != problem.rows.size) {
            return networkSchedulingFailure("路径行重复、无标识或无边界 / Duplicate, unnamed or unbounded path rows")
        }
        for (row in problem.rows) {
            val lower = row.lower?.let {
                when (val result = normalize(it, row.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            val upper = row.upper?.let {
                when (val result = normalize(it, row.unit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            if (lower != null && upper != null && lower > upper) {
                return networkSchedulingFailure("路径行单位或边界无效：${row.id} / Invalid path row unit or bounds: ${row.id}")
            }
            pipelines.add(PathCompilationRows(
                id = row.id,
                symbol = compilation.rows.getValue(row.id),
                lower = lower,
                upper = upper
            ))
        }
        model = target
        for (symbol in compilation.rows.values) {
            when (val result = target.add(symbol)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        usable = true
        when (val result = addColumns(problem.columns, target)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        usable = false
        for (pipeline in pipelines) {
            when (val result = pipeline(target)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        usable = true
        return ok
    }

    /** 增量添加完整列；同标识同规范系数去重，载荷或系数冲突失败。 / Add complete columns; deduplicate identical IDs and reject conflicts.
     * @param candidates 新候选 / New candidates
     * @param target 当前模型 / Current model
     * @return 实际新增候选 / Actually added candidates
     */
    fun addColumns(candidates: List<PathColumn<V, C>>, target: LinearMetaModel<Flt64>): Ret<List<PathColumn<V, C>>> {
        if (!usable || model !== target) return networkSchedulingFailure("路径上下文未注册或已失效 / Path context is unregistered or invalid")
        val additions = linkedMapOf<String, CompiledPathColumn<V, C>>()
        for (candidate in candidates) {
            val compiled = when (val result = compile(candidate)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val existing = columns[candidate.id] ?: additions[candidate.id]
            if (existing != null && (existing.cost != compiled.cost || existing.coefficients != compiled.coefficients ||
                    existing.column.payload != candidate.payload)) {
                return networkSchedulingFailure("重复路径标识冲突：${candidate.id} / Conflicting path identity: ${candidate.id}")
            }
            if (existing == null) additions[candidate.id] = compiled
        }
        if (additions.isEmpty()) return ok(emptyList())
        usable = false
        currentDuals = null
        revision += 1
        for ((id, compiled) in additions) {
            val variable = compilation.add(
                id = id,
                coefficients = compiled.coefficients,
                name = "${name}_column_${serial++}"
            )
            when (val result = target.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = target.minimize(
                polynomial = compiled.cost * LinearPolynomial(variable),
                name = "${variable.name}_cost"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            columns[id] = compiled
        }
        target.flush()
        usable = true
        return ok(additions.values.map { it.column })
    }

    /** 删除当前列并使所有旧对偶失效。 / Remove current columns and invalidate all old duals.
     * @param ids 待删除列标识 / Column IDs to remove
     * @param target 当前模型 / Current model
     * @return 删除结果 / Removal result
     */
    fun removeColumns(ids: Set<String>, target: LinearMetaModel<Flt64>): Try {
        if (!usable || model !== target) return networkSchedulingFailure("路径上下文未注册或已失效 / Path context is unregistered or invalid")
        val existing = ids.intersect(columns.keys)
        if (existing.isEmpty()) return ok
        usable = false
        currentDuals = null
        revision += 1
        val variables = existing.map { compilation.variables.getValue(it) }.toSet()
        when (val result = target.removeObjectiveTerms(variables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        for (id in existing) {
            target.remove(compilation.variables.getValue(id))
            compilation.remove(id)
            columns.remove(id)
        }
        target.flush()
        usable = true
        return ok
    }

    /** 提取当前 LP 的完整行对偶；调用方必须传入本轮求解结果，外部未映射行拒绝定价。
     * / Extract all current LP row duals; callers must supply this solve's duals, and unmapped external rows are rejected.
     * @param target 当前模型 / Current model
     * @param duals 当前 LP 对偶 / Current LP dual solution
     * @param scope 分支、固定及有效图等定价范围版本 / Pricing-scope version including branches, fixings and effective graph
     * @return 本轮快照，不构成全局证明 / Current snapshot, not a global proof
     */
    fun refreshShadowPrices(
        target: LinearMetaModel<Flt64>,
        duals: MetaDualSolution,
        scope: String
    ): Ret<PathCompilationDuals> {
        currentDuals = null
        if (!usable || model !== target || scope.isBlank() ||
            target.constraints.toSet() != pipelines.flatMap { it.constraints }.toSet()) {
            return networkSchedulingFailure("模型或定价范围不完整：额外行必须声明在问题中 / Incomplete model or pricing scope: declare extra rows in the problem")
        }
        when (val result = validateObjective(target)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val prices = linkedMapOf<String, Flt64>()
        for (pipeline in pipelines) {
            var price = Flt64.zero
            for (constraint in pipeline.constraints) {
                val value = duals.constraints[constraint]
                    ?: return networkSchedulingFailure("缺少当前行对偶：${pipeline.id} / Missing current row dual: ${pipeline.id}")
                if (!value.isFinite()) return networkSchedulingFailure("对偶必须有限 / Duals must be finite")
                price += value
                if (!price.isFinite()) return networkSchedulingFailure("聚合对偶必须有限 / Aggregated duals must be finite")
            }
            prices[pipeline.id] = price
        }
        val snapshot = PathCompilationDuals(this, revision, scope, prices)
        currentDuals = snapshot
        dualModelSignature = modelSignature()
        return ok(snapshot)
    }

    /** 以注册时同一完整列定义计算 reduced cost，不忽略缺失行。 / Compute reduced cost from the exact registered column definition.
     * @param candidate 完整业务候选 / Complete business candidate
     * @param duals 当前对偶快照 / Current dual snapshot
     * @param scope 当前业务定价范围 / Current business pricing scope
     * @return solver 边界的 reduced cost / Reduced cost at the solver boundary
     */
    fun reducedCost(
        candidate: PathColumn<V, C>,
        duals: PathCompilationDuals,
        scope: String
    ): Ret<Flt64> {
        if (!usable || duals !== currentDuals || duals.owner !== this || duals.revision != revision ||
            duals.scope != scope || dualModelSignature != modelSignature()) {
            return networkSchedulingFailure("对偶已过期或定价范围不匹配 / Stale duals or mismatched pricing scope")
        }
        val target = model ?: return networkSchedulingFailure("路径模型不存在 / Path model is absent")
        when (val result = validateObjective(target)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val column = when (val result = compile(candidate)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        var value = column.cost
        for ((id, coefficient) in column.coefficients) {
            value -= coefficient * duals.prices.getValue(id)
            if (!value.isFinite()) return networkSchedulingFailure("reduced cost 必须有限 / Reduced costs must be finite")
        }
        return ok(value)
    }

    /** 提取并独立复核二元解，结果只对最终候选集合有效。 / Extract and independently validate a binary solution for the final pool only.
     * @param target 已求解模型 / Solved model
     * @return 选中的路径与完整用量 / Selected paths and complete usage
     */
    fun extractSolution(target: LinearMetaModel<Flt64>): Ret<PathCompilationSolution<V, C>> {
        if (!usable || model !== target) return networkSchedulingFailure("路径上下文未注册或已失效 / Path context is unregistered or invalid")
        when (val result = validateObjective(target)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val selected = mutableListOf<PathColumn<V, C>>()
        val usage = problem.rows.associate { it.id to Flt64.zero }.toMutableMap()
        var cost = Flt64.zero
        for ((id, compiled) in columns) {
            val variable = compilation.variables.getValue(id)
            val value = target.tokens.tokens.firstOrNull { it.belongsTo(variable) }?.result
                ?: return networkSchedulingFailure("缺少路径列解值：$id / Missing path-column value: $id")
            if (!value.isFinite() || (value.abs() > Flt64(1e-6) && (value - Flt64.one).abs() > Flt64(1e-6))) {
                return networkSchedulingFailure("路径列解不是二元值 / Path-column solution is not binary")
            }
            if (value > Flt64(0.5)) {
                selected.add(compiled.column)
                cost += compiled.cost
                compiled.coefficients.forEach { (row, amount) -> usage[row] = usage.getValue(row) + amount }
            }
        }
        if (!cost.isFinite() || usage.values.any { !it.isFinite() }) {
            return networkSchedulingFailure("路径解累计值必须有限 / Accumulated path-solution values must be finite")
        }
        for (pipeline in pipelines) {
            val amount = usage.getValue(pipeline.id)
            if (pipeline.lower?.let { amount < it - Flt64(1e-6) } == true ||
                pipeline.upper?.let { amount > it + Flt64(1e-6) } == true) {
                return networkSchedulingFailure("路径解违反行：${pipeline.id} / Path solution violates row: ${pipeline.id}")
            }
        }
        val amounts = linkedMapOf<String, Quantity<V>>()
        for (row in problem.rows) {
            val value = when (val result = adapter.fromSolverValue(usage.getValue(row.id))) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            amounts[row.id] = Quantity(value, row.unit)
        }
        return adapter.fromSolverValue(cost).map {
            PathCompilationSolution(
                selected = selected.toList(),
                cost = Quantity(it, problem.costUnit),
                usage = amounts,
                candidateIds = columns.keys.toSet()
            )
        }
    }
}
