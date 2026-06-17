package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.example.solveLinearMetaModel

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/** * 投资组合：在风险和资金分配约束下最大化收益。Investment portfolio: maximize yield subject to risk and fund allocation constraints. * * * @see     https://fuookami.github.io/ospf/examples/example12.html */
data object Demo12 {
    /**
     * 具有收益、风险、保费和最低保费的投资产品。An investment product with yield, risk, premium, and minimum premium.
     *
     * @property yield 参数。
     * @property risk 参数。
     * @property premium 参数。
     * @property minPremium 参数。
     */
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

    lateinit var assignment: LinearIntermediateSymbols1<Flt64>
    lateinit var premium: LinearIntermediateSymbols1<Flt64>
    lateinit var risk: LinearExpressionSymbol<Flt64>
    lateinit var yield: LinearIntermediateSymbol<Flt64>

    val metaModel = LinearMetaModel<Flt64>("demo12", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo12::initVariable,
        Demo12::initSymbol,
        Demo12::initObject,
        Demo12::initConstraint,
        Demo12::solve,
        Demo12::analyzeSolution
    )

    /**
     * Runs all sub-processes sequentially to build, solve, and analyze the model.
     *
     * @return 返回结果。
     */
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

    /**
     * Initializes unsigned integer variables for product investment amounts.
     *
     * @return 返回结果。
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable1("x", Shape1(products.size))
        metaModel.add(x)
        return ok
    }

    /**
     * Creates assignment, premium, risk, and yield expression symbols.
     *
     * @return 返回结果。
     */
    private suspend fun initSymbol(): Try {
        assignment = LinearIntermediateSymbols1<Flt64>(
            "assignment",
            Shape1(products.size)
        ) { i, _ ->
            LinearFunctionSymbolAdapter(
                delegate = BinaryzationFunction(
                    polynomial = LinearPolynomial(x[i]),
                    converter = flt64Converter,
                    name = "assignment_$i"
                ),
                converter = flt64Converter
            )
        }
        metaModel.add(assignment)

        premium = LinearIntermediateSymbols1<Flt64>(
            "premium",
            Shape1(products.size)
        ) { i, _ ->
            val product = products[i]
            LinearFunctionSymbolAdapter(
                delegate = MaxFunction(
                    listOf(
                        LinearPolynomial(product.premium * x[i]),
                        LinearPolynomial(product.minPremium * assignment[i])
                    ),
                    converter = flt64Converter,
                    name = "premium_$i"
                ),
                converter = flt64Converter
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

    /**
     * Sets the objective to maximize total yield.
     *
     * @return 返回结果。
     */
    private suspend fun initObject(): Try {
        metaModel.maximize(yield, "yield")
        return ok
    }

    /**
     * Adds fund allocation and risk limit constraints.
     *
     * @return 返回结果。
     */
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

    /**
     * Solves the linear model using the SCIP solver.
     *
     * @return 返回结果。
     */
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

    /**
     * Extracts the investment amounts per product from the solution.
     *
     * @return 返回结果。
     */
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
