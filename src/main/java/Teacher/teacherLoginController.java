package Teacher;

import database.AuthenticationService;
import java.io.IOException;
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

public class teacherLoginController {

    @FXML
    private Button teacherbackButton;

    @FXML
    private TextField teacherEmail;

    @FXML
    private Button teacherLoginButton;

    @FXML
    private PasswordField teacherPass;
    
    private AuthenticationService authService;
    
    public teacherLoginController() {
        authService = new AuthenticationService();
    }

    @FXML
    void teacherBackButton(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("../startPage.fxml"));

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
    
    @FXML
    void teacherLogin(ActionEvent event) {
        String email = teacherEmail.getText().trim();
        String password = teacherPass.getText();
        
        if (email.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Login Error", "Please fill in all fields");
            return;
        }
        
        if (authService.authenticateTeacher(email, password)) {
            String teacherName = authService.getTeacherName(email);
            showAlert(AlertType.INFORMATION, "Login Successful", 
                     "Welcome, " + (teacherName != null ? teacherName : "Teacher") + "!");
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherDashboard.fxml"));
                Parent root = loader.load();
                
                // Pass the teacher email to the dashboard controller
                teacherDashboardController controller = loader.getController();
                controller.setTeacherEmail(email);
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Navigation Error", 
                         "Failed to load Teacher Dashboard: " + e.getMessage());
            }
        } else {
            showAlert(AlertType.ERROR, "Login Failed", "Invalid email or password");
            teacherPass.clear();
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
