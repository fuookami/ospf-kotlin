/**
 * 线性三元模型 / Linear triad model
*/
package fuookami.ospf.kotlin.core.model.intermediate

import java.io.OutputStreamWriter
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.symbol.Linear
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
 * 将任意数值类型转换为 Flt64（求解器边界用） / Convert any numeric value to Flt64 (for solver boundary use)
 *
 * @return 转换后的 Flt64 值 / The converted Flt64 value
*/
private fun Any?.toSolverFlt64(): Flt64 {
    return when (this) {
        is Flt64 -> this
        is RealNumber<*> -> this.toFlt64()
        else -> error("Unsupported solver-boundary numeric value: ${this?.javaClass?.name}")
    }
}

/**
 * 判断此线性约束是否为单变量边界约束（系数为1的单项约束） / Check whether this linear constraint is a single-variable bound constraint (single term with coefficient 1)
 *
 * @return 是否为边界约束 / Whether this is a bound constraint
*/
private fun LinearConstraintImpl<Flt64>.isBound(): Boolean {
    val lhs = (this as LinearConstraintImpl<*>).lhs
    return lhs.size == 1
            && lhs.first().coefficient.toSolverFlt64() eq Flt64.one
    // && from?.second != true (commented-out additional check / 注释掉的附加检查)
}

/**
 * 将求解器边界单元格令牌视为 Flt64 令牌 / Treat a solver-boundary cell token as an Flt64 token
 *
 * @return 转型后的 Flt64 令牌 / The cast Flt64 token
*/
@Suppress("UNCHECKED_CAST")
private fun LinearCell<*>.tokenAsFlt64(): Token<Flt64> {
    return token as Token<Flt64>
}

/**
 * 线性约束单元 / Linear constraint cell
 *
 * 表示线性约束矩阵中的一个非零元素，包含行索引、列索引和系数。 / Represents a non-zero element in the linear constraint matrix,
 * containing row index, column index, and coefficient.
 *
 * @property rowIndex 行索引 / Row index
 * @property colIndex 列索引 / Column index
 * @param coefficient 系数 / Coefficient
*/
class LinearConstraintCell(
    override val rowIndex: Int,
    val colIndex: Int,
    coefficient: Flt64
) : ConstraintCell<LinearConstraintCell>, Cloneable, Copyable<LinearConstraintCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): LinearConstraintCell {
        return LinearConstraintCell(
            rowIndex = rowIndex,
            colIndex = colIndex,
            coefficient = -coefficient
        )
    }

    override fun copy() = LinearConstraintCell(
        rowIndex = rowIndex,
        colIndex = colIndex,
        coefficient = coefficient.copy()
    )

    override fun clone() = copy()

    override fun toString(): String {
        return "(${rowIndex}, ${colIndex}, ${coefficient})"
    }
}

