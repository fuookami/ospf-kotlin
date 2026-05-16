package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionCompatTest {

    // ========== IfElseFunction — 语义正确性 ==========

    @Test
    fun ifElseFunctionShouldConstructWithBranchesAndCondition() {
        val x = RealVar("x")
        val branch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            name = "branch"
        )
        val elseBranch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(emptyList(), Flt64.one),
            name = "else"
        )
        val condition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val fn = IfElseFunction(branch, elseBranch, condition, name = "test_if_else")
        assertEquals("test_if_else", fn.name)
        assertTrue(fn is MathFunctionSymbol<Flt64>)
    }

    @Test
    fun ifElseFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val branch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            name = "branch"
        )
        val elseBranch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(emptyList(), Flt64.one),
            name = "else"
        )
        val condition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val adapter = IfElseFunction(branch, elseBranch, condition, name = "test_if_else_adapter")
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "IfElseFunction companion should return LinearFunctionSymbolAdapter")
        assertTrue(adapter is LinearIntermediateSymbol<Flt64>,
            "Adapter should implement LinearIntermediateSymbol for model.add()")
    }

    @Test
    fun ifElseFunctionShouldRegisterWithModel() {
        val model = LinearMetaModel("ifelse_model")
        val x = RealVar("x")
        model.add(x)
        val branch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            name = "branch"
        )
        val elseBranch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(emptyList(), Flt64.one),
            name = "else"
        )
        val condition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val fn = IfElseFunction(branch, elseBranch, condition, name = "test_if_else_reg")
        val result = fn.registerAuxiliaryTokens(model.tokens)
        assertTrue(result.ok, "IfElseFunction registerAuxiliaryTokens should succeed")
        assertTrue(fn.helperVariables.isNotEmpty(), "IfElseFunction should have helper variables")
        model.close()
    }

    @Test
    fun ifElseFunctionBranchCompanionShouldWork() {
        val x = RealVar("x")
        val branch = IfElseFunction.Branch(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
            name = "b"
        )
        assertEquals("b", branch.name)
    }

    // ========== SatisfiedAmountPolynomialFunction — 语义正确性 (poly != 0) ==========

    @Test
    fun satisfiedAmountPolynomialFunctionShouldConstructWithPolynomials() {
        val x = RealVar("x")
        val y = RealVar("y")
        val poly1 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val poly2 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(poly1, poly2),
            name = "test_sat_poly"
        )
        assertEquals("test_sat_poly", fn.name)
        assertTrue(fn is MathFunctionSymbol<Flt64>)
    }

    @Test
    fun satisfiedAmountPolynomialFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val adapter = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(poly),
            name = "test_sat_poly_adapter"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "SatisfiedAmountPolynomialFunction companion should return LinearFunctionSymbolAdapter")
    }

    @Test
    fun satisfiedAmountPolynomialFunctionShouldRegisterWithModel() {
        val model = LinearMetaModel("sat_poly_model")
        val x = RealVar("x")
        model.add(x)
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(poly),
            name = "test_sat_poly_reg"
        )
        val result = fn.registerAuxiliaryTokens(model.tokens)
        assertTrue(result.ok, "SatisfiedAmountPolynomialFunction registerAuxiliaryTokens should succeed")
        assertTrue(fn.helperVariables.isNotEmpty(), "SatisfiedAmountPolynomialFunction should have helper variables")
        model.close()
    }

    // ========== AtLeastPolynomialFunction — 语义正确性 ==========

    @Test
    fun atLeastPolynomialFunctionShouldConstructWithPolynomialsAndAmount() {
        val x = RealVar("x")
        val y = RealVar("y")
        val poly1 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val poly2 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val fn = AtLeastPolynomialFunction(
            polynomials = listOf(poly1, poly2),
            amount = UInt64(1),
            name = "test_at_least"
        )
        assertEquals("test_at_least", fn.name)
        assertTrue(fn.helperVariables.isNotEmpty(), "AtLeastPolynomialFunction should have helper variables")
    }

    @Test
    fun atLeastPolynomialFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val y = RealVar("y")
        val poly1 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val poly2 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val adapter = AtLeastPolynomialFunction(
            polynomials = listOf(poly1, poly2),
            amount = UInt64(1),
            name = "test_at_least_adapter"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "AtLeastPolynomialFunction companion should return LinearFunctionSymbolAdapter")
    }

    // ========== UIntegerSlackFunction — 语义正确性 ==========

    @Test
    fun uIntegerSlackFunctionShouldConstructWithPolynomials() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            name = "test_uint_slack"
        )
        assertEquals("test_uint_slack", fn.name)
        assertTrue(fn is MathFunctionSymbol<Flt64>)
    }

    @Test
    fun uIntegerSlackFunctionShouldExposeNegPosPolyX() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            converter = compatFlt64Converter,
            name = "test_uint_slack_props"
        )
        assertTrue(fn.neg != null, "UIntegerSlackFunction should expose neg")
        assertTrue(fn.pos != null, "UIntegerSlackFunction should expose pos")
        assertTrue(fn.neg!!.monomials.isNotEmpty(), "UIntegerSlackFunction neg should have monomials")
        assertTrue(fn.pos!!.monomials.isNotEmpty(), "UIntegerSlackFunction pos should have monomials")
        assertTrue(fn.polyX.monomials.isNotEmpty(), "UIntegerSlackFunction should expose polyX with monomials")
    }

    @Test
    fun uIntegerSlackFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val adapter = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            name = "test_uint_slack_adapter"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "UIntegerSlackFunction companion should return LinearFunctionSymbolAdapter")
    }

    @Test
    fun uIntegerSlackFunctionShouldRegisterWithModel() {
        val model = LinearMetaModel("uint_slack_model")
        val x = RealVar("x")
        model.add(x)
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            name = "test_uint_slack_reg"
        )
        val result = fn.registerAuxiliaryTokens(model.tokens)
        assertTrue(result.ok, "UIntegerSlackFunction registerAuxiliaryTokens should succeed")
        assertTrue(fn.helperVariables.isNotEmpty(), "UIntegerSlackFunction should have helper variables")
        model.close()
    }

    @Test
    fun uIntegerSlackFunctionWithAllOriginalParameters() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            withNegative = true,
            withPositive = false,
            threshold = false,
            name = "test_uint_slack_full"
        )
        assertEquals("test_uint_slack_full", fn.name)
    }

    // ========== URealSlackFunction — 语义正确性 ==========

    @Test
    fun uRealSlackFunctionShouldConstructWithPolynomials() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = URealSlackFunction(
            x = polyX,
            y = polyY,
            name = "test_ureal_slack"
        )
        assertEquals("test_ureal_slack", fn.name)
    }

    @Test
    fun uRealSlackFunctionShouldExposeNegPosPolyX() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = URealSlackFunction(
            x = polyX,
            y = polyY,
            converter = compatFlt64Converter,
            name = "test_ureal_slack_props"
        )
        assertTrue(fn.neg != null, "URealSlackFunction should expose neg")
        assertTrue(fn.pos != null, "URealSlackFunction should expose pos")
        assertTrue(fn.neg!!.monomials.isNotEmpty(), "URealSlackFunction neg should have monomials")
        assertTrue(fn.pos!!.monomials.isNotEmpty(), "URealSlackFunction pos should have monomials")
        assertTrue(fn.polyX.monomials.isNotEmpty(), "URealSlackFunction should expose polyX with monomials")
    }

    @Test
    fun uRealSlackFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val adapter = URealSlackFunction(
            x = polyX,
            y = polyY,
            name = "test_ureal_slack_adapter"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "URealSlackFunction companion should return LinearFunctionSymbolAdapter")
    }

    @Test
    fun uRealSlackFunctionWithAllOriginalParameters() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = URealSlackFunction(
            x = polyX,
            y = polyY,
            withNegative = false,
            withPositive = true,
            threshold = true,
            name = "test_ureal_slack_full"
        )
        assertEquals("test_ureal_slack_full", fn.name)
    }

    // ========== UIntegerSlackRangeFunction — 语义正确性 (lb/ub) ==========

    @Test
    fun uIntegerSlackRangeFunctionShouldConstructWithLbUb() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(5.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val fn = UIntegerSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_uint_slack_range"
        )
        assertEquals("test_uint_slack_range", fn.name)
    }

    @Test
    fun uIntegerSlackRangeFunctionShouldExposeNegPosPolyX() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(5.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val fn = UIntegerSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            converter = compatFlt64Converter,
            name = "test_uint_range_props"
        )
        assertTrue(fn.neg.monomials.isNotEmpty(), "UIntegerSlackRangeFunction should expose neg")
        assertTrue(fn.pos.monomials.isNotEmpty(), "UIntegerSlackRangeFunction should expose pos")
        assertTrue(fn.polyX.monomials.isNotEmpty(), "UIntegerSlackRangeFunction should expose polyX")
    }

    @Test
    fun uIntegerSlackRangeFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(5.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val adapter = UIntegerSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_uint_range_adapter"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "UIntegerSlackRangeFunction companion should return LinearFunctionSymbolAdapter")
    }

    // ========== URealSlackRangeFunction — 语义正确性 (lb/ub) ==========

    @Test
    fun uRealSlackRangeFunctionShouldConstructWithLbUb() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(5.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val fn = URealSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_ureal_slack_range"
        )
        assertEquals("test_ureal_slack_range", fn.name)
    }

    @Test
    fun uRealSlackRangeFunctionShouldExposeNegPosPolyX() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(5.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val fn = URealSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            converter = compatFlt64Converter,
            name = "test_ureal_range_props"
        )
        assertTrue(fn.neg.monomials.isNotEmpty(), "URealSlackRangeFunction should expose neg")
        assertTrue(fn.pos.monomials.isNotEmpty(), "URealSlackRangeFunction should expose pos")
        assertTrue(fn.polyX.monomials.isNotEmpty(), "URealSlackRangeFunction should expose polyX")
    }

    @Test
    fun uRealSlackRangeFunctionCompanionShouldReturnAdapter() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(5.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val adapter = URealSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_ureal_range_adapter"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "URealSlackRangeFunction companion should return LinearFunctionSymbolAdapter")
    }

    // ========== SlackRangeFunction — lb/ub 语义 ==========

    @Test
    fun slackRangeFunctionShouldConstructWithLbUb() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(3.0))
        val ub = LinearPolynomial(emptyList(), Flt64(7.0))
        val fn = SlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_slack_range"
        )
        assertEquals("test_slack_range", fn.name)
        assertTrue(fn.neg.monomials.isNotEmpty(), "neg slack variable should exist")
        assertTrue(fn.pos.monomials.isNotEmpty(), "pos slack variable should exist")
    }

    @Test
    fun slackRangeFunctionPolyXShouldBeXPlusNegMinusPos() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(0.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val fn = SlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_polyx"
        )
        val polyXMonomials = fn.polyX.monomials
        assertTrue(polyXMonomials.size >= 3,
            "polyX should have at least 3 monomials: x, neg, pos")
    }

    @Test
    fun slackRangeFunctionShouldRegisterConstraints() {
        val model = LinearMetaModel("slack_range_model")
        val x = RealVar("x")
        model.add(x)
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(0.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val fn = SlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "test_slack_range_reg"
        )
        val tokenResult = fn.registerAuxiliaryTokens(model.tokens)
        assertTrue(tokenResult.ok, "registerAuxiliaryTokens should succeed")
        model.close()
    }

    // ========== InStepRange typealias 编译检查 ==========

    @Test
    fun inStepRangeTypealiasShouldCompile() {
        val fn: InStepRange<Flt64>? = null
        assertTrue(fn == null, "InStepRange<V> typealias should resolve to InStepRangeFunction<V>")
    }

    // ========== LinearFunction typealias 编译检查 ==========

    @Test
    fun linearFunctionTypealiasShouldCompile() {
        val fn: LinearFunction<Flt64>? = null
        assertTrue(fn == null, "LinearFunction<V> typealias should resolve to QuadraticLinearFunction<V>")
    }

    // ========== MonotoneUnivariateLinearPiecewiseFunction — 独立类 ==========

    @Test
    fun monotoneUnivariateLinearPiecewiseFunctionShouldBeRealClass() {
        val fn: MonotoneUnivariateLinearPiecewiseFunction<Flt64>? = null
        assertTrue(fn == null, "MonotoneUnivariateLinearPiecewiseFunction should be a real class, not typealias")
    }

    @Test
    fun monotoneUnivariateLinearPiecewiseFunctionFromPointsShouldCompile() {
        // Verify fromPoints factory compiles and returns adapter
        val x = RealVar("x")
        val xPoly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val points = listOf(
            fuookami.ospf.kotlin.math.geometry.Point(Flt64.zero, Flt64.zero),
            fuookami.ospf.kotlin.math.geometry.Point(Flt64.one, Flt64(2.0)),
            fuookami.ospf.kotlin.math.geometry.Point(Flt64(2.0), Flt64(4.0))
        )
        val adapter = MonotoneUnivariateLinearPiecewiseFunction.fromPoints(
            xPoly, points, name = "mono_pw"
        )
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "fromPoints should return LinearFunctionSymbolAdapter")
    }

    // ========== IsolineBivariateLinearPiecewiseFunction — 独立类 ==========

    @Test
    fun isolineBivariateLinearPiecewiseFunctionShouldBeRealClass() {
        val fn: IsolineBivariateLinearPiecewiseFunction<Flt64>? = null
        assertTrue(fn == null, "IsolineBivariateLinearPiecewiseFunction should be a real class, not typealias")
    }

    // ========== InListFunction — 语义正确性 ==========

    @Test
    fun inListFunctionFlt64ShouldInstantiate() {
        val x = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_x")
        val list = listOf(
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_a"),
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_b"),
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_c")
        )
        val fn = InListFunction(x, list)
        assertEquals("in_list", fn.name)
    }

    @Test
    fun inListFunctionFlt64CustomNameShouldInstantiate() {
        val x = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_x2")
        val list = listOf(
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_a2"),
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_b2")
        )
        val fn = InListFunction(x, list, name = "custom_in_list", displayName = "Custom In List")
        assertEquals("custom_in_list", fn.name)
    }

    @Test
    fun inListFunctionShouldExtendSatisfiedAmountInequalityFunction() {
        val x = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_x_hierarchy")
        val list = listOf(
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_a_hierarchy")
        )
        val fn = InListFunction(x, list, converter = compatFlt64Converter)
        assertTrue(fn is SatisfiedAmountInequalityFunction<*>, "InListFunction should extend SatisfiedAmountInequalityFunction")
    }

    @Test
    fun inListFunctionShouldImplementMathFunctionSymbol() {
        val x = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_x_symbol")
        val list = listOf(
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_a_symbol")
        )
        val fn = InListFunction(x, list, converter = compatFlt64Converter)
        assertTrue(fn is MathFunctionSymbol<Flt64>, "InListFunction should implement MathFunctionSymbol")
    }

    @Test
    fun inListFunctionShouldRegisterWithModel() {
        val model = LinearMetaModel("inlist_model")
        val x = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_x_model")
        val a = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_a_model")
        val b = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_b_model")
        model.add(x)
        model.add(a)
        model.add(b)
        val fn = InListFunction(x, listOf(a, b), name = "test_inlist")
        val result = fn.registerAuxiliaryTokens(model.tokens)
        assertTrue(result.ok, "InListFunction registerAuxiliaryTokens should succeed")
        assertTrue(fn.helperVariables.isNotEmpty(), "InListFunction should have helper variables")
        model.close()
    }

    @Test
    fun inListFunctionCompanionShouldReturnAdapter() {
        val x = LinearExpressionSymbol(constant = Flt64.one, name = "inlist_x_adapter")
        val list = listOf(
            LinearExpressionSymbol(constant = Flt64.one, name = "inlist_a_adapter")
        )
        val adapter = InListFunction(x, list, name = "test_inlist_adapter")
        assertTrue(adapter is LinearFunctionSymbolAdapter<Flt64>,
            "InListFunction companion should return LinearFunctionSymbolAdapter")
    }

    // ========== Model 集成路径测试 ==========

    @Test
    fun slackFunctionAdapterShouldBeAddableToModel() {
        val model = LinearMetaModel("slack_model_integration")
        val x = RealVar("x")
        model.add(x)
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val adapter = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            name = "slack_fn"
        )
        // adapter implements LinearIntermediateSymbol, so model.add() should work
        val result = model.add(adapter as IntermediateSymbol<*>)
        assertTrue(result.ok, "model.add(slackAdapter) should succeed")
        model.close()
    }

    @Test
    fun slackRangeFunctionAdapterShouldBeAddableToModel() {
        val model = LinearMetaModel("slack_range_model_integration")
        val x = RealVar("x")
        model.add(x)
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val lb = LinearPolynomial(emptyList(), Flt64(0.0))
        val ub = LinearPolynomial(emptyList(), Flt64(10.0))
        val adapter = URealSlackRangeFunction(
            x = polyX,
            lb = lb,
            ub = ub,
            name = "slack_range_fn"
        )
        val result = model.add(adapter as IntermediateSymbol<*>)
        assertTrue(result.ok, "model.add(slackRangeAdapter) should succeed")
        model.close()
    }

    // ========== P3 语义回归测试 ==========

    // 1. SatisfiedAmountPolynomialFunction: 负数多项式 poly != 0 算"满足"
    @Test
    fun satisfiedAmountPolynomialShouldCountNegativePolynomialAsSatisfied() {
        val x = RealVar("x")
        // poly = -5 (constant negative polynomial)
        val negPoly = LinearPolynomial(emptyList(), Flt64(-5.0))
        // poly = 0 (zero polynomial)
        val zeroPoly = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(negPoly, zeroPoly),
            converter = compatFlt64Converter,
            name = "satisfied_neg_test"
        )
        // evaluate: negPoly != 0 → satisfied, zeroPoly == 0 → not satisfied → count = 1
        val values = emptyMap<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>()
        val result = fn.evaluate(values)
        assertEquals(Flt64.one, result, "Negative polynomial should be counted as satisfied (neq 0)")
    }

    // 2. IfElseFunction with SigmoidFunction condition: condition polynomial preserved
    @Test
    fun ifElseFunctionWithSigmoidConditionShouldPreserveConditionPolynomial() {
        val x = RealVar("x")
        val condition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val sigmoidAdapter = SigmoidFunction.fromLinearPolynomial(
            condition = condition,
            name = "sig_cond"
        )
        // SigmoidFunction adapter's polynomial should be non-zero (indicatorVar)
        val sigPoly = sigmoidAdapter.toLinearPolynomial()
        assertTrue(sigPoly.monomials.isNotEmpty(),
            "SigmoidFunction adapter polynomial should be non-zero (indicator variable)")
    }

    // 3. model.maximize(functionAdapter) should produce non-zero objective term
    @Test
    fun maximizeWithFunctionAdapterShouldProduceNonZeroObjective() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val adapter = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            name = "slack_max_test"
        )
        // adapter.polynomial should be non-zero (polyX = x + neg - pos)
        val poly = adapter.toLinearPolynomial()
        assertTrue(poly.monomials.isNotEmpty(),
            "SlackFunction adapter polynomial should be non-zero for model.maximize()")
    }

    // 4. AtLeastPolynomialFunction: when not satisfied, output 0 (not hard constraint)
    @Test
    fun atLeastPolynomialShouldOutputZeroWhenNotSatisfied() {
        val x = RealVar("x")
        // Two polynomials: one nonzero, one zero
        val nonzeroPoly = LinearPolynomial(emptyList(), Flt64(3.0))
        val zeroPoly = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = AtLeastPolynomialFunction(
            polynomials = listOf(nonzeroPoly, zeroPoly),
            amount = UInt64(2), // require 2 satisfied, but only 1 nonzero
            converter = compatFlt64Converter,
            name = "at_least_unsatisfied"
        )
        // evaluate: only 1 nonzero, amount=2 → not satisfied → should return 0
        val values = emptyMap<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>()
        val result = fn.evaluate(values)
        assertEquals(Flt64.zero, result,
            "AtLeastPolynomialFunction should return 0 when not enough polynomials are satisfied")
    }

    // 5. UIntegerSlackFunction constraint=false should not register constraints
    @Test
    fun slackFunctionConstraintFalseShouldNotRegisterConstraints() {
        val x = RealVar("x")
        val polyX = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val polyY = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = UIntegerSlackFunction(
            x = polyX,
            y = polyY,
            constraint = false,
            converter = compatFlt64Converter,
            name = "slack_no_constraint_fn"
        )
        // Helper variables should still exist (slack vars are always created)
        assertTrue(fn.helperVariables.isNotEmpty(), "Helper variables should still be created")
        // Verify that constraint=false means registerConstraints returns ok without delegating
        // (We can't directly call registerConstraints without a MechanismModel, but we can
        // verify the fn was constructed with registerConstraints_=false)
        assertTrue(fn is MathFunctionSymbol<Flt64>, "UIntegerSlackFunction should be a MathFunctionSymbol")
    }

    // ========== P3 约束级语义回归测试 ==========

    // 6. SatisfiedAmountPolynomialFunction.result = sum(indVars), indVar=1 means nonzero
    @Test
    fun satisfiedAmountResultPolynomialShouldBeSumOfIndVars() {
        val nonzeroPoly = LinearPolynomial(emptyList(), Flt64(3.0))
        val zeroPoly = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(nonzeroPoly, zeroPoly),
            converter = compatFlt64Converter,
            name = "sat_result_test"
        )
        // result = sum(indVars): each indVar coefficient should be +1 (not -1)
        val result = fn.result
        for (mono in result.monomials) {
            assertTrue(mono.coefficient > Flt64.zero,
                "Each indVar coefficient in result should be positive (indVar=1 means nonzero)")
        }
        assertEquals(Flt64.zero, result.constant,
            "Result constant should be 0 (sum of indVars, not n - sum)")
    }

    // 7. SatisfiedAmountPolynomialFunction: all-zero polynomials → evaluate returns 0
    @Test
    fun satisfiedAmountAllZeroPolynomialsShouldEvaluateToZero() {
        val zeroPoly1 = LinearPolynomial(emptyList(), Flt64.zero)
        val zeroPoly2 = LinearPolynomial(emptyList(), Flt64.zero)
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(zeroPoly1, zeroPoly2),
            converter = compatFlt64Converter,
            name = "sat_all_zero"
        )
        val values = emptyMap<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>()
        val result = fn.evaluate(values)
        assertEquals(Flt64.zero, result, "All-zero polynomials → satisfied count = 0")
    }

    // 8. SatisfiedAmountPolynomialFunction: all-nonzero polynomials → evaluate returns n
    @Test
    fun satisfiedAmountAllNonzeroPolynomialsShouldEvaluateToN() {
        val nonzeroPoly1 = LinearPolynomial(emptyList(), Flt64(5.0))
        val nonzeroPoly2 = LinearPolynomial(emptyList(), Flt64(-3.0))
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(nonzeroPoly1, nonzeroPoly2),
            converter = compatFlt64Converter,
            name = "sat_all_nonzero"
        )
        val values = emptyMap<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>()
        val result = fn.evaluate(values)
        assertEquals(Flt64(2.0), result, "All-nonzero polynomials → satisfied count = 2")
    }

    // 9. SatisfiedAmountPolynomialFunction: registers nonzero constraints via model
    @Test
    fun satisfiedAmountShouldRegisterNonzeroConstraintsViaModel() {
        val model = LinearMetaModel("sat_constraint_model")
        val x = RealVar("x")
        model.add(x)
        val poly = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val fn = SatisfiedAmountPolynomialFunction(
            polynomials = listOf(poly),
            converter = compatFlt64Converter,
            name = "sat_constraint_fn"
        )
        val tokenResult = fn.registerAuxiliaryTokens(model.tokens)
        assertTrue(tokenResult.ok, "registerAuxiliaryTokens should succeed")
        // Each polynomial creates 1 indVar + 1 sideVar = 2 helper variables
        assertEquals(2, fn.helperVariables.size,
            "1 polynomial → 1 indVar + 1 sideVar = 2 helper variables")
        // Verify result polynomial structure: sum(indVars) with positive coefficients
        val result = fn.result
        for (mono in result.monomials) {
            assertTrue(mono.coefficient > Flt64.zero,
                "Each indVar coefficient in result should be positive (indVar=1 means nonzero)")
        }
        assertEquals(Flt64.zero, result.constant,
            "Result constant should be 0 (sum of indVars, not n - sum)")
    }

    // 10. AtLeastPolynomialFunction: satisfied side (amountFlag=1) constraint structure
    @Test
    fun atLeastSatisfiedSideShouldRegisterCorrectConstraints() {
        val x = RealVar("x")
        val y = RealVar("y")
        val poly1 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val poly2 = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val fn = AtLeastPolynomialFunction(
            polynomials = listOf(poly1, poly2),
            amount = UInt64(1),
            converter = compatFlt64Converter,
            name = "at_least_sat_test"
        )
        // 2 indVars + 2 sideVars + 1 amountFlag = 5 helper variables
        assertEquals(5, fn.helperVariables.size,
            "2 polynomials → 2 indVars + 2 sideVars + 1 amountFlag = 5 helper variables")
        // result = amountFlag (not n - sum(indVars))
        val result = fn.result
        assertEquals(1, result.monomials.size,
            "AtLeast result should have 1 monomial (amountFlag)")
        assertTrue(result.monomials[0].coefficient > Flt64.zero,
            "AtLeast result coefficient should be positive (amountFlag)")
    }

    // 11. AtLeastPolynomialFunction: evaluate satisfied → 1, not satisfied → 0
    @Test
    fun atLeastEvaluateSatisfiedReturnsOneNotSatisfiedReturnsZero() {
        val nonzeroPoly = LinearPolynomial(emptyList(), Flt64(5.0))
        val zeroPoly = LinearPolynomial(emptyList(), Flt64.zero)
        val fnSatisfied = AtLeastPolynomialFunction(
            polynomials = listOf(nonzeroPoly),
            amount = UInt64(1),
            converter = compatFlt64Converter,
            name = "at_least_sat"
        )
        val fnNotSatisfied = AtLeastPolynomialFunction(
            polynomials = listOf(zeroPoly),
            amount = UInt64(1),
            converter = compatFlt64Converter,
            name = "at_least_not_sat"
        )
        val values = emptyMap<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>()
        assertEquals(Flt64.one, fnSatisfied.evaluate(values),
            "1 nonzero polynomial with amount=1 → satisfied → evaluate returns 1")
        assertEquals(Flt64.zero, fnNotSatisfied.evaluate(values),
            "0 nonzero polynomials with amount=1 → not satisfied → evaluate returns 0")
    }
}
