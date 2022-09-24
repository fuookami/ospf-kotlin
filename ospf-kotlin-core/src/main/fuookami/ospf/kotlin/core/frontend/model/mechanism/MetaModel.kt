package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import java.io.FileWriter
import java.nio.file.*
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

sealed interface MetaModel<C: Category> {
    class SubObject<C: Category>(
        val category: ObjectCategory,
        val polynomial: Polynomial<C>,
        val name: String = polynomial.name
    )

    val name: String
    val constraints: MutableList<Inequality<C>>
    val objectCategory: ObjectCategory
    val subObjects: MutableList<SubObject<C>>
    val tokens: TokenTable<C>

    fun addVar(item: Item<*, *>) { tokens.add(item) }
    fun addVars(items: Combination<*, *, *>) { tokens.add(items) }
    fun addVars(items: CombinationView<*, *>) { tokens.add(items) }

    fun remove(item: Item<*, *>) { tokens.remove(item) }

    fun addSymbol(symbol: Symbol<C>) { tokens.add(symbol) }
    fun addSymbols(symbols: SymbolCombination<C, *>) { tokens.add(symbols) }
    fun addSymbols(symbols: SymbolView<C>) { tokens.add(symbols) }

    fun addConstraint(inequality: Inequality<C>, name: String? = null, displayName: String? = null) {
        if (name != null) {
            inequality.name = name
        }
        if (displayName != null) {
            inequality.displayName = name
        }
        constraints.add(inequality)
    }

    fun addObject(category: ObjectCategory, polynomial: Polynomial<C>, name: String? = null, displayName: String? = null) {
        if (name != null) {
            polynomial.name = name
        }
        if (displayName != null) {
            polynomial.displayName = displayName
        }
        subObjects.add(SubObject(category, polynomial))
    }
    fun minimize(polynomial: Polynomial<C>, name: String? = null, displayName: String? = null) { addObject(ObjectCategory.Minimum, polynomial, name, displayName) }
    fun maximize(polynomial: Polynomial<C>, name: String? = null, displayName: String? = null) { addObject(ObjectCategory.Maximum, polynomial, name, displayName) }

    fun flush() {
        for (constraint in constraints) {
            constraint.lhs.flush()
            constraint.rhs.flush()
        }
        for (objective in subObjects) {
            objective.polynomial.flush()
        }
    }

    fun export(): Try<Error> {
        return export("$name.opm")
    }

    fun export(name: String): Try<Error> {
        return export(kotlin.io.path.Path(".").resolve(name))
    }

    fun export(path: Path): Try<Error> {
        val file = if (path.isDirectory()) { path.resolve("$name.opm").toFile() } else { path.toFile() }
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file)
        val result = when (file.extension) {
            "opm" -> { exportOpm(writer) }
            // todo: raise error with unknown format
            else -> { Ok(success) }
        }
        writer.flush()
        writer.close()
        return result
    }

    private fun exportOpm(writer: FileWriter): Try<Error> {
        writer.append("Model Name: $name\n")
        writer.append("\n")

        writer.append("Variables:\n")
        for (token in tokens.tokens.toList().sortedBy { it.solverIndex }) {
            val range = token.range
            writer.append("${token.name}, ${token.type}, ")
            if (range.empty()) {
                writer.append("empty\n")
            } else {
                writer.append("${range.lowerInterval.toLowerSign()}${range.lowerBound}, ${range.upperBound}${range.upperInterval.toUpperSign()}\n")
            }
        }
        writer.append("\n")

        writer.append("Symbols:\n")
        for (symbol in tokens.symbols.toList().sortedBy { it.name }) {
            val range = symbol.range
            writer.append("${symbol.name} = $symbol, ")
            if (range.empty()) {
                writer.append("empty")
            } else {
                writer.append("${range.lowerInterval.toLowerSign()}${range.lowerBound}, ${range.upperBound}${range.upperInterval.toUpperSign()}\n")
            }
        }
        writer.append("\n")

        writer.append("Objectives:\n")
        for (obj in subObjects) {
            writer.append("${obj.category} ${obj.name}: ${obj.polynomial} \n")
        }
        writer.append("\n")

        writer.append("Subject to:\n")
        for (constraint in constraints) {
            writer.append("${constraint.name}: $constraint\n")
        }
        writer.append("\n")

        return Ok(success)
    }
}

class LinearMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    manualTokenAddition: Boolean = true
) : MetaModel<Linear> {
    override val constraints: ArrayList<Inequality<Linear>> = ArrayList()
    override val subObjects: ArrayList<MetaModel.SubObject<Linear>> = ArrayList()
    override val tokens: TokenTable<Linear> = if (manualTokenAddition) { ManualAddTokenTable() } else { AutoAddTokenTable() }
}
