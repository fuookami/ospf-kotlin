package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
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
 * @see     https://fuookami.github.io/ospf/examples/example4.html
 */
data object Demo4 {
    data class Material(val available: Flt64) : AutoIndexed(Material::class)

    data class Product(
        val profit: Flt64,
        val maxYield: Flt64,
        val use: Map<Material, Flt64>
    ) : AutoIndexed(Product::class)

    private val materials = listOf(
        Material(Flt64(24.0)),
        Material(Flt64(8.0))
    )
    private val products = listOf(
        Product(
            Flt64(5.0), Flt64(3.0), mapOf(
                materials[0] to Flt64(6.0),
                materials[1] to Flt64(1.0),
            )
        ),
        Product(
            Flt64(4.0), Flt64(2.0), mapOf(
                materials[0] to Flt64(4.0),
                materials[1] to Flt64(2.0),
            )
        )
    )

    private val maxDiff = Int64(1)

    private lateinit var x: RealVariable1
    private lateinit var profit: LinearIntermediateSymbolF64
    private lateinit var use: LinearIntermediateSymbols1Flt64

    private val metaModel = LinearMetaModelF64("demo4")

    private val subProcesses = listOf(
        Demo4::initVariable,
        Demo4::initSymbol,
        Demo4::initObject,
        Demo4::initConstraint,
        Demo4::solve,
        Demo4::analyzeSolution
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
        x = RealVariable1("x", Shape1(products.size))
        for (p in products) {
            x[p].name = "${x.name}_${p.index}"
        }
        metaModel.add(x)
        return ok
    }

    private suspend fun initSymbol(): Try {
        profit = LinearExpressionSymbol(
            sum(products) { p -> p.profit * x[p] },
            name = "profit"
        )
        metaModel.add(profit)

        use = LinearIntermediateSymbols1Flt64("use", Shape1(materials.size)) { m, _ ->
            val material = materials[m]
            val ps = products.filter { it.use.contains(material) }
            LinearExpressionSymbol(
                sum(ps) { p -> p.use[material]!! * x[p] },
                name = "use_${m}"
            )
        }
        metaModel.add(use)
        return ok
    }


    private suspend fun initObject(): Try {
        metaModel.maximize(profit, "profit")
        return ok
    }

    private suspend fun initConstraint(): Try {
        for (p in products) {
            x[p].range.ls(p.maxYield)
        }

        for (m in materials) {
            metaModel.addConstraint(use[m] leq m.available)
        }

        for (p1 in products) {
            for (p2 in products) {
                if (p1.index == p2.index) {
                    continue
                }
                metaModel.addConstraint((x[p1] - x[p2]) leq maxDiff.toFlt64())
            }
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
        val ret = HashMap<Material, Flt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! eq Flt64.one
                && token.variable.belongsTo(x)
            ) {
                ret[materials[token.variable.vectorView[0]]] = token.result!!
            }
        }
        return ok
    }
}



















