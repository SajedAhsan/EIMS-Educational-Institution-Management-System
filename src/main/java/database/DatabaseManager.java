package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
            
            // Add deadline column to educational_assets if not present
            stmt.execute("ALTER TABLE educational_assets ADD COLUMN IF NOT EXISTS deadline TIMESTAMP");

            // Create submissions table
            stmt.execute("CREATE TABLE IF NOT EXISTS submissions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "asset_id INT NOT NULL, " +
                "student_id INT NOT NULL, " +
                "file_path VARCHAR(500) NOT NULL, " +
                "submission_time TIMESTAMP NOT NULL, " +
                "FOREIGN KEY (asset_id) REFERENCES educational_assets(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE, " +
                "UNIQUE(asset_id, student_id))");

            // Add deadline_notified flag to educational_assets if not present
            stmt.execute("ALTER TABLE educational_assets ADD COLUMN IF NOT EXISTS deadline_notified BOOLEAN DEFAULT FALSE");

            // Add evaluation columns to submissions if not present
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS grade INT");
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS feedback TEXT");
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS evaluated BOOLEAN DEFAULT FALSE");
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS evaluation_time TIMESTAMP");

            // Add plagiarism-detection columns to submissions if not present
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS plagiarized BOOLEAN DEFAULT FALSE");
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS similarity_score DOUBLE PRECISION DEFAULT 0.0");
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS matched_submission_id INT");

            // Create notifications table
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "user_type VARCHAR(10) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "is_read BOOLEAN DEFAULT FALSE)");

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
        addEducationalAsset(groupId, title, description, filePath, assetType, null);
    }

    public void addEducationalAsset(int groupId, String title, String description, String filePath, String assetType, LocalDateTime deadline) throws SQLException {
        String sql = "INSERT INTO educational_assets (group_id, title, description, file_path, asset_type, deadline) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setString(4, filePath);
            pstmt.setString(5, assetType);
            pstmt.setTimestamp(6, deadline != null ? Timestamp.valueOf(deadline) : null);
            pstmt.executeUpdate();
        }
    }
    
    public List<AssetData> getGroupAssets(int groupId) throws SQLException {
        String sql = "SELECT id, title, description, file_path, asset_type, shared_at, deadline " +
                     "FROM educational_assets WHERE group_id = ? ORDER BY shared_at DESC";
        List<AssetData> assets = new ArrayList<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp dl = rs.getTimestamp("deadline");
                    assets.add(new AssetData(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("file_path"),
                        rs.getString("asset_type"),
                        rs.getTimestamp("shared_at"),
                        dl != null ? dl.toLocalDateTime() : null
                    ));
                }
            }
        }
        return assets;
    }

    /** Add or replace a student submission (UPSERT via MERGE). */
    public void addSubmission(int assetId, int studentId, String filePath, LocalDateTime submissionTime) throws SQLException {
        String sql = "MERGE INTO submissions (asset_id, student_id, file_path, submission_time) " +
                     "KEY(asset_id, student_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, assetId);
            pstmt.setInt(2, studentId);
            pstmt.setString(3, filePath);
            pstmt.setTimestamp(4, Timestamp.valueOf(submissionTime));
            pstmt.executeUpdate();
        }
    }

    /** Return all submissions for an assignment (for teacher view). */
    public List<SubmissionData> getSubmissionsByAsset(int assetId) throws SQLException {
        String sql = "SELECT s.id, s.asset_id, s.student_id, st.name as student_name, " +
                     "ea.title as assignment_title, " +
                     "ea.deadline as assignment_deadline, " +
                     "s.file_path, s.submission_time, s.grade, s.feedback, s.evaluated, s.evaluation_time, " +
                     "s.plagiarized, s.similarity_score, s.matched_submission_id " +
                     "FROM submissions s " +
                     "JOIN students st ON s.student_id = st.id " +
                     "JOIN educational_assets ea ON s.asset_id = ea.id " +
                     "WHERE s.asset_id = ? ORDER BY s.submission_time DESC";
        List<SubmissionData> list = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp evalTs = rs.getTimestamp("evaluation_time");
                    Timestamp deadlineTs = rs.getTimestamp("assignment_deadline");
                    SubmissionData sd = new SubmissionData(
                        rs.getInt("id"),
                        rs.getInt("asset_id"),
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("assignment_title"),
                        deadlineTs != null ? deadlineTs.toLocalDateTime() : null,
                        rs.getString("file_path"),
                        rs.getTimestamp("submission_time").toLocalDateTime(),
                        rs.getObject("grade") != null ? rs.getInt("grade") : null,
                        rs.getString("feedback"),
                        rs.getBoolean("evaluated"),
                        evalTs != null ? evalTs.toLocalDateTime() : null
                    );
                    sd.setPlagiarized(rs.getBoolean("plagiarized"));
                    sd.setSimilarityScore(rs.getDouble("similarity_score"));
                    sd.setMatchedSubmissionId(
                        rs.getObject("matched_submission_id") != null
                            ? rs.getInt("matched_submission_id") : -1);
                    list.add(sd);
                }
            }
        }
        return list;
    }

    /** Return all submissions across all assignments in a group (for teacher overview). */
    public List<SubmissionData> getSubmissionsByGroup(int groupId) throws SQLException {
        String sql = "SELECT s.id, s.asset_id, s.student_id, st.name as student_name, " +
                     "ea.title as assignment_title, " +
                     "ea.deadline as assignment_deadline, " +
                     "s.file_path, s.submission_time, s.grade, s.feedback, s.evaluated, s.evaluation_time, " +
                     "s.plagiarized, s.similarity_score, s.matched_submission_id " +
                     "FROM submissions s " +
                     "JOIN students st ON s.student_id = st.id " +
                     "JOIN educational_assets ea ON s.asset_id = ea.id " +
                     "WHERE ea.group_id = ? ORDER BY s.submission_time DESC";
        List<SubmissionData> list = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp evalTs = rs.getTimestamp("evaluation_time");
                    Timestamp deadlineTs = rs.getTimestamp("assignment_deadline");
                    SubmissionData sd = new SubmissionData(
                        rs.getInt("id"),
                        rs.getInt("asset_id"),
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("assignment_title"),
                        deadlineTs != null ? deadlineTs.toLocalDateTime() : null,
                        rs.getString("file_path"),
                        rs.getTimestamp("submission_time").toLocalDateTime(),
                        rs.getObject("grade") != null ? rs.getInt("grade") : null,
                        rs.getString("feedback"),
                        rs.getBoolean("evaluated"),
                        evalTs != null ? evalTs.toLocalDateTime() : null
                    );
                    sd.setPlagiarized(rs.getBoolean("plagiarized"));
                    sd.setSimilarityScore(rs.getDouble("similarity_score"));
                    sd.setMatchedSubmissionId(
                        rs.getObject("matched_submission_id") != null
                            ? rs.getInt("matched_submission_id") : -1);
                    list.add(sd);
                }
            }
        }
        return list;
    }

    /** Return one student's submissions across all assignments in a group. */
    public List<SubmissionData> getSubmissionsByGroupAndStudent(int groupId, int studentId) throws SQLException {
        String sql = "SELECT s.id, s.asset_id, s.student_id, st.name as student_name, " +
                     "ea.title as assignment_title, " +
                     "ea.deadline as assignment_deadline, " +
                     "s.file_path, s.submission_time, s.grade, s.feedback, s.evaluated, s.evaluation_time, " +
                     "s.plagiarized, s.similarity_score, s.matched_submission_id " +
                     "FROM submissions s " +
                     "JOIN students st ON s.student_id = st.id " +
                     "JOIN educational_assets ea ON s.asset_id = ea.id " +
                     "WHERE ea.group_id = ? AND s.student_id = ? ORDER BY s.submission_time DESC";
        List<SubmissionData> list = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp evalTs = rs.getTimestamp("evaluation_time");
                    Timestamp deadlineTs = rs.getTimestamp("assignment_deadline");
                    SubmissionData sd = new SubmissionData(
                        rs.getInt("id"),
                        rs.getInt("asset_id"),
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("assignment_title"),
                        deadlineTs != null ? deadlineTs.toLocalDateTime() : null,
                        rs.getString("file_path"),
                        rs.getTimestamp("submission_time").toLocalDateTime(),
                        rs.getObject("grade") != null ? rs.getInt("grade") : null,
                        rs.getString("feedback"),
                        rs.getBoolean("evaluated"),
                        evalTs != null ? evalTs.toLocalDateTime() : null
                    );
                    sd.setPlagiarized(rs.getBoolean("plagiarized"));
                    sd.setSimilarityScore(rs.getDouble("similarity_score"));
                    sd.setMatchedSubmissionId(
                        rs.getObject("matched_submission_id") != null
                            ? rs.getInt("matched_submission_id") : -1);
                    list.add(sd);
                }
            }
        }
        return list;
    }

    /**
     * Persist plagiarism-detection results for a submission.
     * Pass {@code plagiarized = false} and {@code matchedSubmissionId = -1} to clear a previous flag.
     */
    public void updatePlagiarismResult(int submissionId, boolean plagiarized,
                                       double similarityScore, int matchedSubmissionId) throws SQLException {
        String sql = "UPDATE submissions SET plagiarized = ?, similarity_score = ?, " +
                     "matched_submission_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setBoolean(1, plagiarized);
            pstmt.setDouble(2, similarityScore);
            if (matchedSubmissionId > 0) {
                pstmt.setInt(3, matchedSubmissionId);
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.setInt(4, submissionId);
            pstmt.executeUpdate();
        }
    }

    /** Save teacher evaluation for a submission. */
    public void saveEvaluation(int submissionId, int grade, String feedback) throws SQLException {
        String sql = "UPDATE submissions SET grade = ?, feedback = ?, evaluated = TRUE, " +
                     "evaluation_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, grade);
            pstmt.setString(2, feedback);
            pstmt.setInt(3, submissionId);
            pstmt.executeUpdate();
        }
    }

    /** Return the student's own submission for a given assignment, or null. */
    public SubmissionData getSubmissionByStudentAndAsset(int studentId, int assetId) throws SQLException {
        String sql = "SELECT s.id, s.asset_id, s.student_id, st.name as student_name, " +
                     "ea.title as assignment_title, " +
                     "ea.deadline as assignment_deadline, " +
                     "s.file_path, s.submission_time, s.grade, s.feedback, s.evaluated, s.evaluation_time, " +
                     "s.plagiarized, s.similarity_score, s.matched_submission_id " +
                     "FROM submissions s " +
                     "JOIN students st ON s.student_id = st.id " +
                     "JOIN educational_assets ea ON s.asset_id = ea.id " +
                     "WHERE s.student_id = ? AND s.asset_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp evalTs = rs.getTimestamp("evaluation_time");
                    Timestamp deadlineTs = rs.getTimestamp("assignment_deadline");
                    SubmissionData sd = new SubmissionData(
                        rs.getInt("id"),
                        rs.getInt("asset_id"),
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("assignment_title"),
                        deadlineTs != null ? deadlineTs.toLocalDateTime() : null,
                        rs.getString("file_path"),
                        rs.getTimestamp("submission_time").toLocalDateTime(),
                        rs.getObject("grade") != null ? rs.getInt("grade") : null,
                        rs.getString("feedback"),
                        rs.getBoolean("evaluated"),
                        evalTs != null ? evalTs.toLocalDateTime() : null
                    );
                    sd.setPlagiarized(rs.getBoolean("plagiarized"));
                    sd.setSimilarityScore(rs.getDouble("similarity_score"));
                    sd.setMatchedSubmissionId(
                        rs.getObject("matched_submission_id") != null
                            ? rs.getInt("matched_submission_id") : -1);
                    return sd;
                }
            }
        }
        return null;
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

    // ── Notification methods ──────────────────────────────────────────────────

    public void createNotification(int userId, String userType, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, user_type, message) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, userType);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        }
    }

    public List<NotificationData> getNotificationsForUser(int userId, String userType) throws SQLException {
        String sql = "SELECT id, user_id, user_type, message, created_at, is_read " +
                     "FROM notifications WHERE user_id = ? AND user_type = ? ORDER BY created_at DESC";
        List<NotificationData> list = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, userType);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new NotificationData(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("user_type"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("is_read")
                    ));
                }
            }
        }
        return list;
    }

    public int countUnreadNotifications(int userId, String userType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND user_type = ? AND is_read = FALSE";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, userType);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public void markAllNotificationsRead(int userId, String userType) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND user_type = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, userType);
            pstmt.executeUpdate();
        }
    }

    /**
     * Creates TEACHER notifications for every assignment whose deadline has passed
     * but hasn't been flagged yet. Marks each asset as notified so it fires only once.
     */
    public void createDeadlineNotificationsForTeacher(int teacherId) throws SQLException {
        String sql = "SELECT ea.id, ea.title, g.name AS group_name " +
                     "FROM educational_assets ea " +
                     "JOIN groups g ON ea.group_id = g.id " +
                     "WHERE g.teacher_id = ? AND ea.deadline IS NOT NULL " +
                     "AND ea.deadline < CURRENT_TIMESTAMP AND ea.deadline_notified = FALSE";
        List<Integer> toMark = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teacherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    createNotification(teacherId, "TEACHER",
                        "Deadline passed for \"" + rs.getString("title") +
                        "\" in group \"" + rs.getString("group_name") +
                        "\". Submissions are ready for review.");
                    toMark.add(rs.getInt("id"));
                }
            }
        }
        if (!toMark.isEmpty()) {
            String update = "UPDATE educational_assets SET deadline_notified = TRUE WHERE id = ?";
            try (PreparedStatement pstmt = getConnection().prepareStatement(update)) {
                for (int assetId : toMark) {
                    pstmt.setInt(1, assetId);
                    pstmt.executeUpdate();
                }
            }
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
        private LocalDateTime deadline;
        
        public AssetData(int id, String title, String description, String filePath, String assetType, java.sql.Timestamp sharedAt, LocalDateTime deadline) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.filePath = filePath;
            this.assetType = assetType;
            this.sharedAt = sharedAt;
            this.deadline = deadline;
        }
        
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getFilePath() { return filePath; }
        public String getAssetType() { return assetType; }
        public java.sql.Timestamp getSharedAt() { return sharedAt; }
        public LocalDateTime getDeadline() { return deadline; }
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

    public static class NotificationData {
        private final int id;
        private final int userId;
        private final String userType;
        private final String message;
        private final LocalDateTime createdAt;
        private final boolean isRead;

        public NotificationData(int id, int userId, String userType, String message,
                                LocalDateTime createdAt, boolean isRead) {
            this.id = id;
            this.userId = userId;
            this.userType = userType;
            this.message = message;
            this.createdAt = createdAt;
            this.isRead = isRead;
        }

        public int getId()                  { return id; }
        public int getUserId()              { return userId; }
        public String getUserType()         { return userType; }
        public String getMessage()          { return message; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isRead()             { return isRead; }
    }

    public static class SubmissionData {
        private final int id;
        private final int assetId;
        private final int studentId;
        private final String studentName;
        private final String assignmentTitle;
        private final LocalDateTime assignmentDeadline;
        private final String filePath;
        private final LocalDateTime submissionTime;
        private final Integer grade;
        private final String feedback;
        private final boolean evaluated;
        private final LocalDateTime evaluationTime;

        // Plagiarism detection fields (mutable — set after construction by DB read methods)
        private boolean plagiarized = false;
        private double similarityScore = 0.0;
        private int matchedSubmissionId = -1;

        public SubmissionData(int id, int assetId, int studentId, String studentName,
                              String assignmentTitle,
                              LocalDateTime assignmentDeadline,
                              String filePath, LocalDateTime submissionTime,
                              Integer grade, String feedback, boolean evaluated,
                              LocalDateTime evaluationTime) {
            this.id = id;
            this.assetId = assetId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.assignmentTitle = assignmentTitle;
            this.assignmentDeadline = assignmentDeadline;
            this.filePath = filePath;
            this.submissionTime = submissionTime;
            this.grade = grade;
            this.feedback = feedback;
            this.evaluated = evaluated;
            this.evaluationTime = evaluationTime;
        }

        public int getId()                        { return id; }
        public int getAssetId()                   { return assetId; }
        public int getStudentId()                 { return studentId; }
        public String getStudentName()            { return studentName; }
        public String getAssignmentTitle()        { return assignmentTitle; }
        public LocalDateTime getAssignmentDeadline() { return assignmentDeadline; }
        public String getFilePath()               { return filePath; }
        public LocalDateTime getSubmissionTime()  { return submissionTime; }
        public Integer getGrade()                 { return grade; }
        public String getFeedback()               { return feedback; }
        public boolean isEvaluated()              { return evaluated; }
        public LocalDateTime getEvaluationTime()  { return evaluationTime; }
        public boolean isLate() {
            return assignmentDeadline != null && submissionTime != null
                && submissionTime.isAfter(assignmentDeadline);
        }

        // Plagiarism getters & setters
        public boolean isPlagiarized()            { return plagiarized; }
        public double getSimilarityScore()        { return similarityScore; }
        public int getMatchedSubmissionId()       { return matchedSubmissionId; }

        public void setPlagiarized(boolean v)           { plagiarized = v; }
        public void setSimilarityScore(double v)        { similarityScore = v; }
        public void setMatchedSubmissionId(int v)       { matchedSubmissionId = v; }
    }
}
