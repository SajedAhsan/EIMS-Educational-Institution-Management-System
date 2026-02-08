import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class StartPageController {

    @FXML
    private void teacherButton(ActionEvent event) {
        System.out.println("Teacher Button clicked");
        try {
            // Load the teacher login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Teacher/teacherLoginPage.fxml"));
            Parent root = loader.load();
            
            // Get the stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Set the new scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading teacher login page: " + e.getMessage());
        }
    }

    @FXML
    private void studentButton(ActionEvent event) {
        System.out.println("Student Button clicked");
        try {
            // Load the student login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Student/studentLoginPage.fxml"));
            Parent root = loader.load();
            
            // Get the stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Set the new scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading student login page: " + e.getMessage());
        }
    }
}
