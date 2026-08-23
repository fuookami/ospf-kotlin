package fuookami.ospf.kotlin.framework.network_scheduling.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.VrptwSolveResult

/**
 * 解后处理器。 / Solution post-processor.
 *
 * 在 VrptwApplicationService.solve() 末尾调用，可对求解结果做后处理
 * （如添加汇总 KPI、转换单位、记录日志等），无需修改主流程。 / Called at the end of VrptwApplicationService.solve(), enabling post-processing
 * of the solve result (e.g., adding aggregate KPIs, unit conversion, logging)
 * without modifying the main flow.
 */
fun interface SolutionEnricher<V : RealNumber<V>> {
    /**
     * 对求解结果做后处理，返回增强后的结果。 / Post-process the solve result, returning the enriched result.
     */
    fun enrich(result: VrptwSolveResult<V>): VrptwSolveResult<V>
}
