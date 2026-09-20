package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

class QuadraticFunctionCompositionTest {
    private val converter = IntoValue.Identity

    private fun variable(name: String): RealVar = RealVar(name).also {
        it.range.geq(Flt64(-2.0))
        it.range.leq(Flt64(2.0))
    }

    private fun square(input: RealVar): QuadraticPolynomial<Flt64> = QuadraticPolynomial(
        listOf(QuadraticMonomial.quadratic(Flt64.one, input, input)),
        Flt64(-1.0)
    )

    private fun value(function: QuadraticFunctionSymbol<Flt64>, values: Map<Symbol, Flt64>): Flt64? =
        function.evaluate(values = values, tokenTable = null, converter = converter, zeroIfNone = false)

    @Test
    fun extremaAndAbsoluteValueUseOriginalQuadraticInputs() {
        val input = variable("input")
        val polynomial = square(input)
        val zero = QuadraticPolynomial<Flt64>(emptyList(), Flt64.zero)
        val functions = listOf(
            QuadraticMaxFunction(polynomials = listOf(polynomial, zero), converter = converter),
            QuadraticMinMaxFunction(polynomials = listOf(polynomial, zero), converter = converter),
            QuadraticMaxMinFunction(polynomials = listOf(polynomial, zero), converter = converter),
            QuadraticAbsFunction(polynomial = polynomial, converter = converter)
        )
        assertEquals(listOf(0.0, 0.0, -1.0, 1.0), functions.map { value(it, mapOf(input to Flt64.zero))!!.toDouble() })
        assertEquals(listOf(3.0, 3.0, 0.0, 3.0), functions.map { value(it, mapOf(input to Flt64(2.0)))!!.toDouble() })
        functions.forEach {
            assertNull(value(it, emptyMap()))
            assertEquals(it.identifier, it.identifier)
            assertEquals(it.polynomial, it.asMutable().toQuadraticPolynomial())
        }
    }

    @Test
    fun conditionalFunctionsKeepFalseTrueAndGapSemantics() {
        val input = variable("condition")
        val polynomial = square(input)
        val condition = QuadraticIfFunction(
            condition = polynomial,
            strictBoundary = Flt64(0.5),
            converter = converter
        )
        val consequent = QuadraticIfThenFunction(
            condition = polynomial,
            thenPoly = polynomial,
            strictBoundary = Flt64(0.5),
            converter = converter
        )
        val interval = QuadraticIfInFunction(
            input = polynomial,
            lower = Flt64.zero,
            upper = Flt64(2.0),
            strictBoundary = Flt64(0.5),
            converter = converter
        )
        assertEquals(Flt64.zero, value(condition, mapOf(input to Flt64.zero)))
        assertEquals(Flt64.one, value(condition, mapOf(input to Flt64(2.0))))
        assertNull(value(condition, mapOf(input to Flt64(1.1))))
        assertEquals(Flt64.zero, value(consequent, mapOf(input to Flt64.zero)))
        assertEquals(Flt64(3.0), value(consequent, mapOf(input to Flt64(2.0))))
        assertNull(value(consequent, mapOf(input to Flt64(1.1))))
        assertEquals(Flt64.one, value(interval, mapOf(input to Flt64.one)))
        assertEquals(Flt64.zero, value(interval, mapOf(input to Flt64(2.0))))
    }

    @Test
    fun inequalityAndMaskingPreserveNegativeValues() {
        val input = variable("masked")
        val mask = BinVar("mask")
        val polynomial = square(input)
        val masking = QuadraticMaskingFunction(input = polynomial, mask = mask, converter = converter)
        val inequality = QuadraticInequalityFunction(
            lhs = polynomial,
            rhs = Flt64.zero,
            sign = Comparison.LE,
            converter = converter
        )
        assertEquals(Flt64(-1.0), value(masking, mapOf(input to Flt64.zero, mask to Flt64.one)))
        assertEquals(Flt64.zero, value(masking, mapOf(input to Flt64.zero, mask to Flt64.zero)))
        assertEquals(Flt64.one, value(inequality, mapOf(input to Flt64.zero)))
        assertEquals(Flt64.zero, value(inequality, mapOf(input to Flt64(2.0))))
    }

