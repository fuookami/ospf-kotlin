package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Tags describing the location of a cargo position within the aircraft.
 * 描述飞机内货物位置位置的标签。
*/
enum class PositionLocationTag {
    /** Main deck position / 主舱位置 */
    Main,
    /** Lower deck position / 下舱位置 */
    Low,
    /** Lower deck forward position / 下舱前部位置 */
    LowForward,
    /** Lower deck aft position / 下舱后部位置 */
    LowAft,
    /** Bulk cargo position / 散货位置 */
    Bulk,
    /** Head section position / 头部位置 */
    Head,
    /** Tail section position / 尾部位置 */
    Tail
}

/**
 * A set of location tags that classify a cargo position's placement.
 * 分类货物位置放置的一组位置标签。
 *
 * @property tags The set of position location tags. / 位置标签集合
*/
data class PositionLocation(
    val tags: Set<PositionLocationTag>
) {
    companion object {
        val head = PositionLocation(setOf(PositionLocationTag.Main, PositionLocationTag.Head))
        val tail = PositionLocation(setOf(PositionLocationTag.Main, PositionLocationTag.Tail))
        val normalMain = PositionLocation(setOf(PositionLocationTag.Main))
        val lowForwardBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowForward, PositionLocationTag.Bulk))
        val lowForwardNotBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowForward))
        val lowAftBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowAft, PositionLocationTag.Bulk))
        val lowAftNotBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowAft))
    }

    operator fun contains(tag: PositionLocationTag) = tags.contains(tag)

    val main = contains(PositionLocationTag.Main)
    val head = contains(PositionLocationTag.Head)
    val tail = contains(PositionLocationTag.Tail)
    val normalMain = contains(PositionLocationTag.Main)
    val specialMain = head || tail
    val low = contains(PositionLocationTag.Low)
    val lowForward = contains(PositionLocationTag.LowForward)
    val lowAft = contains(PositionLocationTag.LowAft)
    val bulk = contains(PositionLocationTag.Bulk)
    val lowNotBulk = low && !bulk
    val location = when {
        contains(PositionLocationTag.Main) -> DeckLocation.Main
        contains(PositionLocationTag.LowForward) -> DeckLocation.LowForward
        contains(PositionLocationTag.LowAft) -> DeckLocation.LowAft
        else -> DeckLocation.Main
    }
}

/**
 * Check if a deck location contains the given position location.
 * 检查甲板位置是否包含给定的位置分类。
 *
 * @receiver The deck location. / 甲板位置
 * @param location The position location to check. / 要检查的位置分类
 * @return True if the deck location matches the position location. / 如果甲板位置与位置分类匹配则返回 true
*/
operator fun DeckLocation.contains(location: PositionLocation): Boolean {
    return location.location == this
}

