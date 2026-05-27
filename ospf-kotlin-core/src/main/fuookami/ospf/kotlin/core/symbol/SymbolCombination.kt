/**
 * 符号组合容器 / Symbol combination containers
 *
 * 提供基于 [MultiArray] 的中间符号组合容器，支持线性/二次表达式符号
 * 和中间符号的多维数组创建与管理。同时提供 [map] / [flatMap] 便捷工厂函数。
 *
 * Provides [MultiArray]-based containers for intermediate symbols, supporting
 * creation and management of multi-dimensional arrays of linear/quadratic
 * expression symbols and intermediate symbols. Also provides [map] / [flatMap]
 * convenience factory functions.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator

/**
 * 符号组合抽象接口 / Abstract symbol combination interface
 *
 * @property dimension 维度数 / Number of dimensions
 * @property identifier 全局唯一标识符 / Globally unique identifier
 * @property shape 数组形状 / Array shape
 */
interface AbstractSymbolCombination<S : Shape> {
    val dimension: Int
    val identifier: UInt64
    val shape: Shape
}

/**
 * 符号组合 / Symbol combination
 *
 * 基于 [MultiArray] 的中间符号多维数组容器。创建时自动为每个元素设置组引用和索引。
 *
 * A [MultiArray]-based multi-dimensional container for intermediate symbols.
 * Automatically sets group references and indices for each element on creation.
 *
 * @property name 组合名称 / Combination name
 */
class SymbolCombination<out Sym : IntermediateSymbol<*>, S : Shape>(
    val name: String,
    shape: S,
    ctor: (Int, IntArray) -> Sym
) : MultiArray<Sym, S>(shape, ctor), AbstractSymbolCombination<S> {
    override val identifier = IdentifierGenerator.gen()

    init {
        for ((i, sym) in this.withIndex()) {
            when (sym) {
                is LinearExpressionSymbol<*> -> {
                    sym._group = this
                    sym._index = i
                }

                is QuadraticExpressionSymbol<*> -> {
                    sym._group = this
                    sym._index = i
                }

                else -> {}
            }
        }
    }
}

/**
 * 量纲符号组合 / Quantity symbol combination
 *
 * 基于 [MultiArray] 的量纲中间符号多维数组容器。
 *
 * A [MultiArray]-based multi-dimensional container for quantity intermediate symbols.
 *
 * @property name 组合名称 / Combination name
 */
class QuantitySymbolCombination<out Sym : IntermediateSymbol<*>, S : Shape>(
    val name: String,
    shape: S,
    ctor: (Int, IntArray) -> Quantity<Sym>
) : MultiArray<Quantity<Sym>, S>(shape, ctor), AbstractSymbolCombination<S> {
    override val identifier = IdentifierGenerator.gen()

    init {
        for ((i, qsym) in this.withIndex()) {
            when (val sym = qsym.value) {
                is LinearExpressionSymbol<*> -> {
                    sym._group = this
                    sym._index = i
                }

                is QuadraticExpressionSymbol<*> -> {
                    sym._group = this
                    sym._index = i
                }

                else -> {}
            }
        }
    }
}

typealias LinearExpressionSymbols1<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape1>
typealias LinearExpressionSymbols2<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape2>
typealias LinearExpressionSymbols3<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape3>
typealias LinearExpressionSymbols4<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape4>
typealias DynLinearExpressionSymbols<V> = SymbolCombination<LinearExpressionSymbol<V>, DynShape>

typealias QuantityLinearExpressionSymbols1<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape1>
typealias QuantityLinearExpressionSymbols2<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape2>
typealias QuantityLinearExpressionSymbols3<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape3>
typealias QuantityLinearExpressionSymbols4<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape4>
typealias DynQuantityLinearExpressionSymbols<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, DynShape>

typealias LinearIntermediateSymbols1<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape1>
typealias LinearIntermediateSymbols2<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape2>
typealias LinearIntermediateSymbols3<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape3>
typealias LinearIntermediateSymbols4<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape4>
typealias DynLinearIntermediateSymbols<V> = SymbolCombination<LinearIntermediateSymbol<V>, DynShape>

typealias QuantityLinearIntermediateSymbols1<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape1>
typealias QuantityLinearIntermediateSymbols2<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape2>
typealias QuantityLinearIntermediateSymbols3<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape3>
typealias QuantityLinearIntermediateSymbols4<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape4>
typealias DynQuantityLinearIntermediateSymbols<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, DynShape>
typealias QuantityLinearIntermediateSymbol<V> = Quantity<LinearIntermediateSymbol<V>>

typealias QuadraticExpressionSymbols1<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape1>
typealias QuadraticExpressionSymbols2<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape2>
typealias QuadraticExpressionSymbols3<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape3>
typealias QuadraticExpressionSymbols4<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape4>
typealias DynQuadraticExpressionSymbols<V> = SymbolCombination<QuadraticExpressionSymbol<V>, DynShape>

