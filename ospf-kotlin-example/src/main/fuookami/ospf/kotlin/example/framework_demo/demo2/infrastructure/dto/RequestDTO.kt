package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

@Serializable
data class CargoInput(
    val name: String,
    val weight: Double,
    val priority: Int,
    val source: String,
    val destination: String,
    val requiresSeparation: Boolean = false
)

@Serializable
data class PositionInput(
    val name: String,
    val maxWeight: Double,
    val longitudinalArm: Double,
    val lateralArm: Double
)

@Serializable
enum class AircraftTypeInput {
    B737, B757, B767, B747, Unknown
}

@Serializable
data class RequestDTO(
    val id: String,
    val cargos: List<CargoInput> = emptyList(),
    val positions: List<PositionInput> = emptyList(),
    val aircraftType: AircraftTypeInput = AircraftTypeInput.B737,
    val solvePolicy: SolvePolicy = SolvePolicy(),
    val bendersAdaptive: BendersAdaptiveConfig = BendersAdaptiveConfig(),
    val bendersQualityOverrides: BendersQualityOverrideConfig? = null,
    val weightRecommendationObjective: WeightRecommendationObjectiveConfig = WeightRecommendationObjectiveConfig(),
    val payloadUpperBound: Double = 20.0,
    val minPayloadRatio: Double = 0.6,
    val maxAdjacentLoadGap: Double = 8.0,
    val maxCumulativeForwardLoad: Double = 20.0,
    val maxCumulativeBackwardLoad: Double = 20.0,
    val envelopeLongitudinalMomentMin: Double = -20.0,
    val envelopeLongitudinalMomentMax: Double = 20.0,
    val targetLongitudinalMoment: Double = 0.0,
    val maxLongitudinalMomentDeviation: Double = 20.0,
    val maxLateralImbalance: Double = 12.0
) {
    val parameter: Parameter get() {
        TODO("Not yet implemented")
    }

    companion object {
        fun sample(): RequestDTO = RequestDTO(
            id = "sample-001",
            cargos = listOf(
                CargoInput(name = "C1", weight = 8.0, priority = 10, source = "S1", destination = "D1", requiresSeparation = true),
                CargoInput(name = "C2", weight = 6.0, priority = 6, source = "S2", destination = "D1", requiresSeparation = false),
                CargoInput(name = "C3", weight = 4.0, priority = 4, source = "S1", destination = "D2", requiresSeparation = true)
            ),
            positions = listOf(
                PositionInput(name = "P1", maxWeight = 10.0, longitudinalArm = -1.0, lateralArm = -0.5),
                PositionInput(name = "P2", maxWeight = 10.0, longitudinalArm = 1.0, lateralArm = 0.5)
            ),
            aircraftType = AircraftTypeInput.B737,
            solvePolicy = SolvePolicy(preferBenders = false, bendersFallbackToMilp = true),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6),
            payloadUpperBound = 20.0,
            minPayloadRatio = 0.6,
            maxAdjacentLoadGap = 8.0,
            maxCumulativeForwardLoad = 20.0,
            maxCumulativeBackwardLoad = 20.0,
            envelopeLongitudinalMomentMin = -20.0,
            envelopeLongitudinalMomentMax = 20.0,
            targetLongitudinalMoment = 0.0,
            maxLongitudinalMomentDeviation = 20.0,
            maxLateralImbalance = 12.0
        )
    }
}