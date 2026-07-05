/**
 * 元模型导出支持
 * MetaModel export support
 */
package fuookami.ospf.kotlin.core.model.mechanism

import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.token.*

/**
 * MetaModel 导出支持。
 * MetaModel export support.
 *
 * 说明：将模型导出为文本化诊断格式（当前为 `.opm`），用于问题定位与回归对比。
 * Note: exports models to text-oriented diagnostic format (currently `.opm`) for troubleshooting and regression comparison.
 */
/**
 * 将符号转换为 OPM 表达式文本 / Convert a symbol to OPM expression text
 * @param symbol 符号 / Symbol
 * @param unfold 展开层数 / Unfold depth
 * @return OPM 表达式文本 / OPM expression text
 */
private fun symbolToOpmString(symbol: Symbol, unfold: UInt64): String {
    return when {
        symbol is IntermediateSymbol<*> && unfold neq UInt64.zero -> symbol.toRawString(unfold - UInt64.one)
        else -> symbol.displayName ?: symbol.name
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
 * 将 Flt64 线性单项式转换为 OPM 表达式文本 / Convert a Flt64 linear monomial to OPM expression text
 * @param unfold 展开层数 / Unfold depth
 * @return OPM 表达式文本 / OPM expression text
 */
private fun LinearMonomial<Flt64>.toOpmString(unfold: UInt64): String {
    val symbolText = symbolToOpmString(symbol, unfold)
    return when {
        coefficient eq Flt64.one -> symbolText
        coefficient eq -Flt64.one -> "-$symbolText"
        else -> "$coefficient * $symbolText"
    }
}

/**
 * 将 Flt64 二次单项式转换为 OPM 表达式文本 / Convert a Flt64 quadratic monomial to OPM expression text
 * @param unfold 展开层数 / Unfold depth
 * @return OPM 表达式文本 / OPM expression text
 */
private fun QuadraticMonomial<Flt64>.toOpmString(unfold: UInt64): String {
    val symbol1Text = symbolToOpmString(symbol1, unfold)
    val termText = if (symbol2 != null) {
        val symbol2Text = symbolToOpmString(symbol2!!, unfold)
        if (symbol1 == symbol2) {
            "$symbol1Text^2"
        } else {
            "$symbol1Text * $symbol2Text"
        }
    } else {
        symbol1Text
    }
    return when {
        coefficient eq Flt64.one -> termText
        coefficient eq -Flt64.one -> "-$termText"
        else -> "$coefficient * $termText"
    }
}

/**
 * 合并 OPM 表达式文本中的项 / Join terms in an OPM expression text
 * @param terms 项列表 / List of terms
 * @param constant 常数项 / Constant term
 * @return OPM 表达式文本 / OPM expression text
 */
private fun termsToOpmString(terms: List<String>, constant: Flt64): String {
    val allTerms = terms.toMutableList()
    if (constant neq Flt64.zero) {
        allTerms.add(constant.toString())
    }
    return allTerms.ifEmpty { listOf(Flt64.zero.toString()) }
        .mapIndexed { index, term ->
            when {
                index == 0 -> term
                term.startsWith("-") -> " - ${term.removePrefix("-")}"
                else -> " + $term"
            }
        }
        .joinToString("")
}

/**
 * 将 Flt64 线性多项式转换为 OPM 表达式文本 / Convert a Flt64 linear polynomial to OPM expression text
 * @param unfold 展开层数 / Unfold depth
 * @return OPM 表达式文本 / OPM expression text
 */
private fun LinearPolynomial<Flt64>.toOpmString(unfold: UInt64 = UInt64.zero): String {
    return termsToOpmString(
        terms = monomials.filter { it.coefficient neq Flt64.zero }.map { it.toOpmString(unfold) },
        constant = constant
    )
}

/**
 * 将 Flt64 二次多项式转换为 OPM 表达式文本 / Convert a Flt64 quadratic polynomial to OPM expression text
 * @param unfold 展开层数 / Unfold depth
 * @return OPM 表达式文本 / OPM expression text
 */
private fun QuadraticPolynomial<Flt64>.toOpmString(unfold: UInt64 = UInt64.zero): String {
    return termsToOpmString(
        terms = monomials.filter { it.coefficient neq Flt64.zero }.map { it.toOpmString(unfold) },
        constant = constant
    )
}

/** 将泛型二次多项式转换为 Flt64 类型 / Convert a generic quadratic polynomial to Flt64 type */
private fun <V> QuadraticPolynomial<V>.toFlt64Poly(converter: IntoValue<V>): QuadraticPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map {
            QuadraticMonomial(
                coefficient = converter.fromValue(it.coefficient),
                symbol1 = it.symbol1,
                symbol2 = it.symbol2
            )
        },
        converter.fromValue(constant)
    )
}

/**
 * 将比较运算符转换为 OPM 文本 / Convert a comparison operator to OPM text
 * @return OPM 文本 / OPM text
 */
private fun Comparison.toOpmString(): String {
    return symbol
}

/**
 * 将约束名转换为 OPM 前缀 / Convert a constraint name to OPM prefix
 * @param name 约束名称 / Constraint name
 * @param displayName 约束显示名称 / Constraint display name
 * @return OPM 前缀文本 / OPM prefix text
 */
private fun constraintNamePrefix(name: String, displayName: String?): String {
    val text = displayName?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() }
    return if (text != null) {
        "$text: "
    } else {
        ""
    }
}

/** 将线性约束转换为 OPM 文本 / Convert a linear constraint to OPM text */
private fun <V> LinearInequalityConstraint<V>.toOpmString(unfold: UInt64): String
        where V : RealNumber<V>, V : NumberField<V> {
    val lhs = inequality.lhs.toFlt64Poly(converter).toOpmString(unfold)
    val rhs = inequality.rhs.toFlt64Poly(converter).toOpmString(unfold)
    return "${constraintNamePrefix(name, displayName)}$lhs ${sign.toOpmString()} $rhs"
}

/** 将二次约束转换为 OPM 文本 / Convert a quadratic constraint to OPM text */
private fun <V> QuadraticInequalityConstraint<V>.toOpmString(unfold: UInt64): String
        where V : RealNumber<V>, V : NumberField<V> {
    val lhs = inequality.lhs.toFlt64Poly(converter).toOpmString(unfold)
    val rhs = inequality.rhs.toFlt64Poly(converter).toOpmString(unfold)
    return "${constraintNamePrefix(name, displayName)}$lhs ${sign.toOpmString()} $rhs"
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
            writer.append("${obj.category} ${obj.name}: ${obj.polynomial.toFlt64Poly(metaModel.converter).toOpmString(unfold)} \n")
        }
        writer.append("\n")

        writer.append("Subject to:\n")
        when (metaModel) {
            is LinearMetaModel<*> -> {
                @Suppress("UNCHECKED_CAST")
                val linearMetaModel = metaModel as LinearMetaModel<V>
                for (constraint in linearMetaModel.relationConstraints) {
                    writer.append("${constraint.toOpmString(unfold)}\n")
                }
            }

            is QuadraticMetaModel<*> -> {
                @Suppress("UNCHECKED_CAST")
                val quadraticMetaModel = metaModel as QuadraticMetaModel<V>
                for (constraint in quadraticMetaModel.relationConstraints) {
                    writer.append("${constraint.toOpmString(unfold)}\n")
                }
            }

            else -> {
                for (constraint in metaModel.constraints) {
                    writer.append("$constraint\n")
                }
            }
        }
        writer.append("\n")

        ok
    }
}
