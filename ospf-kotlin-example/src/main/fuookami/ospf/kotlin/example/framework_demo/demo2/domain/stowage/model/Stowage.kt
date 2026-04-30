package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*

class Stowage(
    private val items: List<Item>,
    private val positions: List<Position>,
) {
    companion object {
        fun stowageNeeded(item: Item, position: Position): Boolean {
            return item.status.stowageNeeded && position.status.stowageNeeded && position.enabled(item).ok
        }

        fun adjustmentNeeded(item: Item, position: Position): Boolean {
            if (!item.status.adjustmentNeeded) {
                return false
            }

            return if (position.status.stowageNeeded && position.enabled(item).ok) {
                true
            } else if (position.status.adjustmentNeeded && (position.enabled(item).ok || position.loadedItems.contains(item))) {
                true
            } else {
                false
            }
        }
    }

    lateinit var x: BinVariable2
    lateinit var u: BTerVariable2

    lateinit var stowage: LinearIntermediateSymbols2Flt64
    lateinit var loaded: LinearIntermediateSymbols1Flt64

    fun register(
        model: AbstractLinearMetaModelF64
    ): Try {
        if (!::x.isInitialized) {
            x = BinVariable2("x", Shape2(items.size, positions.size))
            for ((i, item) in items.withIndex()) {
                for ((j, position) in positions.withIndex()) {
                    x[i, j].name = "x_${item}_${position}"
                    if (position.loadedItems.contains(item)) {
                        x[i, j].range.eq(true)
                    } else if (!stowageNeeded(item, position)) {
                        x[i, j].range.eq(false)
                    }
                }
            }
        }
        for ((i, item) in items.withIndex()) {
            for ((j, position) in positions.withIndex()) {
                if (stowageNeeded(item, position)) {
                    when (val result = model.add(x[i, j])) {
                        is Ok -> {}

                        is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                    }
                }
            }
        }

        if (!::u.isInitialized) {
            u = BTerVariable2("u", Shape2(items.size, positions.size))
            for ((i, item) in items.withIndex()) {
                for ((j, position) in positions.withIndex()) {
                    u[i, j].name = "u_${item}_${position}"
                    if (!adjustmentNeeded(item, position)) {
                        u[i, j].range.eq(false)
                    }
                }
            }
        }
        for ((i, item) in items.withIndex()) {
            for ((j, position) in positions.withIndex()) {
                if (adjustmentNeeded(item, position)) {
                    when (val result = model.add(u[i, j])) {
                        is Ok -> {}

                        is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                    }
                }
            }
        }

        if (!::stowage.isInitialized) {
            stowage = LinearIntermediateSymbols2Flt64("stowage", Shape2(items.size, positions.size)) { _, v ->
                val item = items[v[0]]
                val position = positions[v[1]]
                val poly = MutableLinearPolynomial()
                if (stowageNeeded(item, position)) {
                    poly += LinearMonomial(Flt64.one, x[v])
                }
                if (adjustmentNeeded(item, position)) {
                    poly += LinearMonomial(Flt64.one, this.u[v])
                }
                if (position.loadedItems.contains(item)) {
                    poly += Flt64.one
                }
                val sym = LinearExpressionSymbol(
                    poly,
                    name = "stowage_${v.joinToString("_")}",
                )
                sym.range.set(ValueRange(Flt64.zero, Flt64.one).value!!)
                sym
            }
            for ((i, item) in items.withIndex()) {
                for ((j, position) in positions.withIndex()) {
                    if (stowage[i, j].range.fixedValue == null) {
                        if (position.enabled(item).ok || position.loadedItems.contains(item)) {
                            stowage[i, j].range.set(ValueRange(Flt64.zero, Flt64.one).value!!)
                        } else {
                            stowage[i, j].range.set(ValueRange(Flt64.zero, Flt64.zero).value!!)
                        }
                    }
                }
            }
        }
        when (val result = model.add(stowage)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loaded.isInitialized) {
            loaded = LinearIntermediateSymbols1Flt64("loaded", Shape1(items.size)) { i, _ ->
                val item = items[i]
                LinearExpressionSymbol(
                    sum(stowage[i, _a]),
                    name = "loaded_${item}",
                )
            }
            for ((i, item) in items.withIndex()) {
                if (loaded[i].range.fixedValue == null) {
                    when (item.status) {
                        ItemStatus.Reserved -> {
                            loaded[i].range.set(ValueRange(Flt64.zero, Flt64.zero).value!!)
                        }

                        ItemStatus.Optional -> {
                            loaded[i].range.set(ValueRange(Flt64.zero, Flt64.one).value!!)
                        }

                        ItemStatus.Preassigned, ItemStatus.Loaded, ItemStatus.AdjustmentNeeded -> {
                            loaded[i].range.set(ValueRange(Flt64.one, Flt64.one).value!!)
                        }
                    }
                }
            }
        }
        when (val result = model.add(loaded)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}













