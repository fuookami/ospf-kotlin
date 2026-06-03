package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderAlgorithmShapeTypeDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderAxis3DTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanItemDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderShapeTypeDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO

class RendererDTOTest {
    @Test
    fun cuboidItemDtoShouldRoundTripWithBackwardCompatibleDefaults() {
        val item = RenderLoadingPlanItemDTO(
            name = "cuboid-1",
            packageType = "CartonContainer",
            width = FltX(1.0),
            height = FltX(2.0),
            depth = FltX(3.0),
            x = FltX.zero,
            y = FltX.zero,
            z = FltX.zero,
            weight = FltX(4.0),
            loadingOrder = UInt64.one
        )

        val encoded = Json.encodeToString(RenderLoadingPlanItemDTO.serializer(), item)
        val decoded = Json.decodeFromString(RenderLoadingPlanItemDTO.serializer(), encoded)

        assertEquals(RenderShapeTypeDTO.Cuboid, decoded.shapeType)
        assertEquals(RenderShapeTypeDTO.Cuboid, decoded.renderShapeType)
        assertEquals(RenderAlgorithmShapeTypeDTO.Cuboid, decoded.algorithmShapeType)
        assertNull(decoded.radius)
        assertNull(decoded.diameter)
        assertNull(decoded.axis)
        assertNull(decoded.actualVolume)
    }

    @Test
    fun cylinderSchemaDtoShouldRoundTripWithShapeMetadata() {
        val item = RenderLoadingPlanItemDTO(
            name = "cylinder-1",
            packageType = "CartonContainer",
            width = FltX(1.0),
            height = FltX(1.2),
            depth = FltX(1.0),
            x = FltX.zero,
            y = FltX.zero,
            z = FltX.zero,
            weight = FltX(1.0),
            loadingOrder = UInt64.one,
            shapeType = RenderShapeTypeDTO.Cylinder,
            renderShapeType = RenderShapeTypeDTO.Cylinder,
            algorithmShapeType = RenderAlgorithmShapeTypeDTO.VerticalCylinder,
            radius = FltX(0.5),
            diameter = FltX(1.0),
            axis = RenderAxis3DTO.Y,
            boundingWidth = FltX(1.0),
            boundingHeight = FltX(1.2),
            boundingDepth = FltX(1.0),
            actualVolume = FltX(0.942477796)
        )
        val schema = SchemaDTO(
            kpi = mapOf("bin_count" to "1"),
            loadingPlans = listOf(
                RenderLoadingPlanDTO(
                    name = "bin-1",
                    typeCode = "BIN-A",
                    width = FltX(3.0),
                    height = FltX(3.0),
                    depth = FltX(3.0),
                    loadingRate = FltX(0.1),
                    weight = FltX(1.0),
                    volume = FltX(0.942477796),
                    items = listOf(item)
                )
            )
        )

        val encoded = Json.encodeToString(SchemaDTO.serializer(), schema)
        val decoded = Json.decodeFromString(SchemaDTO.serializer(), encoded)
        val decodedItem = decoded.loadingPlans.first().items.first()

        assertEquals(RenderShapeTypeDTO.Cylinder, decodedItem.shapeType)
        assertEquals(RenderShapeTypeDTO.Cylinder, decodedItem.renderShapeType)
        assertEquals(RenderAlgorithmShapeTypeDTO.VerticalCylinder, decodedItem.algorithmShapeType)
        assertEquals(RenderAxis3DTO.Y, decodedItem.axis)
        assertEquals(FltX(0.5), decodedItem.radius)
        assertEquals(FltX(1.0), decodedItem.diameter)
        assertEquals(FltX(0.942477796), decodedItem.actualVolume)
    }