/**
 * Coordinate system for a cargo position with longitudinal and lateral arm measurements.
 * 具有纵向和横向臂测量的货物位置坐标系。
 *
 * @property aircraftModel The aircraft model for unit definitions. / 用于单位定义的飞机型号
 * @property frontArm The front arm coordinate. / 前臂坐标
 * @property backArm The back arm coordinate. / 后臂坐标
 * @property leftArm The left arm coordinate. / 左臂坐标
 * @property rightArm The right arm coordinate. / 右臂坐标
 * @property offsets The lateral arm offsets mapped by ULD code. / 按 ULD 代码映射的横向臂偏移量
*/
class PositionCoordinate(
    private val aircraftModel: AircraftModel,
    val frontArm: Quantity<Flt64>,
    val backArm: Quantity<Flt64>,
    val leftArm: Quantity<Flt64>,
    val rightArm: Quantity<Flt64>,
    val offsets: HashMap<ULDCode, Quantity<Flt64>>
) {
    val longitudinalArm = ((frontArm + backArm)!! / Flt64.two)!!
    val lateralArm = ((leftArm + rightArm)!! / Flt64.two)!!

    val averageOffset = if (offsets.isNotEmpty()) {
        (offsets.values.fold(Flt64.zero * aircraftModel.lengthUnit) { acc, offset -> (acc + offset)!! } / Flt64(offsets.size))!!
    } else {
        lateralArm
    }
    val minOffset = if (offsets.isNotEmpty()) {
        offsets.values.minWithThreeWayComparatorOrNull { lhs, rhs -> (lhs partialOrd rhs)!! } ?: (Flt64.zero * aircraftModel.lengthUnit)
    } else {
        lateralArm
    }

    /**
     * Get the lateral arm offset for a specific ULD.
     * 获取特定 ULD 的横向臂偏移量。
     *
     * @param uld The unit load device. / 单元装载设备
     * @return The lateral arm offset for the ULD, or the average offset if not found. / ULD 的横向臂偏移量，若未找到则为平均偏移量
    */
    fun offset(uld: ULD): Quantity<Flt64> {
        return offsets[uld.code] ?: averageOffset
    }

    val transverse = (leftArm leq (Flt64.zero * aircraftModel.lengthUnit))!! && ((Flt64.zero * aircraftModel.lengthUnit) leq rightArm)!!
    val onLeft = !transverse && (lateralArm leq (Flt64.zero * aircraftModel.lengthUnit))!!
    val onRight = !transverse && (lateralArm gr (Flt64.zero * aircraftModel.lengthUnit))!!

    /**
     * Check if this position is in front of the given arm value.
     * 检查该位置是否在给定臂值的前方。
     *
     * @param arm The arm value to compare against. / 要比较的臂值
     * @return True if this position is in front of the arm. / 如果该位置在臂值前方则返回 true
    */
    fun inFrontOf(arm: Quantity<Flt64>): Boolean {
        return (backArm ls arm)!!
    }

    /**
     * Check if this position is behind the given arm value.
     * 检查该位置是否在给定臂值的后方。
     *
     * @param arm The arm value to compare against. / 要比较的臂值
     * @return True if this position is behind the arm. / 如果该位置在臂值后方则返回 true
    */
    fun behind(arm: Quantity<Flt64>): Boolean {
        return (frontArm ls arm)!!
    }

    /**
     * Check if the given arm value lies on this position.
     * 检查给定臂值是否位于该位置上。
     *
     * @param arm The arm value to check. / 要检查的臂值
     * @return True if the arm lies within this position. / 如果臂值位于该位置范围内则返回 true
    */
    fun on(arm: Quantity<Flt64>): Boolean {
        return (frontArm leq arm)!! && (arm leq backArm)!!
    }

    /**
     * Check if this position is entirely between the given front and back arm values.
     * 检查该位置是否完全在给定前后臂值之间。
     *
     * @param frontArm The front arm boundary. / 前臂边界
     * @param backArm The back arm boundary. / 后臂边界
     * @return True if this position is between the boundaries. / 如果该位置在边界之间则返回 true
    */
    fun between(frontArm: Quantity<Flt64>, backArm: Quantity<Flt64>): Boolean {
        return if ((backArm ls frontArm)!!) {
            between(backArm, frontArm)
        } else {
            (frontArm leq this.frontArm)!! && (this.backArm leq backArm)!!
        }
    }

    /**
     * Check if this position has any intersection with the given front and back arm range.
     * 检查该位置是否与给定前后臂范围有任何交集。
     *
     * @param frontArm The front arm boundary. / 前臂边界
     * @param backArm The back arm boundary. / 后臂边界
     * @return True if there is an intersection. / 如果存在交集则返回 true
    */
    fun withIntersectionWith(frontArm: Quantity<Flt64>, backArm: Quantity<Flt64>): Boolean {
        return if ((backArm ls frontArm)!!) {
            withIntersectionWith(backArm, frontArm)
        } else {
            (this.frontArm leq backArm)!! && (this.backArm geq frontArm)!!
        }
    }

    /**
     * Check if this position has any intersection with another position's coordinate range.
     * 检查该位置是否与另一个位置的坐标范围有任何交集。
     *
     * @param other The other position coordinate to check against. / 要比较的另一个位置坐标
     * @return True if there is an intersection. / 如果存在交集则返回 true
    */
    fun withIntersectionWith(other: PositionCoordinate): Boolean {
        return withIntersectionWith(other.frontArm, other.backArm)
                && (this.leftArm leq other.rightArm)!! && (other.leftArm leq this.rightArm)!!
    }
}

