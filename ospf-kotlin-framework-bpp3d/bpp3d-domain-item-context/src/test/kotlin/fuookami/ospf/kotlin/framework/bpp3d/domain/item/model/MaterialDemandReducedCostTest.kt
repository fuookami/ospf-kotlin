package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.math.PI
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

class MaterialDemandReducedCostTest {
    private data class LocalDemandShadowPriceKey(
        val mode: Bpp3dDemandMode,
        val key: Bpp3dDemandKey
    ) : ShadowPriceKey(LocalDemandShadowPriceKey::class)

    private val cargo = object : AbstractCargoAttribute {}

    private data class ReducedCostContainer(
        override val shape: Container3Shape,
        override val units: List<QuantityPlacement3<*>>
    ) : Container3<ReducedCostContainer> {
        override fun copy(): ReducedCostContainer {
            return ReducedCostContainer(shape = shape, units = units)
        }
    }

    private class ZeroShadowPriceMap : BPP3DShadowPriceMap() {
        override fun invoke(arg: BPP3DShadowPriceArguments): Flt64 {
            return Flt64.zero
        }
    }

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderItem(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y
            )
        )
    }

    private fun placementOf(
        item: ActualItem,
        x: Double = 0.0,
        y: Double = 0.0,
        z: Double = 0.0
    ): QuantityPlacement3<Item> {
        return placement3Of(
            view = item.view(Orientation.Upright),
            position = point3(
                x = infraScalar(x) * Meter,
                y = infraScalar(y) * Meter,
                z = infraScalar(z) * Meter
            )
        )
    }

    private fun zeroShadowPriceMap(): BPP3DShadowPriceMap {
        return ZeroShadowPriceMap()
    }

    @Test
    fun reducedCostShouldUseOnlyActiveMaterialDemandEntries() {
        val material = Material(
            no = MaterialNo("M-RC"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC",
            weight = infraScalar(2.0) * Kilogram
        )
        val item = ActualItem(
            id = "item-rc",
            name = "item-rc",
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = infraScalar(1.0) * Meter,
                    height = infraScalar(1.0) * Meter,
                    depth = infraScalar(1.0) * Meter,
                    weight = infraScalar(0.1) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(3))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC"),
            packageAttribute = defaultPackageAttribute()
        )

        val shadowPriceMap = BPP3DShadowPriceMap()
        val materialKey = LocalDemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val itemKey = LocalDemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(item))
        shadowPriceMap[materialKey] = ShadowPrice(materialKey, Flt64(2.0))
        shadowPriceMap[itemKey] = ShadowPrice(itemKey, Flt64(100.0))

        val reducedCost = shadowPriceMap.reducedCost(
            unit = item,
            demandEntries = listOf(Pair(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))),
            shadowPriceOf = { mode, key ->
                shadowPriceMap[LocalDemandShadowPriceKey(mode, key)]?.price?.toDouble()?.let(::InfraNumber)
                    ?: infraScalar(0.0)
            },
            demandValueToScalar = { value ->
                when (value) {
                    is Bpp3dDemandValue.Amount -> infraScalar(value.value.toULong().toDouble())
                    is Bpp3dDemandValue.Weight -> infraScalar(value.value.value.toDouble())
                }
            }
        )

        assertEquals(6.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun legacyReducedCostShouldUseCylinderShapeVolume() {
        val item = cylinderItem("cyl-rc")

        val reducedCost = zeroShadowPriceMap().reducedCost(item)

        assertEquals(PI * 0.25, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun legacyReducedCostShouldUseCylinderShapeVolumeInsideContainer() {
        val first = cylinderItem("cyl-rc-a")
        val second = cylinderItem("cyl-rc-b")
        val container = ReducedCostContainer(
            shape = Container3Shape(
                width = infraScalar(3.0) * Meter,
                height = infraScalar(2.0) * Meter,
                depth = infraScalar(2.0) * Meter
            ),
            units = listOf(
                placementOf(item = first),
                placementOf(item = second, x = 1.0)
            )
        )

        val reducedCost = zeroShadowPriceMap().reducedCost(container)

        assertEquals(PI * 0.5, reducedCost.toDouble(), 1e-10)
    }
}
