package fuookami.ospf.kotlin.example.framework_demo.demo3


import fuookami.ospf.kotlin.math.algebra.number.*
import java.util.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*
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
    private val rest = LinearExpressionSymbol(name = "rest")
    private val yield = LinearExpressionSymbols1("output", Shape1(products.size)) { _, v ->
        LinearExpressionSymbol(name = "output_${v[0]}")
    }
    private val metaModel = LinearMetaModelF64("demo3")
    private val solver: ColumnGenerationSolver = ScipColumnGenerationSolver()

    init {
        metaModel.add(rest)
        metaModel.add(yield)

        metaModel.minimize(rest)

        for (product in products) {
            metaModel.addConstraint(
                yield[product] geq product.demand,
                group = null,
                name = "product_demand_${product.index}",
                args = product
            )
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
        metaModel.add(x)

        val usedLength = cuttingPlan.products.entries.fold(UInt64.zero) { acc, entry ->
            acc + entry.key.length * entry.value
        }
        rest.asMutable() += (length - usedLength) * x
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
    suspend operator fun invoke(iteration: UInt64): Ret<ShadowPriceMap> {
        return when (val result = solver.solveLP("demo1-rmp-$iteration", metaModel)) {
            is Ok -> {
                Ok(extractShadowPriceMap(result.value.dualSolution.toMeta()))
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    // solve ip
    suspend operator fun invoke(): Ret<Map<CuttingPlan, UInt64>> {
        return when (val result = solver.solveMILP("demo1-rmp-ip", metaModel)) {
            is Ok -> {
                Ok(analyzeSolution(result.value.solution))
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }

    private fun extractShadowPriceMap(dualResult: MetaDualSolution): ShadowPriceMap {
        val ret = ShadowPriceMap()

        for (constraint in metaModel.constraints) {
            val product = (constraint.args as? Product) ?: continue
            dualResult.constraints[constraint]?.let {
                ret.put(ShadowPrice(ProductDemandShadowPriceKey(product), it))
            }
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







