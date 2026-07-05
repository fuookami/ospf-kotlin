/**
 * 需求统计模型。
 * Demand statistics model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * BPP3D 需求模式，区分货物/物料及离散/连续域。
 * BPP3D demand mode, distinguishing item/material and discrete/continuous domain.
 */
sealed interface Bpp3dDemandMode {
    /** 货物需求 / Item demand */
    object Item : Bpp3dDemandMode
    /** 物料需求 / Material demand */
    object Material : Bpp3dDemandMode
    /** 货物数量需求 / Item amount demand */
    object ItemAmount : Bpp3dDemandMode
    /** 货物重量需求 / Item weight demand */
    object ItemWeight : Bpp3dDemandMode
    /** 物料数量需求 / Item material amount demand */
    object ItemMaterialAmount : Bpp3dDemandMode
    /** 物料重量需求 / Item material weight demand */
    object ItemMaterialWeight : Bpp3dDemandMode
}

/**
 * 将抽象需求模式转换为具体的离散/连续模式。
 * Convert abstract demand mode to concrete discrete/continuous mode.
 *
 * @param isDiscrete 是否为离散域 / whether the domain is discrete
 * @return 具体需求模式 / concrete demand mode
 */
fun Bpp3dDemandMode.toConcreteMode(isDiscrete: Boolean): Bpp3dDemandMode {
    return when (this) {
        is Bpp3dDemandMode.Item -> if (isDiscrete) Bpp3dDemandMode.ItemAmount else Bpp3dDemandMode.ItemWeight
        is Bpp3dDemandMode.Material -> if (isDiscrete) Bpp3dDemandMode.ItemMaterialAmount else Bpp3dDemandMode.ItemMaterialWeight
        else -> this
    }
}

/**
 * BPP3D 需求键，标识需求针对的货物或物料。
 * BPP3D demand key, identifying the item or material targeted by the demand.
 *
 * @property item 货物实例 / item instance
 * @property material 物料键 / material key
 */
sealed interface Bpp3dDemandKey {
    /** 货物需求键 / Item demand key */
    data class Item(val item: fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item) : Bpp3dDemandKey
    /** 物料需求键 / Material demand key */
    data class Material(val material: MaterialKey) : Bpp3dDemandKey
}

/**
 * 根据需求键类型转换为具体需求模式。
 * Convert to concrete demand mode based on demand key type.
 *
 * @param isDiscrete 是否为离散域 / whether the domain is discrete
 * @return 具体需求模式 / concrete demand mode
 */
fun Bpp3dDemandKey.toConcreteMode(isDiscrete: Boolean): Bpp3dDemandMode {
    return when (this) {
        is Bpp3dDemandKey.Item -> if (isDiscrete) Bpp3dDemandMode.ItemAmount else Bpp3dDemandMode.ItemWeight
        is Bpp3dDemandKey.Material -> if (isDiscrete) Bpp3dDemandMode.ItemMaterialAmount else Bpp3dDemandMode.ItemMaterialWeight
    }
}

/**
 * 根据需求键和离散/连续标志转换为具体需求模式。
 * Convert to concrete demand mode based on demand key and discrete/continuous flag.
 *
 * @param key 需求键 / demand key
 * @param isDiscrete 是否为离散域 / whether the domain is discrete
 * @return 具体需求模式 / concrete demand mode
 */
fun Bpp3dDemandMode.toConcreteMode(
    key: Bpp3dDemandKey,
    isDiscrete: Boolean
): Bpp3dDemandMode {
    val concreteByMode = toConcreteMode(isDiscrete)
    return when (key) {
        is Bpp3dDemandKey.Item -> when (concreteByMode) {
            is Bpp3dDemandMode.ItemAmount,
            /** 需求量值 / Demand amount value */
            is Bpp3dDemandMode.ItemWeight -> concreteByMode

            else -> key.toConcreteMode(isDiscrete)
        }

        is Bpp3dDemandKey.Material -> when (concreteByMode) {
            is Bpp3dDemandMode.ItemMaterialAmount,
            is Bpp3dDemandMode.ItemMaterialWeight -> concreteByMode

            else -> key.toConcreteMode(isDiscrete)
        }
    }
}

