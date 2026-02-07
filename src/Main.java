import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.File;
public class Main extends Application{
    @Override
    public void start(Stage stage) throws Exception {
        StackPane layout = new StackPane();
        Scene scene = new Scene(layout, 717, 460);
        stage.setScene(scene);
        stage.setTitle("EIMS Login");
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
