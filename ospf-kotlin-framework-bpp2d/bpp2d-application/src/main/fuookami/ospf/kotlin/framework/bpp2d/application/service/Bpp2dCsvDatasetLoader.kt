/**
 * CSV 数据集加载器。 / CSV dataset loader.
 *
 * 与 BPP3D 的 CSV 输入协议对齐，提供两种二维排样数据集形态：
 * 1. `grouped-layer`：按 `group_index`、`layer_index` 分组的层数据；
 * 2. `material-width-amount`：按 `material`、`width`、`amount` 描述的物料需求。
 *
 * 二维专属列在两种形态中都可用：`height_meter`（矩形件高度）、`radius_meter`（圆形件半径）、
 * `shape_type`（`rectangle` / `circle`）、`allow_rotate`。
 * / Mirrors the BPP3D CSV input protocol with two 2D dataset shapes: `grouped-layer` (rows grouped by
 * `group_index` and `layer_index`) and `material-width-amount` (demands described by `material`, `width` and
 * `amount`). Both accept the 2D-specific columns `height_meter` (rectangle height), `radius_meter` (circle
 * radius), `shape_type` (`rectangle` / `circle`) and `allow_rotate`.
 *
 * schema 门禁：重复列拒绝、未知列拒绝、必填列缺失拒绝、非法枚举值拒绝，全部以 `Ret` 返回而不抛异常。
 * / Schema guards reject duplicate columns, unknown columns, missing required columns and invalid enum values,
 * always returning `Ret` instead of throwing.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.geometry2d.circleShape2
import fuookami.ospf.kotlin.framework.geometry2d.rectangleShape2
import fuookami.ospf.kotlin.framework.geometry2d.div

/**
 * CSV 数据集形态。 / CSV dataset shape.
*/
enum class Bpp2dCsvDatasetKind {
    /** 分组分层形态 / grouped-layer shape */
    GroupedLayer,

    /** 物料宽度数量形态 / material-width-amount shape */
    MaterialWidthAmount
}

/**
 * 解析后的 CSV 数据集。 / Parsed CSV dataset.
 *
 * @property kind 数据集形态 / dataset shape
 * @property items 物品与件数 / items and amounts
 * @property sheetTypes 从数据推断的板材类型 / sheet types inferred from the data
 * @property rows 原始行数 / raw row count
*/
data class Bpp2dCsvDataset(
    val kind: Bpp2dCsvDatasetKind,
    val items: List<Pair<ActualItem<FltX>, UInt64>>,
    val sheetTypes: List<SheetType<FltX>>,
    val rows: Int
)

