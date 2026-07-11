package fuookami.ospf.kotlin.framework.csp1d.domain.produce.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.model.Pipeline

/**
 * CSP1D 模型上下文接口 / CSP1D model context interface
 *
 * 定义模型注册的基本接口。domain context 实现此接口将建模逻辑注入 MetaModel，
 * 使变量、约束和目标的注册从 solver 硬编码中解耦。
 *
 * Define the basic interface for model registration. Domain contexts implement this interface
 * to inject modeling logic into MetaModel, decoupling variable/constraint/objective registration
 * from solver hard-coding.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dModelContext<V : RealNumber<V>> {

    /**
     * 注册到元模型 / Register to meta model
     *
     * 将变量、中间值、约束和目标注册到指定的元模型中。
     * Register variables, intermediate values, constraints, and objectives to the specified meta model.
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
    */
    fun register(model: LinearMetaModel<Flt64>): Try

    /**
     * 从元模型提取求解结果 / Extract solution from meta model
     *
     * @param model 元模型 / Meta model
     * @return 主问题产出 / Master problem output
    */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<Produce<V>>
}

/**
 * CSP1D 列生成上下文接口 / CSP1D column generation context interface
 *
 * 扩展 ModelContext，支持列生成所需的迭代操作：添加列、提取影子价格、移除列。
 * Extends ModelContext with iterative operations for column generation:
 * addColumns, extractShadowPrice, removeColumns.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dIterativeContext<V : RealNumber<V>> : Csp1dModelContext<V> {

    /**
     * 添加列（新切割方案） / Add columns (new cutting plans)
     *
     * 在列生成迭代过程中，将新生成的切割方案注册为新的列变量，
     * 并更新相关中间值和约束表达式。
     *
     * During column generation iteration, register newly generated cutting plans
     * as new column variables, and update related intermediate values and constraint expressions.
     *
     * @param iteration 当前迭代编号 / Current iteration number
     * @param newPlans 新切割方案列表 / New cutting plan list
     * @param model 元模型 / Meta model
     * @return 去重后的新方案列表 / Deduplicated new plan list
    */
    suspend fun addColumns(
        iteration: UInt64,
        newPlans: List<CuttingPlan<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<CuttingPlan<V>>>

    /**
     * 提取影子价格 / Extract shadow prices
     *
     * 从 LP 松弛的对偶解中提取各约束的影子价格，
     * 用于定价子问题计算 reduced cost。
     * 通过 CGPipeline refresh / extractor 机制自动提取。
     *
     * Extract shadow prices of each constraint from the LP relaxation dual solution,
     * for use in the pricing sub-problem to compute reduced cost.
     * Extraction is automatic via CGPipeline refresh / extractor mechanism.
     *
     * @param model 元模型 / Meta model
     * @param shadowPrices 对偶解 / Dual solution
     * @return 操作结果 / Operation result
    */
    fun extractShadowPrice(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try
}

/**
 * CSP1D 扩展模型上下文接口 / CSP1D extra model context interface
 *
 * 组合基础上下文，允许下游项目在不修改核心代码的情况下注入额外建模逻辑。
 * 下游可通过此接口注册类似 same unit length、same width、宽差、材质兼容等业务约束。
 *
 * Composes base context, allowing downstream projects to inject additional modeling logic
 * without modifying core code. Downstream can register business constraints such as
 * same unit length, same width, width difference, material compatibility via this interface.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dExtraModelContext<V : RealNumber<V>> : Csp1dModelContext<V> {

    /**
     * 基础上下文 / Base context
    */
    val baseContext: Csp1dModelContext<V>
}

