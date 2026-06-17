package fuookami.ospf.kotlin.example.heuristic_demo

import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.*
import fuookami.ospf.kotlin.core.solver.heuristic.pso.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/** 用于 [LinearMetaModel] 中 [IntoValue] 的 Flt64 恒等转换器。Flt64 identity converter for use with [IntoValue] in [LinearMetaModel]. */
private val flt64Converter = object : IntoValue<Flt64> {
    /** @param value Flt64 值。 */
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/** 演示基于 PSO 的启发式优化在通过 [LinearMetaModel] 构建的线性模型上的应用。Demonstrates PSO-based heuristic optimization on a linear model built via [LinearMetaModel]. */
class Demo2 {
    /**
     * Builds a linear model maximizing x + y (subject to bounds), then solves it with PSO.
 *
     * @return 返回结果。
     */
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
        val obj = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
        obj += LinearMonomial(Flt64.one, x)
        obj += LinearMonomial(Flt64.one, y)
        metaModel.maximize(LinearPolynomial(obj.monomials, obj.constant))
        val model = when (val result = runBlocking { LinearMechanismModel(metaModel) }) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                result.value ?: return Failed(Err(ErrorCode.ApplicationError, "linear mechanism model is null"))
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }
//        val solver = ScipLinearSolver()
//        val result = when (val ret = runBlocking { solver(model) }) {
//            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
//                metaModel.tokens.setSolution(ret.value.results)
//                ret.value.results
//            }
//
//            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
//                return ret
//            }
//        }
        val callBackModel = CallBackModel(model, converter = IntoValue.Identity)
        val solver = PSO(policy = PSOPolicy(timeLimit = 10.seconds))
        val result = solver(callBackModel)
        return ok
    }
}
