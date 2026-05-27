package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
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
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
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
        material: Material,
        widthInMeter: Double = 1.0
    ): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = widthInMeter * Meter,
                height = 1.0 * Meter,
                depth = 1.0 * Meter,
                weight = 1.0 * Kilogram,
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

    private fun layerBin(
        items: List<ActualItem>,
        typeCode: String = "BIN-GUROBI",
        depthInMeter: Double = 3.0,
        binType: BinType? = null,
        widthInMeter: Double = 3.0
    ): Bin<BinLayer> {
        val resolvedBinType = binType ?: BinType(
            width = widthInMeter * Meter,
            height = 3.0 * Meter,
            depth = depthInMeter * Meter,
            capacity = 100.0 * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = typeCode
        )
        val placements = items.mapIndexed { index, item ->
            QuantityPlacement3(
                view = item.view(Orientation.Upright),
                position = point3(x = index.toDouble() * Meter, y = 0.0 * Meter, z = 0.0 * Meter)
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = GurobiColumnGenerationTest::class,
            bin = resolvedBinType,
            shape = Container3Shape(resolvedBinType),
            units = placements
        )
        return Bin(
            shape = resolvedBinType,
            units = listOf(
                QuantityPlacement3(
                    view = layer.view(Orientation.Upright)!!,
                    position = point3()
                )
            )
        )
    }

    private data class CsvScenarioRow(
        val groupIndex: Int,
        val layerIndex: Int,
        val itemId: String,
        val materialNo: String,
        val materialName: String,
        val materialWeightKg: Double
    )

    private data class MaterialWidthAmountScenarioRow(
        val material: String,
        val width: Double,
        val amount: UInt64,
        val materialNo: String?,
        val materialName: String?,
        val materialWeightKg: Double?
    )

    private data class CsvDrivenScenario(
        val itemDemands: List<Pair<ActualItem, UInt64>>,
        val demandEntries: List<Bpp3dDemandEntry>,
        val initialColumns: List<BinLayer>,
        val finalBins: List<Bin<BinLayer>>,
        val materialAmountDemands: Map<Material, UInt64>,
        val groupCount: Int,
        val materialCount: Int,
        val totalLayerCount: Int,
        val totalItemCount: Int,
        val packedLayerCount: Int = totalLayerCount
    )

    private data class CsvDrivenScenarioCase(
        val name: String,
        val scenario: CsvDrivenScenario
    )

    private fun buildSolverConfig(
        prefix: String,
        defaultTimeSeconds: Double = 40.0,
        defaultThreadNum: Int = 4,
        defaultGap: Double = 0.01,
        defaultNotImprovementTimeSeconds: Double = 15.0
    ): SolverConfig {
        val timeSeconds = optionalDoubleProperty("$prefix.solver.time.seconds")
            ?: defaultTimeSeconds
        val threadNum = optionalIntProperty("$prefix.solver.thread.num")
            ?: defaultThreadNum
        val gap = optionalDoubleProperty("$prefix.solver.gap")
            ?: defaultGap
        val notImprovementTimeSeconds = optionalDoubleProperty("$prefix.solver.not.improvement.time.seconds")
            ?: defaultNotImprovementTimeSeconds
        return SolverConfig(
            time = timeSeconds.seconds,
            threadNum = UInt64(threadNum.toULong()),
            gap = Flt64(gap),
            notImprovementTime = notImprovementTimeSeconds.seconds
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
        return loadCsvDrivenScenarioFromCsvText(csv)
    }

    private fun loadCsvDrivenScenarioFromFile(filePath: String): CsvDrivenScenario {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalStateException("invalid dataset file path: $filePath")
        }
        return loadCsvDrivenScenarioFromCsvText(file.readText())
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

    private fun loadCsvDrivenScenarioFromCsvText(csv: String): CsvDrivenScenario {
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
        return if (headerColumns.contains("group_index") && headerColumns.contains("layer_index")) {
            loadGroupedLayerCsvScenario(lines)
        } else if (headerColumns.contains("material") && headerColumns.contains("width") && headerColumns.contains("amount")) {
            loadMaterialWidthAmountCsvScenario(lines)
        } else {
            throw IllegalStateException("unsupported csv header: ${lines.first()}")
        }
    }

    private fun loadGroupedLayerCsvScenario(lines: List<String>): CsvDrivenScenario {
        val rows = lines.drop(1).mapIndexed { index, line ->
            val cols = line.split(",").map { it.trim() }
            if (cols.size != 6) {
                throw IllegalStateException("invalid csv columns at row ${index + 2}: $line")
            }
            CsvScenarioRow(
                groupIndex = cols[0].toInt(),
                layerIndex = cols[1].toInt(),
                itemId = cols[2],
                materialNo = cols[3],
                materialName = cols[4],
                materialWeightKg = cols[5].toDouble()
            )
        }

        val materialsByNo = LinkedHashMap<String, Material>()
        val materialWeightKgByNo = LinkedHashMap<String, Double>()
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
            itemsById[row.itemId] = item(
                id = row.itemId,
                material = material
            )
        }

        val rowsByGroup = rows.groupBy { it.groupIndex }.toSortedMap()
        val initialColumns = ArrayList<BinLayer>()
        val finalBins = ArrayList<Bin<BinLayer>>()
        for ((groupIndex, groupRows) in rowsByGroup) {
            val rowsByLayer = groupRows.groupBy { it.layerIndex }.toSortedMap()
            val layerCount = rowsByLayer.size
            val maxItemsPerLayer = rowsByLayer.values.maxOf { it.size }
            val binType = BinType(
                width = maxItemsPerLayer.toDouble() * Meter,
                height = 3.0 * Meter,
                depth = layerCount.toDouble() * Meter,
                capacity = 1500.0 * Kilogram,
                longitudinalBalance = null,
                lateralBalance = null,
                typeCode = "BIN-GUROBI-CSV-$groupIndex"
            )
            finalBins.add(
                Bin(
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
                    depthInMeter = layerCount.toDouble(),
                    binType = binType,
                    widthInMeter = maxItemsPerLayer.toDouble()
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
            Pair(material, (entry.value * weightKg) * Kilogram)
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
        return CsvDrivenScenario(
            itemDemands = itemDemands,
            demandEntries = demandEntries,
            initialColumns = initialColumns,
            finalBins = finalBins,
            materialAmountDemands = materialAmountDemands,
            groupCount = rowsByGroup.size,
            materialCount = materialsByNo.size,
            totalLayerCount = initialColumns.size,
            totalItemCount = rows.size
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

        val materialColumn = requiredColumn("material")
        val widthColumn = requiredColumn("width")
        val amountColumn = requiredColumn("amount")
        val materialNoColumn = optionalColumn("material_no")
        val materialNameColumn = optionalColumn("material_name")
        val materialWeightColumn = optionalColumn("material_weight_kg")
        val widthScale = optionalDoubleProperty("bpp3d.gurobi.dataset.material.width.scale")
            ?: 1000.0
        val defaultMaterialWeightKg = optionalDoubleProperty("bpp3d.gurobi.dataset.material.default.weight.kg")
            ?: 1.0
        val defaultBinDepth = optionalIntProperty("bpp3d.gurobi.dataset.material.default.bin.depth")
            ?: 240

        val rows = ArrayList<MaterialWidthAmountScenarioRow>()
        for ((index, line) in lines.drop(1).withIndex()) {
            val cols = line.split(",").map { it.trim() }
            if (cols.size < 3) {
                throw IllegalStateException("invalid csv columns at row ${index + 2}: $line")
            }
            val material = cols.getOrNull(materialColumn)
                ?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("empty material at row ${index + 2}")
            val width = cols.getOrNull(widthColumn)
                ?.toDoubleOrNull()
                ?: throw IllegalStateException("invalid width at row ${index + 2}: ${cols.getOrNull(widthColumn)}")
            val amountAsDouble = cols.getOrNull(amountColumn)
                ?.toDoubleOrNull()
                ?: throw IllegalStateException("invalid amount at row ${index + 2}: ${cols.getOrNull(amountColumn)}")
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
                    materialWeightKg = materialWeightColumn?.let { cols.getOrNull(it)?.toDoubleOrNull() }
                )
            )
        }
        if (rows.isEmpty()) {
            throw IllegalStateException("csv has no valid demand rows")
        }

        val materialsByNo = LinkedHashMap<String, Material>()
        val itemDemands = ArrayList<Pair<ActualItem, UInt64>>()
        val totalAmountByMaterialNo = LinkedHashMap<String, UInt64>()
        val totalWeightByMaterialNo = LinkedHashMap<String, Double>()
        val widthsInMeter = ArrayList<Double>()
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
            val widthInMeter = maxOf(row.width / widthScale, 0.2)
            widthsInMeter.add(widthInMeter)
            val actualItem = item(
                id = "item-material-width-$index",
                material = material,
                widthInMeter = widthInMeter
            )
            itemDemands.add(Pair(actualItem, row.amount))
            totalAmount += row.amount
            totalAmountByMaterialNo[materialNo] = (totalAmountByMaterialNo[materialNo] ?: UInt64.zero) + row.amount
            val weightContribution = row.amount.toLong().toDouble() * materialWeightKg
            totalWeightByMaterialNo[materialNo] = (totalWeightByMaterialNo[materialNo] ?: 0.0) + weightContribution
        }

        val maxWidthInMeter = widthsInMeter.maxOrNull() ?: 1.0
        val totalDepthInMeter = maxOf(totalAmount.toLong().toDouble(), defaultBinDepth.toDouble())
        val binType = BinType(
            width = maxWidthInMeter * Meter,
            height = 3.0 * Meter,
            depth = totalDepthInMeter * Meter,
            capacity = maxOf(totalAmount.toLong().toDouble(), 1.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-GUROBI-MATERIAL-WIDTH"
        )
        val finalBins: List<Bin<BinLayer>> = listOf(
            Bin(
                shape = binType,
                units = emptyList<QuantityPlacement3<BinLayer>>(),
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
            packedLayerCount = totalAmount.toInt()
        )
    }

    private fun optionalIntProperty(name: String): Int? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toIntOrNull()
    }

    private fun optionalDoubleProperty(name: String): Double? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
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
                CsvDrivenScenarioCase(
                    name = filePath,
                    scenario = loadCsvDrivenScenarioFromFile(
                        filePath = filePath
                    )
                )
            }
        }

        val directoryPath = optionalStringProperty(directoryPropertyName)
            ?: throw IllegalStateException(
                "missing dataset suite properties: $pathsPropertyName or $directoryPropertyName"
            )
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
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
            CsvDrivenScenarioCase(
                name = file.absolutePath,
                scenario = loadCsvDrivenScenarioFromFile(
                    filePath = file.absolutePath
                )
            )
        }
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
            weight = 0.5 * Kilogram
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
                Bin(
                    shape = seedBin.shape,
                    units = emptyList<QuantityPlacement3<BinLayer>>(),
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
            weight = 0.5 * Kilogram
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
                    Bin(
                        shape = seedBin.shape,
                        units = emptyList<QuantityPlacement3<BinLayer>>(),
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
            weight = 1.0 * Kilogram
        )
        val materialB = Material(
            no = MaterialNo("M-GUROBI-B"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-GUROBI-B",
            weight = 1.0 * Kilogram
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
                    Bin(
                        shape = seedBin.shape,
                        units = emptyList<QuantityPlacement3<BinLayer>>(),
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
            width = itemsPerLayer.toDouble() * Meter,
            height = 3.0 * Meter,
            depth = 2.0 * Meter,
            capacity = 300.0 * Kilogram,
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
                weight = 1.0 * Kilogram
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
                depthInMeter = 2.0,
                binType = sharedBinType,
                widthInMeter = itemsPerLayer.toDouble()
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
                gap = Flt64(0.01),
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
                    Bin(
                        shape = sharedBinType,
                        units = emptyList<QuantityPlacement3<BinLayer>>(),
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
            width = itemsPerLayer.toDouble() * Meter,
            height = 3.0 * Meter,
            depth = layerCount.toDouble() * Meter,
            capacity = 600.0 * Kilogram,
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
                weight = 1.0 * Kilogram
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
                depthInMeter = layerCount.toDouble(),
                binType = sharedBinType,
                widthInMeter = itemsPerLayer.toDouble()
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
                gap = Flt64(0.01),
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
                    Bin(
                        shape = sharedBinType,
                        units = emptyList<QuantityPlacement3<BinLayer>>(),
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
                weight = 1.0 * Kilogram
            )
        }
        val binTypes = (0 until groupCount).map { index ->
            BinType(
                width = itemsPerLayer.toDouble() * Meter,
                height = 3.0 * Meter,
                depth = layersPerGroup.toDouble() * Meter,
                capacity = 600.0 * Kilogram,
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
                    depthInMeter = layersPerGroup.toDouble(),
                    binType = binTypes[groupIndex],
                    widthInMeter = itemsPerLayer.toDouble()
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
                gap = Flt64(0.01),
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
                    Bin(
                        shape = binType,
                        units = emptyList<QuantityPlacement3<BinLayer>>(),
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
        val maxElapsedSeconds = optionalDoubleProperty("bpp3d.gurobi.dataset.max.elapsed.seconds")
            ?: 120.0
        val maxMilpGap = optionalDoubleProperty("bpp3d.gurobi.dataset.max.milp.gap")
            ?: 0.05
        val maxLpGap = optionalDoubleProperty("bpp3d.gurobi.dataset.max.lp.gap")
            ?: 0.05

        printScenarioMetrics(
            caseName = "single-dataset",
            response = response
        )

        assertTrue(response.result.finalSolved)
        assertEquals(1, response.result.lpSolvedTimes)
        assertTrue(response.result.lpObjectives.isNotEmpty())
        assertTrue(response.result.elapsed > 0.seconds)
        assertTrue(response.result.elapsed <= maxElapsedSeconds.seconds)
        assertEquals(1, response.result.lpInfos.size)
        assertEquals("gurobi", response.result.lpInfos.first()["solver"])
        assertEquals("gurobi", response.result.finalInfo["solver"])
        assertTrue((response.result.finalInfo["milp_time_ms"] ?: "0").toLong() >= 0L)
        response.result.lpInfos.first()["lp_gap"]
            ?.toDoubleOrNull()
            ?.let { lpGap ->
                assertTrue(lpGap <= maxLpGap)
            }
        response.result.finalInfo["milp_gap"]
            ?.toDoubleOrNull()
            ?.let { milpGap ->
                assertTrue(milpGap <= maxMilpGap)
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
        val maxElapsedSeconds = optionalDoubleProperty("bpp3d.gurobi.dataset.suite.max.elapsed.seconds")
            ?: 180.0
        val maxTotalElapsedSeconds = optionalDoubleProperty("bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds")
            ?: 600.0
        val maxMilpGap = optionalDoubleProperty("bpp3d.gurobi.dataset.suite.max.milp.gap")
            ?: 0.05
        val maxLpGap = optionalDoubleProperty("bpp3d.gurobi.dataset.suite.max.lp.gap")
            ?: 0.05
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
            assertTrue(response.result.elapsed <= maxElapsedSeconds.seconds, scenarioCase.name)
            assertEquals(1, response.result.lpInfos.size, scenarioCase.name)
            assertEquals("gurobi", response.result.lpInfos.first()["solver"], scenarioCase.name)
            assertEquals("gurobi", response.result.finalInfo["solver"], scenarioCase.name)
            response.result.lpInfos.first()["lp_gap"]
                ?.toDoubleOrNull()
                ?.let { lpGap ->
                    assertTrue(
                        lpGap <= maxLpGap,
                        "${scenarioCase.name}: lp_gap=$lpGap exceeds $maxLpGap"
                    )
                }
            response.result.finalInfo["milp_gap"]
                ?.toDoubleOrNull()
                ?.let { milpGap ->
                    assertTrue(
                        milpGap <= maxMilpGap,
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

        assertTrue(totalElapsed <= maxTotalElapsedSeconds.seconds)
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
        val maxElapsedSeconds = optionalDoubleProperty("bpp3d.gurobi.random.dataset.max.elapsed.seconds")
            ?: 120.0
        val maxTotalElapsedSeconds = optionalDoubleProperty("bpp3d.gurobi.random.dataset.max.total.elapsed.seconds")
            ?: 300.0
        val maxMilpGap = optionalDoubleProperty("bpp3d.gurobi.random.dataset.max.milp.gap")
            ?: 0.05
        val maxLpGap = optionalDoubleProperty("bpp3d.gurobi.random.dataset.max.lp.gap")
            ?: 0.05
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
            assertTrue(response.result.elapsed <= maxElapsedSeconds.seconds, scenarioCase.name)
            assertEquals(1, response.result.lpInfos.size, scenarioCase.name)
            assertEquals("gurobi", response.result.lpInfos.first()["solver"], scenarioCase.name)
            assertEquals("gurobi", response.result.finalInfo["solver"], scenarioCase.name)
            response.result.lpInfos.first()["lp_gap"]
                ?.toDoubleOrNull()
                ?.let { lpGap ->
                    assertTrue(
                        lpGap <= maxLpGap,
                        "${scenarioCase.name}: lp_gap=$lpGap exceeds $maxLpGap"
                    )
                }
            response.result.finalInfo["milp_gap"]
                ?.toDoubleOrNull()
                ?.let { milpGap ->
                    assertTrue(
                        milpGap <= maxMilpGap,
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

        assertTrue(totalElapsed <= maxTotalElapsedSeconds.seconds)
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
                weight = (index + 1).toDouble() * Kilogram
            )
        }
        val binTypes = (0 until groupCount).map { index ->
            BinType(
                width = itemsPerLayer.toDouble() * Meter,
                height = 3.0 * Meter,
                depth = layersPerGroup.toDouble() * Meter,
                capacity = 1200.0 * Kilogram,
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
                    depthInMeter = layersPerGroup.toDouble(),
                    binType = binTypes[groupIndex],
                    widthInMeter = itemsPerLayer.toDouble()
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
                (expectedMaterialAmount.toLong() * (index + 1L)).toDouble() * Kilogram
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
                gap = Flt64(0.01),
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
                    Bin(
                        shape = binType,
                        units = emptyList<QuantityPlacement3<BinLayer>>(),
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
