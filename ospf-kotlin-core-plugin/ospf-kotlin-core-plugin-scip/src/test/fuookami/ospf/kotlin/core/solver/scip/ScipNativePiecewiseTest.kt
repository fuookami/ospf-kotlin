package fuookami.ospf.kotlin.core.solver.scip

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Failed

class ScipNativePiecewiseTest {
    private val inputKey = VariableItemKey(UInt64(1), 0)
    private val resultKey = VariableItemKey(UInt64(2), 0)
    private val variableKeys = setOf(inputKey, resultKey)

    @Test
    fun currentBindingExposesExactSos2Creator() {
        assertTrue(scipSupportsNativePiecewise())
    }

    @Test
    fun malformedPrimitiveDataFailsBeforeNativeAccess() {
        val result = validateScipNativePiecewiseData(
            variableKeys = variableKeys,
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, 1.0),
                    yPoints = doubleArrayOf(0.0, Double.NaN),
                    name = "non-finite"
                )
            ),
            infinity = 1.0e20
        )

        assertTrue(result is Failed)
    }

    @Test
    fun positiveAndNegativeInfinityFailPrimitiveValidation() {
        val positiveInfinity = validateScipNativePiecewiseData(
            variableKeys = variableKeys,
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, Double.POSITIVE_INFINITY),
                    yPoints = doubleArrayOf(0.0, 1.0),
                    name = "positive-infinity"
                )
            ),
            infinity = 1.0e20
        )
        val negativeInfinity = validateScipNativePiecewiseData(
            variableKeys = variableKeys,
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, 1.0),
                    yPoints = doubleArrayOf(0.0, Double.NEGATIVE_INFINITY),
                    name = "negative-infinity"
                )
            ),
            infinity = 1.0e20
        )

        assertTrue(positiveInfinity is Failed)
        assertTrue(negativeInfinity is Failed)
    }

    @Test
    fun finitePrimitiveDataWithinScipSentinelPassesValidation() {
        val result = validateScipNativePiecewiseData(
            variableKeys = variableKeys,
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(-1.0e10, 0.0, 1.0e10),
                    yPoints = doubleArrayOf(-2.0e10, 0.0, 2.0e10),
                    name = "finite"
                )
            ),
            infinity = 1.0e20
        )

        assertTrue(result.ok)
    }

    @Test
    fun nonIncreasingOrSentinelBoundPointsFailPrimitiveValidation() {
        val nonIncreasing = validateScipNativePiecewiseData(
            variableKeys = variableKeys,
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, 0.0),
                    yPoints = doubleArrayOf(0.0, 1.0),
                    name = "non-increasing"
                )
            ),
            infinity = 1.0e20
        )
        val sentinelBound = validateScipNativePiecewiseData(
            variableKeys = variableKeys,
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, 1.0e20),
                    yPoints = doubleArrayOf(0.0, 1.0),
                    name = "sentinel-bound"
                )
            ),
            infinity = 1.0e20
        )

        assertTrue(nonIncreasing is Failed)
        assertTrue(sentinelBound is Failed)
    }

    @Test
    fun missingVariableKeyFailsPrimitiveValidation() {
        val result = validateScipNativePiecewiseData(
            variableKeys = setOf(inputKey),
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, 1.0),
                    yPoints = doubleArrayOf(0.0, 1.0),
                    name = "missing-result"
                )
            ),
            infinity = 1.0e20
        )

        assertTrue(result is Failed)
        assertFalse(result.ok)
    }
}
