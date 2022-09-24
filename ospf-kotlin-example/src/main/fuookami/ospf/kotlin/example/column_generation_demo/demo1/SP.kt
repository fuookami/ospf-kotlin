package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

object InitialSolutionGenerator {
    operator fun invoke(length: UInt64, products: List<fuookami.ospf.kotlin.example.column_generation_demo.demo1.Product>): Result<List<fuookami.ospf.kotlin.example.column_generation_demo.demo1.CuttingPlan>, Error> {
        val solution = ArrayList<fuookami.ospf.kotlin.example.column_generation_demo.demo1.CuttingPlan>()
        for (product in products) {
            val amount = length / product.length
            solution.add(
                fuookami.ospf.kotlin.example.column_generation_demo.demo1.CuttingPlan(
                    mapOf(
                        Pair(
                            product,
                            amount
                        )
                    )
                )
            )
        }
        return Ok(solution)
    }
}

class SP {
    operator fun invoke(iteration: UInt64, length: UInt64, products: List<fuookami.ospf.kotlin.example.column_generation_demo.demo1.Product>, shadowPrice: fuookami.ospf.kotlin.example.column_generation_demo.demo1.SPM): Result<fuookami.ospf.kotlin.example.column_generation_demo.demo1.CuttingPlan, Error> {
        val model = LinearMetaModel("demo1-sp-$iteration")

        val y = UIntVariable1("y", Shape1(products.size))
        for (product in products) {
            y[product]!!.name = "${y.name}_${product.index}"
        }
        model.addVars(y)

        val usePoly = LinearPolynomial()
        for (product in products) {
            usePoly += product.length * y[product]!!
        }
        val use = LinearSymbol(usePoly, "use")
        model.addSymbol(use)

        val objPoly = LinearPolynomial(Flt64.one)
        for (product in products) {
            objPoly -= shadowPrice(product) * y[product]!!
        }
        model.minimize(objPoly)

        model.addConstraint(use leq length, "use")

        return when (val result = solveMIP("demo1-sp-$iteration", model)) {
            is Failed -> { Failed(result.error) }
            is Ok -> { Ok(analyze(model, products, result.value)) }
        }
    }

    private fun analyze(model: LinearMetaModel, products: List<fuookami.ospf.kotlin.example.column_generation_demo.demo1.Product>, result: List<Flt64>): fuookami.ospf.kotlin.example.column_generation_demo.demo1.CuttingPlan {
        val cuttingPlan = HashMap<fuookami.ospf.kotlin.example.column_generation_demo.demo1.Product, UInt64>()
        for (token in model.tokens.tokens) {
            if (result[token.solverIndex] geq Flt64.one) {
                val vector = token.variable.vectorView
                cuttingPlan[products[vector[0]]] = result[token.solverIndex].toUInt64()
            }
        }
        return fuookami.ospf.kotlin.example.column_generation_demo.demo1.CuttingPlan(cuttingPlan)
    }
}
