package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class Variable(
    val index: Int,
    lowerBound: Flt64,
    upperBound: Flt64,
    type: VariableType<*>,
    val name: String,
    val initialResult: Flt64? = null
) : Cloneable, Copyable<Variable> {
    internal var _lowerBound = lowerBound
    internal var _upperBound = upperBound
    internal var _type = type

    val lowerBound by ::_lowerBound
    val upperBound by ::_upperBound
    val type by ::_type

    override fun copy() = Variable(index, lowerBound, upperBound, type, name, initialResult)
    override fun clone() = copy()

    override fun toString() = name
}

interface Cell {
    val coefficient: Flt64
}

interface ConstraintCell : Cell {
    val rowIndex: Int
}

class Constraint<Cell>(
    lhs: List<List<Cell>>,
    signs: List<Sign>,
    rhs: List<Flt64>,
    names: List<String>
) : Cloneable, Copyable<Constraint<Cell>>
        where Cell : ConstraintCell, Cell : Copyable<Cell> {
    internal val _lhs = lhs.toMutableList()
    internal val _signs = signs.toMutableList()
    internal val _rhs = rhs.toMutableList()
    internal val _names = names.toMutableList()

    val lhs: List<List<Cell>> by ::_lhs
    val signs: List<Sign> by ::_signs
    val rhs: List<Flt64> by ::_rhs
    val names: List<String> by ::_names

    val size: Int get() = rhs.size
    val indices: IntRange get() = rhs.indices

    override fun copy() = Constraint(
        lhs.map { line -> line.map { it.copy() } },
        signs.toList(),
        rhs.map { it.copy() },
        names.toList()
    )

    override fun clone() = copy()
}

class Objective<Cell : Copyable<Cell>>(
    val category: ObjectCategory,
    val obj: List<Cell>
) : Cloneable, Copyable<Objective<Cell>> {
    override fun copy() = Objective(category, obj.toList())
    override fun clone() = copy()
}

interface BasicModelView<ConCell>
        where ConCell : ConstraintCell, ConCell : Copyable<ConCell> {
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

    fun exportLP(writer: OutputStreamWriter): Try
}

interface ModelView<ConCell, ObjCell>
        where ConCell : ConstraintCell, ConCell : Copyable<ConCell>, ObjCell : Cell, ObjCell : Copyable<ObjCell> {
    val variables: List<Variable>
    val constraints: Constraint<ConCell>
    val objective: Objective<ObjCell>
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

    fun exportLP(writer: FileWriter): Try
}
