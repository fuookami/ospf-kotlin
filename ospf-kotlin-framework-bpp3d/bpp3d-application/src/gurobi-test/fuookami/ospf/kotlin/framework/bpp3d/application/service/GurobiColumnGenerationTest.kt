package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromMaterialAmounts
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromMaterialWeights
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromItems
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber as Flt64
import fuookami.ospf.kotlin.math.algebra.number.Flt64 as SolverFlt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

@EnabledIfSystemProperty(named = "bpp3d.gurobi.cg.test.enabled", matches = "true")
class GurobiColumnGenerationTest {
    private object CargoAttr : fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(
        id: String,
        material: Material<Flt64>,
        widthInMeter: Flt64 = Flt64.one,
        shapeSpec: PackageShapeSpec = PackageShapeSpec.Cuboid
    ): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = widthInMeter * Meter,
                height = Flt64.one * Meter,
                depth = Flt64.one * Meter,
                weight = Flt64.one * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = shapeSpec
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

    private fun layerBin(
        items: List<ActualItem>,
        typeCode: String = "BIN-GUROBI",
        depthInMeter: Flt64 = Flt64(3.0),
        binType: BinType? = null,
        widthInMeter: Flt64 = Flt64(3.0)
    ): LayerBin {
        val resolvedBinType = binType ?: BinType(
            width = widthInMeter * Meter,
            height = Flt64(3.0) * Meter,
            depth = depthInMeter * Meter,
            capacity = Flt64(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = typeCode
        )
        val placements = items.mapIndexed { index, item ->
            item.toItemPlacement(
                x = Flt64(index.toLong()) * Meter
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = GurobiColumnGenerationTest::class,
            bin = resolvedBinType,
            shape = Container3Shape(resolvedBinType),
            units = placements
        )
        return layerBinOf(
            shape = resolvedBinType,
            units = listOf(
                layer.toLayerPlacement()
            )
        )
    }

    private data class CsvScenarioRow(
        val groupIndex: Int,
        val layerIndex: Int,
        val itemId: String,
        val materialNo: String,
        val materialName: String,
        val materialWeightKg: Flt64,
        val shapeType: String?,
        val radiusMeter: Flt64?,
        val radiusMinMeter: Flt64?,
        val radiusMaxMeter: Flt64?,
        val radiusStepMeter: Flt64?,
        val diameterMinMeter: Flt64?,
        val diameterMaxMeter: Flt64?,
        val diameterStepMeter: Flt64?,
        val axis: String?
    )

    private data class MaterialWidthAmountScenarioRow(
        val material: String,
        val width: Flt64,
        val amount: UInt64,
        val materialNo: String?,
        val materialName: String?,
        val materialWeightKg: Flt64?,
        val shapeType: String?,
        val radiusMeter: Flt64?,
        val radiusMinMeter: Flt64?,
        val radiusMaxMeter: Flt64?,
        val radiusStepMeter: Flt64?,
        val diameterMinMeter: Flt64?,
        val diameterMaxMeter: Flt64?,
        val diameterStepMeter: Flt64?,
        val axis: String?
    )

    private data class CsvDrivenScenario(
        val itemDemands: List<Pair<ActualItem, UInt64>>,
        val demandEntries: List<Bpp3dDemandEntry<Flt64>>,
        val initialColumns: List<BinLayer>,
        val finalBins: List<LayerBin>,
        val materialAmountDemands: Map<Material<Flt64>, UInt64>,
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
        defaultTimeSeconds: Flt64 = Flt64(40.0),
        defaultThreadNum: Int = 4,
        defaultGap: Flt64 = Flt64(0.01),
        defaultNotImprovementTimeSeconds: Flt64 = Flt64(15.0)
    ): SolverConfig {
        val timeSeconds = optionalFlt64Property("$prefix.solver.time.seconds")
            ?: defaultTimeSeconds
        val threadNum = optionalIntProperty("$prefix.solver.thread.num")
            ?: defaultThreadNum
        val gap = optionalFlt64Property("$prefix.solver.gap")
            ?: defaultGap
        val notImprovementTimeSeconds = optionalFlt64Property("$prefix.solver.not.improvement.time.seconds")
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
            "axis"
        )
        val hasShapeMetadata = shapeMetadataColumns.any { column ->
            normalizedHeaderColumns.contains(column)
        }
        if (hasShapeMetadata && !hasShapeType) {
            throw IllegalStateException(
                "invalid csv schema: shape_type is required when shape metadata columns exist, header=$headerLine"
            )
        }
    }

    private fun optionalCsvFlt64(
        cols: List<String>,
        column: Int?,
        fieldName: String,
        rowIndex: Int
    ): Flt64? {
        val raw = column
            ?.let { cols.getOrNull(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return raw.toDoubleOrNull()?.let { Flt64(it) }
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
        val axisColumn = optionalColumn("axis")
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
                ?.let { Flt64(it) }
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
                radiusMeter = optionalCsvFlt64(
                    cols = cols,
                    column = radiusMeterColumn,
                    fieldName = "radius_meter",
                    rowIndex = rowIndex
                ),
                radiusMinMeter = optionalCsvFlt64(
                    cols = cols,
                    column = radiusMinMeterColumn,
                    fieldName = "radius_min",
                    rowIndex = rowIndex
                ),
                radiusMaxMeter = optionalCsvFlt64(
                    cols = cols,
                    column = radiusMaxMeterColumn,
                    fieldName = "radius_max",
                    rowIndex = rowIndex
                ),
                radiusStepMeter = optionalCsvFlt64(
                    cols = cols,
                    column = radiusStepMeterColumn,
                    fieldName = "radius_step",
                    rowIndex = rowIndex
                ),
                diameterMinMeter = optionalCsvFlt64(
                    cols = cols,
                    column = diameterMinMeterColumn,
                    fieldName = "diameter_min",
                    rowIndex = rowIndex
                ),
                diameterMaxMeter = optionalCsvFlt64(
                    cols = cols,
                    column = diameterMaxMeterColumn,
                    fieldName = "diameter_max",
                    rowIndex = rowIndex
                ),
                diameterStepMeter = optionalCsvFlt64(
                    cols = cols,
                    column = diameterStepMeterColumn,
                    fieldName = "diameter_step",
                    rowIndex = rowIndex
                ),
                axis = axisColumn?.let { column -> cols.getOrNull(column) }?.takeIf { it.isNotEmpty() }
            )
        }

        val materialsByNo = LinkedHashMap<String, Material<Flt64>>()
        val materialWeightKgByNo = LinkedHashMap<String, Flt64>()
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
            itemsById[row.itemId] = item(
                id = row.itemId,
                material = material,
                shapeSpec = shapeSpec
            )
        }

        val rowsByGroup = rows.groupBy { it.groupIndex }.toSortedMap()
        val initialColumns = ArrayList<BinLayer>()
        val finalBins = ArrayList<LayerBin>()
        for ((groupIndex, groupRows) in rowsByGroup) {
            val rowsByLayer = groupRows.groupBy { it.layerIndex }.toSortedMap()
            val layerCount = rowsByLayer.size
            val maxItemsPerLayer = rowsByLayer.values.maxOf { it.size }
            val binType = BinType(
                width = Flt64(maxItemsPerLayer.toLong()) * Meter,
                height = Flt64(3.0) * Meter,
                depth = Flt64(layerCount.toLong()) * Meter,
                capacity = Flt64(1500.0) * Kilogram,
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
                val bin = layerBin(
                    items = layerItems,
                    typeCode = binType.typeCode,
                    depthInMeter = Flt64(layerCount.toLong()),
                    binType = binType,
                    widthInMeter = Flt64(maxItemsPerLayer.toLong())
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
            Pair(material, (Flt64(entry.value.toLong()) * weightKg) * Kilogram)
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
                runCatching { Orientation.require(raw.trim()) }
                    .getOrElse {
                        throw IllegalStateException("invalid orientation in $fieldName: $raw")
                    }
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
    fun groupedLayerCsvShouldRemainCompatibleWithLegacySixColumns() {
        val csv = """
            group_index,layer_index,item_id,material_no,material_name,material_weight_kg
            0,0,item-legacy,MAT-A,Material-A,1.0
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val legacyItem = scenario.itemDemands
            .firstOrNull()
            ?.first
            ?: throw IllegalStateException("missing legacy item in scenario")

        assertEquals(PackageShapeSpec.Cuboid, legacyItem.packageShape.shapeSpec)
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
    fun csvShapeSpecParserShouldAcceptHorizontalCylinderAxesAsMetadata() {
        val cylinderXSpec = toPackageShapeSpec(
            shapeType = "vertical_cylinder",
            radiusMeter = Flt64(0.5),
            radiusMinMeter = null,
            radiusMaxMeter = null,
            radiusStepMeter = null,
            diameterMinMeter = null,
            diameterMaxMeter = null,
            diameterStepMeter = null,
            axis = "X",
            rowDescription = "axis=x metadata parser test"
        ) as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("axis=x shape spec should be VerticalCylinder")
        val cylinderZSpec = toPackageShapeSpec(
            shapeType = "vertical_cylinder",
            radiusMeter = Flt64(0.5),
            radiusMinMeter = null,
            radiusMaxMeter = null,
            radiusStepMeter = null,
            diameterMinMeter = null,
            diameterMaxMeter = null,
            diameterStepMeter = null,
            axis = "Axis3.Z",
            rowDescription = "axis=z metadata parser test"
        ) as? PackageShapeSpec.VerticalCylinder
            ?: throw IllegalStateException("axis=z shape spec should be VerticalCylinder")

        assertEquals(Axis3.X, cylinderXSpec.axis)
        assertEquals(Axis3.Z, cylinderZSpec.axis)
    }

    @Test
    fun materialWidthAmountCsvShouldRemainCompatibleWithLegacyColumns() {
        val csv = """
            material,width,amount
            MAT-A,1000,1
        """.trimIndent()

        val scenario = loadCsvDrivenScenarioFromCsvText(csv)
        val legacyItem = scenario.itemDemands
            .firstOrNull()
            ?.first
            ?: throw IllegalStateException("missing legacy item in scenario")

        assertEquals(PackageShapeSpec.Cuboid, legacyItem.packageShape.shapeSpec)
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
    fun materialWidthAmountMixedShapeSampleFileShouldBeParsable() {
        val scenario = loadCsvDrivenScenario("gurobi/material-width-amount-cylinder-sample.csv")
        assertEquals(1, scenario.groupCount)
        assertTrue(scenario.totalItemCount > 0)
        assertTrue(scenario.packedLayerCount >= scenario.totalLayerCount)
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
        val axisColumn = optionalColumn("axis")
        val firstLayerAllowedCylinderAxesColumn = optionalColumn("first_layer_allowed_cylinder_axes")
        val lastLayerAllowedCylinderAxesColumn = optionalColumn("last_layer_allowed_cylinder_axes")
        val firstLayerAllowedCuboidOrientationsColumn = optionalColumn("first_layer_allowed_cuboid_orientations")
        val lastLayerAllowedCuboidOrientationsColumn = optionalColumn("last_layer_allowed_cuboid_orientations")
        val widthScale = optionalFlt64Property("bpp3d.gurobi.dataset.material.width.scale")
            ?: Flt64(1000.0)
        val defaultMaterialWeightKg = optionalFlt64Property("bpp3d.gurobi.dataset.material.default.weight.kg")
            ?: Flt64.one
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
                ?.let { Flt64(it) }
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
                    materialWeightKg = materialWeightColumn?.let { cols.getOrNull(it)?.toDoubleOrNull()?.let { value -> Flt64(value) } },
                    shapeType = shapeTypeColumn?.let { cols.getOrNull(it)?.takeIf { value -> value.isNotEmpty() } },
                    radiusMeter = optionalCsvFlt64(
                        cols = cols,
                        column = radiusMeterColumn,
                        fieldName = "radius_meter",
                        rowIndex = rowIndex
                    ),
                    radiusMinMeter = optionalCsvFlt64(
                        cols = cols,
                        column = radiusMinMeterColumn,
                        fieldName = "radius_min",
                        rowIndex = rowIndex
                    ),
                    radiusMaxMeter = optionalCsvFlt64(
                        cols = cols,
                        column = radiusMaxMeterColumn,
                        fieldName = "radius_max",
                        rowIndex = rowIndex
                    ),
                    radiusStepMeter = optionalCsvFlt64(
                        cols = cols,
                        column = radiusStepMeterColumn,
                        fieldName = "radius_step",
                        rowIndex = rowIndex
                    ),
                    diameterMinMeter = optionalCsvFlt64(
                        cols = cols,
                        column = diameterMinMeterColumn,
                        fieldName = "diameter_min",
                        rowIndex = rowIndex
                    ),
                    diameterMaxMeter = optionalCsvFlt64(
                        cols = cols,
                        column = diameterMaxMeterColumn,
                        fieldName = "diameter_max",
                        rowIndex = rowIndex
                    ),
                    diameterStepMeter = optionalCsvFlt64(
                        cols = cols,
                        column = diameterStepMeterColumn,
                        fieldName = "diameter_step",
                        rowIndex = rowIndex
                    ),
                    axis = axisColumn?.let { cols.getOrNull(it)?.takeIf { value -> value.isNotEmpty() } }
                )
            )
        }
        if (rows.isEmpty()) {
            throw IllegalStateException("csv has no valid demand rows")
        }

        val materialsByNo = LinkedHashMap<String, Material<Flt64>>()
        val itemDemands = ArrayList<Pair<ActualItem, UInt64>>()
        val totalAmountByMaterialNo = LinkedHashMap<String, UInt64>()
        val totalWeightByMaterialNo = LinkedHashMap<String, Flt64>()
        val widthsInMeter = ArrayList<Flt64>()
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
            val widthInMeter = maxOf(row.width / widthScale, Flt64(0.2))
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
                axis = row.axis,
                rowDescription = "material=${row.material},row_index=${index + 2}"
            )
            val actualItem = item(
                id = "item-material-width-$index",
                material = material,
                widthInMeter = widthInMeter,
                shapeSpec = shapeSpec
            )
            itemDemands.add(Pair(actualItem, row.amount))
            totalAmount += row.amount
            totalAmountByMaterialNo[materialNo] = (totalAmountByMaterialNo[materialNo] ?: UInt64.zero) + row.amount
            val weightContribution = Flt64(row.amount.toLong().toDouble()) * materialWeightKg
            totalWeightByMaterialNo[materialNo] = (totalWeightByMaterialNo[materialNo] ?: Flt64.zero) + weightContribution
        }

        val maxWidthInMeter = widthsInMeter.maxOrNull() ?: Flt64.one
        val totalDepthInMeter = maxOf(
            Flt64(totalAmount.toLong().toDouble()),
            Flt64(defaultBinDepth.toLong())
        )
        val binType = BinType(
            width = maxWidthInMeter * Meter,
            height = Flt64(3.0) * Meter,
            depth = totalDepthInMeter * Meter,
            capacity = maxOf(Flt64(totalAmount.toLong().toDouble()), Flt64.one) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-MATERIAL-WIDTH"
        )
        val finalBins: List<LayerBin> = listOf(
            layerBinOf(
                shape = binType,
                units = emptyList<BinLayerPlacement>(),
                batchNo = BatchNo("B-GUROBI-MATERIAL-WIDTH")
            )
        )
        val initialColumns = itemDemands.map { (actualItem, _) ->
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
        radiusMeter: Flt64?,
        radiusMinMeter: Flt64?,
        radiusMaxMeter: Flt64?,
        radiusStepMeter: Flt64?,
        diameterMinMeter: Flt64?,
        diameterMaxMeter: Flt64?,
        diameterStepMeter: Flt64?,
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
            val resolvedRadiusMeter = radiusMeter
                ?: radiusMinMeter
                ?: diameterMinMeter?.let { it / Flt64(2.0) }
                ?: throw IllegalStateException(
                    "missing radius_meter, radius_min, or diameter_min for cylinder row: $rowDescription"
                )
            val resolvedAxis = parseAxis3OrThrow(
                axis = axis,
                rowDescription = rowDescription
            )
            return PackageShapeSpec.VerticalCylinder(
                radius = resolvedRadiusMeter * Meter,
                axis = resolvedAxis,
                radiusMin = radiusMinMeter?.let { it * Meter },
                radiusMax = radiusMaxMeter?.let { it * Meter },
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

    private fun optionalIntProperty(name: String): Int? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toIntOrNull()
    }

    private fun optionalFlt64Property(name: String): Flt64? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
            ?.let { Flt64(it) }
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
            weight = Flt64(0.5) * Kilogram
        )
        val actualItem = item("item-gurobi", material)
        val seedBin = layerBin(listOf(actualItem))
        val rawSeedLayer = seedBin.units.first().unit
        val seedLayer = BinLayer(
            iteration = rawSeedLayer.iteration,
            from = rawSeedLayer.from,
            bin = seedBin.shape,
            shape = rawSeedLayer.shape,
            units = rawSeedLayer.units
        )
        val demandValue = Flt64.one
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
                    Flt64
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
                    shape = seedBin.shape,
                    units = emptyList<BinLayerPlacement>(),
                    batchNo = seedBin.batchNo
                )
            )
        )

        var capturedRequest: Bpp3dLayerGenerationRequest<Flt64>? = null
        var analyzedState: ColumnGenerationState<Flt64>? = null
        val algorithm = ColumnGenerationAlgorithm(
            layerGenerator = object : Bpp3dLayerGenerator<Flt64> {
                override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                    capturedRequest = request
                    return emptyList()
                }
            },
            rmpSolver = executors.rmpSolver(),
            finalMilpSolver = executors.finalSolver(),
            layerRequestBuilder = executors.requestBuilder(),
            solutionAnalyzer = ColumnGenerationSolutionAnalyzer { state ->
                analyzedState = state
            },
            initialColumns = { listOf(seedLayer) }
        )

        val result = algorithm.solve(items = listOf<Item>(actualItem))
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
            weight = Flt64(0.5) * Kilogram
        )
        val actualItem = item("item-gurobi-svc", material)
        val seedBin = layerBin(listOf(actualItem))
        val seedLayer = seedBin.units.first().unit
        val demandValue = Flt64.one
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
                    Flt64
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
                        shape = seedBin.shape,
                        units = emptyList<BinLayerPlacement>(),
                        batchNo = seedBin.batchNo
                    )
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        assertNotNull(response.packingSnapshot)
        assertTrue(response.packingSnapshot!!.bins.isNotEmpty())
        assertEquals(response.packingSnapshot!!.bins.size.toString(), response.packingSnapshot!!.schema.kpi["bin_count"])
    }

    @Test
    fun applicationServiceShouldKeepPackingConsistentForMultiMaterialWithGurobi() = runBlocking {
        val materialA = Material(
            no = MaterialNo("M-GUROBI-A"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-A",
            weight = Flt64(1.0) * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("M-GUROBI-B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-B",
            weight = Flt64(1.0) * Kilogram
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
                        shape = seedBin.shape,
                        units = emptyList<BinLayerPlacement>(),
                        batchNo = seedBin.batchNo
                    )
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(1, snapshot.bins.size)
        assertEquals(1, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(1, snapshot.packingResult.aggregation.bins.size)
        assertEquals(2, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
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
            width = Flt64(itemsPerLayer.toDouble()) * Meter,
            height = Flt64(3.0) * Meter,
            depth = Flt64(2.0) * Meter,
            capacity = Flt64(300.0) * Kilogram,
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
                weight = Flt64(1.0) * Kilogram
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
                depthInMeter = Flt64(2.0),
                binType = sharedBinType,
                widthInMeter = Flt64(itemsPerLayer.toLong())
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
                        units = emptyList<BinLayerPlacement>(),
                        batchNo = BatchNo("B-GUROBI-MEDIUM-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 64
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = Flt64(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(finalBinCount, snapshot.bins.size)
        assertEquals(layerCount, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(1, snapshot.packingResult.aggregation.bins.size)
        assertEquals(layerCount * itemsPerLayer, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
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
            width = Flt64(itemsPerLayer.toDouble()) * Meter,
            height = Flt64(3.0) * Meter,
            depth = Flt64(layerCount.toDouble()) * Meter,
            capacity = Flt64(600.0) * Kilogram,
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
                weight = Flt64(1.0) * Kilogram
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
                depthInMeter = Flt64(layerCount.toLong()),
                binType = sharedBinType,
                widthInMeter = Flt64(itemsPerLayer.toLong())
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
                        units = emptyList<BinLayerPlacement>(),
                        batchNo = BatchNo("B-GUROBI-LARGE-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 64
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = Flt64(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(finalBinCount, snapshot.bins.size)
        assertEquals(layerCount, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(1, snapshot.packingResult.aggregation.bins.size)
        assertEquals(layerCount * itemsPerLayer, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
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
                weight = Flt64(1.0) * Kilogram
            )
        }
        val binTypes = (0 until groupCount).map { index ->
            BinType(
                width = Flt64(itemsPerLayer.toDouble()) * Meter,
                height = Flt64(3.0) * Meter,
                depth = Flt64(layersPerGroup.toDouble()) * Meter,
                capacity = Flt64(600.0) * Kilogram,
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
                    depthInMeter = Flt64(layersPerGroup.toLong()),
                    binType = binTypes[groupIndex],
                    widthInMeter = Flt64(itemsPerLayer.toLong())
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
                        units = emptyList<BinLayerPlacement>(),
                        batchNo = BatchNo("B-GUROBI-MIXED-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 64
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = Flt64(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(groupCount, snapshot.bins.size)
        assertEquals(totalLayerCount, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(groupCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(totalItemCount, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
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
                    integralityTolerance = Flt64(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )
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
        val maxElapsedSeconds = optionalFlt64Property("bpp3d.gurobi.dataset.max.elapsed.seconds")
            ?: Flt64(120.0)
        val maxMilpGap = optionalFlt64Property("bpp3d.gurobi.dataset.max.milp.gap")
            ?: Flt64(0.05)
        val maxLpGap = optionalFlt64Property("bpp3d.gurobi.dataset.max.lp.gap")
            ?: Flt64(0.05)

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
                assertTrue(Flt64(lpGap) <= maxLpGap)
            }
        response.result.finalInfo["milp_gap"]
            ?.toDoubleOrNull()
            ?.let { milpGap ->
                assertTrue(Flt64(milpGap) <= maxMilpGap)
            }
        val snapshot = response.packingSnapshot
        assertNotNull(snapshot)
        assertEquals(expectedGroupCount, snapshot.bins.size)
        assertEquals(expectedPackedLayerCount, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(expectedGroupCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(expectedItemCount, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
        assertEquals(expectedMaterialCount, snapshot.packingResult.materialSummary.size)
        assertEquals(expectedGroupCount, snapshot.schema.loadingPlans.size)
        assertEquals(expectedItemCount, snapshot.schema.loadingPlans.sumOf { plan -> plan.items.size })
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
        val maxElapsedSeconds = optionalFlt64Property("bpp3d.gurobi.dataset.suite.max.elapsed.seconds")
            ?: Flt64(180.0)
        val maxTotalElapsedSeconds = optionalFlt64Property("bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds")
            ?: Flt64(600.0)
        val maxMilpGap = optionalFlt64Property("bpp3d.gurobi.dataset.suite.max.milp.gap")
            ?: Flt64(0.05)
        val maxLpGap = optionalFlt64Property("bpp3d.gurobi.dataset.suite.max.lp.gap")
            ?: Flt64(0.05)
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
                        integralityTolerance = Flt64(1e-5)
                    ),
                    generators = listOf(
                        object : Bpp3dLayerGenerator<Flt64> {
                            override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                                return emptyList()
                            }
                        }
                    )
                ),
                packingAnalyzer = ColumnGenerationPackingAnalyzer()
            )
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
                        Flt64(lpGap) <= maxLpGap,
                        "${scenarioCase.name}: lp_gap=$lpGap exceeds $maxLpGap"
                    )
                }
            response.result.finalInfo["milp_gap"]
                ?.toDoubleOrNull()
                ?.let { milpGap ->
                    assertTrue(
                        Flt64(milpGap) <= maxMilpGap,
                        "${scenarioCase.name}: milp_gap=$milpGap exceeds $maxMilpGap"
                    )
                }
            val snapshot = response.packingSnapshot
            assertNotNull(snapshot, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.bins.size, scenarioCase.name)
            assertEquals(scenario.packedLayerCount, snapshot.bins.sumOf { bin -> bin.units.size }, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.packingResult.aggregation.bins.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size }, scenarioCase.name)
            assertEquals(scenario.materialCount, snapshot.packingResult.materialSummary.size, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.schema.loadingPlans.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.schema.loadingPlans.sumOf { plan -> plan.items.size }, scenarioCase.name)
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
        val maxElapsedSeconds = optionalFlt64Property("bpp3d.gurobi.random.dataset.max.elapsed.seconds")
            ?: Flt64(120.0)
        val maxTotalElapsedSeconds = optionalFlt64Property("bpp3d.gurobi.random.dataset.max.total.elapsed.seconds")
            ?: Flt64(300.0)
        val maxMilpGap = optionalFlt64Property("bpp3d.gurobi.random.dataset.max.milp.gap")
            ?: Flt64(0.05)
        val maxLpGap = optionalFlt64Property("bpp3d.gurobi.random.dataset.max.lp.gap")
            ?: Flt64(0.05)
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
                        integralityTolerance = Flt64(1e-5)
                    ),
                    generators = listOf(
                        object : Bpp3dLayerGenerator<Flt64> {
                            override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                                return emptyList()
                            }
                        }
                    )
                ),
                packingAnalyzer = ColumnGenerationPackingAnalyzer()
            )
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
                        Flt64(lpGap) <= maxLpGap,
                        "${scenarioCase.name}: lp_gap=$lpGap exceeds $maxLpGap"
                    )
                }
            response.result.finalInfo["milp_gap"]
                ?.toDoubleOrNull()
                ?.let { milpGap ->
                    assertTrue(
                        Flt64(milpGap) <= maxMilpGap,
                        "${scenarioCase.name}: milp_gap=$milpGap exceeds $maxMilpGap"
                    )
                }
            val snapshot = response.packingSnapshot
            assertNotNull(snapshot, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.bins.size, scenarioCase.name)
            assertEquals(scenario.packedLayerCount, snapshot.bins.sumOf { bin -> bin.units.size }, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.packingResult.aggregation.bins.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size }, scenarioCase.name)
            assertEquals(scenario.materialCount, snapshot.packingResult.materialSummary.size, scenarioCase.name)
            assertEquals(scenario.groupCount, snapshot.schema.loadingPlans.size, scenarioCase.name)
            assertEquals(scenario.totalItemCount, snapshot.schema.loadingPlans.sumOf { plan -> plan.items.size }, scenarioCase.name)
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
                weight = Flt64((index + 1).toDouble()) * Kilogram
            )
        }
        val binTypes = (0 until groupCount).map { index ->
            BinType(
                width = Flt64(itemsPerLayer.toDouble()) * Meter,
                height = Flt64(3.0) * Meter,
                depth = Flt64(layersPerGroup.toDouble()) * Meter,
                capacity = Flt64(1200.0) * Kilogram,
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
                    depthInMeter = Flt64(layersPerGroup.toLong()),
                    binType = binTypes[groupIndex],
                    widthInMeter = Flt64(itemsPerLayer.toLong())
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
                Flt64((expectedMaterialAmount.toLong() * (index + 1L)).toDouble()) * Kilogram
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
                        units = emptyList<BinLayerPlacement>(),
                        batchNo = BatchNo("B-GUROBI-MIXED-WEIGHT-$index")
                    )
                },
                cgConfig = ColumnGenerationConfig(
                    iterationLimit = 4,
                    maxColumnsPerIteration = 96
                ),
                executorConfig = ColumnGenerationStandardExecutorConfig(
                    integralityTolerance = Flt64(1e-5)
                ),
                generators = listOf(
                    object : Bpp3dLayerGenerator<Flt64> {
                        override suspend fun generate(request: Bpp3dLayerGenerationRequest<Flt64>): List<Bpp3dLayerGenerationResult<Flt64>> {
                            return emptyList()
                        }
                    }
                )
            ),
            packingAnalyzer = ColumnGenerationPackingAnalyzer()
        )

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
        assertEquals(totalLayerCount, snapshot.bins.sumOf { bin -> bin.units.size })
        assertEquals(groupCount, snapshot.packingResult.aggregation.bins.size)
        assertEquals(totalItemCount, snapshot.packingResult.aggregation.bins.sumOf { bin -> bin.items.size })
        assertEquals(groupCount, snapshot.schema.loadingPlans.size)
        assertEquals(totalItemCount, snapshot.schema.loadingPlans.sumOf { plan -> plan.items.size })
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

