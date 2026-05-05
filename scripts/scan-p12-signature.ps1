# P12 Public Signature Flt64 Scanner
# Usage: pwsh -File scripts/scan-p12-signature.ps1
# Output: JSON + text summary

$results = @{
    math_symbol_public = 0
    core_public = 0
    math_symbol_details = @()
    core_details = @()
}

# A: math.symbol public signatures
$mathFiles = Get-ChildItem -Recurse ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol -Filter *.kt
$mathMatches = $mathFiles | Select-String -Pattern '^(\s*)(fun|class|interface|data class|typealias|operator fun).*Flt64'
$results.math_symbol_public = $mathMatches.Count
$results.math_symbol_details = $mathMatches | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# B: core public signatures
$coreFiles = Get-ChildItem -Recurse ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core -Filter *.kt
$coreMatches = $coreFiles | Select-String -Pattern '^(\s*)(fun|class|interface|data class|typealias|operator fun|override fun).*Flt64'
$results.core_public = $coreMatches.Count
$results.core_details = $coreMatches | ForEach-Object { "$($_.RelativePath):$($_.LineNumber): $($_.Line.Trim())" }

# Text output
Write-Host "=== P12 Signature Scan ==="
Write-Host "math.symbol public Flt64: $($results.math_symbol_public)"
Write-Host "core public Flt64: $($results.core_public)"
Write-Host ""

if ($results.math_symbol_public -gt 0) {
    Write-Host "--- math.symbol details ---"
    $results.math_symbol_details | ForEach-Object { Write-Host $_ }
    Write-Host ""
}

if ($results.core_public -gt 0) {
    Write-Host "--- core details ---"
    $results.core_details | ForEach-Object { Write-Host $_ }
}

# JSON output
$json = @{
    math_symbol_public = $results.math_symbol_public
    core_public = $results.core_public
    timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss"
} | ConvertTo-Json

$json | Out-File -FilePath scripts/scan-p12-signature-result.json -Encoding utf8
Write-Host ""
Write-Host "JSON saved to scripts/scan-p12-signature-result.json"

# Exit code
if ($results.math_symbol_public -eq 0 -and $results.core_public -eq 0) {
    Write-Host "GATE: PASS"
    exit 0
} else {
    Write-Host "GATE: FAIL"
    exit 1
}
