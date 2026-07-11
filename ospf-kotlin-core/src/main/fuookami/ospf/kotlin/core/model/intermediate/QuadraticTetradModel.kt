/**
 * 二次四元模型
 * Quadratic tetrad model
*/
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
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
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import java.io.OutputStreamWriter
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger

/**
 * 判断此二次约束是否为单变量边界约束（单项、系数为1、无二次项）
 * Check whether this quadratic constraint is a single-variable bound constraint (single term, coefficient 1, no quadratic term)
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
 * 二次约束单元
 * Quadratic constraint cell
 *
 * 表示二次约束矩阵中的一个非零元素，包含行索引、列索引和系数。
 * Represents a non-zero element in the quadratic constraint matrix,
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
 * 二次约束批次
 * Quadratic constraint batch
 *
 * 存储一组二次约束的稀疏矩阵表示，包括约束符号、右侧常量和约束来源。
 * Stores a batch of quadratic constraints in sparse matrix representation,
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
*/
class QuadraticConstraintBatch(
    val sparseLhs: SparseQuadraticMatrix,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    origins: List<QuadraticConstraintImpl<Flt64>?> = (0 until sparseLhs.numRows()).map { null },
    froms: List<Pair<IntermediateSymbol<*>, Boolean>?> = (0 until sparseLhs.numRows()).map { null },
    priorities: List<Int?> = (0 until sparseLhs.numRows()).map { null }
) : ModelConstraint<QuadraticConstraintCell>(sparseLhs.numRows(), signs, rhs, names, sources) {

    /**
     * 二次左侧矩阵的稀疏表示。
     * 每一行是一个 SparseQuadraticVector，其中条目携带 (colIndex1, colIndex2?, coefficient)。
     * 这是主要的约束表示形式。
     *
     * Sparse representation of the quadratic LHS matrix.
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
        priorities.toList()
    )

    override fun close() {
        _origins.clear()
        _froms.clear()
        _priorities.clear()
        super.close()
    }
}

/**
 * 二次目标单元
 * Quadratic objective cell
 *
 * 表示二次目标函数中的一个非零元素，包含列索引和系数。
 * Represents a non-zero element in the quadratic objective function,
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
 * 二次目标函数类型别名
 * Type alias for quadratic objective function
*/
typealias QuadraticObjective = Objective<QuadraticObjectiveCell>

/**
 * 基础二次四元模型
 * Basic quadratic tetrad model
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
         * 但不包含目标函数步骤。
         *
         * Create a [BasicQuadraticTetradModel] from a [QuadraticMechanismModel<Flt64>] by
         * extracting variables and constraints into solver-standard form.
         *
         * This is a convenience factory that mirrors the variable/constraint extraction
         * logic in [QuadraticTetradModel.invoke] without the objective function step.
         *
         * @param model           源机制模型 / the source mechanism model
         * @param tokenIndexMap   符号到求解器列索引的映射 / mapping from tokens to solver column indices
         * @param bounds          每个符号的预计算边界约束 / pre-computed bound constraints per token
         * @param fixedVariables  固定为常量值的变量（被替换掉）/ variables fixed to constant values (substituted out)
         * @return 包含提取的变量和约束的 [BasicQuadraticTetradModel] / a [BasicQuadraticTetradModel] containing the extracted variables and constraints
        */
        fun from(
            model: QuadraticMechanismModel<Flt64>,
            tokenIndexMap: Map<Token<Flt64>, Int>,
            bounds: Map<Token<Flt64>, List<Quadruple<QuadraticConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>> = emptyMap(),
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): BasicQuadraticTetradModel {
            val variables = dumpQuadraticTetradVariables(
                tokenIndexes = tokenIndexMap,
                bounds = bounds
            )
            val constraints = dumpQuadraticTetradConstraints(
                model = model,
                tokenIndexes = tokenIndexMap,
                bounds = bounds,
                fixedVariables = fixedVariables
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
     * 就地线性松弛
     * In-place linear relaxation
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
     * 返回线性松弛后的副本
     * Return a linearly relaxed copy
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
 * 二次四元模型视图接口
 * Quadratic tetrad model view interface
 *
 * 提供二次模型的视图操作，包括线性松弛、对偶、可行性和弹性模型。
 * Provides view operations for quadratic models, including linear relaxation, dual, feasibility, and elastic models.
*/
interface QuadraticTetradModelView : ModelView<QuadraticConstraintCell, QuadraticObjectiveCell> {
    override val constraints: QuadraticConstraintBatch
    val dual: Boolean

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
     * 整理对偶解，将非零对偶值映射回原始约束
     * Tidy dual solution, mapping non-zero dual values back to original constraints
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
 * 二次四元模型
 * Quadratic tetrad model
 *
 * 求解器标准形式的二次优化模型，包含变量、约束和目标函数。
 * Solver-standard form of quadratic optimization model, containing variables, constraints, and objective function.
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
    internal val dualOrigin: QuadraticTetradModelView? = null
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraintBatch by impl::constraints
    override val name: String by impl::name
    override val dual get() = dualOrigin != null

    companion object {
        private val logger = logger()

        /** V->Flt64 转换边界：泛型 V 在二次中间模型构造时解析为具体类型 Flt64。 / V->Flt64 conversion boundary: generic V resolves to concrete Flt64 for quadratic intermediate model construction. */
        suspend operator fun invoke(
            model: QuadraticMechanismModel<Flt64>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            dumpConstraintsToBounds: Boolean? = null,
            forceDumpBounds: Boolean? = null,
            concurrent: Boolean? = null
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
                            bounds = bounds
                        )
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpQuadraticTetradConstraintsAsync(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables
                        )
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpQuadraticTetradObjectives(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            fixedVariables = fixedVariables
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
                            bounds = bounds
                        ),
                        constraints = dumpQuadraticTetradConstraints(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables
                        ),
                        name = model.name
                    ),
                    tokensInSolver = tokensInSolver,
                    objective = dumpQuadraticTetradObjectives(
                        model = model,
                        tokenIndexes = tokenIndexMap,
                        fixedVariables = fixedVariables
                    )
                )
            }

            logger.trace("QuadraticTetradModel created for $model")
            MemoryCleanupPolicy.cleanupAfterModelBuilt()
            return tetradModel
        }
    }

    override fun copy() = QuadraticTetradModel(
        impl = impl.copy(),
        tokensInSolver = tokensInSolver,
        objective = objective.copy()
    )

    override fun clone() = copy()

    override fun linearRelax(): QuadraticTetradModel {
        impl.linearRelax()
        return this
    }

    override fun linearRelaxed(): QuadraticTetradModel {
        return QuadraticTetradModel(
            impl = impl.linearRelaxed(),
            tokensInSolver = tokensInSolver,
            objective = objective.copy()
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
                    sources = sources
                ),
                name = "$name-dual"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(this.objective.category.reverse, objective),
            dualOrigin = this
        )
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

        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = (farkasVariables + slackVariables + boundVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = QuadraticConstraintBatch(
                    sparseLhs = buildQuadraticSparseLhs(lhs),
                    signs = signs,
                    rhs = rhs,
                    names = names,
                    sources = sources
                ),
                name = "$name-farkas-dual"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(ObjectCategory.Minimum, objective),
            dualOrigin = this
        )
    }
    override fun feasibility(): QuadraticTetradModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Variable>()
        val artifactVariables = ArrayList<Variable>()
        val lhs = this.constraints.indices.map {
                when (if (this.constraints.rhs[it] ls Flt64.zero) {
                    this.constraints.signs[it].reverse
                } else {
                    this.constraints.signs[it]
                }) {
                    ConstraintRelation.LessEqual -> {
                        val slack = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[it]
                            ),
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack",
                            initialResult = Flt64.zero
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
                        val slack = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[it]
                            ),
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        val artifact = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact",
                            initialResult = Flt64.zero
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
                        val artifact = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact",
                            initialResult = Flt64.zero
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
            }
        )
        val objective = artifactVariables.map {
            QuadraticObjectiveCell(
                colIndex1 = it.index,
                colIndex2 = null,
                coefficient = Flt64.one
            )
        }

        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = this.variables + (slackVariables + artifactVariables).sortedBy { it.index },
                constraints = constraints,
                name = "$name-feasibility"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(ObjectCategory.Minimum, objective)
        )
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