    @Test
    fun cylinderDtoShouldRoundTripHorizontalAxisMetadata() {
        val items = listOf(
            RenderLoadingPlanItemDTO(
                name = "cylinder-x-1",
                packageType = "CartonContainer",
                width = FltX(5.0),
                height = FltX(1.0),
                depth = FltX(1.0),
                x = FltX.zero,
                y = FltX.zero,
                z = FltX.zero,
                weight = FltX(1.0),
                loadingOrder = UInt64.one,
                shapeType = RenderShapeTypeDTO.Cylinder,
                renderShapeType = RenderShapeTypeDTO.Cylinder,
                algorithmShapeType = RenderAlgorithmShapeTypeDTO.BoundingCuboid,
                radius = FltX(0.5),
                diameter = FltX(1.0),
                axis = RenderAxis3DTO.X,
                boundingWidth = FltX(5.0),
                boundingHeight = FltX(1.0),
                boundingDepth = FltX(1.0),
                actualVolume = FltX(3.9269908169872414)
            ),
            RenderLoadingPlanItemDTO(
                name = "cylinder-z-1",
                packageType = "CartonContainer",
                width = FltX(1.0),
                height = FltX(1.0),
                depth = FltX(5.0),
                x = FltX.zero,
                y = FltX.zero,
                z = FltX.zero,
                weight = FltX(1.0),
                loadingOrder = UInt64(2),
                shapeType = RenderShapeTypeDTO.Cylinder,
                renderShapeType = RenderShapeTypeDTO.Cylinder,
                algorithmShapeType = RenderAlgorithmShapeTypeDTO.BoundingCuboid,
                radius = FltX(0.5),
                diameter = FltX(1.0),
                axis = RenderAxis3DTO.Z,
                boundingWidth = FltX(1.0),
                boundingHeight = FltX(1.0),
                boundingDepth = FltX(5.0),
                actualVolume = FltX(3.9269908169872414)
            )
        )
        val encoded = Json.encodeToString(ListSerializer(RenderLoadingPlanItemDTO.serializer()), items)
        val decoded = Json.decodeFromString(ListSerializer(RenderLoadingPlanItemDTO.serializer()), encoded)

        assertEquals(RenderAxis3DTO.X, decoded[0].axis)
        assertEquals(RenderAxis3DTO.Z, decoded[1].axis)
        decoded.forEach { item ->
            assertEquals(RenderShapeTypeDTO.Cylinder, item.shapeType)
            assertEquals(RenderShapeTypeDTO.Cylinder, item.renderShapeType)
            assertEquals(RenderAlgorithmShapeTypeDTO.BoundingCuboid, item.algorithmShapeType)
            assertEquals(FltX(0.5), item.radius)
            assertEquals(FltX(1.0), item.diameter)
            assertTrue(item.actualVolume!!.toDouble() > 0.0)
        }
    }

    @Test
    fun mixedShapeRendererFixtureShouldMatchExternalRendererContract() {
        val resource = requireNotNull(
            javaClass.classLoader.getResource("renderer/mixed-shape-renderer-schema.json")
        )
        val fixture = resource.openStream().bufferedReader().use { reader -> reader.readText() }
        val schema = Json.decodeFromString(SchemaDTO.serializer(), fixture)
        val plan = schema.loadingPlans.first()
        val box = plan.items.first { item -> item.name == "box-1" }
        val cylinder = plan.items.first { item -> item.name == "cyl-y-1" }

        assertEquals("mixed-shape-renderer-schema", schema.kpi["fixture"])
        assertEquals("BIN-A", plan.typeCode)
        assertEquals(2, plan.items.size)
        assertTrue(plan.loadingRate.toDouble() > 0.0)

        assertEquals(RenderShapeTypeDTO.Cuboid, box.shapeType)
        assertEquals(RenderShapeTypeDTO.Cuboid, box.renderShapeType)
        assertEquals(RenderAlgorithmShapeTypeDTO.Cuboid, box.algorithmShapeType)
        assertEquals(FltX(1.0), box.boundingWidth)
        assertEquals(FltX(1.0), box.actualVolume)
        assertNull(box.radius)
        assertNull(box.axis)

        assertEquals(RenderShapeTypeDTO.Cylinder, cylinder.shapeType)
        assertEquals(RenderShapeTypeDTO.Cylinder, cylinder.renderShapeType)
        assertEquals(RenderAlgorithmShapeTypeDTO.VerticalCylinder, cylinder.algorithmShapeType)
        assertEquals(RenderAxis3DTO.Y, cylinder.axis)
        assertEquals(FltX(0.5), cylinder.radius)
        assertEquals(FltX(1.0), cylinder.diameter)
        assertEquals(FltX(1.0), cylinder.boundingWidth)
        assertEquals(FltX(1.2), cylinder.boundingHeight)
        assertEquals(FltX(1.0), cylinder.boundingDepth)
        assertTrue(cylinder.actualVolume!!.toDouble() > 0.0)
    }
}
