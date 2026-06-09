@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

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
        C : AbstractMaterial,
        V
        >(
    products: List<Pair<P, MaterialDemand<V>?>>,
    protected val actions: List<A>,
    protected val slots: List<TimeSlot>,
    protected val timeWindow: TimeWindow<V>
) : AbstractProduce<
        ProductionTask<Executor, AssignmentPolicy<Executor>, P, C, V>,
        Executor,
        AssignmentPolicy<Executor>,
        P,
        C,
        V
        >(products.sortedBy { it.first.index }) where V : RealNumber<V>, V : NumberField<V> {

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    final override var quantity: LinearExpressionSymbols1<Flt64> = LinearExpressionSymbols1<Flt64>(
        name = "produce_quantity",
        shape = Shape1(products.size)
    ) { p, _ ->
        val (product, _) = products[p]
        LinearExpressionSymbol(
            Flt64,
            name = "produce_quantity_${product}"
        )
    }

    init {
        for ((product, demand) in products) {
            if (demand != null) {
                quantity[product].range.set(demand.solverValueRange())
            }
        }
    }

    /**
     * 注册变量到模型
     * Register variables to model
     */
    abstract fun register(model: LinearMetaModel<Flt64>): Try

    /**
     * 将 quantity 变量添加到模型
     * Add quantity variables to model
     */
    protected fun addQuantityToModel(model: LinearMetaModel<Flt64>): Try {
        if (products.isNotEmpty()) {
            when (val result = model.add(quantity)) {
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
