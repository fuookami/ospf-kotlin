/**
 * 加法运算符
 * Addition Operator
 *
 * 定义加法运算相关接口，包括加法运算和自增运算。
 * 支持运算符重载，允许使用 + 和 ++ 运算符。
 *
 * Defines interfaces related to addition operations, including addition and increment.
 * Supports operator overloading, allowing the use of + and ++ operators.
 *
 * 数学定义 / Mathematical definition:
 * a + b (加法 / addition)
 * ++a = a + 1 (自增 / increment)
 *
 * 接口说明 / Interface descriptions:
 * - Plus: 加法运算符接口，支持 a + b
 * - PlusTrait: 加法运算符特征接口，用于类型约束
 * - PlusAssign: 加法赋值接口，支持 a += b
 * - Inc: 自增运算符接口，支持 ++a 或 a++
 */
package fuookami.ospf.kotlin.math.operator

interface Plus<in Rhs, out Ret> {
    operator fun plus(rhs: Rhs): Ret
}

interface PlusTrait<Self, in Rhs, out Ret> {
    operator fun Self.plus(rhs: Rhs): Ret
}

interface PlusAssign<in Rhs> {
    operator fun plusAssign(rhs: Rhs)
}

interface Inc<Self> {
    operator fun inc(): Inc<Self>
}