/**
 * 线性约束批次 / Linear constraint batch
 *
 * 存储一组线性约束的稀疏矩阵表示，包括约束符号、右侧常量和约束来源。 / Stores a batch of linear constraints in sparse matrix representation,
 * including constraint signs, right-hand side constants, and constraint sources.
 *
 * @property sparseLhs 稀疏矩阵（左侧）/ Sparse matrix (left-hand side)
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
class LinearConstraintBatch(
    val sparseLhs: SparseMatrix<Flt64>,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    origins: List<LinearConstraintImpl<Flt64>?> = (0 until sparseLhs.numRows()).map { null },
    froms: List<Pair<IntermediateSymbol<*>, Boolean>?> = (0 until sparseLhs.numRows()).map { null },
    priorities: List<Int?> = (0 until sparseLhs.numRows()).map { null },
    ids: List<ConstraintId> = emptyList(),
    identityNamespace: String? = null,
    identitySchemaVersion: String? = null,
    identityScopes: List<ModelElementScope> = emptyList(),
    identityOrigins: List<ModelElementOrigin?> = emptyList(),
    identityProvenance: List<List<ModelElementOrigin>> = emptyList(),
    identityMetadataValidationOverride: Try? = null
) : ModelConstraint<LinearConstraintCell>(
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
     * 稀疏矩阵（左侧）的稀疏表示。
     * 每行为一个 SparseVector<Flt64>，其中 entry.index = 列索引，entry.value = 系数。
     * 这是主要的约束表示形式。 / Sparse representation of the LHS matrix.
     * Each row is a SparseVector<Flt64> where entry.index = colIndex, entry.value = coefficient.
     * This is the primary constraint representation.
    */
    override val lhs: List<List<LinearConstraintCell>> by lazy {
        sparseLhs.rows.mapIndexed { rowIndex, row ->
            row.entries.map { entry ->
                LinearConstraintCell(
                    rowIndex = rowIndex,
                    colIndex = entry.index,
                    coefficient = entry.value
                )
            }
        }
    }

    private val _origins: MutableList<LinearConstraintImpl<Flt64>?> = origins.toMutableList()
    val origins: List<LinearConstraintImpl<Flt64>?> by ::_origins

    private val _froms: MutableList<Pair<IntermediateSymbol<*>, Boolean>?> = froms.toMutableList()
    val froms: List<Pair<IntermediateSymbol<*>, Boolean>?> by ::_froms

    private val _priorities: MutableList<Int?> = priorities.toMutableList()
    val priorities: List<Int?> by ::_priorities

    /**
     * 按条件过滤约束批次 / Filter constraint batch by condition
     *
     * @param condition 过滤条件，参数为行索引 / Filter condition, parameter is row index
     * @return 过滤后的约束批次 / Filtered constraint batch
    */
    fun filter(condition: (Int) -> Boolean): LinearConstraintBatch {
        val filteredSparseLhs = SparseMatrix<Flt64>()
        for ((i, row) in sparseLhs.rows.withIndex()) {
            if (condition(i)) {
                filteredSparseLhs.addRow(row)
            }
        }
        return LinearConstraintBatch(
            sparseLhs = filteredSparseLhs,
            signs = signs.filterIndexed { i, _ -> condition(i) },
            rhs = rhs.filterIndexed { i, _ -> condition(i) },
            names = names.filterIndexed { i, _ -> condition(i) },
            sources = sources.filterIndexed { i, _ -> condition(i) },
            origins = origins.filterIndexed { i, _ -> condition(i) },
            froms = froms.filterIndexed { i, _ -> condition(i) },
            priorities = priorities.filterIndexed { i, _ -> condition(i) },
            ids = ids.filterIndexed { i, _ -> condition(i) },
            identityNamespace = identityNamespace,
            identitySchemaVersion = identitySchemaVersion,
            identityScopes = identityScopes.filterIndexed { i, _ -> condition(i) },
            identityOrigins = identityOrigins.filterIndexed { i, _ -> condition(i) },
            identityProvenance = identityProvenance.filterIndexed { i, _ -> condition(i) },
            identityMetadataValidationOverride = identityMetadataValidation
        )
    }

    override fun copy() = LinearConstraintBatch(
        SparseMatrix<Flt64>().also { mat ->
            for (row in sparseLhs.rows) {
                val newRow = SparseVector<Flt64>()
                for (entry in row.entries) {
                    newRow.add(entry.index, entry.value.copy())
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
 * 线性目标单元 / Linear objective cell
 *
 * 表示线性目标函数中的一个非零元素，包含列索引和系数。 / Represents a non-zero element in the linear objective function,
 * containing column index and coefficient.
 *
 * @property colIndex 列索引 / Column index
 * @param coefficient 系数 / Coefficient
*/
class LinearObjectiveCell(
    val colIndex: Int,
    coefficient: Flt64
) : ModelCell<LinearObjectiveCell>, Cloneable, Copyable<LinearObjectiveCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): LinearObjectiveCell {
        return LinearObjectiveCell(colIndex, -coefficient)
    }

    override fun copy() = LinearObjectiveCell(colIndex, coefficient.copy())
    override fun clone() = copy()

    override fun toString(): String {
        return "(${colIndex}, ${coefficient})"
    }
}

/**
 * 线性目标函数类型别名 / Type alias for linear objective function
*/
typealias LinearObjective = Objective<LinearObjectiveCell>

/**
 * 基础线性三元模型视图类型别名 / Type alias for basic linear triad model view
*/
typealias BasicLinearTriadModelView = BasicModelView<LinearConstraintCell>

/**
 * 基础线性三元模型 / Basic linear triad model
 *
 * 线性问题的求解器标准形式（三元：变量 + 约束，无目标函数）。
 * 直接用于 IIS（不可约不可行子系统）计算，以及作为 [LinearTriadModel] 的 [impl] 委托。 / Solver-standard form for linear problems (triad: variables + constraints, no objective).
 * Used directly by IIS (Irreducible Infeasible Subsystem) computation and
 * as the [impl] delegate inside [LinearTriadModel].
 *
 * ### 构造方式 / Construction
 *
 * 直接构造 / Direct constructor:
 * ```kotlin
 * BasicLinearTriadModel(variables, constraints, name)
 * ```
 *
 * 从 LinearMechanismModel 工厂方法 / Factory from LinearMechanismModel:
 * ```kotlin
 * BasicLinearTriadModel.from(mechanismModel, tokenIndexMap, bounds, fixedVariables)
 * ```
 *
 * ### 与 LinearTriadModel 的关系 / Relationship to [LinearTriadModel]
 *
 * [LinearTriadModel] 包装 [BasicLinearTriadModel] 作为其 `impl`，添加目标函数和符号到求解器的映射。
 * [BasicLinearTriadModel] 是仅包含变量和约束的子集。
 * [LinearTriadModel] wraps a [BasicLinearTriadModel] as its `impl`, adding
 * objective function and token-to-solver mapping. [BasicLinearTriadModel] is
 * the subset that only contains variables and constraints.
 *
 * @property variables 求解器索引的变量列表 / Solver-indexed variable list
 * @property constraints 线性约束批次 / Linear constraint batch
 * @property name 模型名称（用于日志和调试）/ Model name (for logging and debugging)
*/
class BasicLinearTriadModel(
    override val variables: List<Variable>,
    override val constraints: LinearConstraintBatch,
    override val name: String
) : BasicLinearTriadModelView, Cloneable, Copyable<BasicLinearTriadModel> {
    companion object {
        /**
         * 从 [LinearMechanismModel<Flt64>] 创建 [BasicLinearTriadModel]，
         * 将变量和约束提取为求解器标准形式。
         *
         * 这是一个便捷工厂方法，复用 [LinearTriadModel.invoke] 中的变量/约束提取逻辑，
         * 但不包含目标函数步骤。 / Create a [BasicLinearTriadModel] from a [LinearMechanismModel<Flt64>] by
         * extracting variables and constraints into solver-standard form.
         *
         * This is a convenience factory that mirrors the variable/constraint extraction
         * logic in [LinearTriadModel.invoke] without the objective function step.
         *
         * @param model           源机制模型 / the source mechanism model
         * @param tokenIndexMap   符号到求解器列索引的映射 / mapping from tokens to solver column indices
         * @param bounds          每个符号的预计算边界约束 / pre-computed bound constraints per token
         * @param fixedVariables  固定为常量值的变量（将被代换消除）/ variables fixed to constant values (substituted out)
         * @param identityRegistry 可选的稳定身份注册表 / optional stable identity registry
         * @return 包含提取的变量和约束的 [BasicLinearTriadModel] / a [BasicLinearTriadModel] containing the extracted variables and constraints
        */
        fun from(
            model: LinearMechanismModel<Flt64>,
            tokenIndexMap: Map<Token<Flt64>, Int>,
            bounds: Map<Token<Flt64>, List<Quadruple<LinearConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>> = emptyMap(),
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            identityRegistry: ModelElementIdentityRegistry? = model.identityRegistry
        ): BasicLinearTriadModel {
            val variables = dumpLinearTriadVariables(
                tokenIndexes = tokenIndexMap,
                bounds = bounds,
                identityRegistry = identityRegistry
            )
            val constraints = dumpLinearTriadConstraints(
                model = model,
                tokenIndexes = tokenIndexMap,
                bounds = bounds,
                fixedVariables = fixedVariables,
                identityRegistry = identityRegistry
            )
            return BasicLinearTriadModel(variables, constraints, model.name)
        }
    }
    override fun copy() = BasicLinearTriadModel(
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
    fun linearRelaxed(): BasicLinearTriadModel {
        return BasicLinearTriadModel(
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
            constraints = constraints,
            name = name
        )
    }
    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.append("Subject To\n")
        for (i in constraints.indices) {
            writer.append(" ${constraints.names[i].ifEmpty { "cons$i" }}: ")
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
                    writer.append("${variables[constraints.lhs[i][j].colIndex]}")
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
            if (variable.free) {
                writer.append(" $variable free\n")
            } else if (variable.positiveNormalized) {
                writer.append(" $variable >= 0\n")
            } else if (variable.negativeNormalized) {
                writer.append(" $variable <= 0\n")
            } else if (variable.positiveFree) {
                writer.append(" $variable >= ${variable.lowerBound}\n")
            } else if (variable.negativeFree) {
                writer.append(" $variable <= ${variable.upperBound}\n")
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
 * 线性三元模型视图 / Linear triad model view
 *
 * 线性优化模型的视图接口，提供变量、约束、目标函数的统一访问，
 * 以及线性松弛、对偶模型、可行性模型、弹性模型等变换操作。 / View interface for linear optimization models, providing unified access
 * to variables, constraints, and objective function, as well as transformation
 * operations such as linear relaxation, dual model, feasibility model, and elastic model.
 *
 * @property constraints 线性约束批次 / Linear constraint batch
 * @property dual 是否为对偶模型 / Whether this is a dual model
*/
interface LinearTriadModelView : ModelView<LinearConstraintCell, LinearObjectiveCell> {
    override val constraints: LinearConstraintBatch
    val dual: Boolean

    /**
     * Identity validation captured while building the intermediate model.
     * 中间模型构建期间捕获的身份校验结果。
     */
    val identityValidation: Try
        get() = ok

    /**
     * 就地线性松弛（修改当前模型） / In-place linear relaxation (modifies the current model)
     *
     * @return 松弛后的自身引用 / Self reference after relaxation
    */
    fun linearRelax(): LinearTriadModelView

    /**
     * 返回线性松弛后的副本 / Return a linearly relaxed copy
     *
     * @return 线性松弛后的模型视图副本 / Linearly relaxed model view copy
    */
    fun linearRelaxed(): LinearTriadModelView

    /**
     * 构建 Farkas 对偶模型 / Build Farkas dual model
     *
     * @return Farkas 对偶线性三元模型视图 / Farkas dual linear triad model view
    */
    suspend fun farkasDual(): LinearTriadModelView

    /**
     * 构建可行性模型（最小化人工变量） / Build feasibility model (minimize artificial variables)
     *
     * @return 可行性线性三元模型视图 / Feasibility linear triad model view
    */
    fun feasibility(): LinearTriadModelView

    /**
     * 构建弹性模型（允许约束松弛） / Build elastic model (allow constraint relaxation)
     *
     * @param minmaxSlack  是否启用最小-最大松弛 / Whether to enable min-max slack
     * @param minSlackAmount  最小松弛量限制 / Minimum slack amount limit
     * @return 弹性线性三元模型视图 / Elastic linear triad model view
    */
    fun elastic(
        minmaxSlack: Boolean = false,
        minSlackAmount: Pair<UInt64, Flt64>? = null
    ): LinearTriadModelView

    /**
     * 整理对偶解，将完整对偶值（包括零值）映射回原始约束 / Tidy dual solution, mapping complete dual values (including zero) back to original constraints
     *
     * @param solution 求解器返回的对偶解向量 / Dual solution vector returned by the solver
     * @return 完整对偶值到原始约束的映射 / Mapping from complete dual values to original constraints
    */
    fun tidyDualSolution(solution: List<Flt64>): kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64> {
        return if (dual) {
            variables.associateNotNull {
                if (it.dualOrigin != null && solution.size > it.index) {
                    (it.dualOrigin as LinearConstraintImpl<Flt64>) to solution[it.index]
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
 * 线性三元模型 / Linear triad model
 *
 * 求解器标准形式的线性优化模型，包含变量、约束和目标函数。 / Solver-standard form of linear optimization model, containing variables, constraints, and objective function.
 *
 * @property impl 基础模型实现 / Basic model implementation
 * @property tokensInSolver 求解器中的符号列表 / Token list in solver
 * @property objective 目标函数 / Objective function
 * @property dualOrigin 对偶模型来源 / Dual model origin
*/
data class LinearTriadModel(
    private val impl: BasicLinearTriadModel,
    val tokensInSolver: List<Token<Flt64>>,
    override val objective: LinearObjective,
    internal val dualOrigin: LinearTriadModelView? = null,
    override val identityValidation: Try = validateDerivedIdentitySet(
        variables = impl.variables,
        constraints = impl.constraints,
        objective = objective
    )
) : LinearTriadModelView, Cloneable, Copyable<LinearTriadModel> {
    companion object {
        private val logger = logger()

        /**
         * V->Flt64 转换边界：泛型 V 在线性中间模型构造时解析为具体的 Flt64 类型。 /
         * V->Flt64 conversion boundary: generic V resolves to concrete Flt64 for linear intermediate model construction.
         *
         * @param model 源线性机制模型 / Source linear mechanism model
         * @param fixedVariables 可选的固定变量 / Optional fixed variables
         * @param dumpConstraintsToBounds 是否转储边界约束 / Whether to dump bound constraints
         * @param forceDumpBounds 是否强制转储可识别边界 / Whether to force recognizable bounds
         * @param concurrent 是否并行转储 / Whether to dump concurrently
         * @param identityRegistry 可选的稳定身份注册表 / Optional stable identity registry
         * @return 线性三元模型 / Linear triad model
         */
        suspend operator fun invoke(
            model: LinearMechanismModel<Flt64>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            dumpConstraintsToBounds: Boolean? = null,
            forceDumpBounds: Boolean? = null,
            concurrent: Boolean? = null,
            identityRegistry: ModelElementIdentityRegistry? = model.identityRegistry
        ): LinearTriadModel {
            logger.trace("Creating LinearTriadModel for $model")
            val tokensInSolver = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokensInSolver
            } else {
                model.tokens.tokensInSolverWithout(fixedVariables.keys)
            }
            val tokenIndexMap = tokensInSolver.withIndex().associate { (index, token) -> token to index }
            val bounds = model.linearConstraints
                .flatMap { constraint ->
                    val thisConstraint = constraint as LinearConstraintImpl<*>
                    if ((dumpConstraintsToBounds ?: true) && constraint.isBound()) {
                        listOf(Quadruple(constraint, thisConstraint.lhs.first().tokenAsFlt64(), constraint.sign, thisConstraint.rhs.toSolverFlt64()))
                    } else if (forceDumpBounds ?: false) {
                        if (thisConstraint.lhs.size == 1) {
                            listOf(
                                Quadruple(
                                    constraint,
                                    thisConstraint.lhs.first().tokenAsFlt64(),
                                    constraint.sign,
                                    thisConstraint.rhs.toSolverFlt64() / thisConstraint.lhs.first().coefficient.toSolverFlt64()
                                )
                            )
                        } else if (thisConstraint.lhs.all { it.coefficient.toSolverFlt64() eq Flt64.one && it.token.lowerBound!!.value.unwrap().toSolverFlt64() geq Flt64.zero }
                            && (constraint.sign == ConstraintRelation.LessEqual || constraint.sign == ConstraintRelation.Equal)
                            && thisConstraint.rhs.toSolverFlt64() eq Flt64.zero
                        ) {
                            thisConstraint.lhs.map { Quadruple(constraint, it.tokenAsFlt64(), ConstraintRelation.Equal, Flt64.zero) }
                        } else if (thisConstraint.lhs.all { it.coefficient.toSolverFlt64() eq -Flt64.one && it.token.lowerBound!!.value.unwrap().toSolverFlt64() geq Flt64.zero }
                            && (constraint.sign == ConstraintRelation.GreaterEqual || constraint.sign == ConstraintRelation.Equal)
                            && thisConstraint.rhs.toSolverFlt64() eq Flt64.zero
                        ) {
                            thisConstraint.lhs.map { Quadruple(constraint, it.tokenAsFlt64(), ConstraintRelation.Equal, Flt64.zero) }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }.groupBy { it.second }
            val triadModel = if (concurrent ?: model.concurrent) {
                coroutineScope {
                    val variablePromise = async(Dispatchers.Default) {
                        dumpLinearTriadVariables(
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            identityRegistry = identityRegistry
                        )
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpLinearTriadConstraintsAsync(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables,
                            identityRegistry = identityRegistry
                        )
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpLinearTriadObjectives(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            fixedVariables = fixedVariables,
                            identityRegistry = identityRegistry
                        )
                    }

                    LinearTriadModel(
                        impl = BasicLinearTriadModel(
                            variables = variablePromise.await(),
                            constraints = constraintPromise.await(),
                            name = model.name
                        ),
                        tokensInSolver = tokensInSolver,
                        objective = objectivePromise.await()
                    )
                }
            } else {
                LinearTriadModel(
                    impl = BasicLinearTriadModel(
                        variables = dumpLinearTriadVariables(
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            identityRegistry = identityRegistry
                        ),
                        constraints = dumpLinearTriadConstraints(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables,
                            identityRegistry = identityRegistry
                        ),
                        name = model.name
                    ),
                    tokensInSolver = tokensInSolver,
                    objective = dumpLinearTriadObjectives(
                        model = model,
                        tokenIndexes = tokenIndexMap,
                        fixedVariables = fixedVariables,
                        identityRegistry = identityRegistry
                    )
                )
            }

            val identityValidation = combineIdentityValidation(
                materializedValidation = validateDerivedIdentitySet(
                    variables = triadModel.variables,
                    constraints = triadModel.constraints,
                    objective = triadModel.objective,
                    allowGeneratedArtifactPrefix = true
                ),
                registryValidation = identityRegistry?.validate()
            )
            val validatedTriadModel = triadModel.copy(identityValidation = identityValidation)
            logger.trace("LinearTriadModel created for $model")
            MemoryCleanupPolicy.cleanupAfterModelBuilt()
            return validatedTriadModel
        }
    }

    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraintBatch by impl::constraints
    override val name: String by impl::name
    override val dual get() = dualOrigin != null

    override fun copy() = LinearTriadModel(
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

    override fun linearRelax(): LinearTriadModel {
        impl.linearRelax()
        return this
    }

    override fun linearRelaxed(): LinearTriadModel {
        return LinearTriadModel(
            impl = impl.linearRelaxed(),
            tokensInSolver = tokensInSolver,
            objective = objective.copy(),
            identityValidation = identityValidation
        )
    }

    /**
     * 构建对偶模型 / Build dual model
     *
     * @return 对偶线性三元模型 / Dual linear triad model
    */
    suspend fun dual(): LinearTriadModel {
        val dualVariables = this.constraints.indices.map {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (this.objective.category) {
                ObjectCategory.Maximum -> {
                    when (this.constraints.signs[it]) {
                        ConstraintRelation.LessEqual -> {
                            // <= constraint => y >= 0 / 小于等于约束 => 对偶变量 y >= 0
                            lowerBound = Flt64.zero
                        }

                        ConstraintRelation.GreaterEqual -> {
                            // >= constraint => y <= 0 / 大于等于约束 => 对偶变量 y <= 0
                            upperBound = Flt64.zero
                        }

                        else -> {}
                    }
                }

                ObjectCategory.Minimum -> {
                    when (this.constraints.signs[it]) {
                        ConstraintRelation.LessEqual -> {
                            // <= constraint => y <= 0 / 小于等于约束 => 对偶变量 y <= 0
                            upperBound = Flt64.zero
                        }

                        ConstraintRelation.GreaterEqual -> {
                            // >= constraint => y >= 0 / 大于等于约束 => 对偶变量 y >= 0
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
                        // x >= lb => lambda_lb <= 0 / x >= 下界 => 下界对偶变量 <= 0
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
                        // x <= ub => lambda_ub >= 0 / x <= 上界 => 上界对偶变量 >= 0
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
                        // lb <= x <= ub => lambda_lb <= 0, lambda_ub >= 0 / 下界 <= x <= 上界 => 下界对偶变量 <= 0, 上界对偶变量 >= 0
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
                        // x >= lb => lambda_lb >= 0 / x >= 下界 => 下界对偶变量 >= 0
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
                        // x <= ub => lambda_ub <= 0 / x <= 上界 => 上界对偶变量 <= 0
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
                        // lb <= x <= ub => lambda_lb >= 0, lambda_ub <= 0 / 下界 <= x <= 上界 => 下界对偶变量 >= 0, 上界对偶变量 <= 0
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

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this@LinearTriadModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }
        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map { col ->
                async(Dispatchers.Default) {
                    coefficients[col].map { cell ->
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = cell.first,
                            coefficient = cell.second
                        )
                    } + listOfNotNull(boundDualVariables[col].first, boundDualVariables[col].second).map {
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = it.index,
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
                        // negative normalized => <= constraint / 非正变量 => 小于等于约束
                        ConstraintRelation.LessEqual
                    }

                    ObjectCategory.Minimum -> {
                        // negative normalized => >= constraint / 非正变量 => 大于等于约束
                        ConstraintRelation.GreaterEqual
                    }
                }
            } else if (it.positiveNormalized) {
                // positive normalized variable / 非负变量
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        // positive normalized => >= constraint / 非负变量 => 大于等于约束
                        ConstraintRelation.GreaterEqual
                    }

                    ObjectCategory.Minimum -> {
                        // positive normalized => <= constraint / 非负变量 => 小于等于约束
                        ConstraintRelation.LessEqual
                    }
                }
            } else {
                ConstraintRelation.Equal
            }
        }
        val rhs = this.variables.map { col ->
            this.objective.objective.find { it.colIndex == col.index }?.coefficient ?: Flt64.zero
        }
        val names = this.variables.map { "${it.name}_dual" }
        val sources = this.variables.map { ConstraintSource.Dual }

        val objective = constraints.indices.map {
            LinearObjectiveCell(
                colIndex = it,
                coefficient = this.constraints.rhs[it]
            )
        } + boundDualVariables.flatMapIndexed { col, (lb, ub) ->
            listOfNotNull(
                lb?.let {
                    LinearObjectiveCell(
                        colIndex = lb.index,
                        coefficient = this.variables[col].lowerBound
                    )
                },
                ub?.let {
                    LinearObjectiveCell(
                        colIndex = it.index,
                        coefficient = this.variables[col].upperBound
                    )
                }
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = (dualVariables + boundDualVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = LinearConstraintBatch(
                    sparseLhs = buildLinearSparseLhs(lhs),
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
            objective = LinearObjective(
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
    override suspend fun farkasDual(): LinearTriadModel {
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
                // x >= lb => mu_lb <= 0 / x >= 下界 => 下界对偶变量 mu_lb <= 0
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
                // x <= ub => mu_ub >= 0 / x <= 上界 => 上界对偶变量 mu_ub >= 0
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
                // lb <= x <= ub => mu_lb <= 0, mu_ub >= 0 / 下界 <= x <= 上界 => 下界对偶变量 <= 0, 上界对偶变量 >= 0
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

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this@LinearTriadModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }

        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map { col ->
                async(Dispatchers.Default) {
                    coefficients[col].map { cell ->
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = cell.first,
                            coefficient = cell.second
                        )
                    } + listOfNotNull(boundVariables[col].first, boundVariables[col].second).map {
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = it.index,
                            coefficient = Flt64.one
                        )
                    }
                }
            } + listOf(async(Dispatchers.Default) {
                this@LinearTriadModel.constraints.indices.map {
                    LinearConstraintCell(
                        rowIndex = this@LinearTriadModel.variables.size,
                        colIndex = farkasVariables[it].index,
                        coefficient = this@LinearTriadModel.constraints.rhs[it]
                    )
                } + this@LinearTriadModel.variables.flatMapIndexed { col, variable ->
                    listOfNotNull(
                        boundVariables[col].first?.let {
                            LinearConstraintCell(
                                rowIndex = this@LinearTriadModel.variables.size,
                                colIndex = it.index,
                                coefficient = variable.lowerBound
                            )
                        },
                        boundVariables[col].second?.let {
                            LinearConstraintCell(
                                rowIndex = this@LinearTriadModel.variables.size,
                                colIndex = it.index,
                                coefficient = variable.upperBound
                            )
                        }
                    )
                }
            })
            val slackConstraintPromises = async(Dispatchers.Default) {
                var rowIndex = this@LinearTriadModel.variables.size + 1
                var i = 0
                this@LinearTriadModel.constraints.indices.mapNotNull {
                    when (this@LinearTriadModel.constraints.signs[it]) {
                        ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                            null
                        }

                        ConstraintRelation.Equal -> {
                            val result = listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = farkasVariables[it].index,
                                    coefficient = Flt64.one
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = slackVariables[2 * i].index,
                                    coefficient = -Flt64.one
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = slackVariables[2 * i + 1].index,
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
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        } + negFarkasVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = -Flt64.one
            )
        } + slackVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
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
        val constraintSourceIds = this.variables.map { it.id?.value }
        val constraintIds = constraintSourceIds.mapIndexed { index, sourceId ->
            ConstraintId(
                derivedModelElementIdentity(
                    kind = ModelElementKind.Constraint,
                    role = "farkas-balance",
                    sourceId = sourceId,
                    sourceScope = this.variables[index].identityScope,
                    sourceOrigin = this.variables[index].identityOrigin,
                    sourceProvenance = this.variables[index].identityProvenanceOrOrigin(),
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

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = (derivedFarkasVariables + derivedSlackVariables + derivedBoundVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = LinearConstraintBatch(
                    sparseLhs = buildLinearSparseLhs(lhs),
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
            objective = LinearObjective(
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
    override fun feasibility(): LinearTriadModel {
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
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = slack.index,
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
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = slack.index,
                                coefficient = -Flt64.one
                            ),
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = artifact.index,
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
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = artifact.index,
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
        val constraints = LinearConstraintBatch(
            sparseLhs = buildLinearSparseLhs(lhs),
            signs = this.constraints.indices.map {
                ConstraintRelation.Equal
            },
            rhs = this.constraints.indices.map {
                abs(this.constraints.rhs[it])
            },
            names = this.constraints.indices.map {
                "${this.constraints.names[it].ifEmpty { "cons${it}" }}_feasibility"
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
            LinearObjectiveCell(
                colIndex = it.index,
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
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = this.variables + (slackVariables + artifactVariables).sortedBy { it.index },
                constraints = constraints,
                name = "$name-feasibility"
            ),
            tokensInSolver = tokensInSolver,
            objective = LinearObjective(
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
    override fun elastic(
        minmaxSlack: Boolean,
        minSlackAmount: Pair<UInt64, Flt64>?
    ): LinearTriadModel {
        return buildElasticModel(minmaxSlack, minSlackAmount)
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
                writer.append("${variables[cell.colIndex]}")
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
