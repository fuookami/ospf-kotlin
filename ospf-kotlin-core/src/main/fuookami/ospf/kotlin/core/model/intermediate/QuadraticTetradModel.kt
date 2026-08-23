/**
 * 二次四元模型 / Quadratic tetrad model
*/
package fuookami.ospf.kotlin.core.model.intermediate

import java.io.OutputStreamWriter
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementKind
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.derivedModelElementIdentity
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BalancedTernary
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.Integer
import fuookami.ospf.kotlin.core.variable.Percentage
import fuookami.ospf.kotlin.core.variable.Ternary
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger

/**
 * 判断此二次约束是否为单变量边界约束（单项、系数为1、无二次项） / Check whether this quadratic constraint is a single-variable bound constraint (single term, coefficient 1, no quadratic term)
 *
 * @return 若为单变量边界约束则返回 true，否则返回 false / true if this is a single-variable bound constraint, false otherwise
*/
private fun QuadraticConstraintImpl<Flt64>.isBound(): Boolean {
    return lhs.size == 1
            && lhs.first().coefficient eq Flt64.one
            && lhs.first().token2 == null
    // && from?.second != true
}

/**
 * 二次约束单元 / Quadratic constraint cell
 *
 * 表示二次约束矩阵中的一个非零元素，包含行索引、列索引和系数。 / Represents a non-zero element in the quadratic constraint matrix,
 * containing row index, column indices, and coefficient.
 *
 * @property rowIndex 行索引 / Row index
 * @property colIndex1 第一列索引 / First column index
 * @property colIndex2 第二列索引（null 表示线性项）/ Second column index (null for linear term)
 * @param coefficient 系数 / Coefficient
*/
class QuadraticConstraintCell(
    override val rowIndex: Int,
    val colIndex1: Int,
    val colIndex2: Int?,
    coefficient: Flt64
) : ConstraintCell<QuadraticConstraintCell>, Cloneable, Copyable<QuadraticConstraintCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): QuadraticConstraintCell {
        return QuadraticConstraintCell(
            rowIndex = rowIndex,
            colIndex1 = colIndex1,
            colIndex2 = colIndex2,
            coefficient = -coefficient
        )
    }

    override fun copy() = QuadraticConstraintCell(
        rowIndex = rowIndex,
        colIndex1 = colIndex1,
        colIndex2 = colIndex2,
        coefficient = coefficient.copy()
    )

    override fun clone() = copy()
}

/**
 * 二次约束批次 / Quadratic constraint batch
 *
 * 存储一组二次约束的稀疏矩阵表示，包括约束符号、右侧常量和约束来源。 / Stores a batch of quadratic constraints in sparse matrix representation,
 * including constraint signs, right-hand side constants, and constraint sources.
 *
 * @property sparseLhs 稀疏二次矩阵（左侧）/ Sparse quadratic matrix (left-hand side)
 * @param signs 约束关系符号列表 / Constraint relation sign list
 * @param rhs 右侧常量列表 / Right-hand side constant list
 * @param names 约束名称列表 / Constraint name list
 * @param sources 约束来源类型列表 / Constraint source type list
 * @param origins 约束来源列表 / Constraint origin list
 * @param froms 约束来源符号列表 / Constraint from-symbol list
 * @param priorities 约束优先级列表 / Constraint priority list
 * @param ids 稳定约束 ID 列表 / Stable constraint ID list
 * @param identityScopes 每行身份作用域 / Identity scope for each row
 * @param identityOrigins 每行稳定身份来源 / Stable identity origin for each row
 * @param identityProvenance 每行完整身份来源集合 / Complete identity provenance for each row
 * @param identityMetadataValidationOverride 复制或过滤时保留的身份元数据校验结果 / Identity metadata validation result preserved across copies or filters
*/
class QuadraticConstraintBatch(
    val sparseLhs: SparseQuadraticMatrix,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    origins: List<QuadraticConstraintImpl<Flt64>?> = (0 until sparseLhs.numRows()).map { null },
    froms: List<Pair<IntermediateSymbol<*>, Boolean>?> = (0 until sparseLhs.numRows()).map { null },
    priorities: List<Int?> = (0 until sparseLhs.numRows()).map { null },
    ids: List<ConstraintId> = emptyList(),
    identityNamespace: String? = null,
    identitySchemaVersion: String? = null,
    identityScopes: List<ModelElementScope> = emptyList(),
    identityOrigins: List<ModelElementOrigin?> = emptyList(),
    identityProvenance: List<List<ModelElementOrigin>> = emptyList(),
    identityMetadataValidationOverride: Try? = null
) : ModelConstraint<QuadraticConstraintCell>(
    sparseLhs.numRows(),
    signs,
    rhs,
    names,
    sources,
    ids,
    identityNamespace,
    identitySchemaVersion,
    identityScopes,
    identityOrigins,
    identityProvenance,
    identityMetadataValidationOverride
) {

    /**
     * 二次左侧矩阵的稀疏表示。
     * 每一行是一个 SparseQuadraticVector，其中条目携带 (colIndex1, colIndex2?, coefficient)。
     * 这是主要的约束表示形式。 / Sparse representation of the quadratic LHS matrix.
     * Each row is a SparseQuadraticVector where entries carry (colIndex1, colIndex2?, coefficient).
     * This is the primary constraint representation.
    */
    override val lhs: List<List<QuadraticConstraintCell>> by lazy {
        sparseLhs.rows.mapIndexed { rowIndex, row ->
            row.entries.map { entry ->
                QuadraticConstraintCell(
                    rowIndex = rowIndex,
                    colIndex1 = entry.colIndex1,
                    colIndex2 = entry.colIndex2,
                    coefficient = entry.coefficient
                )
            }
        }
    }

    private val _origins: MutableList<QuadraticConstraintImpl<Flt64>?> = origins.toMutableList()
    val origins: List<QuadraticConstraintImpl<Flt64>?> by ::_origins

    private val _froms: MutableList<Pair<IntermediateSymbol<*>, Boolean>?> = froms.toMutableList()
    val froms: List<Pair<IntermediateSymbol<*>, Boolean>?> by ::_froms

    private val _priorities: MutableList<Int?> = priorities.toMutableList()
    val priorities: List<Int?> by ::_priorities

    override fun copy() = QuadraticConstraintBatch(
        SparseQuadraticMatrix().also { mat ->
            for (row in sparseLhs.rows) {
                val newRow = SparseQuadraticVector()
                for (entry in row.entries) {
                    newRow.add(entry.colIndex1, entry.colIndex2, entry.coefficient.copy())
                }
                mat.addRow(newRow)
            }
        },
        signs.toList(),
        rhs.map { it.copy() },
        names.toList(),
        sources.toList(),
        origins.toList(),
        froms.toList(),
        priorities.toList(),
        ids.toList(),
        identityNamespace,
        identitySchemaVersion,
        identityScopes.toList(),
        identityOrigins.toList(),
        identityProvenance.map { it.toList() },
        identityMetadataValidation
    )

    override fun close() {
        _origins.clear()
        _froms.clear()
        _priorities.clear()
        super.close()
    }
}

