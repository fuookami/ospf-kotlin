package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import java.util.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*
import fuookami.ospf.kotlin.framework.solver.*

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
    private val solver: ColumnGenerationSolver = SCIPColumnGenerationSolver()

    suspend operator fun invoke(
        iteration: UInt64,
        length: UInt64,
        products: List<Product>,
        shadowPrice: SPM
    ): Ret<CuttingPlan> {
        val model = LinearMetaModel("demo1-sp-$iteration")

        val y = UIntVariable1("y", Shape1(products.size))
        for (product in products) {
            y[product].name = "${y.name}_${product.index}"
        }
        model.addVars(y)

        val use = LinearExpressionSymbol(sum(products) { p -> p.length * y[p] }, "use")
        model.addSymbol(use)

        model.minimize(Flt64.one - sum(products) { p -> shadowPrice(p) * y[p] })
        model.addConstraint(use leq length, "use")

        return when (val result = solver.solveMILP("demo1-sp-$iteration", model)) {
            is Failed -> {
                Failed(result.error)
            }

            is Ok -> {
                Ok(analyze(model, products, result.value.solution))
            }
        }
    }

    private fun analyze(model: LinearMetaModel, products: List<Product>, result: List<Flt64>): CuttingPlan {
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
