/** 最大完工时间 / Makespan */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.symbol.function.MinMaxFunction
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 最大完工时间 / Makespan
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param taskTime 任务时间对象 / Task time object
 * @param extra 是否使用额外的最小最大函数 / Whether to use extra min-max function
 */
class Makespan<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val taskTime: TaskTime,
    private val extra: Boolean = false
) {
    lateinit var makespan: LinearIntermediateSymbol<Flt64>

    /**
     * 注册最大完工时间到模型 / Register makespan to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: MetaModel<Flt64>): Try {
        if (!::makespan.isInitialized) {
            makespan = if (extra) {
                MinMaxFunction.fromSymbols(
                    tasks.map {
                        taskTime.estimateEndTime[it]
                    },
                    converter = IntoValue.Identity,
                    name = "makespan"
                )
            } else {
                MaxFunction.fromSymbols(
                    tasks.map {
                        taskTime.estimateEndTime[it]
                    },
                    converter = IntoValue.Identity,
                    name = "makespan"
                )
            }
        }
        when (val result = model.add(makespan)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}
