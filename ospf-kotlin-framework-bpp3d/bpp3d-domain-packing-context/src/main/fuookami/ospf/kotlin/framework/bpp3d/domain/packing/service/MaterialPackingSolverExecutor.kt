@file:Suppress("DEPRECATION")

/**
 * 物料装箱求解器执行器。
 * Material packing solver executor.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingObjectiveConfig
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingScalar
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class MaterialPackingMipRequest<V : FloatingNumber<V>>(
    val demands: Map<MaterialKey, UInt64>,
    val candidates: List<MaterialPackingProgramCandidate<V>>,
    val objective: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig()
)

enum class MaterialPackingMipStatus {
    Optimal,
    Infeasible
}

data class MaterialPackingMipResult(
    val status: MaterialPackingMipStatus,
    val selections: Map<Int, UInt64> = emptyMap(),
    val objective: MaterialPackingScalar? = null,
    val gap: MaterialPackingScalar? = null,
    val timeMillis: Long = 0L,
    val rawStatus: String? = null
)

interface MaterialPackingSolverExecutor {
    suspend fun <V : FloatingNumber<V>> solve(request: MaterialPackingMipRequest<V>): MaterialPackingMipResult
}

