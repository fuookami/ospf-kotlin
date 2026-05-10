package fuookami.ospf.kotlin.core.model.mechanism.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.*

fun <V> MetaModel<V>.add(symbol: Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>): Try where V : RealNumber<V>, V : NumberField<V> {
    return tokens.add(symbol.value)
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addQuantitySymbols")
fun <V> MetaModel<V>.add(symbols: Iterable<Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    return tokens.add(symbols.map { it.value })
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMapQuantitySymbols")
fun <V, K> MetaModel<V>.add(symbols: Map<K, Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    return tokens.add(symbols.values.map { it.value })
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMapQuantitySymbolLists")
fun <V, K> MetaModel<V>.add(symbols: Map<K, Iterable<Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMultiMap2QuantitySymbols")
fun <V, K1, K2> MetaModel<V>.add(symbols: MultiMap2<K1, K2, Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMultiMap2QuantitySymbolLists")
fun <V, K1, K2> MetaModel<V>.add(symbols: MultiMap2<K1, K2, Iterable<Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMultiMap3QuantitySymbols")
fun <V, K1, K2, K3> MetaModel<V>.add(symbols: MultiMap3<K1, K2, K3, Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMultiMap3QuantitySymbolLists")
fun <V, K1, K2, K3> MetaModel<V>.add(symbols: MultiMap3<K1, K2, K3, Iterable<Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMultiMap4QuantitySymbols")
fun <V, K1, K2, K3, K4> MetaModel<V>.add(symbols: MultiMap4<K1, K2, K3, K4, Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

@Suppress("INAPPLICABLE_JVM_NAME")
@JvmName("addMultiMap4QuantitySymbolLists")
fun <V, K1, K2, K3, K4> MetaModel<V>.add(symbols: MultiMap4<K1, K2, K3, K4, Iterable<Quantity<IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>>>): Try where V : RealNumber<V>, V : NumberField<V> {
    for (syms in symbols.values) {
        when (val result = add(syms)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

fun <V> MetaModel<V>.setSolverSolution(solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) where V : RealNumber<V>, V : NumberField<V> {
    tokens.setSolverSolution(solution)
}

fun <V> MetaModel<V>.setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) where V : RealNumber<V>, V : NumberField<V> {
    tokens.setSolverSolution(solution)
}
