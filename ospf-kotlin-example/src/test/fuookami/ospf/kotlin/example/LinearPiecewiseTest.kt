package fuookami.ospf.kotlin.example

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*

class LinearPiecewiseTest {
    @Test
    fun abs() {
        val x = RealVar("x")
        x.range.leq(Flt64.two)
        x.range.geq(-Flt64.two)
        val abs = AbsFunction(LinearPolynomial(x), name = "abs")
        val solver = SCIPLinearSolver(LinearSolverConfig())

        val metaModel1 = LinearMetaModel()
        metaModel1.addVar(x)
        metaModel1.addSymbol(abs)
        metaModel1.minimize(abs)
        val model1 = runBlocking { LinearModel(metaModel1).value!! }
        val result1 = runBlocking { solver(LinearTriadModel(model1)) }
        assert(result1.value!!.obj eq Flt64.zero)

        val metaModel2 = LinearMetaModel()
        metaModel2.addVar(x)
        metaModel2.addSymbol(abs)
        metaModel2.maximize(abs)
        val model2 = runBlocking { LinearModel(metaModel2).value!! }
        val result2 = runBlocking { solver(LinearTriadModel(model2)) }
        assert(result2.value!!.obj eq Flt64.two)

        val metaModel3 = LinearMetaModel()
        metaModel3.addVar(x)
        metaModel3.addSymbol(abs)
        metaModel3.addConstraint(x geq Flt64.one)
        metaModel3.minimize(abs)
        val model3 = runBlocking { LinearModel(metaModel3).value!! }
        val result3 = runBlocking { solver(LinearTriadModel(model3)) }
        assert((result3.value!!.obj - Flt64.one).toFlt32() eq Flt32.zero)

        val metaModel4 = LinearMetaModel()
        metaModel4.addVar(x)
        metaModel4.addSymbol(abs)
        metaModel4.addConstraint(x geq Flt64.one)
        metaModel4.maximize(abs)
        val model4 = runBlocking { LinearModel(metaModel4).value!! }
        val result4 = runBlocking { solver(LinearTriadModel(model4)) }
        assert(result4.value!!.obj eq Flt64.two)

        val metaModel5 = LinearMetaModel()
        metaModel5.addVar(x)
        metaModel5.addSymbol(abs)
        metaModel5.addConstraint(x leq -Flt64.one)
        metaModel5.minimize(abs)
        val model5 = runBlocking { LinearModel(metaModel5).value!! }
        val result5 = runBlocking { solver(LinearTriadModel(model5)) }
        assert((result5.value!!.obj - Flt64.one).toFlt32() eq Flt32.zero)

        val metaModel6 = LinearMetaModel()
        metaModel6.addVar(x)
        metaModel6.addSymbol(abs)
        metaModel6.addConstraint(x leq -Flt64.one)
        metaModel6.maximize(abs)
        val model6 = runBlocking { LinearModel(metaModel6).value!! }
        val result6 = runBlocking { solver(LinearTriadModel(model6)) }
        assert(result6.value!!.obj eq Flt64.two)

        val metaModel7 = LinearMetaModel()
        metaModel7.addVar(x)
        metaModel7.addSymbol(abs)
        metaModel7.addConstraint(abs eq Flt64.one)
        metaModel7.maximize(x)
        val model7 = runBlocking { LinearModel(metaModel7).value!! }
        val result7 = runBlocking { solver(LinearTriadModel(model7)) }
        assert((result7.value!!.obj - Flt64.one).toFlt32() eq Flt32.zero)

        val metaModel8 = LinearMetaModel()
        metaModel8.addVar(x)
        metaModel8.addSymbol(abs)
        metaModel8.addConstraint(abs eq Flt64.one)
        metaModel8.minimize(x)
        val model8 = runBlocking { LinearModel(metaModel8).value!! }
        val result8 = runBlocking { solver(LinearTriadModel(model8)) }
        assert((result8.value!!.obj + Flt64.one).toFlt32() eq Flt32.zero)

        val metaModel9 = LinearMetaModel()
        metaModel9.addVar(x)
        metaModel9.addSymbol(abs)
        metaModel9.addConstraint(abs geq Flt64.one)
        metaModel9.maximize(x)
        val model9 = runBlocking { LinearModel(metaModel9).value!! }
        val result9 = runBlocking { solver(LinearTriadModel(model9)) }
        assert(result9.value!!.obj eq Flt64.two)

        val metaModel10 = LinearMetaModel()
        metaModel10.addVar(x)
        metaModel10.addSymbol(abs)
        metaModel10.addConstraint(abs geq Flt64.one)
        metaModel10.minimize(x)
        val model10 = runBlocking { LinearModel(metaModel10).value!! }
        val result10 = runBlocking { solver(LinearTriadModel(model10)) }
        assert(result10.value!!.obj eq -Flt64.two)
    }

