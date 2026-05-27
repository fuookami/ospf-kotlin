package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as GenericBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.GenericBpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.GenericBpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.ItemPlacement as GenericItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Package as GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.PackageShape as GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.compat.asScalarF64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FltXDirectCompileProofTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun q(value: FltX, unit: PhysicalUnit): Quantity<FltX> {
        return value * unit
    }

    @Test
    fun fltXQuantityCanFlowThroughDomainAndSolverAdapterWithoutLegacyBridge() {
        val material = GenericMaterial(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-1",
            weight = q(FltX(0.5), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.2), Meter),
            height = q(FltX(0.8), Meter),
            depth = q(FltX(0.6), Meter),
            weight = q(FltX(0.3), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val pack = GenericPackage.innerPackage(
            shape = shape,
            materials = mapOf(material to UInt64(2))
        )
        val item = GenericItem(
            id = "item-1",
            name = "item-1",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-1"),
            packageAttribute = defaultPackageAttribute()
        )

        val layer = GenericBinLayer(
            iteration = Int64.zero,
            from = FltXDirectCompileProofTest::class,
            width = q(FltX(5.0), Meter),
            height = q(FltX(5.0), Meter),
            depth = q(FltX(5.0), Meter),
            units = listOf(
                GenericItemPlacement(
                    item = item,
                    x = q(FltX.zero, Meter),
                    y = q(FltX.zero, Meter),
                    z = q(FltX.zero, Meter)
                )
            )
        )

        val adapter = ScaledBpp3dSolverValueAdapter(
            scale = Bpp3dSolverFltXScale(
                amount = FltX(2.0),
                weight = FltX(10.0)
            )
        )

        val itemAmountStats = item.statistics(
            mode = Bpp3dDemandMode.ItemAmount,
            amount = UInt64(3)
        )
        val amountSolverValue = when (val amountValue = itemAmountStats.values.single()) {
            is GenericBpp3dDemandValue.Amount -> adapter.amountToSolver(amountValue.value)
            else -> error("Unexpected amount statistics value: $amountValue")
        }
        assertEquals(Flt64(6.0), amountSolverValue)

        val layerWeightStats = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight)
        val materialNo = when (val weightKey = layerWeightStats.keys.single()) {
            is GenericBpp3dDemandKey.Material -> weightKey.material.no
            else -> error("Unexpected weight statistics key: $weightKey")
        }
        assertEquals(MaterialNo("M-1"), materialNo)

        val weightSolverValue = when (val weightValue = layerWeightStats.values.single()) {
            is GenericBpp3dDemandValue.Weight -> adapter.weightToSolver(weightValue.value.asScalarF64())
            else -> error("Unexpected weight statistics value: $weightValue")
        }
        assertEquals(Flt64(10.0), weightSolverValue)
    }
}
