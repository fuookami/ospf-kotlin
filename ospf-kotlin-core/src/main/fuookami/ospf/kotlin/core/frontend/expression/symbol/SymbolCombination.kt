package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

class SymbolCombination<out Sym : IntermediateSymbol, S : Shape>(
    val name: String,
    shape: S,
    ctor: (Int, IntArray) -> Sym
) : MultiArray<Sym, S>(shape, ctor)

typealias IntermediateSymbols = MultiArray<IntermediateSymbol, *>
typealias IntermediateSymbols1 = MultiArray<IntermediateSymbol, Shape1>
typealias IntermediateSymbols2 = MultiArray<IntermediateSymbol, Shape2>
typealias IntermediateSymbols3 = MultiArray<IntermediateSymbol, Shape3>
typealias IntermediateSymbols4 = MultiArray<IntermediateSymbol, Shape4>
typealias DynIntermediateSymbols = MultiArray<IntermediateSymbol, DynShape>
typealias IntermediateSymbolView = MultiArrayView<IntermediateSymbol, *>
typealias IntermediateSymbolView1 = MultiArrayView<IntermediateSymbol, Shape1>
typealias IntermediateSymbolView2 = MultiArrayView<IntermediateSymbol, Shape2>
typealias IntermediateSymbolView3 = MultiArrayView<IntermediateSymbol, Shape3>
typealias IntermediateSymbolView4 = MultiArrayView<IntermediateSymbol, Shape4>
typealias DynIntermediateSymbolView = MultiArrayView<IntermediateSymbol, DynShape>

typealias LinearIntermediateSymbols1 = SymbolCombination<LinearIntermediateSymbol, Shape1>
typealias LinearIntermediateSymbols2 = SymbolCombination<LinearIntermediateSymbol, Shape2>
typealias LinearIntermediateSymbols3 = SymbolCombination<LinearIntermediateSymbol, Shape3>
typealias LinearIntermediateSymbols4 = SymbolCombination<LinearIntermediateSymbol, Shape4>
typealias DynLinearIntermediateSymbols = SymbolCombination<LinearIntermediateSymbol, DynShape>

typealias QuadraticIntermediateSymbols1 = SymbolCombination<QuadraticIntermediateSymbol, Shape1>
typealias QuadraticIntermediateSymbols2 = SymbolCombination<QuadraticIntermediateSymbol, Shape2>
typealias QuadraticIntermediateSymbols3 = SymbolCombination<QuadraticIntermediateSymbol, Shape3>
typealias QuadraticIntermediateSymbols4 = SymbolCombination<QuadraticIntermediateSymbol, Shape4>
typealias DynQuadraticIntermediateSymbols = SymbolCombination<QuadraticIntermediateSymbol, DynShape>

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

typealias LinearIntermediateSymbolView1 = MultiArrayView<LinearIntermediateSymbol, Shape1>
typealias LinearIntermediateSymbolView2 = MultiArrayView<LinearIntermediateSymbol, Shape2>
typealias LinearIntermediateSymbolView3 = MultiArrayView<LinearIntermediateSymbol, Shape3>
typealias LinearIntermediateSymbolView4 = MultiArrayView<LinearIntermediateSymbol, Shape4>
typealias DynLinearIntermediateSymbolView = MultiArrayView<LinearIntermediateSymbol, DynShape>

typealias QuadraticIntermediateSymbolView1 = MultiArrayView<QuadraticIntermediateSymbol, Shape1>
typealias QuadraticIntermediateSymbolView2 = MultiArrayView<QuadraticIntermediateSymbol, Shape2>
typealias QuadraticIntermediateSymbolView3 = MultiArrayView<QuadraticIntermediateSymbol, Shape3>
typealias QuadraticIntermediateSymbolView4 = MultiArrayView<QuadraticIntermediateSymbol, Shape4>
typealias DynQuadraticIntermediateSymbolView = MultiArrayView<QuadraticIntermediateSymbol, DynShape>

data object LinearIntermediateSymbols {
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

data object QuadraticIntermediateSymbols {
    operator fun invoke(
        name: String,
        shape: Shape1
    ): QuadraticExpressionSymbols1 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                QuadraticExpressionSymbol(
                    MutableQuadraticPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: Shape2
    ): QuadraticExpressionSymbols2 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                QuadraticExpressionSymbol(
                    MutableQuadraticPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: Shape3
    ): QuadraticExpressionSymbols3 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                QuadraticExpressionSymbol(
                    MutableQuadraticPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: Shape4
    ): QuadraticExpressionSymbols4 {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                QuadraticExpressionSymbol(
                    MutableQuadraticPolynomial(),
                    name = "${name}_${v.joinToString("_") { "$it" }}"
                )
            }
        )
    }

    operator fun invoke(
        name: String,
        shape: DynShape
    ): DynQuadraticExpressionSymbols {
        return SymbolCombination(
            name = name,
            shape = shape,
            ctor = { _, v ->
                QuadraticExpressionSymbol(
                    MutableQuadraticPolynomial(),
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
