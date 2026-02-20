package Teacher;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class teacherDashboardController {
    
    private String teacherEmail;
    
    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
    }
    
    @FXML
    void handleProfileButton(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Profile", "Profile feature coming soon!");
    }
    
    @FXML
    void handleMarkingButton(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Marking", "Marking feature coming soon!");
    }
    
    @FXML
    void handleGroupsButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GroupsList.fxml"));
            Parent root = loader.load();
            
            GroupsListController controller = loader.getController();
            controller.setTeacherEmail(teacherEmail);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to load groups: " + e.getMessage());
        }
    }
    
    @FXML
    void handleCreateGroupButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CreateGroup.fxml"));
            Parent root = loader.load();
            
            CreateGroupController controller = loader.getController();
            controller.setTeacherEmail(teacherEmail);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to load create group page: " + e.getMessage());
        }
    }
    
    @FXML
    void logout(ActionEvent event) {
        try {
            // Show confirmation
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Logout");
            alert.setHeaderText(null);
            alert.setContentText("You have been logged out successfully!");
            alert.showAndWait();
            
            // Navigate back to start page
            Parent root = FXMLLoader.load(getClass().getResource("../startPage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to logout: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
