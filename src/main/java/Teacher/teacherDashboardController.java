package Teacher;

import database.DatabaseManager;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class teacherDashboardController {
    
    private String teacherEmail;
    private int teacherId;
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label notificationBadgeLabel;
    @FXML private ListView<String> notificationsList;
    @FXML private VBox notificationsPanel;

    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
        try {
            this.teacherId = dbManager.getTeacherIdByEmail(email);
            // Create notifications for any assignments whose deadline just passed
            dbManager.createDeadlineNotificationsForTeacher(teacherId);
            loadNotifications();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadNotifications() throws SQLException {
        List<DatabaseManager.NotificationData> notifications =
            dbManager.getNotificationsForUser(teacherId, "TEACHER");
        int unread = dbManager.countUnreadNotifications(teacherId, "TEACHER");

        if (notificationBadgeLabel != null) {
            if (unread > 0) {
                notificationBadgeLabel.setText(String.valueOf(unread));
                notificationBadgeLabel.setVisible(true);
            } else {
                notificationBadgeLabel.setVisible(false);
            }
        }

        if (notificationsList != null) {
            notificationsList.getItems().clear();
            for (DatabaseManager.NotificationData n : notifications) {
                String prefix = n.isRead() ? "  " : "🔵 ";
                notificationsList.getItems().add(
                    prefix + "[" + n.getCreatedAt().format(DT_FMT) + "] " + n.getMessage());
            }
        }
    }

    @FXML
    void handleNotificationsButton(ActionEvent event) {
        if (notificationsPanel != null) {
            boolean visible = !notificationsPanel.isVisible();
            notificationsPanel.setVisible(visible);
            notificationsPanel.setManaged(visible);
            if (visible) {
                // Mark all as read when panel is opened
                try {
                    dbManager.markAllNotificationsRead(teacherId, "TEACHER");
                    loadNotifications();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
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
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Logout");
            alert.setHeaderText(null);
            alert.setContentText("You have been logged out successfully!");
            alert.showAndWait();
            
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

