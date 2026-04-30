package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Assignment
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Graph
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Service

class Aggregation(
    val graph: Graph,
    val services: List<Service>,
    val assignment: Assignment
) {
    fun register(model: LinearMetaModelFlt64): Try {
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









