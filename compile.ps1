# Compile EIMS Project with H2 Database
Write-Host "Compiling EIMS Project..." -ForegroundColor Green

# Compile Java files from Maven structure with subdirectories
javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "lib\h2-2.2.224.jar;src\main\resources" -d bin src\main\java\*.java src\main\java\Student\*.java src\main\java\Teacher\*.java src\main\java\database\*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful!" -ForegroundColor Green
    
    # Copy FXML files
    Write-Host "Copying FXML files..." -ForegroundColor Yellow
    Copy-Item -Path "src\main\resources\startPage.fxml" -Destination "bin\" -Force
    New-Item -ItemType Directory -Path "bin\Teacher" -Force | Out-Null
    New-Item -ItemType Directory -Path "bin\Student" -Force | Out-Null
    Copy-Item -Path "src\main\resources\Teacher\*.fxml" -Destination "bin\Teacher\" -Force
    Copy-Item -Path "src\main\resources\Student\*.fxml" -Destination "bin\Student\" -Force
    
    # Copy resources folder
    Write-Host "Copying resources..." -ForegroundColor Yellow
    if (Test-Path "src\main\resources\images") {
        Copy-Item -Path "src\main\resources\images" -Destination "bin\" -Recurse -Force
    }
    
    Write-Host "`nProject ready to run! Use .\run.ps1 to start the application." -ForegroundColor Green
    Write-Host "`nTest Credentials:" -ForegroundColor Cyan
    Write-Host "Teacher: teacher@eims.com / teacher123" -ForegroundColor Yellow
    Write-Host "Student: student@eims.com / student123" -ForegroundColor Yellow
} else {
    Write-Host "Compilation failed. Please check the errors above." -ForegroundColor Red
}
