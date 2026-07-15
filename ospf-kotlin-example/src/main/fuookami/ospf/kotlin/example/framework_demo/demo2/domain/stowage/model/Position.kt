package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/** Type alias for the aircraft domain Position class / 飞机域 Position 类的类型别名 */
typealias BasePosition = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.Position

/** Position status code indicating the current loading state of a position / 舱位状态码，表示舱位的当前装载状态 */
enum class PositionStatusCode {
    /** Position is loaded / 舱位已装载 */
    Loaded,
    /** Position is unloaded / 舱位未装载 */
    Unloaded,
    /** Position is preassigned / 舱位已预分配 */
    Preassigned,
    /** Position is reserved / 舱位已预留 */
    Reserved
}

/**
 * Position status describing availability, stowage requirements, and weight needs.
 * 舱位状态，描述可用性、装载需求和重量需求。
 *
 * @property code the position status code / 舱位状态码
 * @property available whether the position is available for stowage / 舱位是否可用于装载
 * @property stowageNeeded whether the position requires stowage decision / 舱位是否需要装载决策
 * @property adjustmentNeeded whether the position requires adjustment / 舱位是否需要调整
 * @property predicateWeightNeeded whether the position requires predicate weight variable / 舱位是否需要谓词重量变量
 * @property recommendedWeightNeeded whether the position requires recommended weight variable / 舱位是否需要推荐重量变量
*/
data class PositionStatus(
    val code: PositionStatusCode,
    val available: Boolean,
    val stowageNeeded: Boolean,
    val adjustmentNeeded: Boolean,
    val predicateWeightNeeded: Boolean,
    val recommendedWeightNeeded: Boolean
) {
    companion object {
        operator fun invoke(
            location: PositionLocation,
            code: PositionStatusCode,
            stowageMode: StowageMode
        ): PositionStatus {
            TODO("not implemented yet")
        }
    }

    val unavailable: Boolean get() = !available
}

/** Position type code indicating special constraints on a position / 舱位类型码，表示舱位的特殊约束 */
enum class PositionTypeCode {
    /** Empty stowage is forbidden / 禁止放空 */
    EmptyForbidden,
    /** Empty stowage is discouraged / 厌恶放空 */
    EmptyHated,
    /** Empty item is forbidden / 禁止装载空集装器 */
    EmptyItemForbidden,
    /** Crush-sensitive item is forbidden / 禁止装载易碎货物 */
    CrushItemForbidden,
    /** Stiff cargo is forbidden / 禁止装载刚硬性货物 */
    StiffCargoForbidden,
    /** AOG or MAT appointed position / 航材指定舱位 */
    AOGMATAppointed,
    /** Normal bulk appointed position / 普通散货指定舱位 */
    NormalBulkAppointed
}

/**
 * Position type combining a set of position type codes.
 * 舱位类型，包含一组舱位类型码集合。
 *
 * @property codes the set of position type codes / 舱位类型码集合
*/
data class PositionType(
    val codes: Set<PositionTypeCode>
) {

    /**
     * 是否包含给定的舱位类型
     *
     * @param code          舱位类型
     * @return              是否包含
    */
    operator fun contains(code: PositionTypeCode): Boolean {
        return codes.contains(code)
    }
}

/**
 * Predicate load weight specification for positions requiring predicate weight validation.
 * 需要谓词重量验证的舱位的谓词装载重量规格。
 *
 * @property plw the predicate load weight / 谓词装载重量
 * @property min the minimum load weight / 最小装载重量
 * @property max the maximum load weight / 最大装载重量
*/
data class PositionPredicateLoadWeight(
    val plw: Quantity<Flt64>,
    val min: Quantity<Flt64>,
    val max: Quantity<Flt64>
)

