package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model.Compilation

class SolutionAnalyzer<E : Executor> {
    operator fun invoke(
        iteration: UInt64,
        tasks: List<Task<E>>,
        bunches: List<List<TaskBunch<E>>>,
        compilation: Compilation<E>,
        model: LinearMetaModel
    ): Ret<Solution<E>> {
        val assignedTasks = ArrayList<Task<E>>()
        val canceledTask = ArrayList<Task<E>>()
        for (token in model.tokens.tokens) {
            for ((i, xi) in compilation.x.withIndex()) {
                if (UInt64(i.toULong()) >= iteration) {
                    break
                }

                if (token.name.startsWith(xi.name) && token.result?.let { it eq Flt64.one } == true) {
                    val assignedBunch = bunches[i][token.variable.vectorView[0]]
                    for (task in assignedBunch.tasks) {
                        if (task != task.originTask) {
                            assignedTasks.add(task)
                        }
                    }
                }
            }

            if (token.name.startsWith(compilation.y.name) && token.result?.let { it eq Flt64.one } == true) {
                canceledTask.add(tasks[token.variable.vectorView[0]])
            }
        }
        return Ok(Solution(assignedTasks, canceledTask))
    }
}
