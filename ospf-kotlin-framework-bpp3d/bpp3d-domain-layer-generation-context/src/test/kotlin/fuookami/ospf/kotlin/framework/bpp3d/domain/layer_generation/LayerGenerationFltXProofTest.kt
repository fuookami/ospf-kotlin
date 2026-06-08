package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgramMaterialValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.enabledStackingOn
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.resolvedPackingShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.toItemPlacementOrNull
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackedBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackedItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingAggregation
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.PackingRendererAdapter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.asShapePlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.toDouble
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.PI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LayerGenerationFltXProofTest {
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

    private fun q(value: FltX, unit: PhysicalUnit): Quantity<FltX> {
        return value * unit
    }

    private fun cylinderItem(
        id: String,
        axis: Axis3,
        radiusValue: Double = 0.5
    ): ActualItem {
        val radius = infraScalar(radiusValue) * Meter
        val diameter = radius + radius
        val length = infraScalar(1.0) * Meter
        val weight = infraScalar(0.2) * Kilogram
        val shape = when (axis) {
            Axis3.X -> PackageShape(
                width = length,
                height = diameter,
                depth = diameter,
                weight = weight,
                packageType = PackageType.CartonContainer
            )

            Axis3.Y -> PackageShape(
                width = diameter,
                height = length,
                depth = diameter,
                weight = weight,
                packageType = PackageType.CartonContainer
            )

            Axis3.Z -> PackageShape(
                width = diameter,
                height = diameter,
                depth = length,
                weight = weight,
                packageType = PackageType.CartonContainer
            )
        }
        val pack = PackingProgram.innerPackage(
            shape = shape,
            materials = emptyMap()
        )
        return ActualItem(
            id = id,
            name = id,
            width = pack.width,
            height = pack.height,
            depth = pack.depth,
            weight = pack.weight,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
    }

    private fun cuboidItem(
        id: String,
        widthValue: Double = 1.0,
        heightValue: Double = 1.0,
        depthValue: Double = 1.0
    ): ActualItem {
        val shape = PackageShape(
            width = infraScalar(widthValue) * Meter,
            height = infraScalar(heightValue) * Meter,
            depth = infraScalar(depthValue) * Meter,
            weight = infraScalar(0.2) * Kilogram,
            packageType = PackageType.CartonContainer
        )
        val pack = PackingProgram.innerPackage(
            shape = shape,
            materials = emptyMap()
        )
        return ActualItem(
            id = id,
            name = id,
            width = pack.width,
            height = pack.height,
            depth = pack.depth,
            weight = pack.weight,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        )
    }

    private fun assertCirclePackingLayerGeometry(layer: BinLayer) {
        val shapePlacements = layer.units.map { placement ->
            val shape = placement.resolvedPackingShape()
            assertTrue(layer.shape.enabled(shape, placement.absolutePosition))
            placement.asShapePlacement3 { shape }
        }
        for (lhsIndex in shapePlacements.indices) {
            for (rhsIndex in (lhsIndex + 1) until shapePlacements.size) {
                val overlap = shapePlacements[lhsIndex].footprintOverlapArea(shapePlacements[rhsIndex])
                assertTrue(abs(overlap.toDouble()) < 1e-7)
            }
        }
    }

    private fun assertStackedLayerGeometry(layer: BinLayer) {
        val shapePlacements = layer.units.map { placement ->
            val shape = placement.resolvedPackingShape()
            assertTrue(layer.shape.enabled(shape, placement.absolutePosition))
            placement.asShapePlacement3 { shape }
        }
        for (lhsIndex in shapePlacements.indices) {
            for (rhsIndex in (lhsIndex + 1) until shapePlacements.size) {
                assertTrue(!(shapePlacements[lhsIndex] overlapped shapePlacements[rhsIndex]))
            }
        }
    }

    @Test
    fun fltXStatisticsShouldBeAvailableInLayerGenerationContext() {
        val context = LayerGenerationContext<InfraNumber>()
        assertNotNull(context)

        val material = GenericMaterial(
            no = MaterialNo("M-LG"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-LG",
            weight = q(FltX(0.25), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
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
            id = "item-lg",
            name = "item-lg",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-LG"),
            packageAttribute = defaultPackageAttribute()
        )

        val stats = item.statistics(
            mode = Bpp3dDemandMode.ItemMaterialWeight,
            amount = UInt64(3)
        )
        assertEquals(1, stats.size)

        val generated = runBlocking {
            context.generate(
                Bpp3dLayerGenerationRequest(
                    iteration = 0,
                    items = emptyList()
                )
            )
        }
        assertTrue(generated.isEmpty())
    }

    @Test
    fun genericRequestAdapterShouldConvertGenericItemsAndLayers() {
        val material = GenericMaterial(
            no = MaterialNo("M-REQ"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-REQ",
            weight = q(FltX(0.15), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.2), Meter),
            height = q(FltX(0.6), Meter),
            depth = q(FltX(0.7), Meter),
            weight = q(FltX(0.4), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val pack = GenericPackage.innerPackage(
            shape = shape,
            materials = mapOf(material to UInt64.one)
        )
        val item = GenericItem(
            id = "item-req",
            name = "item-req",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-REQ"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = GenericBinLayer(
            iteration = fuookami.ospf.kotlin.math.algebra.number.Int64.zero,
            from = LayerGenerationFltXProofTest::class,
            width = q(FltX(2.0), Meter),
            height = q(FltX(2.0), Meter),
            depth = q(FltX(2.0), Meter),
            units = listOf(
                GenericItemPlacement(
                    item = item,
                    x = q(FltX(0.0), Meter),
                    y = q(FltX(0.0), Meter),
                    z = q(FltX(0.0), Meter)
                )
            )
        )

        val request = bpp3dLayerGenerationRequestFromGeneric<FltX, InfraNumber>(
            iteration = 2,
            items = listOf(item),
            existingLayers = listOf(layer),
            maxCandidates = 4
        )

        assertEquals(1, request.items.size)
        assertEquals("item-req", (request.items.first() as ActualItem).id)
        assertEquals(1, request.existingLayers.size)
        assertEquals(1, request.existingLayers.first().units.size)
    }

    @Test
    fun delegatedGeneratorsShouldProvideNonEmptyResultWhenHistoricalLayersExist() = runBlocking {
        val context = LayerGenerationContext<InfraNumber>(
            generators = listOf(
                BlockLayerGenerator(),
                BLLocalLayerGenerator(),
                BLGlobalLayerGenerator(),
                PatternLayerGenerator(),
                PileLayerGenerator(),
                CirclePackingLayerGenerator()
            )
        )

        val material = GenericMaterial(
            no = MaterialNo("M-LG2"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-LG2",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(0.5), Meter),
            depth = q(FltX(0.5), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val pack = GenericPackage.innerPackage(
            shape = shape,
            materials = mapOf(material to UInt64.one)
        )
        val item = GenericItem(
            id = "item-lg2",
            name = "item-lg2",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-LG2"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer = GenericBinLayer(
            iteration = fuookami.ospf.kotlin.math.algebra.number.Int64.zero,
            from = LayerGenerationFltXProofTest::class,
            width = q(FltX(3.0), Meter),
            height = q(FltX(3.0), Meter),
            depth = q(FltX(3.0), Meter),
            units = listOf(
                GenericItemPlacement(
                    item = item,
                    x = q(FltX(0.0), Meter),
                    y = q(FltX(0.0), Meter),
                    z = q(FltX(0.0), Meter)
                )
            )
        ).toModel()

        val generated = context.generate(
            Bpp3dLayerGenerationRequest(
                iteration = 1,
                items = listOf(item.toModel()),
                existingLayers = listOf(layer),
                maxCandidates = 8
            )
        )
        assertTrue(generated.isNotEmpty())
        assertEquals(1, generated.map { it.layer }.distinct().size)
    }

    @Test
    fun generatedLayersShouldBeRankedByShadowPriceWhenEvaluatorProvided() = runBlocking {
        val materialHigh = GenericMaterial(
            no = MaterialNo("M-HIGH"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-HIGH",
            weight = q(FltX(0.2), Kilogram)
        )
        val materialLow = GenericMaterial(
            no = MaterialNo("M-LOW"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-LOW",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(0.5), Meter),
            depth = q(FltX(0.5), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )

        val lowItem = GenericItem(
            id = "item-low",
            name = "item-low",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(materialLow to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-LOW"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val highItem = GenericItem(
            id = "item-high",
            name = "item-high",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(materialHigh to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-HIGH"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val highMaterialKey = highItem.materialAmounts.keys.first()
        val lowMaterialKey = lowItem.materialAmounts.keys.first()

        val generator = BlockLayerGenerator<InfraNumber>()
        val generated = generator.generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                items = listOf(lowItem, highItem),
                demandEntries = listOf(
                    LayerGenerationDemandEntry(
                        mode = Bpp3dDemandMode.ItemMaterialAmount,
                        key = fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey.Material(highMaterialKey)
                    ),
                    LayerGenerationDemandEntry(
                        mode = Bpp3dDemandMode.ItemMaterialAmount,
                        key = fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey.Material(lowMaterialKey)
                    )
                ),
                shadowPrices = linkedMapOf(
                    DemandModeKey(
                        Bpp3dDemandMode.ItemMaterialAmount,
                        fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey.Material(highMaterialKey)
                    ) to InfraNumber(10.0),
                    DemandModeKey(
                        Bpp3dDemandMode.ItemMaterialAmount,
                        fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey.Material(lowMaterialKey)
                    ) to InfraNumber(1.0)
                ),
                scoreByShadowPrice = shadowPriceAwareLayerScore(shadowPriceToScalar = { it }),
                maxCandidates = 2
            )
        )

        assertEquals(2, generated.size)
        val firstScore = generated.first().numericScore
        val lastScore = generated.last().numericScore
        assertNotNull(firstScore)
        assertNotNull(lastScore)
        assertEquals(10.0, firstScore.toDouble(), 1e-10)
        assertEquals(1.0, lastScore.toDouble(), 1e-10)
        assertTrue((firstScore ?: InfraNumber.zero) > (lastScore ?: InfraNumber.zero))
    }

    @Test
    fun shadowPriceAwareLayerScoreShouldRespectWeightOnlyProgramContribution() = runBlocking {
        val material = Material(
            no = MaterialNo("M-WEIGHT-ONLY"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-WEIGHT-ONLY",
            weight = infraScalar(2.0) * Kilogram
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = PackageShape(
                width = infraScalar(1.0) * Meter,
                height = infraScalar(1.0) * Meter,
                depth = infraScalar(1.0) * Meter,
                weight = infraScalar(1.0) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    weight = infraScalar(3.0) * Kilogram
                )
            )
        )
        val item = ActualItem(
            id = "item-weight-only-program",
            name = "item-weight-only-program",
            width = program.width,
            height = program.height,
            depth = program.depth,
            weight = program.weight,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-WEIGHT-ONLY"),
            packageAttribute = defaultPackageAttribute(),
            materialAmountsOverride = program.materialAmounts(),
            materialWeightsOverride = program.materialWeights()
        )
        val demandKey = Bpp3dDemandKey.Material(material.key)
        val generated = BlockLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                items = listOf(item),
                demandEntries = listOf(
                    LayerGenerationDemandEntry(
                        mode = Bpp3dDemandMode.ItemMaterialAmount,
                        key = demandKey
                    ),
                    LayerGenerationDemandEntry(
                        mode = Bpp3dDemandMode.ItemMaterialWeight,
                        key = demandKey
                    )
                ),
                shadowPrices = linkedMapOf(
                    DemandModeKey(
                        mode = Bpp3dDemandMode.ItemMaterialAmount,
                        key = demandKey
                    ) to InfraNumber(10.0),
                    DemandModeKey(
                        mode = Bpp3dDemandMode.ItemMaterialWeight,
                        key = demandKey
                    ) to InfraNumber(2.0)
                ),
                scoreByShadowPrice = shadowPriceAwareLayerScore(shadowPriceToScalar = { it }),
                maxCandidates = 1
            )
        )

        assertEquals(1, generated.size)
        assertNotNull(generated.first().numericScore)
        assertEquals(6.0, generated.first().numericScore!!.toDouble(), 1e-10)
    }

    @Test
    fun blockLayerGeneratorShouldUseBlockLoadingWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-BLOCK"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-BLOCK",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(1.0), Meter),
            depth = q(FltX(1.0), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-block",
            name = "item-block",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-BLOCK"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-BLOCK"
        )

        val generated = BlockLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item, item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.isNotEmpty() })
    }

    @Test
    fun blockLayerGeneratorShouldSupportGenericGenerateEntryPoint() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-BLOCK-GEN"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-BLOCK-GEN",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(1.0), Meter),
            depth = q(FltX(1.0), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-block-gen",
            name = "item-block-gen",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-BLOCK-GEN"),
            packageAttribute = defaultPackageAttribute()
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-BLOCK-GEN"
        )

        val generated = BlockLayerGenerator<InfraNumber>().generateFromGeneric(
            iteration = 0,
            bin = bin,
            items = listOf(item, item),
            maxCandidates = 4
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.isNotEmpty() })
    }

    @Test
    fun patternLayerGeneratorShouldUsePatternGroupedBlockLoadingWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-PATTERN"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PATTERN",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(1.0), Meter),
            depth = q(FltX(1.0), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-pattern",
            name = "item-pattern",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-PATTERN"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-PATTERN"
        )

        val generated = PatternLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item, item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.isNotEmpty() })
    }

    @Test
    fun pileLayerGeneratorShouldStackItemsWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-PILE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PILE",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(1.0), Meter),
            depth = q(FltX(1.0), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-pile",
            name = "item-pile",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-PILE"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val bin = BinType(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-PILE"
        )

        val generated = PileLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.size > 1 })
    }

    @Test
    fun pileLayerGeneratorShouldRejectHorizontalCylinderAxes() = runBlocking {
        val bin = BinType(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-PILE-HORIZONTAL-CYLINDER"
        )

        for (axis in listOf(Axis3.X, Axis3.Z)) {
            val item = cylinderItem(
                id = "item-pile-horizontal-$axis",
                axis = axis
            )

            val error = assertFailsWith<IllegalArgumentException> {
                PileLayerGenerator<InfraNumber>().generate(
                    Bpp3dLayerGenerationRequest(
                        iteration = 0,
                        bin = bin,
                        items = listOf(item),
                        maxCandidates = 4
                    )
                )
            }

            assertTrue(error.message?.contains("PileLayerGenerator") == true)
            assertTrue(error.message?.contains("only upright Axis3.Y items are allowed") == true)
        }
    }

    @Test
    fun fallbackLayerGeneratorsShouldRejectHorizontalCylinderAxesWithoutBin() = runBlocking {
        val generators: List<Pair<String, Bpp3dLayerGenerator<InfraNumber>>> = listOf(
            "block" to BlockLayerGenerator<InfraNumber>(),
            "bl-local" to BLLocalLayerGenerator<InfraNumber>(),
            "bl-global" to BLGlobalLayerGenerator<InfraNumber>(),
            "pattern" to PatternLayerGenerator<InfraNumber>(),
            "pile" to PileLayerGenerator<InfraNumber>(),
            "circle-packing" to CirclePackingLayerGenerator<InfraNumber>()
        )

        for ((source, generator) in generators) {
            for (axis in listOf(Axis3.X, Axis3.Z)) {
                val item = cylinderItem(
                    id = "item-$source-fallback-$axis",
                    axis = axis
                )

                val error = assertFailsWith<IllegalArgumentException> {
                    generator.generate(
                        Bpp3dLayerGenerationRequest(
                            iteration = 0,
                            items = listOf(item),
                            maxCandidates = 4
                        )
                    )
                }

                assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
                assertTrue(error.message?.contains("$axis") == true)
            }
        }
    }

    @Test
    fun blockLoadingLayerGeneratorsShouldRejectHorizontalCylinderAxesWithBin() = runBlocking {
        val generators: List<Pair<String, Bpp3dLayerGenerator<InfraNumber>>> = listOf(
            "block" to BlockLayerGenerator<InfraNumber>(),
            "bl-local" to BLLocalLayerGenerator<InfraNumber>(),
            "bl-global" to BLGlobalLayerGenerator<InfraNumber>(),
            "pattern" to PatternLayerGenerator<InfraNumber>()
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-BLOCK-LOADING-HORIZONTAL-CYLINDER"
        )

        for ((source, generator) in generators) {
            for (axis in listOf(Axis3.X, Axis3.Z)) {
                val item = cylinderItem(
                    id = "item-$source-block-loading-$axis",
                    axis = axis
                )

                val error = assertFailsWith<IllegalArgumentException> {
                    generator.generate(
                        Bpp3dLayerGenerationRequest(
                            iteration = 0,
                            bin = bin,
                            items = listOf(item),
                            maxCandidates = 4
                        )
                    )
                }

                assertTrue(error.message?.contains("SimpleBlockGenerator") == true)
                assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
                assertTrue(error.message?.contains("$axis") == true)
            }
        }
    }

    @Test
    fun circlePackingLayerGeneratorShouldGeneratePackedLayersWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-CIRCLE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-CIRCLE",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(1.0), Meter),
            depth = q(FltX(1.0), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-circle",
            name = "item-circle",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CIRCLE"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.source == "circle-packing-rect" })
        assertTrue(generated.all { it.layer.units.size > 1 })
        generated.forEach { result -> assertCirclePackingLayerGeometry(result.layer) }
    }

    @Test
    fun circlePackingLayerGeneratorShouldPreferHexWhenPatternCountsTie() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-CIRCLE-TIE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-CIRCLE-TIE",
            weight = q(FltX(0.2), Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(FltX(1.0), Meter),
            height = q(FltX(1.0), Meter),
            depth = q(FltX(1.0), Meter),
            weight = q(FltX(0.2), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-circle-tie",
            name = "item-circle-tie",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CIRCLE-TIE"),
            packageAttribute = defaultPackageAttribute()
        ).toModel()
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-TIE"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertEquals("circle-packing-hex", generated.first().source)
    }

    @Test
    fun circlePackingLayerGeneratorShouldGenerateHorizontalCylinderAxisX() = runBlocking {
        val item = cylinderItem(
            id = "item-circle-axis-x",
            axis = Axis3.X
        )
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-AXIS-X"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { result -> result.source == "circle-packing-horizontal-grid-axis=x" })
        assertTrue(generated.any { result -> result.source == "circle-packing-horizontal-grid-single-axis=x" })
        generated.forEach { result ->
            assertTrue(result.layer.units.isNotEmpty())
            assertTrue(
                result.layer.units.all { placement ->
                    val shape = placement.resolvedPackingShape()
                    shape is CylinderPackingShape3 && shape.axis == Axis3.X
                }
            )
            assertCirclePackingLayerGeometry(result.layer)
        }

        val grid = generated.first { result -> result.source == "circle-packing-horizontal-grid-axis=x" }
        val packedBin = PackedBin(
            name = "bin-horizontal-x",
            type = bin,
            items = grid.layer.items.map { placement ->
                PackedItem(
                    placement = placement,
                    loadingOrder = UInt64.zero
                )
            }
        )
        val renderedItem = PackingRendererAdapter().toSchema(
            PackingResult(
                aggregation = PackingAggregation(listOf(packedBin))
            )
        ).loadingPlans.first().items.first()
        assertEquals("HorizontalCylinderX", renderedItem.algorithmShapeType.name)
        assertEquals("X", renderedItem.axis?.name)
    }

    @Test
    fun circlePackingLayerGeneratorShouldGenerateHorizontalCylinderAxisZ() = runBlocking {
        val item = cylinderItem(
            id = "item-circle-axis-z",
            axis = Axis3.Z
        )
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-AXIS-Z"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { result -> result.source == "circle-packing-horizontal-grid-axis=z" })
        assertTrue(generated.any { result -> result.source == "circle-packing-horizontal-grid-single-axis=z" })
        generated.forEach { result ->
            assertTrue(result.layer.units.isNotEmpty())
            assertTrue(
                result.layer.units.all { placement ->
                    val shape = placement.resolvedPackingShape()
                    shape is CylinderPackingShape3 && shape.axis == Axis3.Z
                }
            )
            assertCirclePackingLayerGeometry(result.layer)
        }

        val grid = generated.first { result -> result.source == "circle-packing-horizontal-grid-axis=z" }
        val packedBin = PackedBin(
            name = "bin-horizontal-z",
            type = bin,
            items = grid.layer.items.map { placement ->
                PackedItem(
                    placement = placement,
                    loadingOrder = UInt64.zero
                )
            }
        )
        val renderedItem = PackingRendererAdapter().toSchema(
            PackingResult(
                aggregation = PackingAggregation(listOf(packedBin))
            )
        ).loadingPlans.first().items.first()
        assertEquals("HorizontalCylinderZ", renderedItem.algorithmShapeType.name)
        assertEquals("Z", renderedItem.axis?.name)
    }

    @Test
    fun circlePackingLayerGeneratorShouldGenerateHorizontalCylinderSupportedStack() = runBlocking {
        val support = cuboidItem(
            id = "item-circle-horizontal-stack-support",
            widthValue = 1.2,
            heightValue = 0.2,
            depthValue = 1.0
        )
        val cylinder = cylinderItem(
            id = "item-circle-horizontal-stack-cylinder",
            axis = Axis3.X,
            radiusValue = 0.5
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.5) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-HORIZONTAL-STACK"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(support, cylinder),
                maxCandidates = 12
            )
        )

        val stack = generated.first { result ->
            result.source == "circle-packing-horizontal-supported-stack-axis=x"
        }
        assertEquals(2, stack.layer.units.size)
        assertStackedLayerGeometry(stack.layer)

        val supportPlacement = stack.layer.units.first { placement ->
            placement.resolvedPackingShape() !is CylinderPackingShape3
        }.toItemPlacementOrNull() ?: throw IllegalStateException("missing support item placement")
        val cylinderPlacement = stack.layer.units.first { placement ->
            val shape = placement.resolvedPackingShape()
            shape is CylinderPackingShape3 && shape.axis == Axis3.X
        }.toItemPlacementOrNull() ?: throw IllegalStateException("missing cylinder item placement")
        assertEquals(0.0, supportPlacement.absoluteY.toDouble(), 1e-9)
        assertEquals(supportPlacement.height.toDouble(), cylinderPlacement.absoluteY.toDouble(), 1e-9)
        assertTrue(
            cylinderPlacement.enabledStackingOn(
                bottomItems = listOf(supportPlacement),
                space = stack.layer.shape
            )
        )

        val packedBin = PackedBin(
            name = "bin-horizontal-supported-stack",
            type = bin,
            items = stack.layer.items.map { placement ->
                PackedItem(
                    placement = placement,
                    loadingOrder = UInt64.zero
                )
            }
        )
        val renderedItems = PackingRendererAdapter().toSchema(
            PackingResult(
                aggregation = PackingAggregation(listOf(packedBin))
            )
        ).loadingPlans.single().items
        assertEquals(2, renderedItems.size)
        assertTrue(
            renderedItems.any { item ->
                item.algorithmShapeType.name == "HorizontalCylinderX" && item.axis?.name == "X"
            }
        )
    }

    @Test
    fun circlePackingLayerGeneratorShouldRejectPartialHorizontalCylinderSupportStack() = runBlocking {
        val partialSupport = cuboidItem(
            id = "item-circle-horizontal-stack-partial-support",
            widthValue = 0.8,
            heightValue = 0.2,
            depthValue = 1.0
        )
        val cylinder = cylinderItem(
            id = "item-circle-horizontal-stack-partial-cylinder",
            axis = Axis3.X,
            radiusValue = 0.5
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.5) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-HORIZONTAL-PARTIAL-STACK"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(partialSupport, cylinder),
                maxCandidates = 12
            )
        )

        assertTrue(
            generated.none { result ->
                result.source == "circle-packing-horizontal-supported-stack-axis=x"
            }
        )
    }

    @Test
    fun circlePackingLayerGeneratorShouldPreserveCylinderItemIdentity() = runBlocking {
        val item = cylinderItem(
            id = "item-circle-axis-y",
            axis = Axis3.Y
        )
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-AXIS-Y"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.all { it.layer.units.isNotEmpty() })
        assertTrue(
            generated.flatMap { it.layer.units }.all { placement ->
                val unit = placement.view.unit as? Item
                val shape = unit?.packingShape
                (unit === item) &&
                    shape is CylinderPackingShape3 &&
                    shape.axis == Axis3.Y
            }
        )
    }

    @Test
    fun circlePackingLayerGeneratorShouldValidateVerticalCylinderFootprints() = runBlocking {
        val item = cylinderItem(
            id = "item-circle-geometry",
            axis = Axis3.Y
        )
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-GEOMETRY"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.size > 1 })
        generated.forEach { result -> assertCirclePackingLayerGeometry(result.layer) }
    }

    @Test
    fun circlePackingLayerGeneratorShouldKeepMixedShapeCandidatesGeometricallyValid() = runBlocking {
        val cuboid = cuboidItem(
            id = "item-circle-mixed-cuboid",
            widthValue = 0.9,
            depthValue = 0.8
        )
        val smallCylinder = cylinderItem(
            id = "item-circle-mixed-small-cylinder",
            axis = Axis3.Y,
            radiusValue = 0.5
        )
        val largeCylinder = cylinderItem(
            id = "item-circle-mixed-large-cylinder",
            axis = Axis3.Y,
            radiusValue = 0.75
        )
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(3.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-MIXED"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(cuboid, smallCylinder, largeCylinder),
                maxCandidates = 8
            )
        )

        assertTrue(generated.any { result -> result.layer.units.any { it.unit === cuboid } })
        assertTrue(generated.any { result -> result.layer.units.any { it.unit === smallCylinder } })
        assertTrue(generated.any { result -> result.layer.units.any { it.unit === largeCylinder } })
        generated.forEach { result -> assertCirclePackingLayerGeometry(result.layer) }
    }

    @Test
    fun circlePackingLayerGeneratorShouldAcceptBoundaryTangentVerticalCylinder() = runBlocking {
        val item = cylinderItem(
            id = "item-circle-boundary",
            axis = Axis3.Y,
            radiusValue = 1.0
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-BOUNDARY"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.all { it.layer.units.size == 1 })
        generated.forEach { result -> assertCirclePackingLayerGeometry(result.layer) }
    }

    @Test
    fun circlePackingLayerGeneratorShouldExpandDynamicRadiusCandidates() = runBlocking {
        val radius = infraScalar(0.15) * Meter
        val height = infraScalar(1.0) * Meter
        val weight = infraScalar(0.2) * Kilogram
        val shape = PackageShape(
            width = radius + radius,
            height = height,
            depth = radius + radius,
            weight = weight,
            packageType = PackageType.CartonContainer
        )
        val pack = PackingProgram.innerPackage(
            shape = shape,
            materials = emptyMap()
        )
        val item = ActualItem(
            id = "item-circle-dynamic-radius",
            name = "item-circle-dynamic-radius",
            width = pack.width,
            height = pack.height,
            depth = pack.depth,
            weight = pack.weight,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CIRCLE-DYNAMIC-RADIUS"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.Y,
                diameterMin = infraScalar(0.30) * Meter,
                diameterMax = infraScalar(0.36) * Meter,
                diameterStep = infraScalar(0.01) * Meter
            )
        )
        val bin = BinType(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-DYNAMIC-RADIUS"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 20
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { result -> result.source.contains("r=0.15") })
        assertTrue(generated.any { result -> result.source.contains("r=0.18") })
        assertTrue(generated.flatMap { it.layer.units }.all { placement -> placement.unit === item })
        generated.forEach { result -> assertCirclePackingLayerGeometry(result.layer) }

        val candidatesByRadius = generated.map { result ->
            val shape = result.layer.units.first().resolvedPackingShape() as CylinderPackingShape3
            Pair(result, shape.radius.toDouble())
        }
        val smallRadius = candidatesByRadius.first { (_, radiusValue) -> abs(radiusValue - 0.15) < 1e-9 }.first
        val largeRadius = candidatesByRadius.first { (_, radiusValue) -> abs(radiusValue - 0.18) < 1e-9 }.first
        assertTrue(smallRadius.layer.units.size > largeRadius.layer.units.size)

        val packedBin = PackedBin(
            name = "bin-dynamic-radius",
            type = bin,
            items = largeRadius.layer.items.map { placement ->
                PackedItem(
                    placement = placement,
                    loadingOrder = UInt64.zero
                )
            }
        )
        val schema = PackingRendererAdapter().toSchema(
            PackingResult(
                aggregation = PackingAggregation(listOf(packedBin))
            )
        )
        val sourcePlacement = largeRadius.layer.items.first()
        val renderedItem = schema.loadingPlans.first().items.first()
        assertEquals(0.18, renderedItem.radius!!.toDouble(), 1e-9)
        assertEquals(0.36, renderedItem.diameter!!.toDouble(), 1e-9)
        assertEquals(sourcePlacement.absoluteX.toDouble(), renderedItem.x.toDouble(), 1e-9)
        assertEquals(sourcePlacement.absoluteY.toDouble(), renderedItem.y.toDouble(), 1e-9)
        assertEquals(sourcePlacement.absoluteZ.toDouble(), renderedItem.z.toDouble(), 1e-9)
        assertEquals(PI * 0.18 * 0.18 * 1.0, renderedItem.actualVolume!!.toDouble(), 1e-9)
    }

    @Test
    fun circlePackingLayerGeneratorShouldExpandDynamicRadiusHorizontalCandidates() = runBlocking {
        val radius = infraScalar(0.15) * Meter
        val diameter = radius + radius
        val length = infraScalar(1.0) * Meter
        val weight = infraScalar(0.2) * Kilogram
        val shape = PackageShape(
            width = length,
            height = diameter,
            depth = diameter,
            weight = weight,
            packageType = PackageType.CartonContainer
        )
        val pack = PackingProgram.innerPackage(
            shape = shape,
            materials = emptyMap()
        )
        val item = ActualItem(
            id = "item-circle-horizontal-dynamic-radius",
            name = "item-circle-horizontal-dynamic-radius",
            width = pack.width,
            height = pack.height,
            depth = pack.depth,
            weight = pack.weight,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CIRCLE-HORIZONTAL-DYNAMIC-RADIUS"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = Axis3.X,
                diameterMin = infraScalar(0.30) * Meter,
                diameterMax = infraScalar(0.36) * Meter,
                diameterStep = infraScalar(0.01) * Meter
            )
        )
        val bin = BinType(
            width = infraScalar(2.2) * Meter,
            height = infraScalar(0.4) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-HORIZONTAL-DYNAMIC-RADIUS"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 20
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { result -> result.source.contains("circle-packing-horizontal-grid-r=0.15-axis=x") })
        assertTrue(generated.any { result -> result.source.contains("circle-packing-horizontal-grid-r=0.18-axis=x") })
        generated.forEach { result -> assertCirclePackingLayerGeometry(result.layer) }

        val candidatesByRadius = generated
            .filter { result -> result.source.startsWith("circle-packing-horizontal-grid-r=") }
            .map { result ->
                val shape = result.layer.units.first().resolvedPackingShape() as CylinderPackingShape3
                Pair(result, shape.radius.toDouble())
            }
        val smallRadius = candidatesByRadius.first { (_, radiusValue) -> abs(radiusValue - 0.15) < 1e-9 }.first
        val largeRadius = candidatesByRadius.first { (_, radiusValue) -> abs(radiusValue - 0.18) < 1e-9 }.first
        assertTrue(smallRadius.layer.units.size > largeRadius.layer.units.size)

        val packedBin = PackedBin(
            name = "bin-horizontal-dynamic-radius",
            type = bin,
            items = largeRadius.layer.items.map { placement ->
                PackedItem(
                    placement = placement,
                    loadingOrder = UInt64.zero
                )
            }
        )
        val renderedItem = PackingRendererAdapter().toSchema(
            PackingResult(
                aggregation = PackingAggregation(listOf(packedBin))
            )
        ).loadingPlans.first().items.first()
        assertEquals("HorizontalCylinderX", renderedItem.algorithmShapeType.name)
        assertEquals("X", renderedItem.axis?.name)
        assertEquals(0.18, renderedItem.radius!!.toDouble(), 1e-9)
        assertEquals(0.36, renderedItem.diameter!!.toDouble(), 1e-9)
    }

    @Test
    fun circlePackingLayerGeneratorShouldRejectOversizedCylinderCandidate() = runBlocking {
        val item = cylinderItem(
            id = "item-circle-oversized",
            axis = Axis3.Y,
            radiusValue = 1.1
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(10.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-OVERSIZED"
        )

        val generated = CirclePackingLayerGenerator<InfraNumber>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isEmpty())
    }
}



