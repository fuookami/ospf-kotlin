package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.neq
import fuookami.ospf.kotlin.utils.functional.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.isDirectory

private fun LinearPolynomial<Flt64>.toRawString(unfold: UInt64 = UInt64.zero): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (constant neq Flt64.zero) {
        "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }} + $constant"
    } else {
        monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }
    }
}

private fun <V> LinearPolynomial<V>.toFlt64Poly(converter: IntoValue<V>): LinearPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
        converter.fromValue(constant)
    )
}

internal suspend fun <V> exportMetaModel(
    metaModel: MetaModel<V>,
    path: Path,
    unfold: UInt64 = UInt64.zero
): Try where V : RealNumber<V>, V : NumberField<V> {
    val file = if (path.isDirectory()) {
        path.resolve("${metaModel.name}.opm").toFile()
    } else {
        path.toFile()
    }
    if (!file.exists()) {
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }
    }
    val writer = withContext(Dispatchers.IO) {
        FileWriter(file)
    }
    val result = when (file.extension) {
        "opm" -> {
            exportMetaModelOpm(metaModel, writer, unfold)
        }

        else -> {
            ok
        }
    }
    withContext(Dispatchers.IO) {
        writer.flush()
        writer.close()
    }
    return result
}

private suspend fun <V> exportMetaModelOpm(
    metaModel: MetaModel<V>,
    writer: FileWriter,
    unfold: UInt64
): Try where V : RealNumber<V>, V : NumberField<V> {
    val temp = when (metaModel.tokens) {
        is MutableTokenTable<*> -> metaModel.tokens.copy()
        is ConcurrentMutableTokenTable<*> -> metaModel.tokens.copy()
        else -> throw IllegalStateException("Unknown token table type: ${metaModel.tokens::class}")
    }

    for (symbol in metaModel.tokens.symbols) {
        if (symbol is MathFunctionSymbolBase<*>) {
            when (val result = SolverBoundaryCasts.registerAuxiliaryTokensStar(symbol, temp)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
    }

    return withContext(Dispatchers.IO) {
        writer.append("Model Name: ${metaModel.name}\n")
        writer.append("\n")

        writer.append("Variables:\n")
        for (token in metaModel.tokens.tokens.toList().sortedBy { it.solverIndex }) {
            val range = token.range
            writer.append("${token.name}, ${token.type}, ")
            if (range == null) {
                writer.append("empty\n")
            } else {
                writer.append("${range}\n")
            }
        }
        writer.append("\n")

        writer.append("Symbols:\n")
        for (symbol in metaModel.tokens.symbols.toList().sortedBy { it.name }) {
            val range = symbol.range
            writer.append("$symbol = ${symbol.toRawString(UInt64.one)}, ")
            if (range.empty) {
                writer.append("empty")
            } else {
                writer.append("${range}\n")
            }
        }
        writer.append("\n")

        writer.append("Objectives:\n")
        for (obj in metaModel.subObjects) {
            writer.append("${obj.category} ${obj.name}: ${obj.polynomial.toFlt64Poly(metaModel.converter).toRawString(unfold)} \n")
        }
        writer.append("\n")

        writer.append("Subject to:\n")
        for (constraint in metaModel.constraints) {
            writer.append("$constraint\n")
        }
        writer.append("\n")

        ok
    }
}

