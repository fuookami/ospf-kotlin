# OSPF Kotlin Multiarray

:us: [English](README.md) | :cn: 简体中文

高性能 Kotlin 多维数组库，支持多种存储顺序、视图、基于分块的稀疏数组和表格数据结构。

### 特性

- **多维数组**: 类型安全的 1 维到 N 维数组，支持编译期形状检查
- **存储顺序**: 行主序（C 风格）和列主序（Fortran 风格）
- **形状系统**: 编译期形状（Shape1-4）和动态形状（DynShape）
- **视图**: 切片、索引和维度映射视图，无需数据复制
- **分块数组**: 基于分块存储的稀疏数组
- **DataFrame**: 带命名列的二维表格数据结构
- **扩展操作符**: 为 List 和 Map 类型提供多维数组访问操作符
- **迭代器协议**: 支持访问顺序感知的迭代，具有正确的快照语义

### 安装

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-multiarray</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 快速开始

```kotlin
import fuookami.ospf.kotlin.multiarray.*

// 创建 2x3 数组，填充零
val array = MutableMultiArray.newWith(Shape2(2, 3), 0)

// 设置和获取值
array[0, 1] = 10
array[1, 2] = 20
println(array[0, 1])  // 10

// 使用生成器创建
val matrix = MultiArray.newBy(Shape3(2, 3, 4)) { i, vec -> i * 10 }

// 遍历 (线性索引, 向量索引, 值)
for ((linearIdx, vectorIdx, value) in matrix.enumerate()) {
    println("[$linearIdx] ${vectorIdx.toList()} = $value")
}

// 使用虚拟索引切片
val view = array[_a, 1]  // 所有行，第 1 列

// 转换存储顺序
val columnMajor = matrix.toStorageOrder(StorageOrder.ColumnMajor)
```

### 核心组件

#### 形状系统

| 类型 | 描述 | 用途 |
|------|------|------|
| `Shape1` | 一维形状 | 向量 |
| `Shape2` | 二维形状 | 矩阵 |
| `Shape3` | 三维形状 | 张量 |
| `Shape4` | 四维形状 | 批量张量 |
| `DynShape` | 动态维度形状 | 运行时确定的形状 |

```kotlin
// 创建指定存储顺序的形状
val rowMajor = Shape3(2, 3, 4)
val columnMajor = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)

// 形状操作
val shape = Shape2(3, 4)
println(shape.dimension)   // 2
println(shape.size)         // 12
val vec = shape.vector(5)   // [1, 1] (线性索引转向量)
val idx = shape.index(intArrayOf(1, 1))  // 5 (向量转线性索引)
```

#### MultiArray 和 MutableMultiArray

`MultiArray` 是不可变的；`MutableMultiArray` 允许元素修改。

```kotlin
// 工厂方法
val arr1 = MultiArray.new(Shape2(3, 3))                    // 默认值
val arr2 = MultiArray.newWith(Shape2(3, 3), 42)            // 指定填充值
val arr3 = MultiArray.newBy(Shape2(3, 3)) { i, v -> i }    // 生成器
val arr4 = MultiArray.fromList(Shape2(2, 2), listOf(1, 2, 3, 4))  // 从列表创建

// 可变数组
val mutable = MutableMultiArray.newWith(Shape2(2, 2), 0)
mutable[0, 0] = 1
mutable[1, 1] = 2
val immutable = mutable.toImmutable()

// 便捷函数
val v = multiArrayOf(5, 0)           // 1 维数组，5 个零
val m = multiArrayOf(3, 3, 0)        // 3x3 矩阵，填充零
```

#### 视图与切片

视图提供零拷贝的子数组访问。

```kotlin
val array = MultiArray.newBy(Shape3(2, 3, 4)) { i, _ -> i }

// 使用虚拟索引创建切片视图
val slice = array[_a, 1, _a]      // 所有行，第 1 列，所有深度
val range = array[0..1, _a, 0]    // 第 0-1 行，所有列，深度 0

// 映射视图（转置）
val transposed = MappedMultiArrayView(array, listOf(
    MapIndex.Map(2),  // 维度 2 -> 位置 0
    MapIndex.Map(0),  // 维度 0 -> 位置 1
    MapIndex.Map(1)   // 维度 1 -> 位置 2
))
```

#### DummyIndex 和 MapIndex

`DummyIndex` 切片类型：

| 类型 | 示例 | 描述 |
|------|------|------|
| `DummyIndex.All` | `_a` | 该维度的所有元素 |
| `DummyIndex.Index(n)` | `1` | 单个索引 |
| `DummyIndex.Range(r)` | `0..2` | 连续范围 |
| `DummyIndex.IndexArray(list)` | `[0, 2, 4]` | 离散索引集合 |

`MapIndex` 维度重映射类型：

