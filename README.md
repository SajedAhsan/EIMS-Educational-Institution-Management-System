# EIMS - Educational Institution Management System

## Project Structure

```
EIMS/
├── src/
│   ├── Main.java                    # Application entry point
│   ├── StartPageController.java     # Start page controller
│   ├── startPage.fxml              # Start page UI layout
│   ├── Student/                    # Student-related code
│   │   ├── StudentLogin.java
│   │   └── StudentDashboard.java
│   └── Teacher/                    # Teacher-related code
│       ├── TeacherLogin.java
│       └── TeacherDashboard.java
├── bin/                            # Compiled .class files (auto-generated)
├── resources/
│   └── images/                     # Image resources
│       ├── Teacher.jpg
│       └── Student.jpg
└── javafx-sdk-24.0.2/             # JavaFX SDK
```

## Requirements

- Java Development Kit (JDK) 11 or higher
- JavaFX SDK 24.0.2 (included in project)

## Compile and Run Commands

### 1. Compile the Project

```powershell
javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -d bin src\*.java src\Student\*.java src\Teacher\*.java
```

This command:
- Compiles all Java files from `src/`, `src/Student/`, and `src/Teacher/`
- Places compiled `.class` files in the `bin/` directory
- Includes JavaFX modules for controls and FXML

### 2. Copy FXML Files to bin

```powershell
Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force
```

### 3. Run the Application

```powershell
java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp bin Main
```

## Quick Commands (All-in-One)

### Compile and Run Together:

```powershell
javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -d bin src\*.java src\Student\*.java src\Teacher\*.java; Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force; java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp bin Main
```

### For Linux/Mac:

**Compile:**
```bash
javac --module-path "javafx-sdk-24.0.2/lib" --add-modules javafx.controls,javafx.fxml -d bin src/*.java src/Student/*.java src/Teacher/*.java
```

**Copy FXML:**
```bash
cp src/startPage.fxml bin/
```

**Run:**
```bash
java --module-path "javafx-sdk-24.0.2/lib" --add-modules javafx.controls,javafx.fxml -cp bin Main
```

## Features

- **Start Page**: Choose between Teacher and Student login
- **Teacher Module**: Login and dashboard (placeholder)
- **Student Module**: Login and dashboard (placeholder)
- **Organized Structure**: Student and Teacher code in separate folders
- **Clean Build**: All .class files in separate bin/ directory

## Adding New Features

### For Student Features:
Add new Java files in `src/Student/` folder

### For Teacher Features:
Add new Java files in `src/Teacher/` folder

### For New UI Screens:
1. Create FXML file in `src/`
2. Create corresponding controller Java file
3. Remember to copy FXML to `bin/` after changes

## Troubleshooting

**Issue**: `ClassNotFoundException` or class not found
**Solution**: Make sure you compiled with `-d bin` and running with `-cp bin`

**Issue**: FXML LoadException
**Solution**: Make sure you copied the FXML file to bin/ directory

**Issue**: Image not loading
**Solution**: Verify images exist in `resources/images/` directory

## Notes

- The `bin/` directory contains all compiled `.class` files
- Student-related code is in `src/Student/`
- Teacher-related code is in `src/Teacher/`
- Main application files are in `src/` root
- Resources (images) are in `resources/` directory


javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -d bin src\*.java src\Student\*.java src\Teacher\*.java; Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force; java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp bin Main