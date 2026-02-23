package main.java.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticationService {
    
    private DatabaseManager dbManager;
    
    public AuthenticationService() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Authenticate a teacher
     * @param email Teacher's email
     * @param password Teacher's password
     * @return true if authentication successful, false otherwise
     */
    public boolean authenticateTeacher(String email, String password) {
        String query = "SELECT * FROM teachers WHERE email = ? AND password = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true if a matching record is found
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Teacher authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate a student
     * @param email Student's email
     * @param password Student's password
     * @return true if authentication successful, false otherwise
     */
    public boolean authenticateStudent(String email, String password) {
        String query = "SELECT * FROM students WHERE email = ? AND password = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true if a matching record is found
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Student authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get teacher details by email
     * @param email Teacher's email
     * @return Teacher name or null if not found
     */
    public String getTeacherName(String email) {
        String query = "SELECT name FROM teachers WHERE email = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get student details by email
     * @param email Student's email
     * @return Student name or null if not found
     */
    public String getStudentName(String email) {
        String query = "SELECT name FROM students WHERE email = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
