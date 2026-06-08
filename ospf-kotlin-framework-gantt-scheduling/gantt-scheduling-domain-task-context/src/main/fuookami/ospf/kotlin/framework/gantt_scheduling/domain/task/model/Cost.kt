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
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/** 成本物理量 / Cost quantity */
typealias CostQuantity<V> = Quantity<V>

/** Flt64 成本物理量兼容类型 / Flt64 cost quantity compatibility type */
typealias Flt64CostQuantity = CostQuantity<Flt64>

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
    @Deprecated(
        message = "Use the Quantity-typed primary constructor instead",
        replaceWith = ReplaceWith("CostItem(tag, value?.let { Quantity(it, NoneUnit) }, message)")
    )
    constructor(
        tag: String,
        value: V?,
        message: String? = null
    ) : this(
        tag = tag,
        costQuantity = value?.let { Quantity(it, NoneUnit) },
        message = message
    )

    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("costQuantity?.value")
    )
    val value: V? get() = costQuantity?.value
    val valid get() = value != null

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

        @Deprecated(
            message = "Use the Quantity-typed factory instead",
            replaceWith = ReplaceWith("Cost(items, costSum)")
        )
        operator fun <V : RealNumber<V>> invoke(
            items: List<CostItem<V>>,
            constants: RealNumberConstants<V>,
            sums: V? = if (items.isNotEmpty() && items.all { it.value != null }) {
                items.sumOf(constants) { it.value!! }
            } else {
                null
            }
        ): ImmutableCost<V> {
            return ImmutableCost(
                items = items,
                costSum = sums?.let { Quantity(it, NoneUnit) }
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
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("costSum?.value")
    )
    val sum: V? get() = costSum?.value
    val valid: Boolean get() = sum != null

    /**
     * 总成本物理量 / Sum cost as a physical quantity
     *
     * @param unit 成本单位 / Cost unit
     * @return 总成本物理量 / Sum cost quantity
     */
    fun sumQuantity(unit: PhysicalUnit = NoneUnit): CostQuantity<V>? {
        return costSum?.let { Quantity(it.value, unit) }
    }

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
    override var costSum: CostQuantity<V>? = if (items.isNotEmpty() && items.all { it.value != null }) {
        items.sumOf(constants) { it.value!! }
            .let { Quantity(it, NoneUnit) }
    } else {
        null
    }
) : Cost<V> {
    @Deprecated(
        message = "Use the Quantity-typed primary constructor instead",
        replaceWith = ReplaceWith("MutableCost(constants, items, sum?.let { Quantity(it, NoneUnit) })")
    )
    constructor(
        constants: RealNumberConstants<V>,
        items: MutableList<CostItem<V>> = ArrayList(),
        sum: V?
    ) : this(
        constants = constants,
        items = items,
        costSum = sum?.let { Quantity(it, NoneUnit) }
    )

    operator fun plusAssign(rhs: CostItem<V>) {
        if (!rhs.valid || rhs.value!! neq constants.zero) {
            items.add(rhs)
        }

        if (rhs.valid) {
            costSum = sum?.plus(rhs.value!!)?.let { Quantity(it, costSum?.unit ?: rhs.costQuantity?.unit ?: NoneUnit) }
        }
    }

    operator fun plusAssign(rhs: Cost<V>) {
        items.addAll(rhs.items.filter { !it.valid || it.value!! neq constants.zero })

        costSum = if (this.valid && rhs.valid) {
            Quantity(
                value = sum!! + rhs.sum!!,
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
    override val costSum: CostQuantity<V>? = if (items.isNotEmpty() && items.all { it.value != null }) {
        items.first().value!!.constants.let { constants ->
            items.sumOf(constants) { it.value!! }
                .let { Quantity(it, items.first().costQuantity?.unit ?: NoneUnit) }
        }
    } else {
        null
    }
) : Cost<V> {
    @Deprecated(
        message = "Use the Quantity-typed primary constructor instead",
        replaceWith = ReplaceWith("ImmutableCost(items, sum?.let { Quantity(it, NoneUnit) })")
    )
    constructor(
        items: List<CostItem<V>>,
        sum: V?
    ) : this(
        items = items,
        costSum = sum?.let { Quantity(it, NoneUnit) }
    )

    override fun copy(): ImmutableCost<V> {
        return ImmutableCost(
            items = items.map { it.copy() },
            costSum = costSum
        )
    }
}

// ── Flt64 向后兼容 typealias ──

@Deprecated("Use CostItem<Flt64> directly") typealias Flt64CostItem = CostItem<Flt64>
@Deprecated("Use Cost<Flt64> directly") typealias Flt64Cost = Cost<Flt64>
@Deprecated("Use MutableCost<Flt64> directly") typealias Flt64MutableCost = MutableCost<Flt64>
@Deprecated("Use ImmutableCost<Flt64> directly") typealias Flt64ImmutableCost = ImmutableCost<Flt64>
