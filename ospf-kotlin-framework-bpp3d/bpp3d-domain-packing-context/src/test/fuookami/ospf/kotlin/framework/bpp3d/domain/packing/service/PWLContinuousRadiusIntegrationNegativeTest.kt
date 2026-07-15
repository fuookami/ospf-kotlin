/**
 * PWL 连续半径集成负例测试：PWLRadiusSelectionMetadata、renderer actualVolume 回写和互斥协议。
 * PWL continuous-radius integration negative tests: PWLRadiusSelectionMetadata, renderer actualVolume writeback, and mutual exclusion.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.*
import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PWLRadiusSelectionMetadata

class PWLContinuousRadiusIntegrationNegativeTest {

    // ===== PWLRadiusSelectionMetadata 验证 =====

    @Test
    fun testPWLSelectionMetadataRejectsInvalidSegments() {
        try {
            PWLRadiusSelectionMetadata(
                solverRadiusSquared = FltX(9.0),
                actualRadiusSquared = FltX(9.0),
                pwlAbsoluteError = FltX.zero,
                pwlRelativeError = FltX.zero,
                maxPWLRelativeError = FltX.zero,
                numSegments = 0,
                isWithinEnvelope = true
            )
            throw AssertionError("Should have thrown for numSegments = 0")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testPWLSelectionMetadataRejectsBlankSource() {
        try {
            PWLRadiusSelectionMetadata(
                solverRadiusSquared = FltX(9.0),
                actualRadiusSquared = FltX(9.0),
                pwlAbsoluteError = FltX.zero,
                pwlRelativeError = FltX.zero,
                maxPWLRelativeError = FltX.zero,
                numSegments = 4,
                isWithinEnvelope = true,
                selectionSource = ""
            )
            throw AssertionError("Should have thrown for blank selectionSource")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testPWLSelectionMetadataActualVolumeUsesRealRadius() {
        // actualVolume 必须使用 actualRadiusSquared (r²)，而非 solverRadiusSquared (q)
        val metadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = FltX(10.0), // q ≈ r² (PWL 近似)
            actualRadiusSquared = FltX(9.0),   // r² = 9 (真实)
            pwlAbsoluteError = FltX(1.0),
            pwlRelativeError = FltX(1.0 / 9.0),
            maxPWLRelativeError = FltX(0.1),
            numSegments = 2,
            isWithinEnvelope = true
        )
        val height = FltX(5.0)
        val pi = FltX(PI)

        val actualVol = metadata.actualVolume(height, pi).toDouble()
        val pwlVol = metadata.pwlVolume(height, pi).toDouble()

        // actualVolume = π * actualRadiusSquared * h = π * 9 * 5 = 45π
        assertEquals(PI * 9.0 * 5.0, actualVol, 1e-6,
            "actualVolume should use actualRadiusSquared (r²)")
        // pwlVolume = π * solverRadiusSquared * h = π * 10 * 5 = 50π
        assertEquals(PI * 10.0 * 5.0, pwlVol, 1e-6,
            "pwlVolume should use solverRadiusSquared (q)")
        // 两者必须不同（否则 PWL 没有意义）
        assertTrue(abs(actualVol - pwlVol) > 1.0,
            "actualVolume and pwlVolume should differ when PWL has approximation error")
    }

    @Test
    fun testPWLActualVolumeAlwaysLessThanPwlVolume() {
        // PWL 过近似 r²，所以 actualVolume 应始终 <= pwlVolume
        val metadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = FltX(10.0),
            actualRadiusSquared = FltX(9.0),
            pwlAbsoluteError = FltX(1.0),
            pwlRelativeError = FltX(1.0 / 9.0),
            maxPWLRelativeError = FltX(0.1),
            numSegments = 2,
            isWithinEnvelope = true
        )
        val height = FltX(8.0)
        val pi = FltX(PI)
        assertTrue(
            metadata.actualVolume(height, pi).toDouble() <= metadata.pwlVolume(height, pi).toDouble(),
            "actualVolume should be <= pwlVolume when PWL overapproximates"
        )
    }

    @Test
    fun testPWLMetadataWithEnvelopeViolation() {
        // 当 solver 选出半径超出 envelope 时，isWithinEnvelope 应为 false
        val metadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = FltX(16.0), // r=4 的 q 值
            actualRadiusSquared = FltX(16.0),
            pwlAbsoluteError = FltX.zero,
            pwlRelativeError = FltX.zero,
            maxPWLRelativeError = FltX(0.05),
            numSegments = 4,
            isWithinEnvelope = false // 超出 envelope
        )
        // actualVolume 仍然可以计算（即使超出 envelope）
        val height = FltX(5.0)
        val pi = FltX(PI)
        val actualVol = metadata.actualVolume(height, pi).toDouble()
        assertTrue(actualVol > 0.0, "actualVolume should still be positive even when outside envelope")
        // 但 isWithinEnvelope 标记了违规
        assertTrue(!metadata.isWithinEnvelope, "isWithinEnvelope should be false for overflow")
    }

    @Test
    fun testPWLRendererInfoDiagnostics() {
        // 验证 PWL 诊断信息可用于 renderer 回写
        val metadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = FltX(10.0),
            actualRadiusSquared = FltX(9.0),
            pwlAbsoluteError = FltX(1.0),
            pwlRelativeError = FltX(1.0 / 9.0),
            maxPWLRelativeError = FltX(0.1),
            numSegments = 4,
            isWithinEnvelope = true
        )
        val height = FltX(5.0)
        val pi = FltX(PI)

        // 验证 metadata 可以计算 pwlVolume 和 actualVolume
        val pwlVol = metadata.pwlVolume(height, pi)
        val actualVol = metadata.actualVolume(height, pi)
        assertTrue(pwlVol.toDouble() > 0.0, "pwlVolume should be positive")
        assertTrue(actualVol.toDouble() > 0.0, "actualVolume should be positive")
        // actualVolume 应小于 pwlVolume（因为 PWL 过近似）
        assertTrue(actualVol.toDouble() < pwlVol.toDouble(),
            "actualVolume should be less than pwlVolume when PWL overapproximates")
    }

    @Test
    fun testPWLSelectionSourceDefaultsToPwl() {
        // PWL 选择来源应默认为 "pwl"
        val metadata = PWLRadiusSelectionMetadata(
            solverRadiusSquared = FltX(9.0),
            actualRadiusSquared = FltX(9.0),
            pwlAbsoluteError = FltX.zero,
            pwlRelativeError = FltX.zero,
            maxPWLRelativeError = FltX.zero,
            numSegments = 1,
            isWithinEnvelope = true
        )
        assertEquals("pwl", metadata.selectionSource,
            "PWL selection source should default to 'pwl'")
    }
}
