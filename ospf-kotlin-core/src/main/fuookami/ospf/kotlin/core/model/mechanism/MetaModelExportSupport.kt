/**
 * 元模型导出支持
 * MetaModel export support
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlinx.coroutines.*

/**
 * MetaModel 导出支持。
 * MetaModel export support.
 *
 * 说明：将模型导出为文本化诊断格式（当前为 `.opm`），用于问题定位与回归对比。
 * Note: exports models to text-oriented diagnostic format (currently `.opm`) for troubleshooting and regression comparison.
 */
/** 将 Flt64 线性多项式转换为原始字符串表示 / Convert a Flt64 linear polynomial to raw string representation */
private fun LinearPolynomial<Flt64>.toRawString(unfold: UInt64 = UInt64.zero): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (constant neq Flt64.zero) {
        "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }} + $constant"
    } else {
        monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }
    }
}

/** 将泛型线性多项式转换为 Flt64 类型 / Convert a generic linear polynomial to Flt64 type */
private fun <V> LinearPolynomial<V>.toFlt64Poly(converter: IntoValue<V>): LinearPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
        converter.fromValue(constant)
    )
}

/**
 * 导出 MetaModel 到目标路径；当路径是目录时自动生成 `<model>.opm` 文件名。
 * Export MetaModel to target path; when path is a directory, `<model>.opm` is generated automatically.
 */
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

/**
 * 按 OPM 文本格式写出模型主体内容。
 * Write model body in OPM text format.
 */
/** 按 OPM 文本格式写出模型主体内容 / Write model body in OPM text format */
private suspend fun <V> exportMetaModelOpm(
    metaModel: MetaModel<V>,
    writer: FileWriter,
    unfold: UInt64
): Try where V : RealNumber<V>, V : NumberField<V> {
    val temp = when (metaModel.tokens) {
        is MutableTokenTable<*> -> metaModel.tokens.copy()
        is ConcurrentMutableTokenTable<*> -> metaModel.tokens.copy()
        else -> return Failed(ErrorCode.IllegalArgument, "Unknown token table type: ${metaModel.tokens::class}")
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
