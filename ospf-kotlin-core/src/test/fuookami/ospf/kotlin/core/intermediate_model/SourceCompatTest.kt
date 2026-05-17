package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols
import fuookami.ospf.kotlin.core.intermediate_symbol.SymbolCombination
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.le
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.ge
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.eq
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.sum
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.sumVars
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.plus
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Source-level compatibility tests for P1/P4/P5 stable API surface.
 * These tests verify that the public API patterns compile and produce
 * correct types — they guard against accidental signature changes.
 */
class SourceCompatTest {

    // ========== P1: MetaModel default companion factories ==========

    @Test
    fun linearMetaModelDefaultCompanionShouldReturnFlt64Model() {
        val model: LinearMetaModel<Flt64> = LinearMetaModel("p1_default")
        try {
            assertTrue(model.converter.intoValue(Flt64(1.0)) == Flt64(1.0),
                "Default LinearMetaModel converter should be Flt64 identity")
        } finally {
            model.close()
        }
    }

    @Test
    fun linearMetaModelBridgeCompanionShouldReturnTypedModel() {
        val fltXModel: LinearMetaModel<FltX> = LinearMetaModel("p1_fltx", FltX)
        val rtn64Model: LinearMetaModel<Rtn64> = LinearMetaModel("p1_rtn64", Rtn64)
        val rtnXModel: LinearMetaModel<RtnX> = LinearMetaModel("p1_rtnx", RtnX)
        try {
            assertEquals(Flt64(1.0), fltXModel.converter.fromValue(fltXModel.converter.intoValue(Flt64(1.0))),
                "FltX-bridged model converter round-trip should preserve value")
            assertEquals(Flt64(1.0), rtn64Model.converter.fromValue(rtn64Model.converter.intoValue(Flt64(1.0))),
                "Rtn64-bridged model converter round-trip should preserve value")
            assertEquals(Flt64(1.0), rtnXModel.converter.fromValue(rtnXModel.converter.intoValue(Flt64(1.0))),
                "RtnX-bridged model converter round-trip should preserve value")
        } finally {
            fltXModel.close()
            rtn64Model.close()
            rtnXModel.close()
        }
    }

    @Test
    fun quadraticMetaModelDefaultCompanionShouldReturnFlt64Model() {
        val model: QuadraticMetaModel<Flt64> = QuadraticMetaModel("p1_quad_default")
        try {
            assertTrue(model.converter.intoValue(Flt64(1.0)) == Flt64(1.0),
                "Default QuadraticMetaModel converter should be Flt64 identity")
        } finally {
            model.close()
        }
    }

    @Test
    fun quadraticMetaModelBridgeCompanionShouldReturnTypedModel() {
        val fltXModel: QuadraticMetaModel<FltX> = QuadraticMetaModel("p1_quad_fltx", FltX)
        val rtn64Model: QuadraticMetaModel<Rtn64> = QuadraticMetaModel("p1_quad_rtn64", Rtn64)
        val rtnXModel: QuadraticMetaModel<RtnX> = QuadraticMetaModel("p1_quad_rtnx", RtnX)
        try {
            assertEquals(Flt64(1.0), fltXModel.converter.fromValue(fltXModel.converter.intoValue(Flt64(1.0))),
                "FltX-bridged quadratic model converter round-trip should preserve value")
            assertEquals(Flt64(1.0), rtn64Model.converter.fromValue(rtn64Model.converter.intoValue(Flt64(1.0))),
                "Rtn64-bridged quadratic model converter round-trip should preserve value")
            assertEquals(Flt64(1.0), rtnXModel.converter.fromValue(rtnXModel.converter.intoValue(Flt64(1.0))),
                "RtnX-bridged quadratic model converter round-trip should preserve value")
        } finally {
            fltXModel.close()
            rtn64Model.close()
            rtnXModel.close()
        }
    }

    // ========== P4: Aggregation DSL ==========

    @Test
    fun sumShouldProduceLinearPolynomialFromSymbols() {
        val x = RealVar("sum_x")
        val y = RealVar("sum_y")
        val poly: LinearPolynomial<Flt64> = sum(listOf(x, y))
        assertTrue(poly.monomials.size == 2,
            "sum of two symbols should produce polynomial with 2 monomials")
    }

