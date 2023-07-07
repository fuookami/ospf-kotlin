package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*

class SymbolCombination<C : Category, S : Shape>(
    val name: String = "",
    shape: S
) : MultiArray<Symbol<C>, S>(shape) {
    val items = this
}

typealias Symbols1<C> = SymbolCombination<C, Shape1>
typealias Symbols2<C> = SymbolCombination<C, Shape2>
typealias Symbols3<C> = SymbolCombination<C, Shape3>
typealias Symbols4<C> = SymbolCombination<C, Shape4>
typealias DynSymbols<C> = SymbolCombination<C, DynShape>
typealias SymbolView<C> = MultiArrayView<Symbol<C>, *>
typealias SymbolView1<C> = MultiArrayView<Symbol<C>, Shape1>
typealias SymbolView2<C> = MultiArrayView<Symbol<C>, Shape2>
typealias SymbolView3<C> = MultiArrayView<Symbol<C>, Shape3>
typealias SymbolView4<C> = MultiArrayView<Symbol<C>, Shape4>
typealias DynSymbolView<C> = MultiArrayView<Symbol<C>, DynShape>

typealias LinearSymbols1 = Symbols1<Linear>
typealias LinearSymbols2 = Symbols2<Linear>
typealias LinearSymbols3 = Symbols3<Linear>
typealias LinearSymbols4 = Symbols4<Linear>
typealias DynLinearSymbols = DynSymbols<Linear>
typealias LinearSymbolView = SymbolView<Linear>
typealias LinearSymbolView1 = SymbolView1<Linear>
typealias LinearSymbolView2 = SymbolView2<Linear>
typealias LinearSymbolView3 = SymbolView3<Linear>
typealias LinearSymbolView4 = SymbolView4<Linear>
typealias DynLinearSymbolView = DynSymbolView<Linear>
