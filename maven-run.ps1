# Maven Run Script for EIMS Project
Write-Host "Starting EIMS Application using Maven..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Default Login Credentials:" -ForegroundColor Yellow
Write-Host "Teacher - Email: teacher@eims.com | Password: teacher123" -ForegroundColor Yellow
Write-Host "Student - Email: student@eims.com | Password: student123" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Maven is available
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnCmd) {
    Write-Host "Maven not found. Attempting to use mvnw wrapper..." -ForegroundColor Yellow
    if (Test-Path ".\mvnw.cmd") {
        $mvnCmd = ".\mvnw.cmd"
    } else {
        Write-Host "ERROR: Maven is not installed and wrapper not found." -ForegroundColor Red
        Write-Host "Please install Maven from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
        Write-Host "Or use the original run.ps1 script as fallback." -ForegroundColor Yellow
        exit 1
    }
} else {
    $mvnCmd = "mvn"
}

# Run the application using JavaFX plugin
& $mvnCmd javafx:run
