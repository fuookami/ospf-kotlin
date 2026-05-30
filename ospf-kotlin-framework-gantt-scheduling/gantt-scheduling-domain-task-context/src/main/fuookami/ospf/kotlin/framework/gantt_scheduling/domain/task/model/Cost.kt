@file:Suppress("DEPRECATION")

/**
 * 成本模型，支持可变和不可变成本 / Cost model supporting mutable and immutable costs
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 成本项，包含标签、值和消息 / Cost item containing tag, value, and message
 *
 * @property tag 标签 / The tag
 * @property value 值 / The value
 * @property message 消息 / The message
 */
data class CostItem(
    val tag: String,
    val value: Flt64? = null,
    val message: String? = null
) : Copyable<CostItem> {
    val valid get() = value != null

    override fun copy() = CostItem(tag, value, message)
}

/**
 * 成本接口，支持迭代和复制 / Cost interface supporting iteration and copying
 */
sealed interface Cost : Iterable<CostItem>, Copyable<Cost> {
    companion object {
        operator fun invoke(
            cost: MutableCost
        ): ImmutableCost {
            return ImmutableCost(cost.items.map { it.copy() }, cost.sum)
        }

        operator fun invoke(
            items: List<CostItem> = emptyList(),
            sums: Flt64? = if (items.all { it.value != null }) {
                items.sumOf { it.value!! }
            } else {
                null
            }
        ): ImmutableCost {
            return ImmutableCost(items, sums)
        }
    }

    val items: List<CostItem>
    val sum: Flt64?
    val valid: Boolean get() = sum != null

    fun asMutable(): MutableCost? {
        return when (this) {
            is MutableCost -> {
                this
            }

            else -> {
                null
            }
        }
    }

    operator fun plus(rhs: CostItem): ImmutableCost {
        return ImmutableCost(items + rhs)
    }

    operator fun plus(rhs: Cost): ImmutableCost {
        return ImmutableCost(items + rhs.items)
    }

    override fun iterator() = items.iterator()
}

/**
 * 可变成本，支持动态添加成本项 / Mutable cost supporting dynamic addition of cost items
 *
 * @property items 成本项列表 / The list of cost items
 * @property sum 成本总和 / The sum of costs
 */
class MutableCost(
    override val items: MutableList<CostItem> = ArrayList(),
    override var sum: Flt64? = if (items.all { it.value != null }) {
        items.sumOf { it.value!! }
    } else {
        null
    }
) : Cost {
    operator fun plusAssign(rhs: CostItem) {
        if (!rhs.valid || rhs.value!! neq Flt64.zero) {
            items.add(rhs)
        }

        if (rhs.valid) {
            sum = sum?.plus(rhs.value!!)
        }
    }

    operator fun plusAssign(rhs: Cost) {
        items.addAll(rhs.items.filter { !it.valid || it.value!! neq Flt64.zero })

        sum = if (this.valid && rhs.valid) {
            sum!! + rhs.sum!!
        } else {
            null
        }
    }

    override fun copy(): MutableCost {
        return MutableCost(items.map { it.copy() }.toMutableList(), sum)
    }
}

/**
 * 不可变成本 / Immutable cost
 *
 * @property items 成本项列表 / The list of cost items
 * @property sum 成本总和 / The sum of costs
 */
data class ImmutableCost(
    override val items: List<CostItem>,
    override val sum: Flt64? = if (items.all { it.value != null }) {
        items.sumOf { it.value!! }
    } else {
        null
    }
) : Cost {
    override fun copy(): ImmutableCost {
        return ImmutableCost(items.map { it.copy() }, sum)
    }
}