    @Test
    fun sumWithSelectorShouldProduceLinearPolynomial() {
        val items = listOf(RealVar("sel_0"), RealVar("sel_1"), RealVar("sel_2"))
        val poly: LinearPolynomial<Flt64> = sumVars(items) { it }
        assertTrue(poly.monomials.size == 3,
            "sumVars with selector should produce polynomial with 3 monomials")
    }

    // ========== P4: Comparison aliases ==========

    @Test
    fun symbolLeFlt64ShouldProduceLinearInequality() {
        val x = RealVar("le_x")
        val ineq: LinearInequality<Flt64> = x le Flt64(10.0)
        assertTrue(ineq.lhs.monomials.isNotEmpty(),
            "x le 10.0 should produce non-empty inequality")
    }

    @Test
    fun symbolGeFlt64ShouldProduceLinearInequality() {
        val x = RealVar("ge_x")
        val ineq: LinearInequality<Flt64> = x ge Flt64(0.0)
        assertTrue(ineq.lhs.monomials.isNotEmpty(),
            "x ge 0.0 should produce non-empty inequality")
    }

    @Test
    fun symbolEqFlt64ShouldProduceLinearInequality() {
        val x = RealVar("eq_x")
        val ineq: LinearInequality<Flt64> = x eq Flt64(5.0)
        assertTrue(ineq.lhs.monomials.isNotEmpty(),
            "x eq 5.0 should produce non-empty inequality")
    }

    // ========== P5: Variable creation (MultiArray deferred init) ==========

    @Test
    fun binVariable1ShouldConstructWithShape1() {
        val bins = BinVariable1("bins", Shape1(5))
        assertTrue(bins.size == 5,
            "BinVariable1 with Shape1(5) should have 5 elements")
    }

    @Test
    fun intVarShouldConstructWithDefaults() {
        val x = IntVar("int_x")
        assertTrue(x.name == "int_x",
            "IntVar should preserve name")
    }

    // ========== P5: Symbol creation ==========

    @Test
    fun linearExpressionSymbolFromVariableShouldCompile() {
        val x = RealVar("sym_x")
        val symbol = LinearExpressionSymbol(x, name = "expr_x")
        assertTrue(symbol.name == "expr_x",
            "LinearExpressionSymbol from variable should preserve name")
    }

    @Test
    fun linearExpressionSymbolFromPolynomialShouldCompile() {
        val x = RealVar("poly_x")
        val poly = sum(listOf(x))
        val symbol = LinearExpressionSymbol(poly, name = "poly_expr")
        assertTrue(symbol.name == "poly_expr",
            "LinearExpressionSymbol from polynomial should preserve name")
    }

    @Test
    fun linearIntermediateSymbols1ShouldConstructViaFactory() {
        val symbols: LinearIntermediateSymbols1<Flt64> = LinearIntermediateSymbols("syms", Shape1(3))
        assertTrue(symbols.size == 3,
            "LinearIntermediateSymbols with Shape1(3) should have 3 elements")
    }

    @Test
    fun linearIntermediateSymbols1ShouldConstructWithCtor() {
        val x = RealVar("ctor_x")
        val symbols: LinearIntermediateSymbols1<Flt64> = SymbolCombination("ctor_syms", Shape1(2)) { i, _ ->
            LinearExpressionSymbol(x, name = "sym_$i")
        }
        assertTrue(symbols.size == 2,
            "SymbolCombination with Shape1(2) should have 2 elements")
    }

    // ========== P5: Function symbol creation ==========

    @Test
    fun slackFunctionFromVariableItemShouldReturnAdapter() {
        val x = IntVar("slack_x")
        val adapter = SlackFunction(
            x = x,
            y = Flt64(5.0),
            type = UInteger,
            name = "slack_fn"
        )
        assertEquals("slack_fn", adapter.name,
            "SlackFunction companion with variable item should preserve name")
        assertTrue(adapter.helperVariables.isNotEmpty(),
            "SlackFunction adapter should have helper variables")
    }

    @Test
    fun slackFunctionFromIntermediateSymbolShouldReturnAdapter() {
        val x = IntVar("slack_sym_x")
        val expr = LinearExpressionSymbol(x, name = "slack_expr")
        val adapter = SlackFunction(
            x = expr,
            y = Flt64(3.0),
            type = UInteger,
            name = "slack_sym_fn"
        )
        assertEquals("slack_sym_fn", adapter.name,
            "SlackFunction companion with intermediate symbol should preserve name")
        assertTrue(adapter.helperVariables.isNotEmpty(),
            "SlackFunction adapter should have helper variables")
    }

