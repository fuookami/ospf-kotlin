package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial

/**
 * Generic SubObject<V> with V-typed public evaluation.
 * Flt64 evaluation is handled by the solver adapter, not the core chain.
 */
sealed class SubObject<V : RealNumber<V>>(
    val category: ObjectCategory,
    val name: String = ""
) {
    abstract val cells: List<Cell<V>>
    abstract val constant: V

    abstract fun evaluate(): V?
    abstract fun evaluate(results: List<V>): V?
}

class LinearSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell<V>>,
    private val _constant: V,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constant: V get() = _constant

    override fun evaluate(): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    override fun evaluate(results: List<V>): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }

    companion object {
        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenData<V>,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): LinearSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = createLinearCells(
                flattenData.monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
                tokens,
                converter
            )
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }

        @Deprecated("Use invoke with LinearFlattenData<V> instead. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmName("invokeFlt64")
        operator fun invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenData<Flt64>,
            tokens: AbstractTokenTable<Flt64>,
            name: String = ""
        ): LinearSubObject<Flt64> {
            val cells = createLinearCells(flattenData.monomials, tokens, IntoValue.Identity)
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }

        @Deprecated("Use invoke with LinearFlattenData<V> instead. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmName("invokeFlt64Converter")
        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenData<Flt64>,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): LinearSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = ArrayList<LinearCell<V>>(flattenData.monomials.size)
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(
                        fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl(
                            tokenTable = tokens,
                            _coefficientFlt64 = monomial.coefficient,
                            token = token,
                            converter = converter
                        )
                    )
                }
            }
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = converter.intoValue(flattenData.constant),
                name = name
            )
        }
    }
}

class QuadraticSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCell<V>>,
    private val _constant: V,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constant: V get() = _constant

    override fun evaluate(): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    override fun evaluate(results: List<V>): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }

    companion object {
        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenData<V>,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): QuadraticSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = createQuadraticCells(
                flattenData.monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
                tokens,
                converter
            )
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }

        @Deprecated("Use invoke with QuadraticFlattenData<V> instead. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmName("invokeFlt64")
        operator fun invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenData<Flt64>,
            tokens: AbstractTokenTable<Flt64>,
            name: String = ""
        ): QuadraticSubObject<Flt64> {
            val cells = createQuadraticCells(flattenData.monomials, tokens, IntoValue.Identity)
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }

        @Deprecated("Use invoke with QuadraticFlattenData<V> instead. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmName("invokeFlt64Converter")
        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenData<Flt64>,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): QuadraticSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = ArrayList<QuadraticCell<V>>(flattenData.monomials.size)
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(
                        fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl(
                            tokenTable = tokens,
                            _coefficientFlt64 = monomial.coefficient,
                            token1 = token1,
                            token2 = token2,
                            converter = converter
                        )
                    )
                }
            }
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = converter.intoValue(flattenData.constant),
                name = name
            )
        }
    }
}

