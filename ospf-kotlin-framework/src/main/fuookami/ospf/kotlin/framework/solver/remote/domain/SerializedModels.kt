/**
 * 远程求解序列化模型 / Remote solve serialized models
*/
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 序列化变量类型。 / Serialized variable type.
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
 * 序列化约束符号。 / Serialized constraint sign.
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
 * 序列化目标类型。 / Serialized objective category.
*/
@Serializable
enum class SerializedObjectiveCategory {
    /** 最小化 / Minimize */
    MINIMIZE,

    /** 最大化 / Maximize */
    MAXIMIZE
}

/**
 * 序列化模型元素来源。 / Serialized model-element provenance entry.
 *
 * @property kind 来源类型 / Provenance kind
 * @property key 来源键 / Provenance key
 */
@Serializable
data class SerializedModelElementOrigin(
    val kind: String,
    val key: String
)

/**
 * 约束矩阵单元。 / Constraint matrix cell.
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
 * 目标函数单元。 / Objective cell.
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
 * 序列化变量。 / Serialized variable.
 *
 * @property index 变量索引 / Variable index
 * @property name 变量名 / Variable name
 * @property lowerBound 下界 / Lower bound
 * @property upperBound 上界 / Upper bound
 * @property type 变量类型 / Variable type
 * @property identityId 稳定身份 ID / Stable identity ID
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOriginKind 身份来源类型 / Identity origin kind
 * @property identityOriginKey 身份来源键 / Identity origin key
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedVariable(
    val index: Int,
    val name: String,
    val lowerBound: Flt64,
    val upperBound: Flt64,
    val type: SerializedVariableType,
    val identityId: String? = null,
    val identityScope: String = "MODEL_LOCAL",
    val identityOriginKind: String? = null,
    val identityOriginKey: String? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<SerializedModelElementOrigin> = emptyList()
)

/**
 * 序列化线性约束。 / Serialized linear constraint.
 *
 * @property cells 约束单元 / Constraint cells
 * @property sign 约束符号 / Constraint sign
 * @property rhs 右端值 / Right-hand side
 * @property name 约束名 / Constraint name
 * @property identityId 稳定身份 ID / Stable identity ID
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOriginKind 身份来源类型 / Identity origin kind
 * @property identityOriginKey 身份来源键 / Identity origin key
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedConstraint(
    val cells: List<SerializedConstraintCell>,
    val sign: SerializedConstraintSign,
    val rhs: Flt64,
    val name: String,
    val identityId: String? = null,
    val identityScope: String = "MODEL_LOCAL",
    val identityOriginKind: String? = null,
    val identityOriginKey: String? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<SerializedModelElementOrigin> = emptyList()
)

/**
 * 序列化线性目标。 / Serialized linear objective.
 *
 * @property category 目标类型 / Objective category
 * @property cells 目标单元 / Objective cells
 * @property constant 常数项 / Constant term
 * @property identityId 稳定身份 ID / Stable identity ID
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOriginKind 身份来源类型 / Identity origin kind
 * @property identityOriginKey 身份来源键 / Identity origin key
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedObjective(
    val category: SerializedObjectiveCategory,
    val cells: List<SerializedObjectiveCell>,
    val constant: Flt64 = Flt64.zero,
    val identityId: String? = null,
    val identityScope: String = "MODEL_LOCAL",
    val identityOriginKind: String? = null,
    val identityOriginKey: String? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<SerializedModelElementOrigin> = emptyList()
)

/**
 * 序列化线性模型。 / Serialized linear model.
 *
 * @property name 模型名 / Model name
 * @property variables 变量列表 / Variables
 * @property constraints 约束列表 / Constraints
 * @property objective 目标函数 / Objective
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedLinearModel(
    val name: String,
    val variables: List<SerializedVariable>,
    val constraints: List<SerializedConstraint>,
    val objective: SerializedObjective,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null
) {

    /** 变量数量 / Variable count */
    val variableCount: Int get() = variables.size

    /** 约束数量 / Constraint count */
    val constraintCount: Int get() = constraints.size

    /**
     * 导出 LP 格式。 / Export LP format.
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
         * 创建空模型。 / Create empty model.
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
 * 序列化二次模型。 / Serialized quadratic model.
 *
 * @property name 模型名 / Model name
 * @property variables 变量列表 / Variables
 * @property linearConstraints 线性约束 / Linear constraints
 * @property quadraticConstraints 二次约束 / Quadratic constraints
 * @property objective 二次目标 / Quadratic objective
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedQuadraticModel(
    val name: String,
    val variables: List<SerializedVariable>,
    val linearConstraints: List<SerializedConstraint>,
    val quadraticConstraints: List<SerializedQuadraticConstraint>,
    val objective: SerializedQuadraticObjective,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null
) {

    /** 变量数量 / Variable count */
    val variableCount: Int get() = variables.size

    /** 线性约束数量 / Linear constraint count */
    val linearConstraintCount: Int get() = linearConstraints.size

    /** 二次约束数量 / Quadratic constraint count */
    val quadraticConstraintCount: Int get() = quadraticConstraints.size
}

