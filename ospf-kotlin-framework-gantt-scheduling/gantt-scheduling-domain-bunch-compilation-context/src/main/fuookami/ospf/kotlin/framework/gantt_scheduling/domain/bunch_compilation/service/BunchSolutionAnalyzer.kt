/** 任务束解分析器 / Bunch solution analyzer */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.concept.findOrGet
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/** 任务束解分析器 / Bunch solution analyzer */
data object BunchSolutionAnalyzer {
    /**
     * 从任务束编译 solver 解中提取任务束解 / Extract bunch solution from a bunch-compilation solver solution
     *
     * `model` 与 `solution` 保留 `Flt64`，因为这里直接读取 solver token 结果；
     * 返回值已恢复为领域任务束解。
     *
     * `model` and `solution` keep `Flt64` because this analyzer reads solver token results directly;
     * the returned value is restored to a domain bunch solution.
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
     * @return 任务束解 / Bunch solution
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
    ): Ret<BunchSolution<B, V, T, E, A>> {
        val assignedBunches = ArrayList<B>()
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
                    assignedBunches.add(bunches[i].findOrGet(x.index))
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

        return Ok(BunchSolution(assignedBunches, canceledTasks))
    }
}
