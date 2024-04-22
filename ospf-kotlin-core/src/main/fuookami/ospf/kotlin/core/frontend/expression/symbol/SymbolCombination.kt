package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

class SymbolCombination<out Sym : Symbol, S : Shape>(
    val name: String,
    shape: S,
    ctor: (Int, IntArray) -> Sym
) : MultiArray<Sym, S>(shape, ctor)

typealias Symbols = MultiArray<Symbol, *>
typealias Symbols1 = MultiArray<Symbol, Shape1>
typealias Symbols2 = MultiArray<Symbol, Shape2>
typealias Symbols3 = MultiArray<Symbol, Shape3>
typealias Symbols4 = MultiArray<Symbol, Shape4>
typealias DynSymbols = MultiArray<Symbol, DynShape>
typealias SymbolView = MultiArrayView<Symbol, *>
typealias SymbolView1 = MultiArrayView<Symbol, Shape1>
typealias SymbolView2 = MultiArrayView<Symbol, Shape2>
typealias SymbolView3 = MultiArrayView<Symbol, Shape3>
typealias SymbolView4 = MultiArrayView<Symbol, Shape4>
typealias DynSymbolView = MultiArrayView<Symbol, DynShape>

typealias LinearSymbols1 = SymbolCombination<LinearSymbol, Shape1>
typealias LinearSymbols2 = SymbolCombination<LinearSymbol, Shape2>
typealias LinearSymbols3 = SymbolCombination<LinearSymbol, Shape3>
typealias LinearSymbols4 = SymbolCombination<LinearSymbol, Shape4>
typealias DynLinearSymbols = SymbolCombination<LinearSymbol, DynShape>

typealias QuadraticSymbols1 = SymbolCombination<QuadraticSymbol, Shape1>
typealias QuadraticSymbols2 = SymbolCombination<QuadraticSymbol, Shape2>
typealias QuadraticSymbols3 = SymbolCombination<QuadraticSymbol, Shape3>
typealias QuadraticSymbols4 = SymbolCombination<QuadraticSymbol, Shape4>
typealias DynQuadraticSymbols = SymbolCombination<QuadraticSymbol, DynShape>

typealias LinearExpressionSymbols1 = SymbolCombination<LinearExpressionSymbol, Shape1>
typealias LinearExpressionSymbols2 = SymbolCombination<LinearExpressionSymbol, Shape2>
typealias LinearExpressionSymbols3 = SymbolCombination<LinearExpressionSymbol, Shape3>
typealias LinearExpressionSymbols4 = SymbolCombination<LinearExpressionSymbol, Shape4>
typealias DynLinearExpressionSymbols = SymbolCombination<LinearExpressionSymbol, DynShape>

typealias QuadraticExpressionSymbols1 = SymbolCombination<QuadraticExpressionSymbol, Shape1>
typealias QuadraticExpressionSymbols2 = SymbolCombination<QuadraticExpressionSymbol, Shape2>
typealias QuadraticExpressionSymbols3 = SymbolCombination<QuadraticExpressionSymbol, Shape3>
typealias QuadraticExpressionSymbols4 = SymbolCombination<QuadraticExpressionSymbol, Shape4>
typealias DynQuadraticExpressionSymbols = SymbolCombination<QuadraticExpressionSymbol, DynShape>

typealias LinearFunctionSymbols1 = SymbolCombination<LinearFunctionSymbol, Shape1>
typealias LinearFunctionSymbols2 = SymbolCombination<LinearFunctionSymbol, Shape2>
typealias LinearFunctionSymbols3 = SymbolCombination<LinearFunctionSymbol, Shape3>
typealias LinearFunctionSymbols4 = SymbolCombination<LinearFunctionSymbol, Shape4>
typealias DynLinearFunctionSymbols = SymbolCombination<LinearFunctionSymbol, DynShape>

typealias QuadraticFunctionSymbols1 = SymbolCombination<QuadraticFunctionSymbol, Shape1>
typealias QuadraticFunctionSymbols2 = SymbolCombination<QuadraticFunctionSymbol, Shape2>
typealias QuadraticFunctionSymbols3 = SymbolCombination<QuadraticFunctionSymbol, Shape3>
typealias QuadraticFunctionSymbols4 = SymbolCombination<QuadraticFunctionSymbol, Shape4>
typealias DynQuadraticFunctionSymbols = SymbolCombination<QuadraticFunctionSymbol, DynShape>

typealias LinearSymbolView1 = MultiArrayView<LinearSymbol, Shape1>
typealias LinearSymbolView2 = MultiArrayView<LinearSymbol, Shape2>
typealias LinearSymbolView3 = MultiArrayView<LinearSymbol, Shape3>
typealias LinearSymbolView4 = MultiArrayView<LinearSymbol, Shape4>
typealias DynLinearSymbolView = MultiArrayView<LinearSymbol, DynShape>

typealias QuadraticSymbolView1 = MultiArrayView<QuadraticSymbol, Shape1>
typealias QuadraticSymbolView2 = MultiArrayView<QuadraticSymbol, Shape2>
typealias QuadraticSymbolView3 = MultiArrayView<QuadraticSymbol, Shape3>
typealias QuadraticSymbolView4 = MultiArrayView<QuadraticSymbol, Shape4>
typealias DynQuadraticSymbolView = MultiArrayView<QuadraticSymbol, DynShape>

