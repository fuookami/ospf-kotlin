package fuookami.ospf.kotlin.core.solver.cplex

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import ilog.concert.IloNumVar
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.variable.RealVar

/** CPLEX 原生 PWL writer 定向测试。 / Directed tests for the CPLEX native PWL writer. */
class CplexNativePiecewiseTest {
    private val input = RealVar("cplex_native_input")
    private val result = RealVar("cplex_native_result")
    private val secondResult = RealVar("cplex_native_second_result")

    @Test
    fun detectsExactLinkedSos2Signature() {
        assertTrue(cplexFunctionSolverCapabilities().supports(FunctionNativeCapability.SOS2))
        assertFalse(cplexFunctionSolverCapabilities(WrongCplexApi::class.java).supports(FunctionNativeCapability.SOS2))
    }

    @Test
    fun preflightsTheWholeBatchBeforeAnySdkWrite() {
        var writes = 0
        val outcome = writeCplexNativePiecewise(
            variableKeys = setOf(input.key, result.key),
            data = listOf(
                data(name = "valid"),
                data(name = "invalid", yPoints = doubleArrayOf(0.0))
            ),
            writer = CplexNativePiecewiseWriter { writes++ }
        )

        assertTrue(outcome is Failed)
        assertEquals(0, writes)
    }

    @Test
    fun convertsSdkWriteFailureToFailedAfterPriorWrites() {
        val writtenNames = ArrayList<String>()
        val outcome = writeCplexNativePiecewise(
            variableKeys = setOf(
                input.key,
                result.key,
                secondResult.key
            ),
            data = listOf(data(name = "first"), data(name = "second").copy(resultKey = secondResult.key)),
            writer = CplexNativePiecewiseWriter { piecewise ->
                writtenNames += piecewise.name
                if (piecewise.name == "second") {
                    throw NoSuchMethodError("addSOS2")
                }
            }
        )

        assertTrue(outcome is Failed)
        assertEquals(listOf("first", "second"), writtenNames)
    }

    @Test
    fun validBatchWritesEveryPiecewiseRelationExactlyOnce() {
        val writtenNames = ArrayList<String>()
        val outcome = writeCplexNativePiecewise(
            variableKeys = setOf(
                input.key,
                result.key,
                secondResult.key
            ),
            data = listOf(data(name = "first"), data(name = "second").copy(resultKey = secondResult.key)),
            writer = CplexNativePiecewiseWriter { piecewise -> writtenNames += piecewise.name }
        )

        assertTrue(outcome is Ok)
        assertEquals(listOf("first", "second"), writtenNames)
    }

    private fun data(
        name: String,
        yPoints: DoubleArray = doubleArrayOf(
            0.0,
            1.0,
            3.0
        )
    ): NativePiecewiseData {
        return NativePiecewiseData(
            inputKey = input.key,
            resultKey = result.key,
            xPoints = doubleArrayOf(
                0.0,
                1.0,
                2.0
            ),
            yPoints = yPoints,
            name = name
        )
    }

    private class WrongCplexApi {
        fun addSOS2(variables: Array<IloNumVar>, weights: DoubleArray): String {
            return "${variables.size}:${weights.size}"
        }
    }
}
