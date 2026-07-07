/**
 * Material packing plan model.
 * 物料装箱计划模型。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*

/**
 * Objective weight configuration for material packing optimization.
 * 物料包装目标权重配置。
 *
 * @property packageCountWeight The weight for minimizing the number of packages.
 * 最小化包数量的权重。
 * @property volumeWeight The weight for minimizing total volume.
 * 最小化总体积的权重。
 * @property slackWeight The weight for minimizing material slack (over-coverage).
 * 最小化物料过剩（过度覆盖）的权重。
 */
data class MaterialPackingObjectiveConfig(
    val packageCountWeight: FltX = materialPackingScalar(1_000_000.0),
    val volumeWeight: FltX = materialPackingScalar(1_000.0),
    val slackWeight: FltX = materialPackingOne()
)

/**
 * Material demand with amount and/or weight mixed input.
 * 物料需求，可按数量与重量混合输入。
 *
 * @property material The material to be packed.
 * 待装箱的物料。
 * @property amount The demanded amount of the material.
 * 物料的需求数量。
 * @property weight The demanded weight of the material, nullable.
 * 物料的需求重量，可为空。
 */
data class MaterialPackingDemand<V : FloatingNumber<V>>(
    val material: Material<FltX>,
    val amount: UInt64 = UInt64.zero,
    val weight: Quantity<V>? = null
)

/**
 * Candidate packaging program and item generation options.
 * 包装方案候选，描述包装程序与生成 item 的附加属性。
 *
 * @property id The unique identifier of the candidate.
 * 候选方案的唯一标识。
 * @property program The packing program for this candidate.
 * 此候选方案的装箱程序。
 * @property itemName The name for generated items, defaults to the candidate id.
 * 生成物品的名称，默认为候选方案标识。
 * @property enabledOrientations The list of allowed orientations for generated items.
 * 生成物品允许的方向列表。
 * @property batchNo The batch number for generated items, nullable.
 * 生成物品的批次号，可为空。
 * @property warehouse The warehouse location for generated items, nullable.
 * 生成物品的仓库位置，可为空。
 * @property packageAttribute The package attribute for generated items, nullable.
 * 生成物品的包装属性，可为空。
 */
data class MaterialPackingProgramCandidate<V : FloatingNumber<V>>(
    val id: String,
    val program: PackingProgram<V>,
    val itemName: String = id,
    val enabledOrientations: List<Orientation> = listOf(Orientation.Upright),
    val batchNo: BatchNo? = null,
    val warehouse: String? = null,
    val packageAttribute: PackageAttribute? = null
)

/**
 * Selected package count for a program (x[p]).
 * 包装方案选择结果（x[p]）。
 *
 * @property candidate The selected candidate program.
 * 被选中的候选方案。
 * @property amount The number of packages selected for this candidate.
 * 此候选方案被选中的包数量。
 */
data class PackageSelection(
    val candidate: MaterialPackingProgramCandidate<*>,
    val amount: UInt64
)

/**
 * Assigned material amount for a program (y[p,m]).
 * 包装方案内物料分配结果（y[p,m]）。
 *
 * @property candidate The candidate program that the material is assigned to.
 * 物料分配到的候选方案。
 * @property material The material key being assigned.
 * 被分配的物料键。
 * @property amount The amount of material assigned.
 * 分配的物料数量。
 */
data class MaterialPackingAssignment(
    val candidate: MaterialPackingProgramCandidate<*>,
    val material: MaterialKey,
    val amount: UInt64
)

/**
 * Packaged item and its multiplicity.
 * 包装后的 item 及数量。
 *
 * @property item The actual item produced by packaging.
 * 包装产生的实际物品。
 * @property amount The number of this item.
 * 此物品的数量。
 * @property pending Whether this item is pending (not fully assigned).
 * 此物品是否待定（未完全分配）。
 */
data class PackagedItem(
    val item: ActualItem,
    val amount: UInt64,
    val pending: Boolean
)

/**
 * Status of the material packing solve.
 * 物料包装求解状态。
 */
enum class MaterialPackingStatus {
    Optimal,
    Infeasible
}

/**
 * Material packing solve information including status, objective, and timing.
 * 包装求解信息，包含状态、目标值和耗时。
 *
 * @property status The solve status.
 * 求解状态。
 * @property objective The objective function value, nullable.
 * 目标函数值，可为空。
 * @property gap The optimality gap, nullable.
 * 最优间隙，可为空。
 * @property timeMillis The solve time in milliseconds.
 * 求解耗时（毫秒）。
 * @property selectedPackageCount The total number of selected packages.
 * 被选中的包总数。
 * @property rawStatus The raw solver status string, nullable.
 * 原始求解器状态字符串，可为空。
 */
data class MaterialPackingSolveInfo(
    val status: MaterialPackingStatus,
    val objective: FltX? = null,
    val gap: FltX? = null,
    val timeMillis: Long = 0L,
    val selectedPackageCount: UInt64 = UInt64.zero,
    val rawStatus: String? = null
)

/**
 * Material packing plan output containing packages, items, selections, and assignments.
 * 物料包装规划结果，包含包装、物品、选择和分配。
 *
 * @property packages The list of packages in the plan.
 * 计划中的包装列表。
 * @property packagedItems The list of packaged items with their amounts.
 * 包装后的物品及其数量列表。
 * @property selections The list of package selections.
 * 包装方案选择列表。
 * @property assignments The list of material packing assignments.
 * 物料装箱分配列表。
 * @property restMaterials The remaining unassigned materials and their amounts.
 * 剩余未分配物料及其数量。
 * @property normalizedDemands The normalized demand amounts per material key.
 * 按物料键归一化的需求数量。
 * @property solveInfo The solve information for this plan.
 * 此计划的求解信息。
 */
data class MaterialPackingPlan(
    val packages: List<Package<*>>,
    val packagedItems: List<PackagedItem>,
    val selections: List<PackageSelection>,
    val assignments: List<MaterialPackingAssignment>,
    val restMaterials: Map<MaterialKey, UInt64> = emptyMap(),
    val normalizedDemands: Map<MaterialKey, UInt64> = emptyMap(),
    val solveInfo: MaterialPackingSolveInfo
)
