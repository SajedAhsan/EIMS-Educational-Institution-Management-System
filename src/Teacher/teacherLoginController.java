import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class teacherLoginController {

    @FXML
    private TextField teacherEmail;

    @FXML
    private Button teacherLoginButton;

    @FXML
    private PasswordField teacherPass;

    @FXML
    void teacherLogin(ActionEvent event) {
        // Get the entered credentials
        String email = teacherEmail.getText();
        String password = teacherPass.getText();
        
        // TODO: Add authentication logic here
        System.out.println("Teacher Login attempt with email: " + email);
        
        // For now, just checking if fields are not empty
        if (email.isEmpty() || password.isEmpty()) {
            System.out.println("Please fill in all fields");
        } else {
            System.out.println("Login successful! Navigating to Teacher Dashboard...");
            // TODO: Navigate to Teacher Dashboard
        }
    }

}
