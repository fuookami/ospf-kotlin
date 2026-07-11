/**
 * 乘法运算笌
 * Multiplication Operator
 *
 * 定义乘法运算相关接口，包括标量乘法和向量叉积。
 * 支持运算符重载，允许使用 * 运算符和 x 函数。
 *
 * Defines interfaces related to multiplication operations, including scalar multiplication and vector cross product.
 * Supports operator overloading, allowing the use of * operator and x function.
 *
 * 数学定义 / Mathematical definitions:
 * - a * b (标量乘法 / scalar multiplication)
 * - a x b (向量叉积 / vector cross product，主要用于三维向里
 *
 * 接口说明 / Interface descriptions:
 * - Times: 乘法运算符接口，支持 a * b
 * - TimesAssign: 乘法赋值接口，支持 a *= b
 * - Cross: 叉积运算符接口，支持 a x b（主要用于向量运算）
*/
package fuookami.ospf.kotlin.math.operator

/**
 * 乘法运算符接双
 * Multiplication Operator Interface
 *
 * 定义乘法运算，支挌* 运算符。
 * 乘法是将两个数相乘得到积的基本运算。
 *
 * Defines the multiplication operation, supporting the * operator.
 * Multiplication is a basic operation that multiplies two numbers to get a product.
 *
 * @param Rhs 右操作数（乘数）的类垌
 * @param Ret 返回值（积）的类垌
 *
*/
interface Times<in Rhs, out Ret> {

    /**
     * 乘法运算符，计算秌
     * Multiplication operator, calculates the product
     *
     * @param rhs 乘数
     * @return 秌
     *
     * @return Product
    */
    operator fun times(rhs: Rhs): Ret
}

/**
 * 乘法赋值接双
 * Multiplication Assignment Interface
 *
 * 定义乘法赋值运算，支持 *= 运算符。
 * 将左操作数乘以右操作数，并将结果赋值给左操作数。
 *
 * Defines the multiplication assignment operation, supporting the *= operator.
 * Multiplies the left operand by the right operand and assigns the result to the left operand.
 *
 * @param Rhs 右操作数（乘数）的类垌
 *
*/
interface TimesAssign<in Rhs> {

    /**
     * 乘法赋值运算符，将积赋值给左操作数
     * Multiplication assignment operator, assigns the product to the left operand
     *
     * @param rhs 乘数
     *
    */
    operator fun timesAssign(rhs: Rhs)
}

/**
 * 叉积运算符接双
 * Cross Product Operator Interface
 *
 * 定义叉积运算，支挌x 中缀函数。
 * 叉积主要用于三维向量运算，结果是一个垂直于两个输入向量的新向量。
 *
 * Defines the cross product operation, supporting the x infix function.
 * Cross product is mainly used for 3D vector operations, resulting in a new vector perpendicular to both input vectors.
 *
 * @param Rhs 右操作数的类垌
 * @param Ret 返回值（叉积结果）的类型
 *
*/
interface Cross<in Rhs, out Ret> {

    /**
     * 叉积运算符，计算叉积
     * Cross product operator, calculates the cross product
     *
     * @param rhs 右向里
     * @return 叉积结果向量
     *
     * @return Cross product result vector
    */
    infix fun x(rhs: Rhs): Ret
}
