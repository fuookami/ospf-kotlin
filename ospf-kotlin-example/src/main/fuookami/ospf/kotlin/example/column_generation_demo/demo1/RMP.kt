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
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*
import fuookami.ospf.kotlin.core.backend.plugins.gurobi.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.solver.*

data class ProductDemandShadowPriceKey(
    val product: Product
) : ShadowPriceKey(ProductDemandShadowPriceKey::class)

class RMP(
    private val length: UInt64,
    private val products: List<Product>,
    initialCuttingPlans: List<CuttingPlan>
) {
    private val cuttingPlans: MutableList<CuttingPlan> = ArrayList()
    private val x: MutableList<UIntVar> = ArrayList()
    private val rest = LinearExpressionSymbol(MutableLinearPolynomial(), "rest")
    private val yield = LinearExpressionSymbols1("output", Shape1(products.size)) { v ->
        LinearExpressionSymbol(MutableLinearPolynomial(), "output_${v.second[0]}")
    }
    private val metaModel = LinearMetaModel("demo1")
    private val solver: ColumnGenerationSolver =
        if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) {
            CplexColumnGenerationSolver()
        } else {
            GurobiColumnGenerationSolver()
        }

    init {
        metaModel.addSymbol(rest)
        metaModel.addSymbols(yield)

        metaModel.minimize(rest)
        metaModel.registerConstraintGroup("product_demand")

        for (product in products) {
            metaModel.addConstraint(yield[product] geq product.demand, "product_demand_${product.index}")
        }

        addColumns(initialCuttingPlans)
    }

    fun addColumn(cuttingPlan: CuttingPlan, flush: Boolean = true): Boolean {
        if (cuttingPlans.find { it.products == cuttingPlan.products } != null) {
            return false
        }

        cuttingPlans.add(cuttingPlan)
        val x = UIntVar("x_${cuttingPlan.index}")
        x.range.leq(cuttingPlan.products.maxOf { (product, amount) -> product.demand / amount + UInt64.one })
        this.x.add(x)
        metaModel.addVar(x)

        rest.asMutable() += (length - cuttingPlan.products.sumOf(UInt64) { it.key.length * it.value }) * x
        rest.flush()
        for ((product, amount) in cuttingPlan.products) {
            yield[product].asMutable() += amount * x
            yield[product].flush()
        }
        if (flush) {
            metaModel.flush()
        }
        return true
    }

    fun addColumns(cuttingPlans: List<CuttingPlan>) {
        for (cuttingPlan in cuttingPlans) {
            addColumn(cuttingPlan, false)
        }
        metaModel.flush()
    }

    // solve lp
    suspend operator fun invoke(iteration: UInt64): Ret<SPM> {
        return when (val result = solver.solveLP("demo1-rmp-$iteration", metaModel, true)) {
            is Ok -> {
                Ok(extractShadowPriceMap(result.value.dualSolution))
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    // solve ip
    suspend operator fun invoke(): Ret<Map<CuttingPlan, UInt64>> {
        return when (val result = solver.solveMILP("demo1-rmp-ip", metaModel, true)) {
            is Ok -> {
                Ok(analyzeSolution(result.value.solution))
            }

            is Failed -> {
                Failed(result.error)
            }
        }
    }

    private fun extractShadowPriceMap(dualResult: List<Flt64>): SPM {
        val ret = SPM()

        for ((i, j) in metaModel.indicesOfConstraintGroup("product_demand")!!.withIndex()) {
            ret.put(ShadowPrice(ProductDemandShadowPriceKey(products[i]), dualResult[j]))
        }
        ret.put { map, args ->
            map.map[ProductDemandShadowPriceKey(args)]?.price ?: Flt64.zero
        }

        return ret
    }

    private fun analyzeSolution(result: List<Flt64>): Map<CuttingPlan, UInt64> {
        val solution = HashMap<CuttingPlan, UInt64>()
        for (token in metaModel.tokens.tokens) {
            if (result[token.solverIndex] geq Flt64.one) {
                for (i in cuttingPlans.indices) {
                    if (token.variable.belongsTo(x[i])) {
                        solution[cuttingPlans[i]] = result[token.solverIndex].toUInt64()
                    }
                }
            }
        }
        return solution
    }
}
