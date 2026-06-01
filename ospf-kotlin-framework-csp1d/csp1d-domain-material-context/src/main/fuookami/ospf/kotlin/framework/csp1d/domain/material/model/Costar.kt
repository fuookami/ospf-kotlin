package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 配规（联副产品）/ Co-product (costar)
 *
 * @param V 数值类型 / Numeric value type
 * @property id 配规标识 / Costar identifier
 * @property name 配规名称 / Costar name
 * @property widthRange 可用幅宽范围 / Available width range
 * @property length 配规长度 / Costar length
 * @property unitWeight 配规单位重量 / Costar unit weight
 * @property dynamicLength 是否动态长度 / Whether length is dynamic
 */
data class Costar<V : RealNumber<V>>(
    override val id: String,
    val name: String,
    val widthRange: WidthRange<V>,
    override val length: Quantity<V>? = null,
    override val unitWeight: Quantity<V>? = null,
    val dynamicLength: Boolean = false
) : Production<V>, ManualIndexed() {
    override val width: List<Quantity<V>> by lazy {
        listOf(
            widthRange.lowerBound,
            widthRange.upperBound
        )
    }
}