/**
 * 二次目标单元 / Quadratic objective cell
 *
 * 表示二次目标函数中的一个非零元素，包含列索引和系数。 / Represents a non-zero element in the quadratic objective function,
 * containing column indices and coefficient.
 *
 * @property colIndex1 第一列索引 / First column index
 * @property colIndex2 第二列索引（null 表示线性项）/ Second column index (null for linear term)
 * @param coefficient 系数 / Coefficient
*/
class QuadraticObjectiveCell(
    val colIndex1: Int,
    val colIndex2: Int?,
    coefficient: Flt64
) : ModelCell<QuadraticObjectiveCell>, Cloneable, Copyable<QuadraticObjectiveCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): QuadraticObjectiveCell {
        return QuadraticObjectiveCell(
            colIndex1 = colIndex1,
            colIndex2 = colIndex2,
            coefficient = -coefficient
        )
    }

    override fun copy() = QuadraticObjectiveCell(
        colIndex1 = colIndex1,
        colIndex2 = colIndex2,
        coefficient = coefficient.copy()
    )

    override fun clone() = copy()
}

/**
 * 二次目标函数类型别名 / Type alias for quadratic objective function
*/
typealias QuadraticObjective = Objective<QuadraticObjectiveCell>

/**
 * 基础二次四元模型 / Basic quadratic tetrad model
 *
 * 二次问题的求解器标准形式（四元：变量 + 约束，无目标函数）。
 * 直接用于 IIS（不可约不可行子系统）计算，以及作为 [QuadraticTetradModel] 的 [impl] 委托。
 * Solver-standard form for quadratic problems (tetrad: variables + constraints, no objective).
 * Used directly by IIS (Irreducible Infeasible Subsystem) computation and
 * as the [impl] delegate inside [QuadraticTetradModel].
 *
 * ### 构造方式 / Construction
 *
 * 直接构造 / Direct constructor:
 * ```kotlin
 * BasicQuadraticTetradModel(variables, constraints, name)
 * ```
 *
 * 从 QuadraticMechanismModel 工厂方法 / Factory from QuadraticMechanismModel:
 * ```kotlin
 * BasicQuadraticTetradModel.from(mechanismModel, tokenIndexMap, bounds, fixedVariables)
 * ```
 *
 * ### 与 QuadraticTetradModel 的关系 / Relationship to [QuadraticTetradModel]
 *
 * [QuadraticTetradModel] 包装 [BasicQuadraticTetradModel] 作为其 `impl`，添加目标函数和符号到求解器的映射。
 * [QuadraticTetradModel] wraps a [BasicQuadraticTetradModel] as its `impl`, adding
 * objective function and token-to-solver mapping.
 *
 * @property variables 求解器索引的变量列表 / Solver-indexed variable list
 * @property constraints 二次约束批次 / Quadratic constraint batch
*/
class BasicQuadraticTetradModel(
    override val variables: List<Variable>,
    override val constraints: QuadraticConstraintBatch,
    override val name: String
) : BasicModelView<QuadraticConstraintCell>, Cloneable, Copyable<BasicQuadraticTetradModel> {
    companion object {
        /**
         * 从 [QuadraticMechanismModel<Flt64>] 创建 [BasicQuadraticTetradModel]，
         * 将变量和约束提取到求解器标准形式。
         *
         * 这是一个便捷工厂方法，镜像了 [QuadraticTetradModel.invoke] 中的变量/约束提取逻辑，
         * 但不包含目标函数步骤。 / Create a [BasicQuadraticTetradModel] from a [QuadraticMechanismModel<Flt64>] by
         * extracting variables and constraints into solver-standard form.
         *
         * This is a convenience factory that mirrors the variable/constraint extraction
         * logic in [QuadraticTetradModel.invoke] without the objective function step.
         *
         * @param model           源机制模型 / the source mechanism model
         * @param tokenIndexMap   符号到求解器列索引的映射 / mapping from tokens to solver column indices
         * @param bounds          每个符号的预计算边界约束 / pre-computed bound constraints per token
         * @param fixedVariables  固定为常量值的变量（被替换掉）/ variables fixed to constant values (substituted out)
         * @param identityRegistry 可选的稳定身份注册表 / optional stable identity registry
         * @return 包含提取的变量和约束的 [BasicQuadraticTetradModel] / a [BasicQuadraticTetradModel] containing the extracted variables and constraints
        */
        fun from(
            model: QuadraticMechanismModel<Flt64>,
            tokenIndexMap: Map<Token<Flt64>, Int>,
            bounds: Map<Token<Flt64>, List<Quadruple<QuadraticConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>> = emptyMap(),
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            identityRegistry: ModelElementIdentityRegistry? = model.identityRegistry
        ): BasicQuadraticTetradModel {
            val variables = dumpQuadraticTetradVariables(
                tokenIndexes = tokenIndexMap,
                bounds = bounds,
                identityRegistry = identityRegistry
            )
            val constraints = dumpQuadraticTetradConstraints(
                model = model,
                tokenIndexes = tokenIndexMap,
                bounds = bounds,
                fixedVariables = fixedVariables,
                identityRegistry = identityRegistry
            )
            return BasicQuadraticTetradModel(variables, constraints, model.name)
        }
    }
    override fun copy() = BasicQuadraticTetradModel(
        variables.map { it.copy() },
        constraints.copy(),
        name
    )

    override fun clone() = copy()

    /**
     * 就地线性松弛 / In-place linear relaxation
     *
     * 将整数变量类型松弛为连续类型（Binary->Percentage, Integer->Continuous 等）。
     * Relaxes integer variable types to continuous types (Binary->Percentage, Integer->Continuous, etc.).
    */
    fun linearRelax() {
        variables.forEach {
            when (it.type) {
                is Binary -> {
                    it._type = Percentage
                }

                is Ternary, is UInteger -> {
                    it._type = UContinuous
                }

                is BalancedTernary, is Integer -> {
                    it._type = Continuous
                }

                else -> {}
            }
        }
    }

    /**
     * 返回线性松弛后的副本 / Return a linearly relaxed copy
     *
     * @return 线性松弛后的模型副本 / Linearly relaxed model copy
    */
    fun linearRelaxed(): BasicQuadraticTetradModel {
        return BasicQuadraticTetradModel(
            variables = variables.map {
                when (it.type) {
                    is Binary -> {
                        val ret = it.copy()
                        ret._type = Percentage
                        ret
                    }

                    is Ternary, is UInteger -> {
                        val ret = it.copy()
                        ret._type = UContinuous
                        ret
                    }

                    is BalancedTernary, is Integer -> {
                        val ret = it.copy()
                        ret._type = Continuous
                        ret
                    }

                    else -> it.copy()
                }
            },
            constraints = constraints.copy(),
            name = name
        )
    }
    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.append("Subject To\n")
        for (i in constraints.indices) {
            writer.append(" ${constraints.names[i]}: ")
            var flag = false
            for (j in constraints.lhs[i].indices) {
                if (constraints.lhs[i][j].coefficient eq Flt64.zero) {
                    continue
                }

                val coefficient = if (flag) {
                    if (constraints.lhs[i][j].coefficient leq Flt64.zero) {
                        writer.append(" - ")
                    } else {
                        writer.append(" + ")
                    }
                    abs(constraints.lhs[i][j].coefficient)
                } else {
                    constraints.lhs[i][j].coefficient
                }
                if (coefficient neq Flt64.zero) {
                    if (coefficient neq Flt64.one) {
                        writer.append("$coefficient ")
                    }
                    if (constraints.lhs[i][j].colIndex2 == null) {
                        writer.append("${variables[constraints.lhs[i][j].colIndex1]}")
                    } else {
                        writer.append("${variables[constraints.lhs[i][j].colIndex1]} * ${variables[constraints.lhs[i][j].colIndex2!!]}")
                    }
                }
                flag = true
            }
            if (!flag) {
                writer.append("0")
            }
            writer.append(" ${constraints.signs[i]} ${constraints.rhs[i]}\n")
        }
        writer.append("\n")

        writer.append("Bounds\n")
        for (variable in variables) {
            val lowerInf = variable.lowerBound.isNegativeInfinity()
            val upperInf = variable.upperBound.isInfinity()
            if (lowerInf && upperInf) {
                writer.append(" $variable free\n")
            } else if (lowerInf) {
                writer.append(" $variable <= ${variable.upperBound}\n")
            } else if (upperInf) {
                writer.append(" $variable >= ${variable.lowerBound}\n")
            } else {
                if (variable.lowerBound eq variable.upperBound) {
                    writer.append(" $variable = ${variable.lowerBound}\n")
                } else {
                    writer.append(" ${variable.lowerBound} <= $variable <= ${variable.upperBound}\n")
                }
            }
        }
        writer.append("\n")

        if (containsBinary) {
            writer.append("Binaries\n")
            for (variable in variables) {
                if (variable.type.isBinaryType) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        if (containsNotBinaryInteger) {
            writer.append("Generals\n")
            for (variable in variables) {
                if (variable.type.isNotBinaryIntegerType) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        writer.append("End\n")
        return ok
    }
}

/**
 * 二次四元模型视图接口 / Quadratic tetrad model view interface
 *
 * 提供二次模型的视图操作，包括线性松弛、对偶、可行性和弹性模型。 / Provides view operations for quadratic models, including linear relaxation, dual, feasibility, and elastic models.
*/
interface QuadraticTetradModelView : ModelView<QuadraticConstraintCell, QuadraticObjectiveCell> {
    override val constraints: QuadraticConstraintBatch
    val dual: Boolean

    /**
     * Identity validation captured while building the intermediate model.
     * 中间模型构建期间捕获的身份校验结果。
     */
    val identityValidation: Try
        get() = ok

    /** 就地线性松弛（修改当前模型） / In-place linear relaxation (mutates current model)
     * @return 线性松弛后的模型视图 / The linearly relaxed model view
    */
    fun linearRelax(): QuadraticTetradModelView

    /** 返回线性松弛后的副本 / Return a linearly relaxed copy
     * @return 线性松弛后的模型视图副本 / The linearly relaxed model view copy
    */
    fun linearRelaxed(): QuadraticTetradModelView

    /** 构造对偶模型 / Construct the dual model
     * @return 对偶二次四元模型 / The dual quadratic tetrad model
    */
    suspend fun dual(): QuadraticTetradModel

    /** 构造 Farkas 对偶模型 / Construct the Farkas dual model
     * @return Farkas 对偶二次四元模型 / The Farkas dual quadratic tetrad model
    */
    suspend fun farkasDual(): QuadraticTetradModel

    /** 构造可行性模型 / Construct the feasibility model
     * @return 可行性二次四元模型视图 / The feasibility quadratic tetrad model view
    */
    fun feasibility(): QuadraticTetradModelView

    /** 构造弹性模型 / Construct the elastic model
     * @return 弹性二次四元模型视图 / The elastic quadratic tetrad model view
    */
    fun elastic(): QuadraticTetradModelView

    /**
     * 整理对偶解，将完整对偶值（包括零值）映射回原始约束 / Tidy dual solution, mapping complete dual values (including zero) back to original constraints
     *
     * @param solution 求解器返回的对偶解向量 / Dual solution vector returned by the solver
     * @return 原始约束到对偶值的映射 / Mapping from original constraints to dual values
    */
    fun tidyDualSolution(solution: List<Flt64>): kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64> {
        return if (dual) {
            variables.associateNotNull {
                if (it.dualOrigin != null && solution.size > it.index) {
                    (it.dualOrigin as QuadraticConstraintImpl<Flt64>) to solution[it.index]
                } else {
                    null
                }
            }
        } else {
            constraints.indices.associateNotNull {
                if (constraints.origins[it] != null && solution.size > it) {
                    constraints.origins[it]!! to solution[it]
                } else {
                    null
                }
            }
        }
    }
}

/**
 * 二次四元模型 / Quadratic tetrad model
 *
 * 求解器标准形式的二次优化模型，包含变量、约束和目标函数。 / Solver-standard form of quadratic optimization model, containing variables, constraints, and objective function.
 *
 * @property impl 基础模型实现 / Basic model implementation
 * @property tokensInSolver 求解器中的符号列表 / Token list in solver
 * @property objective 目标函数 / Objective function
 * @property dualOrigin 对偶模型来源 / Dual model origin
*/
data class QuadraticTetradModel(
    private val impl: BasicQuadraticTetradModel,
    val tokensInSolver: List<Token<Flt64>>,
    override val objective: QuadraticObjective,
    internal val dualOrigin: QuadraticTetradModelView? = null,
    override val identityValidation: Try = validateDerivedIdentitySet(
        variables = impl.variables,
        constraints = impl.constraints,
        objective = objective
    )
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraintBatch by impl::constraints
    override val name: String by impl::name
    override val dual get() = dualOrigin != null

    companion object {
        private val logger = logger()

        /**
         * V->Flt64 转换边界：泛型 V 在二次中间模型构造时解析为具体类型 Flt64。 /
         * V->Flt64 conversion boundary: generic V resolves to concrete Flt64 for quadratic intermediate model construction.
         *
         * @param model 源二次机制模型 / Source quadratic mechanism model
         * @param fixedVariables 可选的固定变量 / Optional fixed variables
         * @param dumpConstraintsToBounds 是否转储边界约束 / Whether to dump bound constraints
         * @param forceDumpBounds 是否强制转储可识别边界 / Whether to force recognizable bounds
         * @param concurrent 是否并行转储 / Whether to dump concurrently
         * @param identityRegistry 可选的稳定身份注册表 / Optional stable identity registry
         * @return 二次四元模型 / Quadratic tetrad model
         */
        suspend operator fun invoke(
            model: QuadraticMechanismModel<Flt64>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            dumpConstraintsToBounds: Boolean? = null,
            forceDumpBounds: Boolean? = null,
            concurrent: Boolean? = null,
            identityRegistry: ModelElementIdentityRegistry? = model.identityRegistry
        ): QuadraticTetradModel {
            logger.trace("Creating QuadraticTetradModel for $model")
            val tokensInSolver = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokensInSolver
            } else {
                model.tokens.tokensInSolverWithout(fixedVariables.keys)
            }
            val tokenIndexMap = tokensInSolver.withIndex().associate { (index, token) -> token to index }
            val bounds = model.quadraticConstraints
                .flatMap { constraint ->
                    if ((dumpConstraintsToBounds ?: true) && constraint.isBound()) {
                        listOf(Quadruple(constraint, constraint.lhs.first().token1, constraint.sign, constraint.rhs))
                    } else if (forceDumpBounds ?: false) {
                        if (constraint.lhs.size == 1 && constraint.lhs.first().token2 == null) {
                            listOf(Quadruple(constraint, constraint.lhs.first().token1, constraint.sign, constraint.rhs / constraint.lhs.first().coefficient))
                        } else if (constraint.lhs.all { it.coefficient eq Flt64.one && it.token2 == null && it.token1.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == ConstraintRelation.LessEqual || constraint.sign == ConstraintRelation.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token1, ConstraintRelation.Equal, Flt64.zero) }
                        } else if (constraint.lhs.all { it.coefficient eq -Flt64.one && it.token2 == null && it.token1.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == ConstraintRelation.GreaterEqual || constraint.sign == ConstraintRelation.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token1, ConstraintRelation.Equal, Flt64.zero) }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }.groupBy { it.second }
            val tetradModel = if (concurrent ?: model.concurrent) {
                coroutineScope {
                    val variablePromise = async(Dispatchers.Default) {
                        dumpQuadraticTetradVariables(
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            identityRegistry = identityRegistry
                        )
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpQuadraticTetradConstraintsAsync(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables,
                            identityRegistry = identityRegistry
                        )
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpQuadraticTetradObjectives(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            fixedVariables = fixedVariables,
                            identityRegistry = identityRegistry
                        )
                    }

                    QuadraticTetradModel(
                        impl = BasicQuadraticTetradModel(
                            variables = variablePromise.await(),
                            constraints = constraintPromise.await(),
                            name = model.name
                        ),
                        tokensInSolver = tokensInSolver,
                        objective = objectivePromise.await()
                    )
                }
            } else {
                QuadraticTetradModel(
                    impl = BasicQuadraticTetradModel(
                        variables = dumpQuadraticTetradVariables(
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            identityRegistry = identityRegistry
                        ),
                        constraints = dumpQuadraticTetradConstraints(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables,
                            identityRegistry = identityRegistry
                        ),
                        name = model.name
                    ),
                    tokensInSolver = tokensInSolver,
                    objective = dumpQuadraticTetradObjectives(
                        model = model,
                        tokenIndexes = tokenIndexMap,
                        fixedVariables = fixedVariables,
                        identityRegistry = identityRegistry
                    )
                )
            }

            val identityValidation = combineIdentityValidation(
                materializedValidation = validateDerivedIdentitySet(
                    variables = tetradModel.variables,
                    constraints = tetradModel.constraints,
                    objective = tetradModel.objective,
                    allowGeneratedArtifactPrefix = true
                ),
                registryValidation = identityRegistry?.validate()
            )
            val validatedTetradModel = tetradModel.copy(identityValidation = identityValidation)
            logger.trace("QuadraticTetradModel created for $model")
            MemoryCleanupPolicy.cleanupAfterModelBuilt()
            return validatedTetradModel
        }
    }

    override fun copy() = QuadraticTetradModel(
        impl = impl.copy(),
        tokensInSolver = tokensInSolver,
        objective = objective.copy(),
        identityValidation = identityValidation
    )

    override fun clone() = copy()

    /**
     * Return the identity validation result captured during model construction. /
     * 返回模型构建期间捕获的身份校验结果。
     *
     * @return Structured identity validation result. / 结构化身份校验结果。
     */
    fun validateIdentity(): Try = identityValidation

    override fun linearRelax(): QuadraticTetradModel {
        impl.linearRelax()
        return this
    }

    override fun linearRelaxed(): QuadraticTetradModel {
        return QuadraticTetradModel(
            impl = impl.linearRelaxed(),
            tokensInSolver = tokensInSolver,
            objective = objective.copy(),
            identityValidation = identityValidation
        )
    }
    override suspend fun dual(): QuadraticTetradModel {
        val dualVariables = this.constraints.indices.map {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (this.objective.category) {
                ObjectCategory.Maximum -> {
                    when (this.constraints.signs[it]) {
                        ConstraintRelation.LessEqual -> {
                            lowerBound = Flt64.zero
                        }

                        ConstraintRelation.GreaterEqual -> {
                            upperBound = Flt64.zero
                        }

                        else -> {}
                    }
                }

                ObjectCategory.Minimum -> {
                    when (this.constraints.signs[it]) {
                        ConstraintRelation.LessEqual -> {
                            upperBound = Flt64.zero
                        }

                        ConstraintRelation.GreaterEqual -> {
                            lowerBound = Flt64.zero
                        }

                        else -> {}
                    }
                }
            }

            Variable(
                index = it,
                lowerBound = lowerBound,
                upperBound = upperBound,
                type = Continuous,
                origin = null,
                dualOrigin = this.constraints.origins[it],
                slack = null,
                name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_dual",
                initialResult = Flt64.zero
            )
        }.mapIndexed { index, variable ->
            val sourceId = this.constraints.ids.getOrNull(index)?.value
            variable.withDerivedIdentity(
                role = "dual-constraint",
                sourceId = sourceId,
                sourceScope = this.constraints.identityScopeAt(index),
                sourceOrigin = this.constraints.identityOriginAt(index),
                sourceProvenance = this.constraints.identityProvenanceOrOriginAt(index),
                namespace = this.constraints.identityNamespace,
                schemaVersion = this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "0", index.toString())
            )
        }
        var colIndex = this.constraints.size
        val boundDualVariables = this.variables.map {
            when (this.objective.category) {
                ObjectCategory.Maximum -> {
                    if (it.negativeNormalized || it.positiveNormalized || it.free) {
                        null to null
                    } else if (it.positiveFree) {
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable to null
                    } else if (it.negativeFree) {
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        null to variable
                    } else {
                        val variable1 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        val variable2 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable1 to variable2
                    }
                }

                ObjectCategory.Minimum -> {
                    if (it.negativeNormalized || it.positiveNormalized || it.free) {
                        null to null
                    } else if (it.positiveFree) {
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable to null
                    } else if (it.negativeFree) {
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        null to variable
                    } else {
                        val variable1 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        val variable2 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable1 to variable2
                    }
                }
            }
        }.mapIndexed { sourceIndex, pair ->
            val source = this.variables[sourceIndex]
            val sourceId = source.id?.value
            pair.first?.withDerivedIdentity(
                role = "dual-bound",
                sourceId = sourceId,
                sourceScope = source.identityScope,
                sourceOrigin = source.identityOrigin,
                sourceProvenance = source.identityProvenanceOrOrigin(),
                namespace = source.identityNamespace,
                schemaVersion = source.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "lower", "$sourceIndex:lower")
            ) to pair.second?.withDerivedIdentity(
                role = "dual-bound",
                sourceId = sourceId,
                sourceScope = source.identityScope,
                sourceOrigin = source.identityOrigin,
                sourceProvenance = source.identityProvenanceOrOrigin(),
                namespace = source.identityNamespace,
                schemaVersion = source.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "upper", "$sourceIndex:upper")
            )
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex1 }
        val coefficients = this@QuadraticTetradModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }
        val lhs = coroutineScope {
            val constraintPromises = this@QuadraticTetradModel.variables.indices.map { col ->
                async(Dispatchers.Default) {
                    coefficients[col].map { cell ->
                        QuadraticConstraintCell(
                            rowIndex = col,
                            colIndex1 = cell.first,
                            colIndex2 = null,
                            coefficient = cell.second
                        )
                    } + listOfNotNull(boundDualVariables[col].first, boundDualVariables[col].second).map {
                        QuadraticConstraintCell(
                            rowIndex = col,
                            colIndex1 = it.index,
                            colIndex2 = null,
                            coefficient = Flt64.one
                        )
                    }
                }
            }
            constraintPromises.awaitAll()
        }
        val signs = this.variables.map {
            if (!it.normalized) {
                ConstraintRelation.Equal
            } else if (it.negativeNormalized) {
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        ConstraintRelation.LessEqual
                    }

                    ObjectCategory.Minimum -> {
                        ConstraintRelation.GreaterEqual
                    }
                }
            } else if (it.positiveNormalized) {
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        ConstraintRelation.GreaterEqual
                    }

                    ObjectCategory.Minimum -> {
                        ConstraintRelation.LessEqual
                    }
                }
            } else {
                ConstraintRelation.Equal
            }
        }
        val rhs = this.variables.map { col ->
            this.objective.objective.find { it.colIndex1 == col.index && it.colIndex2 == null }?.coefficient ?: Flt64.zero
        }
        val names = this.variables.map { "${it.name}_dual" }
        val sources = this.variables.map { ConstraintSource.Dual }

        val objective = constraints.indices.map {
            QuadraticObjectiveCell(
                colIndex1 = it,
                colIndex2 = null,
                coefficient = this.constraints.rhs[it]
            )
        } + boundDualVariables.flatMapIndexed { col, (lb, ub) ->
            listOfNotNull(
                lb?.let {
                    QuadraticObjectiveCell(
                        colIndex1 = lb.index,
                        colIndex2 = null,
                        coefficient = this.variables[col].lowerBound
                    )
                },
                ub?.let {
                    QuadraticObjectiveCell(
                        colIndex1 = it.index,
                        colIndex2 = null,
                        coefficient = this.variables[col].upperBound
                    )
                }
            )
        }

        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = (dualVariables + boundDualVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = QuadraticConstraintBatch(
                    sparseLhs = buildQuadraticSparseLhs(lhs),
                    signs = signs,
                    rhs = rhs,
                    names = names,
                    sources = sources,
                    ids = this.variables.mapIndexed { index, variable ->
                        val sourceId = variable.id?.value
                        ConstraintId(
                            derivedModelElementIdentity(
                                kind = ModelElementKind.Constraint,
                                role = "dual-balance",
                                sourceId = sourceId,
                                sourceScope = variable.identityScope,
                                sourceOrigin = variable.identityOrigin,
                                sourceProvenance = variable.identityProvenanceOrOrigin(),
                                namespace = variable.identityNamespace,
                                schemaVersion = variable.identitySchemaVersion,
                                discriminator = derivedDiscriminator(sourceId, "0", index.toString())
                            ).id.value
                        )
                    },
                    identityNamespace = this.identityNamespace,
                    identitySchemaVersion = this.identitySchemaVersion,
                    identityScopes = this.variables.map { variable ->
                        if (variable.id == null) ModelElementScope.ModelLocal else variable.identityScope
                    },
                    identityOrigins = this.variables.map { it.identityOrigin },
                    identityProvenance = this.variables.map { it.identityProvenanceOrOrigin() }
                ),
                name = "$name-dual"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(
                category = this.objective.category.reverse,
                objective = objective,
                id = ObjectiveId(
                    derivedModelElementIdentity(
                        kind = ModelElementKind.Objective,
                        role = "dual-objective",
                        sourceId = this.objective.id?.value,
                        sourceScope = this.objective.identityScope,
                        sourceOrigin = this.objective.identityOrigin,
                        sourceProvenance = this.objective.identityProvenanceOrOrigin(),
                        namespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
                        schemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion
                    ).id.value
                ),
                identityScope = if (this.objective.id == null) {
                    ModelElementScope.ModelLocal
                } else {
                    this.objective.identityScope
                },
                identityOrigin = if (this.objective.id == null) null else this.objective.identityOrigin,
                identityNamespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
                identitySchemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                identityProvenance = this.objective.identityProvenanceOrOrigin()
            ),
            dualOrigin = this,
            identityValidation = ok
        ).withDerivedIdentityValidation()
    }
    override suspend fun farkasDual(): QuadraticTetradModel {
        var colIndex = this.constraints.size
        val farkasVariables = ArrayList<Variable>()
        val posFarkasVariables = ArrayList<Variable>()
        val negFarkasVariables = ArrayList<Variable>()
        val slackVariables = ArrayList<Variable>()
        for (i in this.constraints.indices) {
            when (this.constraints.signs[i]) {
                ConstraintRelation.LessEqual -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = this.constraints.origins[i],
                        slack = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)
                    posFarkasVariables.add(variable)
                }

                ConstraintRelation.GreaterEqual -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64.zero,
                        type = Continuous,
                        origin = null,
                        dualOrigin = this.constraints.origins[i],
                        slack = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)
                    negFarkasVariables.add(variable)
                }

                ConstraintRelation.Equal -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = this.constraints.origins[i],
                        slack = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)

                    val posSlack = Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            constraint = this.constraints.origins[i]
                        ),
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_pos_slack",
                        initialResult = Flt64.zero
                    )
                    colIndex += 1

                    val negSlack = Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            constraint = this.constraints.origins[i]
                        ),
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_neg_slack",
                        initialResult = Flt64.zero
                    )
                    colIndex += 1

                    slackVariables.add(posSlack)
                    slackVariables.add(negSlack)
                }
            }
        }
        val boundVariables = this.variables.map {
            if (it.free) {
                null to null
            } else if (it.positiveFree) {
                val variable = Variable(
                    index = colIndex,
                    lowerBound = Flt64.negativeInfinity,
                    upperBound = Flt64.zero,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_lb_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                variable to null
            } else if (it.negativeFree) {
                val variable = Variable(
                    index = colIndex,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.infinity,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_ub_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                null to variable
            } else {
                val variable1 = Variable(
                    index = colIndex,
                    lowerBound = Flt64.negativeInfinity,
                    upperBound = Flt64.zero,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_lb_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                val variable2 = Variable(
                    index = colIndex,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.infinity,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_ub_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                variable1 to variable2
            }
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex1 }
        val coefficients = this@QuadraticTetradModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }

        val lhs = coroutineScope {
            val constraintPromises = this@QuadraticTetradModel.variables.indices.map { col ->
                async(Dispatchers.Default) {
                    coefficients[col].map { cell ->
                        QuadraticConstraintCell(
                            rowIndex = col,
                            colIndex1 = cell.first,
                            colIndex2 = null,
                            coefficient = cell.second
                        )
                    } + listOfNotNull(boundVariables[col].first, boundVariables[col].second).map {
                        QuadraticConstraintCell(
                            rowIndex = col,
                            colIndex1 = it.index,
                            colIndex2 = null,
                            coefficient = Flt64.one
                        )
                    }
                }
            } + listOf(async(Dispatchers.Default) {
                this@QuadraticTetradModel.constraints.indices.map {
                    QuadraticConstraintCell(
                        rowIndex = this@QuadraticTetradModel.variables.size,
                        colIndex1 = farkasVariables[it].index,
                        colIndex2 = null,
                        coefficient = this@QuadraticTetradModel.constraints.rhs[it]
                    )
                } + this@QuadraticTetradModel.variables.flatMapIndexed { col, variable ->
                    listOfNotNull(
                        boundVariables[col].first?.let {
                            QuadraticConstraintCell(
                                rowIndex = this@QuadraticTetradModel.variables.size,
                                colIndex1 = it.index,
                                colIndex2 = null,
                                coefficient = variable.lowerBound
                            )
                        },
                        boundVariables[col].second?.let {
                            QuadraticConstraintCell(
                                rowIndex = this@QuadraticTetradModel.variables.size,
                                colIndex1 = it.index,
                                colIndex2 = null,
                                coefficient = variable.upperBound
                            )
                        }
                    )
                }
            })
            val slackConstraintPromises = async(Dispatchers.Default) {
                var rowIndex = this@QuadraticTetradModel.variables.size + 1
                var i = 0
                this@QuadraticTetradModel.constraints.indices.mapNotNull {
                    when (this@QuadraticTetradModel.constraints.signs[it]) {
                        ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                            null
                        }

                        ConstraintRelation.Equal -> {
                            val result = listOf(
                                QuadraticConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex1 = farkasVariables[it].index,
                                    colIndex2 = null,
                                    coefficient = Flt64.one
                                ),
                                QuadraticConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex1 = slackVariables[2 * i].index,
                                    colIndex2 = null,
                                    coefficient = -Flt64.one
                                ),
                                QuadraticConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex1 = slackVariables[2 * i + 1].index,
                                    colIndex2 = null,
                                    coefficient = Flt64.one
                                )
                            )
                            i += 1
                            rowIndex += 1
                            result
                        }
                    }
                }
            }
            constraintPromises.awaitAll() + slackConstraintPromises.await()
        }

        val signs = this.variables.indices.map { ConstraintRelation.Equal } + listOf(ConstraintRelation.Equal) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                    null
                }

                ConstraintRelation.Equal -> {
                    ConstraintRelation.Equal
                }
            }
        }
        val rhs = this.variables.indices.map { Flt64.zero } + listOf(-Flt64.one) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                    null
                }

                ConstraintRelation.Equal -> {
                    Flt64.zero
                }
            }
        }
        val names = this.variables.map { "${it.name}_farkas_dual" } + listOf("normalization") + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                    null
                }

                ConstraintRelation.Equal -> {
                    "${this.constraints.names[it].ifEmpty { "cons${it}" }}_abs"
                }
            }
        }
        val sources = this.variables.map { ConstraintSource.FarkasDual } + listOf(ConstraintSource.FarkasDual) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                    null
                }

                ConstraintRelation.Equal -> {
                    ConstraintSource.FarkasDual
                }
            }
        }

        val objective = posFarkasVariables.map {
            QuadraticObjectiveCell(
                colIndex1 = it.index,
                colIndex2 = null,
                coefficient = Flt64.one
            )
        } + negFarkasVariables.map {
            QuadraticObjectiveCell(
                colIndex1 = it.index,
                colIndex2 = null,
                coefficient = -Flt64.one
            )
        } + slackVariables.map {
            QuadraticObjectiveCell(
                colIndex1 = it.index,
                colIndex2 = null,
                coefficient = Flt64.one
            )
        }

        val derivedFarkasVariables = farkasVariables.mapIndexed { index, variable ->
            val source = this.constraints
            val sourceId = source.ids.getOrNull(index)?.value
            variable.withDerivedIdentity(
                role = "farkas-constraint",
                sourceId = sourceId,
                sourceScope = source.identityScopeAt(index),
                sourceOrigin = source.identityOriginAt(index),
                sourceProvenance = source.identityProvenanceOrOriginAt(index),
                namespace = source.identityNamespace,
                schemaVersion = source.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "0", index.toString())
            )
        }
        val derivedSlackVariables = slackVariables.mapIndexed { index, variable ->
            val sourceIndex = this.constraints.indices
                .filter { this.constraints.signs[it] == ConstraintRelation.Equal }[index / 2]
            val source = this.constraints
            val sourceId = source.ids.getOrNull(sourceIndex)?.value
            variable.withDerivedIdentity(
                role = "farkas-slack",
                sourceId = sourceId,
                sourceScope = source.identityScopeAt(sourceIndex),
                sourceOrigin = source.identityOriginAt(sourceIndex),
                sourceProvenance = source.identityProvenanceOrOriginAt(sourceIndex),
                namespace = source.identityNamespace,
                schemaVersion = source.identitySchemaVersion,
                discriminator = derivedDiscriminator(
                    sourceId,
                    if (index % 2 == 0) "positive" else "negative",
                    "$sourceIndex:${if (index % 2 == 0) "positive" else "negative"}"
                )
            )
        }
        val derivedBoundVariables = boundVariables.mapIndexed { index, pair ->
            val source = this.variables[index]
            val sourceId = source.id?.value
            pair.first?.withDerivedIdentity(
                role = "farkas-bound",
                sourceId = sourceId,
                sourceScope = source.identityScope,
                sourceOrigin = source.identityOrigin,
                sourceProvenance = source.identityProvenanceOrOrigin(),
                namespace = source.identityNamespace ?: this.constraints.identityNamespace,
                schemaVersion = source.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "lower", "$index:lower")
            ) to pair.second?.withDerivedIdentity(
                role = "farkas-bound",
                sourceId = sourceId,
                sourceScope = source.identityScope,
                sourceOrigin = source.identityOrigin,
                sourceProvenance = source.identityProvenanceOrOrigin(),
                namespace = source.identityNamespace ?: this.constraints.identityNamespace,
                schemaVersion = source.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "upper", "$index:upper")
            )
        }
        val constraintIds = this.variables.mapIndexed { index, variable ->
            val sourceId = variable.id?.value
            ConstraintId(
                derivedModelElementIdentity(
                    kind = ModelElementKind.Constraint,
                    role = "farkas-balance",
                    sourceId = sourceId,
                    sourceScope = variable.identityScope,
                    sourceOrigin = variable.identityOrigin,
                    sourceProvenance = variable.identityProvenanceOrOrigin(),
                    namespace = this.identityNamespace ?: this.constraints.identityNamespace,
                    schemaVersion = this.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                    discriminator = derivedDiscriminator(sourceId, "0", index.toString())
                ).id.value
            )
        } + listOf(
            ConstraintId(
                derivedModelElementIdentity(
                    kind = ModelElementKind.Constraint,
                    role = "farkas-normalization",
                    sourceId = null,
                    sourceScope = ModelElementScope.ModelLocal,
                    sourceOrigin = null,
                    namespace = this.identityNamespace ?: this.constraints.identityNamespace,
                    schemaVersion = this.identitySchemaVersion ?: this.constraints.identitySchemaVersion
                ).id.value
            )
        ) + this.constraints.indices.filter { this.constraints.signs[it] == ConstraintRelation.Equal }.map { index ->
            val sourceId = this.constraints.ids.getOrNull(index)?.value
            ConstraintId(
                derivedModelElementIdentity(
                    kind = ModelElementKind.Constraint,
                    role = "farkas-equality",
                    sourceId = sourceId,
                    sourceScope = this.constraints.identityScopeAt(index),
                    sourceOrigin = this.constraints.identityOriginAt(index),
                    sourceProvenance = this.constraints.identityProvenanceOrOriginAt(index),
                    namespace = this.constraints.identityNamespace,
                    schemaVersion = this.constraints.identitySchemaVersion,
                    discriminator = derivedDiscriminator(sourceId, "0", index.toString())
                ).id.value
            )
        }
        val constraintScopes = this.variables.map { variable ->
            if (variable.id == null) ModelElementScope.ModelLocal else variable.identityScope
        } + ModelElementScope.ModelLocal + this.constraints.indices.filter {
            this.constraints.signs[it] == ConstraintRelation.Equal
        }.map { this.constraints.identityScopeAt(it) }
        val constraintOrigins = this.variables.map { it.identityOrigin } + null + this.constraints.indices.filter {
            this.constraints.signs[it] == ConstraintRelation.Equal
        }.map { this.constraints.identityOriginAt(it) }
        val constraintProvenance = this.variables.map { it.identityProvenanceOrOrigin() } + listOf(emptyList()) +
            this.constraints.indices.filter {
                this.constraints.signs[it] == ConstraintRelation.Equal
            }.map { this.constraints.identityProvenanceOrOriginAt(it) }
        val objectiveIdentity = derivedModelElementIdentity(
            kind = ModelElementKind.Objective,
            role = "farkas-objective",
            sourceId = this.objective.id?.value,
            sourceScope = this.objective.identityScope,
            sourceOrigin = this.objective.identityOrigin,
            sourceProvenance = this.objective.identityProvenanceOrOrigin(),
            namespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
            schemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion
        )

        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = (derivedFarkasVariables + derivedSlackVariables + derivedBoundVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = QuadraticConstraintBatch(
                    sparseLhs = buildQuadraticSparseLhs(lhs),
                    signs = signs,
                    rhs = rhs,
                    names = names,
                    sources = sources,
                    ids = constraintIds,
                    identityNamespace = this.identityNamespace ?: this.constraints.identityNamespace,
                    identitySchemaVersion = this.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                    identityScopes = constraintScopes,
                    identityOrigins = constraintOrigins,
                    identityProvenance = constraintProvenance
                ),
                name = "$name-farkas-dual"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = objective,
                id = ObjectiveId(objectiveIdentity.id.value),
                identityScope = objectiveIdentity.scope,
                identityOrigin = objectiveIdentity.origin,
                identityNamespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
                identitySchemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                identityProvenance = objectiveIdentity.provenance
            ),
            dualOrigin = this,
            identityValidation = ok
        ).withDerivedIdentityValidation()
    }
    override fun feasibility(): QuadraticTetradModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Variable>()
        val artifactVariables = ArrayList<Variable>()
        fun artifactVariable(
            constraintIndex: Int,
            role: String,
            index: Int,
            slack: VariableSlack? = null
        ): Variable {
            val sourceId = this.constraints.ids.getOrNull(constraintIndex)?.value
            val identity = derivedModelElementIdentity(
                kind = ModelElementKind.Variable,
                role = "feasibility-$role",
                sourceId = sourceId,
                sourceScope = this.constraints.identityScopeAt(constraintIndex),
                sourceOrigin = this.constraints.identityOriginAt(constraintIndex),
                sourceProvenance = this.constraints.identityProvenanceOrOriginAt(constraintIndex),
                namespace = this.constraints.identityNamespace,
                schemaVersion = this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, role, "$constraintIndex:$role")
            )
            return Variable(
                index = index,
                lowerBound = Flt64.zero,
                upperBound = Flt64.infinity,
                type = Continuous,
                origin = null,
                dualOrigin = null,
                slack = slack,
                name = "${this.constraints.names[constraintIndex].ifEmpty { "cons$constraintIndex" }}_$role",
                initialResult = Flt64.zero,
                id = VariableId(identity.id.value),
                identityScope = identity.scope,
                identityOrigin = identity.origin,
                identityNamespace = this.constraints.identityNamespace,
                identitySchemaVersion = this.constraints.identitySchemaVersion,
                identityProvenance = identity.provenance
            )
        }
        val lhs = this.constraints.indices.map {
                when (if (this.constraints.rhs[it] ls Flt64.zero) {
                    this.constraints.signs[it].reverse
                } else {
                    this.constraints.signs[it]
                }) {
                    ConstraintRelation.LessEqual -> {
                        val slack = artifactVariable(
                            constraintIndex = it,
                            role = "slack",
                            index = colIndex,
                            slack = VariableSlack(constraint = this.constraints.origins[it])
                        )
                        colIndex += 1

                        slackVariables.add(slack)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = slack.index,
                                colIndex2 = null,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    ConstraintRelation.GreaterEqual -> {
                        val slack = artifactVariable(
                            constraintIndex = it,
                            role = "slack",
                            index = colIndex,
                            slack = VariableSlack(constraint = this.constraints.origins[it])
                        )
                        colIndex += 1
                        val artifact = artifactVariable(
                            constraintIndex = it,
                            role = "artifact",
                            index = colIndex
                        )
                        colIndex += 1

                        slackVariables.add(slack)
                        artifactVariables.add(artifact)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = slack.index,
                                colIndex2 = null,
                                coefficient = -Flt64.one
                            ),
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = artifact.index,
                                colIndex2 = null,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    ConstraintRelation.Equal -> {
                        val artifact = artifactVariable(
                            constraintIndex = it,
                            role = "artifact",
                            index = colIndex
                        )
                        colIndex += 1

                        artifactVariables.add(artifact)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = artifact.index,
                                colIndex2 = null,
                                coefficient = Flt64.one
                            )
                        )
                    }
                }
            }
        val constraintIdentities = this.constraints.indices.map { index ->
            val sourceId = this.constraints.ids.getOrNull(index)?.value
            derivedModelElementIdentity(
                kind = ModelElementKind.Constraint,
                role = "feasibility-constraint",
                sourceId = sourceId,
                sourceScope = this.constraints.identityScopeAt(index),
                sourceOrigin = this.constraints.identityOriginAt(index),
                sourceProvenance = this.constraints.identityProvenanceOrOriginAt(index),
                namespace = this.constraints.identityNamespace,
                schemaVersion = this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "0", index.toString())
            )
        }
        val constraints = QuadraticConstraintBatch(
            sparseLhs = buildQuadraticSparseLhs(lhs),
            signs = this.constraints.indices.map {
                ConstraintRelation.Equal
            },
            rhs = this.constraints.indices.map {
                this.constraints.rhs[it]
            },
            names = this.constraints.indices.map {
                this.constraints.names[it].ifEmpty { "cons${it}" }
            },
            sources = this.constraints.indices.map {
                ConstraintSource.Feasibility
            },
            origins = this.constraints.indices.map {
                this.constraints.origins[it]
            },
            froms = this.constraints.indices.map {
                this.constraints.froms[it]
            },
            priorities = this.constraints.indices.map {
                this.constraints.priorities[it]
            },
            ids = constraintIdentities.map { ConstraintId(it.id.value) },
            identityNamespace = this.constraints.identityNamespace,
            identitySchemaVersion = this.constraints.identitySchemaVersion,
            identityScopes = constraintIdentities.map { it.scope },
            identityOrigins = constraintIdentities.map { it.origin },
            identityProvenance = constraintIdentities.map { it.provenance }
        )
        val objective = artifactVariables.map {
            QuadraticObjectiveCell(
                colIndex1 = it.index,
                colIndex2 = null,
                coefficient = Flt64.one
            )
        }

        val objectiveIdentity = derivedModelElementIdentity(
            kind = ModelElementKind.Objective,
            role = "feasibility-objective",
            sourceId = this.objective.id?.value,
            sourceScope = this.objective.identityScope,
            sourceOrigin = this.objective.identityOrigin,
            sourceProvenance = (
                this.objective.identityProvenanceOrOrigin() +
                    this.constraints.indices.flatMap { this.constraints.identityProvenanceOrOriginAt(it) } +
                    this.variables.flatMap { it.identityProvenanceOrOrigin() }
                ).distinct(),
            namespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
            schemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion
        )
        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = this.variables + (slackVariables + artifactVariables).sortedBy { it.index },
                constraints = constraints,
                name = "$name-feasibility"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = objective,
                id = ObjectiveId(objectiveIdentity.id.value),
                identityScope = objectiveIdentity.scope,
                identityOrigin = objectiveIdentity.origin,
                identityNamespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
                identitySchemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                identityProvenance = objectiveIdentity.provenance
            ),
            identityValidation = ok
        ).withDerivedIdentityValidation()
    }
    override fun elastic(): QuadraticTetradModel {
        return buildElasticModel()
    }

    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.write("${objective.category}\n")
        var i = 0
        for (cell in objective.objective) {
            if (cell.coefficient eq Flt64.zero) {
                continue
            }
            val coefficient = if (i != 0) {
                if (cell.coefficient leq Flt64.zero) {
                    writer.append(" - ")
                } else {
                    writer.append(" + ")
                }
                abs(cell.coefficient)
            } else {
                cell.coefficient
            }
            if (coefficient neq Flt64.zero) {
                if (coefficient neq Flt64.one) {
                    writer.append("$coefficient ")
                }
                writer.append("${variables[cell.colIndex1]}")

                if (cell.colIndex2 != null) {
                    writer.append(" * ${variables[cell.colIndex2]}")
                }
            }
            ++i
        }
        writer.append("\n\n")

        return when (val result = impl.exportLP(writer)) {
            is Ok -> {
                ok
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                result
            }
        }
    }

    override fun close() {
        dualOrigin?.close()
        super.close()
    }

    override fun toString(): String {
        return name
    }
}
