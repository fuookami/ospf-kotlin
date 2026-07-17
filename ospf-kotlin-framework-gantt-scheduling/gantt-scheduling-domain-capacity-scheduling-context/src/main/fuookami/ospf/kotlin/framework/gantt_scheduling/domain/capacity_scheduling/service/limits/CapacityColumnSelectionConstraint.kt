package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 执行器-时隙产能列选择影子价格键
 * Executor-slot capacity-column selection shadow-price key
 *
 * @property executor 执行器 / Executor
 * @property slot 时隙 / Time slot
 */
data class CapacityColumnSelectionShadowPriceKey<E : Executor>(
    val executor: E,
    val slot: TimeSlot
) : ShadowPriceKey(CapacityColumnSelectionShadowPriceKey::class)

/**
 * 每个执行器在每个时隙恰好选择一条产能列
 * Select exactly one capacity column for every executor and slot
 *
 * @property executors 执行器列表 / Executors
 * @property slots 时隙列表 / Time slots
 * @property compilation 迭代产能列编译模型 / Iterative capacity-column compilation
 * @property groups 需要 exactly-one 的有效执行器-时隙组 / Active executor-slot groups requiring exactly-one selection
 */
class CapacityColumnSelectionConstraint<
        V : RealNumber<V>,
        E : Executor,
        A : ProductionAction
        >(
    private val executors: List<E>,
    private val slots: List<TimeSlot>,
    private val compilation: IterativeCapacityCompilation<V, E, A>,
    private val groups: Set<Pair<E, TimeSlot>> = executors
        .flatMap { executor -> slots.map { slot -> executor to slot } }
        .toSet()
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override val name: String = "capacity_column_selection"

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (executor in executors) {
            for (slot in slots) {
                if (executor to slot !in groups) {
                    continue
                }
                val expression = compilation.executorSlotCompilation.getValue(executor to slot)
                when (val result = model.addConstraint(
                    expression eq 1,
                    name = "${name}_${executor.id}_${slots.indexOf(slot)}",
                    args = CapacityColumnSelectionShadowPriceKey(executor, slot)
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    /**
     * 从 LP 对偶解提取强类型时隙选列影子价格
     * Extract typed slot-selection shadow prices from an LP dual solution
     *
     * @param model 线性元模型 / Linear meta model
     * @param shadowPrices 元对偶解 / Meta dual solution
     * @return 按强类型键索引的影子价格 / Shadow prices indexed by typed keys
     */
    fun extractShadowPrices(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Map<CapacityColumnSelectionShadowPriceKey<E>, Flt64> {
        val result = LinkedHashMap<CapacityColumnSelectionShadowPriceKey<E>, Flt64>()
        for (constraint in model.constraintsOfGroup(this)) {
            @Suppress("UNCHECKED_CAST")
            val key = constraint.args as? CapacityColumnSelectionShadowPriceKey<E> ?: continue
            val price = shadowPrices.constraints[constraint] ?: continue
            result[key] = (result[key] ?: Flt64.zero) + price
        }
        return result
    }
}
