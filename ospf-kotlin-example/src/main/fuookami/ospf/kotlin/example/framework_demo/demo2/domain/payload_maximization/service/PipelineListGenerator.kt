package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * 生成用于模型构建的载荷最大化目标管线。Generates the pipeline of payload maximization objective for model construction.
 *
 * @property private val aggregation 参数。
 */
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
