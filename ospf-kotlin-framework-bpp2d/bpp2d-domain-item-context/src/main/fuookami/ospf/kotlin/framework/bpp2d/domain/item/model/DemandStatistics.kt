/**
 * 需求统计模型。 / Demand statistics model.
 *
 * 对应 BPP3D 的 `DemandStatistics`：需求模式区分物品/物料与离散/连续域，需求值区分件数与面积。
 * 二维侧没有重量体积之分，连续域用实际占用面积表达。
 * / Mirrors BPP3D `DemandStatistics`: the demand mode distinguishes item/material and discrete/continuous
 * domains, while the demand value is either an amount or an area. 2D uses occupied area for the continuous
 * domain.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 需求域：离散件数或连续面积。 / Demand domain: discrete amounts or continuous areas.
*/
enum class Bpp2dDemandDomain {
    /** 离散域 / discrete */
    Discrete,

    /** 连续域 / continuous
    */
    Continuous
}

/**
 * 需求模式。 / Demand mode.
 *
 * @param V 数值类型 / numeric type
*/
sealed interface Bpp2dDemandMode<V : FloatingNumber<V>> {

    /** 物品需求 / item demand */
    data class Item<V : FloatingNumber<V>>(val item: AbstractItem<V>) : Bpp2dDemandMode<V>

    /** 物品件数需求 / item amount demand */
    data class ItemAmount<V : FloatingNumber<V>>(val item: AbstractItem<V>) : Bpp2dDemandMode<V>

    /** 物品面积需求 / item area demand */
    data class ItemArea<V : FloatingNumber<V>>(val item: AbstractItem<V>) : Bpp2dDemandMode<V>

    /** 物料需求 / material demand */
    data class Material<V : FloatingNumber<V>>(val material: MaterialKey) : Bpp2dDemandMode<V>

    /** 物料件数需求 / material amount demand */
    data class ItemMaterialAmount<V : FloatingNumber<V>>(val material: MaterialKey) : Bpp2dDemandMode<V>

    /** 物料面积需求 / material area demand */
    data class ItemMaterialArea<V : FloatingNumber<V>>(val material: MaterialKey) : Bpp2dDemandMode<V>
}

/**
 * 需求键：标识需求针对的物品、物料或抽象需求类别。 / Demand key identifying the item, material or abstract
 * demand category a demand refers to.
 *
 * @param V 数值类型 / numeric type
*/
sealed interface Bpp2dDemandKey<V : FloatingNumber<V>> {

    /** 物品需求键 / item demand key */
    data class Item<V : FloatingNumber<V>>(val item: AbstractItem<V>) : Bpp2dDemandKey<V>

    /** 物料需求键 / material demand key */
    data class Material<V : FloatingNumber<V>>(val material: MaterialKey) : Bpp2dDemandKey<V>
}

/**
 * 需求值：件数或面积。 / Demand value: amount or area.
 *
 * @param V 数值类型 / numeric type
*/
sealed interface Bpp2dDemandValue<V : FloatingNumber<V>> {

    /** 件数需求值 / amount demand value */
    data class Amount<V : FloatingNumber<V>>(val value: UInt64) : Bpp2dDemandValue<V>

    /** 面积需求值 / area demand value */
    data class Area<V : FloatingNumber<V>>(val value: Quantity<V>) : Bpp2dDemandValue<V>
}

/**
 * 需求项：模式、键、值与取值域的组合。 / Demand entry: mode, key, value and domain.
 *
 * @param V 数值类型 / numeric type
 * @property mode 需求模式 / demand mode
 * @property key 需求键 / demand key
 * @property demand 需求值 / demand value
 * @property quantityDomain 取值域 / value domain
*/
data class Bpp2dDemandEntry<V : FloatingNumber<V>>(
    val mode: Bpp2dDemandMode<V>,
    val key: Bpp2dDemandKey<V>,
    val demand: Bpp2dDemandValue<V>,
    val quantityDomain: Bpp2dDemandDomain = Bpp2dDemandDomain.Discrete
)

/**
 * 合并两个需求值，类型不一致时失败。 / Merge two demand values, failing on mismatched types.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左侧需求值 / left-hand demand value
 * @param rhs 右侧需求值 / right-hand demand value
 * @return 合并后的需求值或失败 / merged demand value, or failure
*/
fun <V : FloatingNumber<V>> mergeDemandValue(
    lhs: Bpp2dDemandValue<V>,
    rhs: Bpp2dDemandValue<V>
): Ret<Bpp2dDemandValue<V>> {
    return when {
        lhs is Bpp2dDemandValue.Amount && rhs is Bpp2dDemandValue.Amount -> ok(
            Bpp2dDemandValue.Amount(lhs.value + rhs.value)
        )

        lhs is Bpp2dDemandValue.Area && rhs is Bpp2dDemandValue.Area -> {
            when (val sum = quantityPlusSafe(lhs.value, rhs.value)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> ok(Bpp2dDemandValue.Area(sum.value))
                is fuookami.ospf.kotlin.utils.functional.Failed -> Failed(sum.error)
                is fuookami.ospf.kotlin.utils.functional.Fatal -> fuookami.ospf.kotlin.utils.functional.Fatal(sum.errors)
            }
        }

        else -> Failed(ErrorCode.IllegalArgument, "需求值类型不兼容：$lhs vs $rhs / Incompatible demand values: $lhs vs $rhs")
    }
}

/**
 * 按倍数缩放需求值。 / Scale a demand value.
 *
 * @param V 数值类型 / numeric type
 * @param value 原始需求值 / original demand value
 * @param multiplier 倍数 / multiplier
 * @return 缩放后的需求值 / scaled demand value
*/
fun <V : FloatingNumber<V>> scaleDemandValue(
    value: Bpp2dDemandValue<V>,
    multiplier: UInt64
): Bpp2dDemandValue<V> {
    return when (value) {
        is Bpp2dDemandValue.Amount -> Bpp2dDemandValue.Amount(value.value * multiplier)
        is Bpp2dDemandValue.Area -> Bpp2dDemandValue.Area(
            quantityScale(value.value, quantityScaler(value.value.value.constants, multiplier))
        )
    }
}

/**
 * 按标量缩放量值。 / Scale a quantity by a scalar.
 *
 * @param V 数值类型 / numeric type
 * @param value 待缩放量值 / quantity to scale
 * @param scaler 缩放标量 / scaling scalar
 * @return 缩放后的量值 / scaled quantity
*/
private fun <V : FloatingNumber<V>> quantityScale(value: Quantity<V>, scaler: V): Quantity<V> {
    return value * scaler
}

/**
 * 把无符号整数换算为数值类型。 / Convert an unsigned integer to the numeric type.
 *
 * @param V 数值类型 / numeric type
 * @param constants 数值常量提供者 / numeric constants provider
 * @param value 无符号整数 / unsigned integer
 * @return 数值类型的量 / value as the numeric type
*/
private fun <V : FloatingNumber<V>> quantityScaler(
    constants: fuookami.ospf.kotlin.math.algebra.concept.FloatingNumberConstants<V>,
    value: UInt64
): V {
    var acc = constants.zero
    var remaining = value
    while (remaining > UInt64.zero) {
        acc += constants.one
        remaining -= UInt64.one
    }
    return acc
}