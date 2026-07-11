@file:Suppress("unused")

/**
 * 符号组合 / Symbol combination
 *
 * 提供基于多维数组的中间符号组合容器及工厂方法。
 * Provides multi-array-based intermediate symbol combination containers and factory methods.
*/
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

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

/**
 * 符号组合抽象接口 / Abstract symbol combination interface
 *
 * @property dimension 维度数 / Number of dimensions
 * @property identifier 全局唯一标识符 / Globally unique identifier
 * @property shape 数组形状 / Array shape
*/
interface AbstractSymbolCombination<S : Shape> {

    /** Number of dimensions in the combination / 组合中的维度数 */
    val dimension: Int

    /** Globally unique identifier / 全局唯一标识符 */
    val identifier: UInt64

    /** Array shape defining the structure / 定义结构的数组形状 */
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
 * @param shape 数组形状 / Array shape
 * @param ctor 符号构造函数 / Symbol constructor function
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
 * @param shape 数组形状 / Array shape
 * @param ctor 量纲符号构造函数 / Quantity symbol constructor function
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

/** 一维线性表达式符号组合 / 1D linear expression symbol combination */
typealias LinearExpressionSymbols1<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape1>

/** 二维线性表达式符号组合 / 2D linear expression symbol combination */
typealias LinearExpressionSymbols2<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape2>

/** 三维线性表达式符号组合 / 3D linear expression symbol combination */
typealias LinearExpressionSymbols3<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape3>

/** 四维线性表达式符号组合 / 4D linear expression symbol combination */
typealias LinearExpressionSymbols4<V> = SymbolCombination<LinearExpressionSymbol<V>, Shape4>

/** 动态维度线性表达式符号组合 / Dynamic-dimension linear expression symbol combination */
typealias DynLinearExpressionSymbols<V> = SymbolCombination<LinearExpressionSymbol<V>, DynShape>

/** 一维量纲线性表达式符号组合 / 1D quantity linear expression symbol combination */
typealias QuantityLinearExpressionSymbols1<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape1>

/** 二维量纲线性表达式符号组合 / 2D quantity linear expression symbol combination */
typealias QuantityLinearExpressionSymbols2<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape2>

/** 三维量纲线性表达式符号组合 / 3D quantity linear expression symbol combination */
typealias QuantityLinearExpressionSymbols3<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape3>

/** 四维量纲线性表达式符号组合 / 4D quantity linear expression symbol combination */
typealias QuantityLinearExpressionSymbols4<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, Shape4>

/** 动态维度量纲线性表达式符号组合 / Dynamic-dimension quantity linear expression symbol combination */
typealias DynQuantityLinearExpressionSymbols<V> = QuantitySymbolCombination<LinearExpressionSymbol<V>, DynShape>

/** 一维线性中间符号组合 / 1D linear intermediate symbol combination */
typealias LinearIntermediateSymbols1<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape1>

/** 二维线性中间符号组合 / 2D linear intermediate symbol combination */
typealias LinearIntermediateSymbols2<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape2>

/** 三维线性中间符号组合 / 3D linear intermediate symbol combination */
typealias LinearIntermediateSymbols3<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape3>

/** 四维线性中间符号组合 / 4D linear intermediate symbol combination */
typealias LinearIntermediateSymbols4<V> = SymbolCombination<LinearIntermediateSymbol<V>, Shape4>

/** 动态维度线性中间符号组合 / Dynamic-dimension linear intermediate symbol combination */
typealias DynLinearIntermediateSymbols<V> = SymbolCombination<LinearIntermediateSymbol<V>, DynShape>

/** 一维量纲线性中间符号组合 / 1D quantity linear intermediate symbol combination */
typealias QuantityLinearIntermediateSymbols1<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape1>

/** 二维量纲线性中间符号组合 / 2D quantity linear intermediate symbol combination */
typealias QuantityLinearIntermediateSymbols2<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape2>

/** 三维量纲线性中间符号组合 / 3D quantity linear intermediate symbol combination */
typealias QuantityLinearIntermediateSymbols3<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape3>

/** 四维量纲线性中间符号组合 / 4D quantity linear intermediate symbol combination */
typealias QuantityLinearIntermediateSymbols4<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, Shape4>

/** 动态维度量纲线性中间符号组合 / Dynamic-dimension quantity linear intermediate symbol combination */
typealias DynQuantityLinearIntermediateSymbols<V> = QuantitySymbolCombination<LinearIntermediateSymbol<V>, DynShape>

/** 量纲线性中间符号 / Quantity linear intermediate symbol */
typealias QuantityLinearIntermediateSymbol<V> = Quantity<LinearIntermediateSymbol<V>>

/** 一维二次表达式符号组合 / 1D quadratic expression symbol combination */
typealias QuadraticExpressionSymbols1<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape1>

/** 二维二次表达式符号组合 / 2D quadratic expression symbol combination */
typealias QuadraticExpressionSymbols2<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape2>

/** 三维二次表达式符号组合 / 3D quadratic expression symbol combination */
typealias QuadraticExpressionSymbols3<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape3>

/** 四维二次表达式符号组合 / 4D quadratic expression symbol combination */
typealias QuadraticExpressionSymbols4<V> = SymbolCombination<QuadraticExpressionSymbol<V>, Shape4>

/** 动态维度二次表达式符号组合 / Dynamic-dimension quadratic expression symbol combination */
typealias DynQuadraticExpressionSymbols<V> = SymbolCombination<QuadraticExpressionSymbol<V>, DynShape>

/** 一维量纲二次表达式符号组合 / 1D quantity quadratic expression symbol combination */
typealias QuantityQuadraticExpressionSymbols1<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape1>

/** 二维量纲二次表达式符号组合 / 2D quantity quadratic expression symbol combination */
typealias QuantityQuadraticExpressionSymbols2<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape2>

/** 三维量纲二次表达式符号组合 / 3D quantity quadratic expression symbol combination */
typealias QuantityQuadraticExpressionSymbols3<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape3>

/** 四维量纲二次表达式符号组合 / 4D quantity quadratic expression symbol combination */
typealias QuantityQuadraticExpressionSymbols4<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, Shape4>

/** 动态维度量纲二次表达式符号组合 / Dynamic-dimension quantity quadratic expression symbol combination */
typealias DynQuantityQuadraticExpressionSymbols<V> = QuantitySymbolCombination<QuadraticExpressionSymbol<V>, DynShape>

/**
 * 根据名称和索引数组生成符号名称。
 * Generate symbol name from name and indices array.
 *
 * @param name 基础名称 / Base name
 * @param indices 索引数组 / Indices array
 * @return 生成的符号名称 / Generated symbol name
*/
private fun symbolName(name: String, indices: IntArray): String {
    return "${name}_${indices.joinToString("_") { "$it" }}"
}

/**
 * 创建空的线性表达式符号。
 * Create an empty linear expression symbol.
 *
 * @param name 符号名称 / Symbol name
 * @param zero 零值 / Zero value
 * @return 空的线性表达式符号 / Empty linear expression symbol
*/
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

/**
 * 创建空的二次表达式符号。
 * Create an empty quadratic expression symbol.
 *
 * @param name 符号名称 / Symbol name
 * @param zero 零值 / Zero value
 * @return 空的二次表达式符号 / Empty quadratic expression symbol
*/
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

/**
 * 从线性多项式创建线性表达式符号。
 * Create linear expression symbol from linear polynomial.
 *
 * @param polynomial 线性多项式 / Linear polynomial
 * @param name 符号名称 / Symbol name
 * @return 线性表达式符号 / Linear expression symbol
*/
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

