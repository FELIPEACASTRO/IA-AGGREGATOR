# IA-AGGREGATOR

## Subir tudo com um comando

No PowerShell, execute na raiz do projeto:

`powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1`

Esse comando:

- sobe Postgres e Redis com Docker Compose;
- aguarda health check dos containers;
- executa build completo de backend e frontend;
- inicia backend em `http://localhost:8080` e frontend em `http://localhost:3001`;
- roda um smoke test básico no final.

### Execução mais rápida (sem rebuild)

`powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1 -SkipBuild`

### Parar tudo

`powershell -ExecutionPolicy Bypass -File .\scripts\stop-solution.ps1`

## Pré-requisitos

- Java 21+
- Maven (`mvn` no PATH)
- Node.js + npm
- Docker Desktop