/**
 * CSP1D 扩展列生成上下文接口 / CSP1D extra iterative context interface
 *
 * 扩展 IterativeContext，允许下游项目在列生成迭代中注入额外逻辑。
 * Extends IterativeContext, allowing downstream projects to inject additional logic
 * during column generation iteration.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dExtraIterativeContext<V : RealNumber<V>> : Csp1dIterativeContext<V>, Csp1dExtraModelContext<V> {
    override val baseContext: Csp1dIterativeContext<V>
}

/**
 * CSP1D 建模模式 / CSP1D modeling mode
 *
 * 区分普通 MILP 和列生成 LP 松弛两种建模路径。
 * LP 模式不加 yield/length slack 变量，保持 demand >= 约束以提取 shadow price。
 *
 * Distinguish between normal MILP and column generation LP relaxation modeling paths.
 * LP mode does not add yield/length slack variables, keeping demand >= constraints
 * for shadow price extraction.
*/
enum class Csp1dModelingMode {
    /** 普通 MILP 模式 / Normal MILP mode */
    MILP,
    /** 列生成 LP 松弛模式 / Column generation LP relaxation mode */
    LP
}

/**
 * CSP1D 扩展适用模式 / CSP1D extension applicable mode
 *
 * 扩展管线可注册到哪些求解阶段。默认 ALL 表示所有阶段均生效。
 * Extension pipelines can be registered to specific solve stages.
 * Default ALL means the extension applies to all stages.
*/
enum class Csp1dExtensionMode {
    /** 普通 MILP / Normal MILP */
    MILP,
    /** 列生成 LP 松弛 / Column generation LP relaxation */
    LP,
    /** 列生成最终 MILP / Column generation final MILP */
    FINAL_MILP,
    /** 所有模式 / All modes */
    ALL;

    /**
     * 判断是否匹配指定建模模式 / Check if this extension mode matches the given modeling mode
     *
     * @param mode 建模模式 / Modeling mode
     * @param isFinalMilp 是否为列生成最终 MILP / Whether this is a column generation final MILP
     * @return true 表示匹配 / true if matches
    */
    fun matches(mode: Csp1dModelingMode, isFinalMilp: Boolean = false): Boolean {
        return when (this) {
            MILP -> mode == Csp1dModelingMode.MILP && !isFinalMilp
            LP -> mode == Csp1dModelingMode.LP
            FINAL_MILP -> isFinalMilp
            ALL -> true
        }
    }
}

/**
 * CSP1D 建模扩展 / CSP1D modeling extension
 *
 * 承载可在求解各阶段注入的额外管线。下游通过此类型注册 same unit length、
 * same width、宽差、材质兼容等业务约束，而不修改 framework 核心代码。
 *
 * Carries additional pipelines that can be injected at various solve stages.
 * Downstream uses this type to register business constraints such as
 * same unit length, same width, width difference, material compatibility,
 * without modifying framework core code.
 *
 * @param V 数值类型 / Numeric value type
 * @property pipeline 扩展管线，当 contextAwarePipeline 存在且 resolvePipeline 收到 context 时被忽略 / Extension pipeline, ignored when contextAwarePipeline is present and resolvePipeline receives a context
 * @property mode 扩展适用模式 / Extension applicable mode
 * @property contextAwarePipeline 上下文感知扩展管线工厂，接收 Csp1dModelingContext 返回 Pipeline；优先于 pipeline 使用 / Context-aware pipeline factory, receives Csp1dModelingContext and returns Pipeline; takes priority over pipeline when present
*/
data class Csp1dModelingExtension<V : RealNumber<V>>(
    val pipeline: Pipeline<LinearMetaModel<Flt64>>? = null,
    val mode: Csp1dExtensionMode = Csp1dExtensionMode.ALL,
    val contextAwarePipeline: ((Csp1dModelingContext<V>) -> Pipeline<LinearMetaModel<Flt64>>)? = null
) {
    init {
        require(pipeline != null || contextAwarePipeline != null) {
            "Csp1dModelingExtension must have either pipeline or contextAwarePipeline"
        }
    }

    /**
     * 解析实际使用的管线 / Resolve the actual pipeline to use
     *
     * 如果有 contextAwarePipeline 且提供了 context，则用它生成管线；
     * 否则回退到静态 pipeline。
     *
     * If contextAwarePipeline is present and context is provided, use it to generate the pipeline;
     * otherwise fall back to the static pipeline.
     *
     * @param context 建模上下文 / Modeling context
     * @return 解析后的管线 / Resolved pipeline
    */
    fun resolvePipeline(context: Csp1dModelingContext<V>? = null): Pipeline<LinearMetaModel<Flt64>> {
        return if (contextAwarePipeline != null && context != null) {
            contextAwarePipeline(context)
        } else {
            pipeline!!
        }
    }
}

