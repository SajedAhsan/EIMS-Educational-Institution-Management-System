import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class StartPageController {

    @FXML
    private void teacherButton(ActionEvent event) {
        System.out.println("Teacher Button clicked");
        // TODO: Navigate to Teacher login
    }

    @FXML
    private void studentButton(ActionEvent event) {
        System.out.println("Student Button clicked");
        // TODO: Navigate to Student login
    }
}
