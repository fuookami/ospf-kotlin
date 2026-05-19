package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

data class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(stowageMode: StowageMode): Ret<PipelineList<AbstractLinearMetaModel<Flt64>>> {
        val pipelines = ArrayList<Pipeline<AbstractLinearMetaModel<Flt64>>>()

        pipelines.add(
            LinearDensityLimit(
                aircraftModel = aggregation.aircraftModel,
                linearDensity = aggregation.linearDensity,
                positions = aggregation.positions
            )
        )

        if (aggregation.maxUnsymmetricalLinearDensity != null) {
            pipelines.add(
                UnsymmetricalLinearDensityLimit(
                    aircraftModel = aggregation.aircraftModel,
                    maxUnsymmetricalLinearDensity = aggregation.maxUnsymmetricalLinearDensity,
                    linearDensity = aggregation.linearDensity,
                    positions = aggregation.positions
                )
            )
        }

        pipelines.add(
            SurfaceDensityLimit(
                surfaceDensity = aggregation.surfaceDensity,
                positions = aggregation.positions
            )
        )

        pipelines.add(
            CumulativeLoadWeightLimit(
                aircraftModel = aggregation.aircraftModel,
                maxCumulativeLoadWeight = aggregation.maxCumulativeLoadWeight,
                positions = aggregation.positions,
                load = aggregation.load
            )
        )

        pipelines.add(
            ZoneLoadWeightLimit(
                aircraftModel = aggregation.aircraftModel,
                fuselage = aggregation.fuselage,
                maxZoneLoadWeight = aggregation.maxZoneLoadWeight,
                positions = aggregation.positions,
                load = aggregation.load
            )
        )

        if (aggregation.ballast != null) {
            pipelines.add(
                BallastWeightLimit(
                    ballast = aggregation.ballast
                )
            )
        }

        pipelines.add(
            LowPayloadLimit(
                payload = aggregation.payload,
                minLowPayload = aggregation.minLowPayload
            )
        )

        pipelines.add(
            PayloadLimit(
                payload = aggregation.payload,
            )
        )

        pipelines.add(
            TotalWeightLimit(
                totalWeight = aggregation.totalWeight
            )
        )

        pipelines.add(
            EnvelopeLimit(
                torque = aggregation.torque,
                envelopes = aggregation.envelopes
            )
        )

        pipelines.add(
            HorizontalStabilizerLimit(
                horizontalStabilizers = aggregation.horizontalStabilizers,
                stowageMode = stowageMode
            )
        )

        if (aggregation.maxCLIM != null) {
            pipelines.add(
                CLIMLimit(
                    torque = aggregation.torque,
                    maxCLIM = aggregation.maxCLIM
                )
            )
        }

        return Ok(pipelines)
    }
}
