$ErrorActionPreference = 'Stop'

$rootPath = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $rootPath

$pidsPath = Join-Path $rootPath ".run/solution-pids.json"

if (Test-Path $pidsPath) {
    $pids = Get-Content $pidsPath | ConvertFrom-Json

    foreach ($name in @('backendPid', 'frontendPid')) {
        $pidValue = $pids.$name
        if ($pidValue) {
            try {
                Stop-Process -Id ([int]$pidValue) -Force -ErrorAction Stop
                Write-Host "Parado $name (PID $pidValue)"
            }
            catch {
                Write-Host "PID $pidValue já não estava em execução"
            }
        }
    }

    Remove-Item $pidsPath -Force
}
else {
    Write-Host "Arquivo de PIDs não encontrado em $pidsPath"
}

Write-Host "Derrubando infraestrutura docker compose..."
docker compose down | Out-Host

Write-Host "Concluído."
