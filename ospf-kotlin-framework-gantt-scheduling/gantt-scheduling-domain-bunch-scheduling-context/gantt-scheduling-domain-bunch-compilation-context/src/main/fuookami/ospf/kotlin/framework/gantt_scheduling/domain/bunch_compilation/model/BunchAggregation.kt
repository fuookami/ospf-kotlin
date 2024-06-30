package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

data class BunchAggregation<
    B : AbstractTaskBunch<T, E, A>,
    out T : AbstractTask<E, A>, 
    out E : Executor, 
    out A : AssignmentPolicy<E>
>(
    private val _bunchesIteration: MutableList<List<B>> = ArrayList(),
    private val _bunches: MutableList<B> = ArrayList(),
    private val _removedBunches: MutableSet<B> = HashSet()
) {
    val bunchesIteration: List<List<B>> by ::_bunchesIteration
    val bunches: List<B> by ::_bunches
    val removedBunches: Set<B> by ::_removedBunches
    val lastIterationBunches: List<B>
        get() = _bunchesIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    suspend fun addColumns(newBunches: List<B>): List<B> {
        val unduplicatedNewBunches = coroutineScope {
            val promises = ArrayList<Deferred<List<B>>>()
            for (bunches in newBunches.groupBy { Pair(it.executor, it.tasks.size) }.values) {
                promises.add(async(Dispatchers.Default) {
                    val unduplicatedNewBunches = ArrayList<B>()
                    for (bunch in bunches) {
                        if (unduplicatedNewBunches.all { bunch neq it }) {
                            unduplicatedNewBunches.add(bunch)
                        }
                    }
                    unduplicatedNewBunches
                })
            }
            promises.flatMap { it.await() }
        }

        val unduplicatedBunches = coroutineScope {
            val promises = ArrayList<Deferred<B?>>()
            for (bunch in unduplicatedNewBunches) {
                promises.add(async(Dispatchers.Default) {
                    if (_bunches.all { bunch neq it }) {
                        bunch
                    } else {
                        null
                    }
                })
            }
            promises.mapNotNull { it.await() }
        }

        ManualIndexed.flush(AbstractTaskBunch::class)
        for (bunch in unduplicatedBunches) {
            bunch.setIndexed(AbstractTaskBunch::class)
        }
        _bunchesIteration.add(unduplicatedBunches)
        _bunches.addAll(unduplicatedBunches)

        return unduplicatedBunches
    }

    fun removeColumn(bunch: B) {
        if (!_removedBunches.contains(bunch)) {
            _removedBunches.add(bunch)
            _bunches.remove(bunch)
        }
    }

    fun removeColumns(bunches: List<B>) {
        for (bunch in bunches) {
            removeColumn(bunch)
        }
    }

    fun clear() {
        _bunchesIteration.clear()
        _bunches.clear()
        _removedBunches.clear()
    }
}