/**
 * CSP1D 建模上下文 / CSP1D modeling context
 *
 * 提供建模管线注册所需的完整领域信息。
 * 下游扩展管线可通过此接口访问产品/物料/设备/方案/聚合根等数据，
 * 无需闭包捕获。
 *
 * Provides complete domain information for modeling pipeline registration.
 * Downstream extension pipelines can access product/material/machine/plan/aggregation
 * data through this interface without closure capture.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dModelingContext<V : RealNumber<V>> {

    /** 建模模式（MILP 或 LP 松弛）/ Modeling mode (MILP or LP relaxation) */
    val mode: Csp1dModelingMode

    /** 是否为列生成最终 MILP 阶段 / Whether this is column generation final MILP stage */
    val isFinalMilp: Boolean

    /** 产出聚合根，包含切割方案使用量变量 x[i] / Produce aggregation with plan usage variables */
    val produce: ProduceAggregation<V>

    /** 需求列表 / Demand list */
    val demands: List<ProductDemand<V>>

    /** 物料列表 / Material list */
    val materials: List<Material<V>>

    /** 设备列表 / Machine list */
    val machines: List<Machine<V>>

    /** 切割方案列表（引用 produce.cuttingPlans）/ Cutting plan list */
    val cuttingPlans: List<CuttingPlan<V>> get() = produce.cuttingPlans

    /** 领域数值样本，用于 solver 值显式转换 / Domain value sample for explicit solver value conversion */
    val domainValueSample: V

    /**
     * 转换为领域数值 / Convert to domain value
     *
     * @param value 浮点数值 / Floating point value
     * @return 领域数值 / Domain value
    */
    fun toDomainValue(value: Flt64): V
}

/**
 * CSP1D 增量扩展管线 / CSP1D incremental extension pipeline
 *
 * 普通 Pipeline 只描述首次注册。若扩展约束或目标需要在列生成新增列时同步刷新，
 * 应实现此接口，让 Csp1dProduceContext.addColumns 在同一个主模型上调用。
 *
 * Plain Pipeline only describes initial registration. When an extension constraint or
 * objective must be refreshed for newly generated columns, implement this interface so
 * Csp1dProduceContext.addColumns can invoke it on the same master model.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dIncrementalPipeline<V : RealNumber<V>> : Pipeline<LinearMetaModel<Flt64>> {

    /**
     * 新增列后的增量刷新 / Incremental refresh after adding columns
     *
     * @param context 建模上下文 / Modeling context
     * @param iteration 当前迭代号 / Current iteration number
     * @param newPlans 已完成去重并注册的新增方案 / Newly added plans after deduplication and registration
     * @param model 元模型 / Meta model
     * @return 增量刷新结果 / Incremental refresh result
    */
    suspend fun addColumns(
        context: Csp1dModelingContext<V>,
        iteration: UInt64,
        newPlans: List<CuttingPlan<V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<CuttingPlan<V>>> {
        return Ok(newPlans)
    }
}

/**
 * CSP1D 方案判断上下文 / CSP1D plan judgment context
 *
 * 用于方案级约束判断（如 same unit length、same width）。
 * 提供同物料/同设备的方案索引集合，支持跨方案约束注册。
 *
 * Used for plan-level constraint judgment (e.g. same unit length, same width).
 * Provides same-material/same-machine plan index sets for cross-plan constraint registration.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dPlanJudgmentContext<V : RealNumber<V>> : Csp1dDomainCalculationContext<V> {

    /** 同物料的所有方案索引 / All plan indices for same material */
    val sameMaterialPlanIndices: List<Int>

    /** 同设备的所有方案索引 / All plan indices for same machine */
    val sameMachinePlanIndices: List<Int>

    /** 所有切割方案 / All cutting plans */
    val allPlans: List<CuttingPlan<V>>
}

