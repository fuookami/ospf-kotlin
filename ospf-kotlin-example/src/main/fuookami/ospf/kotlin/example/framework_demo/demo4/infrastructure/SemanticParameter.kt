@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure

/**
 * Inline value class wrapping a 3-character IATA airport code.
 * 包装 3 字符 IATA 机场代码的内联值类。
 *
 * @property code The 3-character IATA airport code / 3字符IATA机场代码
*/
@JvmInline
value class IATA(val code: String) {
    init {
        assert(code.length == 3)
    }

    override fun toString() = code
}

/**
 * Inline value class wrapping a 4-character ICAO airport code.
 * 包装 4 字符 ICAO 机场代码的内联值类。
 *
 * @property code The 4-character ICAO airport code / 4字符ICAO机场代码
*/
@JvmInline
value class ICAO(val code: String) {
    init {
        assert(code.length == 4)
    }

    override fun toString() = code
}

/**
 * Inline value class wrapping an aircraft type name.
 * 包装飞机类型名称的内联值类。
 *
 * @property name 飞机类型名称字符串 / The aircraft type name string
*/
@JvmInline
value class AircraftTypeName(val name: String) {
    override fun toString() = name
}

/**
 * Inline value class wrapping an aircraft type code.
 * 包装飞机类型代码的内联值类。
 *
 * @property code 飞机类型代码字符串 / The aircraft type code string
*/
@JvmInline
value class AircraftTypeCode(val code: String) {
    override fun toString() = code
}

/**
 * Inline value class wrapping an aircraft minor type name.
 * 包装飞机子类型名称的内联值类。
 *
 * @property name 飞机子类型名称字符串 / The aircraft minor type name string
*/
@JvmInline
value class AircraftMinorTypeName(val name: String) {
    override fun toString() = name
}

/**
 * Inline value class wrapping an aircraft minor type code.
 * 包装飞机子类型代码的内联值类。
 *
 * @property code 飞机子类型代码字符串 / The aircraft minor type code string
*/
@JvmInline
value class AircraftMinorTypeCode(val code: String) {
    override fun toString() = code
}

/**
 * Inline value class wrapping a wing aircraft type code.
 * 包装机翼飞机类型代码的内联值类。
 *
 * @property code 机翼飞机类型代码字符串 / The wing aircraft type code string
*/
@JvmInline
value class WingAircraftTypeCode(val code: String) {
    override fun toString() = code
}

/**
 * Inline value class wrapping an aircraft register number.
 * 包装飞机注册号的内联值类。
 *
 * @property no 飞机注册号字符串 / The aircraft register number string
*/
@JvmInline
value class AircraftRegisterNumber(val no: String) {
    override fun toString() = no
}

/**
 * Inline value class wrapping a passenger class identifier.
 * 包装舱位标识符的内联值类。
 *
 * @property cls 舱位标识符字符串 / The passenger class identifier string
*/
@JvmInline
value class PassengerClass(val cls: String) {
    override fun toString() = cls
}

/**
 * Inline value class wrapping a pilot rank number.
 * 包装飞行员职级编号的内联值类。
 *
 * @property no 飞行员职级编号字符串 / The pilot rank number string
*/
@JvmInline
value class PilotRankNo(val no: String) {
    override fun toString() = no
}

/**
 * Inline value class wrapping a pilot code.
 * 包装飞行员代码的内联值类。
 *
 * @property code 飞行员代码字符串 / The pilot code string
*/
@JvmInline
value class PilotCode(val code: String) {
    override fun toString() = code
}

/**
 * Inline value class wrapping a crew member rank number.
 * 包装机组成员职级编号的内联值类。
 *
 * @property no 机组成员职级编号字符串 / The crew member rank number string
*/
@JvmInline
value class CrewManRankNo(val no: String) {
    override fun toString() = no
}

/**
 * Inline value class wrapping a worker number.
 * 包装工号的内联值类。
 *
 * @property no 工号字符串 / The worker number string
*/
@JvmInline
value class WorkerNo(val no: String) {
    override fun toString() = no
}
