/**
 * 远程求解序列化模型
 * Remote solve serialized models
 */
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 序列化变量类型。
 * Serialized variable type.
 */
@Serializable
enum class SerializedVariableType {
    /** 连续变量 / Continuous variable */
    CONTINUOUS,

    /** 二进制变量 / Binary variable */
    BINARY,

    /** 整数变量 / Integer variable */
    INTEGER,

    /** 半连续变量 / Semi-continuous variable */
    SEMI_CONTINUOUS,

    /** 半整数变量 / Semi-integer variable */
    SEMI_INTEGER
}

/**
 * 序列化约束符号。
 * Serialized constraint sign.
 */
@Serializable
enum class SerializedConstraintSign {
    /** 小于等于 / Less than or equal */
    LESS_EQUAL,

    /** 大于等于 / Greater than or equal */
    GREATER_EQUAL,

    /** 等于 / Equal */
    EQUAL
}

/**
 * 序列化目标类型。
 * Serialized objective category.
 */
@Serializable
enum class SerializedObjectiveCategory {
    /** 最小化 / Minimize */
    MINIMIZE,

    /** 最大化 / Maximize */
    MAXIMIZE
}

/**
 * 约束矩阵单元。
 * Constraint matrix cell.
 *
 * @property rowIndex 行索引 / Row index
 * @property colIndex 列索引 / Column index
 * @property coefficient 系数 / Coefficient
 */
@Serializable
data class SerializedConstraintCell(
    val rowIndex: Int,
    val colIndex: Int,
    val coefficient: Flt64
)

/**
 * 目标函数单元。
 * Objective cell.
 *
 * @property colIndex 列索引 / Column index
 * @property coefficient 系数 / Coefficient
 */
@Serializable
data class SerializedObjectiveCell(
    val colIndex: Int,
    val coefficient: Flt64
)

/**
 * 序列化变量。
 * Serialized variable.
 *
 * @property index 变量索引 / Variable index
 * @property name 变量名 / Variable name
 * @property lowerBound 下界 / Lower bound
 * @property upperBound 上界 / Upper bound
 * @property type 变量类型 / Variable type
 */
@Serializable
data class SerializedVariable(
    val index: Int,
    val name: String,
    val lowerBound: Flt64,
    val upperBound: Flt64,
    val type: SerializedVariableType
)

/**
 * 序列化线性约束。
 * Serialized linear constraint.
 *
 * @property cells 约束单元 / Constraint cells
 * @property sign 约束符号 / Constraint sign
 * @property rhs 右端值 / Right-hand side
 * @property name 约束名 / Constraint name
 */
@Serializable
data class SerializedConstraint(
    val cells: List<SerializedConstraintCell>,
    val sign: SerializedConstraintSign,
    val rhs: Flt64,
    val name: String
)

/**
 * 序列化线性目标。
 * Serialized linear objective.
 *
 * @property category 目标类型 / Objective category
 * @property cells 目标单元 / Objective cells
 * @property constant 常数项 / Constant term
 */
@Serializable
data class SerializedObjective(
    val category: SerializedObjectiveCategory,
    val cells: List<SerializedObjectiveCell>,
    val constant: Flt64 = Flt64.zero
)

/**
 * 序列化线性模型。
 * Serialized linear model.
 *
 * @property name 模型名 / Model name
 * @property variables 变量列表 / Variables
 * @property constraints 约束列表 / Constraints
 * @property objective 目标函数 / Objective
 */
