package fuookami.ospf.kotlin.framework.csp1d.domain.yield

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*

/**
 * 产出偏差聚合根 / Yield deviation aggregation root
 *
 * 管理 yield slack 变量（under_production_i, over_production_i），
 * 替代 Csp1dMilpSolver 中的 YieldSlackVars 内部类。
 *
 * Manage yield slack variables (under_production_i, over_production_i),
 * replacing the YieldSlackVars inner class in Csp1dMilpSolver.
 *
 * @param V 数值类型 / Numeric value type
 * @property config 产出建模配置 / Yield modeling configuration
 * @property demands 需求列表 / Demand list
 * @property needsOverSlackForOverArea 是否因超产面积惩罚需要超产 slack / Whether over-production slack is needed for over-production area penalty
 */
class YieldAggregation<V : RealNumber<V>>(
    val config: YieldModelingConfig<V>,
    val demands: List<ProductDemand<V>>,
    val needsOverSlackForOverArea: Boolean = false
) : Csp1dAggregation<V> {

    /** 欠产松弛变量，按 demand 索引；不需要时为 null / Under-production slack variables, indexed by demand; null when not needed */
    val underProduction: List<URealVar?> = demands.mapIndexed { demandIndex, demand ->
        val demandKey = demandShadowPriceKey(demand)
        if (config.underProductionPenalty.containsKey(demandKey)) {
            URealVar("under_production_$demandIndex")
        } else {
            null
        }
    }

    /** 超产松弛变量，按 demand 索引；不需要时为 null / Over-production slack variables, indexed by demand; null when not needed */
    val overProduction: List<URealVar?> = demands.mapIndexed { demandIndex, demand ->
        val demandKey = demandShadowPriceKey(demand)
        val needsOverSlack = config.overProductionPenalty.containsKey(demandKey) ||
                config.overProductionUpperBound.containsKey(demandKey) ||
                needsOverSlackForOverArea
        if (needsOverSlack) {
            URealVar("over_production_$demandIndex")
        } else {
            null
        }
    }

    /** 是否存在任何 slack 变量 / Whether any slack variables exist */
    val hasAny: Boolean get() = underProduction.any { it != null } || overProduction.any { it != null }

    override val cuttingPlans: List<CuttingPlan<V>> = emptyList()

    /**
     * 注册到元模型 / Register to meta model
     */
    override fun register(model: LinearMetaModel<Flt64>): Try {
        for (var_ in underProduction.filterNotNull()) {
            when (val result = model.add(var_)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (var_ in overProduction.filterNotNull()) {
            when (val result = model.add(var_)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 提取产出建模结果 / Extract yield modeling result
     */
    fun extractResult(model: AbstractLinearMetaModel<Flt64>): YieldModelingResult<V>? {
        val underProductions = ArrayList<ModeledUnderProduction<V>>()
        val overProductions = ArrayList<ModeledOverProduction<V>>()

        for ((demandIndex, demand) in demands.withIndex()) {
            val underVar = underProduction.getOrNull(demandIndex)
            if (underVar != null) {
                val doubleValue = model.tokens.find(underVar)?.doubleResult
                if (doubleValue != null && doubleValue > 0.0) {
                    underProductions.add(ModeledUnderProduction(
                        productId = demand.product.id,
                        unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.toString(),
                        amount = (convertSolverValue(demand.quantity.value, Flt64(doubleValue)) as Ok).value
                    ))
                }
            }

            val overVar = overProduction.getOrNull(demandIndex)
            if (overVar != null) {
                val doubleValue = model.tokens.find(overVar)?.doubleResult
                if (doubleValue != null && doubleValue > 0.0) {
                    overProductions.add(ModeledOverProduction(
                        productId = demand.product.id,
                        unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.toString(),
                        amount = (convertSolverValue(demand.quantity.value, Flt64(doubleValue)) as Ok).value
                    ))
                }
            }
        }

        return YieldModelingResult(
            underProductions = underProductions,
            overProductions = overProductions
        )
    }

    companion object {
        internal fun demandShadowPriceKey(demand: ProductDemand<*>): ProductDemandShadowPriceKey {
            return ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.toString()
            )
        }
    }
}
