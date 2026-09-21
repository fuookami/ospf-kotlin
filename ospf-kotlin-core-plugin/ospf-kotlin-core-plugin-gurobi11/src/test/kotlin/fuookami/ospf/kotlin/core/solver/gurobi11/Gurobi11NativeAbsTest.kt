package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

class Gurobi11NativeAbsTest {
    private val input = RealVar("gurobi11_abs_input")
    private val result = URealVar("gurobi11_abs_result")
    private val positive = URealVar("gurobi11_abs_positive")
    private val negative = URealVar("gurobi11_abs_negative")
    private val sign = BinVar("gurobi11_abs_sign")

    @Test
    fun preparePreservesAsymmetricBounds() {
        val prepared = prepareGurobiNativeAbs(structure())

        assertTrue(prepared is Ok)
        assertEquals(input.key, prepared.value.inputKey)
        assertEquals(result.key, prepared.value.resultKey)
        assertEquals(3.0, prepared.value.positiveBigM)
        assertEquals(7.0, prepared.value.negativeBigM)
    }

    @Test
    fun validatesAllDataBeforeAnySdkWrite() {
        var writes = 0
        val written = writeGurobiNativeAbs(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(
                structure(name = "valid"),
                structure(positiveBigM = Flt64(-1.0), name = "invalid")
            ),
            writer = GurobiNativeAbsWriter { writes++ }
        )

        assertTrue(written is Failed)
        assertEquals(0, writes)
    }

    @Test
    fun missingVariableAvoidsSdkWrite() {
        var writes = 0
        val written = writeGurobiNativeAbs(
            variableKeys = setOf(input.key),
            structures = listOf(structure()),
            writer = GurobiNativeAbsWriter { writes++ }
        )

        assertTrue(written is Failed)
        assertEquals(0, writes)
    }

    @Test
    fun writerFailureIsReturnedAfterPriorWrites() {
        val writtenNames = mutableListOf<String>()
        val written = writeGurobiNativeAbs(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(
                structure(name = "first"),
                structure(name = "second")
            ),
            writer = GurobiNativeAbsWriter { data ->
                writtenNames += data.name
                if (data.name == "second") {
                    throw NoSuchMethodError("addGenConstrAbs")
                }
            }
        )

        assertTrue(written is Failed)
        assertEquals(listOf("first", "second"), writtenNames)
    }

    @Test
    fun capabilityRequiresExactAbsSignature() {
        val capabilities = gurobiFunctionSolverCapabilities()
        assertTrue(capabilities.supports(FunctionNativeCapability.GeneralAbs))
        assertFalse(gurobiFunctionSolverCapabilities(WrongSignature::class.java)
            .supports(FunctionNativeCapability.GeneralAbs))
    }

    private fun structure(
        positiveBigM: Flt64 = Flt64(3.0),
        negativeBigM: Flt64 = Flt64(7.0),
        name: String = "gurobi11-native-abs"
    ): AbsStructure<Flt64> {
        return AbsStructure(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            resultVariable = result,
            positiveVariable = positive,
            negativeVariable = negative,
            signVariable = sign,
            positiveBigM = positiveBigM,
            negativeBigM = negativeBigM,
            converter = IntoValue.Identity,
            name = name
        )
    }

    class WrongSignature {
        fun addGenConstrAbs(first: String, second: String, third: String): String = ""
    }
}