/**
 * 二次约束单元。 / Quadratic constraint cell.
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
 * 序列化二次约束。 / Serialized quadratic constraint.
 *
 * @property linearCells 线性单元 / Linear cells
 * @property quadraticCells 二次单元 / Quadratic cells
 * @property sign 约束符号 / Constraint sign
 * @property rhs 右端值 / Right-hand side
 * @property name 约束名 / Constraint name
 * @property identityId 稳定身份 ID / Stable identity ID
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOriginKind 身份来源类型 / Identity origin kind
 * @property identityOriginKey 身份来源键 / Identity origin key
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedQuadraticConstraint(
    val linearCells: List<SerializedConstraintCell>,
    val quadraticCells: List<SerializedQuadraticConstraintCell>,
    val sign: SerializedConstraintSign,
    val rhs: Flt64,
    val name: String,
    val identityId: String? = null,
    val identityScope: String = "MODEL_LOCAL",
    val identityOriginKind: String? = null,
    val identityOriginKey: String? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<SerializedModelElementOrigin> = emptyList()
)

/**
 * 二次目标单元。 / Quadratic objective cell.
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
 * 序列化二次目标。 / Serialized quadratic objective.
 *
 * @property category 目标类型 / Objective category
 * @property linearCells 线性目标单元 / Linear objective cells
 * @property quadraticCells 二次目标单元 / Quadratic objective cells
 * @property constant 常数项 / Constant term
 * @property identityId 稳定身份 ID / Stable identity ID
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOriginKind 身份来源类型 / Identity origin kind
 * @property identityOriginKey 身份来源键 / Identity origin key
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema version
*/
@Serializable
data class SerializedQuadraticObjective(
    val category: SerializedObjectiveCategory,
    val linearCells: List<SerializedObjectiveCell>,
    val quadraticCells: List<SerializedQuadraticObjectiveCell>,
    val constant: Flt64 = Flt64.zero,
    val identityId: String? = null,
    val identityScope: String = "MODEL_LOCAL",
    val identityOriginKind: String? = null,
    val identityOriginKey: String? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<SerializedModelElementOrigin> = emptyList()
)

/** Stable serialized interval value for CP result materialization. / CP 结果物化使用的稳定 interval 序列化值。
 *
 * @property start Interval start value. / interval 开始值。
 * @property size Interval size value. / interval 长度值。
 * @property end Interval end value. / interval 结束值。
 * @property present Whether the interval is present. / interval 是否存在。
 */
@Serializable
data class SerializedIntervalValue(
    val start: Long,
    val size: Long,
    val end: Long,
    val present: Boolean = true
)

/**
 * 序列化解。 / Serialized solution.
 *
 * @property feasible 是否可行 / Whether feasible
 * @property optimal 是否最优 / Whether optimal
 * @property objectiveValue 线性/二次兼容浮点目标；CP 结果必须为 null / Legacy floating objective for linear/quadratic results; must be null for CP
 * @property objectiveValueInt64 CP 精确整数目标值 / Exact Int64 CP objective value
 * @property gap 最优间隙 / Optimality gap
 * @property variableValues 变量值 / Variable values
 * @property variableValuesById CP 稳定变量值 / CP stable variable values by ID
 * @property intervalValues CP 稳定 interval 值 / CP stable interval values by ID
 * @property problemStatus 结果问题状态 / Problem status reported by the result artifact
 * @property solutionPresence 解存在性 / Solution presence reported by the result artifact
 * @property proofStatus 证明状态 / Proof status reported by the result artifact
 * @property terminationReason 终止原因 / Termination reason reported by the result artifact
 * @property schemaVersion 结果协议版本 / Result protocol version
 * @property provenance 脱敏执行来源 / Redacted execution provenance
 * @property fingerprints 审计指纹 / Audit fingerprints
 * @property fingerprintSchemas 指纹 schema / Fingerprint schemas
 * @property statistics 求解统计 / Solve statistics
 * @property diagnostics 结构化诊断 / Structured diagnostics
 * @property runId 求解运行标识 / Solve run identifier
 * @property attemptId 求解尝试标识 / Solve attempt identifier
 * @property artifactDigest 结果 artifact 摘要 / Result artifact digest
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
    val variableValuesById: Map<String, Long> = emptyMap(),
    val intervalValues: Map<String, SerializedIntervalValue> = emptyMap(),
    val problemStatus: RemoteProblemStatus? = null,
    val solutionPresence: RemoteSolutionPresence? = null,
    val proofStatus: RemoteProofStatus? = null,
    val terminationReason: RemoteTerminationReason? = null,
    val schemaVersion: String = "1.0",
    val provenance: Map<String, String> = emptyMap(),
    val fingerprints: Map<String, String> = emptyMap(),
    val fingerprintSchemas: Map<String, String> = emptyMap(),
    @SerialName("elapsedMs")
    @Serializable(with = RemoteSolverMillisecondsDurationSerializer::class)
    val elapsed: Duration = Duration.ZERO,
    val solverStatus: String = "",
    val message: String? = null,
    val statistics: Map<String, String> = emptyMap(),
    val diagnostics: Map<String, String> = emptyMap(),
    val runId: String? = null,
    val attemptId: String? = null,
    val artifactDigest: String? = null,
    val objectiveValueInt64: Long? = null
) {
    companion object {
        /**
         * 创建不可行解。 / Create infeasible solution.
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
         * 创建无界解。 / Create unbounded solution.
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
         * 创建错误解。 / Create error solution.
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
 * 转换为 LP 约束符号。 / Convert to LP constraint sign.
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
 * 转换为 LP 目标类型。 / Convert to LP objective category.
 *
 * @return LP 目标类型 / LP objective category
*/
fun SerializedObjectiveCategory.toLpString(): String {
    return when (this) {
        SerializedObjectiveCategory.MINIMIZE -> "Minimize"
        SerializedObjectiveCategory.MAXIMIZE -> "Maximize"
    }
}

/**
 * Appends a linear term to the string builder.
 * 将线性项追加到字符串构建器。
 *
 * @param builder 字符串构建器 / the string builder
 * @param first 是否为第一项 / whether this is the first term
 * @param coefficient 系数值 / the coefficient value
 * @param variableName 变量名 / the variable name
*/
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
