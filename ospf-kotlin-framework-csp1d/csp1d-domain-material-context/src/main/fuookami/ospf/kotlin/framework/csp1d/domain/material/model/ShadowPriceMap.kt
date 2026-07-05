/**
 * CSP1D 影子价格映射及相关类型 / CSP1D shadow price map and related types
 *
 * 定义 CSP1D 的影子价格键、参数接口、抽象映射和列生成管线类型别名，
 * 与框架 AbstractShadowPriceMap / CGPipeline 体系对齐。
 *
 * Defines CSP1D shadow price keys, arguments interface, abstract map and
 * column generation pipeline type aliases, aligned with the framework's
 * AbstractShadowPriceMap / CGPipeline hierarchy.
 */
package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap

/**
 * CSP1D 影子价格索引键 / CSP1D shadow price key
 *
 * 继承框架 ShadowPriceKey，提供 limit 属性用于约束限制类型标识。
 * Inherits from framework ShadowPriceKey, providing limit property for constraint limit type identification.
 */
sealed class Csp1dShadowPriceKey(
    limit: KClass<*>
) : fuookami.ospf.kotlin.framework.model.ShadowPriceKey(limit) {
    /**
     * 键名称 / Key name
     */
    abstract val name: String
}

/**
 * 产品需求影子价格键 / Product demand shadow price key
 *
 * @property productId 产品 ID / Product id
 * @property unitSymbol 需求单位符号 / Demand unit symbol
 */
data class ProductDemandShadowPriceKey(
    val productId: ProductId,
    val unitSymbol: String
) : Csp1dShadowPriceKey(ProductDemandShadowPriceKey::class) {
    override val name = "product-demand:$productId:$unitSymbol"
}

/**
 * 物料用量影子价格键 / Material usage shadow price key
 */
data class MaterialUsageShadowPriceKey(
    val materialId: MaterialId
) : Csp1dShadowPriceKey(MaterialUsageShadowPriceKey::class) {
    override val name = "material-usage:$materialId"
}

/**
 * 设备批次数影子价格键 / Machine batch count shadow price key
 */
data class MachineBatchShadowPriceKey(
    val machineId: MachineId
) : Csp1dShadowPriceKey(MachineBatchShadowPriceKey::class) {
    override val name = "machine-batch:$machineId"
}

/**
 * 设备业务产能影子价格键 / Machine business capacity shadow price key
 */
data class MachineCapacityShadowPriceKey(
    val machineId: MachineId
) : Csp1dShadowPriceKey(MachineCapacityShadowPriceKey::class) {
    override val name = "machine-capacity:$machineId"
}

/**
 * 产出超产上限影子价格键 / Yield over-production bound shadow price key
 *
 * @property productId 产品 ID / Product id
 * @property unitSymbol 需求单位符号 / Demand unit symbol
 */
data class YieldOverProductionBoundShadowPriceKey(
    val productId: ProductId,
    val unitSymbol: String
) : Csp1dShadowPriceKey(YieldOverProductionBoundShadowPriceKey::class) {
    override val name = "yield-over-production-bound:$productId:$unitSymbol"
}

/**
 * CSP1D 影子价格参数接口 / CSP1D shadow price arguments interface
 *
 * 用于列生成管线计算 reduced cost 时传递参数。
 * Used for passing arguments when computing reduced cost in column generation pipelines.
 */
interface AbstractCsp1dShadowPriceArguments

/**
 * 切割方案影子价格参数 / Cutting plan shadow price arguments
 *
 * @property plan 切割方案 / Cutting plan
 */
data class Csp1dCuttingPlanShadowPriceArguments<V : RealNumber<V>>(
    val plan: CuttingPlan<V>
) : AbstractCsp1dShadowPriceArguments

/**
 * 抽象 CSP1D 影子价格映射 / Abstract CSP1D shadow price map
 *
 * 继承框架 AbstractShadowPriceMap，与 CGPipeline / extractShadowPrice 体系对齐。
 * Inherits from framework AbstractShadowPriceMap, aligned with CGPipeline / extractShadowPrice hierarchy.
 *
 * @param Args 参数类型 / Arguments type
 */
open class AbstractCsp1dShadowPriceMap<
        out Args : AbstractCsp1dShadowPriceArguments
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, AbstractCsp1dShadowPriceMap<@UnsafeVariance Args>
        >()

/**
 * 切割方案影子价格表（轻量级结果容器） / Cutting plan shadow price map (lightweight result container)
 *
 * 用于 pricing 阶段消费影子价格，不依赖框架 AbstractShadowPriceMap 的完整生命周期。
 * Used for consuming shadow prices during pricing, without depending on the full
 * AbstractShadowPriceMap lifecycle.
 *
 * @param V 数值类型 / Numeric value type
 */
data class ShadowPriceMap<V : RealNumber<V>>(
    val values: Map<Csp1dShadowPriceKey, V> = emptyMap()
) {
    /**
     * 查询影子价格 / Get shadow price by key
     *
     * @param key 影子价格键 / Shadow price key
     * @return 对应影子价格 / Matched shadow price
     */
    operator fun get(key: Csp1dShadowPriceKey): V? = values[key]
}

/**
 * 从框架 AbstractShadowPriceMap 提取轻量级 ShadowPriceMap / Extract lightweight ShadowPriceMap from framework AbstractShadowPriceMap
 *
 * @param V 数值类型 / Numeric value type
 * @param shadowPriceMap 框架影子价格映射 / Framework shadow price map
 * @param converter Flt64 到 V 的转换函数 / Conversion function from Flt64 to V
 * @return 轻量级影子价格映射 / Lightweight shadow price map
 */
fun <V : RealNumber<V>> AbstractCsp1dShadowPriceMap<*>.toShadowPriceMap(
    converter: (Flt64) -> V
): ShadowPriceMap<V> {
    val values = HashMap<Csp1dShadowPriceKey, V>()
    for ((key, price) in map) {
        if (key is Csp1dShadowPriceKey) {
            values[key] = converter(price.price)
        }
    }
    return ShadowPriceMap(values)
}
