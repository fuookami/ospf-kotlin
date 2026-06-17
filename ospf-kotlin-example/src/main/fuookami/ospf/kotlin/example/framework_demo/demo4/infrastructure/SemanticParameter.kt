@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure

/** 包装 3 字符 IATA 机场代码的内联值类。Inline value class wrapping a 3-character IATA airport code. */
@JvmInline
value class IATA(val code: String) {
    init {
        assert(code.length == 3)
    }

    override fun toString() = code
}

/** 包装 4 字符 ICAO 机场代码的内联值类。Inline value class wrapping a 4-character ICAO airport code. */
@JvmInline
value class ICAO(val code: String) {
    init {
        assert(code.length == 4)
    }

    override fun toString() = code
}

/** 包装飞机类型名称的内联值类。Inline value class wrapping an aircraft type name. */
@JvmInline
value class AircraftTypeName(val name: String) {
    override fun toString() = name
}

/** 包装飞机类型代码的内联值类。Inline value class wrapping an aircraft type code. */
@JvmInline
value class AircraftTypeCode(val code: String) {
    override fun toString() = code
}

/** 包装飞机子类型名称的内联值类。Inline value class wrapping an aircraft minor type name. */
@JvmInline
value class AircraftMinorTypeName(val name: String) {
    override fun toString() = name
}

/** 包装飞机子类型代码的内联值类。Inline value class wrapping an aircraft minor type code. */
@JvmInline
value class AircraftMinorTypeCode(val code: String) {
    override fun toString() = code
}

/** 包装机翼飞机类型代码的内联值类。Inline value class wrapping a wing aircraft type code. */
@JvmInline
value class WingAircraftTypeCode(val code: String) {
    override fun toString() = code
}

/** 包装飞机注册号的内联值类。Inline value class wrapping an aircraft register number. */
@JvmInline
value class AircraftRegisterNumber(val no: String) {
    override fun toString() = no
}

/** 包装舱位标识符的内联值类。Inline value class wrapping a passenger class identifier. */
@JvmInline
value class PassengerClass(val cls: String) {
    override fun toString() = cls
}

/** 包装飞行员职级编号的内联值类。Inline value class wrapping a pilot rank number. */
@JvmInline
value class PilotRankNo(val no: String) {
    override fun toString() = no
}

/** 包装飞行员代码的内联值类。Inline value class wrapping a pilot code. */
@JvmInline
value class PilotCode(val code: String) {
    override fun toString() = code
}

/** 包装机组成员职级编号的内联值类。Inline value class wrapping a crew member rank number. */
@JvmInline
value class CrewManRankNo(val no: String) {
    override fun toString() = no
}

/** 包装工号的内联值类。Inline value class wrapping a worker number. */
@JvmInline
value class WorkerNo(val no: String) {
    override fun toString() = no
}
