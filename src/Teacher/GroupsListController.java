package Teacher;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import database.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class GroupsListController {
    
    @FXML
    private ListView<GroupItem> groupsList;
    
    @FXML
    private Label statusLabel;
    
    private ObservableList<GroupItem> groups;
    private String teacherEmail;
    private DatabaseManager dbManager;
    
    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        groups = FXCollections.observableArrayList();
        groupsList.setItems(groups);
    }
    
    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
        loadGroups();
    }
    
    private void loadGroups() {
        try {
            groups.clear();
            int teacherId = dbManager.getTeacherIdByEmail(teacherEmail);
            
            if (teacherId == -1) {
                statusLabel.setText("Teacher not found");
                return;
            }
            
            List<DatabaseManager.GroupData> groupList = dbManager.getGroupsByTeacher(teacherId);
            
            for (DatabaseManager.GroupData data : groupList) {
                GroupItem group = new GroupItem(data.getId(), data.getName(), data.getCreatedAt());
                groups.add(group);
            }
            
            if (groups.isEmpty()) {
                statusLabel.setText("No groups found. Create a new group to get started!");
            } else {
                statusLabel.setText(groups.size() + " group(s) found");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load groups: " + e.getMessage());
        }
    }
    
    @FXML
    void handleGroupSelection(MouseEvent event) {
        if (event.getClickCount() == 2) {
            GroupItem selected = groupsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openGroupDetails(selected);
            }
        }
    }
    
    @FXML
    void handleViewGroup(ActionEvent event) {
        GroupItem selected = groupsList.getSelectionModel().getSelectedItem();
        
        System.out.println("View Group button clicked");
        
        if (selected == null) {
            System.out.println("No group selected");
            showAlert(AlertType.WARNING, "No Selection", "Please select a group to view");
            return;
        }
        
        System.out.println("Opening group: " + selected.getName() + " (ID: " + selected.getId() + ")");
        openGroupDetails(selected);
    }
    
    private void openGroupDetails(GroupItem group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GroupDetails.fxml"));
            Parent root = loader.load();
            
            GroupDetailsController controller = loader.getController();
            // Set both values before triggering the load
            controller.setGroupId(group.getId());
            controller.setTeacherEmail(teacherEmail);
            
            Stage stage = (Stage) groupsList.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to open group details: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Unexpected error: " + e.getMessage());
        }
    }
    
    @FXML
    void handleRefresh(ActionEvent event) {
        loadGroups();
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherDashboard.fxml"));
            Parent root = loader.load();
            
            teacherDashboardController controller = loader.getController();
            controller.setTeacherEmail(teacherEmail);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to go back: " + e.getMessage());
        }
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class to represent a group
    public static class GroupItem {
        private int id;
        private String name;
        private Timestamp createdAt;
        
        public GroupItem(int id, String name, Timestamp createdAt) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
        }
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public Timestamp getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