/**
 * Maximum load weight for a position, supporting piecewise-linear limits based on zero-fuel weight.
 * 舱位的最大装载重量，支持基于零油重的分段线性限制。
 *
 * @property mlw the maximum load weight / 最大装载重量
 * @property segments the piecewise-linear segments / 分段线性段
*/
class PositionMaximumLoadWeight(
    private val aircraftModel: AircraftModel,
    private val position: BasePosition,
    val mlw: Quantity<Flt64>,
    val segments: List<Segment>,
    mzfw: Quantity<Flt64>
) {

    /**
     * A segment in the piecewise-linear maximum load weight function.
     * 分段线性最大装载重量函数中的一个段。
     *
     * @property mlw the maximum load weight for this segment / 此段的最大装载重量
     * @property minZFW the minimum zero-fuel weight for this segment / 此段的最小零油重
     * @property maxZFW the maximum zero-fuel weight for this segment, or null if unbounded / 此段的最大零油重，无上界时为 null
    */
    data class Segment(
        val mlw: Quantity<Flt64>,
        val minZFW: Quantity<Flt64>,
        val maxZFW: Quantity<Flt64>?
    )

    /**
     * A point in the piecewise-linear maximum load weight function.
     * 分段线性最大装载重量函数中的一个点。
     *
     * @property mlw the maximum load weight at this point / 此点的最大装载重量
     * @property zfw the zero-fuel weight at this point / 此点的零油重
    */
    data class Point(
        val mlw: Quantity<Flt64>,
        val zfw: Quantity<Flt64>
    )

    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            position: BasePosition,
            mlw: Quantity<Flt64>,
            mzfw: Quantity<Flt64>
        ): PositionMaximumLoadWeight {
            return PositionMaximumLoadWeight(
                aircraftModel = aircraftModel,
                position = position,
                mlw = mlw,
                segments = emptyList(),
                mzfw = mzfw
            )
        }

        operator fun invoke(
            aircraftModel: AircraftModel,
            position: BasePosition,
            piecewise: List<Segment>,
            mzfw: Quantity<Flt64>
        ): PositionMaximumLoadWeight {
            assert(piecewise.isNotEmpty())
            val mlw = piecewise.map { it.mlw }.maxWithThreeWayComparatorOrNull { lhs, rhs ->
                (lhs partialOrd rhs)!!
            }!!
            return PositionMaximumLoadWeight(
                aircraftModel = aircraftModel,
                position = position,
                mlw = mlw,
                segments = piecewise.sortedWithThreeWayComparator { lhs, rhs ->
                    (lhs.minZFW partialOrd rhs.minZFW)!!
                },
                mzfw = mzfw
            )
        }
    }

    val points: List<Point> by lazy {
        val points = ArrayList<Point>()
        for (segment in segments) {
            points.add(Point(segment.mlw, segment.minZFW))
            if (segment.maxZFW == null) {
                points.add(Point(segment.mlw, mzfw))
            } else {
                points.add(Point(segment.mlw, segment.maxZFW))
            }
        }
        points
    }

    private val piecewise by lazy {
        if (points.size > 2) {
            UnivariateLinearPiecewiseFunction.fromPoints(
                x = LinearPolynomial(),
                points = points.map {
                    point2(
                        it.zfw.to(aircraftModel.weightUnit)!!.value,
                        it.mlw.to(aircraftModel.weightUnit)!!.value
                    )
                },
                converter = IntoValue.Identity,
                name = "mlw"
            )
        } else {
            null
        }
    }

    operator fun invoke(zfw: Quantity<Flt64>): Quantity<Flt64> {
//        return piecewise?.let {
//            Quantity(it.y(zfw.to(aircraftModel.weightUnit)!!.value)!!, aircraftModel.weightUnit)
//        } ?: mlw
        return if (segments.isEmpty()) {
            mlw
        } else if ((zfw ls segments.first().minZFW)!!) {
            segments.first().mlw
        } else {
            for (segment in segments) {
                if ((zfw geq segment.minZFW)!! && (segment.maxZFW == null || (zfw ls segment.maxZFW)!!)) {
                    return segment.mlw
                }
            }
            segments.last().mlw
        }
    }
}

