package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * 货物类型枚举，定义货物的基本分类标签。
 * Cargo type enum defining basic classification tags for cargo.
 *
 * @property stackable 可堆叠 / Stackable
 * @property liquid 液体 / Liquid
 * @property heavy 重型 / Heavy
 * @property oversize 超大 / Oversize
 */
enum class CargoType {
    /** 可堆叠 / Stackable */
    stackable,
    /** 液体 / Liquid */
    liquid,
    /** 重型 / Heavy */
    heavy,
    /** 超大 / Oversize */
    oversize
}

/**
 * 货物约束模型，描述某类货物在装载时受到的限制组合。
 * Cargo constraint model describing the set of restrictions applied when stowing a particular type of cargo.
 *
 * @property stackable 是否允许堆叠 / Whether stacking is allowed
 * @property liquid 是否为液体货物 / Whether it is liquid cargo
 * @property heavy 是否为重型货物 / Whether it is heavy cargo
 * @property oversize 是否为超大货物 / Whether it is oversize cargo
 */
data class ItemCargoLimit(
    val stackable: Boolean = false,
    val liquid: Boolean = false,
    val heavy: Boolean = false,
    val oversize: Boolean = false
) {
    /**
     * 根据当前约束组合推断对应的货物类型。
     * Infer the corresponding cargo type based on the current constraint combination.
     *
     * @return 匹配的 [CargoType] 枚举值 / The matching [CargoType] enum value
     */
    fun getCargoType(): CargoType {
        return when {
            stackable -> CargoType.stackable
            liquid -> CargoType.liquid
            heavy -> CargoType.heavy
            oversize -> CargoType.oversize
            else -> throw IllegalArgumentException("No matching cargo type")
        }
    }

    /**
     * 判断指定仓位是否满足当前货物约束。
     * Check whether the specified stowage location satisfies the current cargo constraints.
     *
     * @param stowageLocation 待检查的仓位 / The stowage location to check
     * @return 如果仓位满足所有约束则返回 true / true if the location satisfies all constraints
     */
    fun hasCargo(stowageLocation: StowageLocation): Boolean {
        return stowageLocation.hasCargo(this)
    }
}
