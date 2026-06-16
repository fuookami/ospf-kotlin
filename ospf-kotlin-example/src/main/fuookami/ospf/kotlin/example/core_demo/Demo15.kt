package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.example.solveLinearMetaModel

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

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/** Multi-plant distribution: minimize logistics cost from manufacturers to distribution centers with substitution. */
/**
 * @see     https://fuookami.github.io/ospf/examples/example15.html
 */
data object Demo15 {
    /** A car model type. */
    data class CarModel(
        val name: String
    ) : AutoIndexed(CarModel::class)

    /** A substitution rule between two car models with maximum substitution rate. */
    data class Replacement(
        val c1: CarModel,
        val c2: CarModel,
        val maximum: Flt64
    )

    /** A distribution center with substitution rules and demand per car model. */
    data class DistributionCenter(
        val name: String,
        val replacements: List<Replacement>,
        val demands: Map<CarModel, UInt64>
    ) : AutoIndexed(DistributionCenter::class)

    /** A manufacturer with production capacity and logistics cost per center. */
    data class Manufacturer(
        val name: String,
        val productivity: Map<CarModel, UInt64>,
        val logisticsCost: Map<DistributionCenter, UInt64>
    ) : AutoIndexed(Manufacturer::class)

    val carModels = listOf(
        CarModel("M1"),
        CarModel("M2"),
        CarModel("M3"),
        CarModel("M4"),
    )

    val distributionCenters = listOf(
        DistributionCenter(
            "丹佛", listOf(
                Replacement(carModels[0], carModels[1], Flt64(0.1)),
                Replacement(carModels[1], carModels[0], Flt64(0.1)),
                Replacement(carModels[2], carModels[3], Flt64(0.2)),
                Replacement(carModels[3], carModels[2], Flt64(0.2))
            ), mapOf(
                carModels[0] to UInt64(700),
                carModels[1] to UInt64(500),
                carModels[2] to UInt64(500),
                carModels[3] to UInt64(600)
            )
        ),
        DistributionCenter(
            "迈阿密", listOf(
                Replacement(carModels[0], carModels[1], Flt64(0.1)),
                Replacement(carModels[1], carModels[0], Flt64(0.1)),
                Replacement(carModels[1], carModels[3], Flt64(0.05)),
                Replacement(carModels[3], carModels[1], Flt64(0.05))
            ), mapOf(
                carModels[0] to UInt64(600),
                carModels[1] to UInt64(500),
                carModels[2] to UInt64(200),
                carModels[3] to UInt64(100)
            )
        )
    )

    val manufacturers = listOf(
        Manufacturer(
            "洛杉矶", mapOf(
                carModels[2] to UInt64(700U),
                carModels[3] to UInt64(300U)
            ), mapOf(
                distributionCenters[0] to UInt64(80),
                distributionCenters[1] to UInt64(215),
            )
        ),
        Manufacturer(
            "底特律", mapOf(
                carModels[0] to UInt64(500U),
                carModels[1] to UInt64(600U),
                carModels[3] to UInt64(400U)
            ), mapOf(
                distributionCenters[0] to UInt64(100),
                distributionCenters[1] to UInt64(108)
            )
        ),
        Manufacturer(
            "新奥尔良", mapOf(
                carModels[0] to UInt64(800U),
                carModels[1] to UInt64(400U)
            ), mapOf(
                distributionCenters[0] to UInt64(102),
                distributionCenters[1] to UInt64(68)
            )
        )
    )

    lateinit var x: UIntVariable3
    lateinit var y: Map<DistributionCenter, PctVariable1>

    lateinit var receive: LinearIntermediateSymbols2<Flt64>
    lateinit var demand: LinearIntermediateSymbols2<Flt64>
    lateinit var trans: LinearIntermediateSymbols2<Flt64>
    lateinit var cost: LinearIntermediateSymbol<Flt64>

    val metaModel = LinearMetaModel<Flt64>("demo15", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo15::initVariable,
        Demo15::initSymbol,
        Demo15::initObject,
        Demo15::initConstraint,
        Demo15::solve,
        Demo15::analyzeSolution
    )

    /** Runs all sub-processes sequentially to build, solve, and analyze the model. */
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

    /** Initializes shipment and substitution rate variables. */
    private suspend fun initVariable(): Try {
        x = UIntVariable3("x", Shape3(manufacturers.size, distributionCenters.size, carModels.size))
        for (m in manufacturers) {
            for (d in distributionCenters) {
                for (c in carModels) {
                    val xi = x[m, d, c]
                    if (m.productivity.containsKey(c)) {
                        xi.name = "x_${m.name}_${d.name}_${c.name}"
                        metaModel.add(xi)
                    } else {
                        xi.range.eq(UInt64.zero)
                    }
                }
            }
        }

        y = distributionCenters.associateWith { d ->
            val y = PctVariable1("y_${d.name}", Shape1(d.replacements.size))
            for ((r, replacement) in d.replacements.withIndex()) {
                val yi = y[r]
                yi.range.leq(replacement.maximum)
                yi.name = "${y.name}_${r}"
            }
            y
        }
        metaModel.add(y.values.flatten())

        return ok
    }