/**
 * Stowage taboo for a position, defining ULD ranges that are prohibited.
 * 舱位的装载禁忌，定义禁止装载的 ULD 范围。
 *
 * @property taboos the list of taboo ranges / 禁忌范围列表
*/
data class PositionStowageTaboo(
    val taboos: List<TabooRange>
) {

    /**
     * A taboo range defining a prohibited ULD number range.
     * 定义禁止 ULD 编号范围的禁忌范围。
     *
     * @property uld the ULD code / ULD 代码
     * @property noRange the prohibited ULD number range / 禁止的 ULD 编号范围
     * @property com the commodity description / 商品描述
    */
    data class TabooRange(
        val uld: ULDCode,
        val noRange: ValueRange<UInt64>,
        val com: String
    )

    operator fun get(item: Item): TabooRange? {
        return taboos.find {
            item.uld?.code == it.uld && it.noRange.contains(fuookami.ospf.kotlin.math.algebra.number.UInt64(item.uld.name.substring(3, 8).toULong()))
        }
    }

    operator fun contains(item: Item): Boolean {
        return get(item) != null
    }
}

/**
 * Stowage position combining aircraft base position with loading constraints and status.
 * 配载舱位，结合飞机基础舱位与装载约束和状态。
 *
 * @property base the aircraft base position / 飞机基础舱位
 * @property loadedItems the set of items already loaded in this position / 此舱位已装载的货物集合
 * @property ala the actual load amount, or null if not applicable / 实际装载量，不适用时为 null
 * @property mla the maximum load amount / 最大装载量
 * @property alw the actual load weight, or null if not applicable / 实际装载重量，不适用时为 null
 * @property mlw the maximum load weight specification / 最大装载重量规格
 * @property plw the predicate load weight specification, or null if not applicable / 谓词装载重量规格，不适用时为 null
 * @property type the position type with constraint codes / 带约束码的舱位类型
 * @property taboo the stowage taboo restrictions / 装载禁忌限制
 * @property status the position status / 舱位状态
*/
data class Position(
    val base: BasePosition,
    val loadedItems: Set<Item>,
    val ala: UInt64?,
    val mla: UInt64,
    val alw: Quantity<Flt64>?,
    val mlw: PositionMaximumLoadWeight,
    val plw: PositionPredicateLoadWeight?,
    val type: PositionType,
    val taboo: PositionStowageTaboo,
    val status: PositionStatus
): ManualIndexed() {
    val id by base::id
    val spaceName by base::spaceName
    val alphaSpaceName by base::alphaSpaceName
    val sizeCode by base::sizeCode
    val loadingOrder by base::loadingOrder
    val coordinate by base::coordinate
    val shape by base::shape
    val location by base::location
    val enabledULDs by base::enabledULDs

    private val disableReason = HashMap<Item, String>()

    /**
     * Checks whether the given item is enabled for stowage at this position.
     * 检查给定货物是否允许在此舱位装载。
     *
     * @param item the cargo item to check / 要检查的货物
     * @return ok if enabled, or a failure with the reason / 允许时返回 ok，否则返回失败原因
    */
    fun enabled(item: Item): Try {
        // 舱位位置限制
        if (this.location.main && item.location.low) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "主舱禁止装载下货舱装载物"
            )
        }

        if (this.location.low && (item.location.head || item.location.tail)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "下货舱禁止装载头尾舱装载物"
            )
        }

        if (this.location.bulk && !item.location.bulk) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "散货舱禁止装载非散货装载物"
            )
        }

        if (!this.location.bulk && item.location.bulk) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "非散货舱禁止装载散货装载物"
            )
        }

        if (this.location.head && !item.location.head) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "头舱禁止装载非头舱装载物"
            )
        }

        if (!this.location.head && item.location.head) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "非头舱禁止装载头舱装载物"
            )
        }

        if (this.location.tail && !item.location.tail) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "尾舱禁止装载非尾舱装载物"
            )
        }

        if (!this.location.tail && item.location.tail) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "非尾舱禁止装载尾舱装载物"
            )
        }

        if (this.location.low && item.cargo.contains(CargoCode.Virtual)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "下货舱禁止装载虚拟装载物"
            )
        }

        // 舱位集装箱限制
        if (item.uld?.code !in enabledULDs) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "不支持该集装器类型"
            )
        }

        val taboo = taboo[item]
        if (taboo != null) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "禁止装载 ${taboo.uld} 的 ${taboo.noRange.lowerBound} ~ ${taboo.noRange.upperBound} 集装器"
            )
        }

        // 舱位类型限制
        if (this.type.contains(PositionTypeCode.StiffCargoForbidden) && item.cargo.contains(CargoCode.Stiff)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "厌恶刚硬性装载物的舱位禁止装载刚硬性装载物"
            )
        }

        if (this.type.contains(PositionTypeCode.CrushItemForbidden) && item.cargo.contains(CargoCode.Crush)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "厌恶易碎装载物的舱位禁止装载易碎装载物"
            )
        }

        if (this.type.contains(PositionTypeCode.EmptyItemForbidden) && item.cargo.contains(CargoCode.Empty)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "厌恶空集装器的舱位禁止装载空集装器"
            )
        }

        if (this.type.contains(PositionTypeCode.EmptyForbidden) && item.cargo.contains(CargoCode.Virtual)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "不可放空舱位禁止装载虚拟装载物"
            )
        }

        if (this.type.contains(PositionTypeCode.AOGMATAppointed) && !item.cargo.contains(CargoCode.AOG) && !item.cargo.contains(CargoCode.MAT)) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "航材指定舱位只能装载航材"
            )
        }

        if (this.type.contains(PositionTypeCode.NormalBulkAppointed)
            && this.loadedItems.none { it.cargo.contains(CargoCode.AOG) || it.cargo.contains(CargoCode.MAT) }
            && (item.cargo.contains(CargoCode.AOG) || item.cargo.contains(CargoCode.MAT))
        ) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "普散货指定舱位只能装载普通散货"
            )
        }

        if (this.loadedItems.any { it.cargo.contains(CargoCode.AOG) || it.cargo.contains(CargoCode.MAT) }) {
            if (!item.cargo.contains(CargoCode.AOG) && !item.cargo.contains(CargoCode.MAT)) {
                return Failed(
                    ErrorCode.ApplicationFailed,
                    "已经装载航材的散舱不能装载普通散货"
                )
            }
        }

        if (this.loadedItems.any { it.location.bulk && !it.cargo.contains(CargoCode.AOG) && !it.cargo.contains(CargoCode.MAT) }) {
            if (item.cargo.contains(CargoCode.AOG) || item.cargo.contains(CargoCode.MAT)) {
                return Failed(
                    ErrorCode.ApplicationFailed,
                    "已经装载普通散货的散舱不能装载航材"
                )
            }

            val destinations = this.loadedItems
                .filter { it.location.bulk && !it.cargo.contains(CargoCode.AOG) && !it.cargo.contains(CargoCode.MAT) }
                .map { it.destination }
                .distinct()
            if (item.destination !in destinations) {
                return Failed(
                    ErrorCode.ApplicationFailed,
                    "散货舱只能装载同一目的地的散货"
                )
            }
        }

        return ok
    }

    override fun toString(): String {
        return alphaSpaceName
    }
}

/** Type alias for a pair of positions / 舱位对的类型别名 */
typealias PositionPair = Pair<Position, Position>

/** Returns the symmetrical (swapped) position pair / 返回对称（交换）的舱位对 */
val PositionPair.symmetrical get() = PositionPair(second, first)
