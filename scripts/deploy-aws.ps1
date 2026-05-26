# Build Docker image và push lên Amazon ECR (FlourishTravel BE)
# Yêu cầu: Docker Desktop, AWS CLI đã `aws configure`, IAM có quyền ecr:* (push) trên repo ftl-be
# Usage (từ thư mục BE):
#   .\scripts\deploy-aws.ps1
#   .\scripts\deploy-aws.ps1 -Region ap-southeast-1 -Repository ftl-be

param(
    [string]$Region = "us-east-1",
    [string]$Repository = "ftl-be",
    [string]$AccountId = "083011581293",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$BeRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $BeRoot

$EcrUri = "${AccountId}.dkr.ecr.${Region}.amazonaws.com"
$ImageRemote = "${EcrUri}/${Repository}:latest"

Write-Host "==> Account: $AccountId  Region: $Region  Repo: $Repository" -ForegroundColor Cyan

if (-not $SkipBuild) {
    Write-Host "==> docker build ..." -ForegroundColor Cyan
    docker build -t "${Repository}:latest" .
}

Write-Host "==> ECR login ($EcrUri) ..." -ForegroundColor Cyan
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $EcrUri

$repoExists = $true
try {
    aws ecr describe-repositories --repository-names $Repository --region $Region 2>$null | Out-Null
} catch {
    $repoExists = $false
}
if (-not $repoExists) {
    Write-Host "==> Creating ECR repository $Repository ..." -ForegroundColor Cyan
    aws ecr create-repository --repository-name $Repository --region $Region | Out-Null
}

Write-Host "==> docker tag + push $ImageRemote ..." -ForegroundColor Cyan
docker tag "${Repository}:latest" $ImageRemote
docker push $ImageRemote

Write-Host ""
Write-Host "Done. Image URI:" -ForegroundColor Green
Write-Host "  $ImageRemote"
Write-Host ""
Write-Host "ECS Express Mode - set env on task (no .env file in container):" -ForegroundColor Yellow
Write-Host "  SPRING_PROFILES_ACTIVE=cloud"
Write-Host "  DB_HOST=db-postgresql-sgp1-flourishtourism-do-user-37760190-0.m.db.ondigitalocean.com"
Write-Host "  DB_PORT=25060"
Write-Host "  DB_NAME=defaultdb"
Write-Host "  DB_USER=doadmin"
Write-Host "  DB_PASSWORD=(DigitalOcean database password)"
Write-Host "  DB_SSL_MODE=require"
Write-Host "  JWT_SECRET=(min 32 chars)"
Write-Host "  FRONTEND_URL=(your FE URL)"
Write-Host "  API_BASE_URL=(ALB URL)/api"
Write-Host "Container port 8080. Health: /api/actuator/health or /api/swagger-ui.html"
Write-Host "See BE/.github/DEPLOY.md section 3.3"
