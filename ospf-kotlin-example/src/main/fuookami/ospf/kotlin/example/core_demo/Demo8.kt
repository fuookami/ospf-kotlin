package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
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
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * @see     https://fuookami.github.io/ospf/examples/example8.html
 */
data object Demo8 {
    data class Product(
        val profit: Flt64
    ) : AutoIndexed(Product::class)

    data class Equipment(
        val amount: UInt64,
        val manHours: Map<Product, Flt64>
    ) : AutoIndexed(Equipment::class)

    private val maxManHours = Flt64(2000)
    private val products = listOf(
        Product(Flt64(123.0)),
        Product(Flt64(94.0)),
        Product(Flt64(105.0)),
        Product(Flt64(132.0)),
        Product(Flt64(118.0)),
    )
    private val equipments = listOf(
        Equipment(
            UInt64(12), mapOf(
                products[0] to Flt64(0.23),
                products[1] to Flt64(0.44),
                products[2] to Flt64(0.17),
                products[3] to Flt64(0.08),
                products[4] to Flt64(0.36),
            )
        ),
        Equipment(
            UInt64(14), mapOf(
                products[0] to Flt64(0.13),
                products[2] to Flt64(0.20),
                products[3] to Flt64(0.37),
                products[4] to Flt64(0.19),
            )
        ),
        Equipment(
            UInt64(8), mapOf(
                products[1] to Flt64(0.25),
                products[2] to Flt64(0.34),
                products[4] to Flt64(0.18),
            )
        ),
        Equipment(
            UInt64(6), mapOf(
                products[0] to Flt64(0.55),
                products[1] to Flt64(0.72),
                products[3] to Flt64(0.61)
            )
        )
    )

    private lateinit var x: UIntVariable1

    private lateinit var profit: LinearIntermediateSymbol<Flt64>
    private lateinit var manHours: LinearIntermediateSymbols1<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo8", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo8::initVariable,
        Demo8::initSymbol,
        Demo8::initObject,
        Demo8::initConstraint,
        Demo8::solve,
        Demo8::analyzeSolution
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
        for (p in products) {
            x[p].name = "${x.name}_${p.index}"
        }
        metaModel.add(x)
        return ok
    }

    private suspend fun initSymbol(): Try {
        profit = LinearExpressionSymbol(
            sum(products.map { p ->
                p.profit * x[p]
            }),
            name = "profit"
        )
        metaModel.add(profit)

        manHours = LinearIntermediateSymbols1<Flt64>(
            "man_hours",
            Shape1(equipments.size)
        ) { i, _ ->
            val e = equipments[i]
            LinearExpressionSymbol(
                sum(products.mapNotNull { p -> e.manHours[p]?.let { it * x[p] } }),
                name = "man_hours_${e.index}"
            )
        }
        metaModel.add(manHours)

        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.maximize(profit, "profit")
        return ok
    }

    private suspend fun initConstraint(): Try {
        for (e in equipments) {
            metaModel.addConstraint(
                manHours[e] leq e.amount.toFlt64() * maxManHours,
                name = "eq_man_hours_${e.index}"
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
        val ret = HashMap<Product, UInt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! neq Flt64.one
                && token.variable.belongsTo(x)
            ) {
                ret[products[token.variable.vectorView[0]]] = token.result!!.round().toUInt64()
            }
        }
        return ok
    }
}



















