package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.policy

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.Route

/**
 * 定价列选择策略。 / Pricing column selector policy.
 *
 * 选择 reduced cost 小于 -pricingTolerance 的列，
 * 并截断到 maxColumnsPerPricing。
 *
 * Selects columns with reduced cost < -pricingTolerance,
 * truncating to maxColumnsPerPricing.
 */
fun interface PricingColumnSelector<V : RealNumber<V>> {

    /**
     * 选择要加入 RMP 的列。 / Select columns to add to the RMP.
     */
    fun select(columns: List<Route<V>>, maxColumns: Int): List<Route<V>>

    companion object {
        /** 默认选择策略：保留所有列（截断由 pricer 处理） / Default: keep all columns (truncation handled by pricer) */
        fun <V : RealNumber<V>> default() = PricingColumnSelector<V> { columns, _ -> columns }
    }
}
