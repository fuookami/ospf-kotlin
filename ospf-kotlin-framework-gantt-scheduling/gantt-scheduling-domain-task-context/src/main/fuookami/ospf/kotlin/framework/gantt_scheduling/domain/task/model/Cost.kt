@file:Suppress("DEPRECATION")

/**
 * 成本模型，支持可变和不可变成本 / Cost model supporting mutable and immutable costs
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.ArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 成本项，包含标签、值和消息 / Cost item containing tag, value, and message
 *
 * @param V 数值类型 / The numeric type
 * @property tag 标签 / The tag
 * @property value 值 / The value
 * @property message 消息 / The message
 */
data class CostItem<V : RealNumber<V>>(
    val tag: String,
    val value: V? = null,
    val message: String? = null
) : Copyable<CostItem<V>> {
    val valid get() = value != null

    override fun copy() = CostItem(tag, value, message)
}

/**
 * 成本接口，支持迭代和复制 / Cost interface supporting iteration and copying
 *
 * @param V 数值类型 / The numeric type
 */
sealed interface Cost<V : RealNumber<V>> : Iterable<CostItem<V>>, Copyable<Cost<V>> {
    companion object {
        operator fun <V : RealNumber<V>> invoke(
            cost: MutableCost<V>
        ): ImmutableCost<V> {
            return ImmutableCost(cost.items.map { it.copy() }, cost.sum)
        }

        operator fun <V : RealNumber<V>> invoke(
            items: List<CostItem<V>>,
            constants: RealNumberConstants<V>,
            sums: V? = if (items.isNotEmpty() && items.all { it.value != null }) {
                items.sumOf(constants) { it.value!! }
            } else {
                null
            }
        ): ImmutableCost<V> {
            return ImmutableCost(items, sums)
        }

        /**
         * 创建空成本 / Create an empty cost with zero sum
         */
        operator fun <V : RealNumber<V>> invoke(
            constants: RealNumberConstants<V>
        ): ImmutableCost<V> {
            return ImmutableCost(emptyList(), null)
        }
    }

    val items: List<CostItem<V>>
    val sum: V?
    val valid: Boolean get() = sum != null

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
 * @property items 成本项列表 / The list of cost items
 * @property sum 成本总和 / The sum of costs
 */
class MutableCost<V : RealNumber<V>>(
    val constants: RealNumberConstants<V>,
    override val items: MutableList<CostItem<V>> = ArrayList(),
    override var sum: V? = if (items.isNotEmpty() && items.all { it.value != null }) {
        items.sumOf(constants) { it.value!! }
    } else {
        null
    }
) : Cost<V> {
    operator fun plusAssign(rhs: CostItem<V>) {
        if (!rhs.valid || rhs.value!! neq constants.zero) {
            items.add(rhs)
        }

        if (rhs.valid) {
            sum = sum?.plus(rhs.value!!)
        }
    }

    operator fun plusAssign(rhs: Cost<V>) {
        items.addAll(rhs.items.filter { !it.valid || it.value!! neq constants.zero })

        sum = if (this.valid && rhs.valid) {
            sum!! + rhs.sum!!
        } else {
            null
        }
    }

    override fun copy(): MutableCost<V> {
        return MutableCost(constants, items.map { it.copy() }.toMutableList(), sum)
    }
}

/**
 * 不可变成本 / Immutable cost
 *
 * @param V 数值类型 / The numeric type
 * @property items 成本项列表 / The list of cost items
 * @property sum 成本总和 / The sum of costs
 */
data class ImmutableCost<V : RealNumber<V>>(
    override val items: List<CostItem<V>>,
    override val sum: V? = if (items.isNotEmpty() && items.all { it.value != null }) {
        items.first().value!!.constants.let { constants ->
            items.sumOf(constants) { it.value!! }
        }
    } else {
        null
    }
) : Cost<V> {
    override fun copy(): ImmutableCost<V> {
        return ImmutableCost(items.map { it.copy() }, sum)
    }
}

// ── Flt64 向后兼容 typealias ──

typealias Flt64CostItem = CostItem<Flt64>
typealias Flt64Cost = Cost<Flt64>
typealias Flt64MutableCost = MutableCost<Flt64>
typealias Flt64ImmutableCost = ImmutableCost<Flt64>
