/**
 * 模型视图
 * Model view
 */
package fuookami.ospf.kotlin.core.model.basic

import java.io.*
import java.nio.file.Path
import kotlin.io.path.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.variable.*

/**
 * 变量松弛信息，关联变量与其约束或界限。
 * Variable slack information associating a variable with its constraint or bounds.
 *
 * @property constraint 关联的约束（可为 null） / The associated constraint (nullable)
 * @property lowerBound 下界变量（可为 null） / Lower bound variable (nullable)
 * @property upperBound 上界变量（可为 null） / Upper bound variable (nullable)
 */
data class VariableSlack(
    val constraint: Constraint<Flt64, *>? = null,
    val lowerBound: Variable? = null,
    val upperBound: Variable? = null
)

/**
 * 求解器中的变量，封装变量的界限、类型和初始值等属性。
 * Variable in the solver, encapsulating bounds, type, and initial value properties.
 *
 * @property index         变量在求解器中的索引 / Variable index in the solver
 * @param    lowerBound    下界 / Lower bound
 * @param    upperBound    上界 / Upper bound
 * @param    type          变量类型 / Variable type
 * @property origin        原始抽象变量项 / The originating abstract variable item
 * @property dualOrigin    对偶约束来源 / The dual constraint origin
 * @property slack         松弛信息 / Slack information
 * @property name          变量名 / Variable name
 * @property initialResult 初始值（可为 null） / Initial value (nullable)
 */
class Variable(
    val index: Int,
    lowerBound: Flt64,
    upperBound: Flt64,
    type: VariableType<*>,
    val origin: AbstractVariableItem<*, *>?,
    val dualOrigin: Constraint<Flt64, *>? = null,
    val slack: VariableSlack? = null,
    val name: String,
    val initialResult: Flt64? = null
) : Cloneable, Copyable<Variable> {
    internal var _lowerBound = lowerBound
    internal var _upperBound = upperBound
    internal var _type = type

    val lowerBound by ::_lowerBound
    val upperBound by ::_upperBound
    val type by ::_type

    /** 是否无界（正负方向均自由） / Whether the variable is free (unbounded in both directions) */
    val free: Boolean
        get() {
            return negativeFree && positiveFree
        }

    /** 是否已归一化（正方向或负方向归一化） / Whether the variable is normalized (in either direction) */
    val normalized: Boolean
        get() {
            return negativeNormalized || positiveNormalized
        }

    /** 负方向是否归一化（负自由且上界为零） / Whether negative-normalized (negative-free and upper bound is zero) */
    val negativeNormalized: Boolean
        get() {
            return negativeFree && upperBound eq Flt64.zero
        }

    /** 负方向是否自由（下界为负无穷） / Whether negative-free (lower bound is negative infinity) */
    val negativeFree: Boolean
        get() {
            return (lowerBound eq Flt64.negativeInfinity || lowerBound leq -Flt64.decimalPrecision.reciprocal())
        }

    /** 正方向是否归一化（正自由且下界为零） / Whether positive-normalized (positive-free and lower bound is zero) */
    val positiveNormalized: Boolean
        get() {
            return positiveFree && lowerBound eq Flt64.zero
        }

    /** 正方向是否自由（上界为正无穷） / Whether positive-free (upper bound is positive infinity) */
    val positiveFree: Boolean
        get() {
            return (upperBound eq Flt64.infinity || upperBound geq Flt64.decimalPrecision.reciprocal())
        }

    override fun copy() = Variable(index, lowerBound, upperBound, type, origin, dualOrigin, slack, name, initialResult)
    override fun clone() = copy()

    override fun toString() = name
}

/**
 * 模型单元格接口，持有系数值。
 * Model cell interface holding a coefficient value.
 */
interface ModelCell<Self : ModelCell<Self>> {
    val coefficient: Flt64

    /** 返回系数取反后的新单元格 / Return a new cell with the coefficient negated */
    operator fun unaryMinus(): Self
}

/**
 * 约束单元格接口，扩展 ModelCell 增加行索引。
 * Constraint cell interface extending ModelCell with a row index.
 */
interface ConstraintCell<Self : ConstraintCell<Self>> : ModelCell<Self> {
    val rowIndex: Int
}

/**
 * 约束来源枚举，标识约束的产生途径。
 * Constraint source enumeration indicating how a constraint was produced.
 */
enum class ConstraintSource {
    /** 原始约束 / Original constraint */
    Origin,
    /** 下界约束 / Lower bound constraint */
    LowerBound,
    /** 上界约束 / Upper bound constraint */
    UpperBound,
    /** 对偶约束 / Dual constraint */
    Dual,
    /** Farkas 对偶约束 / Farkas dual constraint */
    FarkasDual,
    /** 可行性约束 / Feasibility constraint */
    Feasibility,
    /** 弹性约束 / Elastic constraint */
    Elastic,
    /** 弹性下界约束 / Elastic lower bound constraint */
    ElasticLowerBound,
    /** 弹性上界约束 / Elastic upper bound constraint */
    ElasticUpperBound,
    /** 弹性松弛二元约束 / Elastic slack binary constraint */
    ElasticSlackBinary,
    /** 弹性松弛最小最大约束 / Elastic slack minmax constraint */
    ElasticSlackMinmax
}

