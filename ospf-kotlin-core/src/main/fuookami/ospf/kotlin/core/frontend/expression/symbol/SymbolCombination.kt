package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

class SymbolCombination<out Sym : Symbol<Cell, C>, Cell : MonomialCell<Cell, C>, C : Category, S : Shape>(
    val name: String,
    shape: S,
    ctor: (Pair<Int, IntArray>) -> Sym
) : MultiArray<Sym, S>(shape, ctor)

typealias Symbols<Cell, C> = MultiArray<Symbol<Cell, C>, *>
typealias Symbols1<Cell, C> = MultiArray<Symbol<Cell, C>, Shape1>
typealias Symbols2<Cell, C> = MultiArray<Symbol<Cell, C>, Shape2>
typealias Symbols3<Cell, C> = MultiArray<Symbol<Cell, C>, Shape3>
typealias Symbols4<Cell, C> = MultiArray<Symbol<Cell, C>, Shape4>
typealias DynSymbols<Cell, C> = MultiArray<Symbol<Cell, C>, DynShape>
typealias SymbolView<Cell, C> = MultiArrayView<Symbol<Cell, C>, *>
typealias SymbolView1<Cell, C> = MultiArrayView<Symbol<Cell, C>, Shape1>
typealias SymbolView2<Cell, C> = MultiArrayView<Symbol<Cell, C>, Shape2>
typealias SymbolView3<Cell, C> = MultiArrayView<Symbol<Cell, C>, Shape3>
typealias SymbolView4<Cell, C> = MultiArrayView<Symbol<Cell, C>, Shape4>
typealias DynSymbolView<Cell, C> = MultiArrayView<Symbol<Cell, C>, DynShape>

typealias LinearSymbols1 = SymbolCombination<LinearSymbol, LinearMonomialCell, Linear, Shape1>
typealias LinearSymbols2 = SymbolCombination<LinearSymbol, LinearMonomialCell, Linear, Shape2>
typealias LinearSymbols3 = SymbolCombination<LinearSymbol, LinearMonomialCell, Linear, Shape3>
typealias LinearSymbols4 = SymbolCombination<LinearSymbol, LinearMonomialCell, Linear, Shape4>
typealias DynLinearSymbols = SymbolCombination<LinearSymbol, LinearMonomialCell, Linear, DynShape>

typealias LinearExpressionSymbols1 = SymbolCombination<LinearExpressionSymbol, LinearMonomialCell, Linear, Shape1>
typealias LinearExpressionSymbols2 = SymbolCombination<LinearExpressionSymbol, LinearMonomialCell, Linear, Shape2>
typealias LinearExpressionSymbols3 = SymbolCombination<LinearExpressionSymbol, LinearMonomialCell, Linear, Shape3>
typealias LinearExpressionSymbols4 = SymbolCombination<LinearExpressionSymbol, LinearMonomialCell, Linear, Shape4>
typealias DynLinearExpressionSymbols = SymbolCombination<LinearExpressionSymbol, LinearMonomialCell, Linear, DynShape>

typealias LinearFunctionSymbols1 = SymbolCombination<LinearFunctionSymbol, LinearMonomialCell, Linear, Shape1>
typealias LinearFunctionSymbols2 = SymbolCombination<LinearFunctionSymbol, LinearMonomialCell, Linear, Shape2>
typealias LinearFunctionSymbols3 = SymbolCombination<LinearFunctionSymbol, LinearMonomialCell, Linear, Shape3>
typealias LinearFunctionSymbols4 = SymbolCombination<LinearFunctionSymbol, LinearMonomialCell, Linear, Shape4>
typealias DynLinearFunctionSymbols = SymbolCombination<LinearFunctionSymbol, LinearMonomialCell, Linear, DynShape>

typealias LinearSymbolView1 = MultiArrayView<LinearSymbol, Shape1>
typealias LinearSymbolView2 = MultiArrayView<LinearSymbol, Shape2>
typealias LinearSymbolView3 = MultiArrayView<LinearSymbol, Shape3>
typealias LinearSymbolView4 = MultiArrayView<LinearSymbol, Shape4>
typealias DynLinearSymbolView = MultiArrayView<LinearSymbol, DynShape>

object LinearSymbols {
    operator fun invoke(
        name: String,
        shape: Shape1
    ): LinearExpressionSymbols1 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { (_, v) ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    "${name}_${v.joinToString("_") { "$it" }}"
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
            ctor = { (_, v) ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    "${name}_${v.joinToString("_") { "$it" }}"
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
            ctor = { (_, v) ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    "${name}_${v.joinToString("_") { "$it" }}"
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
            ctor = { (_, v) ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    "${name}_${v.joinToString("_") { "$it" }}"
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
            ctor = { (_, v) ->
                LinearExpressionSymbol(
                    MutableLinearPolynomial(),
                    "${name}_${v.joinToString("_") { "$it" }}"
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l[v.second[0]]),
            "${name}_${suffix(Pair(v.second[0], l[v.second[0]]))}"
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l[v.second[0]]),
            "${name}_${suffix(Pair(v.second[0], l[v.second[0]]))}"
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l1[v.second[0]], l2[v.second[1]]),
            "${name}_${suffix(Pair(v.second[0], l1[v.second[0]]), Pair(v.second[1], l2[v.second[1]]))}"
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l1[v.second[0]], l2[v.second[1]]),
            "${name}_${suffix(Pair(v.second[0], l1[v.second[0]]), Pair(v.second[1], l2[v.second[1]]))}"
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l1[v.second[0]], l2[v.second[1]], l3[v.second[2]]),
            "${name}_${
                suffix(
                    Pair(v.second[0], l1[v.second[0]]),
                    Pair(v.second[1], l2[v.second[1]]),
                    Pair(v.second[2], l3[v.second[2]])
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l1[v.second[0]], l2[v.second[1]], l3[v.second[2]]),
            "${name}_${
                suffix(
                    Pair(v.second[0], l1[v.second[0]]),
                    Pair(v.second[1], l2[v.second[1]]),
                    Pair(v.second[2], l3[v.second[2]])
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l1[v.second[0]], l2[v.second[1]], l3[v.second[2]], l4[v.second[4]]),
            "${name}_${
                suffix(
                    Pair(v.second[0], l1[v.second[0]]),
                    Pair(v.second[1], l2[v.second[1]]),
                    Pair(v.second[2], l3[v.second[2]]),
                    Pair(v.second[3], l4[v.second[3]])
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(l1[v.second[0]], l2[v.second[1]], l3[v.second[2]], l4[v.second[4]]),
            "${name}_${
                suffix(
                    Pair(v.second[0], l1[v.second[0]]),
                    Pair(v.second[1], l2[v.second[1]]),
                    Pair(v.second[2], l3[v.second[2]]),
                    Pair(v.second[3], l4[v.second[3]])
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(ls.mapIndexed { i, l -> l[v.second[i]] }),
            "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v.second[i], l[v.second[i]]) })}"
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
    ) { v ->
        LinearExpressionSymbol(
            ctor(ls.mapIndexed { i, l -> l[v.second[i]] }),
            "${name}_${suffix(ls.mapIndexed { i, l -> Pair(v.second[i], l[v.second[i]]) })}"
        )
    }
}
