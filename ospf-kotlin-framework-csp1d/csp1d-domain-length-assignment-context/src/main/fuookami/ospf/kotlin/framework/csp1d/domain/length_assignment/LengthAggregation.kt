package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.ModeledAssignedLength
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.ModeledOverLength

/**
 * 长度分配聚合根 / Length assignment aggregation root
 *
 * 管理动态卷长变量（assigned_length_i）和超长松弛变量（over_length_i），
 * 替代 Csp1dMilpSolver 中的 LengthSlackVars 内部类。
 *
 * @param V 数值类型 / Numeric value type
 * @property config 长度分配建模配置 / Length assignment modeling configuration
 * @property demands 需求列表 / Demand list
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
     * 提取长度分配建模结果 / Extract length assignment modeling result
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
                        assignedLength = solverValueLike(demand.quantity.value, Flt64(doubleValue))
                    ))
                }
            }

            val overVar = overLength.getOrNull(demandIndex)
            if (overVar != null) {
                val doubleValue = model.tokens.find(overVar)?.doubleResult
                if (doubleValue != null && doubleValue > 0.0) {
                    overLengths.add(ModeledOverLength(
                        productId = demand.product.id,
                        overLength = solverValueLike(demand.quantity.value, Flt64(doubleValue))
                    ))
                }
            }
        }

        return LengthAssignmentModelingResult(
            assignedLengths = assignedLengths,
            overLengths = overLengths
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> solverValueLike(sample: V, value: Flt64): V {
        return when (sample) {
            is Flt64 -> value as V
            is FltX -> value.toFltX() as V
            else -> throw IllegalArgumentException("Unsupported RealNumber type: ${sample::class}")
        }
    }
}
