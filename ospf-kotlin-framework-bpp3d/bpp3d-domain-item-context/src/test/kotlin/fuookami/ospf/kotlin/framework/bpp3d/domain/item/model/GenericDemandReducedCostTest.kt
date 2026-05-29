package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GenericDemandReducedCostTest {
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
    fun itemReducedCostShouldUseActiveMaterialDemandEntriesOnly() {
        val material = GenericMaterial(
            no = MaterialNo("M-RC-MODEL-G64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-MODEL-G64",
            weight = infraScalar(0.5) * Kilogram
        )
        val item = GenericItem(
            id = "item-rc-model-g64",
            name = "item-rc-model-g64",
            pack = GenericPackage.innerPackage(
                shape = GenericPackageShape(
                    width = infraScalar(1.0) * Meter,
                    height = infraScalar(1.0) * Meter,
                    depth = infraScalar(1.0) * Meter,
                    weight = infraScalar(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(3))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-MODEL-G64"),
            packageAttribute = defaultPackageAttribute()
        )
        val materialKey = item.statistics(Bpp3dDemandMode.ItemMaterialAmount).keys.single()

        val reducedCost = item.reducedCost(
            demandEntries = listOf(Pair(Bpp3dDemandMode.ItemMaterialAmount, materialKey)),
            shadowPriceOf = { _, _ -> InfraNumber(2.0) },
            amountToScalar = { amount -> InfraNumber(amount.toULong().toDouble()) },
            zero = InfraNumber.zero
        )

        assertEquals(6.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun layerReducedCostShouldSupportFltXDemandValues() {
        val material = GenericMaterial(
            no = MaterialNo("M-RC-MODEL-GX"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-RC-MODEL-GX",
            weight = FltX(0.5) * Kilogram
        )
        val item = GenericItem(
            id = "item-rc-model-gx",
            name = "item-rc-model-gx",
            pack = GenericPackage.innerPackage(
                shape = GenericPackageShape(
                    width = FltX.one * Meter,
                    height = FltX.one * Meter,
                    depth = FltX.one * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64(2))
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-RC-MODEL-GX"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = GenericBinLayer(
            iteration = Int64.zero,
            from = GenericDemandReducedCostTest::class,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            depth = FltX(10.0) * Meter,
            units = listOf(
                GenericItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter),
                GenericItemPlacement(item, FltX.one * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )
        val materialKey = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight).keys.single()

        val reducedCost = layer.reducedCost(
            demandEntries = listOf(Pair(Bpp3dDemandMode.ItemMaterialWeight, materialKey)),
            shadowPriceOf = { _, _ -> FltX(1.5) },
            amountToScalar = { amount -> FltX(amount.toULong().toDouble()) },
            zero = FltX.zero
        )

        assertEquals(3.0, reducedCost.toDouble(), 1e-10)
    }
}

