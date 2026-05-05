# P12 Adapter Boundary Scanner
# Usage: pwsh -File scripts/scan-p12-adapter.ps1
# Scans for Flt64 usage outside allowed adapter boundaries

$results = @{
    math_main_non_adapter_flt64 = 0
    core_main_non_adapter_flt64 = 0
    unchecked_cast_core = 0
    unchecked_cast_math = 0
    details = @()
}

# Scan math.symbol main for Flt64 outside adapter/internal
$mathFiles = Get-ChildItem -Recurse ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol -Filter *.kt
$mathNonAdapter = $mathFiles | Where-Object { $_.FullName -notmatch 'adapter[\\/].*internal' } |
    Select-String -Pattern 'Flt64'
$results.math_main_non_adapter_flt64 = $mathNonAdapter.Count

# Scan core main for Flt64 outside adapter/internal
$coreFiles = Get-ChildItem -Recurse ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core -Filter *.kt
$coreNonAdapter = $coreFiles | Where-Object { $_.FullName -notmatch 'adapter[\\/].*internal' } |
    Select-String -Pattern 'Flt64'
$results.core_main_non_adapter_flt64 = $coreNonAdapter.Count

# Scan uncheckedCast
$m1 = Get-ChildItem -Recurse ospf-kotlin-core/src/main -Filter *.kt | Select-String -Pattern 'uncheckedCast\('
$m2 = Get-ChildItem -Recurse ospf-kotlin-math/src/main -Filter *.kt | Select-String -Pattern 'uncheckedCast\('
$results.unchecked_cast_core = $m1.Count
$results.unchecked_cast_math = $m2.Count

Write-Host "=== P12 Adapter Boundary Scan ==="
Write-Host "math.symbol non-adapter Flt64 refs: $($results.math_main_non_adapter_flt64)"
Write-Host "core non-adapter Flt64 refs: $($results.core_main_non_adapter_flt64)"
Write-Host "core uncheckedCast: $($results.unchecked_cast_core)"
Write-Host "math uncheckedCast: $($results.unchecked_cast_math)"

$json = $results | ConvertTo-Json
$json | Out-File -FilePath scripts/scan-p12-adapter-result.json -Encoding utf8
Write-Host ""
Write-Host "JSON saved to scripts/scan-p12-adapter-result.json"

$totalViolations = $results.unchecked_cast_core + $results.unchecked_cast_math
if ($totalViolations -eq 0) {
    Write-Host "GATE: PASS (uncheckedCast = 0)"
} else {
    Write-Host "GATE: FAIL (uncheckedCast > 0)"
}
