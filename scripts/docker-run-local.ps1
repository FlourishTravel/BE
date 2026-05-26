# Chạy image BE local (cần BE/.env với DB_* DigitalOcean). Port 8080 → /api
# Usage: .\scripts\docker-run-local.ps1

$ErrorActionPreference = "Stop"
$BeRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $BeRoot

$envFile = Join-Path $BeRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Error "Thiếu BE/.env — copy từ .env.example và điền DB_PASSWORD, JWT_SECRET, ..."
}

docker rm -f ftl-be-local 2>$null | Out-Null
docker build -t ftl-be:latest .

Write-Host "Starting http://localhost:8080/api (profile cloud, env from .env) ..." -ForegroundColor Cyan
docker run --name ftl-be-local -p 8080:8080 --env-file $envFile -e SPRING_PROFILES_ACTIVE=cloud ftl-be:latest