@Serializable
data class SerializedLinearModel(
    val name: String,
    val variables: List<SerializedVariable>,
    val constraints: List<SerializedConstraint>,
    val objective: SerializedObjective
) {
    /** 变量数量 / Variable count */
    val variableCount: Int get() = variables.size

    /** 约束数量 / Constraint count */
    val constraintCount: Int get() = constraints.size

    /**
     * 导出 LP 格式。
     * Export LP format.
     *
     * @return LP 格式文本 / LP format text
     */
    fun toLpFormat(): String {
        val builder = StringBuilder()
        builder.append("${objective.category.toLpString()}\n")
        builder.append(" obj: ")
        objective.cells.sortedBy { it.colIndex }.forEachIndexed { index, cell ->
            appendLinearTerm(
                builder = builder,
                first = index == 0,
                coefficient = cell.coefficient,
                variableName = variables[cell.colIndex].name
            )
        }
        if (objective.constant != Flt64.zero) {
            builder.append(" + ${objective.constant}")
        }
        builder.append("\n\n")

        builder.append("Subject To\n")
        constraints.forEach { constraint ->
            builder.append(" ${constraint.name}: ")
            constraint.cells.sortedBy { it.colIndex }.forEachIndexed { index, cell ->
                appendLinearTerm(
                    builder = builder,
                    first = index == 0,
                    coefficient = cell.coefficient,
                    variableName = variables[cell.colIndex].name
                )
            }
            builder.append(" ${constraint.sign.toLpString()} ${constraint.rhs}\n")
        }
        builder.append("\n")

        builder.append("Bounds\n")
        variables.forEach { variable ->
            when {
                variable.lowerBound == Flt64.negativeInfinity &&
                    variable.upperBound == Flt64.infinity -> {
                    builder.append(" ${variable.name} free\n")
                }

                variable.lowerBound == variable.upperBound -> {
                    builder.append(" ${variable.name} = ${variable.lowerBound}\n")
                }

                variable.lowerBound == Flt64.zero &&
                    variable.upperBound == Flt64.infinity &&
                    variable.type != SerializedVariableType.CONTINUOUS -> {
                    // 整数非负上界为无穷时由类型分段表达。 / Non-negative integer variables are expressed by type sections.
                }

                else -> {
                    builder.append(" ${variable.lowerBound} <= ${variable.name} <= ${variable.upperBound}\n")
                }
            }
        }
        builder.append("\n")

        val binaries = variables.filter { it.type == SerializedVariableType.BINARY }
        if (binaries.isNotEmpty()) {
            builder.append("Binaries\n")
            binaries.forEach { builder.append(" ${it.name}") }
            builder.append("\n\n")
        }

        val integers = variables.filter { it.type == SerializedVariableType.INTEGER }
        if (integers.isNotEmpty()) {
            builder.append("Generals\n")
            integers.forEach { builder.append(" ${it.name}") }
            builder.append("\n\n")
        }

        builder.append("End\n")
        return builder.toString()
    }

    companion object {
        /**
         * 创建空模型。
         * Create empty model.
         *
         * @param name 模型名 / Model name
         * @return 空线性模型 / Empty linear model
         */
        fun empty(name: String = "empty"): SerializedLinearModel {
            return SerializedLinearModel(
                name = name,
                variables = emptyList(),
                constraints = emptyList(),
                objective = SerializedObjective(
                    category = SerializedObjectiveCategory.MINIMIZE,
                    cells = emptyList(),
                constant = Flt64.zero
            )
        )
    }
    }
}

/**
 * 序列化二次模型。
 * Serialized quadratic model.
 *
 * @property name 模型名 / Model name
 * @property variables 变量列表 / Variables
 * @property linearConstraints 线性约束 / Linear constraints
 * @property quadraticConstraints 二次约束 / Quadratic constraints
 * @property objective 二次目标 / Quadratic objective
 */
@Serializable
data class SerializedQuadraticModel(
    val name: String,
    val variables: List<SerializedVariable>,
    val linearConstraints: List<SerializedConstraint>,
    val quadraticConstraints: List<SerializedQuadraticConstraint>,
    val objective: SerializedQuadraticObjective
) {
    /** 变量数量 / Variable count */
    val variableCount: Int get() = variables.size

    /** 线性约束数量 / Linear constraint count */
    val linearConstraintCount: Int get() = linearConstraints.size

    /** 二次约束数量 / Quadratic constraint count */
    val quadraticConstraintCount: Int get() = quadraticConstraints.size
}

/**
 * 二次约束单元。
 * Quadratic constraint cell.
 *
 * @property rowIndex 行索引 / Row index
 * @property colIndex1 第一列索引 / First column index
 * @property colIndex2 第二列索引 / Second column index
 * @property coefficient 系数 / Coefficient
 */
@Serializable
data class SerializedQuadraticConstraintCell(
    val rowIndex: Int,
    val colIndex1: Int,
    val colIndex2: Int,
    val coefficient: Flt64
)

/**
 * 序列化二次约束。
 * Serialized quadratic constraint.
 *
 * @property linearCells 线性单元 / Linear cells
 * @property quadraticCells 二次单元 / Quadratic cells
 * @property sign 约束符号 / Constraint sign
 * @property rhs 右端值 / Right-hand side
 * @property name 约束名 / Constraint name
 */
