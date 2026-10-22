package fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 离散货物，身份不可因尺寸相同而合并。 / Discrete cargo whose identity is never merged.
 * @property weight 单件重量 / Item weight
 * @property required 是否必须装载 / Whether loading is mandatory
 * @property fixedPositionId 固定装载位置 / Fixed position
 */
data class LoadingItem<V : FloatingNumber<V>>(
    val id: String,
    val weight: Quantity<V>,
    val required: Boolean = false,
    val fixedPositionId: String? = null
)

/** 固定位置的容量与占用。 / Fixed-position capacity and occupancy.
 * @property maxCount 最大件数 / Maximum item count
 * @property maxWeight 最大总重量 / Maximum total weight
 * @property fixedOccupied 是否有外部固定占用 / External fixed occupancy
 * @property enabled 是否允许决策装载 / Whether decision loading is allowed
 */
data class LoadingPosition<V : FloatingNumber<V>>(
    val id: String,
    val maxCount: UInt64,
    val maxWeight: Quantity<V>,
    val fixedOccupied: Boolean = false,
    val enabled: Boolean = true
)

/** 合法分配的稳定键。 / Stable key for a legal assignment.
 * @property itemId 货物 ID / Cargo ID
 * @property positionId 舱位 ID / Position ID
 */
data class AssignmentKey(val itemId: String, val positionId: String)

/** 独立于几何的互斥边；不具有传递性。 / Geometry-independent, non-transitive conflict edge.
 * @property first 首舱位 / First position
 * @property second 次舱位 / Second position
 */
data class PositionConflict(val first: String, val second: String)

/** 一种布局允许使用的舱位。 / Positions permitted by one layout.
 * @property positionIds 允许使用的舱位 / Permitted positions
 */
data class LoadingLayout(val id: String, val positionIds: Set<String>)

/** 在显式作用域内恰选一种布局。 / Select exactly one layout within an explicit scope.
 * @property positionIds 受本选择组约束的舱位 / Positions governed by this group
 * @property layouts 可选布局，共享舱位可出现在多个布局 / Layouts, with shared positions allowed
 */
data class LayoutSelectionGroup(
    val id: String,
    val positionIds: Set<String>,
    val layouts: List<LoadingLayout>
)

/** 无维度绑定的稀疏装载问题。 / Dimension-independent sparse loading problem.
 * @property items 货物 / Items
 * @property positions 舱位 / Positions
 * @property allowedAssignments 合法组合，不自动补全笛卡尔积 / Legal combinations, never implicitly completed
 * @property conflicts 必须互斥的舱位 / Conflicting positions
 * @property layoutGroups 可选的整机或区域布局选择 / Optional global or regional layout choices
 * @property weightUnit 求解器重量口径 / Solver weight unit
 */
data class DiscreteLoadingProblem<V : FloatingNumber<V>>(
    val items: List<LoadingItem<V>>,
    val positions: List<LoadingPosition<V>>,
    val allowedAssignments: Set<AssignmentKey>,
    val conflicts: Set<PositionConflict>,
    val layoutGroups: List<LayoutSelectionGroup>,
    val weightUnit: PhysicalUnit
)
