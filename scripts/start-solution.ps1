param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$rootPath = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $rootPath

function Test-DockerReady {
    try {
        docker version *> $null
        return ($LASTEXITCODE -eq 0)
    }
    catch {
        return $false
    }
}

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutMs = 1500
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $async.AsyncWaitHandle.WaitOne($TimeoutMs)
        if (-not $connected) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

if (-not (Test-DockerReady)) {
    $dockerDesktopPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (Test-Path $dockerDesktopPath) {
        Write-Host "[0/5] Docker engine indisponível. Iniciando Docker Desktop..."
        Start-Process -FilePath $dockerDesktopPath | Out-Null
    }

    $dockerReady = $false
    for ($i = 0; $i -lt 90; $i++) {
        if (Test-DockerReady) {
            $dockerReady = $true
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $dockerReady) {
        throw "Docker engine não ficou disponível. Inicie o Docker Desktop e tente novamente."
    }
}

Write-Host "[1/5] Subindo infraestrutura (Postgres + Redis)..."
docker compose up -d | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao subir docker compose. Verifique se o Docker Desktop permanece ativo."
}

Write-Host "[2/5] Aguardando containers ficarem healthy..."
$maxAttempts = 60
$healthy = $false
for ($i = 0; $i -lt $maxAttempts; $i++) {
    $status = docker compose ps
    if ($status -match 'ia-aggregator-db\s+.*\(healthy\)' -and $status -match 'ia-aggregator-redis\s+.*\(healthy\)') {
        $healthy = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    throw "Infraestrutura não ficou healthy no tempo esperado."
}

if (-not $SkipBuild) {
    Write-Host "[3/5] Build backend (mvn clean verify)..."
    mvn -f backend/pom.xml clean verify | Out-Host

    Write-Host "[4/5] Build frontend (npm install + npm run build)..."
    npm --prefix frontend install | Out-Host
    npm --prefix frontend run build | Out-Host
}

Write-Host "[5/5] Iniciando backend e frontend..."
$ports = @(8080, 3001)
foreach ($port in $ports) {
    $lines = netstat -ano | findstr ":$port"
    if ($lines) {
        $procIds = $lines | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique
        foreach ($procId in $procIds) {
            if ($procId -and $procId -ne '0') {
                try {
                    Stop-Process -Id ([int]$procId) -Force -ErrorAction Stop
                }
                catch {
                }
            }
        }
    }
}

$backendJarPath = Join-Path $rootPath "backend/ia-aggregator-presentation/target/ia-aggregator-presentation-1.0.0-SNAPSHOT.jar"
$backendCommand = "Set-Location '$rootPath'; `$env:APP_CORS_ALLOWED_ORIGINS='http://localhost:3000,http://localhost:3001'; java -jar '$backendJarPath'"
$frontendCommand = "Set-Location '$rootPath'; npm --prefix frontend run start -- --port 3001"

$runDir = Join-Path $rootPath ".run"
New-Item -Path $runDir -ItemType Directory -Force | Out-Null

$backendStdoutLogPath = Join-Path $runDir "backend.out.log"
$backendStderrLogPath = Join-Path $runDir "backend.err.log"
$frontendStdoutLogPath = Join-Path $runDir "frontend.out.log"
$frontendStderrLogPath = Join-Path $runDir "frontend.err.log"

foreach ($logFile in @($backendStdoutLogPath, $backendStderrLogPath, $frontendStdoutLogPath, $frontendStderrLogPath)) {
    if (Test-Path $logFile) {
        Remove-Item $logFile -Force -ErrorAction SilentlyContinue
    }
}

$backendProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoProfile", "-Command", $backendCommand -RedirectStandardOutput $backendStdoutLogPath -RedirectStandardError $backendStderrLogPath -PassThru
$frontendProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoProfile", "-Command", $frontendCommand -RedirectStandardOutput $frontendStdoutLogPath -RedirectStandardError $frontendStderrLogPath -PassThru

$pidsPath = Join-Path $runDir "solution-pids.json"
@{
    backendPid = $backendProcess.Id
    frontendPid = $frontendProcess.Id
    startedAt = (Get-Date).ToString("o")
} | ConvertTo-Json | Set-Content -Path $pidsPath -Encoding UTF8

Start-Sleep -Seconds 4

$backendHealthy = $false
for ($i = 0; $i -lt 20; $i++) {
    $backendAlive = Get-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue
    if (-not $backendAlive) {
        break
    }

    try {
        $backendStatus = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 3).StatusCode
        if ($backendStatus -eq 200) {
            $backendHealthy = $true
            break
        }
    }
    catch {
    }

    Start-Sleep -Seconds 2
}

if (-not $backendHealthy) {
    $dbReachable = Test-TcpPort -HostName 'localhost' -Port 5432
    $backendStillAlive = Get-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue
    Write-Host "`nBackend não ficou saudável em tempo hábil."
    Write-Host "- Processo backend vivo: $([bool]$backendStillAlive)"
    Write-Host "- Postgres localhost:5432 acessível: $dbReachable"
    Write-Host "- Backend stdout log: $backendStdoutLogPath"
    Write-Host "- Backend stderr log: $backendStderrLogPath"
    Write-Host "- Frontend stdout log: $frontendStdoutLogPath"
    Write-Host "- Frontend stderr log: $frontendStderrLogPath"
    if (Test-Path $backendStdoutLogPath) {
        Write-Host "- Últimas linhas do backend stdout log:"
        Get-Content -Path $backendStdoutLogPath -Tail 40 | Out-Host
    }
    if (Test-Path $backendStderrLogPath) {
        Write-Host "- Últimas linhas do backend stderr log:"
        Get-Content -Path $backendStderrLogPath -Tail 40 | Out-Host
    }
}

$urls = @(
    'http://localhost:8080/actuator/health',
    'http://localhost:3001/',
    'http://localhost:3001/chat',
    'http://localhost:3001/settings'
)

Write-Host "\nSmoke check:" 
foreach ($url in $urls) {
    try {
        $statusCode = (Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 20).StatusCode
    }
    catch {
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
        }
        else {
            $statusCode = 'ERR'
        }
    }
    Write-Host "$url -> $statusCode"
}

Write-Host "\nPronto. Serviços em execução:"
Write-Host "- Backend:  http://localhost:8080"
Write-Host "- Frontend: http://localhost:3001"
Write-Host "PIDs salvos em: $pidsPath"
