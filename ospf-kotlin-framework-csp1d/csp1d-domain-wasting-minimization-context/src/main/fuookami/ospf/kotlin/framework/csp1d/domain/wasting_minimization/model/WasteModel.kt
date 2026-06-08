package fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 余宽浪费记录 / Rest width waste record for a cutting plan
 *
 * @param V 数值类型 / Numeric value type
 * @property plan 切割方案 / Cutting plan
 * @property restWidth 剩余幅宽 / Remaining width
 */
data class RestWidthWaste<V : RealNumber<V>>(
    val plan: CuttingPlan<V>,
    val restWidth: Quantity<V>
)

/**
 * 余料浪费记录 / Rest material waste record
 *
 * @param V 数值类型 / Numeric value type
 * @property plan 切割方案 / Cutting plan
 * @property restMaterial 余料面积代理（按业务单位）/ Rest material area proxy in business unit
 */
data class RestMaterialWaste<V : RealNumber<V>>(
    val plan: CuttingPlan<V>,
    val restMaterial: Quantity<V>
)

/**
 * 超产面积浪费 / Over-production area waste
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property wasteArea 浪费面积代理 / Waste area proxy
 */
data class OverProductionAreaWaste<V : RealNumber<V>>(
    val product: Product<V>,
    val wasteArea: Quantity<V>
)

/**
 * 浪费最小化目标 / Waste minimization objective
 *
 * @param V 数值类型 / Numeric value type
 */
sealed interface WasteMinimizationObjective<V : RealNumber<V>> {
    /**
     * 最小化余宽 / Minimize rest width
     *
     * @param V 数值类型 / Numeric value type
     * @property weight 目标权重 / Objective weight
     */
    data class MinimizeRestWidth<V : RealNumber<V>>(
        val weight: V
    ) : WasteMinimizationObjective<V>

    /**
     * 最小化余料 / Minimize rest material
     *
     * @param V 数值类型 / Numeric value type
     * @property weight 目标权重 / Objective weight
     */
    data class MinimizeRestMaterial<V : RealNumber<V>>(
        val weight: V
    ) : WasteMinimizationObjective<V>

    /**
     * 最小化成本 / Minimize cost
     *
     * @param V 数值类型 / Numeric value type
     * @property weight 目标权重 / Objective weight
     */
    data class MinimizeCost<V : RealNumber<V>>(
        val weight: V
    ) : WasteMinimizationObjective<V>

    /**
     * 最小化超产面积浪费 / Minimize over-production area waste
     *
     * @param V 数值类型 / Numeric value type
     * @property weight 目标权重 / Objective weight
     */
    data class MinimizeOverProductionArea<V : RealNumber<V>>(
        val weight: V
    ) : WasteMinimizationObjective<V>
}
