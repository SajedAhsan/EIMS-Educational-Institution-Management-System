package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./eims_db";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "admin";
    
    private static DatabaseManager instance;
    private Connection connection;
    
    private DatabaseManager() {
        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            initializeTables();
            insertDefaultData();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
    
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
    
    private void initializeTables() {
        try (Statement stmt = connection.createStatement()) {
            // Create Teachers table
            String createTeachersTable = "CREATE TABLE IF NOT EXISTS teachers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "subject VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createTeachersTable);
            
            // Create Students table
            String createStudentsTable = "CREATE TABLE IF NOT EXISTS students (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "grade VARCHAR(50), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createStudentsTable);
            
            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to create tables: " + e.getMessage());
        }
    }
    
    private void insertDefaultData() {
        try {
            // Check if default teacher exists
            String checkTeacher = "SELECT COUNT(*) FROM teachers WHERE email = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(checkTeacher)) {
                pstmt.setString(1, "teacher@eims.com");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    // Insert default teacher
                    String insertTeacher = "INSERT INTO teachers (email, password, name, subject) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertTeacher)) {
                        insertStmt.setString(1, "teacher@eims.com");
                        insertStmt.setString(2, "teacher123");
                        insertStmt.setString(3, "John Doe");
                        insertStmt.setString(4, "Mathematics");
                        insertStmt.executeUpdate();
                        System.out.println("Default teacher account created: teacher@eims.com / teacher123");
                    }
                }
            }
            
            // Check if default student exists
            String checkStudent = "SELECT COUNT(*) FROM students WHERE email = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(checkStudent)) {
                pstmt.setString(1, "student@eims.com");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    // Insert default student
                    String insertStudent = "INSERT INTO students (email, password, name, grade) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertStudent)) {
                        insertStmt.setString(1, "student@eims.com");
                        insertStmt.setString(2, "student123");
                        insertStmt.setString(3, "Jane Smith");
                        insertStmt.setString(4, "Grade 10");
                        insertStmt.executeUpdate();
                        System.out.println("Default student account created: student@eims.com / student123");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to insert default data: " + e.getMessage());
        }
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
