/**
 * Continuous radius selection result extractor that builds CylinderRadiusSelectionResult lists
 * from native and PWL results.
 * 连续半径选择结果提取器，从 native 和 PWL 结果构建 CylinderRadiusSelectionResult 列表。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 从 solver 变量原型和选出结果构建连续半径已选择结果列表（native 路径）。
 * Build continuous-radius selection results from solver prototypes and solver-selected values (native path).
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param solverResults solver 选出半径映射 / solver-selected radius map
 * @return 连续半径已选择结果列表 / continuous-radius selection result list
 */
fun buildNativeContinuousRadiusSelectionResults(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    solverResults: Map<String, FltX>
): List<CylinderRadiusSelectionResult> {
    if (solverResults.isEmpty()) return emptyList()
    val results = ArrayList<CylinderRadiusSelectionResult>()
    for (prototype in prototypes) {
        val solverValue = solverResults[prototype.variableName] ?: continue
        val selectionResult = prototype.withSolverSelectedRadius(
            solverRadius = Quantity(solverValue, Meter)
        )
        results.add(selectionResult)
    }
    return results
}

/**
 * 从 PWL solver 变量原型和 opaque Map 结果构建连续半径已选择结果列表。
 * Build continuous-radius selection results from PWL solver prototypes and opaque Map results.
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param pwlContinuousRadiusResults PWL 连续半径结果 opaque Map / PWL continuous-radius results opaque Map
 * @return 连续半径已选择结果列表（包含 PWL 元数据）/ continuous-radius selection result list (with PWL metadata)
 */
fun buildPWLContinuousRadiusSelectionResults(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    pwlContinuousRadiusResults: Map<String, Map<String, FltX>>
): List<CylinderRadiusSelectionResult> {
    if (pwlContinuousRadiusResults.isEmpty()) return emptyList()
    val results = ArrayList<CylinderRadiusSelectionResult>()
    for ((variableName, values) in pwlContinuousRadiusResults) {
        val prototype = prototypes.firstOrNull { it.variableName == variableName } ?: continue
        val solverRadius = values["solverRadius"] ?: continue
        val solverRadiusSquared = values["solverRadiusSquared"] ?: continue
        val actualRadiusSquared = values["actualRadiusSquared"] ?: continue
        val pwlAbsoluteError = values["pwlAbsoluteError"] ?: FltX.zero
        val pwlRelativeError = values["pwlRelativeError"] ?: FltX.zero
        val isWithinEnvelope = (values["isWithinEnvelope"] ?: FltX.zero).toDouble() > 0.5
        val maxPWLRelativeError = values["maxPWLRelativeError"] ?: FltX.zero
        val numSegments = (values["numSegments"] ?: FltX.zero).toDouble().toInt().coerceAtLeast(1)

        // Build PWL metadata with reconstructed approximation info
        val pwlMetadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = solverRadiusSquared,
            actualRadiusSquared = actualRadiusSquared,
            pwlAbsoluteError = pwlAbsoluteError,
            pwlRelativeError = pwlRelativeError,
            maxPWLRelativeError = maxPWLRelativeError,
            numSegments = numSegments,
            isWithinEnvelope = isWithinEnvelope
        )
        val selectionResult = prototype.withPWLSolverSelectedRadius(
            solverRadius = Quantity(solverRadius, Meter),
            pwlMetadata = pwlMetadata
        )
        results.add(selectionResult)
    }
    return results
}

/**
 * 从 PWL 提取结果列表直接构建连续半径已选择结果列表（结构化路径，无需 opaque Map）。
 * Build continuous-radius selection results directly from PWL extracted results (structured path, no opaque Map needed).
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param extractedResults PWL 提取结果列表 / PWL extracted results
 * @return 连续半径已选择结果列表（包含 PWL 元数据）/ continuous-radius selection result list (with PWL metadata)
 */
fun buildPWLSelectionResultsFromExtracted(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    extractedResults: List<PWLExtractedRadius>
): List<CylinderRadiusSelectionResult> {
    if (extractedResults.isEmpty()) return emptyList()
    val results = ArrayList<CylinderRadiusSelectionResult>()
    for (extracted in extractedResults) {
        val prototype = prototypes.firstOrNull { it.variableName == extracted.variableName } ?: continue
        val pwlMetadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = extracted.solverRadiusSquared,
            actualRadiusSquared = extracted.actualRadiusSquared,
            pwlAbsoluteError = extracted.pwlAbsoluteError,
            pwlRelativeError = extracted.pwlRelativeError,
            maxPWLRelativeError = extracted.pwlApproximation.maxRelativeError,
            numSegments = extracted.pwlApproximation.numSegments,
            isWithinEnvelope = extracted.isWithinEnvelope
        )
        val selectionResult = prototype.withPWLSolverSelectedRadius(
            solverRadius = Quantity(extracted.solverRadius, Meter),
            pwlMetadata = pwlMetadata
        )
        results.add(selectionResult)
    }
    return results
}
