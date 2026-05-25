param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$projectPath = Resolve-Path -Path $ProjectRoot
$moduleRoot = Join-Path $projectPath "ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/math/geometry"
$legacyRoot = Join-Path $projectPath "ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/geometry"
$infrastructureRoot = Join-Path $projectPath "ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure"
$infrastructureTestRoot = Join-Path $projectPath "ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/kotlin/fuookami/ospf/kotlin/framework/bpp3d/infrastructure"

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg not found. Please install ripgrep."
}

if (-not (Test-Path $moduleRoot)) {
    throw "Math geometry (quantity) module root not found: $moduleRoot"
}

$candidateFiles = @(
    "Axis2.kt",
    "Axis3.kt",
    "AxisPlane3.kt",
    "AxisPermutation2.kt",
    "AxisPermutation3.kt",
    "Projection2.kt",
    "Box2.kt",
    "Placement2.kt",
    "Shape3.kt",
    "Cuboid3.kt",
    "Box3.kt",
    "Cylinder3.kt",
    "Placement3.kt",
    "Cuboid3View.kt",
    "PlaneFrame3.kt",
    "QuantityOps.kt"
)

foreach ($file in $candidateFiles) {
    $filePath = Join-Path $moduleRoot $file
    if (-not (Test-Path $filePath)) {
        throw "Candidate file not found in quantity-geometry module: $filePath"
    }
}

$violations = @()
$warnings = @()
$expectedInternalWarningCount = 8

$importLines = rg -n --no-heading --color never "^import " $moduleRoot -g "*.kt" -S
foreach ($line in $importLines) {
    if ($line -match "import\s+.*(framework\.bpp3d|framework\.bpp2d|framework\.csp2d)") {
        $violations += "FORBIDDEN_MODULE_IMPORT:$line"
    }
    if ($line -match "import\s+.*framework\.") {
        $violations += "FORBIDDEN_FRAMEWORK_IMPORT:$line"
    }
}

$packageLines = rg -n --no-heading --color never "^package " $moduleRoot -g "*.kt" -S
foreach ($line in $packageLines) {
    if ($line -notmatch "math\.geometry") {
        $violations += "PACKAGE_MISMATCH:$line"
    }
}

$internalLines = rg -n --no-heading --color never "^internal\s+" $moduleRoot -g "*.kt" -S
foreach ($line in $internalLines) {
    $warnings += "INTERNAL_VISIBILITY_RISK:$line"
}

if ($internalLines.Count -ne $expectedInternalWarningCount) {
    $violations += "INTERNAL_VISIBILITY_BASELINE_MISMATCH:expected=$expectedInternalWarningCount,actual=$($internalLines.Count)"
}

$legacyFiles = @()
if (Test-Path $legacyRoot) {
    $legacyFiles = Get-ChildItem -Path $legacyRoot -Filter "*.kt" | Select-Object -ExpandProperty Name
}
foreach ($legacyFile in $legacyFiles) {
    $violations += "LEGACY_LAYER_VIOLATION:$legacyFile"
}

$legacyImportPattern = "^import\s+fuookami\.ospf\.kotlin\.framework\.bpp3d\.infrastructure\.geometry\."
$legacyImportLines = @()
if (Test-Path $infrastructureRoot) {
    $legacyImportLines += rg -n --no-heading --color never $legacyImportPattern $infrastructureRoot -g "*.kt" -S
}
if (Test-Path $infrastructureTestRoot) {
    $legacyImportLines += rg -n --no-heading --color never $legacyImportPattern $infrastructureTestRoot -g "*.kt" -S
}
foreach ($line in $legacyImportLines) {
    if (-not [string]::IsNullOrWhiteSpace($line)) {
        $violations += "LEGACY_IMPORT_IN_BPP3D:$line"
    }
}

$report = @()
$report += "MODULE_ROOT=$moduleRoot"
$report += "CANDIDATE_COUNT=$($candidateFiles.Count)"
$report += "VIOLATION_COUNT=$($violations.Count)"
$report += "WARNING_COUNT=$($warnings.Count)"
$report += "INTERNAL_WARNING_BASELINE=$expectedInternalWarningCount"
$report += "INTERNAL_WARNING_ACTUAL=$($internalLines.Count)"
$report += "CANDIDATES:"
$report += $candidateFiles
if ($violations.Count -gt 0) {
    $report += "VIOLATIONS:"
    $report += $violations
}
if ($warnings.Count -gt 0) {
    $report += "WARNINGS:"
    $report += $warnings
}

$tmpRoot = Join-Path $env:TEMP ("geometry-module-dry-run-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmpRoot | Out-Null
$reportPath = Join-Path $tmpRoot "dry-run-report.txt"
$report | Set-Content -Path $reportPath -Encoding UTF8
Write-Host "DRY_RUN_REPORT=$reportPath"

if ($violations.Count -gt 0) {
    Write-Host "GEOMETRY_MODULE_DRY_RUN_FAIL"
    exit 1
}

Write-Host "GEOMETRY_MODULE_DRY_RUN_PASS"
if ($warnings.Count -gt 0) {
    Write-Host "GEOMETRY_MODULE_DRY_RUN_WARNINGS=$($warnings.Count)"
}
if ($internalLines.Count -eq $expectedInternalWarningCount) {
    Write-Host "GEOMETRY_MODULE_DRY_RUN_INTERNAL_BASELINE_OK=$expectedInternalWarningCount"
}

Remove-Item -LiteralPath $tmpRoot -Recurse -Force

exit 0
