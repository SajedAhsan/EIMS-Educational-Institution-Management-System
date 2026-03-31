package Admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import database.AuthenticationService;

import java.io.IOException;

public class AdminLoginController {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label errorLabel;
    
    private AuthenticationService authService;
    
    public AdminLoginController() {
        authService = new AuthenticationService();
    }
    
    @FXML
    public void initialize() {
        
        loginButton.setOnAction(e -> handleAdminLogin());
        backButton.setOnAction(e -> handleBackToHome());
    }
    
    private void handleAdminLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password");
            return;
        }
        
        if (authService.authenticateAdmin(username, password)) {
            errorLabel.setText("");
            try {
                // Load AdminDashboard
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Admin/AdminDashboard.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
                
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("EIMS - Admin Dashboard");
                stage.setWidth(1200);
                stage.setHeight(800);
                stage.setResizable(false);
            } catch (IOException ex) {
                ex.printStackTrace();
                errorLabel.setText("Error loading admin dashboard");
            }
        } else {
            errorLabel.setText("Invalid admin credentials");
            passwordField.clear();
        }
    }
    
    private void handleBackToHome() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/startPage.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("EIMS - Start Page");
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.setResizable(false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
