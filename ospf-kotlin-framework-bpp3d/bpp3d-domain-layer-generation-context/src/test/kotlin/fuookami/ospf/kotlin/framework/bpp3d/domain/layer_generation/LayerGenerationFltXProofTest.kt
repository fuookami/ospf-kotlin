package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Package as GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.PackageShape as GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
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
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LayerGenerationFltXProofTest {
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
    fun fltXStatisticsShouldBeAvailableInLayerGenerationContext() {
        val context = LayerGenerationContext<Flt64>()
        assertNotNull(context)

        val material = GenericMaterial(
            no = MaterialNo("M-LG"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-LG",
            weight = q(0.25, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
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
    fun delegatedGeneratorsShouldProvideNonEmptyResultWhenHistoricalLayersExist() = runBlocking {
        val context = LayerGenerationContext<Flt64>(
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
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(0.5, Meter),
            depth = q(0.5, Meter),
            weight = q(0.2, Kilogram),
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
        val layer = fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer(
            iteration = fuookami.ospf.kotlin.math.algebra.number.Int64.zero,
            from = LayerGenerationFltXProofTest::class,
            width = q(3.0, Meter),
            height = q(3.0, Meter),
            depth = q(3.0, Meter),
            units = listOf(
                fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.ItemPlacement(
                    item = item,
                    x = q(0.0, Meter),
                    y = q(0.0, Meter),
                    z = q(0.0, Meter)
                )
            )
        ).toLegacy()

        val generated = context.generate(
            Bpp3dLayerGenerationRequest(
                iteration = 1,
                items = listOf(item.toLegacy()),
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
            weight = q(0.2, Kilogram)
        )
        val materialLow = GenericMaterial(
            no = MaterialNo("M-LOW"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-LOW",
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(0.5, Meter),
            depth = q(0.5, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )

        val lowItem = GenericItem(
            id = "item-low",
            name = "item-low",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(materialLow to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-LOW"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val highItem = GenericItem(
            id = "item-high",
            name = "item-high",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(materialHigh to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-HIGH"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val highMaterialKey = highItem.materialAmounts.keys.first()
        val lowMaterialKey = lowItem.materialAmounts.keys.first()

        val generator = BlockLayerGenerator<Flt64>()
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
                    ) to Flt64(10.0),
                    DemandModeKey(
                        Bpp3dDemandMode.ItemMaterialAmount,
                        fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey.Material(lowMaterialKey)
                    ) to Flt64(1.0)
                ),
                scoreByShadowPrice = shadowPriceAwareLayerScore(shadowPriceToDouble = { it.toDouble() }),
                maxCandidates = 2
            )
        )

        assertEquals(2, generated.size)
        assertEquals(10.0, generated.first().numericScore ?: Double.NaN, 1e-10)
        assertEquals(1.0, generated.last().numericScore ?: Double.NaN, 1e-10)
        assertTrue((generated.first().numericScore ?: 0.0) > (generated.last().numericScore ?: 0.0))
    }

    @Test
    fun blockLayerGeneratorShouldUseBlockLoadingWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-BLOCK"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-BLOCK",
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(1.0, Meter),
            depth = q(1.0, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-block",
            name = "item-block",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-BLOCK"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val bin = BinType(
            width = 2.0 * Meter,
            height = 1.0 * Meter,
            depth = 1.0 * Meter,
            capacity = 10.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-BLOCK"
        )

        val generated = BlockLayerGenerator<Flt64>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item, item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.size >= 2 })
    }

    @Test
    fun patternLayerGeneratorShouldUsePatternGroupedBlockLoadingWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-PATTERN"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PATTERN",
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(1.0, Meter),
            depth = q(1.0, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-pattern",
            name = "item-pattern",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-PATTERN"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val bin = BinType(
            width = 2.0 * Meter,
            height = 1.0 * Meter,
            depth = 1.0 * Meter,
            capacity = 10.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-PATTERN"
        )

        val generated = PatternLayerGenerator<Flt64>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item, item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.layer.units.size >= 2 })
    }

    @Test
    fun pileLayerGeneratorShouldStackItemsWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-PILE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PILE",
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(1.0, Meter),
            depth = q(1.0, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-pile",
            name = "item-pile",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-PILE"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val bin = BinType(
            width = 1.0 * Meter,
            height = 3.0 * Meter,
            depth = 1.0 * Meter,
            capacity = 10.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-PILE"
        )

        val generated = PileLayerGenerator<Flt64>().generate(
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
    fun circlePackingLayerGeneratorShouldGeneratePackedLayersWhenBinProvided() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-CIRCLE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-CIRCLE",
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(1.0, Meter),
            depth = q(1.0, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-circle",
            name = "item-circle",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CIRCLE"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val bin = BinType(
            width = 3.0 * Meter,
            height = 1.0 * Meter,
            depth = 2.0 * Meter,
            capacity = 10.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE"
        )

        val generated = CirclePackingLayerGenerator<Flt64>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { it.source == "circle-packing-rect" })
        assertTrue(generated.any { it.source == "circle-packing-hex" })
        assertTrue(generated.all { it.layer.units.size > 1 })
    }

    @Test
    fun circlePackingLayerGeneratorShouldPreferHexWhenPatternCountsTie() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-CIRCLE-TIE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-CIRCLE-TIE",
            weight = q(0.2, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.0, Meter),
            height = q(1.0, Meter),
            depth = q(1.0, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-circle-tie",
            name = "item-circle-tie",
            pack = GenericPackage.innerPackage(shape = shape, materials = mapOf(material to UInt64.one)),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CIRCLE-TIE"),
            packageAttribute = defaultPackageAttribute()
        ).toLegacy()
        val bin = BinType(
            width = 2.0 * Meter,
            height = 1.0 * Meter,
            depth = 1.0 * Meter,
            capacity = 10.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-LG-CIRCLE-TIE"
        )

        val generated = CirclePackingLayerGenerator<Flt64>().generate(
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
}

