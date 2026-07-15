package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product

/**
 * Length assignment result for a single product.
 * 卷长分配结果，描述单个产品被分配的卷长
 *
 * @param V Numeric value type / 数值类型
 * @property product Product / 产品
 * @property assignedLength Assigned coil length / 分配的卷长
 * @property batchCount Assigned batch count / 分配的批次数
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
     * Minimize total assigned length.
     * 最小化总卷长
     *
     * @param V Numeric value type / 数值类型
     * @property weight Objective weight / 目标权重
    */
    data class MinimizeTotalLength<V : RealNumber<V>>(
        val weight: V
    ) : LengthAssignmentObjective<V>

    /**
     * Minimize batch count.
     * 最小化批次数
     *
     * @param V Numeric value type / 数值类型
     * @property weight Objective weight / 目标权重
    */
    data class MinimizeBatchCount<V : RealNumber<V>>(
        val weight: V
    ) : LengthAssignmentObjective<V>

    /**
     * Minimize over-length.
     * 最小化超长
     *
     * @param V Numeric value type / 数值类型
     * @property weight Objective weight / 目标权重
    */
    data class MinimizeOverLength<V : RealNumber<V>>(
        val weight: V
    ) : LengthAssignmentObjective<V>
}
