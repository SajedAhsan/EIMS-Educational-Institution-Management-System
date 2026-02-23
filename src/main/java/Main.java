import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("startPage.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("EIMS - Educational Institution Management System");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}



// run command
// javac --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -d bin src\*.java src\Student\*.java src\Teacher\*.java; Copy-Item -Path "src\startPage.fxml" -Destination "bin\" -Force; New-Item -ItemType Directory -Path "bin\Teacher" -Force; New-Item -ItemType Directory -Path "bin\Student" -Force; Copy-Item -Path "src\Teacher\*.fxml" -Destination "bin\Teacher\" -Force; Copy-Item -Path "src\Student\*.fxml" -Destination "bin\Student\" -Force; java --module-path "javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp bin Main