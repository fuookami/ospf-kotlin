package fuookami.ospf.kotlin.example.framework_demo.demo6

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.*
import fuookami.ospf.kotlin.core.model.variable.*
import fuookami.ospf.kotlin.core.model.objective.*
import fuookami.ospf.kotlin.core.model.constraint.*
import fuookami.ospf.kotlin.core.model.symbol.*
import fuookami.ospf.kotlin.core.model.solution.*
import fuookami.ospf.kotlin.example.*

/**
 * 表示一个具有两个字段的数据项 / Represents a data item with two fields
 * @property field1 第一个字段 / The first field
 * @property field2 第二个字段 / The second field
 */
data class Item(val field1: UInt64, val field2: UInt64)

/** 表示问题的上下文，包含项目列表和系数 / Represents the problem context, containing items list and coefficients */
data class Context(
    /** 项目列表 / List of items */
    val items: List<Item>,
    /** 系数列表 / List of coefficients */
    val coefficients: List<UInt64>,
    /** 最大系数值 / Maximum coefficient value */
    val maxCoefficient: UInt64
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            coefficients: List<UInt64>,
            maxCoefficient: UInt64
        ): Result<Context> {
            return if (items.size == coefficients.size) {
                Result.ok(Context(items, coefficients, maxCoefficient))
            } else {
                Result.failed(IllegalArgumentException("items and coefficients must have the same size"))
            }
        }
    }
}

/** 第一个约束条件，将 field1 乘以系数 / First constraint, multiplying field1 by the coefficient */
data class Constraint1(
    /** 系数值 / Coefficient value */
    val coefficient: UInt64,
    /** 项目列表 / List of items */
    val items: List<Item>
) : MutableConstraint {
    override fun createSymbols(): Result<MutableList<Symbol>> {
        return Result.ok(items.map { item -> item.field1 * coefficient }.toMutableList())
    }
}

/** 第二个约束条件，将 field2 乘以系数 / Second constraint, multiplying field2 by the coefficient */
data class Constraint2(
    /** 系数值 / Coefficient value */
    val coefficient: UInt64,
    /** 项目列表 / List of items */
    val items: List<Item>
) : MutableConstraint {
    override fun createSymbols(): Result<MutableList<Symbol>> {
        return Result.ok(items.map { item -> item.field2 * coefficient }.toMutableList())
    }
}

/** 演示6的应用程序，展示如何使用自定义约束 / Application for Demo6, demonstrating how to use custom constraints */
class Demo6Application : DemoApplication {
    override fun createModel(): Result<Model> {
        val model = Model()
        val context = Context(
            listOf(
                Item(UInt64(1), UInt64(2)),
                Item(UInt64(3), UInt64(4))
            ),
            listOf(UInt64(2), UInt64(3)),
            UInt64(10)
        )

        val variables = context.items.map { item ->
            model.addVariable(SimpleVariable())
        }.toList().bind { it }
            .map { it.first }

        val constraint1 = Constraint1(UInt64(2), context.items)
        val constraint2 = Constraint2(UInt64(3), context.items)

        model.addConstraint(constraint1)
        model.addConstraint(constraint2)

        val objective = Objective(variables.first())
        model.addObjective(objective)

        return Result.ok(model)
    }

    override fun solveModel(model: Model): Result<Solution> {
        val solver = Solver()
        return solver.solve(model)
    }
}
