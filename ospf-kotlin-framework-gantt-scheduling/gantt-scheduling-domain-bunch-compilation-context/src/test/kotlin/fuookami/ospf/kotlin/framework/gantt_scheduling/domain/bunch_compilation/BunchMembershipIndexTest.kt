@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import kotlin.test.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchCompilation

class BunchMembershipIndexTest {
    @Test
    fun `bunch buckets match membership scans across batches and custom predicates`(): Unit = runBlocking {
        val time = TimeRange(Instant.parse("2026-10-24T00:00:00Z"), Instant.parse("2026-10-24T01:00:00Z"))
        val tasks = (0..3).map { IndexedTask(it, TestTaskId("t$it"), time = time) }
        val executors = (0..1).map { Executor(TestExecutorId("e$it"), "e$it") }
        val first = executors.mapIndexed { index, executor ->
            AbstractTaskBunch(
                executor = executor,
                initialUsability = ExecutorInitialUsability<IndexedTask, Executor, AssignmentPolicy<Executor>>(null, time.start),
                tasks = if (index == 0) tasks.take(2) else listOf(tasks[1], tasks[1], tasks[2]),
                cost = ImmutableCost(emptyList(), Quantity(Flt64.zero, NoneUnit)),
                iteration = Int64.zero
            )
        }
        var membershipCalls = 0
        val customTaskId = tasks[2].id
        val custom = object : AbstractTaskBunch<IndexedTask, Executor, AssignmentPolicy<Executor>, Flt64>(
            executor = executors[0],
            initialUsability = ExecutorInitialUsability(null, time.start),
            tasks = listOf(tasks[3]),
            cost = ImmutableCost(emptyList(), Quantity(Flt64.zero, NoneUnit)),
            iteration = Int64.one
        ) {
            override fun contains(task: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean {
                membershipCalls++
                return task.id == customTaskId
            }
        }
        val compilation = BunchCompilation<AbstractTaskBunch<IndexedTask, Executor, AssignmentPolicy<Executor>, Flt64>, Flt64, IndexedTask, Executor, AssignmentPolicy<Executor>>(
            tasks = tasks,
            executors = executors
        )
        val model = LinearMetaModel<Flt64>(name = "bunch-membership", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(compilation.register(model))
        for ((t, task) in tasks.withIndex()) {
            for ((e, executor) in executors.withIndex()) {
                assertIs<Ok<*, *, *>>(model.addConstraint(LinearPolynomial(compilation.taskAssignment[task, executor]) leq Flt64.one, name = "assign_${t}_$e"))
            }
            assertIs<Ok<*, *, *>>(model.addConstraint(LinearPolynomial(compilation.taskCompilation[task]) leq Flt64.one, name = "task_$t"))
        }
        for ((e, executor) in executors.withIndex()) {
            assertIs<Ok<*, *, *>>(model.addConstraint(LinearPolynomial(compilation.executorCompilation[executor]) leq Flt64.one, name = "executor_$e"))
        }
        val active = ArrayList<Pair<AbstractTaskBunch<IndexedTask, Executor, AssignmentPolicy<Executor>, Flt64>, fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>>>()
        for ((iteration, batch) in listOf(first, listOf(custom)).withIndex()) {
            val added = assertIs<Ok<List<AbstractTaskBunch<IndexedTask, Executor, AssignmentPolicy<Executor>, Flt64>>, *, *>>(compilation.addColumns(
                iteration = UInt64(iteration),
                newBunches = batch,
                model = model
            )).value
            if (iteration == 1) assertEquals(tasks.size, membershipCalls)
            active.addAll(added.map { it to compilation.x[iteration][it] })
            val snapshot = assertIs<Ok<LinearModelSnapshot<Flt64>, *, *>>(model.state.refresh()).value
            for ((t, task) in tasks.withIndex()) {
                val row = snapshot.rows.single { it.sourceName == "task_$t" }
                for ((bunch, variable) in active) {
                    assertEquals(if (bunch.contains(task)) Flt64.one else Flt64.zero, row.coefficients[variable] ?: Flt64.zero)
                }
                assertEquals(Flt64.one, row.coefficients[compilation.y[task]])
                for ((e, executor) in executors.withIndex()) {
                    val assignment = snapshot.rows.single { it.sourceName == "assign_${t}_$e" }
                    for ((bunch, variable) in active) {
                        assertEquals(if (bunch.executor == executor && bunch.contains(task)) Flt64.one else Flt64.zero, assignment.coefficients[variable] ?: Flt64.zero)
                    }
                }
            }
            for ((e, executor) in executors.withIndex()) {
                val row = snapshot.rows.single { it.sourceName == "executor_$e" }
                for ((bunch, variable) in active) {
                    assertEquals(if (bunch.executor == executor) Flt64.one else Flt64.zero, row.coefficients[variable] ?: Flt64.zero)
                }
                assertEquals(Flt64.one, row.coefficients[compilation.z[executor]])
            }
        }
        model.close()
    }
}

private data class IndexedTask(
    override val index: Int,
    override val id: TaskId,
    override val name: String = id.toString(),
    override val time: TimeRange
) : AbstractTask<Executor, AssignmentPolicy<Executor>> {
    override val duration = 1.hours

    override fun partialEq(rhs: AbstractTask<Executor, AssignmentPolicy<Executor>>): Boolean? = this === rhs
}