    @Test
    fun and1() {
        val x = UIntVar("x")
        x.range.leq(UInt64.one)
        val y = UIntVar("y")
        y.range.leq(UInt64.two)
        val and = AndFunction(listOf(LinearPolynomial(x), LinearPolynomial(y)), "and")
        val solver = SCIPLinearSolver(LinearSolverConfig())

        val metaModel1 = LinearMetaModel()
        metaModel1.addVar(x)
        metaModel1.addVar(y)
        metaModel1.addSymbol(and)
        metaModel1.addConstraint(and)
        metaModel1.maximize(x + y)
        val model1 = runBlocking { LinearModel(metaModel1).value!! }
        val result1 = runBlocking { solver(LinearTriadModel(model1)) }
        assert(result1.value!!.obj eq Flt64.three)

        val metaModel2 = LinearMetaModel()
        metaModel2.addVar(x)
        metaModel2.addVar(y)
        metaModel2.addSymbol(and)
        metaModel2.addConstraint(and)
        metaModel2.minimize(x + y)
        val model2 = runBlocking { LinearModel(metaModel2).value!! }
        val result2 = runBlocking { solver(LinearTriadModel(model2)) }
        assert(result2.value!!.obj eq Flt64.two)
    }

    @Test
    fun bin1() {
        val x = URealVar("x")
        x.range.leq(Flt64.two)
        val bin = BinaryzationFunction(LinearPolynomial(x), name = "bin")
        val solver = SCIPLinearSolver(LinearSolverConfig())

        val metaModel1 = LinearMetaModel()
        metaModel1.addVar(x)
        metaModel1.addSymbol(bin)
        metaModel1.minimize(bin)
        val model1 = runBlocking { LinearModel(metaModel1).value!! }
        val result1 = runBlocking { solver(LinearTriadModel(model1)) }
        assert(result1.value!!.obj eq Flt64.zero)

        val metaModel2 = LinearMetaModel()
        metaModel2.addVar(x)
        metaModel2.addSymbol(bin)
        metaModel2.maximize(bin)
        val model2 = runBlocking { LinearModel(metaModel2).value!! }
        val result2 = runBlocking { solver(LinearTriadModel(model2)) }
        assert(result2.value!!.obj eq Flt64.one)

        val metaModel3 = LinearMetaModel()
        metaModel3.addVar(x)
        metaModel3.addSymbol(bin)
        metaModel3.addConstraint(x eq Flt64.zero)
        metaModel3.maximize(bin)
        val model3 = runBlocking { LinearModel(metaModel3).value!! }
        val result3 = runBlocking { solver(LinearTriadModel(model3)) }
        assert(result3.value!!.obj eq Flt64.zero)

        val metaModel4 = LinearMetaModel()
        metaModel4.addVar(x)
        metaModel4.addSymbol(bin)
        metaModel4.addConstraint(x eq Flt64(0.3))
        metaModel4.maximize(bin)
        val model4 = runBlocking { LinearModel(metaModel4).value!! }
        val result4 = runBlocking { solver(LinearTriadModel(model4)) }
        assert(result4.value!!.obj eq Flt64.one)
    }

    @Test
    fun bin2() {
        val x = UIntVar("x")
        x.range.leq(UInt64.two)
        val bin = BinaryzationFunction(LinearPolynomial(x), name = "bin")
        val solver = SCIPLinearSolver(LinearSolverConfig())

        val metaModel1 = LinearMetaModel()
        metaModel1.addVar(x)
        metaModel1.addSymbol(bin)
        metaModel1.minimize(bin)
        val model1 = runBlocking { LinearModel(metaModel1).value!! }
        val result1 = runBlocking { solver(LinearTriadModel(model1)) }
        assert(result1.value!!.obj eq Flt64.zero)

        val metaModel2 = LinearMetaModel()
        metaModel2.addVar(x)
        metaModel2.addSymbol(bin)
        metaModel2.maximize(bin)
        val model2 = runBlocking { LinearModel(metaModel2).value!! }
        val result2 = runBlocking { solver(LinearTriadModel(model2)) }
        assert(result2.value!!.obj eq Flt64.one)

        val metaModel3 = LinearMetaModel()
        metaModel3.addVar(x)
        metaModel3.addSymbol(bin)
        metaModel3.addConstraint(x eq Flt64.zero)
        metaModel3.maximize(bin)
        val model3 = runBlocking { LinearModel(metaModel3).value!! }
        val result3 = runBlocking { solver(LinearTriadModel(model3)) }
        assert(result3.value!!.obj eq Flt64.zero)

        val metaModel4 = LinearMetaModel()
        metaModel4.addVar(x)
        metaModel4.addSymbol(bin)
        metaModel4.addConstraint(x eq Flt64.zero)
        metaModel4.minimize(bin)
        val model4 = runBlocking { LinearModel(metaModel4).value!! }
        val result4 = runBlocking { solver(LinearTriadModel(model4)) }
        assert(result4.value!!.obj eq Flt64.zero)
    }

