# Maven Quick Reference Guide

## Quick Start Commands

### Windows
```powershell
# Compile the project
.\maven-compile.ps1

# Run the application
.\maven-run.ps1
```

### Using Maven Directly

#### Build Commands
```bash
mvn clean               # Clean build artifacts
mvn compile             # Compile source code
mvn clean compile       # Clean and compile
mvn test                # Run tests (when available)
mvn package             # Create JAR file
mvn clean package       # Clean and create JAR
```

#### Run Commands
```bash
mvn javafx:run          # Run the JavaFX application
```

#### Dependency Management
```bash
mvn dependency:tree     # View dependency tree
mvn dependency:purge-local-repository  # Clear local dependencies
mvn clean install -U    # Force update dependencies
```

### Without Maven Installed (Using Wrapper)

Windows:
```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

Linux/Mac:
```bash
./mvnw clean compile
./mvnw javafx:run
```

## Working with the Project

### Adding Dependencies
Edit `pom.xml` and add to `<dependencies>` section:
```xml
<dependency>
    <groupId>group.id</groupId>
    <artifactId>artifact-id</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then run: `mvn clean compile`

### Project Structure (Maven Standard)
```
src/main/java/          # Java source files
src/main/resources/     # FXML, images, config files
target/                 # Build output (auto-generated)
pom.xml                 # Maven configuration
```

### Creating Distributable JAR
```bash
mvn clean package
```
JAR file will be in `target/` directory.

### Running the JAR
```bash
java -jar target/educational-institution-management-system-1.0.0.jar
```

## Troubleshooting

### Maven Not Found
Solution: Use the wrapper scripts (`mvnw.cmd` or `mvnw`)

### Compilation Errors
```bash
mvn clean              # Clean old builds
mvn compile -X         # Compile with debug output
```

### Dependency Issues
```bash
mvn clean install -U   # Force update all dependencies
```

### Clear Maven Cache
```bash
# Windows
Remove-Item -Recurse -Force ~\.m2\repository\com\eims

# Linux/Mac
rm -rf ~/.m2/repository/com/eims
```

### JavaFX Runtime Errors
Make sure you're using `mvn javafx:run` instead of running Main class directly.

## Advantages of Maven

✅ **Automatic dependency management** - No manual JAR downloads  
✅ **Cross-platform** - Works on Windows, Linux, macOS  
✅ **Version control** - All dependencies versioned in pom.xml  
✅ **Build reproducibility** - Same build everywhere  
✅ **CI/CD ready** - Standard commands for automation  
✅ **Easy updates** - Change version in pom.xml and rebuild  

## Legacy Build System

If you prefer the original build system or Maven has issues:

```powershell
.\compile.ps1          # Compile with legacy build
.\run.ps1              # Run with legacy build
```

Both systems work independently and maintain the same functionality.

---

**Need Help?** 
- Maven Documentation: https://maven.apache.org/guides/
- JavaFX with Maven: https://openjfx.io/openjfx-docs/#maven
