/**
 * 成本模型，支持可变和不可变成本 / Cost model supporting mutable and immutable costs
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error.GanttSchedulingLifecycleError
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/** 成本物理量 / Cost quantity */
typealias CostQuantity<V> = Quantity<V>

/**
 * 成本项，包含标签、值和消息 / Cost item containing tag, value, and message
 *
 * @param V 数值类型 / The numeric type
 * @property tag 标签 / The tag
 * @property costQuantity 成本物理量 / The cost quantity
 * @property message 消息 / The message
*/
data class CostItem<V : RealNumber<V>>(
    val tag: String,
    val costQuantity: CostQuantity<V>? = null,
    val message: String? = null
) : Copyable<CostItem<V>> {
    val valid get() = costQuantity != null

    /**
     * 成本项物理量 / Cost item as a physical quantity
     *
     * @param unit 成本单位 / Cost unit
     * @return 成本项物理量 / Cost item quantity
    */
    fun quantity(unit: PhysicalUnit = NoneUnit): CostQuantity<V>? {
        return costQuantity?.let { Quantity(it.value, unit) }
    }

    override fun copy() = CostItem(tag, costQuantity, message)
}

/**
 * 成本接口，支持迭代和复制 / Cost interface supporting iteration and copying
 *
 * @param V 数值类型 / The numeric type
 * @property items 成本项列表 / The list of cost items
 * @property costSum 成本总和物理量 / The sum cost quantity
*/
sealed interface Cost<V : RealNumber<V>> : Iterable<CostItem<V>>, Copyable<Cost<V>> {
    companion object {
        operator fun <V : RealNumber<V>> invoke(
            cost: MutableCost<V>
        ): ImmutableCost<V> {
            return ImmutableCost(
                items = cost.items.map { it.copy() },
                costSum = cost.costSum
            )
        }

        /**
         * 通过成本物理量创建不可变成本 / Create immutable cost from cost quantities
         *
         * @param V 数值类型 / The numeric type
         * @param items 成本项列表 / The list of cost items
         * @param costSum 成本总和物理量 / The sum cost quantity
         * @return 不可变成本 / Immutable cost
        */
        operator fun <V : RealNumber<V>> invoke(
            items: List<CostItem<V>>,
            costSum: CostQuantity<V>?
        ): ImmutableCost<V> {
            return ImmutableCost(
                items = items,
                costSum = costSum
            )
        }

        /**
         * 创建空成本 / Create an empty cost with zero sum
        */
        operator fun <V : RealNumber<V>> invoke(
            constants: RealNumberConstants<V>
        ): ImmutableCost<V> {
            return ImmutableCost(
                items = emptyList(),
                costSum = null
            )
        }
    }

    val items: List<CostItem<V>>
    val costSum: CostQuantity<V>?
    val valid: Boolean get() = costSum != null

    /**
     * 总成本物理量 / Sum cost as a physical quantity
     *
     * @param unit 成本单位 / Cost unit
     * @return 总成本物理量 / Sum cost quantity
    */
    fun sumQuantity(unit: PhysicalUnit = NoneUnit): CostQuantity<V>? {
        return costSum?.let { Quantity(it.value, unit) }
    }

    /**
     * 读取 solver 成本值（nullable） / Read cost as a solver value (nullable)
     *
     * @param default 成本缺失时使用的默认 solver 值 / Default solver value when cost is absent
     * @return solver 成本值，缺失时返回 default / Solver cost value, or default when absent
    */
    fun solverCostOrNull(default: Flt64? = null): Flt64? = costSum?.value?.toFlt64() ?: default

    /**
     * 读取 solver 成本值 / Read cost as a solver value
     *
     * @param default 成本缺失时使用的默认 solver 值 / Default solver value when cost is absent
     * @return solver 成本值 / Solver cost value
    */
    fun solverCost(default: Flt64? = null): Ret<Flt64> {
        val value = costSum?.value?.toFlt64() ?: default
        return if (value != null) {
            Ok(value)
        } else {
            Failed(GanttSchedulingLifecycleError("cost sum is required to build solver cost"))
        }
    }

/**
 * asMutable.
 * asMutable。
 * @return This cost as a MutableCost, or null if it is immutable / 将此成本转换为可变成本，不可变时返回null
*/
    fun asMutable(): MutableCost<V>? {
        return when (this) {
            is MutableCost -> {
                this
            }

            else -> {
                null
            }
        }
    }

    operator fun plus(rhs: CostItem<V>): ImmutableCost<V> {
        return ImmutableCost(items + rhs)
    }

    operator fun plus(rhs: Cost<V>): ImmutableCost<V> {
        return ImmutableCost(items + rhs.items)
    }

    override fun iterator() = items.iterator()
}

/**
 * 可变成本，支持动态添加成本项 / Mutable cost supporting dynamic addition of cost items
 *
 * @param V 数值类型 / The numeric type
 * @property constants 数值常量 / The numeric constants
 * @property items 成本项列表 / The list of cost items
 * @property costSum 成本总和物理量 / The sum cost quantity
*/
class MutableCost<V : RealNumber<V>>(
    val constants: RealNumberConstants<V>,
    override val items: MutableList<CostItem<V>> = ArrayList(),
    override var costSum: CostQuantity<V>? = if (items.isNotEmpty() && items.all { it.costQuantity != null }) {
        items.sumOf(constants) { it.costQuantity!!.value }
            .let { Quantity(it, NoneUnit) }
    } else {
        null
    }
) : Cost<V> {
    operator fun plusAssign(rhs: CostItem<V>) {
        if (!rhs.valid || rhs.costQuantity!!.value neq constants.zero) {
            items.add(rhs)
        }

        if (rhs.valid) {
            costSum = costSum?.value
                ?.plus(rhs.costQuantity!!.value)
                ?.let { Quantity(it, costSum?.unit ?: rhs.costQuantity.unit) }
        }
    }

    operator fun plusAssign(rhs: Cost<V>) {
        items.addAll(rhs.items.filter { !it.valid || it.costQuantity!!.value neq constants.zero })

        costSum = if (this.valid && rhs.valid) {
            Quantity(
                value = costSum!!.value + rhs.costSum!!.value,
                unit = costSum?.unit ?: rhs.costSum?.unit ?: NoneUnit
            )
        } else {
            null
        }
    }

    override fun copy(): MutableCost<V> {
        return MutableCost(
            constants = constants,
            items = items.map { it.copy() }.toMutableList(),
            costSum = costSum
        )
    }
}

/**
 * 不可变成本 / Immutable cost
 *
 * @param V 数值类型 / The numeric type
 * @property items 成本项列表 / The list of cost items
 * @property costSum 成本总和物理量 / The sum cost quantity
*/
data class ImmutableCost<V : RealNumber<V>>(
    override val items: List<CostItem<V>>,
    override val costSum: CostQuantity<V>? = if (items.isNotEmpty() && items.all { it.costQuantity != null }) {
        items.first().costQuantity!!.value.constants.let { constants ->
            items.sumOf(constants) { it.costQuantity!!.value }
                .let { Quantity(it, items.first().costQuantity?.unit ?: NoneUnit) }
        }
    } else {
        null
    }
) : Cost<V> {
    override fun copy(): ImmutableCost<V> {
        return ImmutableCost(
            items = items.map { it.copy() },
            costSum = costSum
        )
    }
}
