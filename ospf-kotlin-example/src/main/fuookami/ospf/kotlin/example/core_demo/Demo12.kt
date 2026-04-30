package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*

/**
 * @see     https://fuookami.github.io/ospf/examples/example12.html
 */
data object Demo12 {
    data class Product(
        val yield: Flt64,
        val risk: Flt64,
        val premium: Flt64,
        val minPremium: Flt64
    ) : AutoIndexed(Product::class)

    val products = listOf(
        Product(Flt64(0.28), Flt64(0.04), Flt64(0.08), Flt64(103.0)),
        Product(Flt64(0.21), Flt64(0.015), Flt64(0.02), Flt64(198.0)),
        Product(Flt64(0.23), Flt64(0.05), Flt64(0.045), Flt64(52.0)),
        Product(Flt64(0.25), Flt64(0.026), Flt64(0.04), Flt64(40.0)),
        Product(Flt64(0.05), Flt64(0.0), Flt64(0.0), Flt64(0.0))
    )
    val funds = Flt64(1000000.0)
    val maxRisk = Flt64(0.02)

    lateinit var x: UIntVariable1

    lateinit var assignment: LinearIntermediateSymbols1Flt64
    lateinit var premium: LinearIntermediateSymbols1Flt64
    lateinit var risk: LinearExpressionSymbolFlt64
    lateinit var yield: LinearIntermediateSymbolFlt64

    val metaModel = LinearMetaModelFlt64("demo12")

    private val subProcesses = listOf(
        Demo12::initVariable,
        Demo12::initSymbol,
        Demo12::initObject,
        Demo12::initConstraint,
        Demo12::solve,
        Demo12::analyzeSolution
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
        x = UIntVariable1("x", Shape1(products.size))
        metaModel.add(x)
        return ok
    }

    private suspend fun initSymbol(): Try {
        assignment = LinearIntermediateSymbols1Flt64(
            "assignment",
            Shape1(products.size)
        ) { i, _ ->
            BinaryzationFunction(
                input = LinearPolynomial(x[i]),
                name = "assignment_$i"
            )
        }
        metaModel.add(assignment)

        premium = LinearIntermediateSymbols1Flt64(
            "premium",
            Shape1(products.size)
        ) { i, _ ->
            val product = products[i]
            LinearFunctionSymbolAdapter(
                MaxFunction(
                    listOf(
                        LinearPolynomial(product.premium * x[i]),
                        LinearPolynomial(product.minPremium * assignment[i])
                    ),
                    name = "premium_$i"
                )
            )
        }
        metaModel.add(premium)

        risk = LinearExpressionSymbol(
            sum(products.map { p -> p.risk * x[p] / funds }),
            name = "risk"
        )
        metaModel.add(risk)

        yield = LinearExpressionSymbol(
            sum(products.map { p -> p.yield * x[p] - premium[p] }),
            name = "yield"
        )
        metaModel.add(yield)

        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.maximize(yield, "yield")
        return ok
    }

    private suspend fun initConstraint(): Try {
        metaModel.addConstraint(
            sum(products.map { p -> x[p] + premium[p] }) eq funds,
            name = "funs"
        )

        metaModel.addConstraint(
            risk leq maxRisk,
            name = "risk"
        )

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
        val ret = HashMap<Product, UInt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable.belongsTo(x)) {
                val vector = token.variable.vectorView
                val product = products[vector[0]]
                ret[product] = token.result!!.round().toUInt64()
            }
        }
        return ok
    }
}



















