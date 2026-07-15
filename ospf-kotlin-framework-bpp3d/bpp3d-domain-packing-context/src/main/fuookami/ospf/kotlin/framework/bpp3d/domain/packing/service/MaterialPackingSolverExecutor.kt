/**
 * Material packing solver executor.
 * 物料装箱求解器执行器。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*

/**
 * Material packing MIP (Mixed Integer Programming) request.
 * 物料装箱混合整数规划请求。
 *
 * @property demands The material demand mapping.
 * 物料需求映射。
 * @property candidates The list of candidate packing programs.
 * 候选装箱方案列表。
 * @property objective The objective function configuration.
 * 目标函数配置。
*/
data class MaterialPackingMipRequest<V : FloatingNumber<V>>(
    val demands: Map<MaterialKey, UInt64>,
    val candidates: List<MaterialPackingProgramCandidate<V>>,
    val objective: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig()
)

/**
 * Material packing MIP status.
 * 物料装箱混合整数规划状态。
*/
enum class MaterialPackingMipStatus {
    Optimal,
    Infeasible
}

/**
 * Material packing MIP result.
 * 物料装箱混合整数规划结果。
 *
 * @property status The solve status.
 * 求解状态。
 * @property selections The mapping of selected candidate indices to their amounts.
 * 被选中的方案索引及其数量映射。
 * @property objective The objective function value, nullable.
 * 目标函数值，可为空。
 * @property gap The optimality gap, nullable.
 * 最优间隙，可为空。
 * @property timeMillis The solve time in milliseconds.
 * 求解耗时（毫秒）。
 * @property rawStatus The raw solver status string, nullable.
 * 原始求解器状态信息，可为空。
*/
data class MaterialPackingMipResult(
    val status: MaterialPackingMipStatus,
    val selections: Map<Int, UInt64> = emptyMap(),
    val objective: FltX? = null,
    val gap: FltX? = null,
    val timeMillis: Long = 0L,
    val rawStatus: String? = null
)

/**
 * Interface for material packing solver executor.
 * 物料装箱求解器执行器接口。
*/
interface MaterialPackingSolverExecutor {

    /**
     * Solve the material packing MIP problem.
     * 求解物料装箱混合整数规划问题。
     *
     * @param V The floating number type.
     * 浮点数类型。
     * @param request The material packing MIP request.
     * 物料装箱混合整数规划请求。
     * @return The material packing MIP result.
     * 物料装箱混合整数规划结果。
    */
    suspend fun <V : FloatingNumber<V>> solve(request: MaterialPackingMipRequest<V>): MaterialPackingMipResult
}