// ===== 统一扩展包 =====

/**
 * CSP1D 目标策略接口 / CSP1D objective policy interface
 *
 * 允许下游注入额外目标项或修正基础目标系数。
 * 默认实现不修改任何目标。
 *
 * Allows downstream to inject additional objective terms or modify
 * base objective coefficients. Default implementation does not modify any objective.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dObjectivePolicy<V : RealNumber<V>> {

    /** 策略名称 / Policy name */
    val name: String

    /**
     * 修正基础 batch coefficient /
     * Modify base batch coefficient for a plan
     *
     * @param context 领域计算上下文 / Domain calculation context
     * @param baseCoefficient 基础系数 / Base coefficient
     * @return 修正后的系数 / Modified coefficient
    */
    fun modifyBatchCoefficient(context: Csp1dDomainCalculationContext<V>, baseCoefficient: Flt64): Flt64 = baseCoefficient
}

/**
 * CSP1D 生成策略接口 / CSP1D generation policy interface
 *
 * 允许下游注入候选生成过滤、排序和验收逻辑。
 * 默认实现不改变任何生成行为。
 *
 * Allows downstream to inject candidate generation filtering,
 * sorting and acceptance logic. Default implementation does not
 * change any generation behavior.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dGenerationStrategy<V : RealNumber<V>> {

    /** 策略名称 / Policy name */
    val name: String

    /**
     * 判断候选方案是否应被接受 /
     * Check if candidate plan should be accepted
     *
     * @param candidate 候选方案 / Candidate plan
     * @param existingPlans 现有方案池 / Existing plan pool
     * @return true 表示接受 / true if accepted
    */
    fun acceptCandidate(candidate: CuttingPlan<V>, existingPlans: List<CuttingPlan<V>>): Boolean = true

    /**
     * 自定义候选方案的 canonical key /
     * Customize canonical key for candidate plan
     *
     * 返回 null 表示使用默认 canonical key。
     * Return null to use the default canonical key.
     *
     * @param candidate 候选方案 / Candidate plan
     * @return 自定义 canonical key 字符串，或 null 使用默认 / Custom canonical key string, or null for default
    */
    fun canonicalKeyFor(candidate: CuttingPlan<V>): String? = null

    /**
     * dominance 判断：是否接受新候选替代已有方案 /
     * Dominance judgment: whether to accept new candidate over existing
     *
     * 返回 true 表示新候选通过 dominance 判断应被接受。
     * 默认返回 true（不额外过滤 dominance）。
     *
     * Return true if the new candidate passes dominance check and should be accepted.
     * Default returns true (no extra dominance filtering).
     *
     * @param candidate 候选方案 / Candidate plan
     * @param existingPlans 现有方案池 / Existing plan pool
     * @return true 表示通过 dominance 判断 / true if passes dominance check
    */
    fun acceptDominance(candidate: CuttingPlan<V>, existingPlans: List<CuttingPlan<V>>): Boolean = true
}