    /** Creates receive, adjusted demand, transport, and cost expression symbols. */
    private suspend fun initSymbol(): Try {
        receive = LinearIntermediateSymbols2<Flt64>(
            "receive",
            Shape2(distributionCenters.size, carModels.size)
        ) { _, v ->
            val d = distributionCenters[v[0]]
            val c = carModels[v[1]]
            LinearExpressionSymbol(
                sum(x[_a, d, c]),
                name = "receive_${d.name}_${c.name}"
            )
        }
        metaModel.add(receive)

        demand = LinearIntermediateSymbols2<Flt64>(
            "demand",
            Shape2(distributionCenters.size, carModels.size)
        ) { _, v ->
            val d = distributionCenters[v[0]]
            val c = carModels[v[1]]
            val replacedDemand = if (d.demands[c]?.let { it gr UInt64.zero } == true) {
                sum(d.replacements.withIndex().mapNotNull { (r, replacement) ->
                    if (replacement.c1 != c) {
                        return@mapNotNull null
                    }
                    d.demands[c]!!.toFlt64() * y[d]!![r]
                })
            } else {
                LinearPolynomial(Flt64.zero)
            }
            val replacedToDemand = sum(d.replacements.withIndex().mapNotNull { (r, replacement) ->
                if (replacement.c2 != c) {
                    return@mapNotNull null
                }
                d.demands[replacement.c1]?.let {
                    if (it gr UInt64.zero) {
                        it.toFlt64() * y[d]!![r]
                    } else {
                        null
                    }
                }
            })
            LinearExpressionSymbol(
                LinearPolynomial((d.demands[c] ?: UInt64.zero).toFlt64()) - replacedDemand + replacedToDemand,
                name = "demand_${d.name}_${c.name}"
            )
        }
        metaModel.add(demand)

        trans = LinearIntermediateSymbols2<Flt64>(
            "trans",
            Shape2(manufacturers.size, carModels.size)
        ) { _, v ->
            val m = manufacturers[v[0]]
            val c = carModels[v[1]]
            LinearExpressionSymbol(
                sum(x[m, _a, c]),
                name = "trans_${m.name}_${c.name}"
            )
        }
        metaModel.add(trans)

        cost = LinearExpressionSymbol(sum(manufacturers.flatMap { m ->
            distributionCenters.flatMap { d ->
                m.logisticsCost[d]?.let {
                    carModels.mapNotNull { c ->
                        if (m.productivity.containsKey(c)) {
                            it * x[m, d, c]
                        } else {
                            null
                        }
                    }
                } ?: emptyList()
            }
        }))
        metaModel.add(cost)

        return ok
    }

    /** Sets the objective to minimize total logistics cost. */
    private suspend fun initObject(): Try {
        metaModel.minimize(cost, "cost")

        return ok
    }

    /** Adds demand satisfaction and production capacity constraints. */
    private suspend fun initConstraint(): Try {
        for (d in distributionCenters) {
            for (c in carModels) {
                d.demands[c]?.let {
                    metaModel.addConstraint(
                        receive[d, c] geq it,
                        name = "demand_${d.name}_${c.name}"
                    )
                }
            }
        }

        for (m in manufacturers) {
            for (c in carModels) {
                m.productivity[c]?.let {
                    metaModel.addConstraint(
                        trans[m, c] geq it,
                        name = "produce_${m.name}_${c.name}"
                    )
                }
            }
        }

        return ok
    }

    /** Solves the linear model using the SCIP solver. */
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

    /** Extracts shipment and substitution data from the solution. */
    private suspend fun analyzeSolution(): Try {
        val trans: MutableMap<Manufacturer, MutableMap<Pair<DistributionCenter, CarModel>, UInt64>> = HashMap()
        val replacement: MutableMap<DistributionCenter, MutableMap<Pair<CarModel, CarModel>, UInt64>> = HashMap()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable belongsTo x) {
                val vector = token.variable.vectorView
                val m = manufacturers[vector[0]]
                val d = distributionCenters[vector[1]]
                val c = carModels[vector[2]]
                trans.getOrPut(m) { hashMapOf() }[d to c] = token.result!!.round().toUInt64()
            }
            for ((_, d) in distributionCenters.withIndex()) {
                val yi = y[d]!!
                if (token.result!! neq Flt64.zero && token.variable belongsTo yi) {
                    val vector = token.variable.vectorView
                    val r = d.replacements[vector[0]]
                    replacement.getOrPut(d) { hashMapOf() }[r.c1 to r.c2] = (token.result!! * d.demands[r.c1]!!.toFlt64()).round().toUInt64()
                }
            }
        }

        return ok
    }
}
