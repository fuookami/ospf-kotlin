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
 * 有界背包问题：在每项数量限制下最大化货物价值。
 * Bounded knapsack: maximize cargo value with quantity limits per item.
 *
 * @see https://fuookami.github.io/ospf/examples/example6.html
 */
data object Demo6 {
    /**
     * 具有重量、价值和可用数量的货物项。A cargo item with weight, value, and available amount.
     *
     * @property weight 重量 / Weight
     * @property value 价值 / Value
     * @property amount 数量 / Amount
     */
    data class Cargo(
        val weight: UInt64,
        val value: UInt64,
        val amount: UInt64
    ) : AutoIndexed(Cargo::class)

    private val cargos = listOf(
        Cargo(UInt64(1), UInt64(6), UInt64(10)),
        Cargo(UInt64(2), UInt64(10), UInt64(5)),
        Cargo(UInt64(2), UInt64(20), UInt64(2))
    )
    private val maxWeight = UInt64(8)

    private lateinit var x: UIntVariable1

    private lateinit var cargoWeight: LinearIntermediateSymbol<Flt64>
    private lateinit var cargoValue: LinearIntermediateSymbol<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo6", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo6::initVariable,
        Demo6::initSymbol,
        Demo6::initObject,
        Demo6::initConstraint,
        Demo6::solve,
        Demo6::analyzeSolution
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
     * 初始化货物数量的无符号整数决策变量。/ Initializes unsigned integer decision variables for cargo quantities.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initVariable(): Try {
        x = UIntVariable1("x", Shape1(cargos.size))
        for (c in cargos) {
            x[c].name = "${x.name}_${c.index}"
        }
        metaModel.add(x)
        return ok
    }

    /**
     * 创建货物价值和重量表达式符号。/ Creates cargo value and weight expression symbols.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initSymbol(): Try {
        cargoValue = LinearExpressionSymbol(
            sum(cargos) { c -> c.value * x[c] },
            name = "value"
        )
        metaModel.add(cargoValue)

        cargoWeight = LinearExpressionSymbol(
            sum(cargos) { c -> c.weight * x[c] },
            name = "weight"
        )
        metaModel.add(cargoWeight)

        return ok
    }

    /**
     * 设置目标函数以最大化总货物价值。/ Sets the objective to maximize total cargo value.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initObject(): Try {
        metaModel.maximize(cargoValue, "value")
        return ok
    }

    /**
     * 添加每项数量限制和重量容量约束。/ Adds per-item amount limits and weight capacity constraint.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun initConstraint(): Try {
        for (c in cargos) {
            x[c].range.ls(c.amount)
        }

        metaModel.addConstraint(
            cargoWeight leq maxWeight,
            name = "weight"
        )
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
     * 从解中提取货物数量。/ Extracts the cargo quantities from the solution.
     *
     * @return 操作结果 / Operation result
     */
    private suspend fun analyzeSolution(): Try {
        val ret = HashMap<Cargo, UInt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable.belongsTo(x)) {
                ret[cargos[token.variable.vectorView[0]]] = token.result!!.round().toUInt64()
            }
        }
        return ok
    }
}
