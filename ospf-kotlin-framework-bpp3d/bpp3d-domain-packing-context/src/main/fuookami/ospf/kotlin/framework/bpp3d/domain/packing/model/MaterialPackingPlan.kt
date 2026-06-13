@file:Suppress("DEPRECATION")
/**
 * 物料装箱计划模型。
 * Material packing plan model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*

/**
 * 物料包装目标权重配置。
 * Material packing objective weights.
 */
data class MaterialPackingObjectiveConfig(
    val packageCountWeight: FltX = materialPackingScalar(1_000_000.0),
    val volumeWeight: FltX = materialPackingScalar(1_000.0),
    val slackWeight: FltX = materialPackingOne()
)

/**
 * 物料需求，可按数量与重量混合输入。
 * Material demand with amount/weight mixed input.
 */
data class MaterialPackingDemand<V : FloatingNumber<V>>(
    val material: Material<FltX>,
    val amount: UInt64 = UInt64.zero,
    val weight: Quantity<V>? = null
)

/**
 * 包装方案候选，描述包装程序与生成 item 的附加属性。
 * Candidate packaging program and item generation options.
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
 * 包装方案选择结果（x[p]）。
 * Selected package count for a program (x[p]).
 */
data class PackageSelection(
    val candidate: MaterialPackingProgramCandidate<*>,
    val amount: UInt64
)

/**
 * 包装方案内物料分配结果（y[p,m]）。
 * Assigned material amount for a program (y[p,m]).
 */
data class MaterialPackingAssignment(
    val candidate: MaterialPackingProgramCandidate<*>,
    val material: MaterialKey,
    val amount: UInt64
)

/**
 * 包装后的 item 及数量。
 * Packaged item and multiplicity.
 */
data class PackagedItem(
    val item: ActualItem,
    val amount: UInt64,
    val pending: Boolean
)

enum class MaterialPackingStatus {
    Optimal,
    Infeasible
}

/**
 * 包装求解信息。
 * Material packing solve information.
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
 * 物料包装规划结果。
 * Material packing plan output.
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