/**
 * CSP1D 定价策略接口 / CSP1D pricing policy interface
 *
 * 允许下游注入 reduced cost 成本修正、isImproving 判断和候选排序逻辑。
 * 默认实现不改变任何定价行为。
 *
 * Allows downstream to inject reduced cost cost modification,
 * isImproving judgment and candidate sorting logic.
 * Default implementation does not change any pricing behavior.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dPricingPolicy<V : RealNumber<V>> {

    /** 策略名称 / Policy name */
    val name: String

    /**
     * 修正 reduced cost 成本 /
     * Modify reduced cost cost for a candidate plan
     *
     * @param candidate 候选方案 / Candidate plan
     * @param baseCost 基础成本 / Base cost
     * @return 修正后的成本 / Modified cost
    */
    fun modifyCost(candidate: CuttingPlan<V>, baseCost: V): V = baseCost

    /**
     * 修正 reduced cost benefit /
     * Modify reduced cost benefit for a candidate plan
     *
     * @param candidate 候选方案 / Candidate plan
     * @param baseBenefit 基础对偶收益 / Base dual benefit
     * @return 修正后的收益 / Modified benefit
    */
    fun modifyBenefit(candidate: CuttingPlan<V>, baseBenefit: V): V = baseBenefit

    /**
     * 自定义 isImproving 判断 /
     * Custom isImproving judgment
     *
     * 返回 null 表示使用默认判断（benefit > cost）。
     * 返回 true/false 表示强制判定结果。
     *
     * Return null to use default judgment (benefit > cost).
     * Return true/false to force the judgment result.
     *
     * @param candidate 候选方案 / Candidate plan
     * @param benefit 对偶收益 / Dual benefit
     * @param cost 目标成本 / Objective cost
     * @return null 使用默认判断，true/false 强制结果 / null for default, true/false to force
    */
    fun isImproving(candidate: CuttingPlan<V>, benefit: V, cost: V): Boolean? = null
}

/**
 * CSP1D 流程上下文 / CSP1D flow context
 *
 * 为流程策略提供求解器状态上下文。下游通过此接口访问
 * 当前迭代号、方案池、迭代上限等信息，用于流程判断。
 *
 * Provides solver state context for flow policies. Downstream
 * accesses current iteration, plan pool, iteration limit etc.
 * for flow control decisions.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dFlowContext<V : RealNumber<V>> {

    /** 当前迭代号（0-based）/ Current iteration number (0-based) */
    val iteration: Int64

    /** 当前方案池 / Current plan pool */
    val currentPlans: List<CuttingPlan<V>>

    /** 列生成迭代上限 / Column generation iteration limit */
    val iterationLimit: Int64

    /** 是否允许部分解 / Whether partial solution is allowed */
    val allowPartialSolution: Boolean

    /** 本轮新方案列表（定价后去重前为空）/ New plans from current pricing (empty before pricing) */
    val newPlans: List<CuttingPlan<V>> get() = emptyList()

    /** pricing 生成统计（累计）/ Accumulated pricing generation statistics */
    val pricingStatistics: CuttingPlanGenerationStatistics? get() = null

    /** LP 求解是否曾成功 / Whether at least one LP solve has succeeded */
    val hasValidLpResult: Boolean get() = false

    /**
     * warm start 方案数量（recovery 场景可用）/ Warm start plan count (available in recovery scenarios)
     *
     * 非 recovery 场景默认为 0。下游 policy 可据此判断 warm start 可复用方案规模。
     * Default 0 in non-recovery scenarios. Downstream can use this to gauge warm-start reusable plan scale.
    */
    val warmStartPlanCount: Int64 get() = Int64.zero

    /**
     * warm start 是否需要 fallback（recovery 场景可用）/ Whether warm start requires fallback (recovery scenarios)
     *
     * 非 recovery 场景默认为 false。当 warm start 不可用（Invalid 或 AdapterUnsupported）时为 true。
     * Default false in non-recovery scenarios. True when warm start is unusable (Invalid or AdapterUnsupported).
    */
    val warmStartRequiresFallback: Boolean get() = false
}

