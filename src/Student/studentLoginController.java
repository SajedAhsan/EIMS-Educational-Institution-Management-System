import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class studentLoginController {

    @FXML
    private TextField studentEmail;

    @FXML
    private Button studentLoginButton;

    @FXML
    private PasswordField studentPass;

    @FXML
    void studentLogin(ActionEvent event) {
        // Get the entered credentials
        String email = studentEmail.getText();
        String password = studentPass.getText();
        
        // TODO: Add authentication logic here
        System.out.println("Student Login attempt with email: " + email);
        
        // For now, just checking if fields are not empty
        if (email.isEmpty() || password.isEmpty()) {
            System.out.println("Please fill in all fields");
        } else {
            System.out.println("Login successful! Navigating to Student Dashboard...");
            // TODO: Navigate to Student Dashboard
        }
    }

}
