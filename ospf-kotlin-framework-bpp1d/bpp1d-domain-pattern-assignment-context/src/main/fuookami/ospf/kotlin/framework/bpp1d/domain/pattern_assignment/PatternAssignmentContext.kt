package fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_assignment

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.PackingValueAdapter
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*

/** 模型注册阶段。 / Model registration stage. */
enum class PackingModelStage { MILP, LP, FINAL_MILP }

/** 下游模型扩展，增删列后可同步额外表达式与目标。 / Downstream modeling extension with column lifecycle hooks. */
interface PatternModelingExtension<V : FloatingNumber<V>> {
    /** 注册附加模型。 / Register an additional model.
     * @param context 注册上下文 / Registration context
     * @return 操作结果 / Operation result
     */
    fun register(context: PatternAssignmentContext<V>): Try

    /** 同步列变化。 / Synchronize column changes.
     * @param context 注册上下文 / Registration context
     * @return 操作结果 / Operation result
     */
    fun refresh(context: PatternAssignmentContext<V>): Try = ok

    /** 附加定价项。 / Additional reduced-cost contribution.
     * @param pattern 候选 / Candidate
     * @param context 注册上下文 / Registration context
     * @param dual 对偶解 / Dual solution
     * @return 附加检验数 / Additional reduced cost
     */
    fun reducedCost(pattern: PackingPattern<V>, context: PatternAssignmentContext<V>, dual: MetaDualSolution): Flt64 = Flt64.zero

    /** 提取扩展结果，普通及最终 MILP 共用。 / Extract extension results for ordinary and final MILP.
     * @param context 注册上下文 / Registration context
     * @param values 模型自身 token 顺序的结果 / Values in model-local token order
     * @return 领域附加指标 / Additional domain indicators
     */
    fun extractSolution(context: PatternAssignmentContext<V>, values: List<Flt64>): Ret<Map<String, String>> = Ok(emptyMap())
}

/** 方案变量及共享中间符号聚合。 / Pattern variables and shared intermediate symbols. */
class PatternAggregation {
    val variables = linkedMapOf<String, UIntVar>()
    val demands = linkedMapOf<String, LinearExpressionSymbol<Flt64>>()
    val inventories = linkedMapOf<String, LinearExpressionSymbol<Flt64>>()
    val objective = LinearExpressionSymbol(Flt64.zero, name = "packing_objective")
}

/** 统一的普通、LP 和最终 MILP 注册路径。 / Unified ordinary, LP and final-MILP registration path.
 * @property problem 输入 / Input
 * @property adapter 数值边界 / Numeric boundary
 * @property model 元模型 / Meta model
 * @property stage 注册阶段 / Registration stage
 * @property extensions 下游扩展 / Downstream extensions
 * @property aggregation 注册构件 / Registration artifacts
 */