typealias QuantityQuadraticExpressionSymbols1<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape1>
typealias QuantityQuadraticExpressionSymbols2<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape2>
typealias QuantityQuadraticExpressionSymbols3<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape3>
typealias QuantityQuadraticExpressionSymbols4<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape4>
typealias DynQuantityQuadraticExpressionSymbols<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, DynShape>

private fun symbolName(name: String, indices: IntArray): String {
    return "${name}_${indices.joinToString("_") { "$it" }}"
}

private fun <V> emptyLinearExpressionSymbol(
    name: String,
    zero: V
): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return LinearExpressionSymbol(
        _utilsPolynomial = MutableLinearPolynomial(
            monomials = emptyList(),
            constant = zero
        ),
        name = name
    )
}

private fun <V> emptyQuadraticExpressionSymbol(
    name: String,
    zero: V
): QuadraticExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return QuadraticExpressionSymbol(
        _utilsPolynomial = MutableQuadraticPolynomial(
            monomials = emptyList(),
            constant = zero
        ),
        category = Quadratic,
        name = name
    )
}

private fun <V> linearExpressionSymbol(
    polynomial: LinearPolynomial<V>,
    name: String
): LinearExpressionSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return LinearExpressionSymbol(
        _utilsPolynomial = MutableLinearPolynomial(
            monomials = polynomial.monomials,
            constant = polynomial.constant
        ),
        name = name
    )
}

/**
 * 线性中间符号工厂 / Linear intermediate symbol factory
 *
 * 提供创建各维度空线性表达式符号组合的工厂方法。
 *
 * Provides factory methods for creating empty linear expression symbol combinations of various dimensions.
 */
data object LinearIntermediateSymbols {
    operator fun <V> invoke(
        name: String,
        shape: Shape1,
        constants: RealNumberConstants<V>
    ): LinearExpressionSymbols1<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyLinearExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: Shape2,
        constants: RealNumberConstants<V>
    ): LinearExpressionSymbols2<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyLinearExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: Shape3,
        constants: RealNumberConstants<V>
    ): LinearExpressionSymbols3<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyLinearExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: Shape4,
        constants: RealNumberConstants<V>
    ): LinearExpressionSymbols4<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyLinearExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: DynShape,
        constants: RealNumberConstants<V>
    ): DynLinearExpressionSymbols<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyLinearExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }
}

/**
 * 二次中间符号工厂 / Quadratic intermediate symbol factory
 *
 * 提供创建各维度空二次表达式符号组合的工厂方法。
 *
 * Provides factory methods for creating empty quadratic expression symbol combinations of various dimensions.
 */
data object QuadraticIntermediateSymbols {
    operator fun <V> invoke(
        name: String,
        shape: Shape1,
        constants: RealNumberConstants<V>
    ): QuadraticExpressionSymbols1<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyQuadraticExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: Shape2,
        constants: RealNumberConstants<V>
    ): QuadraticExpressionSymbols2<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyQuadraticExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: Shape3,
        constants: RealNumberConstants<V>
    ): QuadraticExpressionSymbols3<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyQuadraticExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: Shape4,
        constants: RealNumberConstants<V>
    ): QuadraticExpressionSymbols4<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyQuadraticExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }

    operator fun <V> invoke(
        name: String,
        shape: DynShape,
        constants: RealNumberConstants<V>
    ): DynQuadraticExpressionSymbols<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            emptyQuadraticExpressionSymbol(symbolName(name, v), constants.zero)
        }
    }
}

fun <T, V> map(
    name: String,
    objs: Iterable<T>,
    ctor: (T) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T>) -> String = { (i, _) -> "$i" }
): LinearExpressionSymbols1<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l = objs.toList()
    return LinearExpressionSymbols1(
        name,
        Shape1(l.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l[v[0]]),
            name = "${name}_${suffix(Pair(v[0], l[v[0]]))}"
        )
    }
}

fun <T, V> flatMap(
    name: String,
    objs: Iterable<T>,
    ctor: (T) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T>) -> String = { (i, _) -> "$i" }
): LinearExpressionSymbols1<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l = objs.toList()
    return LinearExpressionSymbols1(
        name,
        Shape1(l.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l[v[0]]),
            name = "${name}_${suffix(Pair(v[0], l[v[0]]))}"
        )
    }
}

fun <T1, T2, V> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    ctor: (T1, T2) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>) -> String = { (i1, _), (i2, _) -> "${i1}_$i2" }
): LinearExpressionSymbols2<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    return LinearExpressionSymbols2(
        name,
        Shape2(l1.size, l2.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]]),
            name = "${name}_${suffix(Pair(v[0], l1[v[0]]), Pair(v[1], l2[v[1]]))}"
        )
    }
}

