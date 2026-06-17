package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
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

/** * 最大流问题：在容量网络中找到从源到汇的最大流。Maximum flow problem: find the maximum flow from source to sink in a capacitated network. * * * @see     https://fuookami.github.io/ospf/examples/example11.html */
data object Demo11 {
    /** 流网络中的节点。A node in the flow network. */
    sealed class Node : AutoIndexed(Node::class)

    /** 流网络的源节点。The source node of the flow network. */
    class RootNode : Node()

    /** 流网络的汇节点。The sink node of the flow network. */
    class EndNode : Node()

    /** 流网络中的中间节点。An intermediate node in the flow network. */
    class NormalNode : Node()

    val nodes: List<Node> = listOf(
        listOf(RootNode()),
        (1..7).map { NormalNode() },
        listOf(EndNode())
    ).flatten()

    /** 连接节点间的边容量。Edge capacity between connected nodes. */
    val capacities = mapOf(
        nodes[0] to mapOf(
            nodes[1] to UInt64(15),
            nodes[2] to UInt64(10),
            nodes[3] to UInt64(40)
        ),
        nodes[1] to mapOf(
            nodes[4] to UInt64(15)
        ),
        nodes[2] to mapOf(
            nodes[5] to UInt64(10),
            nodes[6] to UInt64(35)
        ),
        nodes[3] to mapOf(
            nodes[6] to UInt64(30),
            nodes[7] to UInt64(20)
        ),
        nodes[4] to mapOf(
            nodes[6] to UInt64(10)
        ),
        nodes[5] to mapOf(
            nodes[8] to UInt64(10)
        ),
        nodes[6] to mapOf(
            nodes[7] to UInt64(10)
        ),
        nodes[7] to mapOf(
            nodes[8] to UInt64(45)
        )
    )

    lateinit var x: UIntVariable2
    lateinit var flow: UIntVar

    lateinit var flowIn: LinearIntermediateSymbols1<Flt64>
    lateinit var flowOut: LinearIntermediateSymbols1<Flt64>

    val metaModel = LinearMetaModel<Flt64>("demo11", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo11::initVariable,
        Demo11::initSymbol,
        Demo11::initObject,
        Demo11::initConstraint,
        Demo11::solve,
        Demo11::analyzeSolution
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
     * Initializes edge flow variables and the total flow variable.
     *
     * @return 返回结果。
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable2("x", Shape2(nodes.size, nodes.size))
        for (node1 in nodes) {
            for (node2 in nodes) {
                if (node1 == node2) {
                    continue
                }
                capacities[node1]?.get(node2)?.let {
                    val xi = x[node1, node2]
                    xi.range.leq(it)
                    metaModel.add(xi)
                }
            }
        }
        flow = UIntVar("flow")
        metaModel.add(flow)
        return ok
    }

    /**
     * Creates flow-in and flow-out expression symbols for each node.
     *
     * @return 返回结果。
     */
    private suspend fun initSymbol(): Try {
        flowIn = LinearIntermediateSymbols1<Flt64>(
            "flow_in",
            Shape1(nodes.size)
        ) { i, _ ->
            LinearExpressionSymbol(
                sum(x[_a, i]),
                name = "flow_in_$i"
            )
        }
        metaModel.add(flowIn)
        flowOut = LinearIntermediateSymbols1<Flt64>(
            "flow_out",
            Shape1(nodes.size)
        ) { i, _ ->
            LinearExpressionSymbol(
                sum(x[i, _a]),
                name = "flow_out_$i"
            )
        }
        metaModel.add(flowOut)
        return ok
    }

    /**
     * Sets the objective to maximize total flow.
     *
     * @return 返回结果。
     */
    private suspend fun initObject(): Try {
        metaModel.maximize(flow, "flow")
        return ok
    }

    /**
     * Adds flow conservation constraints for source, sink, and intermediate nodes.
     *
     * @return 返回结果。
     */
    private suspend fun initConstraint(): Try {
        val rootNode = nodes.first { it is RootNode }
        metaModel.addConstraint(
            flowOut[rootNode] - flowIn[rootNode] eq flow,
            name = "flow_${rootNode.index}"
        )
        val endNode = nodes.first { it is EndNode }
        metaModel.addConstraint(
            flowIn[endNode] - flowOut[endNode] eq flow,
            name = "flow_${endNode.index}"
        )
        for (node in nodes.filterIsInstance<NormalNode>()) {
            metaModel.addConstraint(
                flowOut[node] geq flowIn[node],
                name = "flow_lb_${node.index}"
            )
            metaModel.addConstraint(
                flowOut[node] leq flowIn[node],
                name = "flow_ub_${node.index}"
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
     * Extracts the edge flows from the solution.
     *
     * @return 返回结果。
     */
    private suspend fun analyzeSolution(): Try {
        val flow: MutableMap<Node, MutableMap<Node, UInt64>> = hashMapOf()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable belongsTo x) {
                val vector = token.variable.vectorView
                val node1 = nodes[vector[0]]
                val node2 = nodes[vector[1]]
                flow.getOrPut(node1) { hashMapOf() }[node2] = token.result!!.round().toUInt64()
            }
        }
        return ok
    }
}
