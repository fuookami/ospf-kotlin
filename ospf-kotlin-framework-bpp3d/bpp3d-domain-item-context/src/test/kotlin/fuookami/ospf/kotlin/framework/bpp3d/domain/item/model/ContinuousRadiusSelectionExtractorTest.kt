/**
 * 连续半径选择结果提取器测试：native 结果构建、PWL 结果构建和类型化提取。
 * Continuous radius selection result extractor tests: native result building,
 * PWL result building, and typed extraction.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ConservativeRadiusEnvelope
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLBreakpointStrategy
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusApproximationConfig
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusSquaredApproximation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

class ContinuousRadiusSelectionExtractorTest {

    // ===== 辅助方法 / Helper methods =====

    private fun nativePrototype(
        variableName: String = "native_r"
    ): ContinuousCylinderRadiusSolverPrototype {
        return ContinuousCylinderRadiusSolverPrototype(
            source = "test",
            radiusWeightFunctionKey = "key_$variableName",
            axis = Axis3.Y,
            variableName = variableName,
            radiusLowerBound = Quantity(infraScalar(2.0), Meter),
            radiusUpperBound = Quantity(infraScalar(2.0), Meter),
            initialRadius = Quantity(infraScalar(2.0), Meter),
            gaps = emptyList()
        )
    }

    private fun pwlPrototype(
        variableName: String = "pwl_r",
        rMin: Double = 2.0,
        rMax: Double = 5.0
    ): ContinuousCylinderRadiusSolverPrototype {
        return ContinuousCylinderRadiusSolverPrototype(
            source = "test",
            radiusWeightFunctionKey = "key_$variableName",
            axis = Axis3.Y,
            variableName = variableName,
            radiusLowerBound = Quantity(infraScalar(rMin), Meter),
            radiusUpperBound = Quantity(infraScalar(rMax), Meter),
            initialRadius = null,
            gaps = listOf(ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported)
        )
    }

    // ===== 1. Native 选择结果构建测试 / Native selection result building tests =====

    @Test
    fun testBuildNativeSelectionResultsFromSolverValues() {
        val prototypes = listOf(nativePrototype("r1"), nativePrototype("r2"))
        val solverResults = mapOf(
            "r1" to infraScalar(2.5),
            "r2" to infraScalar(3.0)
        )
        val results = buildNativeContinuousRadiusSelectionResults(prototypes, solverResults)
        assertEquals(2, results.size, "Should build 2 selection results")
        assertEquals("key_r1", results[0].key)
        assertEquals("key_r2", results[1].key)
        assertEquals(2.5, results[0].selectedRadius.value.toDouble(), 1e-10)
        assertEquals(3.0, results[1].selectedRadius.value.toDouble(), 1e-10)
    }

    @Test
    fun testBuildNativeSelectionResultsSkipsMissingValues() {
        val prototypes = listOf(nativePrototype("r1"), nativePrototype("r2"))
        val solverResults = mapOf("r1" to infraScalar(2.5))
        val results = buildNativeContinuousRadiusSelectionResults(prototypes, solverResults)
        assertEquals(1, results.size, "Should build 1 result (r2 has no solver value)")
        assertEquals("key_r1", results[0].key)
    }

    @Test
    fun testBuildNativeSelectionResultsEmptyInput() {
        val results = buildNativeContinuousRadiusSelectionResults(
            prototypes = emptyList(),
            solverResults = emptyMap()
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun testBuildNativeSelectionResultsHasNoPWLMetadata() {
        val prototypes = listOf(nativePrototype())
        val solverResults = mapOf("native_r" to infraScalar(2.0))
        val results = buildNativeContinuousRadiusSelectionResults(prototypes, solverResults)
        assertEquals(1, results.size)
        assertEquals(null, results[0].pwlMetadata, "Native result should not have PWL metadata")
    }

    // ===== 2. PWL 选择结果构建测试（opaque Map）/ PWL selection result building tests (opaque Map) =====

    @Test
    fun testBuildPWLSelectionResultsFromOpaqueMap() {
        val prototypes = listOf(pwlPrototype("pwl1"))
        val pwlResults = mapOf(
            "pwl1" to mapOf(
                "solverRadius" to infraScalar(3.0),
                "solverRadiusSquared" to infraScalar(9.5),
                "actualRadiusSquared" to infraScalar(9.0),
                "pwlAbsoluteError" to infraScalar(0.5),
                "pwlRelativeError" to infraScalar(0.5 / 9.0),
                "isWithinEnvelope" to InfraNumber(1.0),
                "maxPWLRelativeError" to infraScalar(0.05),
                "numSegments" to InfraNumber(4.0)
            )
        )
        val results = buildPWLContinuousRadiusSelectionResults(prototypes, pwlResults)
        assertEquals(1, results.size, "Should build 1 PWL selection result")
        val result = results[0]
        assertEquals("key_pwl1", result.key)
        assertEquals(3.0, result.selectedRadius.value.toDouble(), 1e-10)
        assertNotNull(result.pwlMetadata, "PWL result should have PWL metadata")
        assertEquals(9.0, result.pwlMetadata!!.actualRadiusSquared.toDouble(), 1e-10)
        assertEquals(9.5, result.pwlMetadata!!.solverRadiusSquared.toDouble(), 1e-10)
        assertEquals(4, result.pwlMetadata!!.numSegments)
        assertTrue(result.pwlMetadata!!.isWithinEnvelope)
    }

    @Test
    fun testBuildPWLSelectionResultsSkipsMissingPrototypes() {
        val prototypes = listOf(pwlPrototype("pwl1"))
        val pwlResults = mapOf(
            "unknown_var" to mapOf(
                "solverRadius" to infraScalar(3.0),
                "solverRadiusSquared" to infraScalar(9.0),
                "actualRadiusSquared" to infraScalar(9.0),
                "pwlAbsoluteError" to InfraNumber.zero,
                "pwlRelativeError" to InfraNumber.zero,
                "isWithinEnvelope" to InfraNumber(1.0),
                "maxPWLRelativeError" to InfraNumber.zero,
                "numSegments" to InfraNumber(4.0)
            )
        )
        val results = buildPWLContinuousRadiusSelectionResults(prototypes, pwlResults)
        assertTrue(results.isEmpty(), "Should skip unknown variable name")
    }

    @Test
    fun testBuildPWLSelectionResultsEmptyInput() {
        val results = buildPWLContinuousRadiusSelectionResults(
            prototypes = emptyList(),
            pwlContinuousRadiusResults = emptyMap()
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun testBuildPWLSelectionResultsEnvelopeViolation() {
        val prototypes = listOf(pwlPrototype("pwl1"))
        val pwlResults = mapOf(
            "pwl1" to mapOf(
                "solverRadius" to infraScalar(3.0),
                "solverRadiusSquared" to infraScalar(9.5),
                "actualRadiusSquared" to infraScalar(9.0),
                "pwlAbsoluteError" to infraScalar(0.5),
                "pwlRelativeError" to infraScalar(0.5 / 9.0),
                "isWithinEnvelope" to InfraNumber(0.0),
                "maxPWLRelativeError" to infraScalar(0.05),
                "numSegments" to InfraNumber(4.0)
            )
        )
        val results = buildPWLContinuousRadiusSelectionResults(prototypes, pwlResults)
        assertEquals(1, results.size)
        assertNotNull(results[0].pwlMetadata)
        assertTrue(!results[0].pwlMetadata!!.isWithinEnvelope,
            "isWithinEnvelope should be false when solver value is 0.0")
    }

    // ===== 3. PWL 选择结果构建测试（typed PWLExtractedRadius）/ PWL selection result building tests (typed PWLExtractedRadius) =====

    @Test
    fun testBuildPWLSelectionResultsFromExtractedRadius() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(5.0)
        val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val envelope = ConservativeRadiusEnvelope(
            rMin = rMin,
            rMax = rMax
        )
        val extracted = PWLExtractedRadius(
            variableName = "pwl1",
            solverRadius = infraScalar(3.0),
            solverRadiusSquared = infraScalar(9.5),
            actualRadiusSquared = infraScalar(9.0),
            pwlAbsoluteError = infraScalar(0.5),
            pwlRelativeError = infraScalar(0.5 / 9.0),
            isWithinEnvelope = true,
            envelope = envelope,
            pwlApproximation = pwlApproximation
        )
        val prototypes = listOf(pwlPrototype("pwl1"))
        val results = buildPWLSelectionResultsFromExtracted(prototypes, listOf(extracted))
        assertEquals(1, results.size)
        val result = results[0]
        assertEquals("key_pwl1", result.key)
        assertNotNull(result.pwlMetadata)
        assertEquals(9.0, result.pwlMetadata!!.actualRadiusSquared.toDouble(), 1e-10)
        assertEquals(9.5, result.pwlMetadata!!.solverRadiusSquared.toDouble(), 1e-10)
        assertEquals(4, result.pwlMetadata!!.numSegments)
        assertTrue(result.pwlMetadata!!.isWithinEnvelope)
    }

    @Test
    fun testBuildPWLSelectionResultsFromExtractedUsesPWLApproximationNumSegments() {
        val rMin = infraScalar(1.0)
        val rMax = infraScalar(5.0)
        val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 8)
        )
        val envelope = ConservativeRadiusEnvelope(
            rMin = rMin,
            rMax = rMax
        )
        val extracted = PWLExtractedRadius(
            variableName = "pwl1",
            solverRadius = infraScalar(3.0),
            solverRadiusSquared = infraScalar(9.5),
            actualRadiusSquared = infraScalar(9.0),
            pwlAbsoluteError = infraScalar(0.5),
            pwlRelativeError = infraScalar(0.5 / 9.0),
            isWithinEnvelope = true,
            envelope = envelope,
            pwlApproximation = pwlApproximation
        )
        val prototypes = listOf(pwlPrototype("pwl1", rMin = 1.0, rMax = 5.0))
        val results = buildPWLSelectionResultsFromExtracted(prototypes, listOf(extracted))
        assertEquals(1, results.size)
        assertNotNull(results[0].pwlMetadata)
        assertEquals(
            pwlApproximation.numSegments,
            results[0].pwlMetadata!!.numSegments,
            "numSegments should come from PWLExtractedRadius.pwlApproximation"
        )
    }

    @Test
    fun testBuildPWLSelectionResultsFromExtractedEmptyInput() {
        val results = buildPWLSelectionResultsFromExtracted(
            prototypes = emptyList(),
            extractedResults = emptyList()
        )
        assertTrue(results.isEmpty())
    }

    // ===== 4. actualVolume 一致性测试 / actualVolume consistency tests =====

    @Test
    fun testPWLSelectionResultActualVolumeUsesRealRadius() {
        val prototypes = listOf(pwlPrototype("pwl1"))
        val pwlResults = mapOf(
            "pwl1" to mapOf(
                "solverRadius" to infraScalar(3.0),
                "solverRadiusSquared" to infraScalar(9.5),
                "actualRadiusSquared" to infraScalar(9.0),
                "pwlAbsoluteError" to infraScalar(0.5),
                "pwlRelativeError" to infraScalar(0.5 / 9.0),
                "isWithinEnvelope" to InfraNumber(1.0),
                "maxPWLRelativeError" to infraScalar(0.05),
                "numSegments" to InfraNumber(4.0)
            )
        )
        val results = buildPWLContinuousRadiusSelectionResults(prototypes, pwlResults)
        assertEquals(1, results.size)
        val metadata = results[0].pwlMetadata!!
        val height = infraScalar(10.0)
        val pi = infraScalar(PI)
        // actualVolume = π * r² * h = π * 9 * 10 = 90π
        assertEquals(PI * 9.0 * 10.0, metadata.actualVolume(height, pi).toDouble(), 1e-6,
            "actualVolume should use actualRadiusSquared (r²), not solverRadiusSquared (q)")
        // pwlVolume = π * q * h = π * 9.5 * 10 = 95π
        assertEquals(PI * 9.5 * 10.0, metadata.pwlVolume(height, pi).toDouble(), 1e-6,
            "pwlVolume should use solverRadiusSquared (q)")
    }

    @Test
    fun testTypedPWLSelectionResultMatchesOpaqueMapResult() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(5.0)
        val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4)
        )
        val envelope = ConservativeRadiusEnvelope(
            rMin = rMin,
            rMax = rMax
        )
        val extracted = PWLExtractedRadius(
            variableName = "pwl1",
            solverRadius = infraScalar(3.0),
            solverRadiusSquared = infraScalar(9.5),
            actualRadiusSquared = infraScalar(9.0),
            pwlAbsoluteError = infraScalar(0.5),
            pwlRelativeError = infraScalar(0.5 / 9.0),
            isWithinEnvelope = true,
            envelope = envelope,
            pwlApproximation = pwlApproximation
        )
        val prototypes = listOf(pwlPrototype("pwl1"))

        // Build via opaque map
        val opaqueResults = buildPWLContinuousRadiusSelectionResults(
            prototypes,
            mapOf(
                "pwl1" to mapOf(
                    "solverRadius" to infraScalar(3.0),
                    "solverRadiusSquared" to infraScalar(9.5),
                    "actualRadiusSquared" to infraScalar(9.0),
                    "pwlAbsoluteError" to infraScalar(0.5),
                    "pwlRelativeError" to infraScalar(0.5 / 9.0),
                    "isWithinEnvelope" to InfraNumber(1.0),
                    "maxPWLRelativeError" to pwlApproximation.maxRelativeError,
                    "numSegments" to InfraNumber(pwlApproximation.numSegments.toDouble())
                )
            )
        )

        // Build via typed path
        val typedResults = buildPWLSelectionResultsFromExtracted(prototypes, listOf(extracted))

        assertEquals(1, opaqueResults.size)
        assertEquals(1, typedResults.size)
        // Both should produce the same key, selectedRadius and axis
        assertEquals(opaqueResults[0].key, typedResults[0].key)
        assertEquals(
            opaqueResults[0].selectedRadius.value.toDouble(),
            typedResults[0].selectedRadius.value.toDouble(),
            1e-10
        )
        // Both should have PWL metadata
        assertNotNull(opaqueResults[0].pwlMetadata)
        assertNotNull(typedResults[0].pwlMetadata)
        // actualRadiusSquared should be the same
        assertEquals(
            opaqueResults[0].pwlMetadata!!.actualRadiusSquared.toDouble(),
            typedResults[0].pwlMetadata!!.actualRadiusSquared.toDouble(),
            1e-10
        )
    }
}
