/** 生产超限数量最小化 / Produce over quantity minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 生产超限数量最小化 / Produce over quantity minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 产品类型 / Product type
 * @param products 产品与需求对列表 / List of product-demand pairs
 * @param produce 生产对象 / Produce object
 * @param threshold 阈值函数 / Threshold function
 * @param coefficient 成本系数函数 / Cost coefficient function
 * @param name 管道名称 / Pipeline name
 */
class ProduceOverQuantityMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        V
        >(
    products: List<Pair<P, MaterialDemand<V>?>>,
    private val produce: Produce,
    private val threshold: (P) -> Flt64 = { Flt64.zero },
    private val coefficient: (P) -> Flt64 = { Flt64.one },
    override val name: String = "produce_over_quantity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> where V : RealNumber<V>, V : NumberField<V> {
    private val products = if (produce.overEnabled) {
        products.filter { it.second?.overEnabled == true }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (products.isNotEmpty()) {
            val cost = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            for ((product, _) in products) {
                val thresholdValue = threshold(product)
                if (thresholdValue eq Flt64.zero) {
                    cost += LinearMonomial(coefficient(product), produce.overQuantity[product])
                } else {
                    val slack = produceSlack(
                        x = produce.overQuantity[product],
                        threshold = thresholdValue,
                        type = UContinuous,
                        withNegative = false,
                        withPositive = true,
                        name = "produce_over_quantity_minimization_threshold_${product}"
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
            when (val result = model.minimize(
                polynomial = cost.toLinearPolynomial(),
                name = "produce over quantity"
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
