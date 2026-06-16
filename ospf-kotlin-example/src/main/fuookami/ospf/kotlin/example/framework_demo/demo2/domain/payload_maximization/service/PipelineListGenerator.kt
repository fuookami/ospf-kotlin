package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

import fuookami.ospf.kotlin.framework.model.*

/** Generates the pipeline of payload maximization objective for model construction. */
data class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            MaxPayloadLimit(
                aircraftModel = aggregation.aircraftModel,
                payload = aggregation.payload
            )
        )

        return Ok(pipelines)
    }
}
