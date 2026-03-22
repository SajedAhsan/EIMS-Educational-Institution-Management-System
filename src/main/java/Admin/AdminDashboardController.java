package Admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import database.DatabaseManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class AdminDashboardController {
    
    @FXML
    private TableView<DatabaseManager.StudentData> studentsTable;
    
    @FXML
    private TableColumn<DatabaseManager.StudentData, Integer> studentIdColumn;
    
    @FXML
    private TableColumn<DatabaseManager.StudentData, String> studentNameColumn;
    
    @FXML
    private TableColumn<DatabaseManager.StudentData, String> studentEmailColumn;
    
    @FXML
    private TableView<DatabaseManager.TeacherBasicData> teachersTable;
    
    @FXML
    private TableColumn<DatabaseManager.TeacherBasicData, Integer> teacherIdColumn;
    
    @FXML
    private TableColumn<DatabaseManager.TeacherBasicData, String> teacherNameColumn;
    
    @FXML
    private TableColumn<DatabaseManager.TeacherBasicData, String> teacherEmailColumn;
    
    @FXML
    private TableColumn<DatabaseManager.TeacherBasicData, String> teacherSubjectColumn;
    
    @FXML
    private Button addStudentButton;
    
    @FXML
    private Button deleteStudentButton;
    
    @FXML
    private Button refreshStudentsButton;
    
    @FXML
    private Button addTeacherButton;
    
    @FXML
    private Button deleteTeacherButton;
    
    @FXML
    private Button refreshTeachersButton;
    
    @FXML
    private Button logoutButton;
    
    private DatabaseManager db;
    
    @FXML
    public void initialize() {
        try {
            db = DatabaseManager.getInstance();
            
            // Setup student table columns
            studentIdColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
            studentNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
            studentEmailColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
            
            // Setup teacher table columns
            teacherIdColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
            teacherNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
            teacherEmailColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
            teacherSubjectColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubject()));
            
            // Button handlers
            addStudentButton.setOnAction(e -> handleAddStudent());
            deleteStudentButton.setOnAction(e -> handleDeleteStudent());
            refreshStudentsButton.setOnAction(e -> loadStudents());
            
            addTeacherButton.setOnAction(e -> handleAddTeacher());
            deleteTeacherButton.setOnAction(e -> handleDeleteTeacher());
            refreshTeachersButton.setOnAction(e -> loadTeachers());
            
            logoutButton.setOnAction(e -> handleLogout());
            
            // Load initial data
            loadStudents();
            loadTeachers();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Initialization Error", "Failed to initialize admin dashboard: " + e.getMessage());
        }
    }
    
    private void loadStudents() {
        try {
            var students = db.getAllStudents();
            studentsTable.getItems().clear();
            studentsTable.getItems().addAll(students);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load students: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadTeachers() {
        try {
            var teachers = db.getAllTeachers();
            teachersTable.getItems().clear();
            teachersTable.getItems().addAll(teachers);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load teachers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAddStudent() {
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Student");
        dialog.setHeaderText("Enter Student Details");
        
        VBox dialogPane = new VBox(10);
        dialogPane.setPrefWidth(300);
        
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        
        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();
        
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        
        dialogPane.getChildren().addAll(
            nameLabel, nameField,
            emailLabel, emailField,
            passwordLabel, passwordField
        );
        
        dialog.getDialogPane().setContent(dialogPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAlert("Error", "All fields are required");
                return;
            }
            
            try {
                int studentId = db.addStudent(email, password, name);
                if (studentId > 0) {
                    showAlert("Success", "Student added successfully with ID: " + studentId);
                    loadStudents();
                } else {
                    showAlert("Error", "Failed to add student");
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to add student: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleDeleteStudent() {
        DatabaseManager.StudentData selected = studentsTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showAlert("Warning", "Please select a student to delete");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Student");
        confirmAlert.setContentText("Are you sure you want to delete student \"" + selected.getName() + "\"?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = db.deleteStudent(selected.getId());
                if (deleted) {
                    showAlert("Success", "Student deleted successfully");
                    loadStudents();
                } else {
                    showAlert("Error", "Failed to delete student");
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete student: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleAddTeacher() {
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Teacher");
        dialog.setHeaderText("Enter Teacher Details");
        
        VBox dialogPane = new VBox(10);
        dialogPane.setPrefWidth(300);
        
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        
        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();
        
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        
        Label subjectLabel = new Label("Subject:");
        TextField subjectField = new TextField();
        
        dialogPane.getChildren().addAll(
            nameLabel, nameField,
            emailLabel, emailField,
            passwordLabel, passwordField,
            subjectLabel, subjectField
        );
        
        dialog.getDialogPane().setContent(dialogPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String subject = subjectField.getText();
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || subject.isEmpty()) {
                showAlert("Error", "All fields are required");
                return;
            }
            
            try {
                int teacherId = db.addTeacher(email, password, name, subject);
                if (teacherId > 0) {
                    showAlert("Success", "Teacher added successfully with ID: " + teacherId);
                    loadTeachers();
                } else {
                    showAlert("Error", "Failed to add teacher");
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to add teacher: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleDeleteTeacher() {
        DatabaseManager.TeacherBasicData selected = teachersTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showAlert("Warning", "Please select a teacher to delete");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Teacher");
        confirmAlert.setContentText("Are you sure you want to delete teacher \"" + selected.getName() + "\"?\nAll related groups and their data will be deleted.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = db.deleteTeacher(selected.getId());
                if (deleted) {
                    showAlert("Success", "Teacher deleted successfully");
                    loadTeachers();
                } else {
                    showAlert("Error", "Failed to delete teacher");
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete teacher: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleLogout() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/startPage.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 700);
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("EIMS - Start Page");
            stage.setWidth(900);
            stage.setHeight(700);
            stage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to start page");
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
