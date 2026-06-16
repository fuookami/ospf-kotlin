package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context

import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

/** Aggregates the route graph, services, and assignment variables, and registers them with the model. */
class Aggregation(
    val graph: Graph,
    val services: List<Service>,
    val assignment: Assignment
) {
    fun register(model: LinearMetaModel<Flt64>): Try {
        val subprocesses = arrayListOf(
            { return@arrayListOf assignment.register(model) }
        )

        for (subprocess in subprocesses) {
            when (val result = subprocess()) {
                is Failed -> {
                    return result
                }

                is Fatal -> {
                    return result
                }

                is Ok -> {}
            }
        }
        return ok
    }
}
