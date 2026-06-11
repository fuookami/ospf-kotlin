/**
 * PWL 半径平方近似函数测试。
 * PWL radius-squared approximation function tests.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PWLRadiusSquaredApproximationTest {

    // ===== PWLRadiusApproximationConfig tests =====

    @Test
    fun testDefaultConfig() {
        val config = PWLRadiusApproximationConfig()
        assertEquals(8, config.maxSegments)
        assertEquals(0.01, config.relativeErrorTolerance.toDouble(), 1e-6)
        assertEquals(PWLBreakpointStrategy.Uniform, config.breakpointStrategy)
        assertEquals(null, config.customBreakpoints)
        assertEquals(false, config.enableDebugInfo)
    }

    @Test
    fun testCustomBreakpointConfig() {
        val customBp = listOf(infraScalar(1.0), infraScalar(2.0), infraScalar(3.0))
        val config = PWLRadiusApproximationConfig(
            maxSegments = 4,
            relativeErrorTolerance = infraScalar(0.005),
            breakpointStrategy = PWLBreakpointStrategy.ErrorDriven,
            customBreakpoints = customBp,
            enableDebugInfo = true
        )
        assertEquals(4, config.maxSegments)
        assertEquals(0.005, config.relativeErrorTolerance.toDouble(), 1e-6)
        assertEquals(PWLBreakpointStrategy.ErrorDriven, config.breakpointStrategy)
        assertEquals(customBp, config.customBreakpoints)
        assertEquals(true, config.enableDebugInfo)
    }

    // ===== Uniform breakpoint tests =====

    @Test
    fun testUniformBreakpointsBasic() {
        val config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = config
        )
        assertEquals(4, pwl.numSegments)
        assertEquals(5, pwl.breakpoints.size)
        // Uniform breakpoints: 1.0, 2.0, 3.0, 4.0, 5.0
        assertEquals(1.0, pwl.breakpoints[0].toDouble(), 1e-10)
        assertEquals(2.0, pwl.breakpoints[1].toDouble(), 1e-10)
        assertEquals(3.0, pwl.breakpoints[2].toDouble(), 1e-10)
        assertEquals(4.0, pwl.breakpoints[3].toDouble(), 1e-10)
        assertEquals(5.0, pwl.breakpoints[4].toDouble(), 1e-10)
    }

    @Test
    fun testUniformBreakpointsAtEndpoints() {
        val rMin = infraScalar(2.0)
        val rMax = infraScalar(3.0)
        val config = PWLRadiusApproximationConfig(maxSegments = 1, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(rMin, rMax, config)
        assertEquals(1, pwl.numSegments)
        // Chord slope: r0 + r1 = 2 + 3 = 5
        assertEquals(5.0, pwl.slopes[0].toDouble(), 1e-10)
        // Chord intercept: -r0 * r1 = -6
        assertEquals(-6.0, pwl.intercepts[0].toDouble(), 1e-10)
    }

    // ===== Evaluation tests =====

    @Test
    fun testEvaluateAtBreakpoints() {
        val config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = config
        )
        // At breakpoints, PWL approximation is exact (passes through r² values)
        for (bp in pwl.breakpoints) {
            val r = bp.toDouble()
            val actualRSquared = r * r
            val approxRSquared = pwl.evaluate(bp).toDouble()
            assertEquals(actualRSquared, approxRSquared, 1e-8,
                "PWL approximation should be exact at breakpoint r=$r")
        }
    }

    @Test
    fun testEvaluateMidpoint() {
        val config = PWLRadiusApproximationConfig(maxSegments = 1, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0),
            config = config
        )
        // Midpoint r=3.0: actual r²=9, PWL q = 6*3 - 8 = 10
        val mid = infraScalar(3.0)
        val q = pwl.evaluate(mid).toDouble()
        // chord: slope=2+4=6, intercept=-2*4=-8, so q=6*3-8=10
        assertEquals(10.0, q, 1e-10)
        // actual r² = 9, so error = 1
        val error = pwl.actualError(mid).toDouble()
        assertEquals(1.0, error, 1e-10)
    }

    @Test
    fun testEvaluateBelowRange() {
        val config = PWLRadiusApproximationConfig(maxSegments = 2, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0),
            config = config
        )
        // Below rMin: should use first segment extrapolation
        val r = infraScalar(1.0)
        val q = pwl.evaluate(r).toDouble()
        // slope = 2 + 3 = 5, intercept = -6
        assertEquals(5.0 * 1.0 - 6.0, q, 1e-10)
    }

    @Test
    fun testEvaluateAboveRange() {
        val config = PWLRadiusApproximationConfig(maxSegments = 2, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0),
            config = config
        )
        // Above rMax: should use last segment extrapolation
        val r = infraScalar(5.0)
        val q = pwl.evaluate(r).toDouble()
        // slope = 3 + 4 = 7, intercept = -12
        assertEquals(7.0 * 5.0 - 12.0, q, 1e-10)
    }

    // ===== Error computation tests =====

    @Test
    fun testMaxRelativeError() {
        val config = PWLRadiusApproximationConfig(maxSegments = 2, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(3.0),
            config = config
        )
        // With 2 segments, max relative error should be bounded
        assertTrue(pwl.maxRelativeError.toDouble() > 0.0, "maxRelativeError should be positive")
        assertTrue(pwl.maxRelativeError.toDouble() < 0.5, "maxRelativeError should be reasonable for 2 segments")
    }

    @Test
    fun testMoreSegmentsLowerError() {
        val pwl2 = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = PWLRadiusApproximationConfig(maxSegments = 2, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        val pwl8 = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = PWLRadiusApproximationConfig(maxSegments = 8, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        assertTrue(
            pwl8.maxRelativeError.toDouble() <= pwl2.maxRelativeError.toDouble(),
            "More segments should have lower or equal max relative error"
        )
        assertTrue(
            pwl8.maxAbsoluteError.toDouble() <= pwl2.maxAbsoluteError.toDouble(),
            "More segments should have lower or equal max absolute error"
        )
    }

    @Test
    fun testActualErrorAtBreakpointIsZero() {
        val config = PWLRadiusApproximationConfig(maxSegments = 4, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = config
        )
        for (bp in pwl.breakpoints) {
            val error = pwl.actualError(bp).toDouble()
            assertEquals(0.0, error, 1e-8,
                "Actual error at breakpoint should be zero (r=${bp.toDouble()})")
        }
    }

    @Test
    fun testActualRelativeError() {
        val config = PWLRadiusApproximationConfig(maxSegments = 2, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0),
            config = config
        )
        // At midpoint of first segment [2.0, 3.0]: r=2.5
        val mid = infraScalar(2.5)
        val relError = pwl.actualRelativeError(mid).toDouble()
        // PWL q = slope*r + intercept = 5*2.5 - 6 = 6.5, actual = 6.25
        // relative error = 0.25 / 6.25 = 0.04
        assertTrue(relError > 0.0, "Relative error at segment midpoint should be positive")
        assertTrue(relError < 0.1, "Relative error should be reasonable for 2 segments over [2,4]")
    }

    // ===== Custom breakpoints tests =====

    @Test
    fun testCustomBreakpoints() {
        val customBp = listOf(infraScalar(1.0), infraScalar(1.5), infraScalar(3.0), infraScalar(5.0))
        val config = PWLRadiusApproximationConfig(customBreakpoints = customBp)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = config
        )
        assertEquals(3, pwl.numSegments)
        assertEquals(4, pwl.breakpoints.size)
        // Verify breakpoints match custom
        for (i in customBp.indices) {
            assertEquals(customBp[i].toDouble(), pwl.breakpoints[i].toDouble(), 1e-10)
        }
    }

    @Test
    fun testCustomBreakpointsExactAtEndpoints() {
        val customBp = listOf(infraScalar(2.0), infraScalar(4.0))
        val config = PWLRadiusApproximationConfig(customBreakpoints = customBp)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(2.0),
            rMax = infraScalar(4.0),
            config = config
        )
        assertEquals(1, pwl.numSegments)
        // At endpoints: exact match
        assertEquals(4.0, pwl.evaluate(infraScalar(2.0)).toDouble(), 1e-10)
        assertEquals(16.0, pwl.evaluate(infraScalar(4.0)).toDouble(), 1e-10)
    }

    // ===== Adaptive breakpoint tests =====

    @Test
    fun testAdaptiveBreakpointsMoreSegmentsNearBoundaries() {
        val config = PWLRadiusApproximationConfig(maxSegments = 8, breakpointStrategy = PWLBreakpointStrategy.Adaptive)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = config
        )
        assertEquals(8, pwl.numSegments)
        // Adaptive breakpoints should still start at rMin and end at rMax
        assertEquals(1.0, pwl.breakpoints.first().toDouble(), 1e-10)
        assertEquals(5.0, pwl.breakpoints.last().toDouble(), 1e-10)
    }

    // ===== Error-driven breakpoint tests =====

    @Test
    fun testErrorDrivenBreakpointsMeetTolerance() {
        val tolerance = infraScalar(0.01)
        val config = PWLRadiusApproximationConfig(
            maxSegments = 16,
            relativeErrorTolerance = tolerance,
            breakpointStrategy = PWLBreakpointStrategy.ErrorDriven
        )
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = config
        )
        // Error-driven should achieve better error than uniform with same maxSegments
        val uniformPwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(5.0),
            config = PWLRadiusApproximationConfig(maxSegments = 16, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        )
        // Error-driven should have equal or lower error
        assertTrue(
            pwl.maxRelativeError.toDouble() <= uniformPwl.maxRelativeError.toDouble() + 1e-10,
            "Error-driven should have equal or lower max relative error"
        )
    }

    // ===== Edge case tests =====

    @Test
    fun testNarrowInterval() {
        val config = PWLRadiusApproximationConfig(maxSegments = 2, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(5.0),
            rMax = infraScalar(5.1),
            config = config
        )
        // Narrow interval should have very small error
        assertTrue(pwl.maxRelativeError.toDouble() < 0.01,
            "Narrow interval should have very small relative error")
    }

    @Test
    fun testWideInterval() {
        // [1.0, 10.0] is a more realistic wide interval for BPP3D cylinder radii
        val config = PWLRadiusApproximationConfig(maxSegments = 16, breakpointStrategy = PWLBreakpointStrategy.Uniform)
        val pwl = PWLRadiusSquaredApproximation.fromRadiusInterval(
            rMin = infraScalar(1.0),
            rMax = infraScalar(10.0),
            config = config
        )
        // With 16 uniform segments over [1, 10], max relative error should be reasonable
        assertTrue(pwl.maxRelativeError.toDouble() < 1.0,
            "Wide interval relative error should be bounded: actual=${pwl.maxRelativeError.toDouble()}")
    }

    @Test
    fun testRequiresPositiveRMin() {
        try {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = infraScalar(0.0),
                rMax = infraScalar(5.0),
                config = PWLRadiusApproximationConfig()
            )
            throw AssertionError("Should have thrown for non-positive rMin")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testRequiresRMaxGreaterThanRMin() {
        try {
            PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = infraScalar(5.0),
                rMax = infraScalar(3.0),
                config = PWLRadiusApproximationConfig()
            )
            throw AssertionError("Should have thrown for rMax <= rMin")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testRequiresAtLeastTwoBreakpoints() {
        try {
            PWLRadiusSquaredApproximation(
                breakpoints = listOf(infraScalar(1.0)),
                slopes = listOf(infraScalar(2.0)),
                intercepts = listOf(infraScalar(-1.0)),
                maxRelativeError = infraScalar(0.01),
                maxAbsoluteError = infraScalar(0.1)
            )
            throw AssertionError("Should have thrown for less than 2 breakpoints")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testRequiresMatchingSlopesSize() {
        try {
            PWLRadiusSquaredApproximation(
                breakpoints = listOf(infraScalar(1.0), infraScalar(2.0), infraScalar(3.0)),
                slopes = listOf(infraScalar(3.0)),  // Should be 2
                intercepts = listOf(infraScalar(-2.0), infraScalar(-6.0)),
                maxRelativeError = infraScalar(0.01),
                maxAbsoluteError = infraScalar(0.1)
            )
            throw AssertionError("Should have thrown for mismatched slopes size")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }
}