class PatternAssignmentContext<V : FloatingNumber<V>>(
    val problem: PackingProblem<V>,
    val adapter: PackingValueAdapter<V>,
    val model: LinearMetaModel<Flt64>,
    val stage: PackingModelStage,
    val extensions: List<PatternModelingExtension<V>> = emptyList()
) {
    val aggregation = PatternAggregation()
    private val pool = linkedMapOf<String, PackingPattern<V>>()
    val patterns: List<PackingPattern<V>> get() = pool.values.toList()
    private var columnSequence = 0
    private var shadowPrice = MetaDualSolution(emptyMap(), emptyMap())
    private var registered = false
    private var extensionsRegistered = false

    /** 注册初始模型。 / Register the initial model.
     * @param initialPatterns 初始列 / Initial columns
     * @return 操作结果 / Operation result
     */
    fun register(initialPatterns: List<PackingPattern<V>>): Try {
        if (registered) return Failed(ErrorCode.IllegalArgument, "方案上下文已经注册 / Pattern context already registered")
        val validation = problem.validate()
        if (validation !is Ok) return validation
        for (item in problem.items) {
            val symbol = LinearExpressionSymbol(Flt64.zero, name = "demand_${item.id}")
            aggregation.demands[item.id] = symbol
            var result = model.add(symbol)
            if (result !is Ok) return result
            result = model.addConstraint(
                relation = LinearPolynomial(symbol) eq item.amount.toFlt64(),
                name = "demand_${item.id}",
                group = null,
                args = "demand:${item.id}"
            )
            if (result !is Ok) return result
        }
        for (bin in problem.binTypes) {
            val symbol = LinearExpressionSymbol(Flt64.zero, name = "inventory_${bin.id}")
            aggregation.inventories[bin.id] = symbol
            var result = model.add(symbol)
            if (result !is Ok) return result
            if (bin.inventory != null) {
                result = model.addConstraint(
                    relation = LinearPolynomial(symbol) leq bin.inventory!!.toFlt64(),
                    name = "inventory_${bin.id}",
                    group = null,
                    args = "inventory:${bin.id}"
                )
                if (result !is Ok) return result
            }
        }
        var result = model.add(aggregation.objective)
        if (result !is Ok) return result
        result = model.minimize(LinearPolynomial(aggregation.objective), name = "packing")
        if (result !is Ok) return result
        registered = true
        result = addColumns(initialPatterns)
        if (result !is Ok) return result
        for (extension in extensions) {
            result = extension.register(this)
            if (result !is Ok) return result
        }
        extensionsRegistered = true
        return ok
    }

    /** 追加列并刷新所有中间符号。 / Add columns and refresh all intermediate symbols.
     * @param columns 新列 / New columns
     * @return 操作结果 / Operation result
     */
    fun addColumns(columns: List<PackingPattern<V>>): Try {
        if (!registered) return Failed(ErrorCode.IllegalArgument, "方案上下文未注册 / Pattern context not registered")
        for (pattern in columns) {
            if (pattern.key in pool) continue
            val validation = problem.validatePattern(pattern)
            if (validation !is Ok) return validation
            if (pattern.amounts.isEmpty() || pattern.amounts.any { (id, amount) -> amount == UInt64.zero || problem.items.none { it.id == id && amount <= it.amount } } || problem.binTypes.none { it.id == pattern.binTypeId }) {
                return Failed(ErrorCode.IllegalArgument, "方案引用或件数无效 / Invalid pattern references or counts")
            }
            val variable = UIntVar("pattern_${columnSequence++}")
            val upper = pattern.amounts.minOf { (id, amount) -> problem.items.first { it.id == id }.amount / amount }
            variable.range.leq(upper)
            val result = model.add(variable)
            if (result !is Ok) return result
            // 目标注册会展开符号，必须按新列追加目标项。 / Objective registration expands symbols, so append each new column explicitly.
            val objectiveResult = model.minimize(
                polynomial = LinearPolynomial(cost(pattern) * variable),
                name = "packing_${variable.name}"
            )
            if (objectiveResult !is Ok) return objectiveResult
            pool[pattern.key] = pattern
            aggregation.variables[pattern.key] = variable
        }
        return refresh()
    }

    /** 删除列，同时移除 token 并刷新引用。 / Remove columns, tokens and expression references.
     * @param keys 列键 / Column keys
     * @return 操作结果 / Operation result
     */
    fun removeColumns(keys: Set<String>): Try {
        if (!registered) return Failed(ErrorCode.IllegalArgument, "方案上下文未注册 / Pattern context not registered")
        val variables = keys.mapNotNull { aggregation.variables.remove(it) }
        keys.forEach { pool.remove(it) }
        val result = refresh()
        if (result !is Ok) return result
        val objectiveResult = model.removeObjectiveTerms(variables.toSet())
        if (objectiveResult !is Ok) return objectiveResult
        variables.forEach { model.remove(it) }
        return ok
    }

    private fun refresh(): Try {
        fun replace(symbol: LinearExpressionSymbol<Flt64>, polynomial: LinearPolynomial<Flt64>) {
            symbol.flush()
            symbol.asMutable().clear()
            symbol.asMutable() += polynomial
        }
        for ((id, symbol) in aggregation.demands) {
            var polynomial = LinearPolynomial()
            for (pattern in patterns) polynomial += LinearPolynomial((pattern.amounts[id] ?: UInt64.zero).toFlt64() * aggregation.variables.getValue(pattern.key))
            replace(symbol, polynomial)
        }
        for ((id, symbol) in aggregation.inventories) {
            replace(symbol, sum(patterns.filter { it.binTypeId == id }.map { aggregation.variables.getValue(it.key) }))
        }
        var objective = LinearPolynomial()
        for (pattern in patterns) objective += LinearPolynomial(cost(pattern) * aggregation.variables.getValue(pattern.key))
        replace(aggregation.objective, objective)
        for (extension in extensions.filter { extensionsRegistered }) {
            val result = extension.refresh(this)
            if (result !is Ok) return result
        }
        return ok
    }

    /** 目标系数，按显式口径归一。 / Objective coefficient in an explicit normalized metric.
     * @param pattern 候选 / Candidate
     * @return 系数 / Coefficient
     */
    fun cost(pattern: PackingPattern<V>): Flt64 {
        val bin = problem.binTypes.first { it.id == pattern.binTypeId }
        return when (problem.objective) {
            PackingObjective.BinCount -> Flt64.one
            PackingObjective.ConsumedLength -> adapter.toSolver(bin.length.convertTo(Meter)!!.value)
            PackingObjective.Cost -> adapter.toSolver(bin.cost)
        }
    }

    /** 刷新约束身份对应的对偶价格。 / Refresh dual prices by constraint identity.
     * @param dual 当前 LP 对偶解 / Current LP dual solution
     * @return 刷新结果 / Refresh result
     */
    fun refreshShadowPrice(dual: MetaDualSolution): Try {
        if (dual.constraints.values.any { !it.isFinite() } || dual.symbols.values.flatten().any { !it.second.isFinite() }) return Failed(ErrorCode.IllegalArgument, "影子价格必须有限 / Shadow prices must be finite")
        shadowPrice = dual
        return ok
    }

    /** 使用最新影子价格定价。 / Price using the latest shadow prices.
     * @param pattern 候选 / Candidate
     * @return 检验数 / Reduced cost
     */
    fun reducedCost(pattern: PackingPattern<V>): Flt64 = reducedCost(pattern, shadowPrice)

    /** 按约束身份刷新影子价格并计算完整检验数。 / Compute reduced cost using constraint-identity dual prices.
     * @param pattern 候选 / Candidate
     * @param dual 元对偶解 / Meta dual solution
     * @return 检验数 / Reduced cost
     */
    fun reducedCost(pattern: PackingPattern<V>, dual: MetaDualSolution): Flt64 {
        var result = cost(pattern)
        for ((constraint, price) in dual.constraints) {
            val key = constraint.args as? String ?: continue
            if (key.startsWith("demand:")) result -= (pattern.amounts[key.removePrefix("demand:")] ?: UInt64.zero).toFlt64() * price
            if (key == "inventory:${pattern.binTypeId}") result -= price
        }
        for (extension in extensions) result += extension.reducedCost(pattern, this, dual)
        return result
    }

    /** 提取整数列份数。 / Extract integer pattern usages.
     * @param values 模型自身 token 顺序的解 / Solution in model-local token order
     * @return 列份数或失败 / Usages or failure
     */
    fun extractSolution(values: List<Flt64>): Ret<Map<PackingPattern<V>, UInt64>> {
        val usages = linkedMapOf<PackingPattern<V>, UInt64>()
        for (pattern in patterns) {
            val index = model.tokens.indexOf(aggregation.variables.getValue(pattern.key))
                ?: return Failed(ErrorCode.DataNotFound, "缺少方案 token / Missing pattern token")
            val value = values.getOrNull(index) ?: return Failed(ErrorCode.DataNotFound, "缺少方案值 / Missing pattern value")
            val rounded = kotlin.math.round(value.toDouble())
            if (!rounded.isFinite() || rounded < 0 || kotlin.math.abs(value.toDouble() - rounded) > 1e-6 || rounded > Long.MAX_VALUE.toDouble()) return Failed(ErrorCode.ApplicationError, "非法方案份数 / Invalid pattern usage")
            if (rounded > 0) usages[pattern] = UInt64(rounded.toULong())
        }
        return Ok(usages)
    }
}
