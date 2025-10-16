package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

typealias OriginConstraint = fuookami.ospf.kotlin.core.frontend.model.mechanism.Constraint

data class VariableSlack(
    val constraint: OriginConstraint? = null,
    val lowerBound: Variable? = null,
    val upperBound: Variable? = null
)

class Variable(
    val index: Int,
    lowerBound: Flt64,
    upperBound: Flt64,
    type: VariableType<*>,
    val origin: AbstractVariableItem<*, *>?,
    val dualOrigin: OriginConstraint? = null,
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

    val free: Boolean
        get() {
            return negativeFree && positiveFree
        }

    val normalized: Boolean
        get() {
            return negativeNormalized || positiveNormalized
        }

    val negativeNormalized: Boolean
        get() {
            return negativeFree && upperBound eq Flt64.zero
        }

    val negativeFree: Boolean
        get() {
            return (lowerBound eq Flt64.negativeInfinity || lowerBound leq -Flt64.decimalPrecision.reciprocal())
        }

    val positiveNormalized: Boolean
        get() {
            return positiveFree && lowerBound eq Flt64.zero
        }

    val positiveFree: Boolean
        get() {
            return (upperBound eq Flt64.infinity || upperBound geq Flt64.decimalPrecision.reciprocal())
        }

    override fun copy() = Variable(index, lowerBound, upperBound, type, origin, dualOrigin, slack, name, initialResult)
    override fun clone() = copy()

    override fun toString() = name
}

interface Cell<Self : Cell<Self>> {
    val coefficient: Flt64

    operator fun unaryMinus(): Self
}

interface ConstraintCell<Self : ConstraintCell<Self>> : Cell<Self> {
    val rowIndex: Int
}

enum class ConstraintSource {
    Origin,
    LowerBound,
    UpperBound,
    Dual,
    FarkasDual,
    Feasibility,
    Elastic,
    ElasticLowerBound,
    ElasticUpperBound
}

abstract class Constraint<Cell>(
    lhs: List<List<Cell>>,
    signs: List<Sign>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>
) : Cloneable, Copyable<Constraint<Cell>>
        where Cell : ConstraintCell<Cell>, Cell : Copyable<Cell> {
    internal val _lhs = lhs.toMutableList()
    internal val _signs = signs.toMutableList()
    internal val _rhs = rhs.toMutableList()
    internal val _names = names.toMutableList()
    internal val _sources = sources.toMutableList()

    val lhs: List<List<Cell>> by ::_lhs
    val signs: List<Sign> by ::_signs
    val rhs: List<Flt64> by ::_rhs
    val names: List<String> by ::_names
    val sources: List<ConstraintSource> by ::_sources

    val size: Int get() = rhs.size
    val indices: IntRange get() = rhs.indices

    override fun clone() = copy()
}

class Objective<Cell : Copyable<Cell>>(
    val category: ObjectCategory,
    val obj: List<Cell>,
    val constant: Flt64 = Flt64(0.0)
) : Cloneable, Copyable<Objective<Cell>> {
    override fun copy() = Objective(category, obj.toList())
    override fun clone() = copy()
}

interface BasicModelView<ConCell>
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    val variables: List<Variable>
    val constraints: Constraint<ConCell>
    val name: String

    val containsContinuous: Boolean
        get() {
            return variables.any { it.type.isContinuousType }
        }

    val containsBinary: Boolean
        get() {
            return variables.any { it.type.isBinaryType }
        }

    val containsInteger: Boolean
        get() {
            return variables.any { it.type.isIntegerType }
        }

    val containsNotBinaryInteger: Boolean
        get() {
            return variables.any { it.type.isNotBinaryIntegerType }
        }

    fun export(format: ModelFileFormat): Try {
        return export(Path("."), format)
    }

    fun export(name: String, format: ModelFileFormat): Try {
        return export(Path(".").resolve(name), format)
    }

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

    fun exportLP(writer: OutputStreamWriter): Try
}

interface ModelView<ConCell, ObjCell> : BasicModelView<ConCell>
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell>, ObjCell : Cell<ObjCell>, ObjCell : Copyable<ObjCell> {
    val objective: Objective<ObjCell>
}