/**
 * 模型约束的抽象基类，管理约束的左端、符号、右端、名称和来源。
 * Abstract base class for model constraints, managing LHS, signs, RHS, names, and sources.
 *
 * @property constraintCount 约束数量 / Number of constraints
 * @param    signs           约束关系列表 / List of constraint relations
 * @param    rhs             右端值列表 / List of right-hand side values
 * @param    names           约束名称列表 / List of constraint names
 * @param    sources         约束来源列表 / List of constraint sources
 */
abstract class ModelConstraint<ConCell>(
    val constraintCount: Int,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>
) : Cloneable, Copyable<ModelConstraint<ConCell>>, AutoCloseable
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    internal val _signs = signs.toMutableList()
    internal val _rhs = rhs.toMutableList()
    internal val _names = names.toMutableList()
    internal val _sources = sources.toMutableList()

    abstract val lhs: List<List<ConCell>>
    val signs: List<ConstraintRelation> by ::_signs
    val rhs: List<Flt64> by ::_rhs
    val names: List<String> by ::_names
    val sources: List<ConstraintSource> by ::_sources

    val size: Int get() = rhs.size
    val indices: IntRange get() = rhs.indices

    override fun clone() = copy()

    override fun close() {
        _signs.clear()
        _rhs.clear()
        _names.clear()
        _sources.clear()
    }
}

/**
 * 目标函数，包含优化方向、目标单元格列表和常数项。
 * Objective function containing optimization direction, objective cell list, and constant.
 *
 * @property category  优化方向 / Optimization direction
 * @property objective 目标单元格列表 / List of objective cells
 * @property constant  常数项 / Constant term
 */
class Objective<C : Copyable<C>>(
    val category: ObjectCategory,
    val objective: List<C>,
    val constant: Flt64 = Flt64(0.0)
) : Cloneable, Copyable<Objective<C>> {
    override fun copy() = Objective(category, objective.toList())
    override fun clone() = copy()
}

/**
 * 基本模型视图接口，提供变量、约束、名称及导出能力。
 * Basic model view interface providing variables, constraints, name, and export capability.
 */
interface BasicModelView<ConCell> : AutoCloseable
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    val variables: List<Variable>
    val constraints: ModelConstraint<ConCell>
    val name: String

    /** 是否包含连续变量 / Whether the model contains continuous variables */
    val containsContinuous: Boolean
        get() {
            return variables.any { it.type.isContinuousType }
        }

    /** 是否包含二进制变量 / Whether the model contains binary variables */
    val containsBinary: Boolean
        get() {
            return variables.any { it.type.isBinaryType }
        }

    /** 是否包含整数变量 / Whether the model contains integer variables */
    val containsInteger: Boolean
        get() {
            return variables.any { it.type.isIntegerType }
        }

    /** 是否包含非二进制整数变量 / Whether the model contains non-binary integer variables */
    val containsNotBinaryInteger: Boolean
        get() {
            return variables.any { it.type.isNotBinaryIntegerType }
        }

    /**
     * 使用默认路径和指定格式导出模型。
     * Export the model using the default path and specified format.
     *
     * @param format 文件格式 / File format
     * @return 导出结果 / Export result
     */
    fun export(format: ModelFileFormat): Try {
        return export(Path("."), format)
    }

    /**
     * 使用指定文件名和格式导出模型到当前目录。
     * Export the model to the current directory using the given file name and format.
     *
     * @param name   文件名 / File name
     * @param format 文件格式 / File format
     * @return 导出结果 / Export result
     */
    fun export(name: String, format: ModelFileFormat): Try {
        return export(Path(".").resolve(name), format)
    }

    /**
     * 使用指定路径和格式导出模型到文件。
     * Export the model to a file at the given path using the specified format.
     *
     * @param path   导出路径 / Export path
     * @param format 文件格式 / File format
     * @return 导出结果 / Export result
     */
    fun export(path: Path, format: ModelFileFormat): Try {
        val file = if (path.isDirectory()) {
            path.resolve("$name.${format}").toFile()
        } else {
            path.toFile()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file)
        val result = when (format) {
            ModelFileFormat.LP -> {
                exportLP(writer)
            }
        }
        writer.flush()
        writer.close()
        return result
    }

    /**
     * 将模型以 LP 格式写入给定输出流。
     * Write the model in LP format to the given output stream.
     *
     * @param writer 输出流写入器 / Output stream writer
     * @return 导出结果 / Export result
     */
    fun exportLP(writer: OutputStreamWriter): Try

    override fun close() {
        constraints.close()
    }
}

/**
 * 完整模型视图接口，在 BasicModelView 基础上增加目标函数。
 * Full model view interface adding an objective function on top of BasicModelView.
 */
interface ModelView<ConCell, ObjCell> : BasicModelView<ConCell>
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell>, ObjCell : ModelCell<ObjCell>, ObjCell : Copyable<ObjCell> {
    val objective: Objective<ObjCell>
}