    @Test
    fun everyNewFunctionSurvivesMetaMechanismAndIntermediateConversion() = runBlocking {
        val input = variable("pipeline_input")
        val mask = BinVar("pipeline_mask")
        val polynomial = square(input)
        val zero = QuadraticPolynomial<Flt64>(emptyList(), Flt64.zero)
        val functions = listOf(
            QuadraticMaxFunction(polynomials = listOf(polynomial, zero), converter = converter),
            QuadraticMinMaxFunction(polynomials = listOf(polynomial, zero), converter = converter),
            QuadraticMaxMinFunction(polynomials = listOf(polynomial, zero), converter = converter),
            QuadraticAbsFunction(polynomial = polynomial, converter = converter),
            QuadraticMaskingFunction(input = polynomial, mask = mask, converter = converter),
            QuadraticIfFunction(condition = polynomial, converter = converter),
            QuadraticIfInFunction(input = polynomial, lower = Flt64.zero, upper = Flt64.one, converter = converter),
            QuadraticIfThenFunction(condition = polynomial, thenPoly = polynomial, converter = converter),
            QuadraticInequalityFunction(lhs = polynomial, rhs = Flt64.zero, sign = Comparison.LE, converter = converter)
        )
        for (function in functions) {
            val meta = QuadraticMetaModel<Flt64>(name = function.name, converter = converter)
            try {
                assertIs<Ok<*, *, *>>(meta.add(listOf(input, mask)))
                assertIs<Ok<*, *, *>>(meta.add(function))
                assertIs<Ok<*, *, *>>(meta.minimize(function.polynomial))
                val result = QuadraticMechanismModel.invoke<Flt64>(metaModel = meta, concurrent = false)
                assertTrue(result is Ok, "$function: $result")
                val mechanism = result.value
                assertTrue(mechanism.constraints.any { it.name.contains("_input_") })
                val intermediate = QuadraticTetradModel(mechanism)
                assertTrue(intermediate.objective.objective.any { it.coefficient != Flt64.zero })
                assertEquals(mechanism.tokens.tokens.size, intermediate.variables.size)
            } finally {
                meta.close()
            }
        }
    }

    @Test
    fun maskingConstraintsRejectIncorrectResultAndIncorrectBridge() = runBlocking {
        val input = variable("rows_input")
        val mask = BinVar("rows_mask")
        val function = QuadraticMaskingFunction(input = square(input), mask = mask, converter = converter, name = "rows")
        val meta = QuadraticMetaModel<Flt64>(name = "rows", converter = converter)
        try {
            assertIs<Ok<*, *, *>>(meta.add(listOf(input, mask)))
            assertIs<Ok<*, *, *>>(meta.add(function))
            assertIs<Ok<*, *, *>>(meta.minimize(function.polynomial))
            val result = QuadraticMechanismModel.invoke<Flt64>(metaModel = meta, concurrent = false)
            assertTrue(result is Ok)
            val mechanism = result.value
            fun feasible(maskValue: Double, output: Double, bridge: Double = -1.0): Boolean {
                val values = mechanism.tokens.tokens.sortedBy { it.solverIndex }.map { token ->
                    when (token.variable.name) {
                        input.name -> Flt64.zero
                        mask.name -> Flt64(maskValue)
                        "rows_input_0" -> Flt64(bridge)
                        "rows_masking" -> Flt64(output)
                        else -> error(token.variable.name)
                    }
                }
                return mechanism.constraints.all { row ->
                    var lhs = 0.0
                    for (cell in row.lhs) {
                        lhs += cell.evaluate(values)!!.toDouble()
                    }
                    val rhs = row.rhs.toDouble()
                    when (row.sign.toComparison()) {
                        Comparison.LE -> lhs <= rhs + 1e-9
                        Comparison.GE -> lhs >= rhs - 1e-9
                        Comparison.EQ -> kotlin.math.abs(lhs - rhs) <= 1e-9
                        else -> error("Unexpected relation")
                    }
                }
            }
            assertTrue(feasible(1.0, -1.0))
            assertTrue(feasible(0.0, 0.0))
            assertFalse(feasible(1.0, 0.0))
            assertFalse(feasible(0.0, -1.0))
            assertFalse(feasible(1.0, 0.0, 0.0))
        } finally {
            meta.close()
        }
    }

    @Test
    fun invalidBoundsAndEmptyInputsFailBeforeAddingTokens() {
        val input = RealVar("unbounded")
        input.range.set(fuookami.ospf.kotlin.math.algebra.value_range.ValueRange(Flt64.negativeInfinity, Flt64.infinity).value!!)
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        val unbounded = QuadraticAbsFunction(polynomial = square(input), converter = converter)
        assertIs<Failed<*, *, *>>(unbounded.registerAuxiliaryTokens(tokens))
        assertEquals(0, tokens.tokens.size)
        val empty = QuadraticMaxFunction<Flt64>(polynomials = emptyList(), converter = converter)
        assertIs<Failed<*, *, *>>(empty.registerAuxiliaryTokens(tokens))
        assertEquals(0, tokens.tokens.size)
    }

