package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Length assignment aggregation root.
 * 长度分配聚合根
 *
 * Manages dynamic coil length variables (assigned_length_i) and over-length slack variables (over_length_i),
 * replacing the LengthSlackVars inner class in Csp1dMilpSolver.
 * 管理动态卷长变量（assigned_length_i）和超长松弛变量（over_length_i），替代 Csp1dMilpSolver 中的 LengthSlackVars 内部类。
 *
 * @param V Numeric value type / 数值类型
 * @property config Length assignment modeling configuration / 长度分配建模配置
 * @property demands Demand list / 需求列表
*/
class LengthAggregation<V : RealNumber<V>>(
    val config: LengthAssignmentModelingConfig<V>,
    val demands: List<ProductDemand<V>>
) : Csp1dAggregation<V> {

    /** 已分配卷长变量，按 demand 索引 / Assigned length variables, indexed by demand */
    val assignedLength: List<URealVar?> = demands.mapIndexed { demandIndex, demand ->
        val productId = demand.product.id
        val isDynamic = config.dynamicProductIds.contains(productId)
        val hasBound = config.assignedLengthLowerBound.containsKey(productId) ||
                config.assignedLengthUpperBound.containsKey(productId)
        val hasPenalty = config.totalLengthPenalty != null || config.overLengthPenalty.containsKey(productId)
        if (isDynamic && (hasBound || hasPenalty)) {
            URealVar("assigned_length_$demandIndex")
        } else {
            null
        }
    }

    /** 超长松弛变量，按 demand 索引 / Over-length slack variables, indexed by demand */
    val overLength: List<URealVar?> = demands.mapIndexed { demandIndex, demand ->
        val productId = demand.product.id
        val isDynamic = config.dynamicProductIds.contains(productId)
        val hasOverBound = config.overLengthUpperBound.containsKey(productId)
        val hasOverPenalty = config.overLengthPenalty.containsKey(productId)
        val hasMaxOverLength = demand.product.maxOverProduceLength != null
        if (isDynamic && (hasOverBound || hasOverPenalty || hasMaxOverLength)) {
            URealVar("over_length_$demandIndex")
        } else {
            null
        }
    }

    /** 是否存在任何长度变量 / Whether any length variables exist */
    val hasAny: Boolean get() = assignedLength.any { it != null } || overLength.any { it != null }

    override val cuttingPlans: List<CuttingPlan<V>> = emptyList()

    /**
     * 注册到元模型 / Register to meta model
    */
    override fun register(model: LinearMetaModel<Flt64>): Try {
        for (var_ in assignedLength.filterNotNull()) {
            when (val result = model.add(var_)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (var_ in overLength.filterNotNull()) {
            when (val result = model.add(var_)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * Extract length assignment modeling result from solver solution.
     * 从求解器解中提取长度分配建模结果
     *
     * @param model Solved linear meta model / 求解后的线性元模型
     * @return Length assignment modeling result, or null if no length variables exist / 长度分配建模结果，若无长度变量则为 null
    */
    fun extractResult(model: AbstractLinearMetaModel<Flt64>): LengthAssignmentModelingResult<V>? {
        if (!hasAny) return null

        val assignedLengths = ArrayList<ModeledAssignedLength<V>>()
        val overLengths = ArrayList<ModeledOverLength<V>>()

        for ((demandIndex, demand) in demands.withIndex()) {
            val assignedVar = assignedLength.getOrNull(demandIndex)
            if (assignedVar != null) {
                val doubleValue = model.tokens.find(assignedVar)?.doubleResult
                if (doubleValue != null && doubleValue >= 0.0) {
                    assignedLengths.add(ModeledAssignedLength(
                        productId = demand.product.id,
                        assignedLength = (convertSolverValue(demand.quantity.value, Flt64(doubleValue)) as Ok).value
                    ))
                }
            }

            val overVar = overLength.getOrNull(demandIndex)
            if (overVar != null) {
                val doubleValue = model.tokens.find(overVar)?.doubleResult
                if (doubleValue != null && doubleValue > 0.0) {
                    overLengths.add(ModeledOverLength(
                        productId = demand.product.id,
                        overLength = (convertSolverValue(demand.quantity.value, Flt64(doubleValue)) as Ok).value
                    ))
                }
            }
        }

        return LengthAssignmentModelingResult(
            assignedLengths = assignedLengths,
            overLengths = overLengths
        )
    }

}