    /**
     * 创建一维线性表达式符号组合。
     * Create 1D linear expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 一维形状 / 1D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 一维线性表达式符号组合 / 1D linear expression symbol combination
    */
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

    /**
     * 创建二维线性表达式符号组合。
     * Create 2D linear expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 二维形状 / 2D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 二维线性表达式符号组合 / 2D linear expression symbol combination
    */
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

    /**
     * 创建三维线性表达式符号组合。
     * Create 3D linear expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 三维形状 / 3D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 三维线性表达式符号组合 / 3D linear expression symbol combination
    */
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

    /**
     * 创建四维线性表达式符号组合。
     * Create 4D linear expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 四维形状 / 4D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 四维线性表达式符号组合 / 4D linear expression symbol combination
    */
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

    /**
     * 创建动态维度线性表达式符号组合。
     * Create dynamic-dimension linear expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 动态形状 / Dynamic shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 动态维度线性表达式符号组合 / Dynamic-dimension linear expression symbol combination
    */
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

    /**
     * 创建一维二次表达式符号组合。
     * Create 1D quadratic expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 一维形状 / 1D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 一维二次表达式符号组合 / 1D quadratic expression symbol combination
    */
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

    /**
     * 创建二维二次表达式符号组合。
     * Create 2D quadratic expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 二维形状 / 2D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 二维二次表达式符号组合 / 2D quadratic expression symbol combination
    */
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

    /**
     * 创建三维二次表达式符号组合。
     * Create 3D quadratic expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 三维形状 / 3D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 三维二次表达式符号组合 / 3D quadratic expression symbol combination
    */
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