/**
 * CSP1D 流程策略接口 / CSP1D flow policy interface
 *
 * 允许下游注入求解流程控制逻辑，如初始方案过滤、
 * 去重等价判断、终止条件、partial 接受、recovery fallback 等。
 * 默认实现保持当前硬编码行为。
 *
 * Allows downstream to inject solver flow control logic,
 * such as initial plan filtering, dedup equivalence judgment,
 * termination conditions, partial acceptance, recovery fallback.
 * Default implementation preserves current hard-coded behavior.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dFlowPolicy<V : RealNumber<V>> {

    /** 策略名称 / Policy name */
    val name: String

    /**
     * 过滤初始方案池 /
     * Filter initial plan pool
     *
     * @param plans 初始方案列表 / Initial plan list
     * @return 过滤后的方案列表 / Filtered plan list
    */
    fun filterInitialPlans(plans: List<CuttingPlan<V>>): List<CuttingPlan<V>> = plans

    /**
     * 带上下文的初始方案过滤 / Context-aware initial plan filtering
     *
     * 默认回退到无上下文版本。
     * Default falls back to context-free version.
     *
     * @param context 流程上下文 / Flow context
     * @param plans 初始方案列表 / Initial plan list
     * @return 过滤后的方案列表 / Filtered plan list
    */
    fun filterInitialPlans(context: Csp1dFlowContext<V>, plans: List<CuttingPlan<V>>): List<CuttingPlan<V>> =
        filterInitialPlans(plans)

    /**
     * 判断两个方案是否等价（用于去重）/
     * Check if two plans are equivalent (for deduplication)
     *
     * @param existing 已有方案 / Existing plan
     * @param candidate 候选方案 / Candidate plan
     * @return true 表示等价 / true if equivalent
    */
    fun isEquivalent(existing: CuttingPlan<V>, candidate: CuttingPlan<V>): Boolean = false

    /**
     * 带上下文的等价判断 / Context-aware equivalence check
     *
     * 默认回退到无上下文版本。
     * Default falls back to context-free version.
     *
     * @param context 流程上下文 / Flow context
     * @param existing 已有方案 / Existing plan
     * @param candidate 候选方案 / Candidate plan
     * @return true 表示等价 / true if equivalent
    */
    fun isEquivalent(context: Csp1dFlowContext<V>, existing: CuttingPlan<V>, candidate: CuttingPlan<V>): Boolean =
        isEquivalent(existing, candidate)

    /**
     * 是否提前终止迭代（iteration limit 之外的业务停止条件）/
     * Whether to stop iteration early (business stop condition beyond iteration limit)
     *
     * 默认不停止。下游可基于当前方案池大小、LP 目标趋势等判断提前终止。
     * Default does not stop. Downstream may decide early termination based on
     * current plan pool size, LP objective trend, etc.
     *
     * @param context 流程上下文 / Flow context
     * @return true 表示应停止迭代 / true if iteration should stop
    */
    fun shouldStopIteration(context: Csp1dFlowContext<V>): Boolean = false

    /**
     * 自定义终止原因和消息 /
     * Customize termination reason and message
     *
     * 允许扩展 termination reason/message，但默认不变。
     * Allows extending termination reason/message, but default is unchanged.
     *
     * @param context 流程上下文 / Flow context
     * @param defaultReason 默认终止原因 / Default termination reason
     * @param defaultMessage 默认消息 / Default message
     * @return (终止原因名, 消息) 对 / (reason name, message) pair
    */
    fun selectTermination(
        context: Csp1dFlowContext<V>,
        defaultReason: String,
        defaultMessage: String?
    ): Pair<String, String?> = defaultReason to defaultMessage

    /**
     * final MILP 失败时是否接受 partial /
     * Whether to accept partial solution when final MILP fails
     *
     * @param context 流程上下文 / Flow context
     * @param defaultDecision 默认决策 / Default decision
     * @return true 表示接受 partial / true to accept partial
    */
    fun acceptPartial(context: Csp1dFlowContext<V>, defaultDecision: Boolean): Boolean = defaultDecision

    /**
     * recovery/fallback 是否启用 /
     * Whether recovery/fallback is enabled
     *
     * @param context 流程上下文 / Flow context
     * @param defaultDecision 默认决策 / Default decision
     * @return true 表示启用 / true to enable
    */
    fun allowRecoveryFallback(context: Csp1dFlowContext<V>, defaultDecision: Boolean): Boolean = defaultDecision
}

