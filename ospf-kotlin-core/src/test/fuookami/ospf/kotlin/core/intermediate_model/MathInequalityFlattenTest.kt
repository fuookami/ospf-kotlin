package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.toLinearFlattenData
import fuookami.ospf.kotlin.core.model.mechanism.toLinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.model.mechanism.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.variable.*

class MathInequalityFlattenTest {

    @Test
    fun linearExpressionSymbolInConstraintShouldExpandCorrectly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val symbol = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(3.0), x), LinearMonomial(Flt64(2.0), y)),
                constant = Flt64(1.0)
            ),
            name = "sym"
        )

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, symbol)), Flt64.zero)
        val rhs = LinearPolynomial(emptyList(), Flt64(10.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess, "Flatten should succeed for LinearExpressionSymbol")

        val flattenData = result.getOrThrow()
        assertEquals(2, flattenData.monomials.size, "Should have 2 variable monomials after expansion")
        assertTrue(flattenData.constant eq Flt64(1.0 - 10.0), "Constant should be lhs.constant - rhs.constant = -9.0")
    }

    @Test
    fun nestedLinearExpressionSymbolShouldExpandRecursively() {
        val x = RealVar("x")

        val inner = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(2.0), x)),
                constant = Flt64(5.0)
            ),
            name = "inner"
        )

        val outer = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(3.0), inner)),
                constant = Flt64.zero
            ),
            name = "outer"
        )

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, outer)), Flt64.zero)
        val rhs = LinearPolynomial(emptyList(), Flt64(20.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess, "Flatten should succeed for nested symbols")

        val flattenData = result.getOrThrow()
        assertEquals(1, flattenData.monomials.size, "Should have 1 variable monomial after recursive expansion")
        val mono = flattenData.monomials.first()
        assertTrue(mono.coefficient eq Flt64(6.0), "Coefficient should be 3.0 * 2.0 = 6.0")
        assertEquals(x, mono.symbol, "Symbol should be x after expansion")
        assertTrue(flattenData.constant eq Flt64(15.0 - 20.0), "Constant should be 3*5 - 20 = -5.0")
    }

    @Test
    fun sameVariableOnBothSidesShouldMergeCoefficients() {
        val x = RealVar("x")

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(3.0), x)), Flt64.zero)
        val rhs = LinearPolynomial(listOf(LinearMonomial(Flt64(1.0), x)), Flt64(5.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        val flattenData = result.getOrThrow()
        assertEquals(1, flattenData.monomials.size, "Should have 1 monomial after merging")
        val mono = flattenData.monomials.first()
        assertTrue(mono.coefficient eq Flt64(2.0), "Coefficient should be 3.0 - 1.0 = 2.0")
        assertTrue(flattenData.constant eq Flt64(-5.0), "Constant should be 0 - 5 = -5.0")
    }

    @Test
    fun repeatedLhsVariableShouldAccumulateCoefficients() {
        val x = RealVar("x")
        val y = RealVar("y")

        val lhs = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.one, x),
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(-Flt64.one, y)
            ),
            constant = Flt64.zero
        )
        val rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        assertLinearFlattenCoefficients(
            flattenData = result.getOrThrow(),
            expected = mapOf(
                x to Flt64(3.0),
                y to -Flt64.one
            )
        )
    }

    @Test
    fun repeatedLhsVariableFromExpandedSymbolShouldAccumulateCoefficients() {
        val a = RealVar("a")
        val x = RealVar("x")
        val b = RealVar("b")

        val sum = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64.one, a),
                    LinearMonomial(Flt64.one, x),
                    LinearMonomial(Flt64.one, b)
                ),
                constant = Flt64.zero
            ),
            name = "sum"
        )
        val lhs = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.one, x),
                LinearMonomial(Flt64(-0.7), sum)
            ),
            constant = Flt64.zero
        )
        val rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        assertLinearFlattenCoefficients(
            flattenData = result.getOrThrow(),
            expected = mapOf(
                a to Flt64(-0.7),
                x to Flt64(0.3),
                b to Flt64(-0.7)
            )
        )
    }

    @Test
    fun repeatedRhsVariableShouldSubtractEveryCoefficient() {
        val x = RealVar("x")
        val y = RealVar("y")

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(5.0), x)), Flt64.zero)
        val rhs = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64.one, x),
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64.zero
        )
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        assertLinearFlattenCoefficients(
            flattenData = result.getOrThrow(),
            expected = mapOf(
                x to Flt64(2.0),
                y to Flt64(-3.0)
            )
        )
    }

    @Test
    fun repeatedLhsVariableShouldAccumulateInFlt64FlattenPath() {
        val x = RealVar("x")
        val y = RealVar("y")
        val converter = IntoValue.fromConverter(FltX)

        val lhs = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(converter.one, x),
                LinearMonomial(converter.intoValue(Flt64(2.0)), x),
                LinearMonomial(-converter.one, y)
            ),
            constant = converter.zero
        )
        val rhs = LinearPolynomial(emptyList<LinearMonomial<FltX>>(), converter.zero)
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenDataFlt64(converter)
        assertTrue(result.isSuccess)

        assertLinearFlattenCoefficients(
            flattenData = result.getOrThrow(),
            expected = mapOf(
                x to Flt64(3.0),
                y to -Flt64.one
            )
        )
    }

    @Test
    fun repeatedQuadraticMonomialShouldAccumulateCoefficients() {
        val x = RealVar("x")
        val y = RealVar("y")

        val lhs = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(Flt64.one, x, y),
                QuadraticMonomial.quadratic(Flt64(2.0), y, x),
                QuadraticMonomial.linear(Flt64(5.0), x)
            ),
            constant = Flt64.zero
        )
        val rhs = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(Flt64(0.5), x, y),
                QuadraticMonomial.linear(Flt64.one, x)
            ),
            constant = Flt64.zero
        )
        val inequality = QuadraticInequalityOf(lhs, rhs, Comparison.LE)

        val flattenData = inequality.toQuadraticFlattenData()

        assertEquals(2, flattenData.monomials.size, "Repeated quadratic monomials should be merged")
        val xyMono = flattenData.monomials.single {
            (it.symbol1 == x && it.symbol2 == y) || (it.symbol1 == y && it.symbol2 == x)
        }
        assertTrue(xyMono.coefficient eq Flt64(2.5), "x*y coefficient should be 1.0 + 2.0 - 0.5 = 2.5")
        val xMono = flattenData.monomials.single { it.symbol1 == x && it.symbol2 == null }
        assertTrue(xMono.coefficient eq Flt64(4.0), "x coefficient should be 5.0 - 1.0 = 4.0")
    }

    @Test
    fun variableOnlyConstraintShouldWorkAsBefore() {
        val x = RealVar("x")
        val y = RealVar("y")

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(2.0), x), LinearMonomial(Flt64(3.0), y)), Flt64.zero)
        val rhs = LinearPolynomial(emptyList(), Flt64(10.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        val flattenData = result.getOrThrow()
        assertEquals(2, flattenData.monomials.size)
    }

    @Test
    fun linearExpressionSymbolWithCoefficientShouldScaleCorrectly() {
        val x = RealVar("x")

        val symbol = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(2.0), x)),
                constant = Flt64(3.0)
            ),
            name = "sym"
        )

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64(5.0), symbol)), Flt64.zero)
        val rhs = LinearPolynomial(emptyList(), Flt64(30.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        val flattenData = result.getOrThrow()
        assertEquals(1, flattenData.monomials.size)
        val mono = flattenData.monomials.first()
        assertTrue(mono.coefficient eq Flt64(10.0), "Coefficient should be 5.0 * 2.0 = 10.0")
        assertTrue(flattenData.constant eq Flt64(15.0 - 30.0), "Constant should be 5*3 - 30 = -15.0")
    }

    @Test
    fun fltXTypeShouldExpandCorrectly() {
        val x = RealVar("x")
        val two = FltX(2.0)
        val one = FltX.one
        val five = FltX(5.0)

        val symbol = LinearExpressionSymbol<FltX>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(two, x)),
                constant = one
            ),
            name = "sym"
        )

        val lhs = LinearPolynomial(listOf(LinearMonomial(one, symbol)), FltX.zero)
        val rhs = LinearPolynomial(emptyList(), five)
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess, "Flatten should succeed for FltX type")

        val flattenData = result.getOrThrow()
        assertEquals(1, flattenData.monomials.size)
    }

    @Test
    fun rtn64TypeShouldExpandCorrectly() {
        val x = RealVar("x")
        val converter = fuookami.ospf.kotlin.math.algebra.concept.resolveFlt64ValueConverter<Rtn64>(
            "MathInequalityFlattenTest"
        ).valueOrFail()
        val two = converter.intoValue(Flt64(2.0))
        val one = converter.intoValue(Flt64.one)
        val five = converter.intoValue(Flt64(5.0))
        val zero = converter.intoValue(Flt64.zero)

        val symbol = LinearExpressionSymbol<Rtn64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(two, x)),
                constant = one
            ),
            name = "sym"
        )

        val lhs = LinearPolynomial(listOf(LinearMonomial(one, symbol)), zero)
        val rhs = LinearPolynomial(emptyList<LinearMonomial<Rtn64>>(), five)
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess, "Flatten should succeed for Rtn64 type")

        val flattenData = result.getOrThrow()
        assertEquals(1, flattenData.monomials.size)
    }

    @Test
    fun mixedVariableAndSymbolInSameInequalityShouldExpand() {
        val x = RealVar("x")
        val y = RealVar("y")

        val symbol = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(2.0), y)),
                constant = Flt64(3.0)
            ),
            name = "sym"
        )

        val lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x), LinearMonomial(Flt64(4.0), symbol)),
            constant = Flt64.zero
        )
        val rhs = LinearPolynomial(emptyList(), Flt64(20.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertTrue(result.isSuccess)

        val flattenData = result.getOrThrow()
        assertEquals(2, flattenData.monomials.size, "Should have 2 variable monomials (x and y)")
        val xMono = flattenData.monomials.find { it.symbol == x }
        val yMono = flattenData.monomials.find { it.symbol == y }
        assertNotNull(xMono, "x should be present")
        assertNotNull(yMono, "y should be present")
        assertTrue(xMono!!.coefficient eq Flt64(1.0), "x coefficient should be 1.0")
        assertTrue(yMono!!.coefficient eq Flt64(8.0), "y coefficient should be 4.0 * 2.0 = 8.0")
        assertTrue(flattenData.constant eq Flt64(12.0 - 20.0), "Constant should be 4*3 - 20 = -8.0")
    }

    @Test
    fun unsupportedSymbolTypeShouldReturnFailureNotClassCast() {
        val unsupportedSymbol = object : fuookami.ospf.kotlin.math.symbol.Symbol {
            override val name = "unsupported_sym"
            override val displayName: String? = null
        }

        val lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, unsupportedSymbol)), Flt64.zero)
        val rhs = LinearPolynomial(emptyList(), Flt64(5.0))
        val inequality = LinearInequality(lhs, rhs, Comparison.LE)

        val result = inequality.toLinearFlattenData()
        assertFalse(result.isSuccess, "Flatten should fail for unsupported symbol type")
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException,
            "Failure should be IllegalArgumentException, not ClassCastException; got ${exception?.let { it::class.simpleName }}")
        assertTrue(exception?.message?.contains("unsupported_sym") == true,
            "Error message should reference the symbol name")
    }

    private fun assertLinearFlattenCoefficients(
        flattenData: LinearFlattenData<Flt64>,
        expected: Map<AbstractVariableItem<*, *>, Flt64>
    ) {
        assertEquals(expected.size, flattenData.monomials.size, "Repeated linear monomials should be merged")
        val actual = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (mono in flattenData.monomials) {
            val variable = mono.symbol as? AbstractVariableItem<*, *>
                ?: fail("Flattened monomial should use variable symbol: ${mono.symbol.name}")
            actual[variable] = mono.coefficient
        }
        assertEquals(expected.keys, actual.keys)
        for ((variable, coefficient) in expected) {
            val actualCoefficient = actual[variable]
            assertNotNull(actualCoefficient, "Missing coefficient for ${variable.name}")
            assertTrue(
                actualCoefficient eq coefficient,
                "Coefficient for ${variable.name} should be $coefficient but was $actualCoefficient"
            )
        }
    }
}
