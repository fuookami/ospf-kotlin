package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 配规/副产物，可填充切割方案剩余宽度 / Costar/byproduct that can fill remaining width in cutting plans
 *
 * @param V 数值类型 / Numeric value type
 */
open class Costar<V : RealNumber<V>>(
    override val id: CostarId,
    val name: String,
    override val width: List<Quantity<V>>,
    override val length: Quantity<V>? = null,
    override val unitWeight: Quantity<V>? = null
) : Production<V> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Costar<*>) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Costar($id, $name)"
}
