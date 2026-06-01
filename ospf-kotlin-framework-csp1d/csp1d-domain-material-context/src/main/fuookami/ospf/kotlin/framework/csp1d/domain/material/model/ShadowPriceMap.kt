package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * 影子价格索引键 / Shadow price key
 */
sealed interface ShadowPriceKey {
    /**
     * 键名称 / Key name
     */
    val name: String
}

/**
 * 产品需求影子价格键 / Product demand shadow price key
 */
data class ProductDemandShadowPriceKey(
    val productId: String
) : ShadowPriceKey {
    override val name = "product-demand:$productId"
}

/**
 * 物料用量影子价格键 / Material usage shadow price key
 */
data class MaterialUsageShadowPriceKey(
    val materialId: String
) : ShadowPriceKey {
    override val name = "material-usage:$materialId"
}

/**
 * 设备产能影子价格键 / Machine capacity shadow price key
 */
data class MachineCapacityShadowPriceKey(
    val machineId: String
) : ShadowPriceKey {
    override val name = "machine-capacity:$machineId"
}

/**
 * 切割方案影子价格表 / Cutting plan shadow price map
 *
 * @param V 数值类型 / Numeric value type
 */
data class ShadowPriceMap<V : RealNumber<V>>(
    val values: Map<ShadowPriceKey, V> = emptyMap()
) {
    /**
     * 查询影子价格 / Get shadow price by key
     *
     * @param key 影子价格键 / Shadow price key
     * @return 对应影子价格 / Matched shadow price
     */
    operator fun get(key: ShadowPriceKey): V? = values[key]
}

