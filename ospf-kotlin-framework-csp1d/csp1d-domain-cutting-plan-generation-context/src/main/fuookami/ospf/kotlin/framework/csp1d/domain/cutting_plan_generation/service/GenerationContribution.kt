package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * Computes the contribution length for a product on a given material.
 * 计算产品在给定物料上的贡献长度。
 *
 * @param product the product to compute contribution length for
 *                要计算贡献长度的产品
 * @param material the material context used when the product has dynamic length
 *                 当产品具有动态长度时使用的物料上下文
 * @return the contribution length quantity, or null if the product has no length and is not dynamic
 *         贡献长度数量，若产品无长度且非动态则返回 null
 */
internal fun <V : RealNumber<V>> generationContributionLength(
    product: Product<V>,
    material: Material<V>
): Quantity<V>? {
    return product.length ?: if (product.dynamicLength) {
        material.length
    } else {
        null
    }
}
