# EIMS - H2 Database Setup Instructions

## Project Setup Complete! ✅

The H2 database has been successfully integrated into your EIMS project.

## What's Been Added:

1. **Database Package** (`src/database/`)
   - `DatabaseManager.java` - Manages H2 database connection and initialization
   - `AuthenticationService.java` - Handles user authentication

2. **H2 Database Jar** (`lib/h2-2.2.224.jar`)
   - Downloaded and ready to use

3. **Updated Controllers**
   - `teacherLoginController.java` - Now uses database authentication
   - `studentLoginController.java` - Now uses database authentication

4. **Helper Scripts**
   - `compile.ps1` - Compile the project
   - `run.ps1` - Run the application

## How to Use:

### Method 1: Using Helper Scripts (Recommended)

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

**If you get compilation errors:**
- Make sure the H2 jar is in the `lib` folder
- Ensure JavaFX SDK is in the `javafx-sdk-24.0.2` folder
- Check that all source files are present

**If the database doesn't work:**
- Delete `eims_db.mv.db` and restart the application
- Check console for error messages

## Next Steps:

You can now:
1. Add more users to the database
2. Implement password hashing for security
3. Add user registration functionality
4. Expand dashboard features
5. Add more database tables for courses, grades, etc.

---
**Enjoy your EIMS Application with database authentication!** 🎓
