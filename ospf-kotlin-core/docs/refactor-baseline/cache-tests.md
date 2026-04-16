# 缓存一致性测试清单（2026-04-16）

> 测试覆盖清单，用于验证 C3 缓存归属统一和生命周期管理

---

## 1. 测试文件清单

### 1.1 B3 新增测试

| 文件 | 测试方法 | 验收场景 | 状态 |
|------|----------|----------|------|
| `TokenCacheContextsTest.kt` | `concurrentRegisterShouldPreheatValueFlattenAndRangeCache` | 并发注册预热验证 | ⏳ 待验证 |
| `CacheRebindTest.kt` | `removeShouldClearCachesAndAllowRebind` | 移除后重注册验证 | ⏳ 待验证 |
| `CacheRebindTest.kt` | `rebindToNewTokenTableShouldInvalidateOldTableCaches` | 双TokenTable重绑一致性 | ⏳ 待验证 |

---

### 1.2 原有测试

| 文件 | 测试方法 | 验收场景 | 状态 |
|------|----------|----------|------|
| `TokenCacheContextsTest.kt` | `valueCacheContextShouldSeparateSolutionAndFixedCacheKey` | value缓存读写分离 | ✅ 已存在 |
| `TokenCacheContextsTest.kt` | `tokenCacheContextsShouldFlushIndependently` | 独立失效验证 | ✅ 已存在 |
| `TokenCacheContextsTest.kt` | `tokenTableShouldExposeFlattenAndRangeContext` | TokenTable缓存暴露 | ✅ 已存在 |
| `TokenCacheContextsTest.kt` | `registerShouldPopulateFlattenAndRangeContext` | 注册预热验证 | ✅ 已存在 |
| `TokenCacheContextsTest.kt` | `closeShouldUnbindTokenTableContext` | 关闭解绑验证 | ✅ 已存在 |
| `TokenCacheContextsTest.kt` | `contextsShouldSupportNonSymbolCacheKey` | 非Symbol缓存key支持 | ✅ 已存在 |

---

## 2. 测试覆盖矩阵

| 生命周期阶段 | 测试覆盖 | 测试方法 | 状态 |
|--------------|----------|----------|------|
| **注册预热** | 同步注册 | `registerShouldPopulateFlattenAndRangeContext` | ✅ |
| **注册预热** | 并发注册 | `concurrentRegisterShouldPreheatValueFlattenAndRangeCache` | ⏳ B3新增 |
| **注册预热** | 空符号 | `registerShouldPopulateFlattenAndRangeContext` | ✅ |
| **缓存读写** | value分离 | `valueCacheContextShouldSeparateSolutionAndFixedCacheKey` | ✅ |
| **缓存读写** | flatten/range | `tokenTableShouldExposeFlattenAndRangeContext` | ✅ |
| **缓存失效** | flush独立 | `tokenCacheContextsShouldFlushIndependently` | ✅ |
| **符号移除** | 缓存清理 | `removeShouldClearCachesAndAllowRebind` | ⏳ B3新增 |
| **符号重绑** | 旧表失效 | `rebindToNewTokenTableShouldInvalidateOldTableCaches` | ⏳ B3新增 |
| **表关闭** | 解绑清理 | `closeShouldUnbindTokenTableContext` | ✅ |

---

## 3. C3-4 验收结论

### 3.1 主代码编译验证

```bash
mvn -pl ospf-kotlin-core compile -q
```

**结论**: ✅ 主代码编译通过

---

### 3.2 全量测试验证

⚠️ **阻塞说明**: 全量测试仍受 C2 泛型化遗留阻塞

**测试编译失败清单**（C2遗留）:

| 文件 | 错误类型 | 原因 |
|------|----------|------|
| `FlattenUtilityTest.kt` | `LinearFlattenData` vs `LinearFlattenDataOf<Flt64>` 类型不匹配 | 泛型化遗留 |
| `MonomialCoefficientPreservationTest.kt` | `LinearMonomialCell.invoke` 类型参数错误 | 泛型化遗留 |
| `LinearPolynomialBaselineTest.kt` | `evaluate` 重载歧义 | 泛型化遗留 |
| `QuadraticPolynomialBaselineTest.kt` | `evaluate` 重载歧义 | 泛型化遗留 |
| `SubObjectTest.kt` | `LinearFlattenData` 类型不匹配 | 泛型化遗留 |

**验收结论口径**: 
- ✅ 主代码编译验证可完成
- ⏳ 全量测试验收待 C2 测试编译问题清理后补跑

---

## 4. 后续步骤

| 步骤 | 内容 | 状态 |
|------|------|------|
| C2 遗留修复 | 修复测试文件泛型化类型问题 | ⏳ 待执行 |
| C3-4 补验证 | 全量测试通过确认 | ⏳ 待 C2 修复后 |
| C3-5 交付物 | cache-tests.md 已生成 | ✅ 当前文档 |

---

## 5. 测试运行命令

```bash
# 主代码编译验证
mvn -pl ospf-kotlin-core compile -q

# 全量测试运行（待C2修复后）
mvn -pl ospf-kotlin-core test

# 缓存测试单独运行（待C2修复后）
mvn -pl ospf-kotlin-core test -Dtest="TokenCacheContextsTest,CacheRebindTest"
```

---

## 6. 验收标准

| 标准 | 状态 |
|------|------|
| 主代码编译通过 | ✅ |
| 新增测试文件存在 | ✅ |
| 新增测试方法签名正确 | ✅ |
| 全量测试通过 | ⏳ 待 C2 修复后 |

---

## 7. 文档签署

| 角色 | 签署 |
|------|------|
| **C3 执行人** | Claude Code |
| **审核人** | 用户 |
| **日期** | 2026-04-16 |

**验收口径**: 主代码编译验证可完成，全量测试验收待 C2 测试编译问题清理后补跑