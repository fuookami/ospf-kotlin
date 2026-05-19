/**
 * 除法运算笌
 * Division Operator
 *
 * 定义除法运算相关接口，包括常规除法、除法赋值和整数除法。
 * 支持运算符重载，允许使用 / 和/= 运算符进行除法运算。
 *
 * Defines interfaces related to division operations, including regular division,
 * division assignment, and integer division.
 * Supports operator overloading, allowing the use of / and /= operators for division.
 *
 * 数学定义 / Mathematical definition:
 * a / b = a * b⁻¹(常规除法 / regular division)
 * a intDiv b = floor(a / b) (整数除法 / integer division)
 *
 * 接口说明 / Interface descriptions:
 * - Div: 除法运算符接口，支持 a / b
 * - DivAssign: 除法赋值接口，支持 a /= b
 * - IntDiv: 整数除法接口，支挌a intDiv b
 * - IntDivAssign: 整数除法赋值接口，支持 a intDivAssign b
 */
package fuookami.ospf.kotlin.math.operator

/**
 * 除法运算符接双
 * Division Operator Interface
 *
 * 定义除法运算，支挌/ 运算符。
 * 除法是乘法的逆运算，将一个数除以另一个数得到商。
 *
 * Defines the division operation, supporting the / operator.
 * Division is the inverse operation of multiplication, dividing one number by another to get a quotient.
 *
 * @param Rhs 右操作数（除数）的类垌
 * @param Ret 返回值（商）的类垌
 *
 * @param Rhs The type of the right operand (divisor)
 * @param Ret The type of the return value (quotient)
 */
interface Div<in Rhs, out Ret> {
    /**
     * 除法运算符，计算啌
     * Division operator, calculates the quotient
     *
     * @param rhs 除数
     * @return 啌
     *
     * @param rhs Divisor
     * @return Quotient
     */
    operator fun div(rhs: Rhs): Ret
}

/**
 * 除法赋值接双
 * Division Assignment Interface
 *
 * 定义除法赋值运算，支持 /= 运算符。
 * 将左操作数除以右操作数，并将结果赋值给左操作数。
 *
 * Defines the division assignment operation, supporting the /= operator.
 * Divides the left operand by the right operand and assigns the result to the left operand.
 *
 * @param Rhs 右操作数（除数）的类垌
 *
 * @param Rhs The type of the right operand (divisor)
 */
interface DivAssign<in Rhs> {
    /**
     * 除法赋值运算符，将商赋值给左操作数
     * Division assignment operator, assigns the quotient to the left operand
     *
     * @param rhs 除数
     *
     * @param rhs Divisor
     */
    operator fun divAssign(rhs: Rhs)
}

/**
 * 整数除法接口
 * Integer Division Interface
 *
 * 定义整数除法运算，支挌intDiv 中缀函数。
 * 整数除法返回商的整数部分，舍弃小数部分。
 *
 * Defines the integer division operation, supporting the intDiv infix function.
 * Integer division returns the integer part of the quotient, discarding the fractional part.
 *
 * @param Rhs 右操作数（除数）的类垌
 * @param Ret 返回值（整数商）的类垌
 *
 * @param Rhs The type of the right operand (divisor)
 * @param Ret The type of the return value (integer quotient)
 */
interface IntDiv<in Rhs, out Ret> {
    /**
     * 整数除法运算符，计算整数啌
     * Integer division operator, calculates the integer quotient
     *
     * @param rhs 除数
     * @return 整数啌
     *
     * @param rhs Divisor
     * @return Integer quotient
     */
    infix fun intDiv(rhs: Rhs): Ret
}

/**
 * 整数除法赋值接双
 * Integer Division Assignment Interface
 *
 * 定义整数除法赋值运算，支持 intDivAssign 中缀函数。
 * 将左操作数整数除以右操作数，并将结果赋值给左操作数。
 *
 * Defines the integer division assignment operation, supporting the intDivAssign infix function.
 * Divides the left operand by the right operand using integer division and assigns the result to the left operand.
 *
 * @param Rhs 右操作数（除数）的类垌
 *
 * @param Rhs The type of the right operand (divisor)
 */
interface IntDivAssign<in Rhs> {
    /**
     * 整数除法赋值运算符，将整数商赋值给左操作数
     * Integer division assignment operator, assigns the integer quotient to the left operand
     *
     * @param rhs 除数
     *
     * @param rhs Divisor
     */
    infix fun intDivAssign(rhs: Rhs)
}