| 类型 | 示例 | 描述 |
|------|------|------|
| `MapIndex.Map(i)` | `MapIndex.Map(0)` | 将维度映射到目标位置 |
| `MapIndex.Dummy(d)` | `MapIndex.Dummy(_a)` | 保持为虚拟索引 |

#### BlockMultiArray（稀疏数组）

仅存储非默认值的稀疏数组。

```kotlin
// 创建稀疏数组
val sparse = BlockMultiArray.empty<Int, Shape3>(Shape3(100, 100, 100))

// 只设置非零值
sparse[intArrayOf(0, 0, 0)] = 1
sparse[intArrayOf(50, 50, 50)] = 2

// 转换为稠密数组
val dense = sparse.toMultiArray(defaultValue = 0)

// 从已有数组创建（带过滤器）
val sparseFromDense = BlockMultiArray.fromMultiArray(dense) { it != 0 }
```

#### DataFrame

带命名列的二维表格数据结构，类似于 pandas DataFrame。

```kotlin
// 从列数据创建
val df = dataFrameOf(
    "name" to listOf("Alice", "Bob", "Charlie"),
    "age" to listOf(25, 30, 35),
    "city" to listOf("NYC", "LA", "Chicago")
)

// 访问数据
println(df.getByName(0, "name"))  // Alice
println(df.getColumnByName("age"))  // [25, 30, 35]

// 过滤和选择
val filtered = df.filter { row -> (row[1] as Int) > 25 }
val selected = df.select("name", "city")

// 构建器模式
val df2 = DataFrame.build<String>("x", "y") {
    row("a", "b")
    row("c", "d")
}

// 转换为 MultiArray
val multiArray = df.toNullableMultiArray()
```

#### List 扩展

为嵌套 List 类型提供多维数组风格的访问操作符。

```kotlin
val matrix: List2<Int> = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6)
)

// 获取所有行的指定列
val col = matrix[_a, 1]  // [2, 5]

// 获取指定行的所有列
val row = matrix[0, _a]  // [1, 2, 3]

// 3D 列表访问
val cube: List3<Int> = listOf(
    listOf(listOf(1, 2), listOf(3, 4)),
    listOf(listOf(5, 6), listOf(7, 8))
)
val v = cube[0, 1, 0]  // 3
val allFirst = cube[_a, 0, 0]  // [1, 5]
```

#### Map 扩展

为包含 MultiArray 值的 Map 类型提供多维数组访问操作符。

```kotlin
val map = mapOf(
    "a" to MultiArray.newWith(Shape2(2, 2), 1),
    "b" to MultiArray.newWith(Shape2(2, 2), 2)
)

// 通过键和索引访问
val v = map["a", 0, 1]       // 从 MultiArray 值获取元素
val all = map[_a]             // 获取所有值 (Iterable)
map["b", 0, 1] = 42          // 设置 MutableMultiArray 值的元素

// 同样适用于 MultiMap2、MultiMap3、MultiMap4
```

#### 访问顺序与迭代

```kotlin
// 行主序迭代（默认，C 风格）
for (idx in shape.indices(AccessOrder.RowMajor)) {
    println(idx.toList())
}

// 列主序迭代（Fortran 风格）
for (idx in shape.indices(AccessOrder.ColumnMajor)) {
    println(idx.toList())
}

// 按指定顺序展平
val rowFlat = array.flatten(AccessOrder.RowMajor)
val colFlat = array.flatten(AccessOrder.ColumnMajor)

// 按指定顺序枚举
for ((i, vec, value) in array.enumerateWithOrder(AccessOrder.ColumnMajor)) {
    // ...
}
```

### API 参考

#### 工厂方法

- `MultiArray.new(shape)` - 使用默认值创建
- `MultiArray.newWith(shape, value)` - 使用指定填充值创建
- `MultiArray.newBy(shape, generator)` - 使用生成器函数创建 `(线性索引, 向量索引) -> T`
- `MultiArray.fromList(shape, list, accessOrder)` - 从扁平列表创建
- `MutableMultiArray.new(...)` / `newWith(...)` / `newBy(...)` / `fromList(...)` - 可变版本

#### 访问方法

- `array[i]` - 线性索引访问
- `array[i, j, k]` - 向量索引访问
- `array[vec]` - IntArray 向量访问
- `array[_a, 1]` - 切片/视图访问（返回 `MultiArrayView`）

#### 迭代

- `array.iterate(order)` - 按指定访问顺序迭代
- `array.enumerate()` - 带 (线性索引, 向量索引, 值) 三元组的迭代
- `array.enumerateWithOrder(order)` - 按指定顺序枚举
- `array.flatten(order)` - 转换为扁平列表

### 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=ShapeColumnMajorInverseTest
```

### 许可证

MIT License
