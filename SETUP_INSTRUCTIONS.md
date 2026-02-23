# EIMS - Setup Instructions

## Project Setup Complete! ✅

The EIMS project now supports **Maven** for enhanced portability and dependency management!

## What's Been Added:

### Maven Support (NEW!)
- **pom.xml** - Maven configuration with all dependencies
- **Maven wrapper** (`mvnw`, `mvnw.cmd`) - Run without installing Maven
- **Maven build scripts** (`maven-compile.ps1`, `maven-run.ps1`)
- **Standard Maven directory structure** (`src/main/java`, `src/main/resources`)

### Database Components
1. **Database Package** (`src/database/` and `src/main/java/database/`)
   - `DatabaseManager.java` - Manages H2 database connection and initialization
   - `AuthenticationService.java` - Handles user authentication

2. **H2 Database**
   - Managed automatically by Maven
   - Legacy JAR available in `lib/h2-2.2.224.jar`

3. **Updated Controllers**
   - `teacherLoginController.java` - Uses database authentication
   - `studentLoginController.java` - Uses database authentication

## How to Use:

### Method 1: Using Maven (Recommended for Portability) ⭐

```powershell
# Compile the project
.\maven-compile.ps1

# Run the application
.\maven-run.ps1
```

Or use Maven commands directly:
```powershell
mvn clean compile    # Compile
mvn javafx:run       # Run
mvn clean package    # Create JAR
```

**No Maven installed?** Use the wrapper:
```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

### Method 2: Using Legacy Scripts

```powershell
# Compile the project
.\compile.ps1

# Run the application
.\run.ps1
```

### Method 2: Manual Commands

**Compile:**
```powershell
javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "lib\h2-2.2.224.jar" -d bin src\*.java src\Student\*.java src\Teacher\*.java src\database\*.java

Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force
New-Item -ItemType Directory -Path "bin\Teacher" -Force
New-Item -ItemType Directory -Path "bin\Student" -Force
Copy-Item -Path "src\Teacher\*.fxml" -Destination "bin\Teacher\" -Force
Copy-Item -Path "src\Student\*.fxml" -Destination "bin\Student\" -Force
```

**Run:**
```powershell
java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\h2-2.2.224.jar" Main
```

## Default Login Credentials:

### Teacher Account:
- **Email:** teacher@eims.com
- **Password:** teacher123
- **Name:** John Doe
- **Subject:** Mathematics

### Student Account:
- **Email:** student@eims.com
- **Password:** student123
- **Name:** Jane Smith
- **Grade:** Grade 10

## Database Details:

- **Database Type:** H2 (Embedded)
- **Database File:** `eims_db.mv.db` (auto-created in project root)
- **Connection URL:** jdbc:h2:./eims_db
- **Admin User:** admin
- **Admin Password:** admin

## Features:

✅ Automatic table creation on first run
✅ Default user accounts inserted automatically
✅ Login validation with database
✅ User-friendly error messages via Alert dialogs
✅ Secure password verification
✅ Personalized welcome messages

## Database Structure:

### Teachers Table:
- id (INT, Auto-increment, Primary Key)
- email (VARCHAR, Unique)
- password (VARCHAR)
- name (VARCHAR)
- subject (VARCHAR)
- created_at (TIMESTAMP)

### Students Table:
- id (INT, Auto-increment, Primary Key)
- email (VARCHAR, Unique)
- password (VARCHAR)
- name (VARCHAR)
- grade (VARCHAR)
- created_at (TIMESTAMP)

## Troubleshooting:

### Maven Issues:
- **Maven not found:** Use the wrapper scripts (`mvnw.cmd` or `mvnw`)
- **Dependency errors:** Run `mvn clean install -U` to force update
- **Build errors:** Delete `target/` folder and rebuild

### Legacy Build Issues:
**If you get compilation errors:**
- Make sure the H2 jar is in the `lib` folder
- Ensure JavaFX SDK is in the `javafx-sdk-24.0.2` folder
- Check that all source files are present

**If the database doesn't work:**
- Delete `eims_db.mv.db` and restart the application
- Check console for error messages

### General Issues:
- Ensure JDK 17+ is installed: `java -version`
- Make sure you're in the project root directory
- Check console output for detailed error messages

## Next Steps:

You can now:
1. **Deploy anywhere:** Maven ensures portability across Windows, Linux, and macOS
2. Add more users to the database
3. Implement password hashing for security
4. Add user registration functionality
5. Expand dashboard features
6. Add more database tables for courses, grades, etc.
7. **Create executable JAR:** Run `mvn clean package` to create a distributable JAR

## Maven Advantages:

✅ **True portability** - Works on any OS with Java
✅ **Automatic dependency management** - No manual JAR downloads
✅ **Version consistency** - Everyone uses the same dependency versions
✅ **Easy CI/CD integration** - Standard Maven commands
✅ **Maven wrapper included** - No Maven installation required
✅ **Backward compatible** - Legacy build scripts still work

---
**Enjoy your EIMS Application with Maven portability and database authentication!** 🎓
