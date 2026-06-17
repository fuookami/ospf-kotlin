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

/** * 车辆路径问题：在卡车容量约束下最小化从配送中心到经销商的配送距离。Vehicle routing: minimize distribution distance from centers to dealers with truck capacity. * * * @see     https://fuookami.github.io/ospf/examples/example13.html */data object Demo13 {
    /**
     * 具有需求量的经销商。A dealer with a demand quantity.
     *
     * @property demand 参数。
     */
    data class Dealer(
        val demand: UInt64
    ) : AutoIndexed(Dealer::class)

    /**
     * 具有供应和到经销商距离的配送中心。A distribution center with supply and distances to dealers.
     *
     * @property supply 参数。
     * @property distance 参数。
     */
    data class DistributionCenter(
        val supply: UInt64,
        val distance: Map<Dealer, UInt64>
    ) : AutoIndexed(DistributionCenter::class)

    /** 每辆卡车的最大载货量。Maximum cargo capacity per truck. */
    val carCapacity = UInt64(18)

    val dealers = listOf(
        Dealer(UInt64(100)),
        Dealer(UInt64(200)),
        Dealer(UInt64(150)),
        Dealer(UInt64(160)),
        Dealer(UInt64(140))
    )

    val distributionCenters = listOf(
        DistributionCenter(
            UInt64(400), mapOf(
                dealers[0] to UInt64(100),
                dealers[1] to UInt64(150),
                dealers[2] to UInt64(200),
                dealers[3] to UInt64(140),
                dealers[4] to UInt64(35)
            )
        ),
        DistributionCenter(
            UInt64(200), mapOf(
                dealers[0] to UInt64(50),
                dealers[1] to UInt64(70),
                dealers[2] to UInt64(60),
                dealers[3] to UInt64(65),
                dealers[4] to UInt64(80)
            )
        ),
        DistributionCenter(
            UInt64(150), mapOf(
                dealers[0] to UInt64(40),
                dealers[1] to UInt64(90),
                dealers[2] to UInt64(100),
                dealers[3] to UInt64(150),
                dealers[4] to UInt64(130)
            )
        )
    )

    lateinit var x: UIntVariable2
    lateinit var y: UIntVariable2

    lateinit var trans: LinearIntermediateSymbols1<Flt64>
    lateinit var receive: LinearIntermediateSymbols1<Flt64>
    lateinit var cost: LinearIntermediateSymbol<Flt64>

    val metaModel = LinearMetaModel<Flt64>("demo13", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo13::initVariable,
        Demo13::initSymbol,
        Demo13::initObject,
        Demo13::initConstraint,
        Demo13::solve,
        Demo13::analyzeSolution
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
     * Initializes shipment and truck count variables.
 *
     * @return 返回结果。
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable2("x", Shape2(dealers.size, distributionCenters.size))
        metaModel.add(x)

        y = UIntVariable2("y", Shape2(dealers.size, distributionCenters.size))
        metaModel.add(y)

        return ok
    }

    /**
     * Creates transport, receive, and cost expression symbols.
 *
     * @return 返回结果。
     */
    private suspend fun initSymbol(): Try {
        trans = LinearIntermediateSymbols1<Flt64>(
            "trans",
            Shape1(distributionCenters.size)
        ) { i, _ ->
            val distributionCenter = distributionCenters[i]
            LinearExpressionSymbol(
                sum(x[_a, distributionCenter]),
                name = "trans_${distributionCenter.index}"
            )
        }
        metaModel.add(trans)

        receive = LinearIntermediateSymbols1<Flt64>(
            "receive",
            Shape1(dealers.size)
        ) { i, _ ->
            val dealer = dealers[i]
            LinearExpressionSymbol(
                sum(x[dealer, _a]),
                name = "receive_${dealer.index}"
            )
        }
        metaModel.add(receive)

        cost = LinearExpressionSymbol(
            sum(dealers.flatMap { dealer ->
                distributionCenters.mapNotNull { distributionCenter ->
                    val distance = distributionCenter.distance[dealer] ?: return@mapNotNull null
                    distance * y[dealer, distributionCenter]
                }
            }),
            name = "cost"
        )
        metaModel.add(cost)

        return ok
    }

    /**
     * Sets the objective to minimize total distribution distance.
 *
     * @return 返回结果。
     */
    private suspend fun initObject(): Try {
        metaModel.minimize(cost, "cost")

        return ok
    }

    /**
     * Adds supply, demand, and truck capacity constraints.
 *
     * @return 返回结果。
     */
    private suspend fun initConstraint(): Try {
        for (distributionCenter in distributionCenters) {
            metaModel.addConstraint(
                trans[distributionCenter] leq distributionCenter.supply,
                name = "supply_${distributionCenter.index}"
            )
        }

        for (dealer in dealers) {
            metaModel.addConstraint(
                receive[dealer] geq dealer.demand,
                name = "demand_${dealer.index}"
            )
        }

        for (dealer in dealers) {
            for (distributionCenter in distributionCenters) {
                metaModel.addConstraint(
                    x[dealer, distributionCenter] - carCapacity.toFlt64() * y[dealer, distributionCenter] leq Flt64.zero,
                    name = "car_limit_${dealer.index}_${distributionCenter.index}"
                )
            }
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
     * Extracts the shipment quantities per distribution center and dealer.
 *
     * @return 返回结果。
     */
    private suspend fun analyzeSolution(): Try {
        val trans: MutableMap<DistributionCenter, MutableMap<Dealer, UInt64>> = hashMapOf()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable belongsTo x) {
                val vector = token.variable.vectorView
                val dealer = dealers[vector[0]]
                val distributionCenter = distributionCenters[vector[1]]
                trans.getOrPut(distributionCenter) { hashMapOf() }[dealer] = token.result!!.round().toUInt64()
            }
        }

        return ok
    }
}
