/**
 * Gurobi 列生成测试。
 * Gurobi column generation test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64 as SolverFlt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*

@EnabledIfSystemProperty(named = "bpp3d.gurobi.cg.test.enabled", matches = "true")
class GurobiColumnGenerationTest {
    private object CargoAttr : fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute

    /** 按选择器求和 Int 值 / Sum Int values by selector */
    private fun <T> Iterable<T>.sumOfInt(selector: (T) -> Int): Int {
        return fold(0) { acc, item -> acc + selector(item) }
    }

    /** 解包 Ret 值，失败时抛出异常 / Unwrap Ret value, throw on failure */
    private fun <T> Ret<T>.orFail(message: String): T {
        return value ?: fail(failureMessage()?.let { "$message: $it" } ?: message)
    }

    /** 解包 Ret 值为非法参数异常 / Unwrap Ret value as illegal argument exception */
    private fun <T> Ret<T>.orIllegalArgument(message: String): T {
        return value ?: throw IllegalArgumentException(failureMessage()?.let { "$message: $it" } ?: message)
    }

    /** 获取失败消息 / Get failure message */
    private fun <T> Ret<T>.failureMessage(): String? {
        return when (this) {
            is Failed -> message
            is Fatal -> firstError?.message
            else -> null
        }
    }

    /** 按选择器求和 Long 值 / Sum Long values by selector */
    private fun <T> Iterable<T>.sumOfLong(selector: (T) -> Long): Long {
        return fold(0L) { acc, item -> acc + selector(item) }
    }

    /** 创建默认包装属性 / Create default package attribute */
    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    /** 创建测试用长方体货物 / Create test cuboid item */
    private fun item(
        id: String,
        material: Material<FltX>,
        widthInMeter: FltX = FltX.one,
        heightInMeter: FltX = FltX.one,
        depthInMeter: FltX = FltX.one,
        shapeSpec: PackageShapeSpec = PackageShapeSpec.Cuboid
    ): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = widthInMeter * Meter,
                height = heightInMeter * Meter,
                depth = depthInMeter * Meter,
                weight = FltX.one * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = shapeSpec
            ),
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = itemIdOf(id),
            name = id,
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    /** 创建测试用水平圆柱货物 / Create test horizontal cylinder item */
    private fun horizontalCylinderItem(
        id: String,
        material: Material<FltX>,
        axis: Axis3,
        radiusInMeter: FltX = FltX(0.4),
        lengthInMeter: FltX = FltX.one,
        shapeSpec: PackageShapeSpec.VerticalCylinder = PackageShapeSpec.VerticalCylinder(
            radius = radiusInMeter * Meter,
            axis = axis
        )
    ): ActualItem {
        val diameter = radiusInMeter * FltX(2.0)
        val shape = when (axis) {
            Axis3.X -> PackageShape(
                width = lengthInMeter * Meter,
                height = diameter * Meter,
                depth = diameter * Meter,
                weight = FltX.one * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = shapeSpec
            )

            Axis3.Y -> PackageShape(
                width = diameter * Meter,
                height = lengthInMeter * Meter,
                depth = diameter * Meter,
                weight = FltX.one * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = shapeSpec
            )

            Axis3.Z -> PackageShape(
                width = diameter * Meter,
                height = diameter * Meter,
                depth = lengthInMeter * Meter,
                weight = FltX.one * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = shapeSpec
            )
        }
        val pack = Package.innerPackage(
            shape = shape,
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = itemIdOf(id),
            name = id,
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun itemFromCsvWidth(
        id: String,
        material: Material<FltX>,
        widthInMeter: FltX,
        shapeSpec: PackageShapeSpec
    ): ActualItem {
        val cylinderSpec = shapeSpec as? PackageShapeSpec.VerticalCylinder
        return if (cylinderSpec != null && cylinderSpec.axis != Axis3.Y) {
            horizontalCylinderItem(
                id = id,
                material = material,
                axis = cylinderSpec.axis,
                radiusInMeter = cylinderSpec.radius.value,
                lengthInMeter = widthInMeter,
                shapeSpec = cylinderSpec
            )
        } else {
            item(
                id = id,
                material = material,
                widthInMeter = widthInMeter,
                shapeSpec = shapeSpec
            )
        }
    }

    private fun itemFromCsvDimensions(
        id: String,
        material: Material<FltX>,
        widthInMeter: FltX,
        heightInMeter: FltX,
        depthInMeter: FltX,
        shapeSpec: PackageShapeSpec
    ): ActualItem {
        val cylinderSpec = shapeSpec as? PackageShapeSpec.VerticalCylinder
        return if (cylinderSpec != null && cylinderSpec.axis != Axis3.Y) {
            val axisLength = when (cylinderSpec.axis) {
                Axis3.X -> widthInMeter
                Axis3.Y -> heightInMeter
                Axis3.Z -> depthInMeter
            }
            horizontalCylinderItem(
                id = id,
                material = material,
                axis = cylinderSpec.axis,
                radiusInMeter = cylinderSpec.radius.value,
                lengthInMeter = axisLength,
                shapeSpec = cylinderSpec
            )
        } else {
            item(
                id = id,
                material = material,
                widthInMeter = widthInMeter,
                heightInMeter = heightInMeter,
                depthInMeter = depthInMeter,
                shapeSpec = shapeSpec
            )
        }
    }

    private fun horizontalAxisOf(item: Item): Axis3? {
        val shape = item.packingShape as? CylinderPackingShape3 ?: return null
        return shape.axis.takeIf { axis -> axis != Axis3.Y }
    }

    private fun generatedHorizontalSeedLayer(
        item: ActualItem,
        binType: BinType<FltX>
    ): BinLayer? {
        val axis = horizontalAxisOf(item) ?: return null
        val source = "circle-packing-horizontal-grid-single-axis=${axis.name.lowercase()}"
        return runBlocking {
            CirclePackingLayerGenerator<FltX>().generate(
                Bpp3dLayerGenerationRequest(
                    iteration = 0,
                    bin = binType,
                    items = listOf(item),
                    maxCandidates = 16
                )
            ).firstOrNull { result -> result.source == source }?.layer
        }
    }

    private fun generatedHorizontalGridLayer(
        item: ActualItem,
        binType: BinType<FltX>
    ): BinLayer {
        val axis = horizontalAxisOf(item)
            ?: throw IllegalArgumentException("item must be a horizontal cylinder")
        val source = "circle-packing-horizontal-grid-axis=${axis.name.lowercase()}"
        return runBlocking {
            CirclePackingLayerGenerator<FltX>().generate(
                Bpp3dLayerGenerationRequest(
                    iteration = 0,
                    bin = binType,
                    items = listOf(item),
                    maxCandidates = 16
                )
            ).firstOrNull { result -> result.source == source }?.layer
                ?: throw IllegalStateException("missing generated horizontal grid layer: $source")
        }
    }

    private fun generatedHorizontalLayerBySource(
        items: List<ActualItem>,
        binType: BinType<FltX>,
        source: String
    ): BinLayer {
        return runBlocking {
            CirclePackingLayerGenerator<FltX>().generate(
                Bpp3dLayerGenerationRequest(
                    iteration = 0,
                    bin = binType,
                    items = items,
                    maxCandidates = 32
                )
            ).firstOrNull { result -> result.source == source }?.layer
                ?: throw IllegalStateException("missing generated horizontal layer: $source")
        }
    }

    private fun generatedHorizontalSupportedStackLayer(
        items: List<ActualItem>,
        binType: BinType<FltX>
    ): BinLayer? {
        val axis = items.mapNotNull { item -> horizontalAxisOf(item) }
            .singleOrNull()
            ?: return null
        val sources = listOf(
            "circle-packing-horizontal-hanging-support-multi-axis=${axis.name.lowercase()}",
            "circle-packing-horizontal-hanging-support-axis=${axis.name.lowercase()}",
            "circle-packing-horizontal-supported-stack-heterogeneous-axis=${axis.name.lowercase()}",
            "circle-packing-horizontal-supported-stack-multi-axis=${axis.name.lowercase()}",
            "circle-packing-horizontal-supported-stack-axis=${axis.name.lowercase()}"
        )
        return runBlocking {
            val generated = CirclePackingLayerGenerator<FltX>().generate(
                Bpp3dLayerGenerationRequest(
                    iteration = 0,
                    bin = binType,
                    items = items,
                    maxCandidates = 64
                )
            )
            sources.firstNotNullOfOrNull { source ->
                generated.firstOrNull { result -> result.source == source }?.layer
            }
        }
    }

    private fun layerBin(
        items: List<ActualItem>,
        typeCode: String = "BIN-GUROBI",
        depthInMeter: FltX = FltX(3.0),
        binType: BinType<FltX>? = null,
        widthInMeter: FltX = FltX(3.0)
    ): Bin<BinLayer, FltX> {
        val resolvedBinType = binType ?: BinType(
            width = widthInMeter * Meter,
            height = FltX(3.0) * Meter,
            depth = depthInMeter * Meter,
            capacity = FltX(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = typeCode
        )
        val placements = items.mapIndexed { index, item ->
            item.toItemPlacement(
                x = FltX(index.toLong()) * Meter
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = GurobiColumnGenerationTest::class,
            bin = resolvedBinType,
            shape = resolvedBinType.asContainer3Shape(),
            units = placements
        )
        return layerBinOf(
            shape = resolvedBinType,
            units = listOf(
                layer.toLayerPlacement().orIllegalArgument("layer placement should be built")
            )
        )
    }

    private data class CsvScenarioRow(
        val groupIndex: Int,
        val layerIndex: Int,
        val itemId: String,
        val materialNo: String,
        val materialName: String,
        val materialWeightKg: FltX,
        val shapeType: String?,
        val radiusMeter: FltX?,
        val radiusMinMeter: FltX?,
        val radiusMaxMeter: FltX?,
        val radiusStepMeter: FltX?,
        val diameterMinMeter: FltX?,
        val diameterMaxMeter: FltX?,
        val diameterStepMeter: FltX?,
        val radiusWeightFunctionKey: String?,
        val axis: String?,
        val widthMeter: FltX?,
        val heightMeter: FltX?,
        val depthMeter: FltX?
    )

    private data class MaterialWidthAmountScenarioRow(
        val material: String,
        val width: FltX,
        val amount: UInt64,
        val materialNo: String?,
        val materialName: String?,
        val materialWeightKg: FltX?,
        val shapeType: String?,
        val radiusMeter: FltX?,
        val radiusMinMeter: FltX?,
        val radiusMaxMeter: FltX?,
        val radiusStepMeter: FltX?,
        val diameterMinMeter: FltX?,
        val diameterMaxMeter: FltX?,
        val diameterStepMeter: FltX?,
        val radiusWeightFunctionKey: String?,
        val axis: String?
    )

    private data class CsvDrivenScenario(
        val itemDemands: List<Pair<ActualItem, UInt64>>,
        val demandEntries: List<Bpp3dDemandEntry<FltX>>,
        val initialColumns: List<BinLayer>,
        val finalBins: List<Bin<BinLayer, FltX>>,
        val materialAmountDemands: Map<Material<FltX>, UInt64>,
        val groupCount: Int,
        val materialCount: Int,
        val totalLayerCount: Int,
        val totalItemCount: Int,
        val packedLayerCount: Int = totalLayerCount,
        val depthBoundaryLayerOrientationPolicy: DepthBoundaryLayerOrientationPolicy? = null
    )

    private data class CsvDrivenScenarioCase(
        val name: String,
        val scenario: CsvDrivenScenario
    )

    private data class CsvSchemaPrecheckReport(
        val sourceName: String,
        val declaredScenarioKind: CsvScenarioKind?,
        val detectedScenarioKind: CsvScenarioKind,
        val headerColumns: List<String>
    )

    private enum class CsvScenarioKind {
        GroupedLayer,
        MaterialWidthAmount
    }

    private fun buildSolverConfig(
        prefix: String,
        defaultTimeSeconds: FltX = FltX(40.0),
        defaultThreadNum: Int = 4,
        defaultGap: FltX = FltX(0.01),
        defaultNotImprovementTimeSeconds: FltX = FltX(15.0)
    ): SolverConfig {
        val timeSeconds = optionalFltXProperty("$prefix.solver.time.seconds")
            ?: defaultTimeSeconds
        val threadNum = optionalIntProperty("$prefix.solver.thread.num")
            ?: defaultThreadNum
        val gap = optionalFltXProperty("$prefix.solver.gap")
            ?: defaultGap
        val notImprovementTimeSeconds = optionalFltXProperty("$prefix.solver.not.improvement.time.seconds")
            ?: defaultNotImprovementTimeSeconds
        return SolverConfig(
            time = timeSeconds.toDouble().seconds,
            threadNum = UInt64(threadNum.toULong()),
            gap = SolverFlt64(gap.toDouble()),
            notImprovementTime = notImprovementTimeSeconds.toDouble().seconds
        )
    }

    private fun printScenarioMetrics(
        caseName: String,
        response: ColumnGenerationApplicationResponse
    ) {
        val lpObjective = response.result.lpObjectives.lastOrNull()
        val lpGap = response.result.lpInfos.lastOrNull()?.get("lp_gap")
        val milpObjective = response.result.finalInfo["milp_objective"]
        val milpGap = response.result.finalInfo["milp_gap"]
        val selectedBinCount = response.result.finalInfo["selected_bin_count"]
        val selectedLayerCount = response.result.finalInfo["selected_layer_count"]
        println(
            "[gurobi-case] name=$caseName, elapsed_ms=${response.result.elapsed.inWholeMilliseconds}, " +
                    "lp_objective=$lpObjective, lp_gap=$lpGap, milp_objective=$milpObjective, " +
                    "milp_gap=$milpGap, selected_bin_count=$selectedBinCount, selected_layer_count=$selectedLayerCount"
        )
    }

    private fun loadCsvDrivenScenario(resourcePath: String): CsvDrivenScenario {
        val csv = this::class.java.classLoader
            .getResource(resourcePath)
            ?.readText()
            ?: throw IllegalStateException("missing csv resource: $resourcePath")
        return loadCsvDrivenScenarioFromCsvText(
            csv = csv,
            sourceName = resourcePath
        )
    }

    private fun loadCsvDrivenScenarioFromFile(filePath: String): CsvDrivenScenario {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalStateException("invalid dataset file path: $filePath")
        }
        val declaredScenarioKind = declaredScenarioKindFromFileName(file.name)
        return loadCsvDrivenScenarioFromCsvText(
            csv = file.readText(),
            sourceName = file.absolutePath,
            declaredScenarioKind = declaredScenarioKind
        )
    }

    private fun loadCsvDrivenScenarioByPropertyOrResource(
        propertyName: String,
        defaultResourcePath: String
    ): CsvDrivenScenario {
        val filePath = System.getProperty(propertyName)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return if (filePath != null) {
            loadCsvDrivenScenarioFromFile(filePath)
        } else {
            loadCsvDrivenScenario(defaultResourcePath)
        }
    }

    private fun loadCsvDrivenScenarioFromCsvText(
        csv: String,
        sourceName: String = "inline-csv",
        declaredScenarioKind: CsvScenarioKind? = null
    ): CsvDrivenScenario {
        val lines = csv
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.size <= 1) {
            throw IllegalStateException("csv has no data rows")
        }
        val headerColumns = lines.first()
            .split(",")
            .map { it.trim().lowercase(Locale.getDefault()) }
        val scenarioKind = detectCsvScenarioKind(
            headerColumns = headerColumns,
            headerLine = lines.first()
        )
        if (declaredScenarioKind != null && declaredScenarioKind != scenarioKind) {
            throw IllegalStateException(
                "csv scenario kind mismatch: source=$sourceName, declared=$declaredScenarioKind, detected=$scenarioKind"
            )
        }
        validateCsvSchema(
            scenarioKind = scenarioKind,
            headerColumns = headerColumns,
            headerLine = lines.first()
        )
        return when (scenarioKind) {
            CsvScenarioKind.GroupedLayer -> loadGroupedLayerCsvScenario(lines)
            CsvScenarioKind.MaterialWidthAmount -> loadMaterialWidthAmountCsvScenario(lines)
        }
    }

    private fun declaredScenarioKindFromFileName(fileName: String): CsvScenarioKind? {
        val normalized = fileName.lowercase(Locale.getDefault())
        return when {
            normalized.contains("grouped-layer") || normalized.contains("grouped_layer") -> CsvScenarioKind.GroupedLayer
            normalized.contains("material-width-amount") || normalized.contains("material_width_amount") -> CsvScenarioKind.MaterialWidthAmount
            else -> null
        }
    }

    private fun precheckCsvSchemaFromFile(file: File): CsvSchemaPrecheckReport {
        val lines = file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            throw IllegalStateException("csv has no rows: ${file.absolutePath}")
        }
        val headerLine = lines.first()
        val headerColumns = headerLine
            .split(",")
            .map { it.trim().lowercase(Locale.getDefault()) }
        val detectedScenarioKind = detectCsvScenarioKind(
            headerColumns = headerColumns,
            headerLine = headerLine
        )
        val declaredScenarioKind = declaredScenarioKindFromFileName(file.name)
        if (declaredScenarioKind != null && declaredScenarioKind != detectedScenarioKind) {
            throw IllegalStateException(
                "csv scenario kind mismatch in precheck: source=${file.absolutePath}, declared=$declaredScenarioKind, detected=$detectedScenarioKind"
            )
        }
        validateCsvSchema(
            scenarioKind = detectedScenarioKind,
            headerColumns = headerColumns,
            headerLine = headerLine
        )
        return CsvSchemaPrecheckReport(
            sourceName = file.absolutePath,
            declaredScenarioKind = declaredScenarioKind,
            detectedScenarioKind = detectedScenarioKind,
            headerColumns = headerColumns
        )
    }

    private fun printCsvSchemaPrecheckReport(report: CsvSchemaPrecheckReport) {
        val declared = report.declaredScenarioKind?.toString() ?: "UNSPECIFIED"
        println(
            "[csv-precheck] source=${report.sourceName}, declared=$declared, " +
                    "detected=${report.detectedScenarioKind}, columns=${report.headerColumns.joinToString("|")}"
        )
    }

    private fun detectCsvScenarioKind(
        headerColumns: List<String>,
        headerLine: String
    ): CsvScenarioKind {
        return if (headerColumns.containsAll(listOf("group_index", "layer_index"))) {
            CsvScenarioKind.GroupedLayer
        } else if (headerColumns.containsAll(listOf("material", "width", "amount"))) {
            CsvScenarioKind.MaterialWidthAmount
        } else {
            throw IllegalStateException("unsupported csv header: $headerLine")
        }
    }

    private fun validateCsvSchema(
        scenarioKind: CsvScenarioKind,
        headerColumns: List<String>,
        headerLine: String
    ) {
        val requiredColumns = when (scenarioKind) {
            CsvScenarioKind.GroupedLayer -> listOf(
                "group_index",
                "layer_index",
                "item_id",
                "material_no",
                "material_name",
                "material_weight_kg"
            )

            CsvScenarioKind.MaterialWidthAmount -> listOf("material", "width", "amount")
        }
        val missingColumns = requiredColumns.filter { column ->
            !headerColumns.contains(column)
        }
        if (missingColumns.isNotEmpty()) {
            throw IllegalStateException(
                "missing required csv columns: ${missingColumns.joinToString()}, header=$headerLine"
            )
        }
        val normalizedHeaderColumns = headerColumns.map { column ->
            column.lowercase(Locale.getDefault())
        }
        val duplicatedColumns = normalizedHeaderColumns
            .groupingBy { it }
            .eachCount()
            .filter { (_, count) -> count > 1 }
            .keys
        if (duplicatedColumns.isNotEmpty()) {
            throw IllegalStateException(
                "duplicated csv columns: ${duplicatedColumns.joinToString()}, header=$headerLine"
            )
        }
        val hasShapeType = normalizedHeaderColumns.contains("shape_type")
        val shapeMetadataColumns = listOf(
            "radius_meter",
            "radius_min",
            "radius_min_meter",
            "radius_max",
            "radius_max_meter",
            "radius_step",
            "radius_step_meter",
            "diameter_min",
            "diameter_min_meter",
            "diameter_max",
            "diameter_max_meter",
            "diameter_step",
            "diameter_step_meter",
            "radius_weight_function_key",
            "axis"
        )
        val depthBoundaryColumns = listOf(
            "first_layer_allowed_cylinder_axes",
            "last_layer_allowed_cylinder_axes",
            "first_layer_allowed_cuboid_orientations",
            "last_layer_allowed_cuboid_orientations"
        )
        val groupedLayerDimensionColumns = listOf(
            "width_meter",
            "height_meter",
            "depth_meter"
        )
        val optionalColumns = when (scenarioKind) {
            CsvScenarioKind.GroupedLayer -> listOf("shape_type") +
                    shapeMetadataColumns +
                    groupedLayerDimensionColumns +
                    depthBoundaryColumns

            CsvScenarioKind.MaterialWidthAmount -> listOf(
                "material_no",
                "material_name",
                "material_weight_kg",
                "shape_type"
            ) + shapeMetadataColumns + depthBoundaryColumns
        }
        val allowedColumns = (requiredColumns + optionalColumns).toSet()
        val unknownColumns = normalizedHeaderColumns.filter { column ->
            !allowedColumns.contains(column)
        }
        if (unknownColumns.isNotEmpty()) {
            throw IllegalStateException(
                "unsupported csv columns: ${unknownColumns.joinToString()}, header=$headerLine"
            )
        }
        val hasShapeMetadata = shapeMetadataColumns.any { column ->
            normalizedHeaderColumns.contains(column)
        }
        if (hasShapeMetadata && !hasShapeType) {
            throw IllegalStateException(
                "invalid csv schema: shape_type is required when shape metadata columns exist, header=$headerLine"
            )
        }
    }

    private fun optionalCsvFltX(
        cols: List<String>,
        column: Int?,
        fieldName: String,
        rowIndex: Int
    ): FltX? {
        val raw = column
            ?.let { cols.getOrNull(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return raw.toDoubleOrNull()?.let { FltX(it) }
            ?: throw IllegalStateException("invalid $fieldName at row $rowIndex: $raw")
    }

    private fun loadGroupedLayerCsvScenario(lines: List<String>): CsvDrivenScenario {
        val headerColumns = lines.first().split(",").map { it.trim() }
        val headerIndex = headerColumns.withIndex().associate { (index, column) ->
            column.lowercase(Locale.getDefault()) to index
        }
        fun requiredColumn(name: String): Int {
            return headerIndex[name] ?: throw IllegalStateException("missing required csv column: $name")
        }
        fun optionalColumn(name: String): Int? {
            return headerIndex[name]
        }
        fun optionalLengthColumn(name: String): Int? {
            return optionalColumn(name) ?: optionalColumn("${name}_meter")
        }

        val groupIndexColumn = requiredColumn("group_index")
        val layerIndexColumn = requiredColumn("layer_index")
        val itemIdColumn = requiredColumn("item_id")
        val materialNoColumn = requiredColumn("material_no")
        val materialNameColumn = requiredColumn("material_name")
        val materialWeightColumn = requiredColumn("material_weight_kg")
        val shapeTypeColumn = optionalColumn("shape_type")
        val radiusMeterColumn = optionalColumn("radius_meter")
        val radiusMinMeterColumn = optionalLengthColumn("radius_min")
        val radiusMaxMeterColumn = optionalLengthColumn("radius_max")
        val radiusStepMeterColumn = optionalLengthColumn("radius_step")
        val diameterMinMeterColumn = optionalLengthColumn("diameter_min")
        val diameterMaxMeterColumn = optionalLengthColumn("diameter_max")
        val diameterStepMeterColumn = optionalLengthColumn("diameter_step")
        val radiusWeightFunctionKeyColumn = optionalColumn("radius_weight_function_key")
        val axisColumn = optionalColumn("axis")
        val widthMeterColumn = optionalLengthColumn("width")
        val heightMeterColumn = optionalLengthColumn("height")
        val depthMeterColumn = optionalLengthColumn("depth")
        val firstLayerAllowedCylinderAxesColumn = optionalColumn("first_layer_allowed_cylinder_axes")
        val lastLayerAllowedCylinderAxesColumn = optionalColumn("last_layer_allowed_cylinder_axes")
        val firstLayerAllowedCuboidOrientationsColumn = optionalColumn("first_layer_allowed_cuboid_orientations")
        val lastLayerAllowedCuboidOrientationsColumn = optionalColumn("last_layer_allowed_cuboid_orientations")

        val rows = lines.drop(1).mapIndexed { index, line ->
            val cols = line.split(",").map { it.trim() }
            val rowIndex = index + 2
            val requiredColumnCount = listOf(
                groupIndexColumn,
                layerIndexColumn,
                itemIdColumn,
                materialNoColumn,
                materialNameColumn,
                materialWeightColumn
            ).max() + 1
            if (cols.size < requiredColumnCount) {
                throw IllegalStateException("invalid csv columns at row $rowIndex: $line")
            }
            val materialWeight = cols.getOrNull(materialWeightColumn)
                ?.toDoubleOrNull()
                ?.let { FltX(it) }
                ?: throw IllegalStateException("invalid material_weight_kg at row $rowIndex: ${cols.getOrNull(materialWeightColumn)}")
            CsvScenarioRow(
                groupIndex = cols.getOrNull(groupIndexColumn)?.toIntOrNull()
                    ?: throw IllegalStateException("invalid group_index at row $rowIndex: ${cols.getOrNull(groupIndexColumn)}"),
                layerIndex = cols.getOrNull(layerIndexColumn)?.toIntOrNull()
                    ?: throw IllegalStateException("invalid layer_index at row $rowIndex: ${cols.getOrNull(layerIndexColumn)}"),
                itemId = cols.getOrNull(itemIdColumn)
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("empty item_id at row $rowIndex"),
                materialNo = cols.getOrNull(materialNoColumn)
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("empty material_no at row $rowIndex"),
                materialName = cols.getOrNull(materialNameColumn)
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("empty material_name at row $rowIndex"),
                materialWeightKg = materialWeight,
                shapeType = shapeTypeColumn?.let { column -> cols.getOrNull(column) }?.takeIf { it.isNotEmpty() },
                radiusMeter = optionalCsvFltX(
                    cols = cols,
                    column = radiusMeterColumn,
                    fieldName = "radius_meter",
                    rowIndex = rowIndex
                ),
                radiusMinMeter = optionalCsvFltX(
                    cols = cols,
                    column = radiusMinMeterColumn,
                    fieldName = "radius_min",
                    rowIndex = rowIndex
                ),
                radiusMaxMeter = optionalCsvFltX(
                    cols = cols,
                    column = radiusMaxMeterColumn,
                    fieldName = "radius_max",
                    rowIndex = rowIndex
                ),
                radiusStepMeter = optionalCsvFltX(
                    cols = cols,
                    column = radiusStepMeterColumn,
                    fieldName = "radius_step",
                    rowIndex = rowIndex
                ),
                diameterMinMeter = optionalCsvFltX(
                    cols = cols,
                    column = diameterMinMeterColumn,
                    fieldName = "diameter_min",
                    rowIndex = rowIndex
                ),
                diameterMaxMeter = optionalCsvFltX(
                    cols = cols,
                    column = diameterMaxMeterColumn,
                    fieldName = "diameter_max",
                    rowIndex = rowIndex
                ),
                diameterStepMeter = optionalCsvFltX(
                    cols = cols,
                    column = diameterStepMeterColumn,
                    fieldName = "diameter_step",
                    rowIndex = rowIndex
                ),
                radiusWeightFunctionKey = radiusWeightFunctionKeyColumn?.let { column ->
                    cols.getOrNull(column)
                }?.takeIf { it.isNotEmpty() },
                axis = axisColumn?.let { column -> cols.getOrNull(column) }?.takeIf { it.isNotEmpty() },
                widthMeter = optionalCsvFltX(
                    cols = cols,
                    column = widthMeterColumn,
                    fieldName = "width",
                    rowIndex = rowIndex
                ),
                heightMeter = optionalCsvFltX(
                    cols = cols,
                    column = heightMeterColumn,
                    fieldName = "height",
                    rowIndex = rowIndex
                ),
                depthMeter = optionalCsvFltX(
                    cols = cols,
                    column = depthMeterColumn,
                    fieldName = "depth",
                    rowIndex = rowIndex
                )
            )
        }

        val materialsByNo = LinkedHashMap<String, Material<FltX>>()
        val materialWeightKgByNo = LinkedHashMap<String, FltX>()
        for (row in rows) {
            if (materialsByNo.containsKey(row.materialNo)) {
                continue
            }
            materialsByNo[row.materialNo] = Material(
                no = MaterialNo(row.materialNo),
                type = MaterialType.RawMaterial,
                cargo = CargoAttr,
                name = row.materialName,
                weight = row.materialWeightKg * Kilogram
            )
            materialWeightKgByNo[row.materialNo] = row.materialWeightKg
        }

        val itemsById = LinkedHashMap<String, ActualItem>()
        for (row in rows) {
            if (itemsById.containsKey(row.itemId)) {
                continue
            }
            val material = materialsByNo[row.materialNo]
                ?: throw IllegalStateException("missing material for item: ${row.itemId}")
            val shapeSpec = row.toPackageShapeSpec()
            itemsById[row.itemId] = itemFromCsvDimensions(
                id = row.itemId,
                material = material,
                widthInMeter = row.widthMeter ?: FltX.one,
                heightInMeter = row.heightMeter ?: FltX.one,
                depthInMeter = row.depthMeter ?: FltX.one,
                shapeSpec = shapeSpec
            )
        }

        val rowsByGroup = rows.groupBy { it.groupIndex }.toSortedMap()
        val initialColumns = ArrayList<BinLayer>()
        val finalBins = ArrayList<Bin<BinLayer, FltX>>()
        for ((groupIndex, groupRows) in rowsByGroup) {
            val rowsByLayer = groupRows.groupBy { it.layerIndex }.toSortedMap()
            val layerCount = rowsByLayer.size
            val maxItemsPerLayer = rowsByLayer.values.maxOf { it.size }
            val maxLayerWidth = rowsByLayer.values.maxOf { layerRows ->
                layerRows.fold(0.0) { acc, row -> acc + (row.widthMeter?.toDouble() ?: 1.0) }
            }
            val maxLayerHeight = rowsByLayer.values.maxOf { layerRows ->
                layerRows.maxOf { row -> row.heightMeter?.toDouble() ?: 1.0 }
            }
            val maxLayerDepth = rowsByLayer.values.maxOf { layerRows ->
                layerRows.maxOf { row -> row.depthMeter?.toDouble() ?: 1.0 }
            }
            val binType = BinType(
                width = maxOf(FltX(maxItemsPerLayer.toLong()), FltX(maxLayerWidth)) * Meter,
                height = maxOf(FltX(3.0), FltX(maxLayerHeight * 3.0)) * Meter,
                depth = maxOf(FltX(layerCount.toLong()), FltX(maxLayerDepth)) * Meter,
                capacity = FltX(1500.0) * Kilogram,
                longitudinalBalance = null,
                lateralBalance = null,
                typeCode = "BIN-GUROBI-CSV-$groupIndex"
            )
            finalBins.add(
                layerBinOf(
                    shape = binType,
                    units = emptyList(),
                    batchNo = BatchNo("B-GUROBI-CSV-$groupIndex")
                )
            )
            for ((_, layerRows) in rowsByLayer) {
                val layerItems = layerRows.map { row ->
                    itemsById[row.itemId]
                        ?: throw IllegalStateException("missing item: ${row.itemId}")
                }
                val generatedHorizontalLayer = layerItems
                    .singleOrNull()
                    ?.let { item -> generatedHorizontalSeedLayer(item, binType) }
                    ?: generatedHorizontalSupportedStackLayer(
                        items = layerItems,
                        binType = binType
                    )
                if (generatedHorizontalLayer != null) {
                    initialColumns.add(generatedHorizontalLayer)
                } else {
                    val bin = layerBin(
                        items = layerItems,
                        typeCode = binType.typeCode,
                        depthInMeter = FltX(layerCount.toLong()),
                        binType = binType,
                        widthInMeter = FltX(maxItemsPerLayer.toLong())
                    )
                    val rawLayer = bin.units.first().unit
                    initialColumns.add(
                        BinLayer(
                            iteration = rawLayer.iteration,
                            from = rawLayer.from,
                            bin = binType,
                            shape = rawLayer.shape,
                            units = rawLayer.units
                        )
                    )
                }
            }
        }

        val sortedItems = itemsById.values.sortedBy { it.id }
        val itemDemands = sortedItems.map { actualItem ->
            Pair(actualItem, UInt64.one)
        }
        val materialAmountByNo = rows
            .groupingBy { it.materialNo }
            .eachCount()
        val materialAmountDemands = materialAmountByNo.entries.associate { entry ->
            val material = materialsByNo[entry.key]
                ?: throw IllegalStateException("missing material in amount map: ${entry.key}")
            Pair(material, UInt64(entry.value))
        }
        val materialWeightDemands = materialAmountByNo.entries.map { entry ->
            val material = materialsByNo[entry.key]
                ?: throw IllegalStateException("missing material in weight map: ${entry.key}")
            val weightKg = materialWeightKgByNo[entry.key]
                ?: throw IllegalStateException("missing material weight in map: ${entry.key}")
            Pair(material, (FltX(entry.value.toLong()) * weightKg) * Kilogram)
        }
        val depthBoundaryLayerOrientationPolicy = depthBoundaryPolicyFromRows(
            rows = lines.drop(1).map { line -> line.split(",").map { it.trim() } },
            firstLayerAllowedCylinderAxesColumn = firstLayerAllowedCylinderAxesColumn,
            lastLayerAllowedCylinderAxesColumn = lastLayerAllowedCylinderAxesColumn,
            firstLayerAllowedCuboidOrientationsColumn = firstLayerAllowedCuboidOrientationsColumn,
            lastLayerAllowedCuboidOrientationsColumn = lastLayerAllowedCuboidOrientationsColumn
        )
        val demandEntries = demandEntriesFromItems(items = itemDemands) +
                demandEntriesFromMaterialAmounts(
                    materials = materialAmountDemands.entries.map { entry ->
                        Pair(entry.key, entry.value)
                    }
                ) +
                demandEntriesFromMaterialWeights(
                    materials = materialWeightDemands
                )
        return CsvDrivenScenario(
            itemDemands = itemDemands,
            demandEntries = demandEntries,
            initialColumns = initialColumns,
            finalBins = finalBins,
            materialAmountDemands = materialAmountDemands,
            groupCount = rowsByGroup.size,
            materialCount = materialsByNo.size,
            totalLayerCount = initialColumns.size,
            totalItemCount = rows.size,
            depthBoundaryLayerOrientationPolicy = depthBoundaryLayerOrientationPolicy
        )
    }

    private fun CsvScenarioRow.toPackageShapeSpec(): PackageShapeSpec {
        return toPackageShapeSpec(
            shapeType = shapeType,
            radiusMeter = radiusMeter,
            radiusMinMeter = radiusMinMeter,
            radiusMaxMeter = radiusMaxMeter,
            radiusStepMeter = radiusStepMeter,
            diameterMinMeter = diameterMinMeter,
            diameterMaxMeter = diameterMaxMeter,
            diameterStepMeter = diameterStepMeter,
            radiusWeightFunctionKey = radiusWeightFunctionKey,
            axis = axis,
            rowDescription = "item_id=$itemId, group_index=$groupIndex, layer_index=$layerIndex"
        )
    }

    private fun parseAxis3OrNull(raw: String): Axis3? {
        return when (raw.trim().uppercase(Locale.getDefault())) {
            "X", "AXIS3.X" -> Axis3.X
            "Y", "AXIS3.Y" -> Axis3.Y
            "Z", "AXIS3.Z" -> Axis3.Z
            else -> null
        }
    }

    private fun parseAxis3OrThrow(axis: String?, rowDescription: String): Axis3 {
        if (axis.isNullOrBlank()) {
            return Axis3.Y
        }
        return parseAxis3OrNull(axis)
            ?: throw IllegalStateException("invalid axis for cylinder row: axis=$axis, $rowDescription")
    }

    private fun depthBoundaryPolicyFromRows(
        rows: List<List<String>>,
        firstLayerAllowedCylinderAxesColumn: Int?,
        lastLayerAllowedCylinderAxesColumn: Int?,
        firstLayerAllowedCuboidOrientationsColumn: Int?,
        lastLayerAllowedCuboidOrientationsColumn: Int?
    ): DepthBoundaryLayerOrientationPolicy? {
        val firstLayerAllowedCylinderAxes = parseOptionalAxisSetColumn(
            rows = rows,
            column = firstLayerAllowedCylinderAxesColumn,
            fieldName = "first_layer_allowed_cylinder_axes"
        )
        val lastLayerAllowedCylinderAxes = parseOptionalAxisSetColumn(
            rows = rows,
            column = lastLayerAllowedCylinderAxesColumn,
            fieldName = "last_layer_allowed_cylinder_axes"
        )
        val firstLayerAllowedCuboidOrientations = parseOptionalOrientationSetColumn(
            rows = rows,
            column = firstLayerAllowedCuboidOrientationsColumn,
            fieldName = "first_layer_allowed_cuboid_orientations"
        )
        val lastLayerAllowedCuboidOrientations = parseOptionalOrientationSetColumn(
            rows = rows,
            column = lastLayerAllowedCuboidOrientationsColumn,
            fieldName = "last_layer_allowed_cuboid_orientations"
        )
        if (firstLayerAllowedCylinderAxes == null
            && lastLayerAllowedCylinderAxes == null
            && firstLayerAllowedCuboidOrientations == null
            && lastLayerAllowedCuboidOrientations == null
        ) {
            return null
        }
        return DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCylinderAxes = firstLayerAllowedCylinderAxes,
            lastLayerAllowedCylinderAxes = lastLayerAllowedCylinderAxes,
            firstLayerAllowedCuboidOrientations = firstLayerAllowedCuboidOrientations,
            lastLayerAllowedCuboidOrientations = lastLayerAllowedCuboidOrientations
        )
    }

    private fun parseOptionalAxisSetColumn(
        rows: List<List<String>>,
        column: Int?,
        fieldName: String
    ): Set<Axis3>? {
        return parseOptionalSetColumn(
            rows = rows,
            column = column,
            fieldName = fieldName,
            parseValue = { raw ->
                parseAxis3OrNull(raw)
                    ?: throw IllegalStateException("invalid axis in $fieldName: $raw")
            }
        )
    }

    private fun parseOptionalOrientationSetColumn(
        rows: List<List<String>>,
        column: Int?,
        fieldName: String
    ): Set<Orientation>? {
        return parseOptionalSetColumn(
            rows = rows,
            column = column,
            fieldName = fieldName,
            parseValue = { raw ->
                Orientation.require(raw.trim())
                    .value
                    ?: throw IllegalStateException("invalid orientation in $fieldName: $raw")
            }
        )
    }

    private fun <T> parseOptionalSetColumn(
        rows: List<List<String>>,
        column: Int?,
        fieldName: String,
        parseValue: (String) -> T
    ): Set<T>? {
        if (column == null) {
            return null
        }
        val rawValues = rows.map { row -> row.getOrNull(column).orEmpty().trim() }
            .distinct()
        if (rawValues.size > 1) {
            throw IllegalStateException("inconsistent $fieldName in csv rows: ${rawValues.joinToString()}")
        }
        val raw = rawValues.singleOrNull().orEmpty()
        if (raw.isEmpty()) {
            return emptySet()
        }
        return raw.split("|", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapTo(LinkedHashSet()) { parseValue(it) }
    }

    @Test
    fun groupedLayerCsvShouldMapVerticalCylinderShapeSpecFromCsvColumns() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_meter,axis
            0,0,item-cylinder,MAT-A,Material-A,1.0,vertical_cylinder,0.5,Y
            0,0,item-cuboid,MAT-B,Material-B,2.0,,,
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val itemsById = scenario.itemDemands.associate { (item, _) ->
            item.id to item
        }
        val cylinderItem = itemsById["item-cylinder"]
            ?: throw IllegalStateException("missing item-cylinder in scenario")
        val cuboidItem = itemsById["item-cuboid"]
            ?: throw IllegalStateException("missing item-cuboid in scenario")
        val cylinderSpec = cylinderItem.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("item-cylinder shape spec should be VerticalCylinder")

        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(0.5, cylinderSpec.radius.value.toDouble())
        assertEquals(PackageShapeSpec.Cuboid, cuboidItem.packageShape.shapeSpec)
    }

    @Test
    fun groupedLayerCsvShouldMapDynamicDiameterColumnsToVerticalCylinderShapeSpec() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,diameter_min,diameter_max,diameter_step,axis
            0,0,item-cylinder,MAT-A,Material-A,1.0,vertical_cylinder,0.30,0.36,0.01,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val cylinderItem = scenario.itemDemands
            .firstOrNull()
            ?.first
            ?: throw IllegalStateException("missing item-cylinder in scenario")
        val cylinderSpec = cylinderItem.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("item-cylinder shape spec should be VerticalCylinder")

        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(
            expected = 0.15,
            actual = cylinderSpec.radius.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.30,
            actual = cylinderSpec.diameterMin!!.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.36,
            actual = cylinderSpec.diameterMax!!.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.01,
            actual = cylinderSpec.diameterStep!!.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(7, cylinderSpec.resolvedRadiusCandidates.size)
        assertEquals(
            expected = 0.18,
            actual = cylinderSpec.resolvedRadiusCandidates.last().value.toDouble(),
            absoluteTolerance = 1e-9
        )
    }

    @Test
    fun groupedLayerCsvShouldAcceptMinimalSixColumns() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg
            0,0,item-minimal,MAT-A,Material-A,1.0
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val minimalItem = scenario.itemDemands
            .firstOrNull()
            ?.first
            ?: throw IllegalStateException("missing minimal item in scenario")

        assertEquals(PackageShapeSpec.Cuboid, minimalItem.packageShape.shapeSpec)
    }

    @Test
    fun materialWidthAmountCsvShouldMapVerticalCylinderShapeSpecFromCsvColumns() {
        val csv = """
            material,width,amount,material_no,material_name,material_weight_kg,shape_type,radius_meter,axis
            MAT-A,1200,2,MAT-A,Material-A,1.0,vertical_cylinder,0.4,Y
            MAT-B,1000,1,MAT-B,Material-B,2.0,cuboid,,
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val itemDemandsById = scenario.itemDemands.associate { (item, demand) ->
            item.id to Pair(item, demand)
        }
        val cylinderEntry = itemDemandsById["item-material-width-0"]
            ?: throw IllegalStateException("missing cylinder entry")
        val cuboidEntry = itemDemandsById["item-material-width-1"]
            ?: throw IllegalStateException("missing cuboid entry")
        val cylinderSpec = cylinderEntry.first.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("item-material-width-0 should be VerticalCylinder")

        assertEquals(2L, cylinderEntry.second.toLong())
        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(0.4, cylinderSpec.radius.value.toDouble())
        assertEquals(PackageShapeSpec.Cuboid, cuboidEntry.first.packageShape.shapeSpec)
    }

    @Test
    fun materialWidthAmountCsvShouldMapDynamicRadiusColumnsToVerticalCylinderShapeSpec() {
        val csv = """
            material,width,amount,material_no,material_name,material_weight_kg,shape_type,radius_min,radius_max,radius_step,axis
            MAT-A,1200,2,MAT-A,Material-A,1.0,vertical_cylinder,0.15,0.18,0.005,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val cylinderItem = scenario.itemDemands
            .firstOrNull()
            ?.first
            ?: throw IllegalStateException("missing cylinder entry")
        val cylinderSpec = cylinderItem.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("item-material-width-0 should be VerticalCylinder")

        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(
            expected = 0.15,
            actual = cylinderSpec.radius.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.15,
            actual = cylinderSpec.radiusMin!!.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.18,
            actual = cylinderSpec.radiusMax!!.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.005,
            actual = cylinderSpec.radiusStep!!.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(7, cylinderSpec.resolvedRadiusCandidates.size)
        assertEquals(
            expected = 0.18,
            actual = cylinderSpec.resolvedRadiusCandidates.last().value.toDouble(),
            absoluteTolerance = 1e-9
        )
    }

    @Test
    fun materialWidthAmountCsvShouldMapHorizontalCylinderWidthToAxisLength() {
        val csv = """
            material,width,amount,material_no,material_name,material_weight_kg,shape_type,radius_meter,axis
            MAT-X,1200,1,MAT-X,Material-X,1.0,vertical_cylinder,0.25,X
            MAT-Z,1400,1,MAT-Z,Material-Z,1.0,vertical_cylinder,0.30,Z
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val itemsById = scenario.itemDemands.associate { (item, _) ->
            item.id to item
        }
        val itemX = itemsById["item-material-width-0"]
            ?: throw IllegalStateException("missing item-material-width-0")
        val itemZ = itemsById["item-material-width-1"]
            ?: throw IllegalStateException("missing item-material-width-1")

        assertEquals(
            expected = 1.2,
            actual = itemX.packageShape.width.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.5,
            actual = itemX.packageShape.height.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.5,
            actual = itemX.packageShape.depth.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.6,
            actual = itemZ.packageShape.width.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 0.6,
            actual = itemZ.packageShape.height.value.toDouble(),
            absoluteTolerance = 1e-9
        )
        assertEquals(
            expected = 1.4,
            actual = itemZ.packageShape.depth.value.toDouble(),
            absoluteTolerance = 1e-9
        )
    }

    @Test
    fun csvShapeSpecParserShouldAcceptHorizontalCylinderAxesAsMetadata() {
        val cylinderXSpec = toPackageShapeSpec(
            shapeType = "vertical_cylinder",
            radiusMeter = FltX(0.5),
            radiusMinMeter = null,
            radiusMaxMeter = null,
            radiusStepMeter = null,
            diameterMinMeter = null,
            diameterMaxMeter = null,
            diameterStepMeter = null,
            radiusWeightFunctionKey = null,
            axis = "X",
            rowDescription = "axis=x metadata parser test"
        ) as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("axis=x shape spec should be VerticalCylinder")
        val cylinderZSpec = toPackageShapeSpec(
            shapeType = "vertical_cylinder",
            radiusMeter = FltX(0.5),
            radiusMinMeter = null,
            radiusMaxMeter = null,
            radiusStepMeter = null,
            diameterMinMeter = null,
            diameterMaxMeter = null,
            diameterStepMeter = null,
            radiusWeightFunctionKey = null,
            axis = "Axis3.Z",
            rowDescription = "axis=z metadata parser test"
        ) as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("axis=z shape spec should be VerticalCylinder")

        assertEquals(Axis3.X, cylinderXSpec.axis)
        assertEquals(Axis3.Z, cylinderZSpec.axis)
    }

    @Test
    fun materialWidthAmountCsvShouldAcceptMinimalColumns() {
        val csv = """
            material,width,amount
            MAT-A,1000,1
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val minimalItem = scenario.itemDemands
            .firstOrNull()
            ?.first
            ?: throw IllegalStateException("missing minimal item in scenario")

        assertEquals(PackageShapeSpec.Cuboid, minimalItem.packageShape.shapeSpec)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectInvalidCylinderAxis() {
        val csv = """
            material,width,amount,shape_type,radius_meter,axis
            MAT-A,1000,1,vertical_cylinder,0.3,INVALID
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }
        assertTrue(exception.message?.contains("invalid axis") == true)
    }

    @Test
    fun groupedLayerCsvShouldAcceptPWLIntervalOnlyContinuousRadius() {
        // Interval-only continuous radius (no radius_meter, no radius_step) is now accepted
        // via the PWL approximation path. Previously rejected as unsupported, now handled
        // by ContinuousRadiusModelComponent's PWL registration.
        // 仅区间连续半径（无 radius_meter、无 radius_step）现在通过 PWL 近似路径被接受。
        // 之前作为 unsupported 被拒绝，现在由 ContinuousRadiusModelComponent 的 PWL 注册处理。
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_min,radius_max,radius_weight_function_key,axis
            0,0,item-pwl-interval,MAT-A,Material-A,1.0,vertical_cylinder,0.15,0.18,cylinder_pwl_a,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val item = scenario.itemDemands.single().first
        val cylinderSpec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder

        assertNotNull(cylinderSpec)
        assertEquals("cylinder_pwl_a", cylinderSpec.radiusWeightFunctionKey)
        // PWL path uses interval bounds, not a fixed selected radius
        // PWL 路径使用区间边界，而非固定选择半径
        assertNotNull(cylinderSpec.radius)
    }

    @Test
    fun groupedLayerCsvShouldAcceptSelectedContinuousRadiusWeightFunctionKey() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_meter,radius_weight_function_key,axis
            0,0,item-continuous-radius-key,MAT-A,Material-A,1.0,vertical_cylinder,0.15,prefer-large-radius,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val item = scenario.itemDemands.single().first
        val cylinderSpec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder

        assertNotNull(cylinderSpec)
        assertEquals("prefer-large-radius", cylinderSpec.radiusWeightFunctionKey)
        assertEquals(0.15, cylinderSpec.radius.value.toDouble(), 1e-9)
    }

    @Test
    fun groupedLayerCsvShouldRejectSelectedContinuousRadiusOutsideRadiusBounds() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_meter,radius_min,radius_max,radius_weight_function_key,axis
            0,0,item-continuous-radius-key,MAT-A,Material-A,1.0,vertical_cylinder,0.15,0.20,0.30,prefer-large-radius,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(
            exception.message
                ?.contains("selected or initial radius must be greater than or equal to lower bound") == true
        )
        assertTrue(exception.message?.contains("prefer-large-radius") == true)
    }

    @Test
    fun groupedLayerCsvShouldAcceptPWLIntervalWithWeightFunctionKey() {
        // Interval-only radius with radius_weight_function_key is now accepted via PWL path.
        // The solver will select the optimal radius through piecewise-linear approximation.
        // 有 radius_weight_function_key 的仅区间半径现在通过 PWL 路径被接受。
        // solver 将通过分段线性近似选择最优半径。
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_min,radius_max,radius_weight_function_key,axis
            0,0,item-continuous-radius-key,MAT-A,Material-A,1.0,vertical_cylinder,0.15,0.18,prefer-large-radius,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val item = scenario.itemDemands.single().first
        val cylinderSpec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder

        assertNotNull(cylinderSpec)
        assertEquals("prefer-large-radius", cylinderSpec.radiusWeightFunctionKey)
    }

    @Test
    fun materialWidthAmountCsvShouldAcceptSelectedContinuousRadiusWeightFunctionKey() {
        val csv = """
            material,width,amount,shape_type,radius_meter,radius_weight_function_key,axis
            MAT-A,1000,1,vertical_cylinder,0.20,prefer-large-radius,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val item = scenario.itemDemands.single().first
        val cylinderSpec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder

        assertNotNull(cylinderSpec)
        assertEquals("prefer-large-radius", cylinderSpec.radiusWeightFunctionKey)
        assertEquals(0.20, cylinderSpec.radius.value.toDouble(), 1e-9)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectSelectedContinuousRadiusOutsideDiameterBounds() {
        val csv = """
            material,width,amount,shape_type,radius_meter,diameter_min,diameter_max,radius_weight_function_key,axis
            MAT-A,1000,1,vertical_cylinder,0.20,0.50,0.70,prefer-large-radius,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(
            exception.message
                ?.contains("selected or initial radius must be greater than or equal to lower bound") == true
        )
        assertTrue(exception.message?.contains("prefer-large-radius") == true)
    }

    @Test
    fun materialWidthAmountCsvShouldAcceptPWLIntervalWithWeightFunctionKey() {
        // material-width-amount format with interval-only radius and key is now accepted via PWL path.
        // The solver will select the optimal radius through piecewise-linear approximation.
        // material-width-amount 格式的仅区间半径和 key 现在通过 PWL 路径被接受。
        // solver 将通过分段线性近似选择最优半径。
        val csv = """
            material,width,amount,shape_type,radius_min,radius_max,radius_weight_function_key,axis
            MAT-A,1000,1,vertical_cylinder,0.15,0.18,prefer-large-radius,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val item = scenario.itemDemands.single().first
        val cylinderSpec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder

        assertNotNull(cylinderSpec)
        assertEquals("prefer-large-radius", cylinderSpec.radiusWeightFunctionKey)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectContinuousRadiusWeightFunctionKeyWithDiscreteStep() {
        val csv = """
            material,width,amount,shape_type,radius_meter,radius_min,radius_max,radius_step,radius_weight_function_key,axis
            MAT-A,1000,1,vertical_cylinder,0.16,0.15,0.18,0.01,prefer-large-radius,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("radius_weight_function_key cannot be combined") == true)
    }

    @Test
    fun materialWidthAmountCsvShouldAcceptPWLDiameterIntervalOnly() {
        // Interval-only continuous diameter (no diameter_meter, no diameter_step) is now accepted
        // via the PWL approximation path, when radius_weight_function_key is provided.
        // 仅区间连续直径（无 diameter_meter、无 diameter_step）现在在提供
        // radius_weight_function_key 时通过 PWL 近似路径被接受。
        val csv = """
            material,width,amount,material_no,material_name,material_weight_kg,shape_type,diameter_min,diameter_max,radius_weight_function_key,axis
            MAT-A,1000,1,MAT-A,Material-A,1.0,vertical_cylinder,0.30,0.36,cylinder_pwl_diameter,Y
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val item = scenario.itemDemands.single().first
        val cylinderSpec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder

        assertNotNull(cylinderSpec)
        assertEquals("cylinder_pwl_diameter", cylinderSpec.radiusWeightFunctionKey)
    }

    @Test
    fun groupedLayerCsvShouldRejectIntervalWithoutWeightFunctionKey() {
        // Interval-only radius without radius_weight_function_key is still rejected
        // because PWL-registerable prototype requires a weight function key for production writeback.
        // 无 radius_weight_function_key 的仅区间半径仍被拒绝，
        // 因为 PWL 可注册原型需要权重函数键才能进行生产回写。
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_min,radius_max,axis
            0,0,item-no-key,MAT-A,Material-A,1.0,vertical_cylinder,0.15,0.18,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }
        // PWL-registerable but without key → still blocked by gap report
        assertTrue(exception.message?.contains("unsupported") == true || exception.message?.contains("MissingSelectedRadius") == true)
    }

    @Test
    fun groupedLayerCsvShouldRejectInvalidRadiusInterval() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_min,radius_max,radius_step,axis
            0,0,item-invalid-radius,MAT-A,Material-A,1.0,vertical_cylinder,0.20,0.15,0.01,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("radiusMin must be less than or equal to radiusMax") == true)
    }

    @Test
    fun groupedLayerCsvShouldRejectMixedHorizontalAxesBeforeManualLayerPlacement() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_meter,axis
            0,0,item-x,MAT-X,Material-X,1.0,vertical_cylinder,0.25,X
            0,0,item-z,MAT-Z,Material-Z,1.0,vertical_cylinder,0.25,Z
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("verified axis-aware generated candidates") == true)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectShapeColumnsWithoutShapeType() {
        val csv = """
            material,width,amount,radius_meter,axis
            MAT-A,1000,1,0.3,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }
        assertTrue(exception.message?.contains("shape_type is required") == true)
    }

    @Test
    fun groupedLayerCsvShouldRejectSchemaWhenShapeTypeColumnMissingButShapeMetadataColumnsExist() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,radius_meter,axis
            0,0,item-1,MAT-A,Material-A,1.0,0.5,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }
        assertTrue(exception.message?.contains("shape_type is required") == true)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectSchemaWhenShapeTypeColumnMissingButAxisColumnExists() {
        val csv = """
            material,width,amount,axis
            MAT-A,1000,1,Y
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }
        assertTrue(exception.message?.contains("shape_type is required") == true)
    }

    @Test
    fun groupedLayerCsvShouldRejectUnsupportedSchemaColumns() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,unexpected_shape_hint
            0,0,item-a,MAT-A,Material-A,1.0,default
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("unsupported csv columns") == true)
        assertTrue(exception.message?.contains("unexpected_shape_hint") == true)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectDuplicatedSchemaColumns() {
        val csv = """
            material,width,amount,width
            MAT-A,1000,1,1200
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("duplicated csv columns") == true)
        assertTrue(exception.message?.contains("width") == true)
    }

    @Test
    fun groupedLayerCsvShouldMapDepthBoundaryPolicyColumns() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,first_layer_allowed_cylinder_axes,last_layer_allowed_cylinder_axes,first_layer_allowed_cuboid_orientations,last_layer_allowed_cuboid_orientations
            0,0,item-a,MAT-A,Material-A,1.0,Y|Z,X,Upright|Side,Lie
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val policy = scenario.depthBoundaryLayerOrientationPolicy

        assertNotNull(policy)
        assertEquals(setOf(Axis3.Y, Axis3.Z), policy.firstLayerAllowedCylinderAxes)
        assertEquals(setOf(Axis3.X), policy.lastLayerAllowedCylinderAxes)
        assertEquals(setOf(Orientation.Upright, Orientation.Side), policy.firstLayerAllowedCuboidOrientations)
        assertEquals(setOf(Orientation.Lie), policy.lastLayerAllowedCuboidOrientations)
    }

    @Test
    fun materialWidthAmountCsvShouldRejectEmptyDepthBoundaryPolicySet() {
        val csv = """
            material,width,amount,first_layer_allowed_cylinder_axes
            MAT-A,1000,1,
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("firstLayerAllowedCylinderAxes") == true)
    }

    @Test
    fun groupedLayerCsvShouldRejectInvalidDepthBoundaryCylinderAxis() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,first_layer_allowed_cylinder_axes
            0,0,item-a,MAT-A,Material-A,1.0,Q
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("invalid axis in first_layer_allowed_cylinder_axes") == true)
        assertTrue(exception.message?.contains("Q") == true)
    }

    @Test
    fun groupedLayerCsvShouldRejectInvalidDepthBoundaryCuboidOrientation() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,last_layer_allowed_cuboid_orientations
            0,0,item-a,MAT-A,Material-A,1.0,Diagonal
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("invalid orientation in last_layer_allowed_cuboid_orientations") == true)
        assertTrue(exception.message?.contains("Diagonal") == true)
    }

    @Test
    fun declaredGroupedLayerScenarioKindShouldRejectMaterialWidthAmountHeader() {
        val csv = """
            material,width,amount
            MAT-A,1000,1
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            loadCsvDrivenScenarioFromCsvText(
                csv = csv,
                sourceName = "declared-grouped-layer.csv",
                declaredScenarioKind = CsvScenarioKind.GroupedLayer
            )
        }
        assertTrue(exception.message?.contains("scenario kind mismatch") == true)
    }

    @Test
    fun groupedLayerMixedShapeSampleFileShouldBeParsable() {
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-cylinder-mixed-sample.csv")
        assertEquals(2, scenario.groupCount)
        assertTrue(scenario.totalItemCount > 0)
        assertEquals(scenario.totalLayerCount, scenario.packedLayerCount)
    }

    @Test
    fun groupedLayerHorizontalMultiSupportSampleFileShouldGenerateSupportedStackSeedLayer() {
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-horizontal-multisupport-sample.csv")
        val layer = scenario.initialColumns.single()

        assertEquals(1, scenario.groupCount)
        assertEquals(3, scenario.totalItemCount)
        assertEquals(CirclePackingLayerGenerator::class, layer.from)
        assertEquals(3, layer.units.size)
        assertTrue(
            layer.units.any { placement ->
                val shape = placement.resolvedPackingShape() as? CylinderPackingShape3
                shape?.axis == Axis3.X
            }
        )
        val supportIds = layer.units
            .filter { placement -> placement.resolvedPackingShape() !is CylinderPackingShape3 }
            .map { placement -> (placement.view.unit as ActualItem).id }
            .toSet()
        assertEquals(
            setOf(
                "item-horizontal-support-a",
                "item-horizontal-support-b"
            ),
            supportIds
        )
    }

    @Test
    fun groupedLayerHorizontalZMultiSupportSampleFileShouldGenerateSupportedStackSeedLayer() {
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-horizontal-z-multisupport-sample.csv")
        val layer = scenario.initialColumns.single()

        assertEquals(1, scenario.groupCount)
        assertEquals(3, scenario.totalItemCount)
        assertEquals(CirclePackingLayerGenerator::class, layer.from)
        assertEquals(3, layer.units.size)
        assertTrue(
            layer.units.any { placement ->
                val shape = placement.resolvedPackingShape() as? CylinderPackingShape3
                shape?.axis == Axis3.Z
            }
        )
        val supportIds = layer.units
            .filter { placement -> placement.resolvedPackingShape() !is CylinderPackingShape3 }
            .map { placement -> (placement.view.unit as ActualItem).id }
            .toSet()
        assertEquals(
            setOf(
                "item-horizontal-z-support-a",
                "item-horizontal-z-support-b"
            ),
            supportIds
        )
    }

    @Test
    fun groupedLayerHorizontalMultiHangingSupportSampleFileShouldGenerateHangingSeedLayer() {
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-horizontal-hanging-multisupport-sample.csv")
        val layer = scenario.initialColumns.single()

        assertEquals(1, scenario.groupCount)
        assertEquals(4, scenario.totalItemCount)
        assertEquals(CirclePackingLayerGenerator::class, layer.from)
        assertEquals(4, layer.units.size)
        assertTrue(
            layer.units.any { placement ->
                val shape = placement.resolvedPackingShape() as? CylinderPackingShape3
                shape?.axis == Axis3.X
            }
        )
        val supportPlacements = layer.units.filter { placement ->
            placement.resolvedPackingShape() !is CylinderPackingShape3
        }
        assertEquals(3, supportPlacements.size)
        assertTrue(supportPlacements.all { placement -> placement.absoluteZ.value.toDouble() > 0.0 })
        val supportIds = supportPlacements
            .map { placement -> (placement.view.unit as ActualItem).id }
            .toSet()
        assertEquals(
            setOf(
                "item-horizontal-hanging-multi-support-a",
                "item-horizontal-hanging-multi-support-b",
                "item-horizontal-hanging-multi-support-c"
            ),
            supportIds
        )
    }

    @Test
    fun groupedLayerCsvShouldRejectPartialHorizontalSupportFallback() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg,shape_type,radius_meter,axis,width_meter,height_meter,depth_meter
            0,0,item-horizontal-partial-support,MAT-HPS,Material-HPS,1.0,cuboid,,,0.05,0.2,0.4
            0,0,item-horizontal-cylinder-x,MAT-HCX,Material-HCX,1.0,vertical_cylinder,0.5,X,1.0,1.0,1.0
        """.trimIndent()

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            loadCsvDrivenScenarioFromCsvText(csv)
        }

        assertTrue(exception.message?.contains("Unsupported horizontal cylinder") == true)
        assertTrue(exception.message?.contains("only verified axis-aware generated candidates") == true)
    }

    @Test
    fun materialWidthAmountMixedShapeSampleFileShouldBeParsable() {
        val scenario = loadCsvDrivenScenario("gurobi/material-width-amount-cylinder-sample.csv")
        val horizontalAxes = scenario.itemDemands.mapNotNull { (item, _) ->
            horizontalAxisOf(item)
        }

        assertEquals(1, scenario.groupCount)
        assertTrue(scenario.totalItemCount > 0)
        assertTrue(scenario.packedLayerCount >= scenario.totalLayerCount)
        assertTrue(horizontalAxes.contains(Axis3.X))
        assertTrue(
            scenario.initialColumns.any { layer ->
                layer.from == CirclePackingLayerGenerator::class
                        && layer.units.any { placement ->
                            val shape = placement.resolvedPackingShape() as? CylinderPackingShape3
                            shape?.axis == Axis3.X
                        }
            }
        )
    }

    @Test
    fun groupedLayerDepthBoundarySampleFileShouldBeParsable() {
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-depth-boundary-sample.csv")
        val policy = scenario.depthBoundaryLayerOrientationPolicy

        assertEquals(2, scenario.groupCount)
        assertEquals(4, scenario.totalItemCount)
        assertNotNull(policy)
        assertEquals(setOf(Axis3.Y), policy.firstLayerAllowedCylinderAxes)
        assertEquals(setOf(Axis3.Y), policy.lastLayerAllowedCylinderAxes)
        assertEquals(setOf(Orientation.Upright), policy.firstLayerAllowedCuboidOrientations)
        assertEquals(setOf(Orientation.Upright), policy.lastLayerAllowedCuboidOrientations)
    }

    @Test
    fun materialWidthAmountDynamicDiameterSampleFileShouldBeParsable() {
        val scenario = loadCsvDrivenScenario("gurobi/material-width-amount-dynamic-diameter-sample.csv")
        val cylinderSpecs = scenario.itemDemands.mapNotNull { (item, _) ->
            item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
        }

        assertEquals(1, scenario.groupCount)
        assertEquals(2, cylinderSpecs.size)
        assertTrue(cylinderSpecs.all { spec -> spec.axis == Axis3.Y })
        assertTrue(cylinderSpecs.all { spec -> spec.diameterMin != null && spec.diameterMax != null })
        assertTrue(cylinderSpecs.all { spec -> spec.resolvedRadiusCandidates.size >= 3 })
    }

    @Test
    fun groupedLayerPwlTightBinEnvelopeSampleFileShouldBeParsable() {
        // Tight bin + conservative envelope: 验证 PWL 圆柱在紧凑 bin 中 envelope 不越界
        // Tight bin + conservative envelope: verify PWL cylinder envelope stays within bin bounds
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-pwl-tight-bin-envelope-sample.csv")

        assertEquals(2, scenario.groupCount)
        assertEquals(4, scenario.totalItemCount)

        // 验证 PWL 圆柱配置 / verify PWL cylinder config
        val cylinderItems = scenario.itemDemands
            .map { it.first }
            .filter { item ->
                item.packageShape.shapeSpec is PackageShapeSpec.VerticalCylinder
            }
        assertEquals(2, cylinderItems.size)

        // 验证两个圆柱都有 PWL 区间 / verify both cylinders have PWL intervals
        for (item in cylinderItems) {
            val spec = item.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
            assertNotNull(spec.radiusMin, "${item.id} should have radiusMin")
            assertNotNull(spec.radiusMax, "${item.id} should have radiusMax")
            assertNotNull(spec.radiusWeightFunctionKey, "${item.id} should have weight function key")
            assertTrue(
                spec.radiusMax!!.value.toDouble() > spec.radiusMin!!.value.toDouble(),
                "${item.id} should have valid radius interval"
            )
            // envelope diameter = 2 * rMax 应小于 bin width
            // envelope diameter = 2 * rMax should be less than bin width
            val envelopeDiameter = spec.radiusMax!!.value.toDouble() * 2.0
            assertTrue(
                envelopeDiameter < 2.5,
                "${item.id} envelope diameter $envelopeDiameter should be reasonable"
            )
        }
    }

    @Test
    fun groupedLayerPwlSupportSensitiveSampleFileShouldBeParsable() {
        // Support-sensitive vertical cylinder + PWL: 验证 PWL 圆柱与支撑 cuboid 的组合
        // 注意：grouped-layer known-coordinate 路径不支持 X/Z 横向圆柱
        // （LayerPlacementAdapter 会 guard 横向圆柱，横向圆柱只能通过 CirclePackingLayerGenerator 生成）
        // Support-sensitive vertical cylinder + PWL: verify PWL cylinder with support cuboids
        // Note: grouped-layer known-coordinate path does not support X/Z horizontal cylinders
        // (LayerPlacementAdapter guards horizontal cylinders; they must come from CirclePackingLayerGenerator)
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-pwl-horizontal-support-sample.csv")

        assertEquals(2, scenario.groupCount)
        assertEquals(5, scenario.totalItemCount)

        // 验证 PWL 圆柱配置 / verify PWL cylinder config
        val cylinderItems = scenario.itemDemands
            .map { it.first }
            .filter { item ->
                item.packageShape.shapeSpec is PackageShapeSpec.VerticalCylinder
            }
        assertEquals(2, cylinderItems.size)

        // 验证 PWL 区间和 key / verify PWL intervals and key
        for (item in cylinderItems) {
            val spec = item.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
            assertNotNull(spec.radiusMin, "${item.id} should have radiusMin")
            assertNotNull(spec.radiusMax, "${item.id} should have radiusMax")
            assertNotNull(spec.radiusWeightFunctionKey, "${item.id} should have weight function key")
            // 所有圆柱都是 Y 轴（grouped-layer known-coordinate 路径限制）
            assertEquals(Axis3.Y, spec.axis, "${item.id} should be Y-axis for grouped-layer path")
        }

        // 验证两个圆柱有不同的 PWL 区间 / verify different PWL intervals
        val spec0 = cylinderItems[0].packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
        val spec1 = cylinderItems[1].packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
        assertTrue(
            spec0.radiusMin!!.value.toDouble() != spec1.radiusMin!!.value.toDouble()
                || spec0.radiusMax!!.value.toDouble() != spec1.radiusMax!!.value.toDouble(),
            "Two PWL cylinders should have different intervals"
        )
    }

    @Test
    fun groupedLayerPwlMultiMaterialSampleFileShouldBeParsable() {
        // 多 material / 多 demand 场景：验证跨 group 多 material PWL 圆柱的 program demand、
        // material packing 和 renderer metadata 口径一致
        // Multi-material / multi-demand scenario: verify program demand, material packing,
        // and renderer metadata consistency for cross-group multi-material PWL cylinders
        val scenario = loadCsvDrivenScenario("gurobi/grouped-layer-pwl-multi-material-sample.csv")

        assertEquals(2, scenario.groupCount)
        assertEquals(8, scenario.totalItemCount)

        // 验证 4 个不同 material / verify 4 different materials
        assertEquals(4, scenario.materialCount)

        // 验证 PWL 圆柱配置 / verify PWL cylinder config
        val cylinderItems = scenario.itemDemands
            .map { it.first }
            .filter { item ->
                item.packageShape.shapeSpec is PackageShapeSpec.VerticalCylinder
            }
        assertEquals(4, cylinderItems.size)

        // 验证每个圆柱都有 PWL 区间和 key / verify each cylinder has PWL interval and key
        for (item in cylinderItems) {
            val spec = item.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
            assertNotNull(spec.radiusMin, "${item.id} should have radiusMin")
            assertNotNull(spec.radiusMax, "${item.id} should have radiusMax")
            assertNotNull(spec.radiusWeightFunctionKey, "${item.id} should have weight function key")
        }

        // 验证跨 group 使用相同 material_no 的圆柱有不同的 PWL 区间
        // Verify cylinders sharing material_no across groups have different PWL intervals
        val matACylinders = cylinderItems.filter { item ->
            (item.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder).radiusWeightFunctionKey?.startsWith("cylinder_mma") == true
        }
        assertEquals(2, matACylinders.size)
        val matACylinder0 = matACylinders.first()
        val matACylinder1 = matACylinders.last()
        val spec0 = matACylinder0.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
        val spec1 = matACylinder1.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
        // Group 0: [0.35, 0.45], Group 1: [0.40, 0.50] — 不同区间
        assertTrue(
            spec0.radiusMin!!.value.toDouble() != spec1.radiusMin!!.value.toDouble()
                || spec0.radiusMax!!.value.toDouble() != spec1.radiusMax!!.value.toDouble(),
            "Same material PWL cylinders across groups should have different intervals"
        )
    }

    @Test
    fun materialWidthAmountPwlMultiMaterialSampleFileShouldBeParsable() {
        // Material-width-amount 格式的多 material / 多 demand 场景：
        // 验证 amount > 1 的 PWL 圆柱 demand 和 material packing 口径一致
        // Material-width-amount format multi-material / multi-demand scenario:
        // verify amount > 1 PWL cylinder demand and material packing consistency
        val scenario = loadCsvDrivenScenario("gurobi/material-width-amount-pwl-multi-material-sample.csv")

        assertEquals(1, scenario.groupCount)
        assertEquals(4, scenario.materialCount)

        // 验证 PWL 圆柱配置 / verify PWL cylinder config
        val cylinderItems = scenario.itemDemands
            .map { it.first }
            .filter { item ->
                item.packageShape.shapeSpec is PackageShapeSpec.VerticalCylinder
            }
        assertEquals(2, cylinderItems.size)

        for (item in cylinderItems) {
            val spec = item.packageShape.shapeSpec as PackageShapeSpec.VerticalCylinder
            assertNotNull(spec.radiusMin, "${item.id} should have radiusMin")
            assertNotNull(spec.radiusMax, "${item.id} should have radiusMax")
            assertNotNull(spec.radiusWeightFunctionKey, "${item.id} should have weight function key")
        }

        // 验证 material amount demands / verify material amount demands
        assertTrue(
            scenario.materialAmountDemands.isNotEmpty(),
            "Should have material amount demands"
        )
        // MAT-PWL-MA 的 amount=2, MAT-PWL-MC 的 amount=3
        val totalExpectedAmount = scenario.itemDemands.sumOfLong { it.second.toLong().toLong() }
        assertTrue(
            totalExpectedAmount >= 7L,
            "Total amount should be at least 7 (2 + 1 + 3 + 1): actual=$totalExpectedAmount"
        )
    }

    private fun loadMaterialWidthAmountCsvScenario(lines: List<String>): CsvDrivenScenario {
        val headerColumns = lines.first().split(",").map { it.trim() }
        val headerIndex = headerColumns.withIndex().associate { (index, column) ->
            column.lowercase(Locale.getDefault()) to index
        }
        fun requiredColumn(name: String): Int {
            return headerIndex[name] ?: throw IllegalStateException("missing required csv column: $name")
        }
        fun optionalColumn(name: String): Int? {
            return headerIndex[name]
        }
        fun optionalLengthColumn(name: String): Int? {
            return optionalColumn(name) ?: optionalColumn("${name}_meter")
        }

        val materialColumn = requiredColumn("material")
        val widthColumn = requiredColumn("width")
        val amountColumn = requiredColumn("amount")
        val materialNoColumn = optionalColumn("material_no")
        val materialNameColumn = optionalColumn("material_name")
        val materialWeightColumn = optionalColumn("material_weight_kg")
        val shapeTypeColumn = optionalColumn("shape_type")
        val radiusMeterColumn = optionalColumn("radius_meter")
        val radiusMinMeterColumn = optionalLengthColumn("radius_min")
        val radiusMaxMeterColumn = optionalLengthColumn("radius_max")
        val radiusStepMeterColumn = optionalLengthColumn("radius_step")
        val diameterMinMeterColumn = optionalLengthColumn("diameter_min")
        val diameterMaxMeterColumn = optionalLengthColumn("diameter_max")
        val diameterStepMeterColumn = optionalLengthColumn("diameter_step")
        val radiusWeightFunctionKeyColumn = optionalColumn("radius_weight_function_key")
        val axisColumn = optionalColumn("axis")
        val firstLayerAllowedCylinderAxesColumn = optionalColumn("first_layer_allowed_cylinder_axes")
        val lastLayerAllowedCylinderAxesColumn = optionalColumn("last_layer_allowed_cylinder_axes")
        val firstLayerAllowedCuboidOrientationsColumn = optionalColumn("first_layer_allowed_cuboid_orientations")
        val lastLayerAllowedCuboidOrientationsColumn = optionalColumn("last_layer_allowed_cuboid_orientations")
        val widthScale = optionalFltXProperty("bpp3d.gurobi.dataset.material.width.scale")
            ?: FltX(1000.0)
        val defaultMaterialWeightKg = optionalFltXProperty("bpp3d.gurobi.dataset.material.default.weight.kg")
            ?: FltX.one
        val defaultBinDepth = optionalIntProperty("bpp3d.gurobi.dataset.material.default.bin.depth")
            ?: 240

        val rows = ArrayList<MaterialWidthAmountScenarioRow>()
        for ((index, line) in lines.drop(1).withIndex()) {
            val cols = line.split(",").map { it.trim() }
            val rowIndex = index + 2
            if (cols.size < 3) {
                throw IllegalStateException("invalid csv columns at row $rowIndex: $line")
            }
            val material = cols.getOrNull(materialColumn)
                ?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("empty material at row $rowIndex")
            val width = cols.getOrNull(widthColumn)
                ?.toDoubleOrNull()
                ?.let { FltX(it) }
                ?: throw IllegalStateException("invalid width at row $rowIndex: ${cols.getOrNull(widthColumn)}")
            val amountAsDouble = cols.getOrNull(amountColumn)
                ?.toDoubleOrNull()
                ?: throw IllegalStateException("invalid amount at row $rowIndex: ${cols.getOrNull(amountColumn)}")
            val amount = amountAsDouble.toLong()
            if (amount <= 0L) {
                continue
            }
            rows.add(
                MaterialWidthAmountScenarioRow(
                    material = material,
                    width = width,
                    amount = UInt64(amount.toULong()),
                    materialNo = materialNoColumn?.let { cols.getOrNull(it)?.takeIf { value -> value.isNotEmpty() } },
                    materialName = materialNameColumn?.let { cols.getOrNull(it)?.takeIf { value -> value.isNotEmpty() } },
                    materialWeightKg = materialWeightColumn?.let { cols.getOrNull(it)?.toDoubleOrNull()?.let { value -> FltX(value) } },
                    shapeType = shapeTypeColumn?.let { cols.getOrNull(it)?.takeIf { value -> value.isNotEmpty() } },
                    radiusMeter = optionalCsvFltX(
                        cols = cols,
                        column = radiusMeterColumn,
                        fieldName = "radius_meter",
                        rowIndex = rowIndex
                    ),
                    radiusMinMeter = optionalCsvFltX(
                        cols = cols,
                        column = radiusMinMeterColumn,
                        fieldName = "radius_min",
                        rowIndex = rowIndex
                    ),
                    radiusMaxMeter = optionalCsvFltX(
                        cols = cols,
                        column = radiusMaxMeterColumn,
                        fieldName = "radius_max",
                        rowIndex = rowIndex
                    ),
                    radiusStepMeter = optionalCsvFltX(
                        cols = cols,
                        column = radiusStepMeterColumn,
                        fieldName = "radius_step",
                        rowIndex = rowIndex
                    ),
                    diameterMinMeter = optionalCsvFltX(
                        cols = cols,
                        column = diameterMinMeterColumn,
                        fieldName = "diameter_min",
                        rowIndex = rowIndex
                    ),
                    diameterMaxMeter = optionalCsvFltX(
                        cols = cols,
                        column = diameterMaxMeterColumn,
                        fieldName = "diameter_max",
                        rowIndex = rowIndex
                    ),
                    diameterStepMeter = optionalCsvFltX(
                        cols = cols,
                        column = diameterStepMeterColumn,
                        fieldName = "diameter_step",
                        rowIndex = rowIndex
                    ),
                    radiusWeightFunctionKey = radiusWeightFunctionKeyColumn?.let { column ->
                        cols.getOrNull(column)
                    }?.takeIf { value -> value.isNotEmpty() },
                    axis = axisColumn?.let { cols.getOrNull(it)?.takeIf { value -> value.isNotEmpty() } }
                )
            )
        }
        if (rows.isEmpty()) {
            throw IllegalStateException("csv has no valid demand rows")
        }

        val materialsByNo = LinkedHashMap<String, Material<FltX>>()
        val itemDemands = ArrayList<Pair<ActualItem, UInt64>>()
        val totalAmountByMaterialNo = LinkedHashMap<String, UInt64>()
        val totalWeightByMaterialNo = LinkedHashMap<String, FltX>()
        val widthsInMeter = ArrayList<FltX>()
        var totalAmount = UInt64.zero

        for ((index, row) in rows.withIndex()) {
            val materialNo = row.materialNo ?: "MAT-$index-${row.material.hashCode().toUInt()}"
            val materialName = row.materialName ?: row.material
            val materialWeightKg = row.materialWeightKg ?: defaultMaterialWeightKg
            val material = materialsByNo.getOrPut(materialNo) {
                Material(
                    no = MaterialNo(materialNo),
                    type = MaterialType.RawMaterial,
                    cargo = CargoAttr,
                    name = materialName,
                    weight = materialWeightKg * Kilogram
                )
            }
            val widthInMeter = maxOf(row.width / widthScale, FltX(0.2))
            widthsInMeter.add(widthInMeter)
            val shapeSpec = toPackageShapeSpec(
                shapeType = row.shapeType,
                radiusMeter = row.radiusMeter,
                radiusMinMeter = row.radiusMinMeter,
                radiusMaxMeter = row.radiusMaxMeter,
                radiusStepMeter = row.radiusStepMeter,
                diameterMinMeter = row.diameterMinMeter,
                diameterMaxMeter = row.diameterMaxMeter,
                diameterStepMeter = row.diameterStepMeter,
                radiusWeightFunctionKey = row.radiusWeightFunctionKey,
                axis = row.axis,
                rowDescription = "material=${row.material},row_index=${index + 2}"
            )
            val actualItem = itemFromCsvWidth(
                id = "item-material-width-$index",
                material = material,
                widthInMeter = widthInMeter,
                shapeSpec = shapeSpec
            )
            itemDemands.add(Pair(actualItem, row.amount))
            totalAmount += row.amount
            totalAmountByMaterialNo[materialNo] = (totalAmountByMaterialNo[materialNo] ?: UInt64.zero) + row.amount
            val weightContribution = FltX(row.amount.toLong().toDouble()) * materialWeightKg
            totalWeightByMaterialNo[materialNo] = (totalWeightByMaterialNo[materialNo] ?: FltX.zero) + weightContribution
        }

        val maxWidthInMeter = widthsInMeter.maxOrNull() ?: FltX.one
        val totalDepthInMeter = maxOf(
            FltX(totalAmount.toLong().toDouble()),
            FltX(defaultBinDepth.toLong())
        )
        val binType = BinType(
            width = maxWidthInMeter * Meter,
            height = FltX(3.0) * Meter,
            depth = totalDepthInMeter * Meter,
            capacity = maxOf(FltX(totalAmount.toLong().toDouble()), FltX.one) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-MATERIAL-WIDTH"
        )
        val finalBins: List<Bin<BinLayer, FltX>> = listOf(
            layerBinOf(
                shape = binType,
                units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                batchNo = BatchNo("B-GUROBI-MATERIAL-WIDTH")
            )
        )
        val initialColumns = itemDemands.map { (actualItem, _) ->
            generatedHorizontalSeedLayer(actualItem, binType) ?: run {
                val bin = layerBin(
                    items = listOf(actualItem),
                    typeCode = binType.typeCode,
                    depthInMeter = totalDepthInMeter,
                    binType = binType,
                    widthInMeter = maxWidthInMeter
                )
                val rawLayer = bin.units.first().unit
                BinLayer(
                    iteration = rawLayer.iteration,
                    from = rawLayer.from,
                    bin = binType,
                    shape = rawLayer.shape,
                    units = rawLayer.units
                )
            }
        }

        val materialsByNoView = materialsByNo.mapValues { (_, material) -> material }
        val materialAmountDemands = totalAmountByMaterialNo.map { (materialNo, amount) ->
            val material = materialsByNoView[materialNo]
                ?: throw IllegalStateException("missing material for amount demand: $materialNo")
            Pair(material, amount)
        }.toMap()
        val materialWeightDemands = totalWeightByMaterialNo.map { (materialNo, weightKg) ->
            val material = materialsByNoView[materialNo]
                ?: throw IllegalStateException("missing material for weight demand: $materialNo")
            Pair(material, weightKg * Kilogram)
        }
        val demandEntries = demandEntriesFromItems(items = itemDemands) +
                demandEntriesFromMaterialAmounts(
                    materials = materialAmountDemands.entries.map { entry ->
                        Pair(entry.key, entry.value)
                    }
                ) +
                demandEntriesFromMaterialWeights(materials = materialWeightDemands)
        val depthBoundaryLayerOrientationPolicy = depthBoundaryPolicyFromRows(
            rows = lines.drop(1).map { line -> line.split(",").map { it.trim() } },
            firstLayerAllowedCylinderAxesColumn = firstLayerAllowedCylinderAxesColumn,
            lastLayerAllowedCylinderAxesColumn = lastLayerAllowedCylinderAxesColumn,
            firstLayerAllowedCuboidOrientationsColumn = firstLayerAllowedCuboidOrientationsColumn,
            lastLayerAllowedCuboidOrientationsColumn = lastLayerAllowedCuboidOrientationsColumn
        )

        return CsvDrivenScenario(
            itemDemands = itemDemands,
            demandEntries = demandEntries,
            initialColumns = initialColumns,
            finalBins = finalBins,
            materialAmountDemands = materialAmountDemands,
            groupCount = 1,
            materialCount = materialAmountDemands.size,
            totalLayerCount = initialColumns.size,
            totalItemCount = totalAmount.toInt(),
            packedLayerCount = totalAmount.toInt(),
            depthBoundaryLayerOrientationPolicy = depthBoundaryLayerOrientationPolicy
        )
    }

    private fun toPackageShapeSpec(
        shapeType: String?,
        radiusMeter: FltX?,
        radiusMinMeter: FltX?,
        radiusMaxMeter: FltX?,
        radiusStepMeter: FltX?,
        diameterMinMeter: FltX?,
        diameterMaxMeter: FltX?,
        diameterStepMeter: FltX?,
        radiusWeightFunctionKey: String?,
        axis: String?,
        rowDescription: String
    ): PackageShapeSpec {
        val hasShapeMetadata = radiusMeter != null
                || radiusMinMeter != null
                || radiusMaxMeter != null
                || radiusStepMeter != null
                || diameterMinMeter != null
                || diameterMaxMeter != null
                || diameterStepMeter != null
                || !radiusWeightFunctionKey.isNullOrBlank()
                || !axis.isNullOrBlank()
        if (shapeType.isNullOrBlank() && hasShapeMetadata) {
            throw IllegalStateException(
                "shape_type is required when shape metadata is provided: $rowDescription"
            )
        }
        val normalizedShapeType = shapeType
            ?.trim()
            ?.lowercase(Locale.getDefault())
            ?: return PackageShapeSpec.Cuboid
        if (normalizedShapeType.isEmpty() || normalizedShapeType == "cuboid") {
            return PackageShapeSpec.Cuboid
        }
        if (normalizedShapeType == "vertical_cylinder"
            || normalizedShapeType == "vertical-cylinder"
            || normalizedShapeType == "verticalcylinder"
            || normalizedShapeType == "cylinder"
        ) {
            val resolvedAxis = parseAxis3OrThrow(
                axis = axis,
                rowDescription = rowDescription
            )
            requireConcreteCsvRadiusMetadata(
                radiusMeter = radiusMeter,
                radiusMinMeter = radiusMinMeter,
                radiusMaxMeter = radiusMaxMeter,
                radiusStepMeter = radiusStepMeter,
                diameterMinMeter = diameterMinMeter,
                diameterMaxMeter = diameterMaxMeter,
                diameterStepMeter = diameterStepMeter,
                radiusWeightFunctionKey = radiusWeightFunctionKey,
                axis = resolvedAxis,
                rowDescription = rowDescription
            )
            val resolvedRadiusMeter = radiusMeter
                ?: radiusMinMeter
                ?: diameterMinMeter?.let { it / FltX(2.0) }
                ?: throw IllegalStateException(
                    "missing radius_meter, radius_min, or diameter_min for cylinder row: $rowDescription"
                )
            return PackageShapeSpec.VerticalCylinder(
                radius = resolvedRadiusMeter * Meter,
                axis = resolvedAxis,
                radiusMin = radiusMinMeter?.let { it * Meter },
                radiusMax = radiusMaxMeter?.let { it * Meter },
                radiusWeightFunctionKey = radiusWeightFunctionKey,
                radiusStep = radiusStepMeter?.let { it * Meter },
                diameterMin = diameterMinMeter?.let { it * Meter },
                diameterMax = diameterMaxMeter?.let { it * Meter },
                diameterStep = diameterStepMeter?.let { it * Meter }
            )
        }
        throw IllegalStateException(
            "unsupported shape_type for csv row: shape_type=$shapeType, $rowDescription"
        )
    }

    private fun requireConcreteCsvRadiusMetadata(
        radiusMeter: FltX?,
        radiusMinMeter: FltX?,
        radiusMaxMeter: FltX?,
        radiusStepMeter: FltX?,
        diameterMinMeter: FltX?,
        diameterMaxMeter: FltX?,
        diameterStepMeter: FltX?,
        radiusWeightFunctionKey: String?,
        axis: Axis3,
        rowDescription: String
    ) {
        if (radiusStepMeter != null && (radiusMinMeter == null || radiusMaxMeter == null)) {
            throw IllegalStateException(
                "radius_step requires radius_min and radius_max for cylinder row: $rowDescription"
            )
        }
        if (diameterStepMeter != null && (diameterMinMeter == null || diameterMaxMeter == null)) {
            throw IllegalStateException(
                "diameter_step requires diameter_min and diameter_max for cylinder row: $rowDescription"
            )
        }
        val solverPrototypeResult = try {
            continuousCylinderRadiusSolverPrototype(
                source = "Gurobi CSV",
                radiusWeightFunctionKey = radiusWeightFunctionKey,
                axis = axis,
                selectedRadius = radiusMeter?.let { it * Meter },
                radiusMin = radiusMinMeter?.let { it * Meter },
                radiusMax = radiusMaxMeter?.let { it * Meter },
                diameterMin = diameterMinMeter?.let { it * Meter },
                diameterMax = diameterMaxMeter?.let { it * Meter },
                hasDiscreteRadiusStep = radiusStepMeter != null || diameterStepMeter != null
            )
        } catch (exception: IllegalArgumentException) {
            val keyText = radiusWeightFunctionKey
                ?.takeIf { it.isNotBlank() }
                ?.let { ", key=$it" }
                ?: ""
            throw IllegalStateException(
                "invalid continuous cylinder radius metadata$keyText, $rowDescription: ${exception.message}",
                exception
            )
        }
        if (!solverPrototypeResult.ok) {
            val keyText = radiusWeightFunctionKey
                ?.takeIf { it.isNotBlank() }
                ?.let { ", key=$it" }
                ?: ""
            val failureText = solverPrototypeResult.failureMessage()
                ?.let { ": $it" }
                ?: ""
            throw IllegalStateException(
                "invalid continuous cylinder radius metadata$keyText, $rowDescription$failureText"
            )
        }
        val solverPrototype = solverPrototypeResult.value
        val gapReport = continuousCylinderRadiusOptimizationGapReport(
            source = "Gurobi CSV",
            radiusWeightFunctionKey = radiusWeightFunctionKey,
            hasConcreteSelectedRadius = radiusMeter != null,
            hasDiscreteRadiusStep = radiusStepMeter != null || diameterStepMeter != null,
            hasContinuousRadiusInterval = radiusMeter == null
                    && radiusMinMeter != null
                    && radiusMaxMeter != null
                    && radiusStepMeter == null,
            hasContinuousDiameterInterval = radiusMeter == null
                    && diameterMinMeter != null
                    && diameterMaxMeter != null
                    && diameterStepMeter == null
        )
        if (gapReport != null) {
            // 允许 PWL 可注册原型通过（interval-only 连续半径可通过 PWL 近似处理），
            // 但必须提供 radius_weight_function_key 以确保生产回写路径可用。
            // PWL 路径允许 MissingSelectedRadius gap（因为 PWL 就是由 solver 选择半径）。
            //
            // Allow PWL-registerable prototypes to pass (interval-only continuous radius
            // can be handled via PWL approximation), but require radius_weight_function_key
            // to ensure the production writeback path is available.
            // PWL path allows MissingSelectedRadius gap (since PWL is exactly about
            // letting the solver choose the radius).
            val isPWLRegisterableWithKey = solverPrototype != null
                    && solverPrototype.isPWLRegisterable
                    && solverPrototype.radiusWeightFunctionKey != null
            if (!isPWLRegisterableWithKey) {
                throw IllegalStateException(gapReport.message(rowDescription) + (solverPrototype?.messageSuffix() ?: ""))
            }
        }
    }

    private fun optionalIntProperty(name: String): Int? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toIntOrNull()
    }

    private fun optionalFltXProperty(name: String): FltX? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
            ?.let { FltX(it) }
    }

    private fun optionalStringProperty(name: String): String? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun loadCsvDrivenScenarioSuiteByProperties(
        pathsPropertyName: String,
        directoryPropertyName: String
    ): List<CsvDrivenScenarioCase> {
        val explicitPaths = optionalStringProperty(pathsPropertyName)
            ?.split(',', ';')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        if (explicitPaths.isNotEmpty()) {
            return explicitPaths.map { filePath ->
                val file = File(filePath)
                if (!file.exists() || !file.isFile) {
                    throw IllegalStateException("invalid dataset file path: $filePath")
                }
                val report = precheckCsvSchemaFromFile(file)
                printCsvSchemaPrecheckReport(report)
                CsvDrivenScenarioCase(
                    name = filePath,
                    scenario = loadCsvDrivenScenarioFromCsvText(
                        csv = file.readText(),
                        sourceName = file.absolutePath,
                        declaredScenarioKind = report.declaredScenarioKind
                    )
                )
            }
        }

        val directoryPath = optionalStringProperty(directoryPropertyName)
            ?: throw IllegalStateException(
                "missing dataset suite properties: $pathsPropertyName or $directoryPropertyName"
            )
        val directory = resolveExistingDatasetDirectory(directoryPath)
        if (directory == null) {
            throw IllegalStateException("invalid dataset directory path: $directoryPath")
        }
        val files = directory
            .listFiles { file ->
                file.isFile && file.extension.equals(
                    other = "csv",
                    ignoreCase = true
                )
            }
            ?.sortedBy { it.name }
            ?: emptyList()
        if (files.isEmpty()) {
            throw IllegalStateException("no csv files found in dataset directory: $directoryPath")
        }
        return files.map { file ->
            val report = precheckCsvSchemaFromFile(file)
            printCsvSchemaPrecheckReport(report)
            CsvDrivenScenarioCase(
                name = file.absolutePath,
                scenario = loadCsvDrivenScenarioFromCsvText(
                    csv = file.readText(),
                    sourceName = file.absolutePath,
                    declaredScenarioKind = report.declaredScenarioKind
                )
            )
        }
    }

    private fun resolveExistingDatasetDirectory(directoryPath: String): File? {
        val candidates = LinkedHashSet<File>()
        val rawPath = directoryPath.trim()
        candidates.add(File(rawPath))
        if (!File(rawPath).isAbsolute) {
            candidates.add(File(".", rawPath))
            candidates.add(File("..", rawPath))
            val normalized = rawPath.replace('\\', '/')
            val frameworkPrefix = "ospf-kotlin-framework-bpp3d/"
            if (normalized.startsWith(frameworkPrefix)) {
                val trimmed = normalized.removePrefix(frameworkPrefix)
                candidates.add(File(trimmed))
                candidates.add(File("..", trimmed))
            }
        }
        return candidates.firstOrNull { it.exists() && it.isDirectory }
    }

    private fun createDeterministicRandomCsvDrivenScenarioCases(
        seed: Int,
        caseCount: Int
    ): List<CsvDrivenScenarioCase> {
        if (caseCount <= 0) {
            throw IllegalArgumentException("caseCount must be positive")
        }
        val random = Random(seed)
        return (0 until caseCount).map { caseIndex ->
            val groupCount = random.nextInt(
                from = 2,
                until = 4
            )
            val materialCount = random.nextInt(
                from = 3,
                until = 6
            )
            val materials = (0 until materialCount).map { materialIndex ->
                val weight = 0.8 + random.nextInt(
                    from = 0,
                    until = 221
                ) / 100.0
                Triple(
                    "MAT-R-$caseIndex-$materialIndex",
                    "Material-R-$caseIndex-$materialIndex",
                    weight
                )
            }
            val lines = ArrayList<String>()
            lines.add("group_index,layer_index,item_id,material_no,material_name,material_weight_kg")
            var itemIndex = 0
            for (groupIndex in 0 until groupCount) {
                val layerCount = random.nextInt(
                    from = 2,
                    until = 5
                )
                for (layerIndex in 0 until layerCount) {
                    val itemsPerLayer = random.nextInt(
                        from = 4,
                        until = 9
                    )
                    repeat(itemsPerLayer) {
                        val material = materials[random.nextInt(materials.size)]
                        val weightText = String.format(
                            Locale.US,
                            "%.2f",
                            material.third
                        )
                        lines.add(
                            "$groupIndex,$layerIndex,item-r$caseIndex-g$groupIndex-l$layerIndex-i$itemIndex,${material.first},${material.second},$weightText"
                        )
                        itemIndex += 1
                    }
                }
            }
            CsvDrivenScenarioCase(
                name = "random-case-${caseIndex + 1}",
                scenario = loadCsvDrivenScenarioFromCsvText(
                    csv = lines.joinToString(separator = "\n")
                )
            )
        }
    }

    @Test
    fun standardExecutorsShouldWorkWithGurobiDelegate() = runBlocking {
        val material = Material(
            no = MaterialNo("M-GUROBI"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI",
            weight = FltX(0.5) * Kilogram
        )
        val actualItem = item("item-gurobi", material)
        val seedBin = layerBin(listOf(actualItem))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.type,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val demandValue = FltX.one
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
                    FltX
                ).value!!
            )
        )

        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(time = 10.seconds)
        )
        val executors = ColumnGenerationStandardExecutors.fromDemandEntries(
            solver = solver,
            itemDemands = listOf(Pair(actualItem, UInt64.one)),
            demandEntries = demandEntries,
            finalBins = listOf(
                layerBinOf(
                    shape = seedBin.type,
                    units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                    batchNo = seedBin.batchNo
                )
            )
        )

        var capturedRequest: Bpp3dLayerGenerationRequest<FltX>? = null
        var analyzedState: ColumnGenerationState<FltX>? = null
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<FltX> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                    capturedRequest = request
                    return emptyList()
                }
            },
            rmpSolver = executors.rmpSolver(),
            finalMilpSolver = executors.finalSolver(),
            layerRequestBuilder = executors.requestBuilder(),
            solutionAnalyzer = ColumnGenerationSolutionAnalyzer { state ->
                analyzedState = state
                ok
            },
            initialColumns = { listOf(seedLayer) }
        )

        val result = algorithm.solve(items = listOf<Item>(actualItem))
            .orFail("Gurobi column generation algorithm should solve")
        assertNotNull(capturedRequest)
        assertNotNull(analyzedState)
        assertTrue(result.finalSolved)
        assertEquals(1, result.lpSolvedTimes)
        assertTrue(result.lpObjectives.isNotEmpty())
        assertTrue(capturedRequest!!.scoreByShadowPrice != null)
    }

    @Test
    fun applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer() = runBlocking {
        val material = Material(
            no = MaterialNo("M-GUROBI-SVC"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-SVC",
            weight = FltX(0.5) * Kilogram
        )
        val actualItem = item("item-gurobi-svc", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val demandValue = FltX.one
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
                    FltX
                ).value!!
            )
        )

        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(time = 10.seconds)
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = listOf(Pair(actualItem, UInt64.one)),
                demandEntries = demandEntries,
                initialColumns = listOf(seedLayer),
                finalBins = listOf(
                    layerBinOf(
                        shape = seedBin.type,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = seedBin.batchNo
                    )
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi column generation application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        assertNotNull(response.packingSnapshot)
        assertTrue(response.packingSnapshot!!.bins.isNotEmpty())
        assertEquals(response.packingSnapshot!!.bins.size.toString(), response.packingSnapshot!!.schema.kpi["bin_count"])
    }

    @Test
    fun applicationServiceShouldSelectGeneratedHorizontalCylinderCandidatesWithGurobi() = runBlocking {
        val materialX = Material(
            no = MaterialNo("M-GUROBI-HORIZONTAL-X"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-HORIZONTAL-X",
            weight = FltX.one * Kilogram
        )
        val materialZ = Material(
            no = MaterialNo("M-GUROBI-HORIZONTAL-Z"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-HORIZONTAL-Z",
            weight = FltX.one * Kilogram
        )
        val itemX = horizontalCylinderItem(
            id = "item-gurobi-horizontal-x",
            material = materialX,
            axis = Axis3.X,
            radiusInMeter = FltX(0.25),
            lengthInMeter = FltX(0.8)
        )
        val itemZ = horizontalCylinderItem(
            id = "item-gurobi-horizontal-z",
            material = materialZ,
            axis = Axis3.Z,
            radiusInMeter = FltX(0.25),
            lengthInMeter = FltX(0.8)
        )
        val binType = BinType(
            width = FltX(1.6) * Meter,
            height = FltX(0.6) * Meter,
            depth = FltX(1.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-HORIZONTAL"
        )
        val itemDemands = listOf(
            Pair(itemX, UInt64.one),
            Pair(itemZ, UInt64.one)
        )
        val initialColumns = listOf(
            generatedHorizontalGridLayer(itemX, binType),
            generatedHorizontalGridLayer(itemZ, binType)
        )
        val finalBins = listOf(
            layerBinOf(
                shape = binType,
                units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                batchNo = BatchNo("B-GUROBI-HORIZONTAL-X")
            ),
            layerBinOf(
                shape = binType,
                units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                batchNo = BatchNo("B-GUROBI-HORIZONTAL-Z")
            )
        )

        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(time = 10.seconds)
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntriesFromItems(items = itemDemands),
                initialColumns = initialColumns,
                finalBins = finalBins,
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 1,
                    maxColumnsPerIteration = 16
                ),
                generators = listOf(CirclePackingLayerGenerator())
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi horizontal cylinder application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertEquals("2", response.result.finalInfo["selected_layer_count"])
        val selectedAxes = response.result.columns.mapNotNull { layer ->
            layer.units.singleOrNull()?.resolvedPackingShape() as? CylinderPackingShape3
        }.filter { shape -> shape.axis != Axis3.Y }
            .map { shape -> shape.axis }
            .toSet()
        assertEquals(setOf(Axis3.X, Axis3.Z), selectedAxes)
        assertTrue(
            response.result.columns.all { layer ->
                layer.from == CirclePackingLayerGenerator::class && layer.units.size == 1
            }
        )

        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(2, snapshot.bins.size)
        assertEquals(2, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        val renderedItems = snapshot.schema.loadingPlans.flatMap { plan -> plan.items }
        assertTrue(
            renderedItems.any { item ->
                item.algorithmShapeType == RenderAlgorithmShapeTypeDTO.HorizontalCylinderX
                        && item.axis == RenderAxis3DTO.X
            }
        )
        assertTrue(
            renderedItems.any { item ->
                item.algorithmShapeType == RenderAlgorithmShapeTypeDTO.HorizontalCylinderZ
                        && item.axis == RenderAxis3DTO.Z
            }
        )
    }

    @Test
    fun applicationServiceShouldSelectGeneratedHorizontalCylinderHeterogeneousSupportStackWithGurobi() = runBlocking {
        val supportMaterialA = Material(
            no = MaterialNo("M-GUROBI-HORIZONTAL-HETEROGENEOUS-SUPPORT-A"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-HORIZONTAL-HETEROGENEOUS-SUPPORT-A",
            weight = FltX.one * Kilogram
        )
        val supportMaterialB = Material(
            no = MaterialNo("M-GUROBI-HORIZONTAL-HETEROGENEOUS-SUPPORT-B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-HORIZONTAL-HETEROGENEOUS-SUPPORT-B",
            weight = FltX.one * Kilogram
        )
        val cylinderMaterial = Material(
            no = MaterialNo("M-GUROBI-HORIZONTAL-HETEROGENEOUS-CYLINDER"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-HORIZONTAL-HETEROGENEOUS-CYLINDER",
            weight = FltX.one * Kilogram
        )
        val supportA = item(
            id = "item-gurobi-horizontal-heterogeneous-support-a",
            material = supportMaterialA,
            widthInMeter = FltX(0.4)
        )
        val supportB = item(
            id = "item-gurobi-horizontal-heterogeneous-support-b",
            material = supportMaterialB,
            widthInMeter = FltX(0.6)
        )
        val cylinder = horizontalCylinderItem(
            id = "item-gurobi-horizontal-heterogeneous-cylinder",
            material = cylinderMaterial,
            axis = Axis3.X,
            radiusInMeter = FltX(0.5),
            lengthInMeter = FltX.one
        )
        val binType = BinType(
            width = FltX(1.2) * Meter,
            height = FltX(2.2) * Meter,
            depth = FltX.one * Meter,
            capacity = FltX(30.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-HORIZONTAL-HETEROGENEOUS-STACK"
        )
        val itemDemands = listOf(
            Pair(supportA, UInt64.one),
            Pair(supportB, UInt64.one),
            Pair(cylinder, UInt64.one)
        )
        val supportedStackLayer = generatedHorizontalLayerBySource(
            items = listOf(supportA, supportB, cylinder),
            binType = binType,
            source = "circle-packing-horizontal-supported-stack-heterogeneous-axis=x"
        )

        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(time = 10.seconds)
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntriesFromItems(items = itemDemands),
                initialColumns = listOf(supportedStackLayer),
                finalBins = listOf(
                    layerBinOf(
                        shape = binType,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = BatchNo("B-GUROBI-HORIZONTAL-HETEROGENEOUS-STACK")
                    )
                ),
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 1,
                    maxColumnsPerIteration = 16
                ),
                generators = listOf(CirclePackingLayerGenerator())
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi heterogeneous horizontal cylinder application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertEquals("1", response.result.finalInfo["selected_layer_count"])
        assertTrue(
            response.result.columns.any { layer ->
                layer.from == CirclePackingLayerGenerator::class && layer.units.size == 3
            }
        )
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(1, snapshot.bins.size)
        assertEquals(3, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        val renderedItems = snapshot.schema.loadingPlans.flatMap { plan -> plan.items }
        assertTrue(
            renderedItems.any { item ->
                item.algorithmShapeType == RenderAlgorithmShapeTypeDTO.HorizontalCylinderX
                        && item.axis == RenderAxis3DTO.X
            }
        )
    }

    @Test
    fun applicationServiceShouldKeepPackingConsistentForMultiMaterialWithGurobi() = runBlocking {
        val materialA = Material(
            no = MaterialNo("M-GUROBI-A"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-A",
            weight = FltX(1.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("M-GUROBI-B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-B",
            weight = FltX(1.0) * Kilogram
        )
        val itemA = item("item-gurobi-a", materialA)
        val itemB = item("item-gurobi-b", materialB)
        val seedBin = layerBin(
            items = listOf(itemA, itemB),
            typeCode = "BIN-GUROBI-MULTI"
        )
        val seedLayer = seedBin.units.first().unit
        val itemDemands = listOf(
            Pair(itemA, UInt64.one),
            Pair(itemB, UInt64.one)
        )
        val demandEntries = demandEntriesFromItems(
            items = itemDemands
        )
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(time = 10.seconds)
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntries,
                initialColumns = listOf(seedLayer),
                finalBins = listOf(
                    layerBinOf(
                        shape = seedBin.type,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = seedBin.batchNo
                    )
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi multi-material application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(1, snapshot.bins.size)
        assertEquals(1, snapshot.bins.sumOfInt { bin -> bin.units.size })
        assertEquals(1, snapshot.packingResult.aggregation.bins.size)
        assertEquals(2, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        assertEquals(2, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        assertEquals(UInt64.one, materialSummary[materialA.key])
        assertEquals(UInt64.one, materialSummary[materialB.key])
        assertEquals("1", snapshot.schema.kpi["bin_count"])
        assertEquals("2", snapshot.schema.kpi["material_count"])
    }

    @Test
    fun applicationServiceShouldHandleProductionLikeConfigOnMediumScaleScenario() = runBlocking {
        val materialCount = 4
        val layerCount = 2
        val itemsPerLayer = 12
        val finalBinCount = 1
        val sharedBinType = BinType(
            width = FltX(itemsPerLayer.toDouble()) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(2.0) * Meter,
            capacity = FltX(300.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-MEDIUM"
        )
        val materials = (0 until materialCount).map { index ->
            Material(
                no = MaterialNo("M-GUROBI-MEDIUM-$index"),
                type = MaterialType.RawMaterial,
                cargo = CargoAttr,
                name = "M-GUROBI-MEDIUM-$index",
                weight = FltX(1.0) * Kilogram
            )
        }
        val items = (0 until (layerCount * itemsPerLayer)).map { index ->
            val material = materials[index % materialCount]
            item(id = "item-gurobi-medium-$index", material = material)
        }
        val layers = items.chunked(itemsPerLayer).map { chunk ->
            val bin = layerBin(
                items = chunk,
                typeCode = sharedBinType.typeCode,
                depthInMeter = FltX(2.0),
                binType = sharedBinType,
                widthInMeter = FltX(itemsPerLayer.toLong())
            )
            val rawLayer = bin.units.first().unit
            BinLayer(
                iteration = rawLayer.iteration,
                from = rawLayer.from,
                bin = sharedBinType,
                shape = rawLayer.shape,
                units = rawLayer.units
            )
        }
        val itemDemands = items.map { Pair(it, UInt64.one) }
        val demandEntries = demandEntriesFromItems(
            items = itemDemands
        )
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(
                time = 20.seconds,
                threadNum = UInt64(2),
                gap = SolverFlt64(0.01),
                notImprovementTime = 5.seconds
            )
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntries,
                initialColumns = layers,
                finalBins = (0 until finalBinCount).map { index ->
                    layerBinOf(
                        shape = sharedBinType,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = BatchNo("B-GUROBI-MEDIUM-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 64
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = FltX(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi medium production-like application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(finalBinCount, snapshot.bins.size)
        assertEquals(layerCount, snapshot.bins.sumOfInt { bin -> bin.units.size })
        assertEquals(1, snapshot.packingResult.aggregation.bins.size)
        assertEquals(layerCount * itemsPerLayer, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        assertEquals(materialCount, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        val expectedMaterialAmount = UInt64((layerCount * itemsPerLayer) / materialCount)
        for (material in materials) {
            assertEquals(expectedMaterialAmount, materialSummary[material.key])
        }
        assertEquals(finalBinCount.toString(), snapshot.schema.kpi["bin_count"])
        assertEquals(materialCount.toString(), snapshot.schema.kpi["material_count"])
    }

    @Test
    fun applicationServiceShouldHandleProductionLikeConfigOnLargeScaleScenario() = runBlocking {
        val materialCount = 6
        val layerCount = 6
        val itemsPerLayer = 10
        val finalBinCount = 1
        val sharedBinType = BinType(
            width = FltX(itemsPerLayer.toDouble()) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(layerCount.toDouble()) * Meter,
            capacity = FltX(600.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-LARGE"
        )
        val materials = (0 until materialCount).map { index ->
            Material(
                no = MaterialNo("M-GUROBI-LARGE-$index"),
                type = MaterialType.RawMaterial,
                cargo = CargoAttr,
                name = "M-GUROBI-LARGE-$index",
                weight = FltX(1.0) * Kilogram
            )
        }
        val items = (0 until (layerCount * itemsPerLayer)).map { index ->
            val material = materials[index % materialCount]
            item(id = "item-gurobi-large-$index", material = material)
        }
        val layers = items.chunked(itemsPerLayer).map { chunk ->
            val bin = layerBin(
                items = chunk,
                typeCode = sharedBinType.typeCode,
                depthInMeter = FltX(layerCount.toLong()),
                binType = sharedBinType,
                widthInMeter = FltX(itemsPerLayer.toLong())
            )
            val rawLayer = bin.units.first().unit
            BinLayer(
                iteration = rawLayer.iteration,
                from = rawLayer.from,
                bin = sharedBinType,
                shape = rawLayer.shape,
                units = rawLayer.units
            )
        }
        val itemDemands = items.map { Pair(it, UInt64.one) }
        val demandEntries = demandEntriesFromItems(
            items = itemDemands
        )
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(
                time = 30.seconds,
                threadNum = UInt64(4),
                gap = SolverFlt64(0.01),
                notImprovementTime = 10.seconds
            )
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntries,
                initialColumns = layers,
                finalBins = (0 until finalBinCount).map { index ->
                    layerBinOf(
                        shape = sharedBinType,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = BatchNo("B-GUROBI-LARGE-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 64
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = FltX(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi large production-like application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(finalBinCount, snapshot.bins.size)
        assertEquals(layerCount, snapshot.bins.sumOfInt { bin -> bin.units.size })
        assertEquals(1, snapshot.packingResult.aggregation.bins.size)
        assertEquals(layerCount * itemsPerLayer, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        assertEquals(materialCount, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        val expectedMaterialAmount = UInt64((layerCount * itemsPerLayer) / materialCount)
        for (material in materials) {
            assertEquals(expectedMaterialAmount, materialSummary[material.key])
        }
        assertEquals(finalBinCount.toString(), snapshot.schema.kpi["bin_count"])
        assertEquals(materialCount.toString(), snapshot.schema.kpi["material_count"])
    }

    @Test
    fun applicationServiceShouldHandleLargeScaleMixedDemandScenario() = runBlocking {
        val materialCount = 5
        val groupCount = 2
        val layersPerGroup = 6
        val itemsPerLayer = 10
        val totalLayerCount = groupCount * layersPerGroup
        val totalItemCount = totalLayerCount * itemsPerLayer
        val materials = (0 until materialCount).map { index ->
            Material(
                no = MaterialNo("M-GUROBI-MIXED-$index"),
                type = MaterialType.RawMaterial,
                cargo = CargoAttr,
                name = "M-GUROBI-MIXED-$index",
                weight = FltX(1.0) * Kilogram
            )
        }
        val binTypes = (0 until groupCount).map { index ->
            BinType(
                width = FltX(itemsPerLayer.toDouble()) * Meter,
                height = FltX(3.0) * Meter,
                depth = FltX(layersPerGroup.toDouble()) * Meter,
                capacity = FltX(600.0) * Kilogram,
                longitudinalBalance = null,
                lateralBalance = null,
                typeCode = "BIN-GUROBI-MIXED-$index"
            )
        }
        val layers = ArrayList<BinLayer>()
        val items = ArrayList<ActualItem>()
        var globalItemIndex = 0
        for (groupIndex in 0 until groupCount) {
            for (layerIndex in 0 until layersPerGroup) {
                val layerItems = (0 until itemsPerLayer).map {
                    val material = materials[globalItemIndex % materialCount]
                    val actualItem = item(
                        id = "item-gurobi-mixed-$globalItemIndex",
                        material = material
                    )
                    globalItemIndex += 1
                    items.add(actualItem)
                    actualItem
                }
                val bin = layerBin(
                    items = layerItems,
                    typeCode = binTypes[groupIndex].typeCode,
                    depthInMeter = FltX(layersPerGroup.toLong()),
                    binType = binTypes[groupIndex],
                    widthInMeter = FltX(itemsPerLayer.toLong())
                )
                val rawLayer = bin.units.first().unit
                layers.add(
                    BinLayer(
                        iteration = rawLayer.iteration,
                        from = rawLayer.from,
                        bin = binTypes[groupIndex],
                        shape = rawLayer.shape,
                        units = rawLayer.units
                    )
                )
            }
        }
        val itemDemands = items.map { Pair(it, UInt64.one) }
        val materialDemands = materials.associateWith {
            UInt64(totalItemCount / materialCount)
        }
        val demandEntries = demandEntriesFromItems(items = itemDemands) +
                demandEntriesFromMaterialAmounts(
                    materials = materialDemands.entries.map { entry ->
                        Pair(entry.key, entry.value)
                    }
                )
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(
                time = 30.seconds,
                threadNum = UInt64(4),
                gap = SolverFlt64(0.01),
                notImprovementTime = 10.seconds
            )
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntries,
                initialColumns = layers,
                finalBins = binTypes.mapIndexed { index, binType ->
                    layerBinOf(
                        shape = binType,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = BatchNo("B-GUROBI-MIXED-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 64
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = FltX(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi mixed demand application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(groupCount, snapshot.bins.size)
        assertEquals(totalLayerCount, snapshot.bins.sumOfInt { bin -> bin.units.size })
        assertEquals(groupCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(totalItemCount, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        assertEquals(materialCount, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        for ((material, demand) in materialDemands) {
            assertEquals(demand, materialSummary[material.key])
        }
        assertEquals(groupCount.toString(), snapshot.schema.kpi["bin_count"])
        assertEquals(materialCount.toString(), snapshot.schema.kpi["material_count"])
    }

    @Test
    fun applicationServiceShouldSupportCsvDrivenProductionLikeScenario() = runBlocking {
        val scenario = loadCsvDrivenScenarioByPropertyOrResource(
            propertyName = "bpp3d.gurobi.dataset.path",
            defaultResourcePath = "gurobi/production-like-dataset.csv"
        )
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = buildSolverConfig(prefix = "bpp3d.gurobi.dataset")
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = scenario.itemDemands,
                demandEntries = scenario.demandEntries,
                initialColumns = scenario.initialColumns,
                finalBins = scenario.finalBins,
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 96
                ),
                depthBoundaryLayerOrientationPolicy = scenario.depthBoundaryLayerOrientationPolicy,
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = FltX(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi CSV-driven application service should solve")
        val expectedGroupCount = optionalIntProperty("bpp3d.gurobi.dataset.expected.group.count")
            ?: scenario.groupCount
        val expectedMaterialCount = optionalIntProperty("bpp3d.gurobi.dataset.expected.material.count")
            ?: scenario.materialCount
        val expectedLayerCount = optionalIntProperty("bpp3d.gurobi.dataset.expected.layer.count")
            ?: scenario.totalLayerCount
        val expectedPackedLayerCount = optionalIntProperty("bpp3d.gurobi.dataset.expected.packed.layer.count")
            ?: scenario.packedLayerCount
        val expectedItemCount = optionalIntProperty("bpp3d.gurobi.dataset.expected.item.count")
            ?: scenario.totalItemCount
        val maxElapsedSeconds = optionalFltXProperty("bpp3d.gurobi.dataset.max.elapsed.seconds")
            ?: FltX(120.0)
        val maxMilpGap = optionalFltXProperty("bpp3d.gurobi.dataset.max.milp.gap")
            ?: FltX(0.05)
        val maxLpGap = optionalFltXProperty("bpp3d.gurobi.dataset.max.lp.gap")
            ?: FltX(0.05)

        printScenarioMetrics(
            caseName = "single-dataset",
            response = response
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        assertTrue(response.result.elapsed > 0.seconds)
        assertTrue(response.result.elapsed <= maxElapsedSeconds.toDouble().seconds)
        assertEquals(1, response.result.lpInfos.size)
        assertEquals("gurobi", response.result.lpInfos.first()["solver"])
        assertEquals("gurobi", response.result.finalInfo["solver"])
        assertTrue((response.result.finalInfo["milp_time_ms"] ?: "0").toLong() >= 0L)
        response.result.lpInfos.first()["lp_gap"]
            ?.toDoubleOrNull()
            ?.let { lpGap ->
                assertTrue(FltX(lpGap) <= maxLpGap)
            }
        response.result.finalInfo["milp_gap"]
            ?.toDoubleOrNull()
            ?.let { milpGap ->
                assertTrue(FltX(milpGap) <= maxMilpGap)
            }
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(expectedGroupCount, snapshot.bins.size)
        assertEquals(expectedPackedLayerCount, snapshot.bins.sumOfInt { bin -> bin.units.size })
        assertEquals(expectedGroupCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(expectedItemCount, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        assertEquals(expectedMaterialCount, snapshot.packingResult.materialSummary.size)
        assertEquals(expectedGroupCount, snapshot.schema.loadingPlans.size)
        assertEquals(expectedItemCount, snapshot.schema.loadingPlans.sumOfInt { plan -> plan.items.size })
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        for ((material, demand) in scenario.materialAmountDemands) {
            assertEquals(demand, materialSummary[material.key])
        }
        assertEquals(expectedGroupCount.toString(), response.result.finalInfo["selected_bin_count"])
        assertEquals(expectedLayerCount.toString(), response.result.finalInfo["selected_layer_count"])
        assertEquals(response.result.iterationCount.toString(), snapshot.schema.kpi["cg_iteration"])
        assertEquals(expectedGroupCount.toString(), snapshot.schema.kpi["bin_count"])
        assertEquals(expectedMaterialCount.toString(), snapshot.schema.kpi["material_count"])
    }

    @Test
    @EnabledIfSystemProperty(named = "bpp3d.gurobi.dataset.suite.enabled", matches = "true")
    fun applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite() = runBlocking {
        val scenarios = loadCsvDrivenScenarioSuiteByProperties(
            pathsPropertyName = "bpp3d.gurobi.dataset.suite.paths",
            directoryPropertyName = "bpp3d.gurobi.dataset.suite.dir"
        )
        val expectedCaseCount = optionalIntProperty("bpp3d.gurobi.dataset.suite.expected.case.count")
            ?: scenarios.size
        val maxElapsedSeconds = optionalFltXProperty("bpp3d.gurobi.dataset.suite.max.elapsed.seconds")
            ?: FltX(180.0)
        val maxTotalElapsedSeconds = optionalFltXProperty("bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds")
            ?: FltX(600.0)
        val maxMilpGap = optionalFltXProperty("bpp3d.gurobi.dataset.suite.max.milp.gap")
            ?: FltX(0.05)
        val maxLpGap = optionalFltXProperty("bpp3d.gurobi.dataset.suite.max.lp.gap")
            ?: FltX(0.05)
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = buildSolverConfig(prefix = "bpp3d.gurobi.dataset.suite")
        )
        val service = ColumnGenerationApplicationService(solver)
        var totalElapsed = ZERO

        assertEquals(expectedCaseCount, scenarios.size)
        for (scenarioCase in scenarios) {
            val scenario = scenarioCase.scenario
            val response = service.solve(
                request = ColumnGenerationApplicationRequest(
                    itemDemands = scenario.itemDemands,
                    demandEntries = scenario.demandEntries,
                    initialColumns = scenario.initialColumns,
                    finalBins = scenario.finalBins,
                    cgConfig = ColumnGenerationConfig(
                        iterationLimit = 4,
                        maxColumnsPerIteration = 96
                    ),
                    depthBoundaryLayerOrientationPolicy = scenario.depthBoundaryLayerOrientationPolicy,
                    executorConfig = ColumnGenerationStandardExecutorConfig(
                        integralityTolerance = FltX(1e-5)
                    ),
                    generators = listOf(
                        object : Bpp3dLayerGenerator<FltX> {
                            override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                                return emptyList()
                            }
                        }
                    )
                ),
                packingAnalyzer = ColumnGenerationPackingAnalyzer()
            ).orFail("Gurobi CSV-driven suite application service should solve: ${scenarioCase.name}")
            totalElapsed += response.result.elapsed
            printScenarioMetrics(
                caseName = scenarioCase.name,
                response = response
            )

            assertTrue(response.result.finalSolved, scenarioCase.name)
            assertEquals(1, response.result.lpSolvedTimes, scenarioCase.name)
            assertTrue(response.result.lpObjectives.isNotEmpty(), scenarioCase.name)
            assertTrue(response.result.elapsed > 0.seconds, scenarioCase.name)
            assertTrue(response.result.elapsed <= maxElapsedSeconds.toDouble().seconds, scenarioCase.name)
            assertEquals(1, response.result.lpInfos.size, scenarioCase.name)
            assertEquals("gurobi", response.result.lpInfos.first()["solver"], scenarioCase.name)
            assertEquals("gurobi", response.result.finalInfo["solver"], scenarioCase.name)
            response.result.lpInfos.first()["lp_gap"]
                ?.toDoubleOrNull()
                ?.let { lpGap ->
                    assertTrue(
                        FltX(lpGap) <= maxLpGap,
                        "${scenarioCase.name}: lp_gap=$lpGap exceeds $maxLpGap"
                    )
                }
            response.result.finalInfo["milp_gap"]
                ?.toDoubleOrNull()
                ?.let { milpGap ->
                    assertTrue(
                        FltX(milpGap) <= maxMilpGap,
                        "${scenarioCase.name}: milp_gap=$milpGap exceeds $maxMilpGap"
                    )
                }
            val snapshot = response.packingSnapshot
            assertNotNull(snapshot, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.bins.size, scenarioCase.name)
            assertEquals(scenario.packedLayerCount, snapshot.bins.sumOfInt { bin -> bin.units.size }, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.packingResult.aggregation.bins.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size }, scenarioCase.name)
            assertEquals(scenario.materialCount, snapshot.packingResult.materialSummary.size, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.schema.loadingPlans.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.schema.loadingPlans.sumOfInt { plan -> plan.items.size }, scenarioCase.name)
            val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
                entry.material to entry.amount
            }
            for ((material, demand) in scenario.materialAmountDemands) {
                assertEquals(demand, materialSummary[material.key], scenarioCase.name)
            }
            assertEquals(scenario.groupCount.toString(), response.result.finalInfo["selected_bin_count"], scenarioCase.name)
            assertEquals(scenario.totalLayerCount.toString(), response.result.finalInfo["selected_layer_count"], scenarioCase.name)
            assertEquals(response.result.iterationCount.toString(), snapshot.schema.kpi["cg_iteration"], scenarioCase.name)
            assertEquals(scenario.groupCount.toString(), snapshot.schema.kpi["bin_count"], scenarioCase.name)
            assertEquals(scenario.materialCount.toString(), snapshot.schema.kpi["material_count"], scenarioCase.name)
        }

        assertTrue(totalElapsed <= maxTotalElapsedSeconds.toDouble().seconds)
    }

    @Test
    fun applicationServiceShouldSupportDeterministicRandomScenarioSuite() = runBlocking {
        val seed = optionalIntProperty("bpp3d.gurobi.random.dataset.seed")
            ?: 20260527
        val caseCount = optionalIntProperty("bpp3d.gurobi.random.dataset.case.count")
            ?: 3
        val scenarios = createDeterministicRandomCsvDrivenScenarioCases(
            seed = seed,
            caseCount = caseCount
        )
        val maxElapsedSeconds = optionalFltXProperty("bpp3d.gurobi.random.dataset.max.elapsed.seconds")
            ?: FltX(120.0)
        val maxTotalElapsedSeconds = optionalFltXProperty("bpp3d.gurobi.random.dataset.max.total.elapsed.seconds")
            ?: FltX(300.0)
        val maxMilpGap = optionalFltXProperty("bpp3d.gurobi.random.dataset.max.milp.gap")
            ?: FltX(0.05)
        val maxLpGap = optionalFltXProperty("bpp3d.gurobi.random.dataset.max.lp.gap")
            ?: FltX(0.05)
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = buildSolverConfig(prefix = "bpp3d.gurobi.random.dataset")
        )
        val service = ColumnGenerationApplicationService(solver)
        var totalElapsed = ZERO

        assertEquals(caseCount, scenarios.size)
        for (scenarioCase in scenarios) {
            val scenario = scenarioCase.scenario
            val response = service.solve(
                request = ColumnGenerationApplicationRequest(
                    itemDemands = scenario.itemDemands,
                    demandEntries = scenario.demandEntries,
                    initialColumns = scenario.initialColumns,
                    finalBins = scenario.finalBins,
                    cgConfig = ColumnGenerationConfig(
                        iterationLimit = 4,
                        maxColumnsPerIteration = 96
                    ),
                    depthBoundaryLayerOrientationPolicy = scenario.depthBoundaryLayerOrientationPolicy,
                    executorConfig = ColumnGenerationStandardExecutorConfig(
                        integralityTolerance = FltX(1e-5)
                    ),
                    generators = listOf(
                        object : Bpp3dLayerGenerator<FltX> {
                            override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                                return emptyList()
                            }
                        }
                    )
                ),
                packingAnalyzer = ColumnGenerationPackingAnalyzer()
            ).orFail("Gurobi deterministic random application service should solve: ${scenarioCase.name}")
            totalElapsed += response.result.elapsed
            printScenarioMetrics(
                caseName = scenarioCase.name,
                response = response
            )

            assertTrue(response.result.finalSolved, scenarioCase.name)
            assertEquals(1, response.result.lpSolvedTimes, scenarioCase.name)
            assertTrue(response.result.lpObjectives.isNotEmpty(), scenarioCase.name)
            assertTrue(response.result.elapsed > 0.seconds, scenarioCase.name)
            assertTrue(response.result.elapsed <= maxElapsedSeconds.toDouble().seconds, scenarioCase.name)
            assertEquals(1, response.result.lpInfos.size, scenarioCase.name)
            assertEquals("gurobi", response.result.lpInfos.first()["solver"], scenarioCase.name)
            assertEquals("gurobi", response.result.finalInfo["solver"], scenarioCase.name)
            response.result.lpInfos.first()["lp_gap"]
                ?.toDoubleOrNull()
                ?.let { lpGap ->
                    assertTrue(
                        FltX(lpGap) <= maxLpGap,
                        "${scenarioCase.name}: lp_gap=$lpGap exceeds $maxLpGap"
                    )
                }
            response.result.finalInfo["milp_gap"]
                ?.toDoubleOrNull()
                ?.let { milpGap ->
                    assertTrue(
                        FltX(milpGap) <= maxMilpGap,
                        "${scenarioCase.name}: milp_gap=$milpGap exceeds $maxMilpGap"
                    )
                }
            val snapshot = response.packingSnapshot
            assertNotNull(snapshot, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.bins.size, scenarioCase.name)
            assertEquals(scenario.packedLayerCount, snapshot.bins.sumOfInt { bin -> bin.units.size }, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.packingResult.aggregation.bins.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size }, scenarioCase.name)
            assertEquals(scenario.materialCount, snapshot.packingResult.materialSummary.size, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.schema.loadingPlans.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.schema.loadingPlans.sumOfInt { plan -> plan.items.size }, scenarioCase.name)
            val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
                entry.material to entry.amount
            }
            for ((material, demand) in scenario.materialAmountDemands) {
                assertEquals(demand, materialSummary[material.key], scenarioCase.name)
            }
            assertEquals(scenario.groupCount.toString(), response.result.finalInfo["selected_bin_count"], scenarioCase.name)
            assertEquals(scenario.totalLayerCount.toString(), response.result.finalInfo["selected_layer_count"], scenarioCase.name)
            assertEquals(response.result.iterationCount.toString(), snapshot.schema.kpi["cg_iteration"], scenarioCase.name)
            assertEquals(scenario.groupCount.toString(), snapshot.schema.kpi["bin_count"], scenarioCase.name)
            assertEquals(scenario.materialCount.toString(), snapshot.schema.kpi["material_count"], scenarioCase.name)
        }

        assertTrue(totalElapsed <= maxTotalElapsedSeconds.toDouble().seconds)
    }

    @Test
    fun applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario() = runBlocking {
        val materialCount = 6
        val groupCount = 3
        val layersPerGroup = 8
        val itemsPerLayer = 12
        val totalLayerCount = groupCount * layersPerGroup
        val totalItemCount = totalLayerCount * itemsPerLayer
        val materials = (0 until materialCount).map { index ->
            Material(
                no = MaterialNo("M-GUROBI-MIXED-WEIGHT-$index"),
                type = MaterialType.RawMaterial,
                cargo = CargoAttr,
                name = "M-GUROBI-MIXED-WEIGHT-$index",
                weight = FltX((index + 1).toDouble()) * Kilogram
            )
        }
        val binTypes = (0 until groupCount).map { index ->
            BinType(
                width = FltX(itemsPerLayer.toDouble()) * Meter,
                height = FltX(3.0) * Meter,
                depth = FltX(layersPerGroup.toDouble()) * Meter,
                capacity = FltX(1200.0) * Kilogram,
                longitudinalBalance = null,
                lateralBalance = null,
                typeCode = "BIN-GUROBI-MIXED-WEIGHT-$index"
            )
        }
        val layers = ArrayList<BinLayer>()
        val items = ArrayList<ActualItem>()
        var globalItemIndex = 0
        for (groupIndex in 0 until groupCount) {
            for (layerIndex in 0 until layersPerGroup) {
                val layerItems = (0 until itemsPerLayer).map {
                    val material = materials[globalItemIndex % materialCount]
                    val actualItem = item(
                        id = "item-gurobi-mixed-weight-$globalItemIndex",
                        material = material
                    )
                    globalItemIndex += 1
                    items.add(actualItem)
                    actualItem
                }
                val bin = layerBin(
                    items = layerItems,
                    typeCode = binTypes[groupIndex].typeCode,
                    depthInMeter = FltX(layersPerGroup.toLong()),
                    binType = binTypes[groupIndex],
                    widthInMeter = FltX(itemsPerLayer.toLong())
                )
                val rawLayer = bin.units.first().unit
                layers.add(
                    BinLayer(
                        iteration = rawLayer.iteration,
                        from = rawLayer.from,
                        bin = binTypes[groupIndex],
                        shape = rawLayer.shape,
                        units = rawLayer.units
                    )
                )
            }
        }
        val itemDemands = items.map { Pair(it, UInt64.one) }
        val expectedMaterialAmount = UInt64(totalItemCount / materialCount)
        val materialAmountDemands = materials.associateWith { expectedMaterialAmount }
        val materialWeightDemands = materials.mapIndexed { index, material ->
            Pair(
                material,
                FltX((expectedMaterialAmount.toLong() * (index + 1L)).toDouble()) * Kilogram
            )
        }
        val demandEntries = demandEntriesFromItems(items = itemDemands) +
                demandEntriesFromMaterialAmounts(
                    materials = materialAmountDemands.entries.map { entry ->
                        Pair(entry.key, entry.value)
                    }
                ) +
                demandEntriesFromMaterialWeights(
                    materials = materialWeightDemands
                )
        val solver = GurobiDelegatingColumnGenerationSolver(
            config = SolverConfig(
                time = 40.seconds,
                threadNum = UInt64(4),
                gap = SolverFlt64(0.01),
                notImprovementTime = 15.seconds
            )
        )
        val service = ColumnGenerationApplicationService(solver)
        val response = service.solve(
            request = ColumnGenerationApplicationRequest(
                itemDemands = itemDemands,
                demandEntries = demandEntries,
                initialColumns = layers,
                finalBins = binTypes.mapIndexed { index, binType ->
                    layerBinOf(
                        shape = binType,
                        units = emptyList<QuantityPlacement3<BinLayer, FltX>>(),
                        batchNo = BatchNo("B-GUROBI-MIXED-WEIGHT-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 96
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = FltX(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<FltX> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<FltX>): List<Bpp3dLayerGenerationResult<FltX>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        ).orFail("Gurobi large multi-bin triple demand application service should solve")

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        assertTrue(response.result.elapsed > 0.seconds)
        assertTrue(response.result.elapsed <= 120.seconds)
        assertEquals(1, response.result.lpInfos.size)
        assertEquals("gurobi", response.result.lpInfos.first()["solver"])
        assertTrue((response.result.lpInfos.first()["lp_time_ms"] ?: "0").toLong() >= 0L)
        assertEquals("gurobi", response.result.finalInfo["solver"])
        assertTrue((response.result.finalInfo["milp_time_ms"] ?: "0").toLong() >= 0L)
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(groupCount, snapshot.bins.size)
        assertEquals(totalLayerCount, snapshot.bins.sumOfInt { bin -> bin.units.size })
        assertEquals(groupCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(totalItemCount, snapshot.packingResult.aggregation.bins.sumOfInt { bin -> bin.items.size })
        assertEquals(groupCount, snapshot.schema.loadingPlans.size)
        assertEquals(totalItemCount, snapshot.schema.loadingPlans.sumOfInt { plan -> plan.items.size })
        assertEquals(materialCount, snapshot.packingResult.materialSummary.size)
        val materialSummary = snapshot.packingResult.materialSummary.associate { entry ->
            entry.material to entry.amount
        }
        for ((material, demand) in materialAmountDemands) {
            assertEquals(demand, materialSummary[material.key])
        }
        assertEquals(groupCount.toString(), response.result.finalInfo["selected_bin_count"])
        assertEquals(totalLayerCount.toString(), response.result.finalInfo["selected_layer_count"])
        assertEquals(response.result.iterationCount.toString(), snapshot.schema.kpi["cg_iteration"])
        assertEquals(groupCount.toString(), snapshot.schema.kpi["bin_count"])
        assertEquals(materialCount.toString(), snapshot.schema.kpi["material_count"])
    }
}
