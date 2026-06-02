package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignment
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.OverLengthRecord
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 长度推导函数：从需求量和产品属性推导卷长 / Length derivation function: derive coil length from demand quantity and product properties
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface LengthDerivation<V : RealNumber<V>> {
    /**
     * 从需求量和产品属性推导卷长 / Derive coil length from demand quantity and product properties
     *
     * @param demandQuantity 需求量 / Demand quantity
     * @param product 产品 / Product
     * @return 推导的卷长，null 表示无法推导 / Derived coil length, null if derivation is not possible
     */
    fun derive(demandQuantity: Quantity<V>, product: Product<V>): Quantity<V>?
}

/**
 * 长度分配约束 / Length assignment constraint
 *
 * @param V 数值类型 / Numeric value type
 */
sealed interface LengthAssignmentConstraint<V : RealNumber<V>> {
    /**
     * 超长约束：产品卷长不得超过 maxOverProduceLength / Over-length constraint: product length must not exceed maxOverProduceLength
     *
     * @param V 数值类型 / Numeric value type
     */
    data class MaxOverLength<V : RealNumber<V>>(
        val product: Product<V>
    ) : LengthAssignmentConstraint<V>

    /**
     * 最小批次约束：产品分配批次数不得低于给定值 / Minimum batch constraint: assigned batch count must not be below given value
     *
     * @param V 数值类型 / Numeric value type
     */
    data class MinBatchCount<V : RealNumber<V>>(
        val product: Product<V>,
        val minBatches: UInt64
    ) : LengthAssignmentConstraint<V>
}

/**
 * 长度分配输入 / Length assignment input
 *
 * @param V 数值类型 / Numeric value type
 * @property dynamicProducts 动态长度产品列表 / Dynamic-length products
 * @property demands 产品需求 / Product demands
 * @property constraints 长度分配约束 / Length assignment constraints
 */
data class LengthAssignmentInput<V : RealNumber<V>>(
    val dynamicProducts: List<Product<V>>,
    val demands: List<ProductDemand<V>>,
    val constraints: List<LengthAssignmentConstraint<V>> = emptyList()
)

/**
 * 长度分配结果 / Length assignment result
 *
 * @param V 数值类型 / Numeric value type
 * @property assignments 分配列表 / Assignment list
 * @property overLengthRecords 超长记录 / Over-length records
 */
data class LengthAssignmentResult<V : RealNumber<V>>(
    val assignments: List<LengthAssignment<V>>,
    val overLengthRecords: List<OverLengthRecord<V>>
)

/**
 * 长度分配上下文，负责动态卷长分配与超长检测 / Length assignment context: dynamic coil length assignment and over-length detection
 *
 * @param V 数值类型 / Numeric value type
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property lengthDerivation 长度推导函数，由下游项目按业务单位注入 / Length derivation function injected by downstream project per business unit
 */
class LengthAssignmentContext<V : RealNumber<V>>(
    private val arithmetic: QuantityArithmetic<V>,
    private val lengthDerivation: LengthDerivation<V>
) {
    /**
     * 执行长度分配 / Execute length assignment
     *
     * @param input 长度分配输入 / Length assignment input
     * @return 长度分配结果 / Length assignment result
     */
    fun assign(input: LengthAssignmentInput<V>): LengthAssignmentResult<V> {
        val assignments = ArrayList<LengthAssignment<V>>()
        val overLengthRecords = ArrayList<OverLengthRecord<V>>()

        for (product in input.dynamicProducts) {
            val demand = input.demands.firstOrNull { it.product.id == product.id } ?: continue
            val assignedLength = lengthDerivation.derive(demand.quantity, product) ?: continue
            val batchCount = UInt64.one

            assignments.add(
                LengthAssignment(
                    product = product,
                    assignedLength = assignedLength,
                    batchCount = batchCount
                )
            )

            // Check over-length constraint
            val maxOverLength = product.maxOverProduceLength
            if (maxOverLength != null && maxOverLength.unit == assignedLength.unit) {
                val comparison = assignedLength.value partialOrd maxOverLength.value
                if (comparison is Order.Greater) {
                    overLengthRecords.add(
                        OverLengthRecord(
                            product = product,
                            overLength = arithmetic.subtract(assignedLength, maxOverLength)
                        )
                    )
                }
            }
        }

        return LengthAssignmentResult(
            assignments = assignments,
            overLengthRecords = overLengthRecords
        )
    }
}
