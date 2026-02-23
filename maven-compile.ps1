# Maven Compile Script for EIMS Project
Write-Host "Compiling EIMS Project using Maven..." -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Cyan

# Check if Maven is available
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnCmd) {
    Write-Host "Maven not found. Attempting to use mvnw wrapper..." -ForegroundColor Yellow
    if (Test-Path ".\mvnw.cmd") {
        $mvnCmd = ".\mvnw.cmd"
    } else {
        Write-Host "ERROR: Maven is not installed and wrapper not found." -ForegroundColor Red
        Write-Host "Please install Maven from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
        Write-Host "Or use the original compile.ps1 script as fallback." -ForegroundColor Yellow
        exit 1
    }
} else {
    $mvnCmd = "mvn"
}

# Clean and compile
Write-Host "Running Maven clean compile..." -ForegroundColor Yellow
& $mvnCmd clean compile

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nCompilation successful!" -ForegroundColor Green
    Write-Host "`nProject ready to run! Use .\maven-run.ps1 to start the application." -ForegroundColor Green
    Write-Host "`nTest Credentials:" -ForegroundColor Cyan
    Write-Host "Teacher: teacher@eims.com / teacher123" -ForegroundColor Yellow
    Write-Host "Student: student@eims.com / student123" -ForegroundColor Yellow
} else {
    Write-Host "`nCompilation failed. Please check the errors above." -ForegroundColor Red
    Write-Host "You can still use the original compile.ps1 script as fallback." -ForegroundColor Yellow
    exit 1
}
