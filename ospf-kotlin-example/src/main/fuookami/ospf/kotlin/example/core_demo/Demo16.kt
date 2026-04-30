package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*

/**
 * @see     https://fuookami.github.io/ospf/examples/example16.html
 */
data object Demo16 {
    data class Produce(
        val month: UInt64,
        val productivity: UInt64,
        val demand: UInt64
    ) : AutoIndexed(Produce::class)

    val productPrice: Flt64 = Flt64(40.0)
    val delayDeliveryPrice: Flt64 = Flt64(2.0)
    val stowagePrice: Flt64 = Flt64(0.5)

    val produces = listOf(
        Produce(UInt64(3), UInt64(50), UInt64(100)),
        Produce(UInt64(4), UInt64(180), UInt64(200)),
        Produce(UInt64(5), UInt64(280), UInt64(180)),
        Produce(UInt64(6), UInt64(270), UInt64(300))
    )

    lateinit var x: UIntVariable2

    lateinit var produce: LinearIntermediateSymbols1Flt64
    lateinit var supply: LinearIntermediateSymbols1Flt64
    lateinit var delayDeliveryCost: LinearIntermediateSymbolFlt64
    lateinit var storageCost: LinearIntermediateSymbolFlt64
    lateinit var produceCost: LinearIntermediateSymbolFlt64

    val metaModel = LinearMetaModelFlt64("demo16")

    private val subProcesses = listOf(
        Demo16::initVariable,
        Demo16::initSymbol,
        Demo16::initObject,
        Demo16::initConstraint,
        Demo16::solve,
        Demo16::analyzeSolution
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
        x = UIntVariable2("x", Shape2(produces.size, produces.size))
        metaModel.add(x)

        return ok
    }

    private suspend fun initSymbol(): Try {
        produce = LinearIntermediateSymbols1Flt64(
            "produce",
            Shape1(produces.size)
        ) { i, _ ->
            val p = produces[i]
            LinearExpressionSymbol(
                sum(x[p, _a]),
                name = "produce_${p.month}"
            )
        }
        metaModel.add(produce)

        supply = LinearIntermediateSymbols1Flt64(
            "supply",
            Shape1(produces.size)
        ) { i, _ ->
            val p = produces[i]
            LinearExpressionSymbol(
                sum(x[_a, p]),
                name = "supply_${p.month}"
            )
        }
        metaModel.add(supply)

        delayDeliveryCost = LinearExpressionSymbol(
            sum(produces.withIndex().flatMap { (i, _) ->
                produces.withIndex().mapNotNull { (j, _) ->
                    if (i < j) {
                        Flt64(j - i).sqr() * delayDeliveryPrice * x[j, i]
                    } else {
                        null
                    }
                }
            }),
            name = "delay_delivery_cost"
        )
        metaModel.add(delayDeliveryCost)

        storageCost = LinearExpressionSymbol(
            sum(produces.withIndex().flatMap { (i, _) ->
                produces.withIndex().mapNotNull { (j, _) ->
                    if (i < j) {
                        Flt64(j - i) * stowagePrice * x[i, j]
                    } else {
                        null
                    }
                }
            }),
            name = "storage_cost"
        )
        metaModel.add(storageCost)

        produceCost = LinearExpressionSymbol(
            productPrice * sum(x[_a, _a]),
            name = "produce_cost"
        )
        metaModel.add(produceCost)

        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.minimize(
            delayDeliveryCost + storageCost + produceCost,
            "cost"
        )

        return ok
    }

    private suspend fun initConstraint(): Try {
        for (p in produces) {
            metaModel.addConstraint(
                supply[p] geq p.demand,
                name = "demand_${p.month}"
            )
        }

        for (p in produces) {
            metaModel.addConstraint(
                produce[p] leq p.productivity,
                name = "productivity_${p.month}"
            )
        }

        return ok
    }

    private suspend fun solve(): Try {
        val solver = ScipLinearSolver()
        when (val ret = solver(metaModel)) {
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
        val solution = HashMap<UInt64, HashMap<UInt64, UInt64>>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable belongsTo x) {
                val vector = token.variable.vectorView
                val i = UInt64(vector[0])
                val j = UInt64(vector[1])
                solution.getOrPut(i) { HashMap() }[j] = token.result!!.round().toUInt64()
            }
        }

        return ok
    }
}



