/**
 * 通过流程策略列表过滤初始方案 / Filter initial plans through flow policy list
 *
 * @param policies 流程策略列表 / Flow policy list
 * @param context 流程上下文 / Flow context
 * @param plans 初始方案列表 / Initial plan list
 * @return 过滤后的方案列表 / Filtered plan list
*/
fun <V : RealNumber<V>> filterInitialPlansByPolicies(
    policies: List<Csp1dFlowPolicy<V>>,
    context: Csp1dFlowContext<V>,
    plans: List<CuttingPlan<V>>
): List<CuttingPlan<V>> {
    return policies.fold(plans) { acc, policy -> policy.filterInitialPlans(context, acc) }
}

/**
 * 通过流程策略列表检查等价 / Check equivalence through flow policy list
 *
 * @param policies 流程策略列表 / Flow policy list
 * @param context 流程上下文 / Flow context
 * @param existing 已有方案 / Existing plan
 * @param candidate 候选方案 / Candidate plan
 * @return true 表示等价 / true if equivalent
*/
fun <V : RealNumber<V>> isEquivalentByPolicies(
    policies: List<Csp1dFlowPolicy<V>>,
    context: Csp1dFlowContext<V>,
    existing: CuttingPlan<V>,
    candidate: CuttingPlan<V>
): Boolean {
    return policies.any { it.isEquivalent(context, existing, candidate) }
}

/**
 * 通过流程策略列表判断是否提前停止 / Check early stop through flow policy list
 *
 * @param policies 流程策略列表 / Flow policy list
 * @param context 流程上下文 / Flow context
 * @return true 表示应停止 / true if should stop
*/
fun <V : RealNumber<V>> shouldStopByPolicies(
    policies: List<Csp1dFlowPolicy<V>>,
    context: Csp1dFlowContext<V>
): Boolean {
    return policies.any { it.shouldStopIteration(context) }
}

/**
 * 通过流程策略列表自定义终止 / Select termination through flow policy list
 *
 * @param policies 流程策略列表 / Flow policy list
 * @param context 流程上下文 / Flow context
 * @param defaultReason 默认终止原因 / Default termination reason
 * @param defaultMessage 默认消息 / Default message
 * @return (终止原因名, 消息) 对 / (reason name, message) pair
*/
fun <V : RealNumber<V>> selectTerminationByPolicies(
    policies: List<Csp1dFlowPolicy<V>>,
    context: Csp1dFlowContext<V>,
    defaultReason: String,
    defaultMessage: String?
): Pair<String, String?> {
    return policies.fold(defaultReason to defaultMessage) { (reason, msg), policy ->
        policy.selectTermination(context, reason, msg)
    }
}

/**
 * 通过流程策略列表判断 partial 接受 / Accept partial through flow policy list
 *
 * @param policies 流程策略列表 / Flow policy list
 * @param context 流程上下文 / Flow context
 * @param defaultDecision 默认决策 / Default decision
 * @return true 表示接受 partial / true to accept partial
*/
fun <V : RealNumber<V>> acceptPartialByPolicies(
    policies: List<Csp1dFlowPolicy<V>>,
    context: Csp1dFlowContext<V>,
    defaultDecision: Boolean
): Boolean {
    return policies.fold(defaultDecision) { decision, policy -> policy.acceptPartial(context, decision) }
}

/**
 * 通过流程策略列表判断 recovery fallback / Allow recovery fallback through flow policy list
 *
 * @param policies 流程策略列表 / Flow policy list
 * @param context 流程上下文 / Flow context
 * @param defaultDecision 默认决策 / Default decision
 * @return true 表示启用 / true to enable
*/
fun <V : RealNumber<V>> allowRecoveryFallbackByPolicies(
    policies: List<Csp1dFlowPolicy<V>>,
    context: Csp1dFlowContext<V>,
    defaultDecision: Boolean
): Boolean {
    return policies.fold(defaultDecision) { decision, policy -> policy.allowRecoveryFallback(context, decision) }
}

