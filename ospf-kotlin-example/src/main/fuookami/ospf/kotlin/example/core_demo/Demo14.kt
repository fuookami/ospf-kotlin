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
 * 多商品分销：在生产、销售和转运节点下最小化运输成本。
 * Multi-commodity distribution: minimize shipping cost with production, sales, and transshipment nodes.
 *
 * @see     https://fuookami.github.io/ospf/examples/example14.html
 */
data object Demo14 {
    /** 分销网络中的节点。A node in the distribution network. */
    sealed interface Node : Indexed {
        val name: String
    }

    /**
     * 具有存储容量的生产节点。A production node with storage capacity.
     *
     * @property storage 参数。
     */
    class Product(
        override val name: String,
        val storage: UInt64
    ) : Node, AutoIndexed(Node::class)

    /**
     * 具有需求量的销售节点。A sales node with demand quantity.
     *
     * @property demand 参数。
     */
    class Sale(
        override val name: String,
        val demand: UInt64
    ) : Node, AutoIndexed(Node::class)

    /**
     * 转运（分销）节点。A transshipment (distribution) node.
     *
     */
    class Distribution(
        override val name: String
    ) : Node, AutoIndexed(Node::class)

    val nodes: List<Node> = listOf(
        Product("广州", UInt64(600)),
        Product("大连", UInt64(400)),
        Sale("南京", UInt64(200)),
        Sale("济南", UInt64(150)),
        Sale("南昌", UInt64(350)),
        Sale("青岛", UInt64(300)),
        Distribution("上海"),
        Distribution("天津")
    )

    /** 连接节点间的单位运输成本。Unit shipping cost between connected nodes. */
    val unitCost = mapOf(
        nodes[0] to mapOf(
            nodes[6] to UInt64(2),
            nodes[7] to UInt64(3)
        ),
        nodes[1] to mapOf(
            nodes[5] to UInt64(4),
            nodes[6] to UInt64(3),
            nodes[7] to UInt64(1)
        ),
        nodes[6] to mapOf(
            nodes[2] to UInt64(2),
            nodes[3] to UInt64(6),
            nodes[4] to UInt64(3),
            nodes[5] to UInt64(6)
        ),
        nodes[7] to mapOf(
            nodes[2] to UInt64(4),
            nodes[3] to UInt64(4),
            nodes[4] to UInt64(6),
            nodes[5] to UInt64(5)
        )
    )

    lateinit var x: UIntVariable2

    lateinit var cost: LinearIntermediateSymbol<Flt64>
    lateinit var transOut: LinearIntermediateSymbols1<Flt64>
    lateinit var transIn: LinearIntermediateSymbols1<Flt64>

    val metaModel = LinearMetaModel<Flt64>("demo14", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo14::initVariable,
        Demo14::initSymbol,
        Demo14::initObject,
        Demo14::initConstraint,
        Demo14::solve,
        Demo14::analyzeSolution
    )

    /**
     * 顺序运行所有子流程以构建、求解和分析模型。
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
     * 初始化连接节点间的流量变量。
     * Initializes flow variables between connected nodes.
     *
     * @return 返回结果。
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable2("x", Shape2(nodes.size, nodes.size))
        for (node1 in nodes) {
            for (node2 in nodes) {
                val thisUnitCost = unitCost[node1]?.get(node2)
                if (thisUnitCost != null) {
                    if (node1 is Product) {
                        x[node1, node2].range.leq(node1.storage)
                    }
                    metaModel.add(x[node1, node2])
                } else {
                    x[node1, node2].range.eq(UInt64.zero)
                }
            }
        }

        return ok
    }

    /**
     * 创建成本、流出和流入表达式符号。
     * Creates cost, outgoing flow, and incoming flow expression symbols.
     *
     * @return 返回结果。
     */
    private suspend fun initSymbol(): Try {
        cost = LinearExpressionSymbol(
            sum(
                nodes.flatMap { node1 ->
                    nodes.mapNotNull { node2 ->
                        unitCost[node1]?.get(node2)?.let {
                            it * x[node1, node2]
                        }
                    }
                }
            ),
            name = "cost"
        )
        metaModel.add(cost)

        transOut = LinearIntermediateSymbols1<Flt64>(
            "out",
            Shape1(nodes.size)
        ) { i, _ ->
            val node = nodes[i]
            LinearExpressionSymbol(
                sum(x[node, _a]),
                name = "out_${node.name}"
            )
        }
        metaModel.add(transOut)

        transIn = LinearIntermediateSymbols1<Flt64>(
            "in",
            Shape1(nodes.size)
        ) { i, _ ->
            val node = nodes[i]
            LinearExpressionSymbol(
                sum(x[_a, node]),
                name = "in_${node.name}"
            )
        }
        metaModel.add(transIn)

        return ok
    }

    /**
     * 设置目标为最小化总运输成本。
     * Sets the objective to minimize total shipping cost.
     *
     * @return 返回结果。
     */
    private suspend fun initObject(): Try {
        metaModel.minimize(cost)

        return ok
    }

    /**
     * 添加生产、需求和转运平衡约束。
     * Adds production, demand, and transshipment balance constraints.
     *
     * @return 返回结果。
     */
    private suspend fun initConstraint(): Try {
        for (node in nodes.filterIsInstance<Product>()) {
            metaModel.addConstraint(
                transOut[node] leq node.storage,
                name = "out_${node.name}"
            )
        }

        for (node in nodes.filterIsInstance<Sale>()) {
            metaModel.addConstraint(
                transIn[node] geq node.demand,
                name = "in_${node.name}"
            )
        }

        for (node in nodes.filterIsInstance<Distribution>()) {
            metaModel.addConstraint(
                transOut[node] geq transIn[node],
                name = "balance_lb_${node.name}"
            )
            metaModel.addConstraint(
                transOut[node] leq transIn[node],
                name = "balance_ub_${node.name}"
            )
        }

        return ok
    }

    /**
     * 使用 SCIP 求解器求解线性模型。
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
     * 从解中提取节点间的流量。
     * Extracts the flow quantities between nodes from the solution.
     *
     * @return 返回结果。
     */
    private suspend fun analyzeSolution(): Try {
        val trans: MutableMap<Node, MutableMap<Node, UInt64>> = hashMapOf()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable belongsTo x) {
                val vector = token.variable.vectorView
                val from = nodes[vector[0]]
                val to = nodes[vector[1]]
                trans.getOrPut(from) { hashMapOf() }[to] = token.result!!.round().toUInt64()
            }
        }

        return ok
    }
}
