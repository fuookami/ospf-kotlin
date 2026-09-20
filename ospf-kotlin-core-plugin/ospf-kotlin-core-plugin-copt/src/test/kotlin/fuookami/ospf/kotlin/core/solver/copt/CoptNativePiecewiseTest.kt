package fuookami.ospf.kotlin.core.solver.copt

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Failed

class CoptNativePiecewiseTest {
    private val inputKey = VariableItemKey(UInt64(1), 0)
    private val resultKey = VariableItemKey(UInt64(2), 0)

    @Test
    fun currentBindingExposesExactSos2Signature() {
        assertTrue(coptSupportsNativePiecewise())
    }

    @Test
    fun missingSosSignatureDoesNotPassCapabilityCheck() {
        assertFalse(coptSupportsNativePiecewise(Any::class.java))
    }

    @Test
    fun malformedBatchFailsBeforeNativeAccess() {
        val result = validateCoptNativePiecewiseData(
            variableKeys = setOf(inputKey, resultKey),
            data = listOf(
                NativePiecewiseData(
                    inputKey = inputKey,
                    resultKey = resultKey,
                    xPoints = doubleArrayOf(0.0, 1.0),
                    yPoints = doubleArrayOf(0.0, Double.NaN),
                    name = "malformed"
                )
            ),
            infinity = 1.0e30
        )

        assertTrue(result is Failed)
        assertFalse(result.ok)
    }

    @Test
    fun missingVariableFailsBatchPreflight() {
        val result = validateCoptNativePiecewiseData(
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
            infinity = 1.0e30
        )

        assertTrue(result is Failed)
        assertFalse(result.ok)
    }
}
