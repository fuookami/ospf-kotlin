package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingExtension
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtensionSet
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.application.service.WasteMinimizationConfig

/**
 * CSP1D 问题定义 / CSP1D problem definition
 *
 * @param V 数值类型 / Numeric value type
 * @property products 产品列表 / Product list
 * @property materials 物料列表 / Material list
 * @property machines 设备列表 / Machine list
 * @property costars 配规列表 / Costar list
 * @property demands 需求列表 / Demand list
 * @property configuration 求解配置 / Solving configuration
 * @property solveConfig 一站式求解配置，缺省时使用 [configuration] / One-stop solve config, falls back to [configuration]
 */
data class Csp1dProblem<V : RealNumber<V>>(
    val products: List<Product<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>>,
    val costars: List<Costar<V>> = emptyList(),
    val demands: List<ProductDemand<V>>,
    val configuration: Csp1dConfiguration<V> = Csp1dConfiguration(),
    val solveConfig: Csp1dSolveConfig<V>? = null
)

/**
 * CSP1D 求解配置 / CSP1D solving configuration
 *
 * @param V 数值类型 / Numeric value type
 * @property maxInitialPlans 初始方案上限 / Initial plan limit
 * @property maxPricingPlans 每轮定价新增方案上限 / Max pricing plans per iteration
 * @property iterationLimit 列生成迭代上限 / Column generation iteration limit
 */
data class Csp1dConfiguration<V : RealNumber<V>>(
    val maxInitialPlans: Int64 = Int64(1024),
    val maxPricingPlans: Int64 = Int64(64),
    val iterationLimit: Int64 = Int64(8)
)

/**
 * CSP1D 一站式求解配置 / CSP1D one-stop solve configuration
 *
 * @param V 数值类型 / Numeric value type
 * @property columnGeneration 列生成配置 / Column generation configuration
 * @property yieldConfig 产出约束与目标配置 / Yield constraint and objective configuration
 * @property wasteConfig 浪费最小化配置 / Waste minimization configuration
 * @property lengthConfig 长度分配配置 / Length assignment configuration
 * @property topKPlanLimit Top-K 方案保留上限，null 表示不输出 / Top-K plan limit, null for disabled
 * @property allowPartialSolution 最终 MILP 失败时是否返回部分结果 / Whether to return a partial result when final MILP fails
 * @property extensions 建模扩展列表，用于注入额外管线（如 same unit length / same width 等业务约束）/ Modeling extensions for injecting additional pipelines (e.g. same unit length / same width constraints)
 */
data class Csp1dSolveConfig<V : RealNumber<V>>(
    val columnGeneration: Csp1dConfiguration<V> = Csp1dConfiguration(),
    val yieldConfig: YieldModelingConfig<V>? = null,
    val wasteConfig: WasteMinimizationConfig<V>? = null,
    val lengthConfig: LengthAssignmentModelingConfig<V>? = null,
    val topKPlanLimit: Int64? = null,
    val allowPartialSolution: Boolean = true,
    val extensions: List<Csp1dModelingExtension<V>> = emptyList(),
    val extensionSet: Csp1dExtensionSet<V> = Csp1dExtensionSet()
) {
    /**
     * 合并 extensions 与 extensionSet.modelingExtensions /
     * Merge extensions and extensionSet.modelingExtensions
     *
     * 保证直接构造 Csp1dSolveConfig(extensionSet = ...) 时 modelingExtensions 也能进入求解路径。
     * Ensures that modelingExtensions from extensionSet also enters the solve path
     * when Csp1dSolveConfig is constructed directly with extensionSet.
     */
    val allExtensions: List<Csp1dModelingExtension<V>>
        get() = extensions + extensionSet.modelingExtensions.filter { ext ->
            ext !in extensions
        }
}
