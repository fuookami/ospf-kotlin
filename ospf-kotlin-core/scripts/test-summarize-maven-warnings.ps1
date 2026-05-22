Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool] $Condition,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Text,

        [Parameter(Mandatory = $true)]
        [string] $Expected,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Text.Contains($Expected)) {
        throw "$Message`nExpected: $Expected"
    }
}

$summaryScript = Join-Path $PSScriptRoot "summarize-maven-warnings.ps1"
Assert-True -Condition (Test-Path -LiteralPath $summaryScript) -Message "Missing script: $summaryScript"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("ospf-kotlin-warning-summary-smoke-{0}" -f [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

try {
    $logPath = Join-Path $tempRoot "build.log"
    $outputPath = Join-Path $tempRoot "warning-summary.md"

    $logContent = @"
[INFO] Building ospf-kotlin-example 0.0.1
[WARNING] /a/b/FileA.kt:12:8: warning: deprecated call
[WARNING] /a/b/FileA.kt:20:8: warning: deprecated call
[INFO] --- maven-compiler-plugin:3.13.0:compile (default-compile) @ ospf-kotlin-core ---
[WARNING] E:\workspace\ospf-kotlin\ospf-kotlin-core\src\Foo.kt:10:2: warning: unchecked cast
[WARNING] unchecked cast: Any to String
[WARNING] unchecked cast: Any to String
"@
    Set-Content -LiteralPath $logPath -Value $logContent -Encoding UTF8

    & $summaryScript -LogPath $logPath -Output $outputPath -Format markdown | Out-Null
    Assert-True -Condition (Test-Path -LiteralPath $outputPath) -Message "Summary script did not generate markdown report."

    $report = Get-Content -LiteralPath $outputPath -Raw
    Assert-Contains -Text $report -Expected "Total warnings: 5" -Message "Unexpected warning total in summary report."
    Assert-Contains -Text $report -Expected "Unique groups: 3" -Message "Unexpected unique-group total in summary report."
    Assert-Contains -Text $report -Expected "| ospf-kotlin-example | /a/b/FileA.kt | 2 |" -Message "File-level aggregation should merge duplicate warning lines."
    Assert-Contains -Text $report -Expected "| ospf-kotlin-core | unchecked cast: Any to String | 2 |" -Message "Message-level aggregation should merge duplicate warning lines."

    Write-Output "summarize-maven-warnings smoke passed."
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
