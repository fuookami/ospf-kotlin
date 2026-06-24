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
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.example.*

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 * 设施选址：找到最小化到定居点总曼哈顿距离的点。
 * Facility location: find the point minimizing total Manhattan distance to settlements.
 *
 * @see https://fuookami.github.io/ospf/examples/example9.html
 */
data object Demo9 {
    /**
     * 具有 x 和 y 坐标的定居点。A settlement with x and y coordinates.
     *
     * @property x X 坐标 / X coordinate
     * @property y Y 坐标 / Y coordinate
     */
    data class Settlement(
        val x: Flt64,
        val y: Flt64
    ) : AutoIndexed(Settlement::class)

    private val settlements = listOf(
        Settlement(Flt64(9.0), Flt64(2.0)),
        Settlement(Flt64(2.0), Flt64(1.0)),
        Settlement(Flt64(3.0), Flt64(8.0)),
        Settlement(Flt64(3.0), Flt64(-2.0)),
        Settlement(Flt64(5.0), Flt64(9.0)),
        Settlement(Flt64(4.0), Flt64(-2.0))
    )

    private lateinit var x: IntVar
    private lateinit var y: IntVar
    private lateinit var dx: LinearIntermediateSymbols1<Flt64>
    private lateinit var dy: LinearIntermediateSymbols1<Flt64>
    private lateinit var distance: LinearIntermediateSymbols1<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo9", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo9::initVariable,
        Demo9::initSymbol,
        Demo9::initObject,
        Demo9::initConstraint,
        Demo9::solve,
        Demo9::analyzeSolution
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
     * 初始化设施位置的整数决策变量。/ Initializes integer decision variables for the facility position.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initVariable(): Try {
        x = IntVar("x")
        y = IntVar("y")
        metaModel.add(x)
        metaModel.add(y)
        return ok
    }

    /**
     * 创建 x 轴和 y 轴的绝对距离符号，然后求和。/ Creates absolute distance symbols for x and y axes, then sums them.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initSymbol(): Try {
        dx = LinearIntermediateSymbols1<Flt64>("dx", Shape1(settlements.size)) { i, _ ->
            exampleAbsoluteSlack(
                type = UInteger,
                x = flt64Linear(x),
                y = flt64Constant(settlements[i].x),
                name = "dx_$i"
            )
        }
        metaModel.add(dx)

        dy = LinearIntermediateSymbols1<Flt64>("dy", Shape1(settlements.size)) { i, _ ->
            exampleAbsoluteSlack(
                type = UInteger,
                x = flt64Linear(y),
                y = flt64Constant(settlements[i].y),
                name = "dy_$i"
            )
        }
        metaModel.add(dy)

        distance = LinearIntermediateSymbols1<Flt64>("distance", Shape1(settlements.size)) { i, _ ->
            LinearExpressionSymbol(
                dx[i] + dy[i],
                name = "distance_$i"
            )
        }
        metaModel.add(distance)
        return ok
    }

    /**
     * 设置目标函数以最小化总曼哈顿距离。/ Sets the objective to minimize total Manhattan distance.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initObject(): Try {
        metaModel.minimize(sum(distance[_a]))
        return ok
    }

    /**
     * 无需额外约束。/ No additional constraints needed.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initConstraint(): Try {
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
     * 从解中提取最优设施位置。/ Extracts the optimal facility position from the solution.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun analyzeSolution(): Try {
        val position = ArrayList<Flt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.variable.belongsTo(x)) {
                position.add(token.result!!)
            }
        }
        for (token in metaModel.tokens.tokens) {
            if (token.variable.belongsTo(y)) {
                position.add(token.result!!)
            }
        }
        return ok
    }
}
