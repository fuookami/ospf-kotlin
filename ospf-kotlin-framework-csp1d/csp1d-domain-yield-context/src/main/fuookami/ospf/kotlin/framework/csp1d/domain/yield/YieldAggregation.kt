package fuookami.ospf.kotlin.framework.csp1d.domain.yield

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Yield deviation aggregation root.
 * 产出偏差聚合根
 *
 * Manage yield slack variables (under_production_i, over_production_i),
 * replacing the YieldSlackVars inner class in Csp1dMilpSolver.
 * 管理 yield slack 变量（under_production_i, over_production_i），
 * 替代 Csp1dMilpSolver 中的 YieldSlackVars 内部类。
 *
 * @param V Numeric value type / 数值类型
 * @property config Yield modeling configuration / 产出建模配置
 * @property demands Demand list / 需求列表
 * @property needsOverSlackForOverArea Whether over-production slack is needed for over-production area penalty / 是否因超产面积惩罚需要超产 slack
*/
class YieldAggregation<V : RealNumber<V>>(
    val config: YieldModelingConfig<V>,
    val demands: List<ProductDemand<V>>,
    val needsOverSlackForOverArea: Boolean = false
) : Csp1dAggregation<V> {

    /** Under-production slack variables, indexed by demand; null when not needed / 中文欠产松弛变量，按 demand 索引；不需要时为 null */
    val underProduction: List<URealVar?> = demands.mapIndexed { demandIndex, demand ->
        val demandKey = demandShadowPriceKey(demand)
        if (config.underProductionPenalty.containsKey(demandKey)) {
            URealVar("under_production_$demandIndex")
        } else {
            null
        }
    }

    /** Over-production slack variables, indexed by demand; null when not needed / 中文超产松弛变量，按 demand 索引；不需要时为 null */
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

    /** Whether any slack variables exist / 中文是否存在任何 slack 变量 */
    val hasAny: Boolean get() = underProduction.any { it != null } || overProduction.any { it != null }

    override val cuttingPlans: List<CuttingPlan<V>> = emptyList()

    /**
     * Register to meta model.
     * 注册到元模型
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
     * Extract yield modeling result from solver solution.
     * 提取产出建模结果
     *
     * @param model Solved linear meta model / 求解后的线性元模型
     * @return Yield modeling result, or null if no slack variables exist / 产出建模结果，若无偏差变量则为 null
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
        /**
         * Generate demand shadow price key.
         * 生成需求影子价格键
         *
         * @param demand Product demand / 产品需求
         * @return Demand shadow price key / 需求影子价格键
        */
        internal fun demandShadowPriceKey(demand: ProductDemand<*>): ProductDemandShadowPriceKey {
            return ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.toString()
            )
        }
    }
}