fun <T1, T2, V> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    ctor: (T1, T2) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>) -> String = { (i1, _), (i2, _) -> "${i1}_$i2" }
): LinearExpressionSymbols2<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    return LinearExpressionSymbols2(
        name,
        Shape2(l1.size, l2.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]]),
            name = "${name}_${suffix(Pair(v[0], l1[v[0]]), Pair(v[1], l2[v[1]]))}"
        )
    }
}

fun <T1, T2, T3, V> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    ctor: (T1, T2, T3) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>) -> String = { (i1, _), (i2, _), (i3, _) -> "${i1}_${i2}_$i3" }
): LinearExpressionSymbols3<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    return LinearExpressionSymbols3(
        name,
        Shape3(l1.size, l2.size, l3.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]], l3[v[2]]),
            name = "${name}_${
                suffix(
                    Pair(v[0], l1[v[0]]),
                    Pair(v[1], l2[v[1]]),
                    Pair(v[2], l3[v[2]])
                )
            }"
        )
    }
}

fun <T1, T2, T3, V> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    ctor: (T1, T2, T3) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>) -> String = { (i1, _), (i2, _), (i3, _) -> "${i1}_${i2}_$i3" }
): LinearExpressionSymbols3<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    return LinearExpressionSymbols3(
        name,
        Shape3(l1.size, l2.size, l3.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]], l3[v[2]]),
            name = "${name}_${
                suffix(
                    Pair(v[0], l1[v[0]]),
                    Pair(v[1], l2[v[1]]),
                    Pair(v[2], l3[v[2]])
                )
            }"
        )
    }
}

fun <T1, T2, T3, T4, V> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    objs4: Iterable<T4>,
    ctor: (T1, T2, T3, T4) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>, Pair<Int, T4>) -> String =
        { (i1, _), (i2, _), (i3, _), (i4, _) -> "${i1}_${i2}_${i3}_$i4" }
): LinearExpressionSymbols4<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    val l4 = objs4.toList()
    return LinearExpressionSymbols4(
        name,
        Shape4(l1.size, l2.size, l3.size, l4.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]], l3[v[2]], l4[v[3]]),
            name = "${name}_${
                suffix(
                    Pair(v[0], l1[v[0]]),
                    Pair(v[1], l2[v[1]]),
                    Pair(v[2], l3[v[2]]),
                    Pair(v[3], l4[v[3]])
                )
            }"
        )
    }
}

fun <T1, T2, T3, T4, V> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    objs4: Iterable<T4>,
    ctor: (T1, T2, T3, T4) -> LinearPolynomial<V>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>, Pair<Int, T4>) -> String =
        { (i1, _), (i2, _), (i3, _), (i4, _) -> "${i1}_${i2}_${i3}_$i4" }
): LinearExpressionSymbols4<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    val l4 = objs4.toList()
    return LinearExpressionSymbols4(
        name,
        Shape4(l1.size, l2.size, l3.size, l4.size)
    ) { _, v ->
        linearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]], l3[v[2]], l4[v[3]]),
            name = "${name}_${
                suffix(
                    Pair(v[0], l1[v[0]]),
                    Pair(v[1], l2[v[1]]),
                    Pair(v[2], l3[v[2]]),
                    Pair(v[3], l4[v[3]])
                )
            }"
        )
    }
}

@JvmName("mapDynSymbols")
fun <V> map(
    name: String,
    objs: Iterable<Iterable<Any>>,
    ctor: (List<Any>) -> LinearPolynomial<V>,
    suffix: (List<Pair<Int, Any>>) -> String = { ls -> ls.joinToString("_") { "${it.first}" } }
): DynLinearExpressionSymbols<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val ls = objs.map { it.toList() }
    return DynLinearExpressionSymbols(
        name,
        DynShape(ls.map { it.size }.toIntArray())
    ) { _, v ->
        linearExpressionSymbol(
            ctor(ls.mapIndexed { i, l -> l[v[i]] }),
            name = "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v[i], l[v[i]]) })}"
        )
    }
}

@JvmName("flatMapDynSymbols")
fun <V> flatMap(
    name: String,
    objs: List<Iterable<Any>>,
    ctor: (List<Any>) -> LinearPolynomial<V>,
    suffix: (List<Pair<Int, Any>>) -> String = { ls -> ls.joinToString("_") { "${it.first}" } }
): DynLinearExpressionSymbols<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val ls = objs.map { it.toList() }
    return DynLinearExpressionSymbols(
        name,
        DynShape(ls.map { it.size }.toIntArray())
    ) { _, v ->
        linearExpressionSymbol(
            ctor(ls.mapIndexed { i, l -> l[v[i]] }),
            name = "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v[i], l[v[i]]) })}"
        )
    }
}
