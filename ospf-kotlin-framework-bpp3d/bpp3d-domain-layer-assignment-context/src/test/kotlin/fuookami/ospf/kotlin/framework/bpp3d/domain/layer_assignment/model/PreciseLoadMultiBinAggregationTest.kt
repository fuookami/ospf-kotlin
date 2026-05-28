package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.utils.functional.Ok

class PreciseLoadMultiBinAggregationTest {
    private object CargoAttr : AbstractCargoAttribute

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String, material: Material): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = infraScalar(1.0) * Meter,
                height = infraScalar(1.0) * Meter,
                depth = infraScalar(1.0) * Meter,
                weight = infraScalar(1.0) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = id,
            name = id,
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    @Test
    fun preciseLoadShouldAggregateLayerAmountAcrossAllBins() {
        val material = Material(
            no = MaterialNo("M-LOAD-MULTI-BIN"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-LOAD-MULTI-BIN",
            weight = infraScalar(1.0) * Kilogram
        )
        val actualItem = item("item-load-multi-bin", material)
        val sharedBinType = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LOAD-MULTI-BIN"
        )
        val layer = BinLayer(
            iteration = Int64.zero,
            from = PreciseLoadMultiBinAggregationTest::class,
            bin = sharedBinType,
            shape = Container3Shape(sharedBinType),
            units = listOf(
                QuantityPlacement3(
                    view = actualItem.view(Orientation.Upright),
                    position = point3()
                )
            )
        )
        val bins = listOf(
            Bin(
                shape = sharedBinType,
                units = emptyList<QuantityPlacement3<BinLayer>>(),
                batchNo = BatchNo("B-LOAD-MULTI-BIN-0")
            ),
            Bin(
                shape = sharedBinType,
                units = emptyList<QuantityPlacement3<BinLayer>>(),
                batchNo = BatchNo("B-LOAD-MULTI-BIN-1")
            )
        )
        val demandValue = InfraNumber.one
        val demandEntries = listOf(
            Bpp3dDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(actualItem),
                demand = demandValue,
                demandRange = ValueRange(
                    demandValue,
                    demandValue,
                    Interval.Closed,
                    Interval.Closed,
                    InfraNumber
                ).value!!
            )
        )

        val assignment = PreciseAssignment(
            bins = bins,
            layers = listOf(layer)
        )
        val model = LinearMetaModel(
            name = "precise-load-multi-bin-aggregation",
            converter = InfraNumber
        )
        assertTrue(assignment.register(model) is Ok)

        val load = PreciseLoad(
            demandEntries = demandEntries,
            layers = listOf(layer),
            assignment = assignment,
            overEnabled = false,
            lessEnabled = true
        )
        assertTrue(load.register(model) is Ok)

        val symbols = load.load[0]
            .toLinearPolynomial()
            .monomials
            .map { monomial -> monomial.symbol.name }
            .toSet()
        assertTrue(symbols.contains(assignment.x[0, 0].name))
        assertTrue(symbols.contains(assignment.x[1, 0].name))
    }
}


