package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 生产物料接口，定义物料的基本属性 / Production material interface defining basic material properties
 *
 * @param V 数值类型 / Numeric value type
 * @property id 物料标识 / Material identifier
 * @property width 物料宽度列表 / List of material widths
 * @property length 物料长度 / Material length
 * @property unitWeight 单位重量 / Unit weight
*/
interface Production<V : RealNumber<V>> {
    val id: ProductionId?
    val width: List<Quantity<V>>
    val length: Quantity<V>?
    val unitWeight: Quantity<V>?
}
