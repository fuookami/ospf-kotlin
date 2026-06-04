package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

/**
 * 长度分配建模配置，供 application 层消费 / Length assignment modeling configuration for application layer
 *
 * 当提供此配置时，Csp1dMilpSolver 为动态长度产品注册卷长变量、超长松弛变量、边界约束和惩罚目标。
 * 所有权重为无量纲归一化系数，由调用方负责单位换算。
 *
 * @param V 数值类型 / Numeric value type
 * @property dynamicProductIds 需要动态分配卷长的产品 ID 集合 / Product IDs requiring dynamic length assignment
 * @property assignedLengthLowerBound 已分配卷长下界，按产品 ID 口径，缺省表示无下界 / Assigned length lower bound per product id, missing for no lower bound
 * @property assignedLengthUpperBound 已分配卷长上界，按产品 ID 口径，缺省表示无上界 / Assigned length upper bound per product id, missing for no upper bound
 * @property overLengthPenalty 超长惩罚权重，按产品 ID 口径 / Over-length penalty weight per product id
 * @property overLengthUpperBound 超长上限，按产品 ID 口径，缺省表示无上限 / Over-length upper bound per product id, missing for no limit
 * @property totalLengthPenalty 总卷长惩罚权重 / Total assigned length penalty weight
 * @property batchMinPenalty 批次最小化惩罚权重（动态长度产品）/ Batch minimization penalty weight for dynamic-length products
 */
data class LengthAssignmentModelingConfig<V : RealNumber<V>>(
    val dynamicProductIds: Set<String> = emptySet(),
    val assignedLengthLowerBound: Map<String, V> = emptyMap(),
    val assignedLengthUpperBound: Map<String, V> = emptyMap(),
    val overLengthPenalty: Map<String, V> = emptyMap(),
    val overLengthUpperBound: Map<String, V> = emptyMap(),
    val totalLengthPenalty: V? = null,
    val batchMinPenalty: V? = null
)

/**
 * 长度分配建模结果，从 solver solution 回填 / Length assignment modeling result back-filled from solver solution
 *
 * @param V 数值类型 / Numeric value type
 * @property assignedLengths 动态长度产品的卷长分配值 / Assigned lengths for dynamic-length products
 * @property overLengths 超长产品值 / Over-length values for products exceeding bounds
 */
data class LengthAssignmentModelingResult<V : RealNumber<V>>(
    val assignedLengths: List<ModeledAssignedLength<V>> = emptyList(),
    val overLengths: List<ModeledOverLength<V>> = emptyList()
)

/**
 * 动态卷长分配结果（建模层扁平类型）/ Dynamic length assignment result (flat modeling type)
 *
 * @param V 数值类型 / Numeric value type
 * @property productId 产品 ID / Product id
 * @property assignedLength 分配的卷长值 / Assigned length value
 */
data class ModeledAssignedLength<V : RealNumber<V>>(
    val productId: String,
    val assignedLength: V
)

/**
 * 超长建模结果（建模层扁平类型）/ Over-length modeling result (flat modeling type)
 *
 * @param V 数值类型 / Numeric value type
 * @property productId 产品 ID / Product id
 * @property overLength 超长值 / Over-length value
 */
data class ModeledOverLength<V : RealNumber<V>>(
    val productId: String,
    val overLength: V
)
