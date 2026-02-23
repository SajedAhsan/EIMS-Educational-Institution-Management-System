package database;

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
            
            // Add extended profile columns to students table if they don't exist
            String[] alterColumns = {
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS student_no VARCHAR(50)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS level_term VARCHAR(100)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(50)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS phone_number VARCHAR(50)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS mobile_banking VARCHAR(100)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS present_address TEXT",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS permanent_address TEXT",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS contact_person TEXT",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS birth_reg_no VARCHAR(100)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS birth_date VARCHAR(50)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS nid VARCHAR(50)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS name_bangla VARCHAR(255)",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS photo_path VARCHAR(500)"
            };
            for (String alter : alterColumns) {
                stmt.execute(alter);
            }

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

    // ── Student profile ──────────────────────────────────────────────────────

    public StudentProfileData getStudentProfile(String email) throws SQLException {
        String sql = "SELECT name, email, student_no, level_term, mobile_number, bank_account, " +
                     "phone_number, mobile_banking, present_address, permanent_address, " +
                     "contact_person, birth_reg_no, birth_date, nid, name_bangla, photo_path " +
                     "FROM students WHERE email = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new StudentProfileData(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("student_no"),
                        rs.getString("level_term"),
                        rs.getString("mobile_number"),
                        rs.getString("bank_account"),
                        rs.getString("phone_number"),
                        rs.getString("mobile_banking"),
                        rs.getString("present_address"),
                        rs.getString("permanent_address"),
                        rs.getString("contact_person"),
                        rs.getString("birth_reg_no"),
                        rs.getString("birth_date"),
                        rs.getString("nid"),
                        rs.getString("name_bangla"),
                        rs.getString("photo_path")
                    );
                }
            }
        }
        return null;
    }

    public void updateStudentProfile(String email, StudentProfileData p) throws SQLException {
        String sql = "UPDATE students SET name=?, student_no=?, level_term=?, mobile_number=?, " +
                     "bank_account=?, phone_number=?, mobile_banking=?, present_address=?, " +
                     "permanent_address=?, contact_person=?, birth_reg_no=?, birth_date=?, " +
                     "nid=?, name_bangla=?, photo_path=? WHERE email=?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1,  p.getName());
            pstmt.setString(2,  p.getStudentNo());
            pstmt.setString(3,  p.getLevelTerm());
            pstmt.setString(4,  p.getMobileNumber());
            pstmt.setString(5,  p.getBankAccount());
            pstmt.setString(6,  p.getPhoneNumber());
            pstmt.setString(7,  p.getMobileBanking());
            pstmt.setString(8,  p.getPresentAddress());
            pstmt.setString(9,  p.getPermanentAddress());
            pstmt.setString(10, p.getContactPerson());
            pstmt.setString(11, p.getBirthRegNo());
            pstmt.setString(12, p.getBirthDate());
            pstmt.setString(13, p.getNid());
            pstmt.setString(14, p.getNameBangla());
            pstmt.setString(15, p.getPhotoPath());
            pstmt.setString(16, email);
            pstmt.executeUpdate();
        }
    }

    /**
     * @return true if the old password matched and the update succeeded.
     */
    public boolean updateStudentPassword(String email, String oldPassword, String newPassword) throws SQLException {
        String check = "SELECT id FROM students WHERE email=? AND password=?";
        try (PreparedStatement ps = getConnection().prepareStatement(check)) {
            ps.setString(1, email);
            ps.setString(2, oldPassword);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
            }
        }
        String update = "UPDATE students SET password=? WHERE email=?";
        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            ps.executeUpdate();
        }
        return true;
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

    // ── StudentProfileData ────────────────────────────────────────────────────
    public static class StudentProfileData {
        private String name, email, studentNo, levelTerm, mobileNumber, bankAccount;
        private String phoneNumber, mobileBanking, presentAddress, permanentAddress;
        private String contactPerson, birthRegNo, birthDate, nid, nameBangla, photoPath;

        public StudentProfileData(String name, String email, String studentNo, String levelTerm,
                                   String mobileNumber, String bankAccount, String phoneNumber,
                                   String mobileBanking, String presentAddress, String permanentAddress,
                                   String contactPerson, String birthRegNo, String birthDate,
                                   String nid, String nameBangla, String photoPath) {
            this.name = name; this.email = email; this.studentNo = studentNo;
            this.levelTerm = levelTerm; this.mobileNumber = mobileNumber;
            this.bankAccount = bankAccount; this.phoneNumber = phoneNumber;
            this.mobileBanking = mobileBanking; this.presentAddress = presentAddress;
            this.permanentAddress = permanentAddress; this.contactPerson = contactPerson;
            this.birthRegNo = birthRegNo; this.birthDate = birthDate;
            this.nid = nid; this.nameBangla = nameBangla; this.photoPath = photoPath;
        }

        public String getName()            { return name; }
        public String getEmail()           { return email; }
        public String getStudentNo()       { return studentNo; }
        public String getLevelTerm()       { return levelTerm; }
        public String getMobileNumber()    { return mobileNumber; }
        public String getBankAccount()     { return bankAccount; }
        public String getPhoneNumber()     { return phoneNumber; }
        public String getMobileBanking()   { return mobileBanking; }
        public String getPresentAddress()  { return presentAddress; }
        public String getPermanentAddress(){ return permanentAddress; }
        public String getContactPerson()   { return contactPerson; }
        public String getBirthRegNo()      { return birthRegNo; }
        public String getBirthDate()       { return birthDate; }
        public String getNid()             { return nid; }
        public String getNameBangla()      { return nameBangla; }
        public String getPhotoPath()       { return photoPath; }

        public void setName(String v)            { name = v; }
        public void setStudentNo(String v)       { studentNo = v; }
        public void setLevelTerm(String v)       { levelTerm = v; }
        public void setMobileNumber(String v)    { mobileNumber = v; }
        public void setBankAccount(String v)     { bankAccount = v; }
        public void setPhoneNumber(String v)     { phoneNumber = v; }
        public void setMobileBanking(String v)   { mobileBanking = v; }
        public void setPresentAddress(String v)  { presentAddress = v; }
        public void setPermanentAddress(String v){ permanentAddress = v; }
        public void setContactPerson(String v)   { contactPerson = v; }
        public void setBirthRegNo(String v)      { birthRegNo = v; }
        public void setBirthDate(String v)       { birthDate = v; }
        public void setNid(String v)             { nid = v; }
        public void setNameBangla(String v)      { nameBangla = v; }
        public void setPhotoPath(String v)         { photoPath = v; }
    }
}
