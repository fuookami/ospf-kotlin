# ospf-kotlin-quantities 后续改进清单

## 目标

根据 `ospf-rust-quantities` 当前比 `ospf-kotlin-quantities` 多出的公开单位，补齐 Kotlin 版本的单位覆盖与兼容命名。

## 原则

1. 优先补齐真实缺失的单位。
2. 实际相同但命名不同的单位，作为兼容别名处理，不新增重复量纲语义。
3. Rust 侧明显疑似拼写错误的名称，不直接照搬到 Kotlin。
4. 新增单位应补齐符号、比例、domain 和转换测试。
5. 信息单位应保持当前 domain 语义：`Bit`、`Byte` 为 `Discrete`，聚合信息单位为 `Continuous`。

## 建议新增实际单位

### 信息量

- `Kibibyte`
- `Mebibyte`
- `Gibibyte`
- `Tebibyte`

说明：二进制前缀单位，比例分别为 `1024 B`、`1024 KiB`、`1024 MiB`、`1024 GiB`，domain 建议为 `QuantityDomain.Continuous`。

### 带宽

- `BytePerSecond`
- `KilobytePerSecond`
- `MegabytePerSecond`

说明：可由对应信息单位除以 `Second` 派生。

### 长度和体积

- `Decameter`
- `CubicDecameter`
- `CubicHectometer`
- `CubicKilometer`

### 流量

- `LiterPerMinute`

### 力

- `KiloNewton`
- `MegaNewton`

### 应力

- `PascalStress`
- `KilopascalStress`
- `MegapascalStress`

说明：若 Kotlin 已将 pressure 和 stress 共享相同量纲，可作为 stress 命名下的单位对象或兼容别名实现。

### 电阻

- `Megaohm`

### 质量

- `Hectogram`

## 建议新增兼容别名

### 平面角

- `ArcMinute` -> `MinuteAngle`
- `ArcSecond` -> `SecondAngle`

### 加速度

- `MeterPerSecondSquared` -> `MeterPerSquareSecond`
- `CentimeterPerSecondSquared` -> `CentimeterPerSquareSecond`
- `KilometerPerSecondSquared` -> `KilometerPerSquareSecond`
- `InchPerSecondSquared` -> `InchPerSquareSecond`
- `FootPerSecondSquared` -> `FootPerSquareSecond`

### 速度

- `KilometerPerSecond` -> `KilometersPerSecond`

### 质量

- `Tonne` -> `Ton`

## 暂不建议直接添加

- `Cetimeter`

原因：该名称疑似 Rust 侧 `Centimeter` 拼写错误。Kotlin 已有 `Centimeter`，不应新增错误拼写别名，除非后续明确需要兼容 Rust 现有 typo。

## 验收标准

1. 上述真实缺失单位可在 Kotlin 中直接引用。
2. 兼容别名与既有 Kotlin 单位换算结果一致。
3. 新增信息聚合单位 domain 为 `QuantityDomain.Continuous`。
4. 新增单位参与 `to(...)`、加减乘除和单位制推导时行为正确。
5. `ospf-kotlin-quantities` 测试通过。