@Serializable
data class SerializedQuadraticConstraint(
    val linearCells: List<SerializedConstraintCell>,
    val quadraticCells: List<SerializedQuadraticConstraintCell>,
    val sign: SerializedConstraintSign,
    val rhs: Flt64,
    val name: String
)

/**
 * 二次目标单元。
 * Quadratic objective cell.
 *
 * @property colIndex1 第一列索引 / First column index
 * @property colIndex2 第二列索引 / Second column index
 * @property coefficient 系数 / Coefficient
 */
@Serializable
data class SerializedQuadraticObjectiveCell(
    val colIndex1: Int,
    val colIndex2: Int,
    val coefficient: Flt64
)

/**
 * 序列化二次目标。
 * Serialized quadratic objective.
 *
 * @property category 目标类型 / Objective category
 * @property linearCells 线性目标单元 / Linear objective cells
 * @property quadraticCells 二次目标单元 / Quadratic objective cells
 * @property constant 常数项 / Constant term
 */
@Serializable
data class SerializedQuadraticObjective(
    val category: SerializedObjectiveCategory,
    val linearCells: List<SerializedObjectiveCell>,
    val quadraticCells: List<SerializedQuadraticObjectiveCell>,
    val constant: Flt64 = Flt64.zero
)

/**
 * 序列化解。
 * Serialized solution.
 *
 * @property feasible 是否可行 / Whether feasible
 * @property optimal 是否最优 / Whether optimal
 * @property objectiveValue 目标值 / Objective value
 * @property gap 最优间隙 / Optimality gap
 * @property variableValues 变量值 / Variable values
 * @property elapsed 耗时 / Elapsed
 * @property solverStatus 求解器状态 / Solver status
 * @property message 结果消息 / Result message
 */
@Serializable
data class SerializedSolution(
    val feasible: Boolean,
    val optimal: Boolean,
    val objectiveValue: Flt64? = null,
    val gap: Flt64? = null,
    val variableValues: List<Flt64> = emptyList(),
    @SerialName("elapsedMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val elapsed: Duration = Duration.ZERO,
    val solverStatus: String = "",
    val message: String? = null
) {
    companion object {
        /**
         * 创建不可行解。
         * Create infeasible solution.
         *
         * @param message 结果消息 / Result message
         * @return 不可行解 / Infeasible solution
         */
        fun infeasible(message: String? = null): SerializedSolution {
            return SerializedSolution(
                feasible = false,
                optimal = false,
                message = message ?: "Model is infeasible"
            )
        }

        /**
         * 创建无界解。
         * Create unbounded solution.
         *
         * @param message 结果消息 / Result message
         * @return 无界解 / Unbounded solution
         */
        fun unbounded(message: String? = null): SerializedSolution {
            return SerializedSolution(
                feasible = false,
                optimal = false,
                message = message ?: "Model is unbounded"
            )
        }

        /**
         * 创建错误解。
         * Create error solution.
         *
         * @param message 错误消息 / Error message
         * @return 错误解 / Error solution
         */
        fun error(message: String): SerializedSolution {
            return SerializedSolution(
                feasible = false,
                optimal = false,
                message = message
            )
        }
    }
}

/**
 * 转换为 LP 约束符号。
 * Convert to LP constraint sign.
 *
 * @return LP 约束符号 / LP constraint sign
 */
fun SerializedConstraintSign.toLpString(): String {
    return when (this) {
        SerializedConstraintSign.LESS_EQUAL -> "<="
        SerializedConstraintSign.GREATER_EQUAL -> ">="
        SerializedConstraintSign.EQUAL -> "="
    }
}

/**
 * 转换为 LP 目标类型。
 * Convert to LP objective category.
 *
 * @return LP 目标类型 / LP objective category
 */
fun SerializedObjectiveCategory.toLpString(): String {
    return when (this) {
        SerializedObjectiveCategory.MINIMIZE -> "Minimize"
        SerializedObjectiveCategory.MAXIMIZE -> "Maximize"
    }
}

private fun appendLinearTerm(
    builder: StringBuilder,
    first: Boolean,
    coefficient: Flt64,
    variableName: String
) {
    val coefficientValue = coefficient.toDouble()
    if (first) {
        builder.append("$coefficientValue $variableName")
    } else {
        if (coefficientValue >= 0.0) {
            builder.append(" + ")
        } else {
            builder.append(" - ")
        }
        builder.append("${kotlin.math.abs(coefficientValue)} $variableName")
    }
}
