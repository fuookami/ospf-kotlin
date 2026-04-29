@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.intermediate_symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

interface AbstractSymbolCombination<S : Shape> {
    val dimension: Int
    val identifier: UInt64
    val shape: Shape
}

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

typealias LinearExpressionSymbols1 = SymbolCombination<LinearExpressionSymbolF64, Shape1>
typealias LinearExpressionSymbols2 = SymbolCombination<LinearExpressionSymbolF64, Shape2>
typealias LinearExpressionSymbols3 = SymbolCombination<LinearExpressionSymbolF64, Shape3>
typealias LinearExpressionSymbols4 = SymbolCombination<LinearExpressionSymbolF64, Shape4>
typealias DynLinearExpressionSymbols = SymbolCombination<LinearExpressionSymbolF64, DynShape>

typealias QuantityLinearExpressionSymbols1 = QuantitySymbolCombination<LinearExpressionSymbolF64, Shape1>
typealias QuantityLinearExpressionSymbols2 = QuantitySymbolCombination<LinearExpressionSymbolF64, Shape2>
typealias QuantityLinearExpressionSymbols3 = QuantitySymbolCombination<LinearExpressionSymbolF64, Shape3>
typealias QuantityLinearExpressionSymbols4 = QuantitySymbolCombination<LinearExpressionSymbolF64, Shape4>
typealias DynQuantityLinearExpressionSymbols = QuantitySymbolCombination<LinearExpressionSymbolF64, DynShape>

typealias QuadraticExpressionSymbols1 = SymbolCombination<QuadraticExpressionSymbolF64, Shape1>
typealias QuadraticExpressionSymbols2 = SymbolCombination<QuadraticExpressionSymbolF64, Shape2>
typealias QuadraticExpressionSymbols3 = SymbolCombination<QuadraticExpressionSymbolF64, Shape3>
typealias QuadraticExpressionSymbols4 = SymbolCombination<QuadraticExpressionSymbolF64, Shape4>
typealias DynQuadraticExpressionSymbols = SymbolCombination<QuadraticExpressionSymbolF64, DynShape>

typealias QuantityQuadraticExpressionSymbols1 = QuantitySymbolCombination<QuadraticExpressionSymbolF64, Shape1>
typealias QuantityQuadraticExpressionSymbols2 = QuantitySymbolCombination<QuadraticExpressionSymbolF64, Shape2>
typealias QuantityQuadraticExpressionSymbols3 = QuantitySymbolCombination<QuadraticExpressionSymbolF64, Shape3>
typealias QuantityQuadraticExpressionSymbols4 = QuantitySymbolCombination<QuadraticExpressionSymbolF64, Shape4>
typealias DynQuantityQuadraticExpressionSymbols = QuantitySymbolCombination<QuadraticExpressionSymbolF64, DynShape>

data object LinearIntermediateSymbols {
    operator fun invoke(
        name: String,
        shape: Shape1
    ): LinearExpressionSymbols1 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            LinearExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: Shape2
    ): LinearExpressionSymbols2 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            LinearExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: Shape3
    ): LinearExpressionSymbols3 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            LinearExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: Shape4
    ): LinearExpressionSymbols4 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            LinearExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: DynShape
    ): DynLinearExpressionSymbols {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            LinearExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }
}

data object QuadraticIntermediateSymbols {
    operator fun invoke(
        name: String,
        shape: Shape1
    ): QuadraticExpressionSymbols1 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            QuadraticExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: Shape2
    ): QuadraticExpressionSymbols2 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            QuadraticExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: Shape3
    ): QuadraticExpressionSymbols3 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            QuadraticExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: Shape4
    ): QuadraticExpressionSymbols4 {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            QuadraticExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }

    operator fun invoke(
        name: String,
        shape: DynShape
    ): DynQuadraticExpressionSymbols {
        return SymbolCombination(
            name = name,
            shape = shape
        ) { _, v ->
            QuadraticExpressionSymbolF64(
                name = "${name}_${v.joinToString("_") { "$it" }}"
            )
        }
    }
}

fun <T> map(
    name: String,
    objs: Iterable<T>,
    ctor: (T) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T>) -> String = { (i, _) -> "$i" }
): LinearExpressionSymbols1 {
    val l = objs.toList()
    return LinearExpressionSymbols1(
        name,
        Shape1(l.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(l[v[0]]),
            name = "${name}_${suffix(Pair(v[0], l[v[0]]))}"
        )
    }
}

