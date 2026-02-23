package Teacher;

import database.DatabaseManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateGroupController {
    
    @FXML
    private TextField groupNameField;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ListView<StudentItem> availableStudentsList;
    
    @FXML
    private ListView<StudentItem> selectedStudentsList;
    
    @FXML
    private Label statusLabel;
    
    private ObservableList<StudentItem> availableStudents;
    private ObservableList<StudentItem> selectedStudents;
    private String teacherEmail;
    private DatabaseManager dbManager;
    
    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        availableStudents = FXCollections.observableArrayList();
        selectedStudents = FXCollections.observableArrayList();
        
        availableStudentsList.setItems(availableStudents);
        selectedStudentsList.setItems(selectedStudents);
        
        // Load all students initially
        loadAllStudents();
    }
    
    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
    }
    
    private void loadAllStudents() {
        try {
            // Search with empty string to get all students
            List<DatabaseManager.StudentData> results = dbManager.searchStudentsByEmail("");
            
            System.out.println("Loading all students - Found: " + results.size());
            
            for (DatabaseManager.StudentData data : results) {
                StudentItem student = new StudentItem(
                    data.getId(),
                    data.getEmail(),
                    data.getName()
                );
                availableStudents.add(student);
                System.out.println("  - " + data.getName() + " (" + data.getEmail() + ")");            }
            
            if (availableStudents.isEmpty()) {
                statusLabel.setText("No students in database");
            } else {
                statusLabel.setText(availableStudents.size() + " student(s) available");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading students");
        }
    }
    
    @FXML
    void handleSearchStudents(ActionEvent event) {
        String searchQuery = searchField.getText().trim();
        
        if (searchQuery.isEmpty()) {
            statusLabel.setText("Please enter a search term");
            return;
        }
        
        try {
            availableStudents.clear();
            List<DatabaseManager.StudentData> results = dbManager.searchStudentsByEmail(searchQuery);
            
            System.out.println("Search query: '" + searchQuery + "' - Found " + results.size() + " students");
            
            for (DatabaseManager.StudentData data : results) {
                System.out.println("  - " + data.getName() + " (" + data.getEmail() + ")");
                
                StudentItem student = new StudentItem(
                    data.getId(),
                    data.getEmail(),
                    data.getName()
                );
                
                // Only add if not already selected
                if (!isStudentSelected(student.getId())) {
                    availableStudents.add(student);
                }
            }
            
            if (availableStudents.isEmpty()) {
                statusLabel.setText("No students found");
            } else {
                statusLabel.setText(availableStudents.size() + " student(s) found");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Search Error", "Failed to search students: " + e.getMessage());
        }
    }
    
    @FXML
    void handleAddStudent(ActionEvent event) {
        StudentItem selected = availableStudentsList.getSelectionModel().getSelectedItem();
        
        if (selected != null) {
            selectedStudents.add(selected);
            availableStudents.remove(selected);
            statusLabel.setText("Student added to selection");
        }
    }
    
    @FXML
    void handleRemoveStudent(ActionEvent event) {
        StudentItem selected = selectedStudentsList.getSelectionModel().getSelectedItem();
        
        if (selected != null) {
            availableStudents.add(selected);
            selectedStudents.remove(selected);
            statusLabel.setText("Student removed from selection");
        }
    }
    
    @FXML
    void handleCreateGroup(ActionEvent event) {
        String groupName = groupNameField.getText().trim();
        
        if (groupName.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter a group name");
            return;
        }
        
        if (selectedStudents.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please select at least one student");
            return;
        }
        
        try {
            // Get teacher ID
            int teacherId = dbManager.getTeacherIdByEmail(teacherEmail);
            
            if (teacherId == -1) {
                showAlert(AlertType.ERROR, "Error", "Teacher not found");
                return;
            }
            
            // Create group
            int groupId = dbManager.createGroup(groupName, teacherId);
            
            if (groupId == -1) {
                showAlert(AlertType.ERROR, "Error", "Failed to create group");
                return;
            }
            
            // Add students to group
            for (StudentItem student : selectedStudents) {
                dbManager.addStudentToGroup(groupId, student.getId());
            }
            
            // Show success message
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Group '" + groupName + "' created successfully with " + selectedStudents.size() + " student(s)!");
            alert.showAndWait();
            
            // Navigate to groups list to show the newly created group
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("GroupsList.fxml"));
                Parent root = loader.load();
                
                GroupsListController controller = loader.getController();
                controller.setTeacherEmail(teacherEmail);
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
                handleBack(event);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to create group: " + e.getMessage());
        }
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherDashboard.fxml"));
            Parent root = loader.load();
            
            teacherDashboardController controller = loader.getController();
            controller.setTeacherEmail(teacherEmail);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to go back: " + e.getMessage());
        }
    }
    
    private boolean isStudentSelected(int studentId) {
        for (StudentItem student : selectedStudents) {
            if (student.getId() == studentId) {
                return true;
            }
        }
        return false;
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class to represent a student
    public static class StudentItem {
        private int id;
        private String email;
        private String name;
        
        public StudentItem(int id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }
        
        public int getId() {
            return id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name + " (" + email + ")";
        }
    }
}
