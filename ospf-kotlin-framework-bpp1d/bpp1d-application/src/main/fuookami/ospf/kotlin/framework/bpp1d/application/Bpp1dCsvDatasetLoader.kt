package fuookami.ospf.kotlin.framework.bpp1d.application

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*

/** 严格 CSV 读取边界，长度/重量单位由调用方显式声明。 / Strict CSV boundary with caller-declared units.
 * @property parse 数字解析器，不经 Double 损失领域精度 / Numeric parser without an intermediate Double
 */
class Bpp1dCsvDatasetLoader<V : FloatingNumber<V>>(private val parse: (String) -> Ret<V>) {
    /** 读取物品和箱型，表头分别为 id,length,weight,amount 和 id,length,max_weight,cost,inventory。
     * / Read item and bin tables with explicit, closed schemas.
     * @param itemsText 物品 CSV / Item CSV
     * @param binsText 箱型 CSV / Bin CSV
     * @param lengthUnit 长度单位 / Length unit
     * @param weightUnit 重量单位 / Weight unit
     * @param spacing 间距 / Spacing
     * @param objective 目标 / Objective
     * @return 验证后的输入 / Validated input
     */
    fun load(
        itemsText: String,
        binsText: String,
        lengthUnit: PhysicalUnit,
        weightUnit: PhysicalUnit,
        spacing: Quantity<V>,
        objective: PackingObjective = PackingObjective.BinCount
    ): Ret<PackingProblem<V>> {
        val itemRows = when (val result = rows(itemsText, listOf("id", "length", "weight", "amount"))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val binRows = when (val result = rows(binsText, listOf("id", "length", "max_weight", "cost", "inventory"))) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val items = arrayListOf<Item<V>>()
        for (row in itemRows) {
            val length = when (val result = parse(row.getValue("length"))) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val weight = when (val result = parse(row.getValue("weight"))) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val amount = row.getValue("amount").toULongOrNull() ?: return invalid("invalid amount")
            items.add(Item(id = row.getValue("id"), length = Quantity(length, lengthUnit), weight = Quantity(weight, weightUnit), amount = UInt64(amount)))
        }
        val bins = arrayListOf<BinType<V>>()
        for (row in binRows) {
            val length = when (val result = parse(row.getValue("length"))) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val weight = when (val result = parse(row.getValue("max_weight"))) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val cost = when (val result = parse(row.getValue("cost"))) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val stock = row.getValue("inventory")
            val inventory = if (stock.isEmpty()) null else stock.toULongOrNull()?.let { UInt64(it) } ?: return invalid("invalid inventory")
            bins.add(BinType(id = row.getValue("id"), length = Quantity(length, lengthUnit), maxWeight = Quantity(weight, weightUnit), cost = cost, inventory = inventory))
        }
        val problem = PackingProblem(items = items, binTypes = bins, spacing = spacing, objective = objective)
        return when (val result = problem.validate()) {
            is Ok -> Ok(problem)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun <T> invalid(reason: String): Ret<T> = Failed(ErrorCode.IllegalArgument, "CSV 输入无效 / Invalid CSV input: $reason")

    private fun rows(text: String, schema: List<String>): Ret<List<Map<String, String>>> {
        val records = arrayListOf<List<String>>()
        val fields = arrayListOf<String>()
        val value = StringBuilder()
        var quoted = false
        var closed = false
        var index = 0
        fun field() { fields.add(value.toString().trim()); value.clear(); closed = false }
        while (index < text.length) {
            val ch = text[index++]
            when {
                quoted && ch == '"' -> if (index < text.length && text[index] == '"') { value.append('"'); index++ } else { quoted = false; closed = true }
                quoted -> value.append(ch)
                ch == '"' && value.isEmpty() && !closed -> quoted = true
                ch == ',' -> field()
                ch == '\n' || ch == '\r' -> {
                    if (ch == '\r' && index < text.length && text[index] == '\n') index++
                    field()
                    if (fields.any { it.isNotEmpty() }) records.add(fields.toList())
                    fields.clear()
                }
                ch == '"' || (closed && !ch.isWhitespace()) -> return invalid("malformed quote")
                else -> value.append(ch)
            }
        }
        if (quoted) return invalid("unclosed quote")
        field()
        if (fields.any { it.isNotEmpty() }) records.add(fields.toList())
        val header = records.firstOrNull()?.map { it.removePrefix("\uFEFF") } ?: return invalid("missing header")
        if (header.size != schema.size || header.toSet() != schema.toSet()) return invalid("schema must be $schema")
        if (records.drop(1).any { it.size != header.size }) return invalid("row width")
        return Ok(records.drop(1).map { header.zip(it).toMap() })
    }
}
