param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$rootPath = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $rootPath

function Get-AiKeysFromLocalFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    if (-not (Test-Path $FilePath)) {
        return @{}
    }

    $rawLines = Get-Content -Path $FilePath
    $lines = $rawLines | ForEach-Object { $_.Trim() }
    $result = @{}

    function Get-NextKeyLine {
        param(
            [string[]]$AllLines,
            [int]$StartIndex,
            [string]$Prefix
        )

        for ($j = $StartIndex + 1; $j -lt $AllLines.Count; $j++) {
            $candidate = $AllLines[$j]
            if ([string]::IsNullOrWhiteSpace($candidate)) {
                continue
            }
            if ($candidate.StartsWith('curl ', [StringComparison]::OrdinalIgnoreCase)) {
                continue
            }
            if ($candidate.StartsWith($Prefix, [StringComparison]::OrdinalIgnoreCase)) {
                return $candidate
            }
            break
        }
        return $null
    }

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        switch -Regex ($line) {
            '^Open IA$' {
                $value = Get-NextKeyLine -AllLines $lines -StartIndex $i -Prefix 'sk-'
                if ($value) { $result['OPENAI_API_KEY'] = $value }
                continue
            }
            '^Gemini$' {
                $value = Get-NextKeyLine -AllLines $lines -StartIndex $i -Prefix 'AIza'
                if ($value) { $result['GEMINI_API_KEY'] = $value }
                continue
            }
            '^DeepSeek$' {
                $value = Get-NextKeyLine -AllLines $lines -StartIndex $i -Prefix 'sk-'
                if ($value) { $result['DEEPSEEK_API_KEY'] = $value }
                continue
            }
            '^Claude$' {
                $value = Get-NextKeyLine -AllLines $lines -StartIndex $i -Prefix 'sk-ant-'
                if ($value) { $result['ANTHROPIC_API_KEY'] = $value }
                continue
            }
            '^Grok' {
                $value = Get-NextKeyLine -AllLines $lines -StartIndex $i -Prefix 'xai-'
                if ($value) { $result['XAI_API_KEY'] = $value }
                continue
            }
        }
    }

    return $result
}

$localKeysPath = Join-Path $rootPath "IA\local.txt"
$aiKeys = Get-AiKeysFromLocalFile -FilePath $localKeysPath
foreach ($entry in $aiKeys.GetEnumerator()) {
    Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
}
if ($aiKeys.Count -gt 0) {
    Write-Host ("[init] Chaves de IA carregadas de IA\\local.txt: " + (($aiKeys.Keys | Sort-Object) -join ', '))
}

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
    $env:MAVEN_OPTS = "--enable-native-access=ALL-UNNAMED -XX:+EnableDynamicAgentLoading"
    mvn -f backend/pom.xml clean verify | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Falha no build do backend."
    }

    Write-Host "[4/5] Build frontend (npm install + npm run build)..."
    $frontendPath = Join-Path $rootPath "frontend"
    Push-Location $frontendPath
    try {
        npm install | Out-Host
        if ($LASTEXITCODE -ne 0) {
            throw "Falha no npm install do frontend."
        }

        $prismaClientPath = Join-Path $frontendPath "node_modules/.prisma/client"
        if (-not (Test-Path $prismaClientPath)) {
            $env:CODEX_DATABASE_URL = 'postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'
            npm run prisma:generate | Out-Host
            if ($LASTEXITCODE -ne 0) {
                throw "Falha no prisma:generate do frontend."
            }
        }

        npm run build | Out-Host
        if ($LASTEXITCODE -ne 0) {
            throw "Falha no build do frontend."
        }
    }
    finally {
        Pop-Location
    }
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
$backendCommand = "Set-Location '$rootPath'; `$env:APP_CORS_ALLOWED_ORIGINS='http://localhost:3000,http://localhost:3001'; `$env:SERVER_ADDRESS='0.0.0.0'; `$env:JAVA_TOOL_OPTIONS='--enable-native-access=ALL-UNNAMED -XX:+EnableDynamicAgentLoading'; java -jar '$backendJarPath'"
$frontendPath = Join-Path $rootPath "frontend"
$frontendCommand = "Set-Location '$frontendPath'; `$env:CODEX_DATABASE_URL='postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'; `$env:CODEX_REDIS_URL='redis://localhost:6379'; npm run start -- --hostname 0.0.0.0 --port 3001"

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

$frontendHealthy = $false
for ($i = 0; $i -lt 30; $i++) {
    $frontendAlive = Get-Process -Id $frontendProcess.Id -ErrorAction SilentlyContinue
    if (-not $frontendAlive) {
        break
    }

    try {
        $frontendStatus = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:3001/' -TimeoutSec 3).StatusCode
        if ($frontendStatus -eq 200) {
            $frontendHealthy = $true
            break
        }
    }
    catch {
    }

    Start-Sleep -Seconds 2
}

if (-not $frontendHealthy) {
    $frontendStillAlive = Get-Process -Id $frontendProcess.Id -ErrorAction SilentlyContinue
    Write-Host "`nFrontend nÃ£o ficou saudÃ¡vel em tempo hÃ¡bil."
    Write-Host "- Processo frontend vivo: $([bool]$frontendStillAlive)"
    Write-Host "- Frontend stdout log: $frontendStdoutLogPath"
    Write-Host "- Frontend stderr log: $frontendStderrLogPath"
    if (Test-Path $frontendStdoutLogPath) {
        Write-Host "- Ãšltimas linhas do frontend stdout log:"
        Get-Content -Path $frontendStdoutLogPath -Tail 40 | Out-Host
    }
    if (Test-Path $frontendStderrLogPath) {
        Write-Host "- Ãšltimas linhas do frontend stderr log:"
        Get-Content -Path $frontendStderrLogPath -Tail 40 | Out-Host
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