/**
 * CSP1D 提取策略接口 / CSP1D extraction policy interface
 *
 * 允许下游在 solution enrichment 阶段向 solution details 或 render KPI
 * 写入自定义信息，而不修改 solution 模型本身。
 * 默认空策略不改变现有 solution、KPI、render 输出。
 *
 * Allows downstream to write custom information to solution details or render KPI
 * during solution enrichment, without modifying the solution model itself.
 * Default empty policy does not change existing solution, KPI, or render output.
 *
 * @param V 数值类型 / Numeric value type
*/
interface Csp1dExtractionPolicy<V : RealNumber<V>> {

    /** 策略名称 / Policy name */
    val name: String

    /**
     * 向 solution details 和 render KPI 写入自定义信息 /
     * Write custom information to solution details and render KPI
     *
     * @param details 可写的 solution details map / Mutable solution details map
     * @param renderKpi 可写的 render KPI map / Mutable render KPI map
     * @param produce 求解产出（CuttingPlanUsage、MaterialUsage、MachineUsage、UnmetDemand）/ Solve output
     * @param demands 需求列表 / Demand list
     * @param materials 物料列表 / Material list
     * @param machines 设备列表 / Machine list
     * @param generatedPlans 生成的方案池 / Generated plan pool
     * @param iterationCount 列生成迭代次数 / Column generation iteration count
     * @param terminationReason 列生成终止原因名 / Column generation termination reason name
     * @param finalMilpStatus 最终 MILP 状态名 / Final MILP status name
     * @param pricingStatistics pricing 生成统计 / Pricing generation statistics
    */
    fun enrichOutput(
        details: MutableMap<String, String>,
        renderKpi: MutableMap<String, String>,
        produce: Produce<V>,
        demands: List<ProductDemand<V>>,
        materials: List<Material<V>>,
        machines: List<Machine<V>>,
        generatedPlans: List<CuttingPlan<V>>,
        iterationCount: Int64,
        terminationReason: String?,
        finalMilpStatus: String?,
        pricingStatistics: CuttingPlanGenerationStatistics?
    ) {}
}

/**
 * CSP1D 扩展包 / CSP1D extension set
 *
 * 承载所有扩展策略的统一容器。下游通过此类型注入
 * 建模管线、领域策略、目标策略、生成策略、定价策略和流程策略。
 * 默认空实现不改变现有求解行为。
 *
 * Unified container for all extension policies. Downstream injects
 * modeling pipelines, domain policies, objective policies,
 * generation policies, pricing policies and flow policies through this type.
 * Default empty implementation does not change existing solver behavior.
 *
 * @param V 数值类型 / Numeric value type
*/
data class Csp1dExtensionSet<V : RealNumber<V>>(

    /** 建模扩展管线列表 / Modeling extension pipeline list */
    val modelingExtensions: List<Csp1dModelingExtension<V>> = emptyList(),

    /** 领域策略列表 / Domain policy list */
    val domainPolicies: List<Csp1dDomainPolicy<V>> = emptyList(),

    /** 目标策略列表 / Objective policy list */
    val objectivePolicies: List<Csp1dObjectivePolicy<V>> = emptyList(),

    /** 生成策略列表 / Generation strategy list */
    val generationStrategies: List<Csp1dGenerationStrategy<V>> = emptyList(),

    /** 定价策略列表 / Pricing policy list */
    val pricingPolicies: List<Csp1dPricingPolicy<V>> = emptyList(),

    /** 流程策略列表 / Flow policy list */
    val flowPolicies: List<Csp1dFlowPolicy<V>> = emptyList(),

    /** 提取策略列表 / Extraction policy list */
    val extractionPolicies: List<Csp1dExtractionPolicy<V>> = emptyList()
) {
    companion object {
        /** 空扩展包 / Empty extension set
         *
         * @return 空的扩展包实例 / Empty extension set instance
        */
        fun <V : RealNumber<V>> empty(): Csp1dExtensionSet<V> = Csp1dExtensionSet()
    }
}
