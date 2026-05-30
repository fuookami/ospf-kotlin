@file:Suppress("DEPRECATION")

/**
 * 鐗╂枡瑁呯璁″垝妯″瀷銆?
 * Material packing plan model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingNumber
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.materialPackingOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.materialPackingScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 鐗╂枡鍖呰鐩爣鏉冮噸閰嶇疆銆?
 * Material packing objective weights.
 */
data class MaterialPackingObjectiveConfig(
    val packageCountWeight: MaterialPackingNumber = materialPackingScalar(1_000_000.0),
    val volumeWeight: MaterialPackingNumber = materialPackingScalar(1_000.0),
    val slackWeight: MaterialPackingNumber = materialPackingOne()
)

/**
 * 鐗╂枡闇€姹傦紝鍙寜鏁伴噺涓庨噸閲忔贩鍚堣緭鍏ャ€?
 * Material demand with amount/weight mixed input.
 */
data class MaterialPackingDemand<V : FloatingNumber<V>>(
    val material: Material<MaterialPackingNumber>,
    val amount: UInt64 = UInt64.zero,
    val weight: Quantity<V>? = null
)

/** InfraNumber 鐗╂枡瑁呯闇€姹傚埆鍚嶃€侷nfraNumber material-packing demand alias. */
typealias InfraMaterialPackingDemand = MaterialPackingDemand<MaterialPackingNumber>

/**
 * 鍖呰鏂规鍊欓€夛紝鎻忚堪鍖呰绋嬪簭涓庣敓鎴?item 鐨勯檮鍔犲睘鎬с€?
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

/** InfraNumber 瑁呯绋嬪簭鍊欓€夊埆鍚嶃€侷nfraNumber packing-program candidate alias. */
typealias InfraMaterialPackingProgramCandidate = MaterialPackingProgramCandidate<MaterialPackingNumber>

/**
 * 鍖呰鏂规閫夋嫨缁撴灉锛坸[p]锛夈€?
 * Selected package count for a program (x[p]).
 */
data class PackageSelection(
    val candidate: MaterialPackingProgramCandidate<*>,
    val amount: UInt64
)

/**
 * 鍖呰鏂规鍐呯墿鏂欏垎閰嶇粨鏋滐紙y[p,m]锛夈€?
 * Assigned material amount for a program (y[p,m]).
 */
data class MaterialPackingAssignment(
    val candidate: MaterialPackingProgramCandidate<*>,
    val material: MaterialKey,
    val amount: UInt64
)

/**
 * 鍖呰鍚庣殑 item 鍙婃暟閲忋€?
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
 * 鍖呰姹傝В淇℃伅銆?
 * Material packing solve information.
 */
data class MaterialPackingSolveInfo(
    val status: MaterialPackingStatus,
    val objective: MaterialPackingNumber? = null,
    val gap: MaterialPackingNumber? = null,
    val timeMillis: Long = 0L,
    val selectedPackageCount: UInt64 = UInt64.zero,
    val rawStatus: String? = null
)

/**
 * 鐗╂枡鍖呰瑙勫垝缁撴灉銆?
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

