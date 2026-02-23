package main.java.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
        try (Statement stmt = getConnection().createStatement()) {
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
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createStudentsTable);
            
            // Create Groups table
            String createGroupsTable = "CREATE TABLE IF NOT EXISTS groups (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "teacher_id INT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (teacher_id) REFERENCES teachers(id))";
            stmt.execute(createGroupsTable);
            
            // Create Group Members table
            String createGroupMembersTable = "CREATE TABLE IF NOT EXISTS group_members (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "group_id INT NOT NULL, " +
                    "student_id INT NOT NULL, " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE, " +
                    "UNIQUE(group_id, student_id))";
            stmt.execute(createGroupMembersTable);
            
            // Create Educational Assets table
            String createAssetsTable = "CREATE TABLE IF NOT EXISTS educational_assets (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "group_id INT NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "file_path VARCHAR(500), " +
                    "asset_type VARCHAR(50), " +
                    "shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE)";
            stmt.execute(createAssetsTable);
            
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
            try (PreparedStatement pstmt = getConnection().prepareStatement(checkTeacher)) {
                pstmt.setString(1, "teacher@eims.com");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    // Insert default teacher
                    String insertTeacher = "INSERT INTO teachers (email, password, name, subject) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = getConnection().prepareStatement(insertTeacher)) {
                        insertStmt.setString(1, "teacher@eims.com");
                        insertStmt.setString(2, "teacher123");
                        insertStmt.setString(3, "Teacher 1");
                        insertStmt.setString(4, "Mathematics");
                        insertStmt.executeUpdate();
                        System.out.println("Default teacher account created: teacher@eims.com / teacher123");
                    }
                }
            }
            
            // Check if default student exists
            String checkStudent = "SELECT COUNT(*) FROM students WHERE email = ?";
            try (PreparedStatement pstmt = getConnection().prepareStatement(checkStudent)) {
                pstmt.setString(1, "student@eims.com");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    // Insert default students
                    String insertStudent = "INSERT INTO students (email, password, name) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = getConnection().prepareStatement(insertStudent)) {
                        // Student 1 (original)
                        insertStmt.setString(1, "student@eims.com");
                        insertStmt.setString(2, "12345678");
                        insertStmt.setString(3, "Student 1");
                        insertStmt.executeUpdate();
                        
                        // Student 2-10
                        for (int i = 2; i <= 10; i++) {
                            insertStmt.setString(1, "student" + i + "@eims.com");
                            insertStmt.setString(2, "12345678");
                            insertStmt.setString(3, "Student " + i);
                            insertStmt.executeUpdate();
                        }
                        
                        System.out.println("Default student accounts created successfully (student@eims.com, student2-10@eims.com)");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to insert default data: " + e.getMessage());
        }
    }
    
    // Group management methods
    public int createGroup(String groupName, int teacherId) throws SQLException {
        String sql = "INSERT INTO groups (name, teacher_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, groupName);
            pstmt.setInt(2, teacherId);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
    
    public void addStudentToGroup(int groupId, int studentId) throws SQLException {
        String sql = "INSERT INTO group_members (group_id, student_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, studentId);
            pstmt.executeUpdate();
        }
    }
    
    public void deleteAllGroups() throws SQLException {
        String deleteMembers = "DELETE FROM group_members";
        String deleteAssets = "DELETE FROM educational_assets";
        String deleteGroups = "DELETE FROM groups";
        
        try (PreparedStatement pstmt1 = getConnection().prepareStatement(deleteMembers);
             PreparedStatement pstmt2 = getConnection().prepareStatement(deleteAssets);
             PreparedStatement pstmt3 = getConnection().prepareStatement(deleteGroups)) {
            pstmt1.executeUpdate();
            pstmt2.executeUpdate();
            pstmt3.executeUpdate();
            System.out.println("All groups deleted successfully");
        }
    }
    
    public List<StudentData> searchStudentsByEmail(String searchPattern) throws SQLException {
        String sql = "SELECT id, email, name FROM students " +
                     "WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ? " +
                     "ORDER BY CASE WHEN LOWER(name) LIKE ? THEN 0 ELSE 1 END, name";
        List<StudentData> students = new ArrayList<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            String pattern = "%" + searchPattern.toLowerCase() + "%";
            String startPattern = searchPattern.toLowerCase() + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, startPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(new StudentData(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("name")
                    ));
                }
            }
        }
        
        return students;
    }
    
    public List<GroupData> getGroupsByTeacher(int teacherId) throws SQLException {
        String sql = "SELECT id, name, created_at FROM groups WHERE teacher_id = ? ORDER BY created_at DESC";
        List<GroupData> groups = new ArrayList<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teacherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(new GroupData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return groups;
    }
    
    public List<GroupData> getGroupsByStudent(int studentId) throws SQLException {
        String sql = "SELECT g.id, g.name, g.created_at " +
                     "FROM groups g " +
                     "JOIN group_members gm ON g.id = gm.group_id " +
                     "WHERE gm.student_id = ? " +
                     "ORDER BY g.created_at DESC";
        List<GroupData> groups = new ArrayList<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(new GroupData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return groups;
    }
    
    public List<MemberData> getGroupMembers(int groupId) throws SQLException {
        String sql = "SELECT s.id, s.email, s.name, gm.joined_at " +
                     "FROM group_members gm " +
                     "JOIN students s ON gm.student_id = s.id " +
                     "WHERE gm.group_id = ? ORDER BY s.name";
        List<MemberData> members = new ArrayList<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(new MemberData(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getTimestamp("joined_at")
                    ));
                }
            }
        }
        return members;
    }
    
    public void addEducationalAsset(int groupId, String title, String description, String filePath, String assetType) throws SQLException {
        String sql = "INSERT INTO educational_assets (group_id, title, description, file_path, asset_type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setString(4, filePath);
            pstmt.setString(5, assetType);
            pstmt.executeUpdate();
        }
    }
    
    public List<AssetData> getGroupAssets(int groupId) throws SQLException {
        String sql = "SELECT id, title, description, file_path, asset_type, shared_at " +
                     "FROM educational_assets WHERE group_id = ? ORDER BY shared_at DESC";
        List<AssetData> assets = new ArrayList<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assets.add(new AssetData(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("file_path"),
                        rs.getString("asset_type"),
                        rs.getTimestamp("shared_at")
                    ));
                }
            }
        }
        return assets;
    }
    
    public GroupDetailData getGroupById(int groupId) throws SQLException {
        String sql = "SELECT g.id, g.name, g.created_at, t.name as teacher_name, t.email as teacher_email " +
                     "FROM groups g JOIN teachers t ON g.teacher_id = t.id WHERE g.id = ?";
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new GroupDetailData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at"),
                        rs.getString("teacher_name"),
                        rs.getString("teacher_email")
                    );
                }
            }
        }
        return null;
    }
    
    public int getTeacherIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM teachers WHERE email = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }
    
    public int getStudentIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM students WHERE email = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
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
    
    // Data classes for transferring data safely
    public static class StudentData {
        private int id;
        private String email;
        private String name;
        
        public StudentData(int id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
    }
    
    public static class GroupData {
        private int id;
        private String name;
        private java.sql.Timestamp createdAt;
        
        public GroupData(int id, String name, java.sql.Timestamp createdAt) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public java.sql.Timestamp getCreatedAt() { return createdAt; }
    }
    
    public static class MemberData {
        private int id;
        private String email;
        private String name;
        private java.sql.Timestamp joinedAt;
        
        public MemberData(int id, String email, String name, java.sql.Timestamp joinedAt) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.joinedAt = joinedAt;
        }
        
        public int getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public java.sql.Timestamp getJoinedAt() { return joinedAt; }
    }
    
    public static class AssetData {
        private int id;
        private String title;
        private String description;
        private String filePath;
        private String assetType;
        private java.sql.Timestamp sharedAt;
        
        public AssetData(int id, String title, String description, String filePath, String assetType, java.sql.Timestamp sharedAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.filePath = filePath;
            this.assetType = assetType;
            this.sharedAt = sharedAt;
        }
        
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getFilePath() { return filePath; }
        public String getAssetType() { return assetType; }
        public java.sql.Timestamp getSharedAt() { return sharedAt; }
    }
    
    public static class GroupDetailData {
        private int id;
        private String name;
        private java.sql.Timestamp createdAt;
        private String teacherName;
        private String teacherEmail;
        
        public GroupDetailData(int id, String name, java.sql.Timestamp createdAt, String teacherName, String teacherEmail) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
            this.teacherName = teacherName;
            this.teacherEmail = teacherEmail;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public java.sql.Timestamp getCreatedAt() { return createdAt; }
        public String getTeacherName() { return teacherName; }
        public String getTeacherEmail() { return teacherEmail; }
    }
}
