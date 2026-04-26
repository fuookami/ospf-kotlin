package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

import kotlin.jvm.JvmName

@JvmName("convertQuantitySymbol")
fun <V : Symbol> Quantity<V>.to(unit: PhysicalUnit): Quantity<V>? {
    return if (this.unit.canConvertTo(unit)) {
        Quantity(this.value, unit)
    } else {
        null
    }
}
