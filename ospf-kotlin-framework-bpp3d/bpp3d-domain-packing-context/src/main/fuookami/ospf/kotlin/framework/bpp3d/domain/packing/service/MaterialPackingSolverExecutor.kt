/**
 * 物料装箱求解器执行器。
 * Material packing solver executor.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*

/**
 * 物料装箱混合整数规划请求。
 * Material packing MIP request.
 *
 * @property demands 物料需求映射
 * @property candidates 候选装箱方案列表
 * @property objective 目标函数配置
 */
data class MaterialPackingMipRequest<V : FloatingNumber<V>>(
    val demands: Map<MaterialKey, UInt64>,
    val candidates: List<MaterialPackingProgramCandidate<V>>,
    val objective: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig()
)

/**
 * 物料装箱混合整数规划状态。
 * Material packing MIP status.
 */
enum class MaterialPackingMipStatus {
    Optimal,
    Infeasible
}

/**
 * 物料装箱混合整数规划结果。
 * Material packing MIP result.
 *
 * @property status 求解状态
 * @property selections 被选中的方案索引及其数量映射
 * @property objective 目标函数值
 * @property gap 最优间隙
 * @property timeMillis 求解耗时（毫秒）
 * @property rawStatus 原始求解器状态信息
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
 * 物料装箱求解器执行器接口。
 * Interface for material packing solver executor.
 */
interface MaterialPackingSolverExecutor {
    /**
     * 求解物料装箱混合整数规划问题。
     * Solve the material packing MIP problem.
     *
     * @param V 浮点数类型
     * @param request 物料装箱混合整数规划请求
     * @return 物料装箱混合整数规划结果
     */
    suspend fun <V : FloatingNumber<V>> solve(request: MaterialPackingMipRequest<V>): MaterialPackingMipResult
}
