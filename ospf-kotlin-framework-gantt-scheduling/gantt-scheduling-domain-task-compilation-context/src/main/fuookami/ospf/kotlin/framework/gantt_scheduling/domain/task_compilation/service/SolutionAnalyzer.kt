@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.IterativeAbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.IterativeTaskCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSchedulingTaskTime
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.concept.findOrGet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Instant

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * 将求解流程中的任务对象收敛为目标任务类型。
 * 任务列表与编译产物来自同一上下文，构建路径保证运行期类型不变量。
 *
 * Narrows task objects in the solving flow to the target task type.
 * The task list and compilation artifacts come from the same context,
 * so the construction path owns the runtime type invariant.
 */
@Suppress("UNCHECKED_CAST")
private fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > analyzedTaskOf(task: AbstractTask<E, A>): T {
    return task as T
}

data object SolutionAnalyzer {
    operator fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            > invoke(
        tasks: List<T>,
        executors: List<E>,
        compilation: TaskCompilation<T, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        assignedPolicyGenerator: (executor: E?) -> A?,
        solution: List<Flt64>? = null,
    ): Ret<TaskSolution<T, E, A>> {
        val assignedExecutor = HashMap<AbstractTask<E, A>, E>()
        for (x in compilation.x) {
            val token = model.tokens.find(x) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }.round().toUInt64()
            if (result geq UInt64.one) {
                val task = tasks.findOrGet(x.vectorView[0])
                val executor = executors.findOrGet(x.vectorView[1])
                assignedExecutor[task] = executor
            }
        }

        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()
        for (task in tasks) {
            val assignedTask = assignedPolicyGenerator(assignedExecutor[task])?.let {
                when (val result = task.assign(it)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
            if (assignedTask != null) {
                assignedTasks.add(analyzedTaskOf(assignedTask))
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }
    operator fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            > invoke(
        timeWindow: TimeWindow,
        tasks: List<T>,
        executors: List<E>,
        compilation: TaskCompilation<T, E, A>,
        taskTime: TaskSchedulingTaskTime<T, E, A>,
        results: List<Flt64>,
        model: AbstractLinearMetaModel<Flt64>,
        assignedPolicyGenerator: (time: TimeRange?, executor: E?) -> A?,
        solution: List<Flt64>? = null,
    ): Ret<TaskSolution<T, E, A>> {
        val assignedExecutor = HashMap<AbstractTask<E, A>, E>()
        for (x in compilation.x) {
            val token = model.tokens.find(x) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }.round().toUInt64()
            if (result geq UInt64.one) {
                val task = tasks.findOrGet(x.vectorView[0])
                val executor = executors.findOrGet(x.vectorView[1])
                assignedExecutor[task] = executor
            }
        }

        val assignedEST = HashMap<AbstractTask<E, A>, Instant>()
        for (est in taskTime.est) {
            val task = tasks.findOrGet(est.index)
            if (!assignedExecutor.containsKey(task)) {
                continue
            }

            val token = model.tokens.find(est) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }
            assignedEST[task] = timeWindow.instantOf(result)
        }

        val assignedECT = assignedExecutor.entries.associate { (task, _) ->
            task to ((taskTime.estimateEndTime[task] as IntermediateSymbol<Flt64>).evaluate(results, model.tokens, flt64Converter)?.let {
                with(timeWindow) { it.instant }
            } ?: Instant.DISTANT_FUTURE)
        }

        val assignedTime = assignedExecutor.entries.mapNotNull { (task, _) ->
            if (assignedEST.containsKey(task) && assignedECT.containsKey(task)) {
                task to TimeRange(assignedEST[task]!!, assignedECT[task]!!)
            } else {
                null
            }
        }.toMap()

        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()
        for (task in tasks) {
            val assignedTask = assignedPolicyGenerator(assignedTime[task], assignedExecutor[task])?.let {
                when (val result = task.assign(it)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
            if (assignedTask != null) {
                assignedTasks.add(analyzedTaskOf(assignedTask))
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }
    operator fun <
            IT : IterativeAbstractTask<E, A>,
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>
            > invoke(
        iteration: UInt64,
        originTasks: List<T>,
        tasks: List<List<IT>>,
        compilation: IterativeTaskCompilation<IT, T, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>? = null,
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
                if (result geq UInt64.one) {
                    val task = tasks[i].findOrGet(x.index)
                    assignedTasks.add(analyzedTaskOf(task))
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
                val task = originTasks.findOrGet(y.index)
                canceledTasks.add(task)
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }
}


