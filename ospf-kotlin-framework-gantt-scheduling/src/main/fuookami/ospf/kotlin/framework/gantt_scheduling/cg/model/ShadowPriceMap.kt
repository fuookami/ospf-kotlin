package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.Extractor
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*

@Suppress("UNCHECKED_CAST")
fun <M : OriginShadowPriceMap<M>, E : Executor> wrap(extractor: (OriginShadowPriceMap<M>, Task<E>?, Task<E>?, E) -> Flt64): Extractor<M> {
    return { map, args -> extractor(map, args[0] as Task<E>?, args[1] as Task<E>?, args[2] as E) }
}

class ShadowPriceMap<E : Executor> : AbstractShadowPriceMap<ShadowPriceMap<E>>() {
    operator fun invoke(prevTask: Task<E>?, thisTask: Task<E>?, executor: E): Flt64 {
        return super.invoke(prevTask, thisTask, executor)
    }

    fun reducedCost(bunch: TaskBunch<E>): Flt64 {
        var ret = bunch.cost.sum!!
        if (bunch.executor.indexed) {
            ret -= this(null, null, bunch.executor)
            for ((index, task) in bunch.tasks.withIndex()) {
                val prevTask = if (index != 0) {
                    bunch.tasks[index - 1]
                } else {
                    bunch.lastTask
                }
                ret -= this(prevTask, task, bunch.executor)
            }
            if (bunch.tasks.isNotEmpty()) {
                ret -= this(bunch.tasks.last(), null, bunch.executor)
            }
        }
        return ret
    }
}
