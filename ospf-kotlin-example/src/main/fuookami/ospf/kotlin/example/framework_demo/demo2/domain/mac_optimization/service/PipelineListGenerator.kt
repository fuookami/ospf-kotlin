package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

import fuookami.ospf.kotlin.framework.model.*

/** Generates the pipeline of MAC optimization constraints for longitudinal balance, lateral balance, and stabilizers. */
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        stowageMode: StowageMode,
        parameter: Parameter
    ): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            LongitudinalBalanceLimit(
                aircraftModel = aggregation.aircraftModel,
                longitudinalBalance = aggregation.longitudinalBalance,
                coefficient = { macRangeType ->
                    TODO("NOT IMPLEMENTED YET")
                }
            )
        )

        if (aggregation.lateralBalance != null) {
            pipelines.add(
                LateralBalanceLimit(
                    aircraftModel = aggregation.aircraftModel,
                    lateralBalance = aggregation.lateralBalance,
                    coefficient = {
                        TODO("NOT IMPLEMENTED YET")
                    }
                )
            )
        }

        pipelines.add(
            HorizontalStabilizerLimit(
                horizontalStabilizers = aggregation.horizontalStabilizers,
                coefficient = {
                    TODO("NOT IMPLEMENTED YET")
                }
            )
        )

        return Ok(pipelines)
    }
}
