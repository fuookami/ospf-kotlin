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
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*

/**
 * @see     https://fuookami.github.io/ospf/examples/example1.html
 */
data object Demo1 {
    data class Company(
        val capital: Flt64,
        val liability: Flt64,
        val profit: Flt64
    ) : AutoIndexed(Company::class)

    private val companies = listOf(
        Company(Flt64(3.48), Flt64(1.28), Flt64(5400.0)),
        Company(Flt64(5.62), Flt64(2.53), Flt64(2300.0)),
        Company(Flt64(7.33), Flt64(1.02), Flt64(4600.0)),
        Company(Flt64(6.27), Flt64(3.55), Flt64(3300.0)),
        Company(Flt64(2.14), Flt64(0.53), Flt64(980.0))
    )
    private val minCapital = Flt64(10.0)
    private val maxLiability = Flt64(5.0)

    private lateinit var x: BinVariable1
    private lateinit var capital: LinearExpressionSymbolF64
    private lateinit var liability: LinearExpressionSymbolF64
    private lateinit var profit: LinearExpressionSymbolF64

    private val metaModel = LinearMetaModelF64("demo1")

    private val subProcesses = listOf(
        Demo1::initVariable,
        Demo1::initSymbol,
        Demo1::initObject,
        Demo1::initConstraint,
        Demo1::solve,
        Demo1::analyzeSolution
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
        x = BinVariable1("x", Shape1(companies.size))
        for (c in companies) {
            x[c].name = "${x.name}_${c.index}"
        }
        metaModel.add(x)
        return ok
    }

    private suspend fun initSymbol(): Try {
        capital = LinearExpressionSymbol(
            sum(companies) { it.capital * x[it] },
            name = "capital"
        )
        metaModel.add(capital)

        liability = LinearExpressionSymbol(
            sum(companies) { it.liability * x[it] },
            name = "liability"
        )
        metaModel.add(liability)

        profit = LinearExpressionSymbol(
            sum(companies) { it.profit * x[it] },
            name = "profit"
        )
        metaModel.add(profit)
        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.maximize(profit)
        return ok
    }

    private suspend fun initConstraint(): Try {
        metaModel.addConstraint(capital geq minCapital)
        metaModel.addConstraint(liability leq maxLiability)
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
        val ret = ArrayList<Company>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! eq Flt64.one) {
                ret.add(companies[token.variable.index])
            }
        }
        return ok
    }
}


