    // ========== P5: Modeling chain ==========

    @Test
    fun modelAddVariableShouldSucceed() {
        val model = LinearMetaModel("chain_add_var")
        val x = RealVar("chain_x")
        try {
            val result = model.add(x)
            assertTrue(result is Ok, "model.add(variable) should return Ok")
        } finally {
            model.close()
        }
    }

    @Test
    fun modelAddSymbolShouldSucceed() {
        val model = LinearMetaModel("chain_add_sym")
        val x = RealVar("chain_sym_x")
        val symbol = LinearExpressionSymbol(x, name = "chain_obj")
        try {
            model.add(x)
            val result = model.add(symbol)
            assertTrue(result is Ok, "model.add(symbol) should return Ok")
        } finally {
            model.close()
        }
    }

    @Test
    fun modelMinimizeSymbolShouldSucceed() {
        val model = LinearMetaModel("chain_minimize")
        val x = RealVar("min_x")
        val symbol = LinearExpressionSymbol(x, name = "min_obj")
        try {
            model.add(x)
            val result = model.minimize(symbol = symbol, name = "min_x_obj")
            assertTrue(result is Ok, "model.minimize(symbol) should return Ok")
        } finally {
            model.close()
        }
    }

    @Test
    fun modelMaximizeSymbolShouldSucceed() {
        val model = LinearMetaModel("chain_maximize")
        val x = RealVar("max_x")
        val symbol = LinearExpressionSymbol(x, name = "max_obj")
        try {
            model.add(x)
            val result = model.maximize(symbol = symbol, name = "max_x_obj")
            assertTrue(result is Ok, "model.maximize(symbol) should return Ok")
        } finally {
            model.close()
        }
    }

    // ========== P5: Full Demo-like modeling chain ==========

    @Test
    fun demo9LikeModelingChainShouldCompile() {
        val model = LinearMetaModel("demo9_compat")
        val x = IntVar("x")
        val y = IntVar("y")
        val result1 = model.add(x)
        assertTrue(result1 is Ok, "model.add(x) should succeed")
        val result2 = model.add(y)
        assertTrue(result2 is Ok, "model.add(y) should succeed")

        val settlements = listOf(
            Flt64(9.0), Flt64(2.0), Flt64(3.0)
        )
        val dx: LinearIntermediateSymbols1<Flt64> = SymbolCombination("dx", Shape1(settlements.size)) { i, _ ->
            SlackFunction(
                type = UInteger,
                x = x,
                y = settlements[i],
                name = "dx_$i"
            )
        }
        val result3 = model.add(dx)
        assertTrue(result3 is Ok, "model.add(dx) should succeed")

        val distance: LinearIntermediateSymbols1<Flt64> = SymbolCombination("dist", Shape1(settlements.size)) { i, _ ->
            LinearExpressionSymbol(
                dx[i],
                name = "distance_$i"
            )
        }
        val result4 = model.add(distance)
        assertTrue(result4 is Ok, "model.add(distance) should succeed")

        // P10: intermediate symbol constraint — previously blocked by ClassCast
        val constraintResult = model.addConstraint(distance[0] le Flt64(10.0), name = "dist0_limit")
        assertTrue(constraintResult is Ok, "addConstraint with intermediate symbol should succeed")

        model.close()
    }

    @Test
    fun addConstraintWithLinearExpressionSymbolShouldSucceed() {
        val model = LinearMetaModel("constraint_compat")
        val x = RealVar("x")
        val y = RealVar("y")
        try {
            model.add(x)
            model.add(y)

            val symbol = LinearExpressionSymbol<Flt64>(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64(3.0), x), LinearMonomial(Flt64(2.0), y)),
                    constant = Flt64(1.0)
                ),
                name = "expr"
            )
            model.add(symbol)

            val result = model.addConstraint(symbol le Flt64(10.0), name = "expr_limit")
            assertTrue(result is Ok, "addConstraint(symbol le Flt64) should succeed after P10 fix")
        } finally {
            model.close()
        }
    }
}
