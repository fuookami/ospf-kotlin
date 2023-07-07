package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*

class Makespan<E : Executor>(
    val extra: Boolean = false
) {
    lateinit var makespan: Symbol<Linear>

    fun register(tasks: List<Task<E>>, taskTime: TaskTime<E>, model: LinearMetaModel): Try<Error> {
        if (!this::makespan.isInitialized) {
            makespan = MaxFunction(tasks.map { LinearPolynomial(taskTime.ect[it]!!) }, extra, name = "makespan")
        }
        model.addSymbol(makespan)

        return Ok(success)
    }
}
