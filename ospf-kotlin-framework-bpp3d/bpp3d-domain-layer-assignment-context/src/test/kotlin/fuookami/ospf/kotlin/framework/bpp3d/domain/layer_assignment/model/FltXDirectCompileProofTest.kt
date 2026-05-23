package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.ItemPlacement as GenericItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as GenericBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Package as GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.PackageShape as GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
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

    private fun q(value: Double, unit: PhysicalUnit): Quantity<FltX> {
        return FltX(value) * unit
    }

    @Test
    fun fltXQuantityCanFlowThroughDomainAndSolverAdapter() {
        val material = GenericMaterial(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-1",
            weight = q(0.5, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.2, Meter),
            height = q(0.8, Meter),
            depth = q(0.6, Meter),
            weight = q(0.3, Kilogram),
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
            iteration = Int64(0),
            from = FltXDirectCompileProofTest::class,
            width = q(5.0, Meter),
            height = q(5.0, Meter),
            depth = q(5.0, Meter),
            units = listOf(
                GenericItemPlacement(
                    item = item,
                    x = q(0.0, Meter),
                    y = q(0.0, Meter),
                    z = q(0.0, Meter)
                )
            )
        ).toLegacy()

        val adapter = ScaledBpp3dSolverValueAdapter(
            scale = Bpp3dSolverFltXScale(
                amount = FltX(2.0),
                weight = FltX(10.0)
            )
        )

        val fixedRange = ValueRange(
            UInt64(3),
            UInt64(3),
            Interval.Closed,
            Interval.Closed,
            UInt64
        ).value!!
        val demandEntries = demandEntriesFromItemRanges(
            items = listOf(Triple(item.toLegacy(), UInt64(3), fixedRange)),
            demandValueAdapter = adapter
        )
        assertEquals(6.0, demandEntries.single().demand.toDouble(), 1e-10)

        val statistics = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight)
        val weightDemand = statistics[Bpp3dDemandKey.Material(material.toLegacy().key)] as Bpp3dDemandValue.Weight
        val solverValue = adapter.toSolver(weightDemand)

        assertEquals(10.0, solverValue.toDouble(), 1e-10)
    }
}
