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
 * 生产排程：最小化跨月份的生产、仓储和延迟交付成本。
 * Production scheduling: minimize production, storage, and delay delivery costs across months.
 *
 * @see https://fuookami.github.io/ospf/examples/example16.html
*/
data object Demo16 {

    /**
     * 具有月份索引、生产率和需求的生产周期。A production period with month index, productivity, and demand.
     *
     * @property month 月份索引 / Month index
     * @property productivity 生产率 / Productivity
     * @property demand 需求量 / Demand quantity
    */
    data class Produce(
        val month: UInt64,
        val productivity: UInt64,
        val demand: UInt64
    ) : AutoIndexed(Produce::class)

    /** 单位生产成本。Unit production cost. */
    val productPrice: Flt64 = Flt64(40.0)

    /** 每平方时间段的单位延迟交付成本。Unit delay delivery cost per squared time period. */
    val delayDeliveryPrice: Flt64 = Flt64(2.0)

    /** 每时间段的单位存储成本。Unit storage cost per time period. */
    val stowagePrice: Flt64 = Flt64(0.5)

    val produces = listOf(
        Produce(UInt64(3), UInt64(50), UInt64(100)),
        Produce(UInt64(4), UInt64(180), UInt64(200)),
        Produce(UInt64(5), UInt64(280), UInt64(180)),
        Produce(UInt64(6), UInt64(270), UInt64(300))
    )

    lateinit var x: UIntVariable2

    lateinit var produce: LinearIntermediateSymbols1<Flt64>
    lateinit var supply: LinearIntermediateSymbols1<Flt64>
    lateinit var delayDeliveryCost: LinearIntermediateSymbol<Flt64>
    lateinit var storageCost: LinearIntermediateSymbol<Flt64>
    lateinit var produceCost: LinearIntermediateSymbol<Flt64>

    val metaModel = LinearMetaModel<Flt64>("demo16", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo16::initVariable,
        Demo16::initSymbol,
        Demo16::initObject,
        Demo16::initConstraint,
        Demo16::solve,
        Demo16::analyzeSolution
    )

    /**
     * 顺序运行所有子流程以构建、求解和分析模型。/ Runs all sub-processes sequentially to build, solve, and analyze the model.
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
     * 初始化期间间的生产分配变量。/ Initializes production allocation variables between periods.
     *
     * @return 操作结果 / Operation result
    */
    private suspend fun initVariable(): Try {
        x = UIntVariable2("x", Shape2(produces.size, produces.size))
        metaModel.add(x)

        return ok
    }

    /**
     * 创建生产、供应、延迟交付、库存和生产成本符号。/ Creates production, supply, delay delivery, storage, and production cost symbols.
     *
     * @return 操作结果 / Operation result
    */
    private suspend fun initSymbol(): Try {
        produce = LinearIntermediateSymbols1<Flt64>(
            "produce",
            Shape1(produces.size)
        ) { i, _ ->
            val p = produces[i]
            LinearExpressionSymbol(
                sum(x[p, _a]),
                name = "produce_${p.month}"
            )
        }
        metaModel.add(produce)

        supply = LinearIntermediateSymbols1<Flt64>(
            "supply",
            Shape1(produces.size)
        ) { i, _ ->
            val p = produces[i]
            LinearExpressionSymbol(
                sum(x[_a, p]),
                name = "supply_${p.month}"
            )
        }
        metaModel.add(supply)

        delayDeliveryCost = LinearExpressionSymbol(
            sum(produces.withIndex().flatMap { (i, _) ->
                produces.withIndex().mapNotNull { (j, _) ->
                    if (i < j) {
                        Flt64(j - i).sqr() * delayDeliveryPrice * x[j, i]
                    } else {
                        null
                    }
                }
            }),
            name = "delay_delivery_cost"
        )
        metaModel.add(delayDeliveryCost)

        storageCost = LinearExpressionSymbol(
            sum(produces.withIndex().flatMap { (i, _) ->
                produces.withIndex().mapNotNull { (j, _) ->
                    if (i < j) {
                        Flt64(j - i) * stowagePrice * x[i, j]
                    } else {
                        null
                    }
                }
            }),
            name = "storage_cost"
        )
        metaModel.add(storageCost)

        produceCost = LinearExpressionSymbol(
            productPrice * sum(x[_a, _a]),
            name = "produce_cost"
        )
        metaModel.add(produceCost)

        return ok
    }

    /**
     * 设置目标函数以最小化总成本（生产 + 库存 + 延迟）。/ Sets the objective to minimize total cost (production + storage + delay).
     *
     * @return 操作结果 / Operation result
    */
    private suspend fun initObject(): Try {
        metaModel.minimize(
            delayDeliveryCost + storageCost + produceCost,
            "cost"
        )

        return ok
    }

    /**
     * 添加需求满足和生产力约束。/ Adds demand satisfaction and productivity capacity constraints.
     *
     * @return 操作结果 / Operation result
    */
    private suspend fun initConstraint(): Try {
        for (p in produces) {
            metaModel.addConstraint(
                supply[p] geq p.demand,
                name = "demand_${p.month}"
            )
        }

        for (p in produces) {
            metaModel.addConstraint(
                produce[p] leq p.productivity,
                name = "productivity_${p.month}"
            )
        }

        return ok
    }

    /**
     * 使用 SCIP 求解器求解线性模型。/ Solves the linear model using the SCIP solver.
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
     * 从解中提取生产分配矩阵。/ Extracts the production allocation matrix from the solution.
     *
     * @return 操作结果 / Operation result
    */
    private suspend fun analyzeSolution(): Try {
        val solution = HashMap<UInt64, HashMap<UInt64, UInt64>>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable belongsTo x) {
                val vector = token.variable.vectorView
                val i = UInt64(vector[0])
                val j = UInt64(vector[1])
                solution.getOrPut(i) { HashMap() }[j] = token.result!!.round().toUInt64()
            }
        }

        return ok
    }
}
