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

function Assert-NotContains {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Text,

        [Parameter(Mandatory = $true)]
        [string] $Unexpected,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if ($Text.Contains($Unexpected)) {
        throw "$Message`nUnexpected: $Unexpected"
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
[74.378s][warning][codecache] CodeHeap 'non-profiled nmethods' is full. Compiler has been disabled.
Java HotSpot(TM) 64-Bit Server VM warning: CodeHeap 'non-profiled nmethods' is full. Compiler has been disabled.
"@
    Set-Content -LiteralPath $logPath -Value $logContent -Encoding UTF8

    & $summaryScript -LogPath $logPath -Output $outputPath -Format markdown | Out-Null
    Assert-True -Condition (Test-Path -LiteralPath $outputPath) -Message "Summary script did not generate markdown report."

    $report = Get-Content -LiteralPath $outputPath -Raw
    Assert-Contains -Text $report -Expected "Total warnings: 7" -Message "Unexpected warning total in summary report."
    Assert-Contains -Text $report -Expected "Unique groups: 4" -Message "Unexpected unique-group total in summary report."
    Assert-Contains -Text $report -Expected "Displayed groups: 4" -Message "Displayed-group total should default to all groups."
    Assert-Contains -Text $report -Expected "| ospf-kotlin-example | /a/b/FileA.kt | 2 |" -Message "File-level aggregation should merge duplicate warning lines."
    Assert-Contains -Text $report -Expected "| ospf-kotlin-core | unchecked cast: Any to String | 2 |" -Message "Message-level aggregation should merge duplicate warning lines."
    Assert-Contains -Text $report -Expected "| ospf-kotlin-core | CodeHeap 'non-profiled nmethods' is full. Compiler has been disabled. | 2 |" -Message "CodeHeap warnings should be summarized and grouped."

    $idxCodeHeap = $report.IndexOf("| ospf-kotlin-core | CodeHeap 'non-profiled nmethods' is full. Compiler has been disabled. | 2 |")
    $idxUnchecked = $report.IndexOf("| ospf-kotlin-core | unchecked cast: Any to String | 2 |")
    $idxFileA = $report.IndexOf("| ospf-kotlin-example | /a/b/FileA.kt | 2 |")
    Assert-True -Condition ($idxFileA -ge 0 -and $idxCodeHeap -ge 0 -and $idxUnchecked -ge 0) -Message "Expected rows for sort-order assertions are missing."
    Assert-True -Condition ($idxCodeHeap -lt $idxUnchecked -and $idxUnchecked -lt $idxFileA) -Message "Summary rows should be stably sorted by Count desc, Module asc, Key asc."

    $topOutputPath = Join-Path $tempRoot "warning-summary-top2.md"
    & $summaryScript -LogPath $logPath -Output $topOutputPath -Format markdown -Top 2 | Out-Null
    Assert-True -Condition (Test-Path -LiteralPath $topOutputPath) -Message "Top-N summary report was not generated."
    $topReport = Get-Content -LiteralPath $topOutputPath -Raw
    Assert-Contains -Text $topReport -Expected "Displayed groups: 2" -Message "Top-N summary should report displayed group count."
    Assert-Contains -Text $topReport -Expected "| ospf-kotlin-core | CodeHeap 'non-profiled nmethods' is full. Compiler has been disabled. | 2 |" -Message "Top-N summary should keep the first sorted row."
    Assert-Contains -Text $topReport -Expected "| ospf-kotlin-core | unchecked cast: Any to String | 2 |" -Message "Top-N summary should keep the second sorted row."
    Assert-NotContains -Text $topReport -Unexpected "| ospf-kotlin-example | /a/b/FileA.kt | 2 |" -Message "Top-N summary should truncate rows after the requested limit."

    Write-Output "summarize-maven-warnings smoke passed."
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