    /**
     * 创建四维二次表达式符号组合。
     * Create 4D quadratic expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 四维形状 / 4D shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 四维二次表达式符号组合 / 4D quadratic expression symbol combination
    */
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

    /**
     * 创建动态维度二次表达式符号组合。
     * Create dynamic-dimension quadratic expression symbol combination.
     *
     * @param name 组合名称 / Combination name
     * @param shape 动态形状 / Dynamic shape
     * @param constants 实数常量定义 / Real number constants definition
     * @return 动态维度二次表达式符号组合 / Dynamic-dimension quadratic expression symbol combination
    */
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

/**
 * 从一维可迭代对象映射创建线性表达式符号组合。
 * Create linear expression symbol combination by mapping 1D iterable.
 *
 * @param name 组合名称 / Combination name
 * @param objs 可迭代对象 / Iterable objects
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 一维线性表达式符号组合 / 1D linear expression symbol combination
*/
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

/**
 * 从一维可迭代对象扁平映射创建线性表达式符号组合。
 * Create linear expression symbol combination by flat-mapping 1D iterable.
 *
 * @param name 组合名称 / Combination name
 * @param objs 可迭代对象 / Iterable objects
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 一维线性表达式符号组合 / 1D linear expression symbol combination
*/
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

/**
 * 从二维可迭代对象映射创建线性表达式符号组合。
 * Create linear expression symbol combination by mapping 2D iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs1 第一维可迭代对象 / First dimension iterable
 * @param objs2 第二维可迭代对象 / Second dimension iterable
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 二维线性表达式符号组合 / 2D linear expression symbol combination
*/
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

/**
 * 从二维可迭代对象扁平映射创建线性表达式符号组合。
 * Create linear expression symbol combination by flat-mapping 2D iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs1 第一维可迭代对象 / First dimension iterable
 * @param objs2 第二维可迭代对象 / Second dimension iterable
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 二维线性表达式符号组合 / 2D linear expression symbol combination
*/
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

/**
 * 从三维可迭代对象映射创建线性表达式符号组合。
 * Create linear expression symbol combination by mapping 3D iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs1 第一维可迭代对象 / First dimension iterable
 * @param objs2 第二维可迭代对象 / Second dimension iterable
 * @param objs3 第三维可迭代对象 / Third dimension iterable
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 三维线性表达式符号组合 / 3D linear expression symbol combination
*/
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

/**
 * 从三维可迭代对象扁平映射创建线性表达式符号组合。
 * Create linear expression symbol combination by flat-mapping 3D iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs1 第一维可迭代对象 / First dimension iterable
 * @param objs2 第二维可迭代对象 / Second dimension iterable
 * @param objs3 第三维可迭代对象 / Third dimension iterable
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 三维线性表达式符号组合 / 3D linear expression symbol combination
*/
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

/**
 * 从四维可迭代对象映射创建线性表达式符号组合。
 * Create linear expression symbol combination by mapping 4D iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs1 第一维可迭代对象 / First dimension iterable
 * @param objs2 第二维可迭代对象 / Second dimension iterable
 * @param objs3 第三维可迭代对象 / Third dimension iterable
 * @param objs4 第四维可迭代对象 / Fourth dimension iterable
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 四维线性表达式符号组合 / 4D linear expression symbol combination
*/
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

/**
 * 从四维可迭代对象扁平映射创建线性表达式符号组合。
 * Create linear expression symbol combination by flat-mapping 4D iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs1 第一维可迭代对象 / First dimension iterable
 * @param objs2 第二维可迭代对象 / Second dimension iterable
 * @param objs3 第三维可迭代对象 / Third dimension iterable
 * @param objs4 第四维可迭代对象 / Fourth dimension iterable
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 四维线性表达式符号组合 / 4D linear expression symbol combination
*/
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

/**
 * 从动态维度可迭代对象映射创建线性表达式符号组合。
 * Create linear expression symbol combination by mapping dynamic-dimension iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs 嵌套可迭代对象 / Nested iterable objects
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 动态维度线性表达式符号组合 / Dynamic-dimension linear expression symbol combination
*/
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

/**
 * 从动态维度可迭代对象扁平映射创建线性表达式符号组合。
 * Create linear expression symbol combination by flat-mapping dynamic-dimension iterables.
 *
 * @param name 组合名称 / Combination name
 * @param objs 嵌套可迭代对象列表 / Nested iterable objects list
 * @param ctor 线性多项式构造函数 / Linear polynomial constructor
 * @param suffix 名称后缀函数 / Name suffix function
 * @return 动态维度线性表达式符号组合 / Dynamic-dimension linear expression symbol combination
*/
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
