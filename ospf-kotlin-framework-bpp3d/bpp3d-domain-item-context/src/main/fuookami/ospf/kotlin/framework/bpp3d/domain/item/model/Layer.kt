/**
 * Layer model.
 * 层模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.sortedWithThreeWayComparator
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
/**
 * PlaneLayer class.
 * PlaneLayer类。
*/
class PlaneLayer<P : ProjectivePlane>(
    // inherited from Container2<PlaneLayer<P>, P>
    override val shape: AbstractContainer2Shape<P>,
    override val units: List<QuantityPlacement2<Item, FltX, P>>
) : Container2<PlaneLayer<P>, FltX, P> {
    companion object {
        operator fun <P : ProjectivePlane> invoke(
            space: AbstractContainer3Shape,
            units: List<QuantityPlacement2<Item, FltX, P>>,
            plane: P
        ): PlaneLayer<P> {
            return PlaneLayer(
                shape = Container2Shape(
                    space = space,
                    plane = plane
                ),
                units = units
            )
        }

        operator fun <P : ProjectivePlane, S : Container3<S, FltX>> invoke(
            container: Container3<S, FltX>,
            units: List<QuantityPlacement2<Item, FltX, P>>,
            plane: P
        ): PlaneLayer<P> {
            return this(
                space = Container3Shape(
                    width = container.shape.width,
                    height = container.shape.height,
                    depth = container.shape.depth
                ),
                units = units,
                plane = plane
            )
        }
    }

    override fun copy() = PlaneLayer(
        shape = shape,
        units = units.map { it.copy() }
    )
}

/**
 * BinLayer class.
 * BinLayer类。
*/
class BinLayer(
    val iteration: Int64,
    val from: KClass<*>,
    val bin: BinType<FltX>? = null,
    // inherited from Container3<BinLayer>
    override val shape: AbstractContainer3Shape,
    override val units: List<QuantityPlacement3<*, FltX>>,
) : ItemContainer<BinLayer>, ManualIndexed() {
    companion object {
        operator fun invoke(
            iteration: Int64,
            bin: BinType<FltX>,
            layer: PlaneLayer<Side>
        ): BinLayer {
            return BinLayer(
                iteration = iteration,
                from = PlaneLayer::class,
                bin = bin,
                shape = Container3Shape(layer),
                units = layer
                    .units
                    .flatMap { it.toPlacement3() }
                    .sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }
            )
        }

        operator fun invoke(
            iteration: Int64,
            from: KClass<*>,
            bin: BinType<FltX>,
            units: List<QuantityPlacement3<Item, FltX>>
        ): BinLayer {
            return BinLayer(
                iteration = iteration,
                from = from,
                bin = bin,
                shape = Container3Shape(Container2Shape(bin.asContainer3Shape(), Side)),
                units = units.sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }
            )
        }
    }

    // inherited from Cuboid<BinLayer>
    override val depth: Quantity<FltX> = units.maxOfOrNullQuantity { it.maxZ } ?: (shape.depth * FltX.zero)

    // inherited from ItemContainer<BinLayer>
    override val bottomOnly: Boolean = true

    override fun copy() = BinLayer(
        iteration = iteration,
        from = from,
        bin = bin,
        shape = shape,
        units = units.map { it.copy() }
    )

    override fun hashCode() = units.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinLayer) return false

        if (shape != other.shape) return false
        if (units.size != other.units.size) return false
        if (!(items.toTypedArray() contentEquals other.items.toTypedArray())) return false

        return true
    }
}

/**
 * PalletLayer class.
 * PalletLayer类。
*/
class PalletLayer(
    val iteration: Int64,
    val from: KClass<*>,
    // inherited from Container3<PalletLayer>
    override val shape: AbstractContainer3Shape,
    override val units: List<QuantityPlacement3<*, FltX>>,
) : ItemContainer<PalletLayer>, ManualIndexed() {
    companion object {
        operator fun invoke(
            iteration: Int64,
            layer: PlaneLayer<Bottom>
        ): BinLayer {
            return BinLayer(
                iteration = iteration,
                from = PlaneLayer::class,
                shape = Container3Shape(layer),
                units = layer
                    .units
                    .flatMap { it.toPlacement3() }
                    .sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }
            )
        }

        operator fun invoke(
            iteration: Int64,
            from: KClass<*>,
            bin: BinType<FltX>,
            units: List<QuantityPlacement3<Item, FltX>>
        ): BinLayer {
            return BinLayer(
                iteration = iteration,
                from = from,
                shape = Container3Shape(Container2Shape(bin.asContainer3Shape(), Bottom)),
                units = units.sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }
            )
        }
    }

    // inherited from Cuboid<PalletLayer>
    override val height: Quantity<FltX> = units.maxOfOrNullQuantity { it.maxY } ?: (shape.height * FltX.zero)

    // inherited from ItemContainer<PalletLayer>
    override val topFlat: Boolean = true

    override fun copy() = PalletLayer(
        iteration = iteration,
        from = from,
        shape = shape,
        units = units.map { it.copy() }
    )

    override fun hashCode() = units.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinLayer) return false

        if (shape != other.shape) return false
        if (units.size != other.units.size) return false
        if (!(units.toTypedArray() contentEquals other.units.toTypedArray())) return false

        return true
    }
}