/**
 * BPP3D 需求值，表示数量或重量。
 * BPP3D demand value, representing amount or weight.
 */
sealed interface Bpp3dDemandValue {
    /** 数量需求值 / Amount demand value */
    data class Amount(val value: UInt64) : Bpp3dDemandValue
    /** 重量需求值 / Weight demand value */
    data class Weight(val value: Quantity<FltX>) : Bpp3dDemandValue
}

/**
 * 合并需求值。
 * Merge demand values.
 *
 * @param lhs 左侧需求值 / left-hand side demand value
 * @param rhs 右侧需求值 / right-hand side demand value
 * @return 合并后的需求值或错误 / merged demand value or error
 */
private fun mergeDemandValue(lhs: Bpp3dDemandValue, rhs: Bpp3dDemandValue): Ret<Bpp3dDemandValue> {
    return when {
        lhs is Bpp3dDemandValue.Amount && rhs is Bpp3dDemandValue.Amount -> ok(Bpp3dDemandValue.Amount(lhs.value + rhs.value))
        lhs is Bpp3dDemandValue.Weight && rhs is Bpp3dDemandValue.Weight -> ok(Bpp3dDemandValue.Weight(lhs.value + rhs.value))
        else -> Failed(ErrorCode.IllegalArgument, "Incompatible demand values: $lhs vs $rhs")
    }
}

/**
 * 按倍数缩放需求值。
 * Scale demand value by a multiplier.
 *
 * @param value 原始需求值 / original demand value
 * @param multiplier 缩放倍数 / scaling multiplier
 * @return 缩放后的需求值 / scaled demand value
 */
private fun scaleDemandValue(value: Bpp3dDemandValue, multiplier: UInt64): Bpp3dDemandValue {
    return when (value) {
        is Bpp3dDemandValue.Amount -> Bpp3dDemandValue.Amount(value.value * multiplier)
        is Bpp3dDemandValue.Weight -> Bpp3dDemandValue.Weight(value.value * FltX(multiplier.toULong().toDouble()))
    }
}

/**
 * 合并需求到可变映射中。
 * Merge a demand entry into a mutable map.
 *
 * @param key 需求键 / demand key
 * @param value 需求值 / demand value
 */
private fun MutableMap<Bpp3dDemandKey, Bpp3dDemandValue>.mergeDemand(
    this[key] = this[key]?.let { mergeDemandValue(it, value).value!! } ?: value
}

/**
 * 合并一组需求到可变映射中。
 * Merge a collection of demands into a mutable map.
 *
 * @param values 需求键值对映射 / mapping of demand key-value pairs
 */
private fun MutableMap<Bpp3dDemandKey, Bpp3dDemandValue>.mergeDemand(
    values: Map<Bpp3dDemandKey, Bpp3dDemandValue>
) {
    for ((key, value) in values) {
        mergeDemand(key, value)
    }
}

/**
 * 按倍数缩放需求映射。
 * Scale a demand map by a multiplier.
 *
 * @param multiplier 缩放倍数 / scaling multiplier
 * @return 缩放后的需求映射 / scaled demand map
 */
private fun Map<Bpp3dDemandKey, Bpp3dDemandValue>.scale(
    multiplier: UInt64
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    if (multiplier == UInt64.zero) {
        return emptyMap()
    }
    return mapValues { (_, value) -> scaleDemandValue(value, multiplier) }
}

