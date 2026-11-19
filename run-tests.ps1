$ROOT = "C:\Users\MSI\Documents\SOA\ParcelFlow"
$PASS = 0
$FAIL = 0

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   ParcelFlow -- Lancement des Tests" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Etape 1 : Installer le parent POM
Write-Host "[1/3] Installation du Parent POM..." -ForegroundColor Yellow
mvn -f "$ROOT\pom.xml" install -N -q
if ($LASTEXITCODE -eq 0) {
    Write-Host "      OK : Parent POM installe" -ForegroundColor Green
} else {
    Write-Host "      ECHEC : Parent POM" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Etape 2 : Tests Order Service
Write-Host "[2/3] Tests Order Service..." -ForegroundColor Yellow
Write-Host "      Fichier : OrderControllerIntegrationTest.java" -ForegroundColor Gray
Write-Host "      4 tests : shouldCreateOrder | shouldRejectInvalidOrder | shouldReturn404ForUnknownId | shouldReturnAllOrders" -ForegroundColor Gray
Write-Host ""

mvn -f "$ROOT\order-service\pom.xml" test "-Dspring.profiles.active=test"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "      PASS [ORDER SERVICE] 4/4 tests reussis !" -ForegroundColor Green
    $PASS++
} else {
    Write-Host ""
    Write-Host "      FAIL [ORDER SERVICE] Des tests ont echoue !" -ForegroundColor Red
    $FAIL++
}

Write-Host ""

# Etape 3 : Resume
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   RESUME FINAL" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   PASS : $PASS service(s)" -ForegroundColor Green

if ($FAIL -gt 0) {
    Write-Host "   FAIL : $FAIL service(s)" -ForegroundColor Red
} else {
    Write-Host "   FAIL : $FAIL service(s)" -ForegroundColor Gray
}

$REPORT = "$ROOT\order-service\target\surefire-reports"
if (Test-Path $REPORT) {
    Write-Host ""
    Write-Host "   Rapports XML dans : $REPORT" -ForegroundColor Cyan
}

Write-Host ""
if ($FAIL -gt 0) { exit 1 } else { exit 0 }
