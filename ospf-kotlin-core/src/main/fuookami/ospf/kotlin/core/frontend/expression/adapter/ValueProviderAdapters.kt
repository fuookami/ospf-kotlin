package fuookami.ospf.kotlin.core.frontend.expression.adapter

import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.adapter.ValueProvider

fun Map<Symbol, Flt64>.toUtilsValueProvider(): ValueProvider {
    return MapValueProvider(this)
}

fun Map<Symbol, Flt64>.toUtilsValueProvider(
    tokenList: AbstractTokenList?,
    zeroIfNone: Boolean = false
): ValueProvider {
    val values = this
    return ValueProvider { symbol ->
        values[symbol] ?: when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenList?.find(item = symbol)?.result
            }

            is IntermediateSymbol -> {
                symbol.evaluate(
                    values = values,
                    tokenList = tokenList,
                    zeroIfNone = zeroIfNone
                )
            }

            else -> {
                null
            }
        } ?: if (zeroIfNone) {
            Flt64.zero
        } else {
            null
        }
    }
}

fun Map<Symbol, Flt64>.toUtilsValueProvider(
    tokenTable: AbstractTokenTable?,
    zeroIfNone: Boolean = false
): ValueProvider {
    val values = this
    return ValueProvider { symbol ->
        values[symbol] ?: when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenTable?.find(item = symbol)?.result
            }

            is IntermediateSymbol -> {
                symbol.evaluate(
                    values = values,
                    tokenTable = tokenTable,
                    zeroIfNone = zeroIfNone
                )
            }

            else -> {
                null
            }
        } ?: if (zeroIfNone) {
            Flt64.zero
        } else {
            null
        }
    }
}

fun AbstractTokenList.toUtilsValueProvider(
    zeroIfNone: Boolean = false
): ValueProvider {
    return ValueProvider { symbol ->
        when (symbol) {
            is AbstractVariableItem<*, *> -> {
                find(item = symbol)?.result ?: if (zeroIfNone) {
                    Flt64.zero
                } else {
                    null
                }
            }

            is IntermediateSymbol -> {
                symbol.evaluate(this, zeroIfNone)
            }

            else -> {
                if (zeroIfNone) {
                    Flt64.zero
                } else {
                    null
                }
            }
        }
    }
}

fun AbstractTokenTable.toUtilsValueProvider(
    zeroIfNone: Boolean = false
): ValueProvider {
    return ValueProvider { symbol ->
        when (symbol) {
            is AbstractVariableItem<*, *> -> {
                find(item = symbol)?.result ?: if (zeroIfNone) {
                    Flt64.zero
                } else {
                    null
                }
            }

            is IntermediateSymbol -> {
                symbol.evaluate(this, zeroIfNone)
            }

            else -> {
                if (zeroIfNone) {
                    Flt64.zero
                } else {
                    null
                }
            }
        }
    }
}

fun List<Flt64>.toUtilsValueProvider(
    tokenList: AbstractTokenList,
    zeroIfNone: Boolean = false
): ValueProvider {
    val results = this
    return ValueProvider { symbol ->
        when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenList.indexOf(item = symbol)?.let {
                    if (it in results.indices) {
                        results[it]
                    } else {
                        null
                    }
                }
            }

            is IntermediateSymbol -> {
                symbol.evaluate(
                    results = results,
                    tokenList = tokenList,
                    zeroIfNone = zeroIfNone
                )
            }

            else -> {
                null
            }
        } ?: if (zeroIfNone) {
            Flt64.zero
        } else {
            null
        }
    }
}

fun List<Flt64>.toUtilsValueProvider(
    tokenTable: AbstractTokenTable,
    zeroIfNone: Boolean = false
): ValueProvider {
    val results = this
    return ValueProvider { symbol ->
        when (symbol) {
            is AbstractVariableItem<*, *> -> {
                tokenTable.indexOf(item = symbol)?.let {
                    if (it in results.indices) {
                        results[it]
                    } else {
                        null
                    }
                }
            }

            is IntermediateSymbol -> {
                symbol.evaluate(
                    results = results,
                    tokenTable = tokenTable,
                    zeroIfNone = zeroIfNone
                )
            }

            else -> {
                null
            }
        } ?: if (zeroIfNone) {
            Flt64.zero
        } else {
            null
        }
    }
}



