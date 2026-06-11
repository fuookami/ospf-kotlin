/**
 * PWL 连续半径负例测试：Big-M 边界、envelope 溢出、PWL 误差超限、silent downgrade 防护。
 * PWL continuous-radius negative tests: Big-M bounds, envelope overflow, PWL error excess, silent downgrade prevention.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PWLContinuousRadiusNegativeTest {

    // ===== 1. Big-M 边界验证 =====

    @Test
    fun testBigMBoundMustCoverRadiusRange() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val maxQ = rMax * rMax
        for (bp in pwl.breakpoints) {
            val q = pwl.evaluate(bp).toDouble()
            assertTrue(
                q <= maxQ.toDouble() + 1e-8,
                "PWL q at breakpoint r=${bp.toDouble()} should not exceed rMax²=${maxQ.toDouble()}: actual q=$q"
            )
        }
    }

    @Test
    fun testBigMBoundAtMaximumRadius() {
        val rMin = infraScalar(1.0)
        val rMax = infraScalar(10.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 8, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val qAtMax = pwl.evaluate(rMax).toDouble()
        val actualAtMax = rMax.toDouble() * rMax.toDouble()
        assertEquals(actualAtMax, qAtMax, 1e-8,
            "PWL must be exact at rMax (it is a breakpoint)")
    }

    @Test
    fun testBigMBoundAtMinimumRadius() {
        val rMin = infraScalar(1.5)
        val rMax = infraScalar(6.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val qAtMin = pwl.evaluate(rMin).toDouble()
        val actualAtMin = rMin.toDouble() * rMin.toDouble()
        assertEquals(actualAtMin, qAtMin, 1e-8,
            "PWL must be exact at rMin (it is a breakpoint)")
    }

    // ===== 2. Envelope 溢出（r 超出 [rMin, rMax]）=====

    @Test
    fun testEnvelopeRejectsRadiusBelowMin() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(5.0)
        )
        assertFalse(envelope.isRadiusValid(infraScalar(1.5)),
            "Radius below rMin should be invalid")
        assertFalse(envelope.isRadiusValid(infraScalar(0.5)),
            "Radius well below rMin should be invalid")
    }

    @Test
    fun testEnvelopeRejectsRadiusAboveMax() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(5.0)
        )
        assertFalse(envelope.isRadiusValid(infraScalar(5.5)),
            "Radius above rMax should be invalid")
        assertFalse(envelope.isRadiusValid(infraScalar(10.0)),
            "Radius well above rMax should be invalid")
    }

    @Test
    fun testEnvelopeAcceptsRadiusWithinBounds() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(5.0)
        )
        assertTrue(envelope.isRadiusValid(infraScalar(2.0)), "rMin should be valid")
        assertTrue(envelope.isRadiusValid(infraScalar(3.5)), "Mid-range should be valid")
        assertTrue(envelope.isRadiusValid(infraScalar(5.0)), "rMax should be valid")
    }

    @Test
    fun testEnvelopeOverflowExceedsConservativeBounds() {
        // 当 solver 选出 r > rMax 时，保守 envelope 不再安全
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val overflowRadius = infraScalar(3.5)
        // rMax-based envelope diameter = 6.0
        assertEquals(6.0, envelope.envelopeDiameter.toDouble(), 1e-10)
        // 真实 diameter with overflow = 7.0 > envelope 6.0
        assertTrue(
            overflowRadius.toDouble() * 2.0 > envelope.envelopeDiameter.toDouble(),
            "Overflow radius diameter should exceed conservative envelope"
        )
    }

    @Test
    fun testEnvelopeUnderflowViolatesVariableConstraint() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(3.0)
        )
        val underflowRadius = infraScalar(1.5)
        assertFalse(envelope.isRadiusValid(underflowRadius),
            "Underflow radius should be invalid (violates variable lower bound)")
    }

    // ===== 3. PWL 误差超限 =====

    @Test
    fun testPWLExceedsToleranceWithTooFewSegments() {
        val rMin = infraScalar(1.0)
        val rMax = infraScalar(10.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 1, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        assertTrue(
            pwl.maxRelativeError.toDouble() > 0.01,
            "With only 1 segment over [1, 10], max relative error should exceed 1%: actual=${pwl.maxRelativeError.toDouble()}"
        )
    }

    @Test
    fun testPWLMeetsToleranceWithEnoughSegments() {
        val rMin = infraScalar(1.0)
        val rMax = infraScalar(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(
                maxSegments = 32,
                breakpointStrategy = PWLBreakpointStrategy.ErrorDriven,
                relativeErrorTolerance = infraScalar(0.005)
            )
        )
        assertTrue(
            pwl.maxRelativeError.toDouble() < 0.01,
            "With 32 error-driven segments, max relative error should be < 1%: actual=${pwl.maxRelativeError.toDouble()}"
        )
    }

    @Test
    fun testPWLAlwaysOverapproximates() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        for (i in 0 until pwl.numSegments) {
            val r0 = pwl.breakpoints[i].toDouble()
            val r1 = pwl.breakpoints[i + 1].toDouble()
            val midR = infraScalar((r0 + r1) / 2.0)
            val q = pwl.evaluate(midR).toDouble()
            val actualRSquared = midR.toDouble() * midR.toDouble()
            assertTrue(
                q >= actualRSquared - 1e-10,
                "PWL should overapproximate r² at midpoint of segment $i: q=$q, r²=$actualRSquared"
            )
        }
    }

    @Test
    fun testPWLDoesNotProduceNegativeVolume() {
        val rMin = infraScalar(1.0)
        val rMax = infraScalar(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val testPoints = (0..20).map { rMin + (rMax - rMin) * infraScalar(it.toDouble() / 20.0) }
        for (r in testPoints) {
            val q = pwl.evaluate(r).toDouble()
            assertTrue(q > 0.0,
                "PWL q should be positive for r=${r.toDouble()}: actual q=$q")
        }
    }

    // ===== 4. Silent downgrade 防护 =====

    @Test
    fun testPWLConfigDoesNotDefaultToDiscrete() {
        val config = PWLRadiusApproximationConfig()
        assertFalse(config.enableDebugInfo, "Debug info should default to false")
        assertEquals(PWLBreakpointStrategy.Uniform, config.breakpointStrategy,
            "Default strategy should be PWL, not discrete")
    }

    @Test
    fun testPWLBreakpointStrategyExcludesDiscrete() {
        val strategies = PWLBreakpointStrategy.entries
        assertTrue(strategies.contains(PWLBreakpointStrategy.Uniform))
        assertTrue(strategies.contains(PWLBreakpointStrategy.Adaptive))
        assertTrue(strategies.contains(PWLBreakpointStrategy.ErrorDriven))
        assertFalse(strategies.any { it.name.contains("Discrete", ignoreCase = true) },
            "PWL breakpoint strategies should not include discrete fallback")
        assertFalse(strategies.any { it.name.contains("Fallback", ignoreCase = true) },
            "PWL breakpoint strategies should not include fallback")
    }

    @Test
    fun testPWLRequiresContinuousRadiusInterval() {
        try {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = infraScalar(3.0),
                rMax = infraScalar(3.0),
                config = PWLRadiusApproximationConfig()
            )
            throw AssertionError("Should have thrown for rMax == rMin (degenerate interval)")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testEnvelopeRequiresNonDegenerateInterval() {
        try {
            ConservativeRadiusEnvelope(
                rMin = infraScalar(0.0),
                rMax = infraScalar(3.0)
            )
            throw AssertionError("Should have thrown for rMin = 0")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testPWLRequiresRMaxGreaterThanRMin() {
        try {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = infraScalar(5.0),
                rMax = infraScalar(3.0),
                config = PWLRadiusApproximationConfig()
            )
            throw AssertionError("Should have thrown for rMax < rMin")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    // ===== 5. 互斥协议验证 =====

    @Test
    fun testEnvelopeConservativeForAllValidRadii() {
        // 保守 envelope 必须对所有有效半径 [rMin, rMax] 都是安全的
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0)
        )
        // 验证 envelope 半径 >= 任何有效半径
        val testRadii = listOf(2.0, 2.5, 3.0, 3.5, 4.0)
        for (r in testRadii) {
            assertTrue(
                envelope.envelopeRadius.toDouble() >= r,
                "Envelope radius should be >= any valid radius r=$r"
            )
            assertTrue(
                envelope.envelopeDiameter.toDouble() >= 2.0 * r,
                "Envelope diameter should be >= any valid diameter 2*r=${2.0 * r}"
            )
        }
    }

    @Test
    fun testEnvelopeSupportCoverageConservativeForAllValidRadii() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0)
        )
        // 支撑覆盖半径使用 rMax，必须 >= 任何有效半径
        val supportRadius = envelope.supportCoverageRadius().toDouble()
        for (r in listOf(2.0, 3.0, 4.0)) {
            assertTrue(
                supportRadius >= r,
                "Support coverage radius should be >= any valid radius r=$r"
            )
        }
    }
}