/**
 * 统计任意单位在指定需求模式下的需求分布。
 * Calculate demand distribution of an arbitrary unit under the specified demand mode.
 *
 * @param unit 货物、容器或投影等单元 / unit such as item, container or projection
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
private fun statisticsOf(
    unit: Any,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (unit) {
        is Item -> unit.statistics(mode)
        is ItemContainer<*> -> unit.statistics(mode)
        is Container3<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (unit as Container3<*, FltX>).statistics(mode)
        }
        is Container2<*, *, *> -> {
            @Suppress("UNCHECKED_CAST")
            (unit as Container2<*, FltX, *>).statistics(mode)
        }
        else -> emptyMap()
    }
}

/**
 * 统计货物在指定需求模式下的需求分布。
 * Calculate demand distribution of an item under the specified demand mode.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun Item.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (mode) {
        is Bpp3dDemandMode.Item -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Amount(UInt64.one)
        )

        is Bpp3dDemandMode.Material -> materialAmounts
            .mapKeys { (key, _) -> Bpp3dDemandKey.Material(key) }
            .mapValues { (_, value) -> Bpp3dDemandValue.Amount(value) }

        is Bpp3dDemandMode.ItemAmount -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Amount(UInt64.one)
        )

        is Bpp3dDemandMode.ItemWeight -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Weight(weight)
        )

        is Bpp3dDemandMode.ItemMaterialAmount -> materialAmounts
            .mapKeys { (key, _) -> Bpp3dDemandKey.Material(key) }
            .mapValues { (_, value) -> Bpp3dDemandValue.Amount(value) }

        is Bpp3dDemandMode.ItemMaterialWeight -> materialWeights
            .mapKeys { (key, _) -> Bpp3dDemandKey.Material(key) }
            .mapValues { (_, value) -> Bpp3dDemandValue.Weight(value) }
    }
}

/**
 * 统计货物在指定需求模式和数量下的需求分布。
 * Calculate demand distribution of an item under the specified demand mode and amount.
 *
 * @param mode 需求模式 / demand mode
 * @param amount 货物数量 / item amount
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun Item.statistics(
    mode: Bpp3dDemandMode,
    amount: UInt64
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return statistics(mode).scale(amount)
}

/**
 * 统计货物视图的需求分布。
 * Calculate demand distribution of an item view.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun ItemView.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return unit.statistics(mode)
}

/**
 * 统计二维放置的需求分布。
 * Calculate demand distribution of a 2D placement.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun QuantityPlacement2<*, FltX, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in toPlacement3()) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

/**
 * 统计三维放置的需求分布。
 * Calculate demand distribution of a 3D placement.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun QuantityPlacement3<*, FltX>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return statisticsOf(unit, mode)
}

/**
 * 统计投影的需求分布。
 * Calculate demand distribution of a projection.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun Projection<*, FltX, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (this) {
        is PlaneProjection<*, FltX, *> -> statisticsOf(unit, mode)
        is PileProjection<*, FltX, *> -> statisticsOf(unit, mode).scale(layer)
        is MultiPileProjection<*, FltX, *> -> {
            val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
            for (view in views) {
                counter.mergeDemand(statisticsOf(view.unit, mode))
            }
            counter
        }
    }
}

/**
 * 统计二维容器的需求分布。
 * Calculate demand distribution of a 2D container.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun Container2<*, FltX, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in units) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

/**
 * 统计三维容器的需求分布。
 * Calculate demand distribution of a 3D container.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun Container3<*, FltX>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in units) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

/**
 * 统计货物容器的需求分布。
 * Calculate demand distribution of an item container.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun ItemContainer<*>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return (this as Container3<*, FltX>).statistics(mode)
}

/**
 * 统计三维放置集合的需求分布。
 * Calculate demand distribution of a collection of 3D placements.
 *
 * @param mode 需求模式 / demand mode
 * @return 需求键到需求值的映射 / mapping from demand key to demand value
 */
fun Iterable<QuantityPlacement3<*, FltX>>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in this) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

/**
 * 创建零重量需求值。
 * Create a zero-weight demand value.
 *
 * @return 零重量需求值 / zero-weight demand value
 */
fun noWeightDemandValue(): Bpp3dDemandValue.Weight = Bpp3dDemandValue.Weight(FltX.zero * Kilogram)
