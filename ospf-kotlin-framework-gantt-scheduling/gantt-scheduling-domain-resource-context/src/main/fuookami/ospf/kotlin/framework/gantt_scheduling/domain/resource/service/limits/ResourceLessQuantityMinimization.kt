/** 资源下限数量最小化 / Resource less quantity minimization */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 资源下限数量最小化 / Resource less quantity minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param S 资源时间槽类型 / Resource time slot type
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param quantity 资源使用对象 / Resource usage object
 * @param threshold 阈值函数 / Threshold function
 * @param coefficient 成本系数函数 / Cost coefficient function
 * @param name 管道名称 / Pipeline name
*/
class ResourceLessQuantityMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        S : ResourceTimeSlot<R, C, V>,
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    private val quantity: ResourceUsage<S, R, C, V>,
    private val threshold: (S) -> Flt64 = { Flt64.zero },
    private val coefficient: (S) -> Flt64 = { Flt64.one },
    override val name: String = "resource_less_capacity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> where V : RealNumber<V>, V : NumberField<V> {
    private val slots = if (quantity.lessEnabled) {
        quantity.timeSlots.filter { it.resourceCapacity.lessEnabled }
    } else {
        emptyList()
    }

    /**
     * 向模型添加下限数量最小化目标 / Add less-quantity minimization objective to the model
     *
     * @param model 线性元模型 / Linear meta model
     * @return 成功与否 / Success or failure
    */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (slots.isNotEmpty()) {
            val cost = MutableLinearPolynomial(constant = Flt64.zero)
            for (slot in slots) {
                val thresholdValue = threshold(slot)
                val thisCoefficient = coefficient(slot)
                if (thresholdValue eq Flt64.zero) {
                    cost += LinearMonomial(thisCoefficient, quantity.lessQuantity[slot])
                } else {
                    val slack = resourceSlack(
                        x = quantity.lessQuantity[slot],
                        threshold = thresholdValue,
                        type = UContinuous,
                        withNegative = false,
                        withPositive = true,
                        name = "${quantity.name}_${slot}_${name}_over_quantity_threshold"
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
                    cost += LinearMonomial(thisCoefficient, slack)
                }
            }
            when (val result = model.minimize(
                polynomial = cost.toLinearPolynomial(),
                name = "${quantity.name} less quantity"
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