fun <T> flatMap(
    name: String,
    objs: Iterable<T>,
    ctor: (T) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T>) -> String = { (i, _) -> "$i" }
): LinearExpressionSymbols1 {
    val l = objs.toList()
    return LinearExpressionSymbols1(
        name,
        Shape1(l.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(l[v[0]]),
            name = "${name}_${suffix(Pair(v[0], l[v[0]]))}"
        )
    }
}

fun <T1, T2> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    ctor: (T1, T2) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>) -> String = { (i1, _), (i2, _) -> "${i1}_$i2" }
): LinearExpressionSymbols2 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    return LinearExpressionSymbols2(
        name,
        Shape2(l1.size, l2.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(l1[v[0]], l2[v[1]]),
            name = "${name}_${suffix(Pair(v[0], l1[v[0]]), Pair(v[1], l2[v[1]]))}"
        )
    }
}

fun <T1, T2> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    ctor: (T1, T2) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>) -> String = { (i1, _), (i2, _) -> "${i1}_$i2" }
): LinearExpressionSymbols2 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    return LinearExpressionSymbols2(
        name,
        Shape2(l1.size, l2.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(l1[v[0]], l2[v[1]]),
            name = "${name}_${suffix(Pair(v[0], l1[v[0]]), Pair(v[1], l2[v[1]]))}"
        )
    }
}

fun <T1, T2, T3> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    ctor: (T1, T2, T3) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>) -> String = { (i1, _), (i2, _), (i3, _) -> "${i1}_${i2}_$i3" }
): LinearExpressionSymbols3 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    return LinearExpressionSymbols3(
        name,
        Shape3(l1.size, l2.size, l3.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
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

fun <T1, T2, T3> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    ctor: (T1, T2, T3) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>) -> String = { (i1, _), (i2, _), (i3, _) -> "${i1}_${i2}_$i3" }
): LinearExpressionSymbols3 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    return LinearExpressionSymbols3(
        name,
        Shape3(l1.size, l2.size, l3.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
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

fun <T1, T2, T3, T4> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    objs4: Iterable<T4>,
    ctor: (T1, T2, T3, T4) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>, Pair<Int, T4>) -> String =
        { (i1, _), (i2, _), (i3, _), (i4, _) -> "${i1}_${i2}_${i3}_$i4" }
): LinearExpressionSymbols4 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    val l4 = objs4.toList()
    return LinearExpressionSymbols4(
        name,
        Shape4(l1.size, l2.size, l3.size, l4.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(l1[v[0]], l2[v[1]], l3[v[2]], l4[v[4]]),
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

fun <T1, T2, T3, T4> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    objs3: Iterable<T3>,
    objs4: Iterable<T4>,
    ctor: (T1, T2, T3, T4) -> UtilsLinearPolynomial<Flt64>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>, Pair<Int, T4>) -> String =
        { (i1, _), (i2, _), (i3, _), (i4, _) -> "${i1}_${i2}_${i3}_$i4" }
): LinearExpressionSymbols4 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    val l4 = objs4.toList()
    return LinearExpressionSymbols4(
        name,
        Shape4(l1.size, l2.size, l3.size, l4.size)
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(l1[v[0]], l2[v[1]], l3[v[2]], l4[v[4]]),
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
fun map(
    name: String,
    objs: Iterable<Iterable<Any>>,
    ctor: (List<Any>) -> UtilsLinearPolynomial<Flt64>,
    suffix: (List<Pair<Int, Any>>) -> String = { ls -> ls.joinToString("_") { "${it.first}" } }
): DynLinearExpressionSymbols {
    val ls = objs.map { it.toList() }
    return DynLinearExpressionSymbols(
        name,
        DynShape(ls.map { it.size }.toIntArray())
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(ls.mapIndexed { i, l -> l[v[i]] }),
            name = "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v[i], l[v[i]]) })}"
        )
    }
}

@JvmName("flatMapDynSymbols")
fun flatMap(
    name: String,
    objs: List<Iterable<Any>>,
    ctor: (List<Any>) -> UtilsLinearPolynomial<Flt64>,
    suffix: (List<Pair<Int, Any>>) -> String = { ls -> ls.joinToString("_") { "${it.first}" } }
): DynLinearExpressionSymbols {
    val ls = objs.map { it.toList() }
    return DynLinearExpressionSymbols(
        name,
        DynShape(ls.map { it.size }.toIntArray())
    ) { _, v ->
        LinearExpressionSymbolF64(
            ctor(ls.mapIndexed { i, l -> l[v[i]] }),
            name = "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v[i], l[v[i]]) })}"
        )
    }
}