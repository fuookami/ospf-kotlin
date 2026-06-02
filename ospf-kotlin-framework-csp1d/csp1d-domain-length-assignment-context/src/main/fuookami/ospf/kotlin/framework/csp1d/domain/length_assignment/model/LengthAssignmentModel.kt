package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 卷长分配结果，描述单个产品被分配的卷长 / Length assignment result for a single product
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property assignedLength 分配的卷长 / Assigned coil length
 * @property batchCount 分配的批次数 / Assigned batch count
 */
data class LengthAssignment<V : RealNumber<V>>(
    val product: Product<V>,
    val assignedLength: Quantity<V>,
    val batchCount: UInt64
)

/**
 * 超长记录，产品实际卷长超过最大超产长度的部分 / Over-length record: length exceeding maxOverProduceLength
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property overLength 超长量 / Over-length quantity
 */
data class OverLengthRecord<V : RealNumber<V>>(
    val product: Product<V>,
    val overLength: Quantity<V>
)

/**
 * 长度分配目标 / Length assignment objective
 *
 * @param V 数值类型 / Numeric value type
 */
sealed interface LengthAssignmentObjective<V : RealNumber<V>> {
    /**
     * 最小化总卷长 / Minimize total assigned length
     *
     * @param V 数值类型 / Numeric value type
     */
    data class MinimizeTotalLength<V : RealNumber<V>>(
        val weight: V
    ) : LengthAssignmentObjective<V>

    /**
     * 最小化批次数 / Minimize batch count
     *
     * @param V 数值类型 / Numeric value type
     */
    data class MinimizeBatchCount<V : RealNumber<V>>(
        val weight: V
    ) : LengthAssignmentObjective<V>

    /**
     * 最小化超长 / Minimize over-length
     *
     * @param V 数值类型 / Numeric value type
     */
    data class MinimizeOverLength<V : RealNumber<V>>(
        val weight: V
    ) : LengthAssignmentObjective<V>
}