    @Test
    fun affineInputsDoNotIntroduceBindingVariables() {
        val input = variable("affine")
        val polynomial = QuadraticPolynomial(listOf(QuadraticMonomial.linear(Flt64.one, input)), Flt64.zero)
        val function = QuadraticAbsFunction(polynomial = polynomial, converter = converter)
        assertFalse(function.helperVariables.any { it.name.contains("_input_") })
        assertEquals(4, function.helperVariables.size)
    }

    @Test
    fun nestedSymbolsRegisterDependenciesAndEvaluateAcrossEntryPoints() = runBlocking {
        val input = variable("nested_input")
        val inner = QuadraticAbsFunction(polynomial = square(input), converter = converter, name = "inner")
        val outer = QuadraticMaxFunction(
            polynomials = listOf(
                QuadraticPolynomial(listOf(QuadraticMonomial.linear(Flt64.one, inner)), Flt64.zero),
                QuadraticPolynomial(emptyList(), Flt64(2.0))
            ),
            converter = converter,
            name = "outer"
        )
        assertTrue(inner in outer.dependencies)
        assertEquals(Flt64(3.0), value(outer, mapOf(input to Flt64(2.0))))
        val meta = QuadraticMetaModel<Flt64>(name = "nested", converter = converter)
        try {
            assertIs<Ok<*, *, *>>(meta.add(input))
            assertIs<Ok<*, *, *>>(meta.add(inner))
            assertIs<Ok<*, *, *>>(meta.add(outer))
            assertIs<Ok<*, *, *>>(meta.minimize(outer.polynomial))
            val result = QuadraticMechanismModel.invoke<Flt64>(metaModel = meta, concurrent = false)
            assertTrue(result is Ok, result.toString())
            val mechanism = result.value
            assertTrue(mechanism.constraints.any { it.name.startsWith("inner") })
            assertTrue(mechanism.constraints.any { it.name.startsWith("outer") })
            val values = List(mechanism.tokens.tokens.size) { Flt64(2.0) }
            assertEquals(Flt64(3.0), outer.evaluate(values, mechanism.tokens, converter, false))
            assertEquals(Flt64(3.0), outer.prepare(mapOf(input to Flt64(2.0)), mechanism.tokens, converter))
        } finally {
            meta.close()
        }
    }

    @Test
    fun wideningCapturedInputBoundsIsRejected() {
        val input = variable("widened")
        val function = QuadraticAbsFunction(polynomial = square(input), converter = converter)
        function.helperVariables
        input.range.set(fuookami.ospf.kotlin.math.algebra.value_range.ValueRange(Flt64(-10.0), Flt64(10.0)).value!!)
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertIs<Failed<*, *, *>>(function.registerAuxiliaryTokens(tokens))
        assertEquals(0, tokens.tokens.size)
    }

    @Test
    fun failedConstraintWriteRestoresRowsAndTokens() = runBlocking {
        val input = variable("failure_input")
        val function = QuadraticAbsFunction(polynomial = square(input), converter = converter)
        val meta = QuadraticMetaModel<Flt64>(name = "failure", converter = converter)
        try {
            assertIs<Ok<*, *, *>>(meta.add(input))
            assertIs<Ok<*, *, *>>(meta.minimize(square(input)))
            assertIs<Ok<*, *, *>>(function.registerAuxiliaryTokens(meta.tokens))
            val result = QuadraticMechanismModel.invoke<Flt64>(metaModel = meta, concurrent = false)
            assertTrue(result is Ok)
            val mechanism = result.value
            val beforeRows = mechanism.constraints.size
            val beforeTokens = mechanism.tokens.tokens.size
            var writes = 0
            val target = object : AbstractQuadraticMechanismModel<Flt64> by mechanism {
                override fun addConstraint(
                    relation: QuadraticInequalityOf<Flt64>,
                    name: String?,
                    from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
                ): Try {
                    if (++writes == 2) return Failed(ErrorCode.ApplicationError, "injected")
                    return mechanism.addConstraint(relation = relation, name = name, from = from)
                }
            }
            assertIs<Failed<*, *, *>>(function.registerConstraints(target))
            assertEquals(beforeRows, mechanism.constraints.size)
            assertEquals(beforeTokens, mechanism.tokens.tokens.size)
        } finally {
            meta.close()
        }
    }
}
