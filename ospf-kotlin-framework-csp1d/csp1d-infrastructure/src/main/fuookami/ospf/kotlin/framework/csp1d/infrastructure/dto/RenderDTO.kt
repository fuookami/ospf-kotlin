package fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.math.*

enum class RenderProductionType {
    Product,
    Costar,
    DefectCostar
}

@Serializable
data class RenderCuttingPlanProductionDTO(
    val name: String,
    val x: FltX,
    val width: FltX,
    val unitLength: FltX,
    val productionType: RenderProductionType,
    val info: Map<String, String>
)

@Serializable
data class RenderCuttingPlanDTO(
    val group: List<String>,
    val productions: List<RenderCuttingPlanProductionDTO>,
    val width: FltX,
    val standardWidth: FltX,
    val amount: UInt64,
    val info: Map<String, String>
)

@Serializable
data class RenderSchemaDTO(
    val kpi: Map<String, String>,
    val cuttingPlans: List<RenderCuttingPlanDTO>,
)
