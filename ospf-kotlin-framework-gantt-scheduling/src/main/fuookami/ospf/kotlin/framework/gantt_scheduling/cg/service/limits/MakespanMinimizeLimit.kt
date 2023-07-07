package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.ShadowPriceMap

class MakespanMinimizeLimit<E : Executor>(
    private val makespan: Makespan<E>,
    private val coefficient: Flt64 = Flt64.one,
    override val name: String = "makespan_limit"
) : CGPipeline<LinearMetaModel, ShadowPriceMap<E>> {
    override operator fun invoke(model: LinearMetaModel): Try<Error> {
        val makespan = makespan.makespan
        model.minimize(coefficient * LinearPolynomial(makespan), "makespan")
        return Ok(success)
    }
}
