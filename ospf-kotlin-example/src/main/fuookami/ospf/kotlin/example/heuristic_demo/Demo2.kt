package fuookami.ospf.kotlin.example.heuristic_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.config.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.heuristic.pso.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class Demo2 {
    operator fun invoke(): Try {
        val metaModel = LinearMetaModel<Flt64>(converter = flt64Converter)
        val x = URealVar("x")
        val y = URealVar("y")
        x.range.leq(Flt64.two)
        y.range.leq(Flt64.two)
        metaModel.add(x)
        metaModel.add(y)
//        val abs = AbsFunction(x - Flt64.one)
//        metaModel.add(abs)
//        metaModel.addObject(ObjectCategory.Minimum, LinearPolynomial(abs))
        val obj = MutableLinearPolynomial()
        obj += LinearMonomial(Flt64.one, x)
        obj += LinearMonomial(Flt64.one, y)
        metaModel.maximize(LinearPolynomial(obj.monomials, obj.constant))
        val model = when (val result = runBlocking { LinearMechanismModel(metaModel) }) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
//        val solver = ScipLinearSolver()
//        val result = when (val ret = runBlocking { solver(model) }) {
//            is Ok -> {
//                metaModel.tokens.setSolution(ret.value.results)
//                ret.value.results
//            }
//
//            is Failed -> {
//                return ret
//            }
//        }
        val callBackModel = CallBackModel(model)
        val solver = PSO(policy = PSOPolicy(timeLimit = 10.seconds))
        val result = solver(callBackModel)
        return ok
    }
}












