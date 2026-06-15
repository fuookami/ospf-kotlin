/**
 * 层生成方案候选适配器测试。
 * Layer generation program candidate adapters test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate

class LayerGenerationProgramCandidateAdaptersTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun makeProgram(material: Material<FltX>): PackingProgram<FltX> {
        return PackingProgram.innerPackageWithMaterialValues(
            shape = PackageShape(
                width = FltX(1.0) * Meter,
                height = FltX(0.8) * Meter,
                depth = FltX(0.6) * Meter,
                weight = FltX(0.2) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    amount = UInt64(3),
                    weight = FltX(6.0) * Kilogram
                )
            )
        )
    }

    @Test
    fun programCandidateShouldConvertToLayerGenerationItem() {
        val material = Material(
            no = MaterialNo("M-PROG-ADAPTER"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PROG-ADAPTER",
            weight = FltX(2.0) * Kilogram
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-a",
            program = makeProgram(material),
            itemName = "candidate-item-a",
            enabledOrientations = emptyList()
        )

        val item = candidate.toLayerGenerationItem(
            sequence = 7,
            materialCatalog = mapOf(material.key to material)
        ) as ActualItem

        assertEquals("program-candidate-candidate-a-7", item.id)
        assertEquals("candidate-item-a", item.name)
        assertEquals(Orientation.Upright, item.enabledOrientations.first())
        assertEquals(UInt64(3), item.materialAmounts[material.key])
        assertEquals(6.0, item.materialWeights[material.key]!!.value.toDouble(), 1e-10)
    }

    @Test
    fun programCandidateShouldPreserveCylinderShapeSpecForLayerGenerationItem() {
        val material = Material(
            no = MaterialNo("M-PROG-CYLINDER"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PROG-CYLINDER",
            weight = FltX(2.0) * Kilogram
        )
        val radius = FltX(0.5) * Meter
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = PackageShape(
                width = FltX(1.0) * Meter,
                height = FltX(1.2) * Meter,
                depth = FltX(1.0) * Meter,
                weight = FltX(0.2) * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = PackageShapeSpec.VerticalCylinder(
                    radius = radius,
                    axis = Axis3.Y
                )
            ),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    amount = UInt64(3)
                )
            )
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-cylinder",
            program = program,
            itemName = "candidate-cylinder-item"
        )

        val item = candidate.toLayerGenerationItem(
            sequence = 9,
            materialCatalog = mapOf(material.key to material)
        )
        val shape = item.packingShape

        assertTrue(shape is CylinderPackingShape3)
        assertEquals(Axis3.Y, shape.axis)
        assertEquals(0.5, shape.radius.value.toDouble(), 1e-10)
    }

    @Test
    fun programDemandsShouldConvertToItemDemandsInOrder() {
        val material = Material(
            no = MaterialNo("M-PROG-DEMAND"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PROG-DEMAND",
            weight = FltX(1.0) * Kilogram
        )
        val first = MaterialPackingProgramCandidate(
            id = "candidate-first",
            program = makeProgram(material),
            itemName = "first"
        )
        val second = MaterialPackingProgramCandidate(
            id = "candidate-second",
            program = makeProgram(material),
            itemName = "second"
        )

        val itemDemands = layerGenerationItemDemandsFromPrograms(
            programDemands = listOf(
                Pair(first, UInt64(2)),
                Pair(second, UInt64(5))
            ),
            materialCatalog = mapOf(material.key to material)
        )

        assertEquals(2, itemDemands.size)
        assertEquals(UInt64(2), itemDemands[0].second)
        assertEquals(UInt64(5), itemDemands[1].second)
        assertEquals("program-candidate-candidate-first-1", (itemDemands[0].first as ActualItem).id)
        assertEquals("program-candidate-candidate-second-2", (itemDemands[1].first as ActualItem).id)
        assertTrue(itemDemands.all { (item, _) -> item is ActualItem })
    }

    @Test
    fun requestFactoryShouldSupportProgramDemandsDirectly() {
        val material = Material(
            no = MaterialNo("M-PROG-REQUEST"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PROG-REQUEST",
            weight = FltX(1.0) * Kilogram
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-request",
            program = makeProgram(material),
            itemName = "request-item"
        )
        val request = bpp3dLayerGenerationRequestFromProgramDemands<FltX>(
            iteration = 3,
            items = emptyList(),
            programDemands = listOf(Pair(candidate, UInt64(2))),
            programMaterialCatalog = mapOf(material.key to material),
            existingLayers = emptyList(),
            demandEntries = emptyList(),
            shadowPrices = emptyMap(),
            maxCandidates = 8
        )

        assertEquals(2, request.items.size)
        assertTrue(request.items.all { it is ActualItem })
        val first = request.items.first() as ActualItem
        assertEquals("program-candidate-candidate-request-1", first.id)
        assertEquals(UInt64(3), first.materialAmounts[material.key])
    }

    @Test
    fun generatorShouldSupportProgramDemandEntryPoint() = runBlocking {
        val material = Material(
            no = MaterialNo("M-PROG-GEN"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-PROG-GEN",
            weight = FltX(1.0) * Kilogram
        )
        val candidate = MaterialPackingProgramCandidate(
            id = "candidate-gen",
            program = makeProgram(material),
            itemName = "gen-item"
        )
        val existingLayer = BinLayer(
            iteration = Int64.zero,
            from = LayerGenerationProgramCandidateAdaptersTest::class,
            shape = fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape(
                width = FltX(1.0) * Meter,
                height = FltX(1.0) * Meter,
                depth = FltX(1.0) * Meter
            ),
            units = emptyList()
        )
        val generated = HistoricalLayerGenerator<FltX>().generateFromProgramDemands(
            iteration = 4,
            items = emptyList(),
            programDemands = listOf(Pair(candidate, UInt64.one)),
            programMaterialCatalog = mapOf(material.key to material),
            existingLayers = listOf(existingLayer),
            maxCandidates = 4
        )

        assertNotNull(generated)
        assertEquals(1, generated.size)
        assertEquals("historical", generated.first().source)
    }
}
