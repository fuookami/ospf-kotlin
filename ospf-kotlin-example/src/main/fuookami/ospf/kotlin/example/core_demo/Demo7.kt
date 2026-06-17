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

/** * 运输问题：最小化从仓库到商店的运输成本。Transportation problem: minimize shipping cost from warehouses to stores. * * * @see     https://fuookami.github.io/ospf/examples/example7.html */
data object Demo7 {
    /**
     * 具有需求量的商店。A store with a demand quantity.
     *
     * @property demand 参数。
     */
    data class Store(
        val demand: Flt64
    ) : AutoIndexed(Store::class)

    /**
     * 具有仓储容量和每商店运输成本的仓库。A warehouse with stowage capacity and shipping cost per store.
     *
     * @property stowage 参数。
     * @property cost 参数。
     */
    data class Warehouse(
        val stowage: Flt64,
        val cost: Map<Store, Flt64>
    ) : AutoIndexed(Warehouse::class)

    private val stores = listOf(
        Store(Flt64(200.0)),
        Store(Flt64(400.0)),
        Store(Flt64(600.0)),
        Store(Flt64(300.0))
    )
    private val warehouses = listOf(
        Warehouse(
            Flt64(510.0), mapOf(
                stores[0] to Flt64(12.0),
                stores[1] to Flt64(13.0),
                stores[2] to Flt64(21.0),
                stores[3] to Flt64(7.0)
            )
        ),
        Warehouse(
            Flt64(470.0), mapOf(
                stores[0] to Flt64(14.0),
                stores[1] to Flt64(17.0),
                stores[2] to Flt64(8.0),
                stores[3] to Flt64(18.0)
            )
        ),
        Warehouse(
            Flt64(520.0), mapOf(
                stores[0] to Flt64(10.0),
                stores[1] to Flt64(11.0),
                stores[2] to Flt64(9.0),
                stores[3] to Flt64(15.0)
            )
        )
    )

    private lateinit var x: UIntVariable2

    private lateinit var cost: LinearIntermediateSymbol<Flt64>
    private lateinit var shipment: LinearIntermediateSymbols1<Flt64>
    private lateinit var purchase: LinearIntermediateSymbols1<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo7", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo7::initVariable,
        Demo7::initSymbol,
        Demo7::initObject,
        Demo7::initConstraint,
        Demo7::solve,
        Demo7::analyzeSolution
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
     * Initializes unsigned integer variables for warehouse-store shipments.
     *
     * @return 返回结果。
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable2("x", Shape2(warehouses.size, stores.size))
        for (w in warehouses) {
            for (s in stores) {
                x[w, s].name = "${x.name}_(${w.index},${s.index})"
            }
        }
        metaModel.add(x)
        return ok
    }

    /**
     * Creates cost, shipment, and purchase expression symbols.
     *
     * @return 返回结果。
     */
    private suspend fun initSymbol(): Try {
        cost = LinearExpressionSymbol(
            sum(warehouses.map { w ->
                sum(stores.filter { w.cost.contains(it) }.map { s ->
                    w.cost[s]!! * x[w, s]
                })
            }),
            name = "cost"
        )
        metaModel.add(cost)

        shipment = LinearIntermediateSymbols1<Flt64>(
            "shipment",
            Shape1(warehouses.size)
        ) { i, _ ->
            val w = warehouses[i]
            LinearExpressionSymbol(
                sum(stores.filter { w.cost.contains(it) }.map { s -> x[w, s] }),
                name = "shipment_${w.index}"
            )
        }
        metaModel.add(shipment)

        purchase = LinearIntermediateSymbols1<Flt64>(
            "purchase",
            Shape1(stores.size)
        ) { i, _ ->
            val s = stores[i]
            LinearExpressionSymbol(
                sum(warehouses.filter { w -> w.cost.contains(s) }.map { w -> x[w, s] }),
                name = "purchase_${s.index}"
            )
        }
        metaModel.add(purchase)
        return ok
    }

    /**
     * Sets the objective to minimize total shipping cost.
     *
     * @return 返回结果。
     */
    private suspend fun initObject(): Try {
        metaModel.minimize(cost, "cost")
        return ok
    }

    /**
     * Adds warehouse stowage and store demand constraints.
     *
     * @return 返回结果。
     */
    private suspend fun initConstraint(): Try {
        for (w in warehouses) {
            metaModel.addConstraint(
                shipment[w] leq w.stowage,
                name = "stowage_${w.index}"
            )
        }

        for (s in stores) {
            metaModel.addConstraint(
                purchase[s] geq s.demand,
                name = "demand_${s.index}"
            )
        }
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
     * Extracts the shipment quantities per store and warehouse from the solution.
     *
     * @return 返回结果。
     */
    private suspend fun analyzeSolution(): Try {
        val solution = stores.associateWith { warehouses.associateWith { Flt64.zero }.toMutableMap() }
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one
                && token.variable.belongsTo(x)
            ) {
                val warehouse = warehouses[token.variable.vectorView[0]]
                val store = stores[token.variable.vectorView[1]]
                solution[store]!![warehouse] = solution[store]!![warehouse]!! + token.result!!
            }
        }
        return ok
    }
}
