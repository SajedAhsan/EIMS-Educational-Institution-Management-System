# EIMS - Educational Institution Management System

A JavaFX-based management system for educational institutions with H2 database integration.

## 📋 Requirements

- **Java Development Kit (JDK)** 17 or higher
- **Maven** 3.6+ (optional - Maven wrapper included for portability)

### Legacy Build Requirements (if not using Maven)
- **JavaFX SDK** 24.0.2 (included in project)
- **H2 Database** (included in lib folder)
- **Windows** operating system

## 🚀 Quick Start

### Method 1: Using Maven (Recommended for Portability)

```powershell
# Compile
.\maven-compile.ps1

# Run
.\maven-run.ps1
```

Or use Maven commands directly:
```powershell
mvn clea
│   ├── main/
│   │   ├── java/                   # Java source files (Maven)
│   │   │   ├── Main.java           # Application entry point
│   │   │   ├── StartPageController.java
│   │   │   ├── database/           # Database package
│   │   │   │   ├── DatabaseManager.java
│   │   │   │   └── AuthenticationService.java
│   │   │   ├── Student/            # Student module
│   │   │   └── Teacher/            # Teacher module
│   │   └── resources/              # Resources (Maven)
│   │       ├── startPage.fxml      # FXML files
│   │       ├── Student/            # Student FXML files
│   │       ├── Teacher/            # Teacher FXML files
│   │       └── images/             # Image assets
│   │           ├── Teacher.jpg
│   │           └── Student.jpg
│   └── [legacy source files]       # Original source structure (kept for compatibility)
├── lib/                            # External libraries (legacy)
│   └── h2-2.2.224.jar
├── javafx-sdk-24.0.2/             # JavaFX SDK (legacy)
├── target/                         # Maven build output
├── bin/                            # Legacy build output
├── pom.xml                         # Maven configuration
├── mvnw, mvnw.cmd                 # Maven wrapper scripts
├── maven-compile.ps1              # Maven compile script
├── maven-run.ps1                  # Maven run script
├── compile.ps1                    # Legacy compile script
└── run.ps1                        # Legacy rtudent module
│   │   ├── studentLoginController.java
│   │   ├── studentLoginPage.fxml
│   │   ├── studentDashboardController.java
│   │   └── StudentDashboard.fxml
│   └── Teacher/                    # Teacher module
│       ├── teacherLoginController.java
│       ├── teacherLoginPage.fxml
│       ├── teacherDashboardController.java
│       └── TeacherDashboard.fxml
├── resources/                      # Resources folder
│   └── images/                     # Image assets
│       ├── Teacher.jpg
│       └── Student.jpg
├── lib/                            # External libraries
│   └── h2-2.2.224.jar             # H2 Database
├── javafx-sdk-24.0.2/             # JavaFX SDK
├── bin/                            # Compiled classes (auto-generated)
├── compile.ps1                     # Compile script
└── run.ps1                         # Run script
```

## 🔐 Default Login Credentials

### Teacher Account
- **Email:** teacher@eims.com
- **Password:** teacher123

### Student Account
- **Email:** student@eims.com
- **Password:** student123

## 💾 Database

- **Type:** H2 (Embedded)
- **File:** `eims_db.mv.db` (auto-created on first run)
- **Connection:** `jdbc:h2:./eims_db`
- **Version:** 2.2.224 (managed by Maven or included in lib/)
- **Tables:** `teachers`, `students`

Maven automatically downloads and manages the H2 dependency. For legacy builds, the H2 JAR is included in the `lib/` folder.

## 🛠️ Manual Compilation & Execution

### Using Maven

**Compile:**
```powershell
mvn clean compile
```

**Run:**
```powershell
mvn javafx:run
```

**Package as JAR:**
```powershell
mvn clean package
```

**Run without Maven installed (using wrapper):**
```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

### Using Legacy Build (Windows)

**Compile:**
```powershelljavac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "lib\h2-2.2.224.jar;resources" -d bin src\*.java src\Student\*.java src\Teacher\*.java src\database\*.java

# Copy FXML files
Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force
New-Item -ItemType Directory -Path "bin\Teacher" -Force
New-Item -ItemType Directory -Path "bin\Student" -Force
Copy-Item -Path "src\Teacher\*.fxml" -Destination "bin\Teacher\" -Force
Copy-Item -Path "src\Student\*.fxml" -Destination "bin\Student\" -Force

# Copy resources
Copy-Item -Path "resources" -Destination "bin\" -Recurse -Force
```

**Run:**
```powershell
java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\h2-2.2.224.jar;resources" Main
```

### Linux / macOS

**C project includes `.vscode/launch.json` for easy debugging. Simply:
1. Open the project in VS Code
2. Install Java Extension Pack
3. Press F5 to run/debug

## ✨ Features

- ✅ **Maven support for true cross-platform portability**
- ✅ Maven wrapper included - no Maven installation required
- ✅ Automatic dependency management
- ✅ Cross-platform compatibility (Windows, Linux, macOS)
- ✅ Teacher and Student login modules
- ✅ Database authentication with H2
- ✅ Automatic table creation and default data insertion
- ✅ Relative paths - works from any directory
- ✅ Clean MVC architecture
- ✅ Legacy build scripts maintained for backward compatibility

## 🐛 Troubleshooting

### Maven Issues
- If Maven is not installed, use the wrapper: `.\mvnw.cmd` (Windows) or `./mvnw` (Linux/Mac)
- Clear Maven cache: `mvn clean` or delete `target/` folder
- Update dependencies: `mvn clean install -U`

### Compilation Errors
- Ensure JDK 17+ is installed: `java -version`
- For Maven: Check `pom.xml` for correct dependencies
- For legacy: Verify JavaFX SDK is in `javafx-sdk-24.0.2` folder
- Check that H2 jar is in `lib` folder (legacy build)

### Database Issues
- Delete `eims_db.mv.db` and restart
- Check console for error messages

### Permission Denied (Linux/Mac)
```bash
chmod +x compile.sh run.sh
```

### Teacher and Student login modules
- ✅ Database authentication with H2
- ✅ Automatic table creation and default data insertion
- ✅ Relative paths - works from any directory on Windows
- ✅ Clean MVC architecture
- ✅ Easy-to-use PowerShell scripts

### New Teacher Features
Add Java files to `src/Teacher/` folder

### New Student Features
Add Java files to `src/Student/` folder

### New UI Screens
1. Create FXML file in appropriate folder
2. Create controller class
3. Update compile scripts to copy new FXML files

## Scripts Won't Run
- Add attendance system
- Enhance dashboard features

## 📄 License

Educational project - free to use and modify

---

**Made with ❤️ for Educational Institutions**