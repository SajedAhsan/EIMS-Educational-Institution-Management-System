# Run EIMS Project
Write-Host "Starting EIMS Application..." -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Default Login Credentials:" -ForegroundColor Yellow
Write-Host "Teacher - Email: teacher@eims.com | Password: teacher123" -ForegroundColor Yellow
Write-Host "Student - Email: student@eims.com | Password: student123" -ForegroundColor Yellow
Write-Host "================================" -ForegroundColor Cyan

java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\h2-2.2.224.jar;resources" Main
