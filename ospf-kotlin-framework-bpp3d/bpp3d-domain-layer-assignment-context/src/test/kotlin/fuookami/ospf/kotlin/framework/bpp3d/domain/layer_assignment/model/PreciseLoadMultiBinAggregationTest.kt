package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.math.abs
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
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
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.itemPlacement3Of
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Axis3
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

    private fun item(
        id: String,
        material: Material<InfraNumber>,
        materialAmount: UInt64 = UInt64.one,
        shapeSpec: PackageShapeSpec = PackageShapeSpec.Cuboid
    ): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = infraScalar(1.0) * Meter,
                height = infraScalar(1.0) * Meter,
                depth = infraScalar(1.0) * Meter,
                weight = infraScalar(1.0) * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = shapeSpec
            ),
            materials = mapOf(material to materialAmount)
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
        val actualItem = item(
            id = "item-load-multi-bin",
            material = material
        )
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
                itemPlacement3Of(
                    view = actualItem.view(Orientation.Upright),
                    position = point3()
                )
            )
        )
        val bins = listOf(
            layerBinOf(
                shape = sharedBinType,
                units = emptyList<BinLayerPlacement>(),
                batchNo = BatchNo("B-LOAD-MULTI-BIN-0")
            ),
            layerBinOf(
                shape = sharedBinType,
                units = emptyList<BinLayerPlacement>(),
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

    @Test
    fun preciseLoadShouldKeepMixedDemandCoefficientsForCylinderAcrossBinsAndLayers() {
        val materialA = Material(
            no = MaterialNo("M-LOAD-MIX-A"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-LOAD-MIX-A",
            weight = infraScalar(1.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("M-LOAD-MIX-B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-LOAD-MIX-B",
            weight = infraScalar(2.0) * Kilogram
        )
        val cuboidItem = item(
            id = "item-load-cuboid",
            material = materialA,
            materialAmount = UInt64(2)
        )
        val cylinderItem = item(
            id = "item-load-cylinder",
            material = materialB,
            materialAmount = UInt64(3),
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y
            )
        )
        val sharedBinType = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LOAD-MIXED-DEMAND"
        )
        val layerWithCuboid = BinLayer(
            iteration = Int64.zero,
            from = PreciseLoadMultiBinAggregationTest::class,
            bin = sharedBinType,
            shape = Container3Shape(sharedBinType),
            units = listOf(
                itemPlacement3Of(
                    view = cuboidItem.view(Orientation.Upright),
                    position = point3()
                )
            )
        )
        val layerWithCylinder = BinLayer(
            iteration = Int64.one,
            from = PreciseLoadMultiBinAggregationTest::class,
            bin = sharedBinType,
            shape = Container3Shape(sharedBinType),
            units = listOf(
                itemPlacement3Of(
                    view = cylinderItem.view(Orientation.Upright),
                    position = point3(x = infraScalar(1.0) * Meter)
                )
            )
        )
        val bins = listOf(
            layerBinOf(
                shape = sharedBinType,
                units = emptyList<BinLayerPlacement>(),
                batchNo = BatchNo("B-LOAD-MIX-0")
            ),
            layerBinOf(
                shape = sharedBinType,
                units = emptyList<BinLayerPlacement>(),
                batchNo = BatchNo("B-LOAD-MIX-1")
            )
        )
        val demandEntries = listOf(
            Bpp3dDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(cylinderItem),
                demand = InfraNumber.one,
                demandRange = ValueRange(
                    InfraNumber.one,
                    InfraNumber.one,
                    Interval.Closed,
                    Interval.Closed,
                    InfraNumber
                ).value!!
            )
        ) + demandEntriesFromMaterialAmounts(
            materials = listOf(Pair(materialA, UInt64(2)))
        ) + demandEntriesFromMaterialWeights(
            materials = listOf(Pair(materialB, infraScalar(6.0) * Kilogram))
        )

        val layers = listOf(layerWithCuboid, layerWithCylinder)
        val assignment = PreciseAssignment(
            bins = bins,
            layers = layers
        )
        val model = LinearMetaModel(
            name = "precise-load-mixed-demand-cylinder-aggregation",
            converter = InfraNumber
        )
        assertTrue(assignment.register(model) is Ok)

        val load = PreciseLoad(
            demandEntries = demandEntries,
            layers = layers,
            assignment = assignment,
            overEnabled = false,
            lessEnabled = true
        )
        assertTrue(load.register(model) is Ok)

        fun coefficientAt(demandIndex: Int, binIndex: Int, layerIndex: Int): Double {
            val coefficientsBySymbol = load.load[demandIndex]
                .toLinearPolynomial()
                .monomials
                .associate { monomial ->
                    monomial.symbol.name to monomial.coefficient.toDouble()
                }
            return coefficientsBySymbol[assignment.x[binIndex, layerIndex].name] ?: 0.0
        }

        for (binIndex in bins.indices) {
            assertTrue(abs(coefficientAt(demandIndex = 0, binIndex = binIndex, layerIndex = 0) - 0.0) < 1e-10)
            assertTrue(abs(coefficientAt(demandIndex = 0, binIndex = binIndex, layerIndex = 1) - 1.0) < 1e-10)

            assertTrue(abs(coefficientAt(demandIndex = 1, binIndex = binIndex, layerIndex = 0) - 2.0) < 1e-10)
            assertTrue(abs(coefficientAt(demandIndex = 1, binIndex = binIndex, layerIndex = 1) - 0.0) < 1e-10)

            assertTrue(abs(coefficientAt(demandIndex = 2, binIndex = binIndex, layerIndex = 0) - 0.0) < 1e-10)
            assertTrue(abs(coefficientAt(demandIndex = 2, binIndex = binIndex, layerIndex = 1) - 6.0) < 1e-10)
        }
    }
}

