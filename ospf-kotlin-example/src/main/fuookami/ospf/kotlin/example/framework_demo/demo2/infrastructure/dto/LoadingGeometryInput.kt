package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.quantities.unit.*

/** 请求长度单位。 / Request length unit. */
@Serializable
enum class LengthUnitInput {
    Meter, Centimeter, Inch;
    /** 物理单位。 / Physical unit. */
    val unit: PhysicalUnit get() = when (this) {
        Meter -> fuookami.ospf.kotlin.quantities.unit.Meter
        Centimeter -> fuookami.ospf.kotlin.quantities.unit.Centimeter
        Inch -> fuookami.ospf.kotlin.quantities.unit.Inch
    }
}

/** 请求重量单位。 / Request weight unit. */
@Serializable
enum class WeightUnitInput {
    Kilogram;
    /** 物理单位。 / Physical unit. */
    val unit: PhysicalUnit get() = fuookami.ospf.kotlin.quantities.unit.Kilogram
}

/** 甲板分类。 / Deck classification. */
@Serializable
enum class DeckLocationInput { Main, LowForward, LowAft }

/** 独立物理空间。 / Independent physical space.
 * @property location 甲板分类 / Deck classification
 */
@Serializable
data class LoadingSpaceInput(val id: String, val location: DeckLocationInput)

/** 显式矩形边界；窄体的占位仅使用纵向区间。 / Explicit rectangle; narrow-body occupancy uses its longitudinal interval.
 * @property front 前边界 / Front boundary
 * @property back 后边界 / Back boundary
 * @property left 左边界 / Left boundary
 * @property right 右边界 / Right boundary
 */
@Serializable
data class PositionGeometryInput(val front: Double, val back: Double, val left: Double, val right: Double)

/** 一种布局。 / One layout.
 * @property positions 可用舱位名 / Permitted position names
 */
@Serializable
data class LoadingLayoutInput(val id: String, val positions: Set<String>)

/** 区域或整机恰选一种布局。 / Exactly-one regional or global layout choice.
 * @property positions 受约束舱位名 / Governed position names
 * @property layouts 可选布局 / Available layouts
 */
@Serializable
data class LayoutGroupInput(val id: String, val positions: Set<String>, val layouts: List<LoadingLayoutInput>)

/** 额外业务互斥，不覆盖或取消几何冲突。 / Additional business conflict, never overriding geometry conflicts.
 * @property first 首舱位名 / First position name
 * @property second 次舱位名 / Second position name
 */
@Serializable
data class PositionConflictInput(val first: String, val second: String)
