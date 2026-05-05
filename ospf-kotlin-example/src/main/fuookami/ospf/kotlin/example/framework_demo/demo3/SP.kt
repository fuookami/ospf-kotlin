package fuookami.ospf.kotlin.example.framework_demo.demo3


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import java.util.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.*

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

object InitialSolutionGenerator {
    operator fun invoke(length: UInt64, products: List<Product>): Ret<List<CuttingPlan>> {
        val solution = ArrayList<CuttingPlan>()
        for (product in products) {
            val amount = length / product.length
            solution.add(CuttingPlan(mapOf(Pair(product, amount))))
        }
        return Ok(solution)
    }
}

class SP {
    private val solver: ColumnGenerationSolver = ScipColumnGenerationSolver()

    suspend operator fun invoke(
        iteration: UInt64,
        length: UInt64,
        products: List<Product>,
        shadowPrice: ShadowPriceMap
    ): Ret<CuttingPlan> {
        val model = LinearMetaModel<Flt64>("demo1-sp-$iteration", converter = flt64Converter)

        val y = UIntVariable1("y", Shape1(products.size))
        for (product in products) {
            y[product].name = "${y.name}_${product.index}"
        }
        model.add(y)

        val use = LinearExpressionSymbol(
            sum(products) { p -> p.length * y[p] },
            name = "use"
        )
        model.add(use)

        model.minimize(Flt64.one - sum(products) { p -> shadowPrice(p) * y[p] })
        model.addConstraint(use leq length, name = "use")

        return when (val result = solver.solveMILP("demo1-sp-$iteration", model)) {
            is Failed -> {
                Failed(result.error)
            }

            is Ok -> {
                Ok(analyze(model, products, result.value.solution))
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    private fun analyze(model: LinearMetaModel<Flt64>, products: List<Product>, result: List<Flt64>): CuttingPlan {
        val cuttingPlan = HashMap<Product, UInt64>()
        for (token in model.tokens.tokens) {
            if (result[token.solverIndex] geq Flt64.one) {
                val vector = token.variable.vectorView
                cuttingPlan[products[vector[0]]] = result[token.solverIndex].toUInt64()
            }
        }
        return CuttingPlan(cuttingPlan)
    }
}








