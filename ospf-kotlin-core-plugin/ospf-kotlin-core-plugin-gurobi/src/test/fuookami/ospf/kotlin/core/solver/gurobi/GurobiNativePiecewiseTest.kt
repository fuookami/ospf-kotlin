package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

class GurobiNativePiecewiseTest {
    private val input = RealVar("native_input")
    private val result = RealVar("native_result")

    @Test
    fun prepareConvertsValuesToPwlPrimitives() {
        val converter = object : IntoValue<Flt64> {
            override fun intoValue(value: Flt64): Flt64 = value
            override val zero: Flt64 get() = Flt64.zero
            override val one: Flt64 get() = Flt64.one
            override fun fromValue(value: Flt64): Flt64 = Flt64(value.toDouble() * 2.0)
        }
        val prepared = prepareGurobiNativePiecewise(
            structure(
                inputCoefficient = Flt64(0.5),
                breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
                slopes = listOf(Flt64(0.5), Flt64.one),
                intercepts = listOf(Flt64.zero, Flt64(-1.0)),
                converter = converter
            )
        )
        assertTrue(prepared is Ok)
        val data = (prepared as Ok).value
        assertEquals(input.key, data.inputKey)
        assertEquals(result.key, data.resultKey)
        assertContentEquals(doubleArrayOf(0.0, 2.0, 4.0), data.xPoints)
        assertContentEquals(doubleArrayOf(0.0, 2.0, 6.0), data.yPoints)
    }

    @Test
    fun mismatchedDataOrVariablesAvoidsEverySdkWrite() {
        var malformedWrites = 0
        val malformed = writeGurobiNativePiecewise(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(
                structure(name = "valid"),
                structure(slopes = listOf(Flt64.one), name = "invalid")
            ),
            writer = GurobiNativePiecewiseWriter { malformedWrites++ }
        )
        assertTrue(malformed is Failed)
        assertEquals(0, malformedWrites)

        var missingVariableWrites = 0
        val missingVariable = writeGurobiNativePiecewise(
            variableKeys = setOf(input.key),
            structures = listOf(structure()),
            writer = GurobiNativePiecewiseWriter { missingVariableWrites++ }
        )
        assertTrue(missingVariable is Failed)
        assertEquals(0, missingVariableWrites)
    }

    @Test
    fun validDataWritesExactlyOnce() {
        val writes = mutableListOf<GurobiNativePiecewiseData>()
        val written = writeGurobiNativePiecewise(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(structure()),
            writer = GurobiNativePiecewiseWriter { data -> writes.add(data) }
        )
        assertTrue(written is Ok)
        assertEquals(1, writes.size)
        assertEquals("native-piecewise", writes.single().name)
        assertContentEquals(doubleArrayOf(0.0, 1.0, 2.0), writes.single().xPoints)
        assertContentEquals(doubleArrayOf(0.0, 1.0, 3.0), writes.single().yPoints)
    }

    @Test
    fun secondWriteFailureReturnsFailedAfterPriorWrite() {
        val writtenNames = mutableListOf<String>()
        val written = writeGurobiNativePiecewise(
            variableKeys = setOf(input.key, result.key),
            structures = listOf(
                structure(name = "first"),
                structure(name = "second")
            ),
            writer = GurobiNativePiecewiseWriter { data ->
                writtenNames.add(data.name)
                if (data.name == "second") {
                    throw NoSuchMethodError("addGenConstrPWL")
                }
            }
        )
        assertTrue(written is Failed)
        assertEquals(listOf("first", "second"), writtenNames)
    }

    private fun structure(
        inputCoefficient: Flt64 = Flt64.one,
        breakpoints: List<Flt64> = listOf(Flt64.zero, Flt64.one, Flt64.two),
        slopes: List<Flt64> = listOf(Flt64.one, Flt64.two),
        intercepts: List<Flt64> = listOf(Flt64.zero, Flt64(-1.0)),
        converter: IntoValue<Flt64> = IntoValue.Identity,
        name: String = "native-piecewise"
    ): UnivariateLinearPiecewiseStructure<Flt64> {
        return UnivariateLinearPiecewiseStructure(
            input = LinearPolynomial(
                listOf(LinearMonomial(inputCoefficient, input)),
                Flt64.zero
            ),
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            resultVariable = result,
            selectorVariables = emptyList(),
            converter = converter,
            name = name
        )
    }
}
