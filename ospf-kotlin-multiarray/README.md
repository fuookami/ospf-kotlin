# OSPF Kotlin Multiarray

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## English

A high-performance multi-dimensional array library for Kotlin, supporting various storage orders, views, and block-based sparse arrays.

### Features

- **Multi-dimensional Arrays**: Support for 1D to N-dimensional arrays with type-safe shapes
- **Storage Orders**: Row-major (C-style) and Column-major (Fortran-style) storage orders
- **Shape System**: Compile-time shapes (Shape1, Shape2, Shape3, Shape4) and dynamic shapes (DynShape)
- **Views**: Slicing, indexing, and mapping views without data copying
- **Block Arrays**: Sparse array support with block-based storage
- **Iterator Protocol**: Access order-aware iteration with proper snapshot semantics

### Installation

Add dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-multiarray</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Quick Start

```kotlin
import fuookami.ospf.kotlin.multiarray.*

// Create a 2x3 array with default values
val array = MultiArray.newWith(Shape2(2, 3), 0)

// Set values
array[0, 1] = 10
array[1, 2] = 20

// Get values
println(array[0, 1])  // Output: 10
println(array[1, 0])  // Output: 0

// Create with generator
val matrix = MultiArray.newBy(Shape3(2, 3, 4)) { i, vec ->
    i * 10  // Linear index times 10
}

// Iterate over elements
for ((linearIdx, vectorIdx, value) in matrix.enumerate()) {
    println("[$linearIdx] ${vectorIdx.toList()} = $value")
}

// Create a view (slice)
val view = array[_a, 1]  // Get column 1 from all rows

// Convert storage order
val columnMajor = matrix.toStorageOrder(StorageOrder.ColumnMajor)
```

### Shape Types

| Type | Description | Use Case |
|------|-------------|----------|
| `Shape1` | 1-dimensional shape | Vectors |
| `Shape2` | 2-dimensional shape | Matrices |
| `Shape3` | 3-dimensional shape | Tensors |
| `Shape4` | 4-dimensional shape | Batch tensors |
| `DynShape` | Dynamic dimension shape | Runtime-determined shapes |

### Storage Orders

```kotlin
// Row-major (default): last dimension varies fastest
val rowMajor = Shape3.withOrder(2, 3, 4, StorageOrder.RowMajor)

// Column-major: first dimension varies fastest
val columnMajor = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)

// Convert between orders
val converted = array.toStorageOrder(StorageOrder.ColumnMajor)
```

### Views and Slicing

```kotlin
// Slice using dummy indices
val slice = array[_a, 1]           // All rows, column 1
val range = array[0..1, _a]        // Rows 0-1, all columns
val specific = array[0, 1..2]      // Row 0, columns 1-2

// Create mapped view (transpose-like operations)
val transposed = MappedMultiArrayView(array, listOf(
    MapIndex.Map(1),  // Map dimension 1 to position 0
    MapIndex.Map(0)   // Map dimension 0 to position 1
))
```

### Block Arrays (Sparse)

```kotlin
// Create sparse array
val sparse = BlockMultiArray.empty<Int, Shape3>(Shape3(100, 100, 100))

// Set only non-zero values
sparse[intArrayOf(0, 0, 0)] = 1
sparse[intArrayOf(50, 50, 50)] = 2

// Convert to dense array
val dense = sparse.toMultiArray(defaultValue = 0)
```

### API Reference

#### Factory Methods

- `MultiArray.new(shape)` - Create with default values
- `MultiArray.newWith(shape, value)` - Create with specific value
- `MultiArray.newBy(shape, generator)` - Create with generator function
- `MultiArray.fromList(shape, list, accessOrder)` - Create from list

#### Access Methods

- `array[i]` - Linear index access
- `array[i, j, k]` - Vector index access
- `array[vec]` - IntArray vector access
- `array[_a, 1]` - Slice/view access

#### Iteration