/**
 * Physical dimensions and area of a cargo position.
 * 货物位置的物理尺寸和面积。
 *
 * @property aircraftModel The aircraft model for unit definitions. / 用于单位定义的飞机型号
 * @property length The length of the position. / 位置长度
 * @property width The width of the position. / 位置宽度
 * @property volume The volume of the position. / 位置体积
*/
data class PositionShape(
    private val aircraftModel: AircraftModel,
    val length: Quantity<Flt64>,
    val width: Quantity<Flt64>,
    val volume: Quantity<Flt64>
) {
    val area = (length * width)!!.to(aircraftModel.areaUnit)!!
}

/**
 * A cargo position on the aircraft with its coordinates, shape, location, and loading order.
 * 飞机上的货物位置（具有坐标、形状、位置和装载顺序）。
 *
 * @property id The unique identifier of the position. / 位置唯一标识
 * @property spaceName The space name of the position. / 位置空间名称
 * @property sizeCode The size code of the position. / 位置尺寸代码
 * @property linearLoadingOrder The linear loading order index. / 线性装载顺序索引
 * @property coordinate The coordinate system of the position. / 位置坐标系
 * @property shape The physical dimensions of the position. / 位置物理尺寸
 * @property location The location classification of the position. / 位置分类
*/
data class Position(
    val id: UInt64,
    val spaceName: String,
    val sizeCode: String,
    val linearLoadingOrder: UInt8,
    val coordinate: PositionCoordinate,
    val shape: PositionShape,
    val location: PositionLocation
) {
    companion object {
        /**
         * Create a Position from raw arm coordinates and dimensions.
         * 从原始臂坐标和尺寸创建位置。
         *
         * @param aircraftModel The aircraft model for unit definitions. / 用于单位定义的飞机型号
         * @param id The unique identifier. / 唯一标识
         * @param spaceName The space name. / 空间名称
         * @param sizeCode The size code. / 尺寸代码
         * @param frontArm The front arm coordinate. / 前臂坐标
         * @param backArm The back arm coordinate. / 后臂坐标
         * @param leftArm The left arm coordinate. / 左臂坐标
         * @param rightArm The right arm coordinate. / 右臂坐标
         * @param volume The volume of the position. / 位置体积
         * @param offsets The lateral arm offsets mapped by ULD code. / 按 ULD 代码映射的横向臂偏移量
         * @param location The location classification. / 位置分类
         * @param linearLoadingOrder The linear loading order index. / 线性装载顺序索引
         * @return The constructed Position. / 构建的位置
        */
        operator fun invoke(
            aircraftModel: AircraftModel,
            id: UInt64,
            spaceName: String,
            sizeCode: String,
            frontArm: Quantity<Flt64>,
            backArm: Quantity<Flt64>,
            leftArm: Quantity<Flt64>,
            rightArm: Quantity<Flt64>,
            volume: Quantity<Flt64>,
            offsets: HashMap<ULDCode, Quantity<Flt64>>,
            location: PositionLocation,
            linearLoadingOrder: UInt8,
        ): Position {
            val coordinate = PositionCoordinate(aircraftModel, frontArm, backArm, leftArm, rightArm, offsets)
            val shape = PositionShape(aircraftModel, (backArm - frontArm)!!, (rightArm - leftArm)!!, volume)
            return Position(
                id = id,
                spaceName = spaceName,
                sizeCode = sizeCode,
                linearLoadingOrder = linearLoadingOrder,
                coordinate = coordinate,
                shape = shape,
                location = location
            )
        }
    }

    internal lateinit var _loadingOrder: LoadingOrder
    val loadingOrder by ::_loadingOrder

    val alphaSpaceName: String = spaceName.filter { it.isLetterOrDigit() }
    val enabledULDs by coordinate.offsets::keys
}
