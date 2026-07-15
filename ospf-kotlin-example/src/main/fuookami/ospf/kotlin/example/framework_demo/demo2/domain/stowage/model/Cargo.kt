package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Cargo code enumeration representing standard cargo category codes / 货物代码枚举，表示标准货物类别代码 */
enum class CargoCode {
    /** Ballast cargo / 压舱货物 */
    BAL,
    /** Elderly assistance cargo / 老年人辅助设备 */
    ELD,
    /** Fishing kit cargo / 渔具货物 */
    FKI,
    /** Cold chain cargo / 冷链货物 */
    CCD,
    /** Ice cargo / 冰鲜货物 */
    ICE,
    /** Live animal cargo / 活体动物货物 */
    AVI,
    /** Aircraft on ground parts / 航材AOG */
    AOG,
    /** Lithium ion battery cargo / 锂离子电池货物 */
    ELI,
    /** Lithium metal battery cargo / 锂金属电池货物 */
    ELM,
    /** Magnetized cargo / 磁性货物 */
    MAG,
    /** Human remains / 遗体货物 */
    HUB,
    /** Perishable cargo / 易腐货物 */
    PER,
    /** Miscellaneous item / 杂项物品 */
    YYI,
    /** Material cargo / 材料货物 */
    MAT,
    /** Radioactive material / 放射性材料 */
    RRM,
    /** Oversized cargo / 超大货物 */
    BIG,
    /** Overhanging cargo / 超限货物 */
    OHG,
    /** Frozen general cargo / 冷冻普通货物 */
    RFG,
    /** Frozen liquid cargo / 冷冻液体货物 */
    RFL,
    /** Frozen special cargo / 冷冻特殊货物 */
    RFS,
    /** Oxidizing cargo / 氧化性货物 */
    ROX,
    /** Explosive cargo / 爆炸性货物 */
    YYE,
    /** Heavy weight cargo / 重型货物 */
    HWJ,
    /** Dry ice cargo / 干冰货物 */
    RRY,
    /** Wet cargo / 湿润货物 */
    RRW,

    /** Valuable cargo / 贵重货物 */
    CVV,

    /** Crush-sensitive cargo / 易碎货物 */
    Crush,

    /** Stiff cargo / 刚硬性货物 */
    Stiff,

    /** Empty container / 空集装器 */
    Empty,

    /** Virtual cargo / 虚拟货物 */
    Virtual
}

/**
 * Cargo type combining a code and a type string, with cached factory methods.
 * 货物类型，结合代码和类型字符串，带有缓存的工厂方法。
 *
 * @property code the cargo code, or null if not a standard code / 货物代码，非标准代码时为 null
 * @property type the cargo type string / 货物类型字符串
*/
data class CargoType(
    val code: CargoCode?,
    val type: String
) {
    companion object {
        private val cache: MutableMap<String, CargoType> = HashMap()

        operator fun invoke(code: CargoCode): CargoType {
            return cache.getOrPut(code.name) {
                CargoType(
                    code = code,
                    type = code.name
                )
            }
        }

        operator fun invoke(name: String): CargoType {
            return cache.getOrPut(name) {
                CargoType(
                    code = CargoCode.entries.firstOrNull { it.name == name },
                    type = name,
                )
            }
        }
    }
}

/** Cargo priority category classification / 货物优先级类别分类 */
enum class CargoPriorityCategory {
    /** High priority / 高优先级 */
    High,
    /** Normal priority / 普通优先级 */
    Normal,
    /** Low priority / 低优先级 */
    Low
}

/**
 * Cargo priority with a numeric priority value, category, and transfer flag.
 * 货物优先级，包含数值优先级、类别和中转标志。
 *
 * @property priority the numeric priority value / 数值优先级
 * @property category the priority category / 优先级类别
 * @property transfer whether this is a transfer cargo / 是否为中转货物
*/
data class CargoPriority(
    val name: String,
    val priority: UInt64,
    val category: CargoPriorityCategory,
    val transfer: Boolean = false
)

/**
 * Compares two cargo priorities by their numeric priority values to determine ordering.
 * 通过数值优先级比较两个货物优先级以确定排序。
 * @param rhs The right-hand side cargo priority to compare with / 要比较的右侧货物优先级
 * @return The ordering result (less, equal, or greater) based on priority values / 基于优先级值的排序结果（小于、等于或大于）
*/

infix fun CargoPriority.ord(rhs: CargoPriority): Order {
    return orderOf(this.priority compareTo rhs.priority)
}
