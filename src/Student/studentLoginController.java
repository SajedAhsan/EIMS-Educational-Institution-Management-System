package Student;

import java.io.IOException;

import database.AuthenticationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class studentLoginController {

    @FXML
    private Button backButton;

    @FXML
    private TextField studentEmail;

    @FXML
    private Button studentLoginButton;

    @FXML
    private PasswordField studentPass;
    
    private AuthenticationService authService;
    
    public studentLoginController() {
        authService = new AuthenticationService();
    }

    @FXML
    void stdBackButton(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/startPage.fxml"));

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    void studentLogin(ActionEvent event) {
        String email = studentEmail.getText().trim();
        String password = studentPass.getText();
        
        if (email.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Login Error", "Please fill in all fields");
            return;
        }
        
        if (authService.authenticateStudent(email, password)) {
            String studentName = authService.getStudentName(email);
            showAlert(AlertType.INFORMATION, "Login Successful", 
                     "Welcome, " + (studentName != null ? studentName : "Student") + "!");
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Student/StudentDashboard.fxml"));
                Parent root = loader.load();
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Navigation Error", 
                         "Failed to load Student Dashboard: " + e.getMessage());
            }
        } else {
            showAlert(AlertType.ERROR, "Login Failed", "Invalid email or password");
            studentPass.clear();
        }
    }
    
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


