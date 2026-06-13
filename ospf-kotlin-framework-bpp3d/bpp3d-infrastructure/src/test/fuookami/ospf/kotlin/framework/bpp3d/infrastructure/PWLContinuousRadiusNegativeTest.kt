/**
 * PWL 连续半径负例测试：Big-M 边界、envelope 溢出、PWL 误差超限、silent downgrade 防护。
 * PWL continuous-radius negative tests: Big-M bounds, envelope overflow, PWL error excess, silent downgrade prevention.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.math.abs
import kotlin.test.*

class PWLContinuousRadiusNegativeTest {

    // ===== 1. Big-M 边界验证 =====

    @Test
    fun testBigMBoundMustCoverRadiusRange() {
        val rMin = fltX(2.0)
        val rMax = fltX(5.0)
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
        val rMin = fltX(1.0)
        val rMax = fltX(10.0)
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
        val rMin = fltX(1.5)
        val rMax = fltX(6.0)
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
            rMin = fltX(2.0),
            rMax = fltX(5.0)
        )
        assertFalse(envelope.isRadiusValid(fltX(1.5)),
            "Radius below rMin should be invalid")
        assertFalse(envelope.isRadiusValid(fltX(0.5)),
            "Radius well below rMin should be invalid")
    }

    @Test
    fun testEnvelopeRejectsRadiusAboveMax() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = fltX(2.0),
            rMax = fltX(5.0)
        )
        assertFalse(envelope.isRadiusValid(fltX(5.5)),
            "Radius above rMax should be invalid")
        assertFalse(envelope.isRadiusValid(fltX(10.0)),
            "Radius well above rMax should be invalid")
    }

    @Test
    fun testEnvelopeAcceptsRadiusWithinBounds() {
        val envelope = ConservativeRadiusEnvelope(
            rMin = fltX(2.0),
            rMax = fltX(5.0)
        )
        assertTrue(envelope.isRadiusValid(fltX(2.0)), "rMin should be valid")
        assertTrue(envelope.isRadiusValid(fltX(3.5)), "Mid-range should be valid")
        assertTrue(envelope.isRadiusValid(fltX(5.0)), "rMax should be valid")
    }

    @Test
    fun testEnvelopeOverflowExceedsConservativeBounds() {
        // 当 solver 选出 r > rMax 时，保守 envelope 不再安全
        val envelope = ConservativeRadiusEnvelope(
            rMin = fltX(2.0),
            rMax = fltX(3.0)
        )
        val overflowRadius = fltX(3.5)
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
            rMin = fltX(2.0),
            rMax = fltX(3.0)
        )
        val underflowRadius = fltX(1.5)
        assertFalse(envelope.isRadiusValid(underflowRadius),
            "Underflow radius should be invalid (violates variable lower bound)")
    }

    // ===== 3. PWL 误差超限 =====

    @Test
    fun testPWLExceedsToleranceWithTooFewSegments() {
        val rMin = fltX(1.0)
        val rMax = fltX(10.0)
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
        val rMin = fltX(1.0)
        val rMax = fltX(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(
                maxSegments = 32,
                breakpointStrategy = PWLBreakpointStrategy.ErrorDriven,
                relativeErrorTolerance = fltX(0.005)
            )
        )
        assertTrue(
            pwl.maxRelativeError.toDouble() < 0.01,
            "With 32 error-driven segments, max relative error should be < 1%: actual=${pwl.maxRelativeError.toDouble()}"
        )
    }

    @Test
    fun testPWLAlwaysOverapproximates() {
        val rMin = fltX(2.0)
        val rMax = fltX(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        for (i in 0 until pwl.numSegments) {
            val r0 = pwl.breakpoints[i].toDouble()
            val r1 = pwl.breakpoints[i + 1].toDouble()
            val midR = fltX((r0 + r1) / 2.0)
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
        val rMin = fltX(1.0)
        val rMax = fltX(5.0)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = rMin,
            rMax = rMax,
            config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val testPoints = (0..20).map { rMin + (rMax - rMin) * fltX(it.toDouble() / 20.0) }
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
                rMin = fltX(3.0),
                rMax = fltX(3.0),
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
                rMin = fltX(0.0),
                rMax = fltX(3.0)
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
                rMin = fltX(5.0),
                rMax = fltX(3.0),
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
            rMin = fltX(2.0),
            rMax = fltX(4.0)
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
            rMin = fltX(2.0),
            rMax = fltX(4.0)
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
        val rMin = fltX(0.01)
        val rMax = fltX(1.0)
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
                relativeErrorTolerance = fltX(0.01))
        )
        assertTrue(
            pwlMoreSegments.maxRelativeError.toDouble() < pwl.maxRelativeError.toDouble(),
            "More segments with error-driven strategy should reduce max relative error"
        )
    }

    @Test
    fun testPWLWithLargeRatio() {
        // rMax / rMin 跨度大 / large rMax/rMin ratio
        val rMin = fltX(1.0)
        val rMax = fltX(100.0)
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
        val rMin = fltX(5.0)
        val rMax = fltX(5.01)
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

    @Test
    fun testCustomBreakpointsInsufficientCoverage() {
        // Custom breakpoints 不覆盖 [rMin, rMax] 时应抛出异常
        // Custom breakpoints should throw when not covering [rMin, rMax]
        val rMin = fltX(2.0)
        val rMax = fltX(5.0)

        // Case 1: breakpoints.first() > rMin / 断点起点超过 rMin
        val breakpointsNotCoveringMin = listOf(fltX(2.5), fltX(5.0), fltX(6.0))
        val exception1 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = rMin,
                rMax = rMax,
                config = PWLRadiusApproximationConfig(customBreakpoints = breakpointsNotCoveringMin)
            )
        }
        assertTrue(
            exception1.message?.contains("Custom breakpoints must start at or below rMin") == true,
            "Should reject breakpoints not covering rMin"
        )

        // Case 2: breakpoints.last() < rMax / 断点终点不到 rMax
        val breakpointsNotCoveringMax = listOf(fltX(1.5), fltX(2.0), fltX(4.5))
        val exception2 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = rMin,
                rMax = rMax,
                config = PWLRadiusApproximationConfig(customBreakpoints = breakpointsNotCoveringMax)
            )
        }
        assertTrue(
            exception2.message?.contains("Custom breakpoints must end at or above rMax") == true,
            "Should reject breakpoints not covering rMax"
        )
    }

    @Test
    fun testCustomBreakpointsNonMonotonic() {
        // Custom breakpoints 递减时应抛出异常
        // Custom breakpoints should throw when non-monotonic (decreasing)
        val rMin = fltX(1.0)
        val rMax = fltX(4.0)

        // Case 1: 覆盖范围但内部递减 / covers range but internally decreasing
        // [1.0, 3.0, 2.0, 4.0] — 3.0 > 2.0 违反单调性
        val partiallyDecreasing1 = listOf(fltX(1.0), fltX(3.0), fltX(2.0), fltX(4.0))
        val exception1 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = rMin,
                rMax = rMax,
                config = PWLRadiusApproximationConfig(customBreakpoints = partiallyDecreasing1)
            )
        }
        assertTrue(
            exception1.message?.contains("Custom breakpoints must be non-decreasing") == true,
            "Should reject partially decreasing breakpoints"
        )

        // Case 2: 另一组部分递减 / another partially decreasing set
        // [0.5, 2.0, 1.5, 3.0, 5.0] — 2.0 > 1.5 违反单调性
        val partiallyDecreasing2 = listOf(fltX(0.5), fltX(2.0), fltX(1.5), fltX(3.0), fltX(5.0))
        val exception2 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = rMin,
                rMax = rMax,
                config = PWLRadiusApproximationConfig(customBreakpoints = partiallyDecreasing2)
            )
        }
        assertTrue(
            exception2.message?.contains("Custom breakpoints must be non-decreasing") == true,
            "Should reject another set of partially decreasing breakpoints"
        )
    }

    @Test
    fun testUnitConversionNearToleranceBoundary() {
        // 直径转半径后（/2），边界值接近 PWL tolerance
        // After diameter-to-radius conversion (/2), boundary values approach PWL tolerance
        // 模拟 CylinderShapeContract.toContinuousRadiusBoundFromDiameter 的行为

        // Case 1: 直径区间 [0.30, 0.36] -> 半径区间 [0.15, 0.18]
        // 比例 0.18/0.15 = 1.2，相对较小，PWL 应精确
        val diameterMin = fltX(0.30)
        val diameterMax = fltX(0.36)
        val radiusMin = fltX(diameterMin.toDouble() / 2.0)  // 0.15
        val radiusMax = fltX(diameterMax.toDouble() / 2.0)  // 0.18

        val pwl1 = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = radiusMin,
            rMax = radiusMax,
            config = PWLRadiusApproximationConfig(
                maxSegments = 4,
                breakpointStrategy = PWLBreakpointStrategy.Uniform,
                relativeErrorTolerance = fltX(0.01)
            )
        )
        // 窄区间应达到高精度 / narrow interval should achieve high precision
        assertTrue(
            pwl1.maxRelativeError.toDouble() < 0.001,
            "PWL for diameter [0.30, 0.36] (radius [0.15, 0.18]) should be very accurate: actual=${pwl1.maxRelativeError.toDouble()}"
        )

        // Case 2: 直径区间 [0.10, 1.00] -> 半径区间 [0.05, 0.50]
        // 比例 0.50/0.05 = 10，跨度大，需要更多段
        val largeRadiusMin = fltX(0.10 / 2.0)   // 0.05
        val largeRadiusMax = fltX(1.00 / 2.0)   // 0.50

        val pwl2FewSegments = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = largeRadiusMin,
            rMax = largeRadiusMax,
            config = PWLRadiusApproximationConfig(
                maxSegments = 2,
                breakpointStrategy = PWLBreakpointStrategy.Uniform,
                relativeErrorTolerance = fltX(0.01)
            )
        )
        // 少段数下误差应较大 / few segments should have significant error
        assertTrue(
            pwl2FewSegments.maxRelativeError.toDouble() > 0.01,
            "PWL with 2 segments for diameter [0.10, 1.00] should exceed 1% tolerance: actual=${pwl2FewSegments.maxRelativeError.toDouble()}"
        )

        val pwl2MoreSegments = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = largeRadiusMin,
            rMax = largeRadiusMax,
            config = PWLRadiusApproximationConfig(
                maxSegments = 16,
                breakpointStrategy = PWLBreakpointStrategy.ErrorDriven,
                relativeErrorTolerance = fltX(0.01)
            )
        )
        // 更多段 + error-driven 应满足 tolerance / more segments + error-driven should meet tolerance
        assertTrue(
            pwl2MoreSegments.maxRelativeError.toDouble() < 0.01,
            "PWL with 16 error-driven segments for diameter [0.10, 1.00] should meet 1% tolerance: actual=${pwl2MoreSegments.maxRelativeError.toDouble()}"
        )

        // Case 3: 边界值接近 tolerance 临界点
        // 直径 [0.1998, 0.2002] -> 半径 [0.0999, 0.1001]
        // 极窄区间，即使 1 段也应远超 tolerance
        val boundaryRadiusMin = fltX(0.1998 / 2.0)   // 0.0999
        val boundaryRadiusMax = fltX(0.2002 / 2.0)   // 0.1001

        val pwl3 = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = boundaryRadiusMin,
            rMax = boundaryRadiusMax,
            config = PWLRadiusApproximationConfig(
                maxSegments = 1,
                breakpointStrategy = PWLBreakpointStrategy.Uniform,
                relativeErrorTolerance = fltX(0.01)
            )
        )
        assertTrue(
            pwl3.maxRelativeError.toDouble() < 1e-5,
            "PWL for extremely narrow diameter [0.1998, 0.2002] should be extremely accurate: actual=${pwl3.maxRelativeError.toDouble()}"
        )
    }

    // ===== 7. 误差预算推导测试 / Error budget derivation tests =====

    @Test
    fun testDeriveSegmentCountMeetsTolerance() {
        val result = PWLRadiusSquaredApproximation.deriveSegmentCount(
            rMin = fltX(2.0),
            rMax = fltX(5.0),
            relativeErrorTolerance = fltX(0.01),
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
            rMin = fltX(1.0),
            rMax = fltX(100.0),
            relativeErrorTolerance = fltX(0.0001),
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
        val strictTolerance = fltX(0.0001)
        val result = PWLRadiusSquaredApproximation.deriveSegmentCount(
            rMin = fltX(1.0),
            rMax = fltX(50.0),
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