- `array.iterate(order)` - Iterate with specified access order
- `array.enumerate()` - Iterate with (index, vector, value)
- `array.flatten(order)` - Convert to flat list

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ShapeColumnMajorInverseTest
```

### License

MIT License

---

<a name="中文"></a>
## 中文

高性能 Kotlin 多维数组库，支持多种存储顺序、视图和基于分块的稀疏数组。

### 特性

- **多维数组**: 支持 1 维到 N 维数组的类型安全形状
- **存储顺序**: 行主序（C 风格）和列主序（Fortran 风格）存储顺序
- **形状系统**: 编译期形状（Shape1-4）和动态形状（DynShape）
- **视图**: 切片、索引和映射视图，无需数据复制
- **分块数组**: 基于分块存储的稀疏数组支持
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

// 创建 2x3 数组，默认值为 0
val array = MultiArray.newWith(Shape2(2, 3), 0)

// 设置值
array[0, 1] = 10
array[1, 2] = 20

// 获取值
println(array[0, 1])  // 输出: 10
println(array[1, 0])  // 输出: 0

// 使用生成器创建
val matrix = MultiArray.newBy(Shape3(2, 3, 4)) { i, vec ->
    i * 10  // 线性索引乘以 10
}

// 遍历元素
for ((linearIdx, vectorIdx, value) in matrix.enumerate()) {
    println("[$linearIdx] ${vectorIdx.toList()} = $value")
}

// 创建视图（切片）
val view = array[_a, 1]  // 获取所有行的第 1 列

// 转换存储顺序
val columnMajor = matrix.toStorageOrder(StorageOrder.ColumnMajor)
```

### 形状类型

| 类型 | 描述 | 用途 |
|------|------|------|
| `Shape1` | 一维形状 | 向量 |
| `Shape2` | 二维形状 | 矩阵 |
| `Shape3` | 三维形状 | 张量 |
| `Shape4` | 四维形状 | 批量张量 |
| `DynShape` | 动态维度形状 | 运行时确定的形状 |

### 存储顺序

```kotlin
// 行主序（默认）：最后一维变化最快
val rowMajor = Shape3.withOrder(2, 3, 4, StorageOrder.RowMajor)

// 列主序：第一维变化最快
val columnMajor = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)

// 在顺序间转换
val converted = array.toStorageOrder(StorageOrder.ColumnMajor)
```

### 视图与切片

```kotlin
// 使用虚拟索引切片
val slice = array[_a, 1]           // 所有行，第 1 列
val range = array[0..1, _a]        // 第 0-1 行，所有列
val specific = array[0, 1..2]      // 第 0 行，第 1-2 列

// 创建映射视图（类似转置操作）
val transposed = MappedMultiArrayView(array, listOf(
    MapIndex.Map(1),  // 将维度 1 映射到位置 0
    MapIndex.Map(0)   // 将维度 0 映射到位置 1
))
```

### 分块数组（稀疏）

```kotlin
// 创建稀疏数组
val sparse = BlockMultiArray.empty<Int, Shape3>(Shape3(100, 100, 100))

// 只设置非零值
sparse[intArrayOf(0, 0, 0)] = 1
sparse[intArrayOf(50, 50, 50)] = 2

// 转换为稠密数组
val dense = sparse.toMultiArray(defaultValue = 0)
```

### API 参考

#### 工厂方法

- `MultiArray.new(shape)` - 使用默认值创建
- `MultiArray.newWith(shape, value)` - 使用指定值创建
- `MultiArray.newBy(shape, generator)` - 使用生成器函数创建
- `MultiArray.fromList(shape, list, accessOrder)` - 从列表创建

#### 访问方法

- `array[i]` - 线性索引访问
- `array[i, j, k]` - 向量索引访问
- `array[vec]` - IntArray 向量访问
- `array[_a, 1]` - 切片/视图访问

#### 迭代

- `array.iterate(order)` - 按指定访问顺序迭代
- `array.enumerate()` - 带索引、向量、值的迭代
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