    @Test
    fun bter1() {
        val x = RealVar("x")
        x.range.leq(Flt64.two)
        x.range.geq(-Flt64.two)
        val bter = BalanceTernaryzationFunction(LinearPolynomial(x), name = "bter")
        val solver = SCIPLinearSolver(LinearSolverConfig())

        val metaModel1 = LinearMetaModel()
        metaModel1.addVar(x)
        metaModel1.addSymbol(bter)
        metaModel1.minimize(bter)
        val model1 = runBlocking { LinearModel(metaModel1).value!! }
        val result1 = runBlocking { solver(LinearTriadModel(model1)) }
        assert(result1.value!!.obj eq -Flt64.one)

        val metaModel2 = LinearMetaModel()
        metaModel2.addVar(x)
        metaModel2.addSymbol(bter)
        metaModel2.maximize(bter)
        val model2 = runBlocking { LinearModel(metaModel2).value!! }
        val result2 = runBlocking { solver(LinearTriadModel(model2)) }
        assert(result2.value!!.obj eq Flt64.one)

        val metaModel3 = LinearMetaModel()
        metaModel3.addVar(x)
        metaModel3.addSymbol(bter)
        metaModel3.addConstraint(x geq Flt64.zero)
        metaModel3.minimize(bter)
        val model3 = runBlocking { LinearModel(metaModel3).value!! }
        val result3 = runBlocking { solver(LinearTriadModel(model3)) }
        assert(result3.value!!.obj eq Flt64.zero)

        val metaModel4 = LinearMetaModel()
        metaModel4.addVar(x)
        metaModel4.addSymbol(bter)
        metaModel4.addConstraint(x geq Flt64.zero)
        metaModel4.maximize(bter)
        val model4 = runBlocking { LinearModel(metaModel4).value!! }
        val result4 = runBlocking { solver(LinearTriadModel(model4)) }
        assert(result4.value!!.obj eq Flt64.one)

        val metaModel5 = LinearMetaModel()
        metaModel5.addVar(x)
        metaModel5.addSymbol(bter)
        metaModel5.addConstraint(x leq Flt64.zero)
        metaModel5.minimize(bter)
        val model5 = runBlocking { LinearModel(metaModel5).value!! }
        val result5 = runBlocking { solver(LinearTriadModel(model5)) }
        assert(result5.value!!.obj eq -Flt64.one)

        val metaModel6 = LinearMetaModel()
        metaModel6.addVar(x)
        metaModel6.addSymbol(bter)
        metaModel6.addConstraint(x leq Flt64.zero)
        metaModel6.maximize(bter)
        val model6 = runBlocking { LinearModel(metaModel6).value!! }
        val result6 = runBlocking { solver(LinearTriadModel(model6)) }
        assert(result6.value!!.obj eq Flt64.zero)

        val metaModel7 = LinearMetaModel()
        metaModel7.addVar(x)
        metaModel7.addSymbol(bter)
        metaModel7.addConstraint(x leq Flt64(0.3))
        metaModel7.maximize(bter)
        val model7 = runBlocking { LinearModel(metaModel7).value!! }
        val result7 = runBlocking { solver(LinearTriadModel(model7)) }
        assert(result7.value!!.obj eq Flt64.one)

        val metaModel8 = LinearMetaModel()
        metaModel8.addVar(x)
        metaModel8.addSymbol(bter)
        metaModel8.addConstraint(x geq -Flt64(0.3))
        metaModel8.minimize(bter)
        val model8 = runBlocking { LinearModel(metaModel8).value!! }
        val result8 = runBlocking { solver(LinearTriadModel(model8)) }
        assert(result8.value!!.obj eq -Flt64.one)
    }

