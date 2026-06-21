package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

typealias BasePosition = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.Position

enum class PositionStatusCode {
    Loaded,
    Unloaded,
    Preassigned,
    Reserved
}

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

enum class PositionTypeCode {
    EmptyForbidden,
    EmptyHated,
    EmptyItemForbidden,
    CrushItemForbidden,
    StiffCargoForbidden,
    AOGMATAppointed,
    NormalBulkAppointed
}

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

data class PositionPredicateLoadWeight(
    val plw: Quantity<Flt64>,
    val min: Quantity<Flt64>,
    val max: Quantity<Flt64>
)

class PositionMaximumLoadWeight(
    private val aircraftModel: AircraftModel,
    private val position: BasePosition,
    val mlw: Quantity<Flt64>,
    val segments: List<Segment>,
    mzfw: Quantity<Flt64>
) {
    data class Segment(
        val mlw: Quantity<Flt64>,
        val minZFW: Quantity<Flt64>,
        val maxZFW: Quantity<Flt64>?
    )

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

data class PositionStowageTaboo(
    val taboos: List<TabooRange>
) {
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

typealias PositionPair = Pair<Position, Position>
val PositionPair.symmetrical get() = PositionPair(second, first)
