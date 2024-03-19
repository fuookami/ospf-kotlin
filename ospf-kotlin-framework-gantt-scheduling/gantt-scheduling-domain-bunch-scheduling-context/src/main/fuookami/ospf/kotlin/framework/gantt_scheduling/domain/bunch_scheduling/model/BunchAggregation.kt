package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

data class BunchAggregation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val _bunchesIteration: MutableList<List<AbstractTaskBunch<T, E, A>>> = ArrayList(),
    private val _bunches: MutableList<AbstractTaskBunch<T, E, A>> = ArrayList(),
    private val _removedBunches: MutableSet<AbstractTaskBunch<T, E, A>> = HashSet()
) {
    val bunchesIteration: List<List<AbstractTaskBunch<T, E, A>>> by ::_bunchesIteration
    val bunches: List<AbstractTaskBunch<T, E, A>> by ::_bunches
    val removedBunches: Set<AbstractTaskBunch<T, E, A>> by ::_removedBunches
    val lastIterationBunches: List<AbstractTaskBunch<T, E, A>>
        get() = _bunchesIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    suspend fun addColumns(newBunches: List<AbstractTaskBunch<T, E, A>>): List<AbstractTaskBunch<T, E, A>> {
        val unduplicatedNewBunches = ArrayList<AbstractTaskBunch<T, E, A>>()
        for (bunch in newBunches) {
            if (unduplicatedNewBunches.all { bunch neq it }) {
                unduplicatedNewBunches.add(bunch)
            }
        }

        val unduplicatedBunches = coroutineScope {
            val promises = ArrayList<Deferred<AbstractTaskBunch<T, E, A>?>>()
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

    fun removeColumn(bunch: AbstractTaskBunch<T, E, A>) {
        if (!_removedBunches.contains(bunch)) {
            _removedBunches.add(bunch)
            _bunches.remove(bunch)
        }
    }

    fun removeColumns(bunches: List<AbstractTaskBunch<T, E, A>>) {
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