data object LinearSymbols {
    operator fun invoke(
        name: String,
        shape: Shape1
    ): LinearExpressionSymbols1 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: Shape2
    ): LinearExpressionSymbols2 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: Shape3
    ): LinearExpressionSymbols3 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: Shape4
    ): LinearExpressionSymbols4 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: DynShape
    ): DynLinearExpressionSymbols {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }
}

fun <T> map(
    name: String,
    objs: Iterable<T>,
    ctor: (T) -> LinearMonomial,
    suffix: (Pair<Int, T>) -> String = { (i, _) -> "$i" }
): LinearExpressionSymbols1 {
    val l = objs.toList()
    return LinearExpressionSymbols1(
        name,
        Shape1(l.size)
    ) { _, v ->
        LinearExpressionSymbol(
            ctor(l[v[0]]),
            name = "${name}_${suffix(Pair(v[0], l[v[0]]))}"
        )
    }
}

fun <T> flatMap(
    name: String,
    objs: Iterable<T>,
    ctor: (T) -> AbstractLinearPolynomial<*>,
    suffix: (Pair<Int, T>) -> String = { (i, _) -> "$i" }
): LinearExpressionSymbols1 {
    val l = objs.toList()
    return LinearExpressionSymbols1(
        name,
        Shape1(l.size)
    ) { _, v ->
        LinearExpressionSymbol(
            ctor(l[v[0]]),
            name = "${name}_${suffix(Pair(v[0], l[v[0]]))}"
        )
    }
}

fun <T1, T2> map(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    ctor: (T1, T2) -> LinearMonomial,
    suffix: (Pair<Int, T1>, Pair<Int, T2>) -> String = { (i1, _), (i2, _) -> "${i1}_$i2" }
): LinearExpressionSymbols2 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    return LinearExpressionSymbols2(
        name,
        Shape2(l1.size, l2.size)
    ) { _, v ->
        LinearExpressionSymbol(
            ctor(l1[v[0]], l2[v[1]]),
            name = "${name}_${suffix(Pair(v[0], l1[v[0]]), Pair(v[1], l2[v[1]]))}"
        )
    }
}

fun <T1, T2> flatMap(
    name: String,
    objs1: Iterable<T1>,
    objs2: Iterable<T2>,
    ctor: (T1, T2) -> AbstractLinearPolynomial<*>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>) -> String = { (i1, _), (i2, _) -> "${i1}_$i2" }
): LinearExpressionSymbols2 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    return LinearExpressionSymbols2(
        name,
        Shape2(l1.size, l2.size)
    ) { _, v ->
        LinearExpressionSymbol(
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
    ctor: (T1, T2, T3) -> LinearMonomial,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>) -> String = { (i1, _), (i2, _), (i3, _) -> "${i1}_${i2}_$i3" }
): LinearExpressionSymbols3 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    return LinearExpressionSymbols3(
        name,
        Shape3(l1.size, l2.size, l3.size)
    ) { _, v ->
        LinearExpressionSymbol(
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
    ctor: (T1, T2, T3) -> AbstractLinearPolynomial<*>,
    suffix: (Pair<Int, T1>, Pair<Int, T2>, Pair<Int, T3>) -> String = { (i1, _), (i2, _), (i3, _) -> "${i1}_${i2}_$i3" }
): LinearExpressionSymbols3 {
    val l1 = objs1.toList()
    val l2 = objs2.toList()
    val l3 = objs3.toList()
    return LinearExpressionSymbols3(
        name,
        Shape3(l1.size, l2.size, l3.size)
    ) { _, v ->
        LinearExpressionSymbol(
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
    ctor: (T1, T2, T3, T4) -> LinearMonomial,
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
        LinearExpressionSymbol(
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
    ctor: (T1, T2, T3, T4) -> AbstractLinearPolynomial<*>,
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
        LinearExpressionSymbol(
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
    ctor: (List<Any>) -> AbstractLinearPolynomial<*>,
    suffix: (List<Pair<Int, Any>>) -> String = { ls -> ls.joinToString("_") { "${it.first}" } }
): DynLinearExpressionSymbols {
    val ls = objs.map { it.toList() }
    return DynLinearExpressionSymbols(
        name,
        DynShape(ls.map { it.size }.toIntArray())
    ) { _, v ->
        LinearExpressionSymbol(
            ctor(ls.mapIndexed { i, l -> l[v[i]] }),
            name = "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v[i], l[v[i]]) })}"
        )
    }
}

@JvmName("flatMapDynSymbols")
fun flatMap(
    name: String,
    objs: List<Iterable<Any>>,
    ctor: (List<Any>) -> AbstractLinearPolynomial<*>,
    suffix: (List<Pair<Int, Any>>) -> String = { ls -> ls.joinToString("_") { "${it.first}" } }
): DynLinearExpressionSymbols {
    val ls = objs.map { it.toList() }
    return DynLinearExpressionSymbols(
        name,
        DynShape(ls.map { it.size }.toIntArray())
    ) { _, v ->
        LinearExpressionSymbol(
            ctor(ls.mapIndexed { i, l -> l[v[i]] }),
            name = "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v[i], l[v[i]]) })}"
        )
    }
}
