package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.assertEquals
import kotlin.test.assertNull
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
}
