/** 生产数量最大化 / Produce quantity maximization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.AbstractMaterial
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.produceSlack
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel

/**
 * 生产数量最大化 / Produce quantity maximization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 产品类型 / Product type
 * @param products 产品列表 / List of products
 * @param produce 生产对象 / Produce object
 * @param threshold 阈值函数 / Threshold function
 * @param coefficient 成本系数函数 / Cost coefficient function
 * @param name 管道名称 / Pipeline name
 */
class ProduceQuantityMaximization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial
        >(
    private val products: List<P>,
    private val produce: Produce,
    private val threshold: (P) -> Flt64 = { Flt64.zero },
    private val coefficient: (P) -> Flt64 = { Flt64.one },
    override val name: String = "produce_quantity_maximization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val cost = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        for (product in products) {
            val thresholdValue = threshold(product)
            if (thresholdValue eq Flt64.zero) {
                cost += LinearMonomial(coefficient(product), produce.quantity[product])
            } else {
                val slack = produceSlack(
                    x = produce.quantity[product],
                    threshold = thresholdValue,
                    type = UContinuous,
                    withNegative = false,
                    withPositive = true,
                    name = "produce_quantity_maximization_threshold_$product"
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
                cost += LinearMonomial(coefficient(product), slack)
            }
        }
        when (val result = model.maximize(
            polynomial = cost.toLinearPolynomial(),
            name = "produce quantity"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        return ok
    }
}
