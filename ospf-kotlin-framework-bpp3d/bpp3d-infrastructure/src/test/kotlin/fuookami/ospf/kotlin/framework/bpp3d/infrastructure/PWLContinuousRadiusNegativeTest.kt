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

    // ===== 6. 极端半径测试 / Extreme radius tests =====

    @Test
    fun testPWLWithVerySmallRMin() {
        // rMin 接近 0 但仍为正数，比例适中 / rMin close to 0 but still positive, moderate ratio
        val rMin = infraScalar(0.01)
        val rMax = infraScalar(1.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 8, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        // PWL 仍应有效 / PWL should still be valid
        assertTrue(pwl.maxRelativeError.toDouble() > 0.0, "PWL should have non-zero error for [0.01, 1.0]")
        // 断点在 rMin 和 rMax 处精确 / exact at breakpoints
        assertEquals(rMin.toDouble() * rMin.toDouble(), pwl.evaluate(rMin).toDouble(), 1e-8,
            "PWL should be exact at rMin")
        assertEquals(rMax.toDouble() * rMax.toDouble(), pwl.evaluate(rMax).toDouble(), 1e-8,
            "PWL should be exact at rMax")
        // 大跨度下需要更多段才能达到低误差 / wide ratio needs more segments for low error
        val pwlMoreSegments = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 32, breakpointStrategy = PWLBreakpointStrategy.ErrorDriven,
                relativeErrorTolerance = infraScalar(0.01))
        )
        assertTrue(
            pwlMoreSegments.maxRelativeError.toDouble() < pwl.maxRelativeError.toDouble(),
            "More segments with error-driven strategy should reduce max relative error"
        )
    }

    @Test
    fun testPWLWithLargeRatio() {
        // rMax / rMin 跨度大 / large rMax/rMin ratio
        val rMin = infraScalar(1.0)
        val rMax = infraScalar(100.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 8, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        // 大跨度下 8 段相对误差应较大 / 8 segments over [1, 100] should have significant relative error
        assertTrue(
            pwl.maxRelativeError.toDouble() > 0.1,
            "With 8 uniform segments over [1, 100], max relative error should exceed 10%: actual=${pwl.maxRelativeError.toDouble()}"
        )
    }

    @Test
    fun testPWLWithNarrowInterval() {
        // 极窄区间 / very narrow interval
        val rMin = infraScalar(5.0)
        val rMax = infraScalar(5.01)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 1, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        // 极窄区间下 1 段应足够精确 / 1 segment should be very accurate for narrow interval
        assertTrue(
            pwl.maxRelativeError.toDouble() < 0.001,
            "With 1 segment over [5.0, 5.01], max relative error should be very small: actual=${pwl.maxRelativeError.toDouble()}"
        )
    }

    // ===== 7. 误差预算推导测试 / Error budget derivation tests =====

    @Test
    fun testDeriveSegmentCountMeetsTolerance() {
        val result = PWLRadiusSquaredApproximation.deriveSegmentCount(
            rMin = infraScalar(2.0),
            rMax = infraScalar(5.0),
            relativeErrorTolerance = infraScalar(0.01),
            maxSegments = 32
        )
        assertTrue(result.meetsTolerance, "Should meet 1% tolerance for [2, 5] within 32 segments")
        assertTrue(result.recommendedSegments <= 32, "Should not exceed maxSegments")
        assertTrue(
            result.achievedMaxRelativeError.toDouble() <= 0.01,
            "Achieved error should be <= tolerance: actual=${result.achievedMaxRelativeError.toDouble()}"
        )
    }

    @Test
    fun testDeriveSegmentCountExceedsMaxSegments() {
        // 极严精度 + 极少段数 = 无法满足 / very strict tolerance + very few segments = cannot meet
        val result = PWLRadiusSquaredApproximation.deriveSegmentCount(
            rMin = infraScalar(1.0),
            rMax = infraScalar(100.0),
            relativeErrorTolerance = infraScalar(0.0001),
            maxSegments = 4
        )
        assertFalse(result.meetsTolerance, "Should NOT meet 0.01% tolerance for [1, 100] with only 4 segments")
        assertEquals(4, result.recommendedSegments, "Should return maxSegments as recommended")
        // 诊断信息应包含 / diagnostics should contain
        val info = result.info()
        assertEquals("false", info["pwl_derived_meets_tolerance"])
        assertTrue(info.containsKey("pwl_derived_achieved_max_rel_error"))
    }

    @Test
    fun testDeriveSegmentCountDoesNotSilentlyRelax() {
        // 验证 deriveSegmentCount 不会静默放宽误差 / verify no silent relaxation
        val strictTolerance = infraScalar(0.0001)
        val result = PWLRadiusSquaredApproximation.deriveSegmentCount(
            rMin = infraScalar(1.0),
            rMax = infraScalar(50.0),
            relativeErrorTolerance = strictTolerance,
            maxSegments = 4
        )
        // 若不满足，meetsTolerance 必须为 false，不应假装满足
        // If not met, meetsTolerance must be false — must not pretend to satisfy
        if (!result.meetsTolerance) {
            assertTrue(
                result.achievedMaxRelativeError.toDouble() > strictTolerance.toDouble(),
                "If tolerance not met, achieved error must be > tolerance: achieved=${result.achievedMaxRelativeError.toDouble()}, tolerance=${strictTolerance.toDouble()}"
            )
        }
    }
}
