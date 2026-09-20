package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import gurobi.GRBModel
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

/** Gurobi 10 MAX primitive/writer 定向测试。 / Focused Gurobi 10 MAX primitive/writer tests. */
class GurobiNativeMaxTest {
    private val input = RealVar("gurobi_max_input")
    private val result = RealVar("gurobi_max_result")
    private val selectors = listOf(
        BinVar("gurobi_max_selector_0"),
        BinVar("gurobi_max_selector_1"),
        BinVar("gurobi_max_selector_2")
    )

    init {
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(2.0))
    }

    @Test
    fun prepareKeepsDirectAffineAndConstantInputs() {
        val prepared = prepareGurobiNativeMax(structure())

        assertTrue(prepared is Ok)
        assertEquals(input.key, prepared.value.inputs[0].terms.keys.single())
        assertEquals(1.0, prepared.value.inputs[0].terms.getValue(input.key))
        assertEquals(2.0, prepared.value.inputs[1].terms.getValue(input.key))
        assertEquals(1.0, prepared.value.inputs[1].constant)
        assertTrue(prepared.value.inputs[2].terms.isEmpty())
        assertEquals(4.0, prepared.value.inputs[2].constant)
    }

    @Test
    fun validatesEveryStructureBeforeTheFirstWrite() {
        var writes = 0
        val written = writeGurobiNativeMax(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(
                structure(name = "valid"),
                structure(bigMValues = listOf(Flt64(6.0), Flt64(8.0), Flt64.one), name = "invalid")
            ),
            writer = GurobiNativeMaxWriter { writes++ }
        )

        assertTrue(written is Failed)
        assertEquals(0, writes)
    }

    @Test
    fun writerFailureIsReturnedAfterPriorMaxWrites() {
        val writtenNames = mutableListOf<String>()
        val written = writeGurobiNativeMax(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(structure(name = "first"), structure(name = "second")),
            writer = GurobiNativeMaxWriter { data ->
                writtenNames += data.name
                if (data.name == "second") {
                    throw NoSuchMethodError("addGenConstrMax")
                }
            }
        )

        assertTrue(written is Failed)
        assertEquals(listOf("first", "second"), writtenNames)
    }

    @Test
    fun capabilityRequiresExactMaxSignatures() {
        assertTrue(gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.GeneralMinMax))
        assertFalse(gurobiFunctionSolverCapabilities(WrongSignature::class.java)
            .supports(FunctionNativeCapability.GeneralMinMax))
    }

    @Test
    fun nativeDataRetainsMinimumDirection() {
        val prepared = prepareGurobiNativeMax(structure(minimum = true))

        assertTrue(prepared is Ok)
        assertTrue(prepared.value.minimum)
    }

    private fun structure(
        bigMValues: List<Flt64> = listOf(Flt64(7.0), Flt64(8.0), Flt64.one),
        name: String = "native-max",
        minimum: Boolean = false
    ): MaxStructure<Flt64> {
        return MaxStructure(
            inputs = listOf(
                LinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, input)),
                    constant = Flt64.zero
                ),
                LinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64(2.0), input)),
                    constant = Flt64.one
                ),
                LinearPolynomial(emptyList(), Flt64(4.0))
            ),
            resultVariable = result,
            selectorVariables = selectors,
            bigMValues = bigMValues,
            converter = IntoValue.Identity,
            name = name,
            minimum = minimum
        )
    }

    class WrongSignature {
        fun addGenConstrMax(first: String, second: String, third: String, fourth: String): String = ""
    }
}
