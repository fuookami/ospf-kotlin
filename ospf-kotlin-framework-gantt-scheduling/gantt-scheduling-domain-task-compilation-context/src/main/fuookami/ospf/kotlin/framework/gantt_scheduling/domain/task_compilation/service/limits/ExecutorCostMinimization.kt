/** 执行器成本最小化 / Executor cost minimization */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

/**
 * 执行器成本最小化 / Executor cost minimization
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param executors 执行器列表 / List of executors
 * @param compilation 编译结果 / Compilation result
 * @param coefficient 执行器成本系数提取器 / Extractor for executor cost coefficient
 * @param name 管道名称 / Pipeline name
 */
class ExecutorCostMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val compilation: Compilation,
    private val coefficient: Extractor<Flt64?, E>? = null,
    override val name: String = "executor_cost_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        coefficient?.let {
            when (val result = model.minimize(
                polynomial = sum(executors.map { e ->
                    val penalty = it(e) ?: Flt64.infinity
                    penalty * compilation.executorCompilation[e].toLinearPolynomial()
                }),
                name = "executor"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
