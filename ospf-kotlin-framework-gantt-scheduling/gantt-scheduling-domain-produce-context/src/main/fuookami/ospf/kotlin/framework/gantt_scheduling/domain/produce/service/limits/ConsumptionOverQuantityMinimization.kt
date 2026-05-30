/** 消费超限数量最小化 / Consumption over quantity minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.AbstractMaterial
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Consumption
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.produceSlack
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel

/**
 * 消费超限数量最小化 / Consumption over quantity minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param C 材料类型 / Material type
 * @param products 产品与储备对列表 / List of product-reserve pairs
 * @param consumption 消费对象 / Consumption object
 * @param threshold 阈值函数 / Threshold function
 * @param coefficient 成本系数函数 / Cost coefficient function
 * @param name 管道名称 / Pipeline name
 */
class ConsumptionOverQuantityMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        C : AbstractMaterial
        >(
    products: List<Pair<C, MaterialReserves?>>,
    private val consumption: Consumption,
    private val threshold: (C) -> Flt64 = { Flt64.zero },
    private val coefficient: (C) -> Flt64 = { Flt64.one },
    override val name: String = "consumption_over_quantity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val materials = if (consumption.overEnabled) {
        products.filter { it.second?.overEnabled == true }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (materials.isNotEmpty()) {
            val cost = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            for ((material, _) in materials) {
                val thresholdValue = threshold(material)
                if (thresholdValue eq Flt64.zero) {
                    cost += LinearMonomial(coefficient(material), consumption.overQuantity[material])
                } else {
                    val slack = produceSlack(
                        x = consumption.overQuantity[material],
                        threshold = thresholdValue,
                        type = UContinuous,
                        withNegative = false,
                        withPositive = true,
                        name = "consumption_over_quantity_minimization_threshold_$material"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    cost += LinearMonomial(coefficient(material), slack)
                }
            }
            when (val result = model.minimize(
                polynomial = cost.toLinearPolynomial(),
                name = "consumption over quantity"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }
}
