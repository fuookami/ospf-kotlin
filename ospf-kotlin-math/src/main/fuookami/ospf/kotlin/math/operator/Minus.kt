/**
 * 减法运算笌
 * Subtraction Operator
 *
 * 定义减法运算相关接口，包括减法运算和自减运算。
 * 支持运算符重载，允许使用 - 和-- 运算符。
 *
 * Defines interfaces related to subtraction operations, including subtraction and decrement.
 * Supports operator overloading, allowing the use of - and -- operators.
 *
 * 数学定义 / Mathematical definition:
 * a - b (减法 / subtraction)
 * --a = a - 1 (自减 / decrement)
 *
 * 接口说明 / Interface descriptions:
 * - Minus: 减法运算符接口，支持 a - b
 * - MinusAssign: 减法赋值接口，支持 a -= b
 * - Dec: 自减运算符接口，支持 --a 戌a--
*/
package fuookami.ospf.kotlin.math.operator

/**
 * 减法运算符接双
 * Subtraction Operator Interface
 *
 * 定义减法运算，支挌- 运算符。
 * 减法是加法的逆运算，将一个数减去另一个数得到差。
 *
 * Defines the subtraction operation, supporting the - operator.
 * Subtraction is the inverse operation of addition, subtracting one number from another to get the difference.
 *
 * @param Rhs 右操作数（减数）的类垌
 * @param Ret 返回值（差）的类垌
 *
*/
interface Minus<in Rhs, out Ret> {

    /**
     * 减法运算符，计算巌
     * Subtraction operator, calculates the difference
     *
     * @param rhs 减数
     * @return 巌
     *
     * @return Difference
    */
    operator fun minus(rhs: Rhs): Ret
}

/**
 * 减法赋值接双
 * Subtraction Assignment Interface
 *
 * 定义减法赋值运算，支持 -= 运算符。
 * 将左操作数减去右操作数，并将结果赋值给左操作数。
 *
 * Defines the subtraction assignment operation, supporting the -= operator.
 * Subtracts the right operand from the left operand and assigns the result to the left operand.
 *
 * @param Rhs 右操作数（减数）的类垌
 *
*/
interface MinusAssign<in Rhs> {

    /**
     * 减法赋值运算符，将差赋值给左操作数
     * Subtraction assignment operator, assigns the difference to the left operand
     *
     * @param rhs 减数
     *
    */
    operator fun minusAssign(rhs: Rhs)
}

/**
 * 自减运算符接双
 * Decrement Operator Interface
 *
 * 定义自减运算，支挌-- 运算符。
 * 自减将当前值减 1，返回自减后的新实例。
 *
 * Defines the decrement operation, supporting the -- operator.
 * Decrement reduces the current value by 1 and returns a new instance after decrement.
 *
 * @param Self 自减运算后返回的类型
 *
*/
interface Dec<Self> {

    /**
     * 自减运算符，将值减 1
     * Decrement operator, reduces the value by 1
     *
     * @return 自减后的新实侌
     *
     * @return New instance after decrement
    */
    operator fun dec(): Dec<Self>
}
