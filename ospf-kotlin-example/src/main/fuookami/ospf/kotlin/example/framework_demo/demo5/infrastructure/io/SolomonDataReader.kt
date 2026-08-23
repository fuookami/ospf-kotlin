package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.io

import java.math.BigDecimal
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/** Solomon 示例文本读取器。 / Solomon demo text reader. */
object SolomonDataReader {
    private data class SourceLine(val number: Int, val text: String)

    /**
     * 读取标准 Solomon 文本。 / Read standard Solomon text.
     *
     * @param text Solomon 文本 / Solomon text
     * @return 原始示例数据或带行号的解析失败 / Raw demo data or a line-aware parsing failure
     */
    fun read(text: String): Ret<SolomonData> {
        val lines = text.lineSequence()
            .mapIndexed { index, line -> SourceLine(index + 1, line.trim()) }
            .filter { it.text.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) {
            return networkSchedulingFailure(
                "读取 Solomon 数据失败：输入为空 / Failed to read Solomon data: input is empty"
            )
        }

        val vehicleHeader = lines.indexOfFirst { it.text.equals("VEHICLE", ignoreCase = true) }
        val customerHeader = lines.indexOfFirst { it.text.equals("CUSTOMER", ignoreCase = true) }
        if (vehicleHeader < 0 || customerHeader <= vehicleHeader) {
            return networkSchedulingFailure(
                "读取 Solomon 数据失败：缺少 VEHICLE 或 CUSTOMER 段 / " +
                        "Failed to read Solomon data: VEHICLE or CUSTOMER section is missing"
            )
        }

        val vehicleLine = lines.subList(vehicleHeader + 1, customerHeader)
            .firstOrNull { line ->
                val fields = fields(line.text)
                fields.size == 2 && fields[0].toIntOrNull() != null && decimal(fields[1]) != null
            }
            ?: return networkSchedulingFailure(
                "读取 Solomon 数据失败：VEHICLE 段缺少车辆数量和容量 / " +
                        "Failed to read Solomon data: vehicle amount and capacity are missing"
            )
        val vehicleFields = fields(vehicleLine.text)
        val amount = vehicleFields[0].toIntOrNull()
        val capacity = decimal(vehicleFields[1])
        if (amount == null || amount <= 0 || capacity == null || capacity.signum() <= 0) {
            return lineFailure(
                line = vehicleLine,
                chinese = "车辆数量和容量必须为正数",
                english = "vehicle amount and capacity must be positive"
            )
        }

        val nodeRows = mutableListOf<SolomonNodeData>()
        for (line in lines.drop(customerHeader + 1)) {
            val columns = fields(line.text)
            if (columns.firstOrNull()?.toBigDecimalOrNull() == null) {
                continue
            }
            if (columns.size != 7) {
                return lineFailure(
                    line = line,
                    chinese = "客户数据必须恰好包含 7 列，实际为 ${columns.size} 列",
                    english = "customer data must contain exactly 7 columns, got ${columns.size}"
                )
            }
            val numbers = columns.drop(1).map(::decimal)
            if (numbers.any { it == null }) {
                return lineFailure(
                    line = line,
                    chinese = "客户数据包含非法数值",
                    english = "customer data contains an invalid number"
                )
            }
            val values = numbers.filterNotNull()
            if (values[2].signum() < 0 || values[3].signum() < 0 || values[4] < values[3] || values[5].signum() < 0) {
                return lineFailure(
                    line = line,
                    chinese = "需求、时间窗或服务时间非法",
                    english = "demand, time window, or service time is invalid"
                )
            }
            nodeRows.add(
                SolomonNodeData(
                    id = columns[0],
                    x = FltX(values[0]),
                    y = FltX(values[1]),
                    demand = FltX(values[2]),
                    readyTime = FltX(values[3]),
                    dueTime = FltX(values[4]),
                    serviceTime = FltX(values[5])
                )
            )
        }
        if (nodeRows.isEmpty()) {
            return networkSchedulingFailure(
                "读取 Solomon 数据失败：CUSTOMER 段没有节点数据 / " +
                        "Failed to read Solomon data: CUSTOMER section contains no node data"
            )
        }
        val duplicateId = nodeRows.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicateId != null) {
            return networkSchedulingFailure(
                "读取 Solomon 数据失败：节点 ID $duplicateId 重复 / " +
                        "Failed to read Solomon data: duplicate node ID $duplicateId"
            )
        }
        val depot = nodeRows.singleOrNull { it.id == "0" }
            ?: return networkSchedulingFailure(
                "读取 Solomon 数据失败：必须且只能存在一个 ID 为 0 的 depot / " +
                        "Failed to read Solomon data: exactly one depot with ID 0 is required"
            )
        return ok(
            SolomonData(
                name = lines.first().text,
                vehicle = SolomonVehicleData(amount, FltX(capacity)),
                depot = depot,
                customers = nodeRows.filter { it.id != depot.id }
            )
        )
    }

    private fun fields(line: String): List<String> = line.split(Regex("\\s+"))

    private fun decimal(value: String): BigDecimal? = value.toBigDecimalOrNull()

    private fun <T> lineFailure(line: SourceLine, chinese: String, english: String): Ret<T> {
        return networkSchedulingFailure(
            "读取 Solomon 数据失败：第 ${line.number} 行$chinese / " +
                    "Failed to read Solomon data: line ${line.number} $english"
        )
    }
}
