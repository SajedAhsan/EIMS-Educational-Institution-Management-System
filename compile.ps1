# Compile EIMS Project with H2 Database
Write-Host "Compiling EIMS Project..." -ForegroundColor Green

# Compile Java files
javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "lib\h2-2.2.224.jar;resources" -d bin src\*.java src\Student\*.java src\Teacher\*.java src\database\*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful!" -ForegroundColor Green
    
    # Copy FXML files
    Write-Host "Copying FXML files..." -ForegroundColor Yellow
    Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force
    New-Item -ItemType Directory -Path "bin\Teacher" -Force | Out-Null
    New-Item -ItemType Directory -Path "bin\Student" -Force | Out-Null
    Copy-Item -Path "src\Teacher\*.fxml" -Destination "bin\Teacher\" -Force
    Copy-Item -Path "src\Student\*.fxml" -Destination "bin\Student\" -Force
    
    # Copy resources folder
    Write-Host "Copying resources..." -ForegroundColor Yellow
    if (Test-Path "resources") {
        Copy-Item -Path "resources" -Destination "bin\" -Recurse -Force
    }
    
    Write-Host "`nProject ready to run! Use .\run.ps1 to start the application." -ForegroundColor Green
    Write-Host "`nTest Credentials:" -ForegroundColor Cyan
    Write-Host "Teacher: teacher@eims.com / teacher123" -ForegroundColor Yellow
    Write-Host "Student: student@eims.com / student123" -ForegroundColor Yellow
} else {
    Write-Host "Compilation failed. Please check the errors above." -ForegroundColor Red
}
