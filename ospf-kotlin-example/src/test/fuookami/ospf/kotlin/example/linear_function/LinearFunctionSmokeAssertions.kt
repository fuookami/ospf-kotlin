package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.AndFunction
import fuookami.ospf.kotlin.core.symbol.function.BalanceTernaryzationFunction
import fuookami.ospf.kotlin.core.symbol.function.BinaryzationFunction
import fuookami.ospf.kotlin.core.symbol.function.BivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.symbol.function.CeilingFunction
import fuookami.ospf.kotlin.core.symbol.function.FloorFunction
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.symbol.function.MinFunction
import fuookami.ospf.kotlin.core.symbol.function.ModFunction
import fuookami.ospf.kotlin.core.symbol.function.NotFunction
import fuookami.ospf.kotlin.core.symbol.function.OneOfFunction
import fuookami.ospf.kotlin.core.symbol.function.OrFunction
import fuookami.ospf.kotlin.core.symbol.function.RoundingFunction
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Triangle
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import fuookami.ospf.kotlin.math.geometry.point3

object LinearFunctionSmokeAssertions {
    private fun polyOf(symbol: Symbol): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, symbol)),
            constant = Flt64.zero
        )
    }

    fun assertAbsFunctionWorks() {
        val x = RealVar("example_abs_x")
        val function = AbsFunction(
            polynomial = polyOf(x),
            converter = flt64TestConverter,
            name = "example_abs"
        )
        val positive = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(3.0)))
        val negative = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(-2.0)))
        assertEquals(Flt64(3.0), positive)
        assertEquals(Flt64(2.0), negative)
    }

    fun assertAndFunctionWorks() {
        val x = RealVar("example_and_x")
        val y = RealVar("example_and_y")
        val function = AndFunction(
            polynomials = listOf(polyOf(x), polyOf(y)),
            converter = flt64TestConverter,
            name = "example_and"
        )
        val allNonZero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.one, y to Flt64(2.0)))
        val containsZero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.one, y to Flt64.zero))
        assertEquals(Flt64.one, allNonZero)
        assertEquals(Flt64.zero, containsZero)
    }

    fun assertBinaryzationFunctionWorks() {
        val x = RealVar("example_bin_x")
        val function = BinaryzationFunction(
            polynomial = polyOf(x),
            converter = flt64TestConverter,
            name = "example_bin"
        )
        val positive = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.one))
        val zero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero))
        val negative = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(-3.0)))
        assertEquals(Flt64.one, positive)
        assertEquals(Flt64.zero, zero)
        assertEquals(Flt64.zero, negative)
    }

    fun assertBivariateLinearPiecewiseFunctionWorks() {
        val x = RealVar("example_blp_x")
        val y = RealVar("example_blp_y")
        val function = BivariateLinearPiecewiseFunction(
            x = polyOf(x),
            y = polyOf(y),
            triangles = listOf(
                Triangle(
                    point3(Flt64.zero, Flt64.zero, Flt64.zero),
                    point3(Flt64.one, Flt64.zero, Flt64.one),
                    point3(Flt64.zero, Flt64.one, Flt64.one)
                )
            ),
            converter = flt64TestConverter,
            name = "example_blp"
        )
        val inside = function.evaluate(
            mapOf<Symbol, Flt64>(
                x to Flt64(0.25),
                y to Flt64(0.25)
            )
        )
        val outside = function.evaluate(
            mapOf<Symbol, Flt64>(
                x to Flt64.one,
                y to Flt64.one
            )
        )
        assertEquals(Flt64(0.75), inside)
        assertNull(outside)
    }

    fun assertBalanceTernaryzationFunctionWorks() {
        val x = RealVar("example_bter_x")
        val function = BalanceTernaryzationFunction(
            x = polyOf(x),
            converter = flt64TestConverter,
            name = "example_bter"
        )
        val positive = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(5.0)))
        val zero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero))
        val negative = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(-5.0)))
        assertEquals(Flt64.one, positive)
        assertEquals(Flt64.zero, zero)
        assertEquals(Flt64(-1.0), negative)
    }

    fun assertCeilingFunctionWorks() {
        val x = RealVar("example_ceil_x")
        val function = CeilingFunction(x = polyOf(x), converter = flt64TestConverter, name = "example_ceil")
        val value = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(1.2)))
        assertEquals(Flt64(2.0), value)
    }

    fun assertFloorFunctionWorks() {
        val x = RealVar("example_floor_x")
        val function = FloorFunction(x = polyOf(x), converter = flt64TestConverter, name = "example_floor")
        val value = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(1.8)))
        assertEquals(Flt64.one, value)
    }

    fun assertIfFunctionWorks() {
        val x = RealVar("example_if_x")
        val function = IfFunction(condition = polyOf(x), converter = flt64TestConverter, name = "example_if")
        val positive = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.1)))
        val negative = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(-0.1)))
        assertEquals(Flt64.one, positive)
        assertEquals(Flt64.zero, negative)
    }

    fun assertMaskingFunctionWorks() {
        val x = RealVar("example_mask_x")
        val z = BinVar("example_mask_z")
        val input = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.one
        )
        val function = MaskingFunction(
            input = input,
            mask = z,
            converter = flt64TestConverter,
            name = "example_mask"
        )
        val maskOn = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), z to Flt64.one))
        val maskOff = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(2.0), z to Flt64.zero))
        assertEquals(Flt64(3.0), maskOn)
        assertEquals(Flt64.zero, maskOff)
    }

    fun assertMaxFunctionWorks() {
        val x = RealVar("example_max_x")
        val y = RealVar("example_max_y")
        val z = RealVar("example_max_z")
        val function = MaxFunction(
            polynomials = listOf(polyOf(x), polyOf(y), polyOf(z)),
            converter = flt64TestConverter,
            name = "example_max"
        )
        val result = function.evaluate(
            mapOf<Symbol, Flt64>(
                x to Flt64(2.0),
                y to Flt64(5.0),
                z to Flt64.one
            )
        )
        assertEquals(Flt64(5.0), result)
    }

    fun assertMinFunctionWorks() {
        val x = RealVar("example_min_x")
        val y = RealVar("example_min_y")
        val z = RealVar("example_min_z")
        val function = MinFunction(
            polynomials = listOf(polyOf(x), polyOf(y), polyOf(z)),
            converter = flt64TestConverter,
            name = "example_min"
        )
        val result = function.evaluate(
            mapOf<Symbol, Flt64>(
                x to Flt64(2.0),
                y to Flt64(5.0),
                z to Flt64.one
            )
        )
        assertEquals(Flt64.one, result)
    }

    fun assertModFunctionWorks() {
        val x = RealVar("example_mod_x")
        val function = ModFunction(
            x = polyOf(x),
            d = Flt64.two,
            converter = flt64TestConverter,
            name = "example_mod"
        )
        val result = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(5.0)))
        assertEquals(Flt64.one, result)
    }

    fun assertNotFunctionWorks() {
        val x = RealVar("example_not_x")
        val function = NotFunction(polynomial = polyOf(x), converter = flt64TestConverter, name = "example_not")
        val zero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero))
        val nonZero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(3.0)))
        assertEquals(Flt64.one, zero)
        assertEquals(Flt64.zero, nonZero)
    }

    fun assertOneOfFunctionWorks() {
        val x = RealVar("example_oneof_x")
        val y = RealVar("example_oneof_y")
        val constantTwo = LinearPolynomial<Flt64>(emptyList(), Flt64(2.0))
        val function = OneOfFunction(
            polynomials = listOf(polyOf(x), polyOf(y), constantTwo),
            converter = flt64TestConverter,
            name = "example_oneof"
        )
        val exactlyOne = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero, y to Flt64.zero))
        val multiple = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero, y to Flt64(1.0)))
        assertEquals(Flt64.one, exactlyOne)
        assertEquals(Flt64.zero, multiple)
    }

    fun assertOrFunctionWorks() {
        val x = RealVar("example_or_x")
        val y = RealVar("example_or_y")
        val function = OrFunction(
            polynomials = listOf(polyOf(x), polyOf(y)),
            converter = flt64TestConverter,
            name = "example_or"
        )
        val allZero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero, y to Flt64.zero))
        val hasNonZero = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero, y to Flt64(3.0)))
        assertEquals(Flt64.zero, allZero)
        assertEquals(Flt64.one, hasNonZero)
    }

    fun assertRoundingFunctionWorks() {
        val x = RealVar("example_round_x")
        val function = RoundingFunction(x = polyOf(x), converter = flt64TestConverter, name = "example_round")
        val low = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(1.2)))
        val high = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(1.6)))
        assertEquals(Flt64.one, low)
        assertEquals(Flt64.two, high)
    }

    fun assertSlackRangeFunctionWorks() {
        val x = RealVar("example_slack_range_x")
        val function = SlackRangeFunction(
            x = polyOf(x),
            lb = LinearPolynomial(emptyList(), -Flt64.two),
            ub = LinearPolynomial(emptyList(), Flt64.two),
            converter = flt64TestConverter,
            name = "example_slack_range"
        )
        val high = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64(5.0)))
        val low = function.evaluate(mapOf<Symbol, Flt64>(x to Flt64.one))
        assertEquals(Flt64(3.0), high)
        assertEquals(Flt64.zero, low)
    }

    fun assertSlackFunctionWorks() {
        val x = RealVar("example_slack_x")
        val y = RealVar("example_slack_y")
        val function = SlackFunction(
            x = polyOf(x),
            y = polyOf(y),
            converter = flt64TestConverter,
            name = "example_slack"
        )
        val result = function.evaluate(
            mapOf<Symbol, Flt64>(
                x to Flt64.one,
                y to Flt64(4.0)
            )
        )
        assertEquals(Flt64(3.0), result)
    }
}
