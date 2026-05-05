@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class PassengerContext {
    lateinit var aggregation: Aggregation
    lateinit var pipelineList: CGPipelineList

    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = aggregation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::pipelineList.isInitialized) {
            pipelineList = when (val result = PipelineListGenerator(aggregation)()) {
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
        when (val result = pipelineList(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}










