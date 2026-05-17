package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.SemiFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.minus
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.core_demo.ScipAvailability
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class SemiTest {
    @Test
    fun semi() {
        assumeTrue(ScipAvailability.isAvailable(), "SCIP runtime not available in current environment")

        val x = URealVar("x")
        x.range.leq(Flt64.three)
        val y = URealVar("y")
        y.range.geq(Flt64.two)
        y.range.leq(Flt64.five)

        val px = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val py = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val semi = SemiFunction(px - py, name = "semi")
        val solver = ScipLinearSolver()

        val model1 = LinearMetaModel<Flt64>(converter = flt64Converter)
        model1.add(x)
        model1.add(y)
        semi.register(model1)
        model1.minimize(semi.y)
        val result1 = runBlocking { solver(model1) }
        assert(result1.value!!.obj eq Flt64.zero)
        assert(result1.value!!.solution[1] geq result1.value!!.solution[0])

        val model2 = LinearMetaModel<Flt64>(converter = flt64Converter)
        model2.add(x)
        model2.add(y)
        semi.register(model2)
        model2.maximize(semi.y)
        val result2 = runBlocking { solver(model2) }
        assert(result2.value!!.obj eq Flt64.one)
    }

    }
