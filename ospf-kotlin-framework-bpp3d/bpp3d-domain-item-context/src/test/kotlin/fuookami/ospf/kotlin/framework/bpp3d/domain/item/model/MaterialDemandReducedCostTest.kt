package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MaterialDemandReducedCostTest {
    private data class LocalDemandShadowPriceKey(
        val mode: Bpp3dDemandMode,
        val key: Bpp3dDemandKey
    ) : ShadowPriceKey(LocalDemandShadowPriceKey::class)

    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
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
            cuboid = item,
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
}