/**
 * CSV 数据集加载器。 / CSV dataset loader.
 *
 * @property defaultSheetWidth 缺省板材宽度（米） / default sheet width in metres
 * @property defaultSheetHeight 缺省板材高度（米） / default sheet height in metres
*/
class Bpp2dCsvDatasetLoader(
    private val defaultSheetWidth: Double = 20.0,
    private val defaultSheetHeight: Double = 12.0
) {
    companion object {
        /** 分组分层形态的必填列。 / Required columns of the grouped-layer shape. */
        private val GROUPED_REQUIRED = listOf("group_index", "layer_index", "item_id", "amount")

        /** 物料宽度数量形态的必填列。 / Required columns of the material-width-amount shape. */
        private val MATERIAL_REQUIRED = listOf("material", "width", "amount")

        /** 两种形态都允许的可选列。 / Optional columns allowed in both shapes. */
        private val OPTIONAL = listOf(
            "width",
            "height_meter",
            "radius_meter",
            "shape_type",
            "allow_rotate",
            "material_name",
            "material_no",
            "material_weight_kg",
            "sheet_width_meter",
            "sheet_height_meter",
            "sheet_type"
        )
    }

    /**
     * 从 CSV 文本加载数据集。 / Load a dataset from CSV text.
     *
     * @param text CSV 文本，首行为表头 / CSV text whose first line is the header
     * @param kind 期望的数据集形态，null 表示按表头自动识别 / expected dataset shape, null meaning auto-detect
     * @return 数据集或失败 / dataset, or failure
    */
    fun load(text: String, kind: Bpp2dCsvDatasetKind? = null): Ret<Bpp2dCsvDataset> {
        val rawLines = text.split("\r\n", "\n").filter { it.isNotBlank() }
        if (rawLines.isEmpty()) {
            return Failed(ErrorCode.IllegalArgument, "CSV 为空 / CSV is empty")
        }
        val header = rawLines.first().split(",").map { it.trim() }
        val duplicate = header.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicate.isNotEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CSV 存在重复列：${duplicate.joinToString()} / Duplicate CSV columns: ${duplicate.joinToString()}"
            )
        }

        val detected = when {
            GROUPED_REQUIRED.all { header.contains(it) } -> Bpp2dCsvDatasetKind.GroupedLayer
            MATERIAL_REQUIRED.all { header.contains(it) } -> Bpp2dCsvDatasetKind.MaterialWidthAmount
            else -> return Failed(
                ErrorCode.IllegalArgument,
                "CSV 表头无法识别：（需要 ${GROUPED_REQUIRED.joinToString()} 或 " +
                        "${MATERIAL_REQUIRED.joinToString()}）/ Unrecognised CSV header"
            )
        }
        if (kind != null && kind != detected) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CSV 形态不匹配：声明 $kind，实际 $detected / Declared $kind but detected $detected"
            )
        }

        val allowed = when (detected) {
            Bpp2dCsvDatasetKind.GroupedLayer -> GROUPED_REQUIRED + OPTIONAL
            Bpp2dCsvDatasetKind.MaterialWidthAmount -> MATERIAL_REQUIRED + OPTIONAL
        }
        val unknown = header.filter { it !in allowed }
        if (unknown.isNotEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CSV 存在未知列：${unknown.joinToString()} / Unknown CSV columns: ${unknown.joinToString()}"
            )
        }

        val items = ArrayList<Pair<ActualItem<FltX>, UInt64>>()
        val sheetTypes = ArrayList<SheetType<FltX>>()
        val accumulator = LinkedHashMap<String, ItemAccumulator>()

        for (lineIndex in 1 until rawLines.size) {
            val cells = rawLines[lineIndex].split(",").map { it.trim() }
            if (cells.size != header.size) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "第 ${lineIndex + 1} 行列数不匹配：期望 ${header.size}，实际 ${cells.size} / " +
                            "Row ${lineIndex + 1} has ${cells.size} cells but the header has ${header.size}"
                )
            }
            val row = header.indices.associate { header[it] to cells[it] }

            val itemId = when (detected) {
                Bpp2dCsvDatasetKind.GroupedLayer -> row["item_id"].orEmpty()
                Bpp2dCsvDatasetKind.MaterialWidthAmount -> row["material"].orEmpty()
            }
            if (itemId.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "第 ${lineIndex + 1} 行缺少物品标识 / Row ${lineIndex + 1} is missing the item identifier"
                )
            }

            val amount = when (val parsed = parseAmount(row["amount"], lineIndex)) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val width = when (val parsed = parseMeter(row["width"], "width", lineIndex)) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val height = when (val parsed = parseMeter(row["height_meter"], "height_meter", lineIndex, optional = true)) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val radius = when (val parsed = parseMeter(row["radius_meter"], "radius_meter", lineIndex, optional = true)) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val allowRotate = when (val parsed = parseBoolean(row["allow_rotate"], lineIndex)) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val shapeType = when (val parsed = parseShapeType(row["shape_type"], lineIndex)) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }

            val sheetWidth = when (
                val parsed = parseMeter(row["sheet_width_meter"], "sheet_width_meter", lineIndex, optional = true)
            ) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val sheetHeight = when (
                val parsed = parseMeter(row["sheet_height_meter"], "sheet_height_meter", lineIndex, optional = true)
            ) {
                is Ok -> parsed.value
                is Failed -> return Failed(parsed.error)
                is Fatal -> return Fatal(parsed.errors)
            }
            val sheetType = sheetTypeOf(sheetWidth, sheetHeight)
            if (sheetTypes.none { it.typeCode == sheetType.typeCode }) {
                sheetTypes.add(sheetType)
            }

            val accumulatorEntry = accumulator.getOrPut(itemId) {
                ItemAccumulator(
                    id = itemId,
                    width = width ?: sheetType.width,
                    height = height,
                    radius = radius,
                    shapeType = shapeType,
                    allowRotate = allowRotate
                )
            }
            if (!accumulatorEntry.matches(width, height, radius, shapeType, allowRotate)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "物品 $itemId 在多行中的尺寸或形状不一致 / Item $itemId has inconsistent geometry across rows"
                )
            }
            accumulatorEntry.amount += amount.toULong()
        }

        for (entry in accumulator.values) {
            val shape = when (entry.shapeType) {
                CsvShapeType.Circle -> {
                    // 圆形件可用 radius_meter 直接给出半径，也可用 width 作为直径推导半径。
                    // A circle may give its radius through radius_meter, or its diameter through width.
                    val radius = entry.radius
                        ?: (entry.width / entry.width.value.constants.two)
                    circleShape2(radius)
                }

                CsvShapeType.Rectangle -> rectangleShape2(
                    width = entry.width,
                    height = entry.height ?: entry.width
                )
            }
            val concrete = when (shape) {
                is Ok -> shape.value
                is Failed -> return Failed(shape.error)
                is Fatal -> return Fatal(shape.errors)
            }
            items.add(
                ActualItem(
                    id = ItemId(entry.id),
                    name = entry.id,
                    shape = concrete,
                    allowRotate = entry.allowRotate,
                    weight = FltX(1.0) * Kilogram
                ) to UInt64(entry.amount)
            )
        }

        if (sheetTypes.isEmpty()) {
            sheetTypes.add(sheetTypeOf(null, null))
        }

        return ok(
            Bpp2dCsvDataset(
                kind = detected,
                items = items,
                sheetTypes = sheetTypes,
                rows = rawLines.size - 1
            )
        )
    }

    /**
     * 物品在 CSV 多行中的累积状态。 / Accumulated state of one item across CSV rows.
     *
     * @property id 物品标识 / item identifier
     * @property width 宽度 / width
     * @property height 高度 / height
     * @property radius 半径 / radius
     * @property shapeType 形状类型 / shape type
     * @property allowRotate 是否允许旋转 / whether rotation is allowed
    */
    private class ItemAccumulator(
        val id: String,
        val width: Quantity<FltX>,
        val height: Quantity<FltX>?,
        val radius: Quantity<FltX>?,
        val shapeType: CsvShapeType,
        val allowRotate: Boolean
    ) {
        var amount: ULong = 0uL

        /**
         * 判断后续行的几何与当前行是否一致。 / Check whether a later row matches the current geometry.
         *
         * @param width 宽度 / width
         * @param height 高度 / height
         * @param radius 半径 / radius
         * @param shapeType 形状类型 / shape type
         * @param allowRotate 是否允许旋转 / whether rotation is allowed
         * @return 是否一致 / whether consistent
        */
        fun matches(
            width: Quantity<FltX>?,
            height: Quantity<FltX>?,
            radius: Quantity<FltX>?,
            shapeType: CsvShapeType,
            allowRotate: Boolean
        ): Boolean {
            if (this.shapeType != shapeType || this.allowRotate != allowRotate) {
                return false
            }
            if (width != null && !sameValue(this.width, width)) {
                return false
            }
            if (height != null && (this.height == null || !sameValue(this.height, height))) {
                return false
            }
            if (radius != null && (this.radius == null || !sameValue(this.radius, radius))) {
                return false
            }
            return true
        }

        /**
         * 比较两个量值的数值与单位。 / Compare two quantities by value and unit.
         *
         * @param lhs 左量值 / left quantity
         * @param rhs 右量值 / right quantity
         * @return 是否相等 / whether equal
        */
        private fun sameValue(lhs: Quantity<FltX>, rhs: Quantity<FltX>): Boolean {
            return lhs.value == rhs.value && lhs.unit == rhs.unit
        }
    }

    /**
     * CSV 形状类型。 / CSV shape type.
    */
    private enum class CsvShapeType {
        /** 矩形件 / rectangle */
        Rectangle,

        /** 圆形件 / circle */
        Circle
    }

    /**
     * 解析件数。 / Parse an amount.
     *
     * @param raw 原始文本 / raw text
     * @param lineIndex 行号 / row index
     * @return 件数或失败 / amount, or failure
    */
    private fun parseAmount(raw: String?, lineIndex: Int): Ret<ULong> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "第 ${lineIndex + 1} 行缺少 amount / Row ${lineIndex + 1} is missing amount"
            )
        }
        val parsed = text.toULongOrNull() ?: return Failed(
            ErrorCode.IllegalArgument,
            "第 ${lineIndex + 1} 行 amount 非法：$text / Row ${lineIndex + 1} has an invalid amount: $text"
        )
        if (parsed == 0uL) {
            return Failed(
                ErrorCode.IllegalArgument,
                "第 ${lineIndex + 1} 行 amount 必须为正 / Row ${lineIndex + 1} amount must be positive"
            )
        }
        return ok(parsed)
    }

    /**
     * 解析米制尺寸。 / Parse a metric size.
     *
     * @param raw 原始文本 / raw text
     * @param column 列名 / column name
     * @param lineIndex 行号 / row index
     * @param optional 是否可选 / whether the column is optional
     * @return 量值或失败 / quantity, or failure
    */
    private fun parseMeter(
        raw: String?,
        column: String,
        lineIndex: Int,
        optional: Boolean = false
    ): Ret<Quantity<FltX>?> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            return if (optional) {
                ok(null)
            } else {
                Failed(
                    ErrorCode.IllegalArgument,
                    "第 ${lineIndex + 1} 行缺少 $column / Row ${lineIndex + 1} is missing $column"
                )
            }
        }
        val parsed = text.toDoubleOrNull() ?: return Failed(
            ErrorCode.IllegalArgument,
            "第 ${lineIndex + 1} 行 $column 非法：$text / Row ${lineIndex + 1} has an invalid $column: $text"
        )
        if (parsed <= 0.0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "第 ${lineIndex + 1} 行 $column 必须为正 / Row ${lineIndex + 1} $column must be positive"
            )
        }
        return ok(FltX(parsed) * Meter)
    }

    /**
     * 解析布尔值。 / Parse a boolean.
     *
     * @param raw 原始文本 / raw text
     * @param lineIndex 行号 / row index
     * @return 布尔值或失败 / boolean, or failure
    */
    private fun parseBoolean(raw: String?, lineIndex: Int): Ret<Boolean> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            // 缺省允许旋转，与领域模型默认一致。 / Defaults to allowing rotation, matching the domain default.
            return ok(true)
        }
        return when (text.lowercase()) {
            "true", "1", "yes", "y" -> ok(true)
            "false", "0", "no", "n" -> ok(false)
            else -> Failed(
                ErrorCode.IllegalArgument,
                "第 ${lineIndex + 1} 行 allow_rotate 非法：$text / Row ${lineIndex + 1} has an invalid allow_rotate: $text"
            )
        }
    }

    /**
     * 解析形状类型。 / Parse a shape type.
     *
     * @param raw 原始文本 / raw text
     * @param lineIndex 行号 / row index
     * @return 形状类型或失败 / shape type, or failure
    */
    private fun parseShapeType(raw: String?, lineIndex: Int): Ret<CsvShapeType> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            return ok(CsvShapeType.Rectangle)
        }
        return when (text.lowercase()) {
            "rectangle", "rect" -> ok(CsvShapeType.Rectangle)
            "circle" -> ok(CsvShapeType.Circle)
            else -> Failed(
                ErrorCode.IllegalArgument,
                "第 ${lineIndex + 1} 行 shape_type 非法：$text（只支持 rectangle / circle）/ " +
                        "Row ${lineIndex + 1} has an invalid shape_type: $text (only rectangle / circle)"
            )
        }
    }

    /**
     * 由尺寸构造板材类型。 / Build a sheet type from sizes.
     *
     * @param width 宽度，null 时用缺省值 / width, defaulting when null
     * @param height 高度，null 时用缺省值 / height, defaulting when null
     * @return 板材类型 / sheet type
    */
    private fun sheetTypeOf(width: Quantity<FltX>?, height: Quantity<FltX>?): SheetType<FltX> {
        val actualWidth = width ?: (FltX(defaultSheetWidth) * Meter)
        val actualHeight = height ?: (FltX(defaultSheetHeight) * Meter)
        return SheetType(
            width = actualWidth,
            height = actualHeight,
            maxLoad = FltX(10000.0) * Kilogram,
            typeCode = SheetTypeId("CSV"),
            isMain = true
        )
    }
}