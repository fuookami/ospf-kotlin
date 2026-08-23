package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 从飞机、装载和 MAC 上下文初始化适航安全聚合。Initializes the airworthiness security aggregation from aircraft, stowage, and MAC contexts. */
data object AggregationInitializer {
    operator fun invoke(
        aircraftAggregation: AircraftAggregation,
        stowageAggregation: StowageAggregation,
        macAggregation: MACAggregation,
        input: RequestDTO
    ): Ret<Aggregation> {
        val aircraftModel = aircraftAggregation.aircraftModel
        val positions = stowageAggregation.positions
        if (positions.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "适航安全聚合至少需要一个装载位置 / Airworthiness aggregation requires at least one stowage position"
            )
        }
        if (!input.maxCumulativeForwardLoad.isFinite() || input.maxCumulativeForwardLoad < 0.0
            || !input.maxCumulativeBackwardLoad.isFinite() || input.maxCumulativeBackwardLoad < 0.0
            || !input.maxAdjacentLoadGap.isFinite() || input.maxAdjacentLoadGap < 0.0
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "适航载荷上限必须为有限非负值 / Airworthiness load limits must be finite and non-negative"
            )
        }
        if (!input.payloadUpperBound.isFinite() || input.payloadUpperBound < 0.0
            || !input.minPayloadRatio.isFinite() || input.minPayloadRatio < 0.0 || input.minPayloadRatio > 1.0
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "业载边界必须为有限值且最小比例位于 [0, 1] / Payload bounds must be finite and the minimum ratio must be within [0, 1]"
            )
        }
        if (!input.envelopeLongitudinalMomentMin.isFinite()
            || !input.envelopeLongitudinalMomentMax.isFinite()
            || input.envelopeLongitudinalMomentMin > input.envelopeLongitudinalMomentMax
            || !input.targetLongitudinalMoment.isFinite()
            || !input.maxLongitudinalMomentDeviation.isFinite()
            || input.maxLongitudinalMomentDeviation < 0.0
            || !input.maxLateralImbalance.isFinite()
            || input.maxLateralImbalance < 0.0
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "力矩与横向平衡边界必须为有效有限区间 / Moment and lateral-balance bounds must be valid finite intervals"
            )
        }

        val lengthUnit = aircraftModel.lengthUnit
        val forwardPositions = positions.sortedBy {
            it.coordinate.backArm.to(lengthUnit)!!.value.toDouble()
        }
        val aftPositions = positions.sortedByDescending {
            it.coordinate.frontArm.to(lengthUnit)!!.value.toDouble()
        }
        val cumulativeLoadWeight = MaxCumulativeLoadWeight(listOf(
            MaxCumulativeLoadWeight.LimitZone(
                direction = MaxCumulativeLoadWeight.Direction.FWD,
                fromArm = forwardPositions.first().coordinate.frontArm,
                points = forwardPositions.map { position ->
                    MaxCumulativeLoadWeight.Point(
                        toArm = position.coordinate.backArm,
                        maxSum = Quantity(
                            Flt64(input.maxCumulativeForwardLoad),
                            aircraftModel.weightUnit
                        )
                    )
                },
                positions = positions
            ),
            MaxCumulativeLoadWeight.LimitZone(
                direction = MaxCumulativeLoadWeight.Direction.AFT,
                fromArm = aftPositions.first().coordinate.backArm,
                points = aftPositions.map { position ->
                    MaxCumulativeLoadWeight.Point(
                        toArm = position.coordinate.frontArm,
                        maxSum = Quantity(
                            Flt64(input.maxCumulativeBackwardLoad),
                            aircraftModel.weightUnit
                        )
                    )
                },
                positions = positions
            )
        ))
        val totalCargoWeight = stowageAggregation.items.fold(0.0) { total, item ->
            total + item.weight.to(aircraftModel.weightUnit)!!.value.toDouble()
        }
        val minPayload = Quantity(
            Flt64(minOf(input.payloadUpperBound, totalCargoWeight) * input.minPayloadRatio),
            aircraftModel.weightUnit
        )
        val minLowPayload = if (positions.any { position ->
                position.location.location == DeckLocation.LowForward || position.location.location == DeckLocation.LowAft
            }
        ) {
            minPayload
        } else {
            Quantity(Flt64.zero, aircraftModel.weightUnit)
        }
        val minLowPayloadPoints = listOf(
            MinLowPayload.Point(
                minLowPayload = minLowPayload,
                zfw = Quantity(Flt64.zero, aircraftModel.weightUnit)
            ),
            MinLowPayload.Point(
                minLowPayload = minLowPayload,
                zfw = Quantity(Flt64(input.payloadUpperBound), aircraftModel.weightUnit)
            )
        )

        val densityLocations = positions.map { it.location.location }.toSet()
        val densityFrontArm = positions.minBy {
            it.coordinate.frontArm.to(lengthUnit)!!.value.toDouble()
        }.coordinate.frontArm
        val densityBackArm = positions.maxBy {
            it.coordinate.backArm.to(lengthUnit)!!.value.toDouble()
        }.coordinate.backArm
        val linearDensityLimitZones = listOf(
            LinearDensity.LimitZone(
                name = "request_linear_density",
                locations = densityLocations,
                frontArm = densityFrontArm,
                backArm = densityBackArm,
                maxLinearDensity = Quantity(Flt64(500.0), aircraftModel.linearDensityUnit)
            )
        )
        val surfaceDensityLimitZones = listOf(
            SurfaceDensity.LimitZone(
                name = "request_surface_density",
                locations = densityLocations,
                frontArm = densityFrontArm,
                backArm = densityBackArm,
                maxSurfaceDensity = Quantity(Flt64(1000.0), aircraftModel.surfaceDensityUnit)
            )
        )

        val envelopePoints = listOf(
            AbstractEnvelope.Point(
                totalWeight = Quantity(Flt64.zero, aircraftModel.weightUnit),
                index = Quantity(Flt64(input.envelopeLongitudinalMomentMin), aircraftModel.torqueUnit)
            ),
            AbstractEnvelope.Point(
                totalWeight = Quantity(
                    Flt64(maxOf(1.0, input.payloadUpperBound, totalCargoWeight)),
                    aircraftModel.weightUnit
                ),
                index = Quantity(Flt64(input.envelopeLongitudinalMomentMin), aircraftModel.torqueUnit)
            )
        )
        val envelopeUpperPoints = envelopePoints.map {
            it.copy(index = Quantity(Flt64(input.envelopeLongitudinalMomentMax), aircraftModel.torqueUnit))
        }
        val maxClim = maxOf(
            kotlin.math.abs(input.envelopeLongitudinalMomentMin),
            kotlin.math.abs(input.envelopeLongitudinalMomentMax)
        )
        val maxCLIMPoints = if (aircraftModel.wideBody) {
            listOf(
                MaxCLIM.Point(
                    tow = Quantity(Flt64.zero, aircraftModel.weightUnit),
                    maxCLIM = Quantity(Flt64(maxClim), aircraftModel.torqueUnit)
                ),
                MaxCLIM.Point(
                    tow = Quantity(
                        Flt64(maxOf(1.0, input.payloadUpperBound, totalCargoWeight)),
                        aircraftModel.weightUnit
                    ),
                    maxCLIM = Quantity(Flt64(maxClim), aircraftModel.torqueUnit)
                )
            )
        } else {
            null
        }

        return Ok(Aggregation(
            aircraftModel = aircraftModel,
            fuselage = aircraftAggregation.fuselage,
            positions = positions,
            linearDensityLimitZones = linearDensityLimitZones,
            surfaceDensityLimitZones = surfaceDensityLimitZones,
            maxZoneLoadWeight = MaxZoneLoadWeight(
                aircraftModel = aircraftModel,
                limitZones = emptyList(),
                load = stowageAggregation.load
            ),
            maxCumulativeLoadWeight = cumulativeLoadWeight,
            minPayload = minPayload,
            envelopeLongitudinalMomentMin = Quantity(
                Flt64(input.envelopeLongitudinalMomentMin),
                aircraftModel.torqueUnit
            ),
            envelopeLongitudinalMomentMax = Quantity(
                Flt64(input.envelopeLongitudinalMomentMax),
                aircraftModel.torqueUnit
            ),
            targetLongitudinalMoment = Quantity(
                Flt64(input.targetLongitudinalMoment),
                aircraftModel.torqueUnit
            ),
            maxLongitudinalMomentDeviation = Quantity(
                Flt64(input.maxLongitudinalMomentDeviation),
                aircraftModel.torqueUnit
            ),
            maxLateralImbalance = Quantity(
                Flt64(input.maxLateralImbalance),
                aircraftModel.torqueUnit
            ),
            maxCLIMPoints = maxCLIMPoints,
            minLowPayloadPoints = minLowPayloadPoints,
            envelopeBuilders = { phase, totalWeight ->
                listOf(
                    Envelope(
                        aircraftModel = aircraftModel,
                        phase = phase,
                        name = "request_envelope_${phase.name.lowercase()}",
                        lhsSide = AbstractEnvelope.Side(
                            aircraftModel = aircraftModel,
                            name = "request_envelope_${phase.name.lowercase()}_min",
                            type = AbstractEnvelope.SideType.Left,
                            points = envelopePoints
                        ),
                        rhsSide = AbstractEnvelope.Side(
                            aircraftModel = aircraftModel,
                            name = "request_envelope_${phase.name.lowercase()}_max",
                            type = AbstractEnvelope.SideType.Right,
                            points = envelopeUpperPoints
                        ),
                        totalWeight = totalWeight
                    )
                )
            },
            load = stowageAggregation.load,
            payload = stowageAggregation.payload,
            totalWeight = stowageAggregation.totalWeight,
            ballast = stowageAggregation.ballast,
            torque = macAggregation.torque,
            horizontalStabilizers = macAggregation.horizontalStabilizers,
            stowage = stowageAggregation.stowage,
            maxAdjacentLoadGap = input.maxAdjacentLoadGap
        ))
    }
}
