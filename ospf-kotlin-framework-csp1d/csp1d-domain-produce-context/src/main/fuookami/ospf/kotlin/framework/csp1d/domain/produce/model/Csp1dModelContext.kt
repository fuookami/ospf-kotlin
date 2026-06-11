package fuookami.ospf.kotlin.framework.csp1d.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
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
     *
     * Extract shadow prices of each constraint from the LP relaxation dual solution,
     * for use in the pricing sub-problem to compute reduced cost.
     *
     * @param model 元模型 / Meta model
     * @param shadowPrices 对偶解 / Dual solution
     * @param shadowPriceKeys 约束名到影子价格键的映射 / Constraint name to shadow price key mapping
     * @return 操作结果 / Operation result
     */
    fun extractShadowPrice(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution,
        shadowPriceKeys: MutableMap<String, fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey>
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
 * CSP1D 管线列表类型别名 / CSP1D pipeline list type alias
 *
 * CSP1D 建模管线的有序列表，按注册顺序执行。
 * Ordered list of CSP1D modeling pipelines, executed in registration order.
 */
typealias Csp1dPipelineList = List<Pipeline<LinearMetaModel<Flt64>>>

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
 * @property pipeline 扩展管线 / Extension pipeline
 * @property mode 扩展适用模式 / Extension applicable mode
 */
data class Csp1dModelingExtension<V : RealNumber<V>>(
    val pipeline: Pipeline<LinearMetaModel<Flt64>>,
    val mode: Csp1dExtensionMode = Csp1dExtensionMode.ALL
)
