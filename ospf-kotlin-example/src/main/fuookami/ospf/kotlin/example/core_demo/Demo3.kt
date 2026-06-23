package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.example.solveLinearMetaModel

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 * 生产规划：在满足产品产量要求的同时最小化成本。
 * Production planning: minimize cost while meeting product yield requirements.
 *
 * @see     https://fuookami.github.io/ospf/examples/example3.html
 */
data object Demo3 {
    /**
     * 具有最低产量要求的产品。
     * A product with a minimum yield requirement.
     *
     * @property minYield 最低产量。
     */
    data class Product(val minYield: Flt64) : AutoIndexed(Product::class)

    /**
     * 具有成本和每产品产量的物料。
     * A material with cost and yield per product.
     *
     * @property cost 成本。
     * @property yieldQuantity 每产品产量。
     */
    data class Material(
        val cost: Flt64,
        val yieldQuantity: Map<Product, Flt64>
    ) : AutoIndexed(Material::class)

    private val products = listOf(
        Product(Flt64(15000.0)),
        Product(Flt64(15000.0)),
        Product(Flt64(10000.0))
    )
    private val materials = listOf(
        Material(
            Flt64(115.0), mapOf(
                products[0] to Flt64(30.0),
                products[1] to Flt64(10.0)
            )
        ),
        Material(
            Flt64(97.0), mapOf(
                products[0] to Flt64(15.0),
                products[2] to Flt64(20.0)
            )
        ),
        Material(
            Flt64(82.0), mapOf(
                products[1] to Flt64(25.0),
                products[2] to Flt64(15.0)
            )
        ),
        Material(
            Flt64(76.0), mapOf(
                products[0] to Flt64(15.0),
                products[1] to Flt64(15.0),
                products[2] to Flt64(15.0)
            )
        )
    )

    private lateinit var x: UIntVariable1

    private lateinit var cost: LinearIntermediateSymbol<Flt64>
    private lateinit var yield: LinearIntermediateSymbols1<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo3", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo3::initVariable,
        Demo3::initSymbol,
        Demo3::initObject,
        Demo3::initConstraint,
        Demo3::solve,
        Demo3::analyzeSolution
    )

    /**
     * Runs all sub-processes sequentially to build, solve, and analyze the model.
     *
     * @return 操作结果 / Operation result
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
     * Initializes unsigned integer variables for material quantities.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable1("x", Shape1(materials.size))
        for (c in materials) {
            x[c].name = "${x.name}_${c.index}"
        }
        metaModel.add(x)
        return ok
    }

    /**
     * Creates cost and per-product yield expression symbols.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initSymbol(): Try {
        cost = LinearExpressionSymbol(
            sum(materials) { it.cost * x[it] },
            name = "cost"
        )
        metaModel.add(cost)

        yield = LinearIntermediateSymbols1<Flt64>("yield", Shape1(products.size)) { p, _ ->
            val product = products[p]
            LinearExpressionSymbol(
                sum(materials.filter { it.yieldQuantity.contains(product) }) { m ->
                    m.yieldQuantity[product]!! * x[m]
                },
                name = "yieldProduct_${p}"
            )
        }
        metaModel.add(yield)

        return ok
    }

    /**
     * Sets the objective to minimize material cost.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initObject(): Try {
        metaModel.minimize(cost)
        return ok
    }

    /**
     * Adds yield equality constraints for each product.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initConstraint(): Try {
        for (p in products) {
            metaModel.addConstraint(yield[p.index] geq p.minYield)
            metaModel.addConstraint(yield[p.index] leq p.minYield)
        }
        return ok
    }

    /**
     * Solves the linear model using the SCIP solver.
     *
     * @return 操作结果 / Operation result
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
     * Extracts the material quantities from the solution.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun analyzeSolution(): Try {
        val ret = HashMap<Material, UInt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! eq Flt64.one
                && token.variable.belongsTo(x)
            ) {
                ret[materials[token.variable.vectorView[0]]] = token.result!!.round().toUInt64()
            }
        }
        return ok
    }
}
