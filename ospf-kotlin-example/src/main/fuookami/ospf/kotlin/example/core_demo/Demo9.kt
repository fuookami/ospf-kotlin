package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.example.exampleAbsoluteSlack
import fuookami.ospf.kotlin.example.flt64Constant
import fuookami.ospf.kotlin.example.flt64Linear
import fuookami.ospf.kotlin.example.solveLinearMetaModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * @see     https://fuookami.github.io/ospf/examples/example9.html
 */
data object Demo9 {
    data class Settlement(
        val x: Flt64,
        val y: Flt64
    ) : AutoIndexed(Settlement::class)

    private val settlements = listOf(
        Settlement(Flt64(9.0), Flt64(2.0)),
        Settlement(Flt64(2.0), Flt64(1.0)),
        Settlement(Flt64(3.0), Flt64(8.0)),
        Settlement(Flt64(3.0), Flt64(-2.0)),
        Settlement(Flt64(5.0), Flt64(9.0)),
        Settlement(Flt64(4.0), Flt64(-2.0))
    )

    private lateinit var x: IntVar
    private lateinit var y: IntVar
    private lateinit var dx: LinearIntermediateSymbols1<Flt64>
    private lateinit var dy: LinearIntermediateSymbols1<Flt64>
    private lateinit var distance: LinearIntermediateSymbols1<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo9", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo9::initVariable,
        Demo9::initSymbol,
        Demo9::initObject,
        Demo9::initConstraint,
        Demo9::solve,
        Demo9::analyzeSolution
    )

    suspend operator fun invoke(): Try {
        for (process in subProcesses) {
            when (val result = process()) {
                is Ok -> {}

                is Failed -> {
                    return result
                }

                is Fatal -> {
                    return result
                }
            }
        }
        return ok
    }

    private suspend fun initVariable(): Try {
        x = IntVar("x")
        y = IntVar("y")
        metaModel.add(x)
        metaModel.add(y)
        return ok
    }

    private suspend fun initSymbol(): Try {
        dx = LinearIntermediateSymbols1<Flt64>("dx", Shape1(settlements.size)) { i, _ ->
            exampleAbsoluteSlack(
                type = UInteger,
                x = flt64Linear(x),
                y = flt64Constant(settlements[i].x),
                name = "dx_$i"
            )
        }
        metaModel.add(dx)

        dy = LinearIntermediateSymbols1<Flt64>("dy", Shape1(settlements.size)) { i, _ ->
            exampleAbsoluteSlack(
                type = UInteger,
                x = flt64Linear(y),
                y = flt64Constant(settlements[i].y),
                name = "dy_$i"
            )
        }
        metaModel.add(dy)

        distance = LinearIntermediateSymbols1<Flt64>("distance", Shape1(settlements.size)) { i, _ ->
            LinearExpressionSymbol(
                dx[i] + dy[i],
                name = "distance_$i"
            )
        }
        metaModel.add(distance)
        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.minimize(sum(distance[_a]))
        return ok
    }

    private suspend fun initConstraint(): Try {
        return ok
    }

    private suspend fun solve(): Try {
        val solver = ScipLinearSolver()
        when (val ret = solveLinearMetaModel(solver, metaModel)) {
            is Ok -> {
                metaModel.tokens.setSolution(ret.value.solution)
            }

            is Failed -> {
                return Failed(ret.error)
            }

                is Fatal -> {
                return Fatal(ret.errors)
            }
        }
        return ok
    }

    private suspend fun analyzeSolution(): Try {
        val position = ArrayList<Flt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.variable.belongsTo(x)) {
                position.add(token.result!!)
            }
        }
        for (token in metaModel.tokens.tokens) {
            if (token.variable.belongsTo(y)) {
                position.add(token.result!!)
            }
        }
        return ok
    }
}

