    @Test
    fun bter2() {
        val x = IntVar("x")
        x.range.leq(Int64.two)
        x.range.geq(-Int64.two)
        val bter = BalanceTernaryzationFunction(LinearPolynomial(x), name = "bter")
        val solver = SCIPLinearSolver(LinearSolverConfig())

        val metaModel1 = LinearMetaModel()
        metaModel1.addVar(x)
        metaModel1.addSymbol(bter)
        metaModel1.minimize(bter)
        val model1 = runBlocking { LinearModel(metaModel1).value!! }
        val result1 = runBlocking { solver(LinearTriadModel(model1)) }
        assert(result1.value!!.obj eq -Flt64.one)

        val metaModel2 = LinearMetaModel()
        metaModel2.addVar(x)
        metaModel2.addSymbol(bter)
        metaModel2.maximize(bter)
        val model2 = runBlocking { LinearModel(metaModel2).value!! }
        val result2 = runBlocking { solver(LinearTriadModel(model2)) }
        assert(result2.value!!.obj eq Flt64.one)

        val metaModel3 = LinearMetaModel()
        metaModel3.addVar(x)
        metaModel3.addSymbol(bter)
        metaModel3.addConstraint(x geq Flt64.zero)
        metaModel3.minimize(bter)
        val model3 = runBlocking { LinearModel(metaModel3).value!! }
        val result3 = runBlocking { solver(LinearTriadModel(model3)) }
        assert(result3.value!!.obj eq Flt64.zero)

        val metaModel4 = LinearMetaModel()
        metaModel4.addVar(x)
        metaModel4.addSymbol(bter)
        metaModel4.addConstraint(x geq Flt64.zero)
        metaModel4.maximize(bter)
        val model4 = runBlocking { LinearModel(metaModel4).value!! }
        val result4 = runBlocking { solver(LinearTriadModel(model4)) }
        assert(result4.value!!.obj eq Flt64.one)

        val metaModel5 = LinearMetaModel()
        metaModel5.addVar(x)
        metaModel5.addSymbol(bter)
        metaModel5.addConstraint(x leq Flt64.zero)
        metaModel5.minimize(bter)
        val model5 = runBlocking { LinearModel(metaModel5).value!! }
        val result5 = runBlocking { solver(LinearTriadModel(model5)) }
        assert(result5.value!!.obj eq -Flt64.one)

        val metaModel6 = LinearMetaModel()
        metaModel6.addVar(x)
        metaModel6.addSymbol(bter)
        metaModel6.addConstraint(x leq Flt64.zero)
        metaModel6.maximize(bter)
        val model6 = runBlocking { LinearModel(metaModel6).value!! }
        val result6 = runBlocking { solver(LinearTriadModel(model6)) }
        assert(result6.value!!.obj eq Flt64.zero)
    }

    @Test
    fun semi() {
        val metaModel = LinearMetaModel()

        val x = URealVar("x")
        x.range.leq(Flt64.two)
        metaModel.addVar(x)

        val y = URealVar("y")
        y.range.geq(Flt64.three)
        metaModel.addVar(y)

        val semi = SemiURealFunction(LinearPolynomial(x - y), name = "semi")
        metaModel.addSymbol(semi)

        metaModel.minimize(semi)

        val solver = SCIPLinearSolver(LinearSolverConfig())
        val model = runBlocking { LinearTriadModel(LinearModel(metaModel).value!!) }
        val result = runBlocking { solver(model) }
        assert(result.value!!.obj eq Flt64.zero)
    }

    @Test
    fun univariate() {
        val metaModel = LinearMetaModel()

        val x = URealVar("x")
        x.range.leq(Flt64.two)
        metaModel.addVar(x)

        val ulp = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(x),
            points = listOf(
                point2(),
                point2(x = Flt64.one, y = Flt64.two),
                point2(x = Flt64.two, y = Flt64.one)
            ),
            name = "y"
        )
        metaModel.addSymbol(ulp)

        metaModel.maximize(LinearPolynomial(ulp))

        val solver = SCIPLinearSolver(LinearSolverConfig())
        val model = runBlocking { LinearTriadModel(LinearModel(metaModel).value!!) }
        val result = runBlocking { solver(model) }
        assert(result.value!!.solution[0] eq Flt64.one)
    }

    @Test
    fun bivariate() {
        val metaModel = LinearMetaModel()

        val x = URealVar("x")
        val y = URealVar("y")
        x.range.leq(Flt64.two)
        y.range.leq(Flt64.two)
        metaModel.addVar(x)
        metaModel.addVar(y)

        val blp = BivariateLinearPiecewiseFunction(
            x = LinearPolynomial(x),
            y = LinearPolynomial(y),
            points = listOf(
                point3(),
                point3(x = Flt64.two),
                point3(y = Flt64.two),
                point3(x = Flt64.two, y = Flt64.two),
                point3(x = Flt64.one, y = Flt64.one, z = Flt64.one)
            ),
            name = "z"
        )
        metaModel.addSymbol(blp)

        metaModel.maximize(LinearPolynomial(blp))

        val solver = SCIPLinearSolver(LinearSolverConfig())
        val model = runBlocking { LinearTriadModel(LinearModel(metaModel).value!!) }
        val result = runBlocking { solver(model) }
        assert(result.value!!.solution[0] eq Flt64.one)
        assert(result.value!!.solution[1] eq Flt64.one)
    }
}
