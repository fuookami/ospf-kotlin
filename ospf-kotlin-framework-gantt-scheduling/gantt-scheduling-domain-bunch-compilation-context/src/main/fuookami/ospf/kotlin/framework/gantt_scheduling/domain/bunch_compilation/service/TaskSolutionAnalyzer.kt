@file:Suppress("DEPRECATION")

/** 任务束上下文任务解分析器 / Task solution analyzer in bunch context */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSolution
import fuookami.ospf.kotlin.utils.concept.findOrGet
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/** 任务束上下文任务解分析器 / Task solution analyzer in bunch context */
data object TaskSolutionAnalyzer {
    /**
     * 从任务束编译 solver 解中提取任务解 / Extract task solution from a bunch-compilation solver solution
     *
     * `model` 与 `solution` 保留 `Flt64`，因为这里直接读取 solver token 结果；
     * 返回值已恢复为领域任务解。
     *
     * `model` and `solution` keep `Flt64` because this analyzer reads solver token results directly;
     * the returned value is restored to a domain task solution.
     *
     * @param B 任务束类型 / Task bunch type
     * @param V 任务束数值类型 / Task bunch numeric type
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param iteration 当前迭代号 / Current iteration number
     * @param tasks 任务列表 / List of tasks
     * @param bunches 按迭代分组的任务束 / Task bunches grouped by iteration
     * @param compilation 任务束编译结果 / Bunch compilation result
     * @param model solver 模型边界 / Solver model boundary
     * @param solution 可选 solver 解向量 / Optional solver solution vector
     * @return 任务解 / Task solution
     */
    operator fun <
            B : AbstractTaskBunch<T, E, A, V>,
            V : RealNumber<V>,
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            > invoke(
        iteration: UInt64,
        tasks: List<T>,
        bunches: List<List<B>>,
        compilation: BunchCompilation<B, V, T, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>? = null
    ): Ret<TaskSolution<T, E, A>> {
        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()

        for ((i, xi) in compilation.x.withIndex()) {
            if (UInt64(i.toULong()) > iteration) {
                break
            }

            for (x in xi) {
                val token = model.tokens.find(x) ?: continue
                val result = if (token.result != null) {
                    token.result!!
                } else {
                    val index = model.tokens.indexOf(token) ?: continue
                    solution?.get(index) ?: continue
                }.round().toUInt64()
                if (result eq UInt64.one) {
                    val bunch = bunches[i].findOrGet(x.index)
                    for (task in bunch.tasks) {
                        when (val policy = task.assignmentPolicy) {
                            null -> {}

                            else -> {
                                if (!policy.empty) {
                                    assignedTasks.add(task)
                                }
                            }
                        }
                    }
                }
            }
        }

        for (y in compilation.y) {
            val token = model.tokens.find(y) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }.round().toUInt64()
            if (result eq UInt64.one) {
                canceledTasks.add(tasks.findOrGet(y.index))
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }
}
