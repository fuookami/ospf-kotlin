package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 产能调度场景的产品产量管理抽象基类
 * Abstract base class for produce management in Capacity Scheduling
 *
 * 提供产能调度场景下产品产量计算的通用框架
 * Provides a common framework for product quantity calculation in capacity scheduling scenarios
 */
abstract class CapacitySchedulingProduce<
    A : ProductionAction,
    P : AbstractMaterial,
    C : AbstractMaterial
>(
    products: List<Pair<P, MaterialDemand?>>,
    protected val actions: List<A>,
    protected val slots: List<TimeSlot>,
    protected val timeWindow: TimeWindow
) : AbstractProduce<
    ProductionTask<Executor, AssignmentPolicy<Executor>, P, C>,
    Executor,
    AssignmentPolicy<Executor>,
    P,
    C
>(products.sortedBy { it.first.index }) {

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    final override var quantity: LinearExpressionSymbols1 = LinearExpressionSymbols1(
        name = "produce_quantity",
        shape = Shape1(products.size)
    ) { p, _ ->
        val (product, _) = products[p]
        LinearExpressionSymbol(
            name = "produce_quantity_${product}"
        )
    }

    init {
        for ((product, demand) in products) {
            if (demand != null) {
                quantity[product].range.set(
                    ValueRange(
                        demand.quantity.lowerBound.value.unwrap() - (demand.lessQuantity ?: Flt64.zero),
                        demand.quantity.upperBound.value.unwrap() + (demand.overQuantity ?: Flt64.zero)
                    ).value!!
                )
            }
        }
    }

    /**
     * 注册变量到模型
     * Register variables to model
     */
    abstract fun register(model: LinearMetaModel): Try

    /**
     * 将 quantity 变量添加到模型
     * Add quantity variables to model
     */
    protected fun addQuantityToModel(model: LinearMetaModel): Try {
        if (products.isNotEmpty()) {
            when (val result = model.add(quantity)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        return ok
    }
}
