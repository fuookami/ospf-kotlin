package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Represents a minor aircraft model variant identifier.
 * 表示航空器子型号标识。
 *
 * @property model The minor model identifier string. / 子型号标识字符串
*/
@JvmInline
value class AircraftMinorModel(val model: String)

/**
 * Represents an aircraft registration number.
 * 表示航空器注册号。
 *
 * @property no The registration number string. / 注册号字符串
*/
@JvmInline
value class RegNo(val no: String)

/**
 * Represents a flight number.
 * 表示航班号。
 *
 * @property no The flight number string. / 航班号字符串
*/
@JvmInline
value class FlightNo(val no: String)

/**
 * Represents an IATA airport code.
 * 表示 IATA 机场代码。
 *
 * @property code The IATA code string. / IATA 代码字符串
*/
@JvmInline
value class IATA(val code: String)

/**
 * Represents a Mean Aerodynamic Chord (MAC) percentage value with partial and total ordering support.
 * 表示平均空气动力弦（MAC）百分比值，支持偏序和全序比较。
 *
 * @property mac The MAC percentage value. / MAC 百分比值
*/
@JvmInline
value class MAC(val mac: Flt64): PartialOrd<MAC>, Ord<MAC> {
    companion object {
        private val precision = Flt64(1e-3)
    }

    override fun partialOrd(rhs: MAC): Order {
        val less = Less<Flt64, Flt64>(precision)
        return if (less(mac, rhs.mac)) {
            Order.Less()
        } else if (less(rhs.mac, mac)) {
            Order.Greater()
        } else {
            Order.Equal
        }
    }

}

/**
 * Represents a horizontal stabilizer angle setting.
 * 表示水平安定面角度设置。
 *
 * @property angle The stabilizer angle as a string. / 安定面角度字符串
*/
@JvmInline
value class HorizontalStabilizerAngle(val angle: String)

/**
 * Represents a horizontal stabilizer thrust derate modifier.
 * 表示水平安定面推力减额修正量。
 *
 * @property mod The thrust derate modifier as a string. / 推力减额修正量字符串
*/
@JvmInline
value class HorizontalStabilizerThrustDrate(val mod: String)
