package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.asContainer3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.binLayerPlacementOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.continuousCylinderRadiusSolverSource
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.continuousRadiusSolverPrototype
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.dump
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.itemPlacement3Of
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.withSolverSelectedRadius
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackedBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackedItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingAggregation
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingContext
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PackerAndRendererAdapterTest {
    private object CargoAttr : fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(
        id: String,
        material: Material<FltX>,
        widthValue: Double = 1.0,
        heightValue: Double = 1.0,
        depthValue: Double = 1.0
    ): ActualItem {
        val width = fltX(widthValue) * Meter
        val height = fltX(heightValue) * Meter
        val depth = fltX(depthValue) * Meter
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = width,
                height = height,
                depth = depth,
                weight = fltX(1.0) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = id,
            name = id,
            pack = pack,
            width = width,
            height = height,
            depth = depth,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun cylinderItem(
        id: String,
        material: Material<FltX>,
        axis: Axis3 = Axis3.Y,
        radiusValue: Double = 0.5,
        lengthValue: Double = 1.2,
        radiusWeightFunctionKey: String? = null
    ): ActualItem {
        val radius = fltX(radiusValue) * Meter
        val height = fltX(lengthValue) * Meter
        val cylinderWeight = fltX(1.0) * Kilogram
        val diameter = radius + radius
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = when (axis) {
                    Axis3.X -> height
                    Axis3.Y, Axis3.Z -> diameter
                },
                height = height,
                depth = when (axis) {
                    Axis3.Z -> height
                    Axis3.X, Axis3.Y -> diameter
                },
                weight = cylinderWeight,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = id,
            name = id,
            pack = pack,
            width = when (axis) {
                Axis3.X -> height
                Axis3.Y, Axis3.Z -> diameter
            },
            height = when (axis) {
                Axis3.X, Axis3.Z -> diameter
                Axis3.Y -> height
            },
            depth = when (axis) {
                Axis3.Z -> height
                Axis3.X, Axis3.Y -> diameter
            },
            weight = cylinderWeight,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis,
                radiusWeightFunctionKey = radiusWeightFunctionKey
            )
        )
    }

    private fun toPackingResult(bin: LayerBin): PackingResult {
        val packed = PackedBin(
            name = "bin-test",
            type = bin.type,
            items = bin.dump().units.map { placement ->
                PackedItem(
                    placement = placement,
                    loadingOrder = UInt64.zero
                )
            }
        )
        return PackingResult(
            aggregation = PackingAggregation(listOf(packed))
        )
    }

    private fun binType(): BinType<FltX> {
        return BinType(
            width = fltX(3.0) * Meter,
            height = fltX(3.0) * Meter,
            depth = fltX(3.0) * Meter,
            capacity = fltX(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-A"
        )
    }

    private fun layerBin(
        items: List<ActualItem>,
        positions: List<Pair<Double, Double>> = items.indices.map { index -> Pair(index.toDouble(), 0.0) }
    ): LayerBin {
        val binType = binType()
        val placements = items.mapIndexed { index, item ->
            val (x, z) = positions[index]
            itemPlacement3Of(
                view = item.view(Orientation.Upright),
                position = point3(
                    x = fltX(x) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(z) * Meter
                )
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = PackerAndRendererAdapterTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = placements
        )
        return layerBinOf(
            shape = binType,
            units = listOf(
                binLayerPlacementOf(
                    view = layer.view(Orientation.Upright)!!,
                    position = point3FltX()
                )
            )
        )
    }

    private fun multiLayerBin(layers: List<Pair<List<ActualItem>, Double>>): LayerBin {
        val binType = binType()
        val layerPlacements = layers.mapIndexed { index, (items, z) ->
            val layer = BinLayer(
                iteration = Int64(index.toLong()),
                from = PackerAndRendererAdapterTest::class,
                bin = binType,
                shape = Container3Shape(binType.asContainer3Shape()),
                units = items.map { item ->
                    itemPlacement3Of(
                        view = item.view(Orientation.Upright),
                        position = point3FltX()
                    )
                }
            )
            binLayerPlacementOf(
                view = layer.view(Orientation.Upright)!!,
                position = point3(
                    x = fltX(0.0) * Meter,
                    y = fltX(0.0) * Meter,
                    z = fltX(z) * Meter
                )
            )
        }
        return layerBinOf(
            shape = binType,
            units = layerPlacements
        )
    }

    @Test
    fun rendererAdapterShouldUseVariableNameWhenContinuousRadiusKeysAreShared() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-SHARED-RADIUS-KEY"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-SHARED-RADIUS-KEY",
            weight = fltX(0.5) * Kilogram
        )
        val itemA = cylinderItem(
            id = "cyl-shared-key-a",
            material = material,
            radiusWeightFunctionKey = "shared-radius-key"
        )
        val itemB = cylinderItem(
            id = "cyl-shared-key-b",
            material = material,
            radiusWeightFunctionKey = "shared-radius-key"
        )
        val result = toPackingResult(
            layerBin(
                items = listOf(itemA, itemB),
                positions = listOf(Pair(0.0, 0.0), Pair(1.0, 0.0))
            )
        )
        fun selectedRadius(item: ActualItem, radiusValue: Double) =
            assertNotNull(
                (item.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder).continuousRadiusSolverPrototype(
                    source = continuousCylinderRadiusSolverSource(item)
                )
            ).withSolverSelectedRadius(
                solverRadius = fltX(radiusValue) * Meter
            )

        val schema = PackingRendererAdapter().toSchema(
            result = result,
            continuousRadiusSelectionResults = listOf(
                selectedRadius(itemA, 0.3),
                selectedRadius(itemB, 0.4)
            )
        )
        val renderedByName = schema.loadingPlans.first().items.associateBy { it.name }

        assertEquals(0.3, assertNotNull(renderedByName[itemA.name]?.radius).toDouble(), 1e-10)
        assertEquals(0.4, assertNotNull(renderedByName[itemB.name]?.radius).toDouble(), 1e-10)
    }

    @Test
    fun packerShouldSummarizeMaterialsAndRendererShouldBuildSchema() = runBlocking {
        val material = Material(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-1",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(listOf(item("item-1", material), item("item-2", material)))

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext(info = mapOf("source" to "unit-test"))
        )
        assertEquals(1, result.materialSummary.size)
        assertEquals(UInt64(2), result.materialSummary.first().amount)

        val schema = PackingRendererAdapter().toSchema(result)
        assertEquals("1", schema.kpi["bin_count"])
        assertEquals("1", schema.kpi["material_count"])
        assertEquals(1, schema.loadingPlans.size)
        assertEquals(2, schema.loadingPlans.first().items.size)
        assertTrue(schema.loadingPlans.first().loadingRate > FltX.zero)
    }

    @Test
    fun packerShouldKeepLayerPlacementOffsetInDumpedItemPositions() = runBlocking {
        val material = Material(
            no = MaterialNo("M-LAYER-OFFSET"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-LAYER-OFFSET",
            weight = fltX(0.5) * Kilogram
        )
        val binType = binType()
        val firstLayer = BinLayer(
            iteration = Int64.zero,
            from = PackerAndRendererAdapterTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = listOf(
                itemPlacement3Of(
                    view = item("item-offset-1", material).view(Orientation.Upright),
                    position = point3FltX()
                )
            )
        )
        val secondLayer = BinLayer(
            iteration = Int64.one,
            from = PackerAndRendererAdapterTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = listOf(
                itemPlacement3Of(
                    view = item("item-offset-2", material).view(Orientation.Upright),
                    position = point3FltX()
                )
            )
        )
        val bin = layerBinOf(
            shape = binType,
            units = listOf(
                binLayerPlacementOf(
                    view = firstLayer.view(Orientation.Upright)!!,
                    position = point3FltX()
                ),
                binLayerPlacementOf(
                    view = secondLayer.view(Orientation.Upright)!!,
                    position = point3(
                        x = fltX(0.0) * Meter,
                        y = fltX(0.0) * Meter,
                        z = fltX(1.0) * Meter
                    )
                )
            )
        )

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext()
        )
        val items = PackingRendererAdapter().toSchema(result).loadingPlans.first().items

        assertEquals(2, items.size)
        assertEquals(0.0, items[0].z.toDouble(), 1e-9)
        assertEquals(1.0, items[1].z.toDouble(), 1e-9)
    }

    @Test
    fun rendererShouldEmitCylinderMetadataWhenItemProvidesExplicitPackingShape() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(listOf(cylinderItem("cyl-1", material)))

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext()
        )
        val schema = PackingRendererAdapter().toSchema(result)
        val item = schema.loadingPlans.first().items.first()

        assertEquals("Cylinder", item.shapeType.name)
        assertEquals("Cylinder", item.renderShapeType.name)
        assertEquals("VerticalCylinder", item.algorithmShapeType.name)
        assertEquals("Y", item.axis?.name)
        assertTrue(item.radius != null)
        assertTrue(item.diameter != null)
        assertTrue(item.actualVolume != null)
    }

    @Test
    fun rendererShouldUseActualVolumeForMixedCuboidAndCylinder() = runBlocking {
        val material = Material(
            no = MaterialNo("M-MIX"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-MIX",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(listOf(item("box-1", material), cylinderItem("cyl-1", material)))

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext()
        )
        val schema = PackingRendererAdapter().toSchema(result)
        val plan = schema.loadingPlans.first()

        assertEquals(2, plan.items.size)
        assertTrue(plan.items.any { it.shapeType.name == "Cuboid" })
        assertTrue(plan.items.any { it.shapeType.name == "Cylinder" })

        val itemVolumeSum = plan.items.fold(0.0) { acc, next ->
            acc + next.actualVolume!!.toDouble()
        }
        val planVolume = plan.volume.toDouble()
        val loadingRate = plan.loadingRate.toDouble()
        val expectedLoadingRate = itemVolumeSum / (3.0 * 3.0 * 3.0)

        assertTrue(abs(planVolume - itemVolumeSum) < 1e-9)
        assertTrue(abs(loadingRate - expectedLoadingRate) < 1e-9)
    }

    @Test
    fun packerShouldRejectPlacementOutsideBinByRealShapeGeometry() = runBlocking {
        val material = Material(
            no = MaterialNo("M-OUT"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-OUT",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(cylinderItem("cyl-out", material)),
            positions = listOf(Pair(2.2, 0.0))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            Packer().invoke(
                bins = listOf(bin),
                context = PackingContext()
            )
        }
        assertTrue(error.message?.contains("outside bin") == true)
        assertTrue(error.message?.contains("type=outside_bin") == true)
        assertTrue(error.message?.contains("item[0]") == true)
    }

    @Test
    fun packerShouldRejectCuboidAndCylinderFootprintOverlap() = runBlocking {
        val material = Material(
            no = MaterialNo("M-OVERLAP"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-OVERLAP",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(item("box-overlap", material), cylinderItem("cyl-overlap", material)),
            positions = listOf(Pair(0.0, 0.0), Pair(0.25, 0.0))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            Packer().invoke(
                bins = listOf(bin),
                context = PackingContext()
            )
        }
        assertTrue(error.message?.contains("overlaps") == true)
        assertTrue(error.message?.contains("type=overlap") == true)
        assertTrue(error.message?.contains("bin=bin-1") == true)
    }

    @Test
    fun packerShouldAcceptCuboidAndCylinderBoundaryTangent() = runBlocking {
        val material = Material(
            no = MaterialNo("M-TANGENT"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-TANGENT",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(item("box-tangent", material), cylinderItem("cyl-tangent", material)),
            positions = listOf(Pair(0.0, 0.0), Pair(1.0, 0.0))
        )

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext()
        )

        assertEquals(2, result.aggregation.bins.first().items.size)
    }

    @Test
    fun rendererAdapterShouldRejectManuallyBuiltOverlappedPackingResult() = runBlocking {
        val material = Material(
            no = MaterialNo("M-RENDER-OVERLAP"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-RENDER-OVERLAP",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(item("box-render-overlap", material), cylinderItem("cyl-render-overlap", material)),
            positions = listOf(Pair(0.0, 0.0), Pair(0.25, 0.0))
        )
        val result = toPackingResult(bin)

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(result)
        }
        assertTrue(error.message?.contains("overlaps") == true)
        assertTrue(error.message?.contains("type=overlap") == true)
    }

    @Test
    fun rendererAdapterShouldRejectManuallyBuiltOutsideBinHorizontalCylinder() = runBlocking {
        val material = Material(
            no = MaterialNo("M-RENDER-OUTSIDE-HORIZONTAL"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-RENDER-OUTSIDE-HORIZONTAL",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(cylinderItem("cyl-x-render-outside", material, Axis3.X)),
            positions = listOf(Pair(2.2, 0.0))
        )
        val result = toPackingResult(bin)

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(result)
        }
        assertTrue(error.message?.contains("type=outside_bin") == true)
        assertTrue(error.message?.contains("HorizontalCylinderX") == true)
        assertTrue(error.message?.contains("PackingRendererAdapter.toSchema") == true)
    }

    @Test
    fun packerAndRendererShouldAcceptHorizontalCylinderFinalGeometry() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-X"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-X",
            weight = fltX(0.5) * Kilogram
        )
        val bin = multiLayerBin(
            layers = listOf(
                Pair(listOf(cylinderItem("cyl-x-1", material, Axis3.X)), 0.0),
                Pair(listOf(cylinderItem("cyl-z-1", material, Axis3.Z)), 1.5)
            )
        )

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext()
        )
        val items = PackingRendererAdapter().toSchema(result).loadingPlans.first().items

        assertEquals(2, items.size)
        assertTrue(items.any { it.axis?.name == "X" && it.algorithmShapeType.name == "HorizontalCylinderX" })
        assertTrue(items.any { it.axis?.name == "Z" && it.algorithmShapeType.name == "HorizontalCylinderZ" })
    }

    @Test
    fun packerAndRendererShouldAcceptKnownCoordinateHorizontalCylindersAcrossBins() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-MULTI-BIN"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-MULTI-BIN",
            weight = fltX(0.5) * Kilogram
        )
        val binX = layerBin(
            items = listOf(cylinderItem("cyl-x-bin", material, Axis3.X)),
            positions = listOf(Pair(0.0, 0.0))
        )
        val binZ = layerBin(
            items = listOf(cylinderItem("cyl-z-bin", material, Axis3.Z)),
            positions = listOf(Pair(0.0, 0.0))
        )

        val result = Packer().invoke(
            bins = listOf(binX, binZ),
            context = PackingContext()
        )
        val schema = PackingRendererAdapter().toSchema(result)
        val items = schema.loadingPlans.flatMap { it.items }

        assertEquals(2, schema.loadingPlans.size)
        assertTrue(items.any { it.axis?.name == "X" && it.algorithmShapeType.name == "HorizontalCylinderX" })
        assertTrue(items.any { it.axis?.name == "Z" && it.algorithmShapeType.name == "HorizontalCylinderZ" })
    }

    @Test
    fun packerShouldRejectMixedCylinderAxesWithinSameLayer() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-MIXED-AXIS"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-MIXED-AXIS",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(cylinderItem("cyl-x-mixed", material, Axis3.X), cylinderItem("cyl-z-mixed", material, Axis3.Z)),
            positions = listOf(Pair(0.0, 0.0), Pair(2.0, 0.0))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            Packer().invoke(
                bins = listOf(bin),
                context = PackingContext()
            )
        }
        assertTrue(error.message?.contains("mixes cylinder axes") == true)
    }

    @Test
    fun packerShouldRejectHorizontalCylinderAndCuboidRealGeometryOverlap() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-H-OVERLAP"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-H-OVERLAP",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(item("box-horizontal-overlap", material), cylinderItem("cyl-x-overlap", material, Axis3.X)),
            positions = listOf(Pair(0.0, 0.0), Pair(0.0, 0.0))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            Packer().invoke(
                bins = listOf(bin),
                context = PackingContext()
            )
        }
        assertTrue(error.message?.contains("overlaps") == true)
    }

    @Test
    fun rendererAdapterShouldRejectManuallyBuiltOverlappedHorizontalAndVerticalCylinders() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-HV-OVERLAP"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-HV-OVERLAP",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(cylinderItem("cyl-x-render-overlap", material, Axis3.X), cylinderItem("cyl-y-render-overlap", material)),
            positions = listOf(Pair(0.0, 0.0), Pair(0.0, 0.0))
        )
        val result = toPackingResult(bin)

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(result)
        }
        assertTrue(error.message?.contains("overlaps") == true)
    }

    @Test
    fun rendererAdapterShouldRejectManuallyBuiltOverlappedHorizontalCylindersWithDifferentAxes() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-XZ-OVERLAP"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-XZ-OVERLAP",
            weight = fltX(0.5) * Kilogram
        )
        val bin = layerBin(
            items = listOf(cylinderItem("cyl-x-render-overlap", material, Axis3.X), cylinderItem("cyl-z-render-overlap", material, Axis3.Z)),
            positions = listOf(Pair(0.0, 0.0), Pair(0.0, 0.0))
        )
        val result = toPackingResult(bin)

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(result)
        }
        assertTrue(error.message?.contains("overlaps") == true)
    }

    @Test
    fun packerShouldRejectSuspendedHorizontalCylinder() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-H-SUSPENDED"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-H-SUSPENDED",
            weight = fltX(0.5) * Kilogram
        )
        val binType = binType()
        val layer = BinLayer(
            iteration = Int64.zero,
            from = PackerAndRendererAdapterTest::class,
            bin = binType,
            shape = Container3Shape(binType.asContainer3Shape()),
            units = listOf(
                itemPlacement3Of(
                    view = cylinderItem("cyl-x-suspended", material, Axis3.X).view(Orientation.Upright),
                    position = point3(
                        x = fltX(0.0) * Meter,
                        y = fltX(0.2) * Meter,
                        z = fltX(0.0) * Meter
                    )
                )
            )
        )
        val bin = layerBinOf(
            shape = binType,
            units = listOf(
                binLayerPlacementOf(
                    view = layer.view(Orientation.Upright)!!,
                    position = point3FltX()
                )
            )
        )

        val error = assertFailsWith<IllegalArgumentException> {
            Packer().invoke(
                bins = listOf(bin),
                context = PackingContext()
            )
        }
        assertTrue(error.message?.contains("must be placed on bin floor") == true)
        assertTrue(error.message?.contains("type=horizontal_support") == true)
    }

    @Test
    fun rendererAdapterShouldAcceptHorizontalCylinderWithFullLengthCuboidSupport() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-H-SUPPORTED"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-H-SUPPORTED",
            weight = fltX(0.5) * Kilogram
        )
        val result = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = item("support-full", material).view(Orientation.Upright),
                                    position = point3FltX()
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem(
                                        id = "cyl-x-supported",
                                        material = material,
                                        axis = Axis3.X,
                                        lengthValue = 1.0
                                    ).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(1.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            )
                        )
                    )
                )
            )
        )

        val items = PackingRendererAdapter().toSchema(result).loadingPlans.first().items

        assertEquals(2, items.size)
        assertTrue(items.any { it.axis?.name == "X" && it.algorithmShapeType.name == "HorizontalCylinderX" })
    }

    @Test
    fun rendererAdapterShouldAcceptZAxisHorizontalCylinderWithFullLengthCuboidSupport() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-Z-SUPPORTED"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-Z-SUPPORTED",
            weight = fltX(0.5) * Kilogram
        )
        val result = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = item("support-z-full", material).view(Orientation.Upright),
                                    position = point3FltX()
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem(
                                        id = "cyl-z-supported",
                                        material = material,
                                        axis = Axis3.Z,
                                        lengthValue = 1.0
                                    ).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(1.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            )
                        )
                    )
                )
            )
        )

        val items = PackingRendererAdapter().toSchema(result).loadingPlans.first().items

        assertEquals(2, items.size)
        assertTrue(items.any { it.axis?.name == "Z" && it.algorithmShapeType.name == "HorizontalCylinderZ" })
    }

    @Test
    fun rendererAdapterShouldAcceptHorizontalCylinderWithMultipleCuboidSupportIntervals() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-H-MULTI-SUPPORT"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-H-MULTI-SUPPORT",
            weight = fltX(0.5) * Kilogram
        )
        val result = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = item(
                                        id = "support-interval-a",
                                        material = material,
                                        widthValue = 0.5,
                                        heightValue = 1.0,
                                        depthValue = 1.0
                                    ).view(Orientation.Upright),
                                    position = point3FltX()
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = item(
                                        id = "support-interval-b",
                                        material = material,
                                        widthValue = 0.7,
                                        heightValue = 1.0,
                                        depthValue = 1.0
                                    ).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.5) * Meter,
                                        y = fltX(0.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem(
                                        id = "cyl-x-multi-supported",
                                        material = material,
                                        axis = Axis3.X,
                                        lengthValue = 1.2
                                    ).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(1.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(3)
                            )
                        )
                    )
                )
            )
        )

        val items = PackingRendererAdapter().toSchema(result).loadingPlans.first().items

        assertEquals(3, items.size)
        assertTrue(items.any { it.axis?.name == "X" && it.algorithmShapeType.name == "HorizontalCylinderX" })
    }

    @Test
    fun rendererAdapterShouldRejectHorizontalCylinderWithPartialCuboidSupport() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-H-PARTIAL-SUPPORT"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-H-PARTIAL-SUPPORT",
            weight = fltX(0.5) * Kilogram
        )
        val result = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = item("support-partial", material).view(Orientation.Upright),
                                    position = point3FltX()
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem(
                                        id = "cyl-x-partial-support",
                                        material = material,
                                        axis = Axis3.X,
                                        lengthValue = 1.2
                                    ).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(1.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            )
                        )
                    )
                )
            )
        )

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(result)
        }
        assertTrue(error.message?.contains("type=horizontal_support") == true)
        assertTrue(error.message?.contains("cuboid support coverage") == true)
    }

    @Test
    fun rendererAdapterShouldRejectHorizontalCylinderWithMisalignedSupportLine() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-H-SUPPORT-LINE"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-H-SUPPORT-LINE",
            weight = fltX(0.5) * Kilogram
        )
        val result = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = item(
                                        id = "support-line-mismatch",
                                        material = material,
                                        widthValue = 1.2,
                                        heightValue = 1.0,
                                        depthValue = 0.2
                                    ).view(Orientation.Upright),
                                    position = point3FltX()
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem(
                                        id = "cyl-x-support-line-mismatch",
                                        material = material,
                                        axis = Axis3.X,
                                        lengthValue = 1.2
                                    ).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(1.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            )
                        )
                    )
                )
            )
        )

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(result)
        }
        assertTrue(error.message?.contains("type=horizontal_support") == true)
        assertTrue(error.message?.contains("cuboid support coverage") == true)
    }

    @Test
    fun rendererAdapterShouldAcceptSameAxisCylinderBoundaryTangentAndRejectTinyOverlap() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-SAME-AXIS"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-SAME-AXIS",
            weight = fltX(0.5) * Kilogram
        )
        val tangentResult = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem("cyl-y-tangent-1", material, radiusValue = 0.4).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(0.0) * Meter,
                                        z = fltX(0.2) * Meter
                                    )
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem("cyl-y-tangent-2", material, radiusValue = 0.6, lengthValue = 1.8).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.8) * Meter,
                                        y = fltX(0.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            )
                        )
                    )
                )
            )
        )

        val tangentSchema = PackingRendererAdapter().toSchema(tangentResult)
        assertEquals(2, tangentSchema.loadingPlans.first().items.size)

        val overlapResult = PackingResult(
            aggregation = PackingAggregation(
                listOf(
                    PackedBin(
                        name = "bin-test",
                        type = binType(),
                        items = listOf(
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem("cyl-y-overlap-1", material, radiusValue = 0.4).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.0) * Meter,
                                        y = fltX(0.0) * Meter,
                                        z = fltX(0.2) * Meter
                                    )
                                ),
                                loadingOrder = UInt64.one
                            ),
                            PackedItem(
                                placement = itemPlacement3Of(
                                    view = cylinderItem("cyl-y-overlap-2", material, radiusValue = 0.6, lengthValue = 1.8).view(Orientation.Upright),
                                    position = point3(
                                        x = fltX(0.799) * Meter,
                                        y = fltX(0.0) * Meter,
                                        z = fltX(0.0) * Meter
                                    )
                                ),
                                loadingOrder = UInt64(2)
                            )
                        )
                    )
                )
            )
        )

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(overlapResult)
        }
        assertTrue(error.message?.contains("type=overlap") == true)
        assertTrue(error.message?.contains("VerticalCylinder") == true)
    }

    @Test
    fun rendererAdapterShouldAcceptAndRejectDifferentAxisCylinderCriticalGeometry() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-DIFF-AXIS"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-DIFF-AXIS",
            weight = fltX(0.5) * Kilogram
        )
        val separatedBin = layerBin(
            items = listOf(
                cylinderItem("cyl-x-separated", material, axis = Axis3.X),
                cylinderItem("cyl-y-separated", material, axis = Axis3.Y)
            ),
            positions = listOf(Pair(0.0, 0.0), Pair(0.0, 1.1))
        )
        val separatedResult = toPackingResult(separatedBin)
        val separatedSchema = PackingRendererAdapter().toSchema(separatedResult)

        assertEquals(2, separatedSchema.loadingPlans.first().items.size)

        val crossingBin = layerBin(
            items = listOf(
                cylinderItem("cyl-x-crossing", material, axis = Axis3.X),
                cylinderItem("cyl-y-crossing", material, axis = Axis3.Y)
            ),
            positions = listOf(Pair(0.0, 0.0), Pair(0.0, 0.25))
        )
        val crossingResult = toPackingResult(crossingBin)

        val error = assertFailsWith<IllegalArgumentException> {
            PackingRendererAdapter().toSchema(crossingResult)
        }
        assertTrue(error.message?.contains("type=overlap") == true)
        assertTrue(error.message?.contains("HorizontalCylinderX") == true)
        assertTrue(error.message?.contains("VerticalCylinder") == true)
    }

    @Test
    fun packerShouldUseRealCylinderBoxCornerGeometryAtBoundary() = runBlocking {
        val material = Material(
            no = MaterialNo("M-CYL-BOX-CORNER"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-CYL-BOX-CORNER",
            weight = fltX(0.5) * Kilogram
        )
        val tangentBin = layerBin(
            items = listOf(item("box-corner-tangent", material), cylinderItem("cyl-corner-tangent", material)),
            positions = listOf(Pair(0.0, 0.0), Pair(1.0 - 0.5 + (0.5 / sqrt(2.0)), 1.0 - 0.5 + (0.5 / sqrt(2.0))))
        )

        val tangentResult = Packer().invoke(
            bins = listOf(tangentBin),
            context = PackingContext()
        )
        assertEquals(2, tangentResult.aggregation.bins.first().items.size)

        val overlapBin = layerBin(
            items = listOf(item("box-corner-overlap", material), cylinderItem("cyl-corner-overlap", material)),
            positions = listOf(Pair(0.0, 0.0), Pair(0.75, 0.75))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            Packer().invoke(
                bins = listOf(overlapBin),
                context = PackingContext()
            )
        }
        assertTrue(error.message?.contains("type=overlap") == true)
        assertTrue(error.message?.contains("Cuboid") == true)
        assertTrue(error.message?.contains("VerticalCylinder") == true)
    }
}
