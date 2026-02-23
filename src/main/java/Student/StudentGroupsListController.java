package Student;

import java.io.IOException;
import java.sql.SQLException;
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

public class StudentGroupsListController {
    
    @FXML
    private ListView<GroupItem> groupsList;
    
    @FXML
    private Label statusLabel;
    
    private ObservableList<GroupItem> groups;
    private String studentEmail;
    private DatabaseManager dbManager;
    
    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        groups = FXCollections.observableArrayList();
        groupsList.setItems(groups);
    }
    
    public void setStudentEmail(String email) {
        this.studentEmail = email;
        loadGroups();
    }
    
    private void loadGroups() {
        try {
            groups.clear();
            int studentId = dbManager.getStudentIdByEmail(studentEmail);
            
            if (studentId == -1) {
                statusLabel.setText("Student not found");
                return;
            }
            
            List<DatabaseManager.GroupData> groupList = dbManager.getGroupsByStudent(studentId);
            
            for (DatabaseManager.GroupData data : groupList) {
                GroupItem group = new GroupItem(data.getId(), data.getName());
                groups.add(group);
            }
            
            if (groups.isEmpty()) {
                statusLabel.setText("No groups found. You haven't been added to any groups yet!");
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
        
        if (selected == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a group to view");
            return;
        }
        
        openGroupDetails(selected);
    }
    
    private void openGroupDetails(GroupItem group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentGroupDetails.fxml"));
            Parent root = loader.load();
            
            StudentGroupDetailsController controller = loader.getController();
            controller.setGroupId(group.getId());
            controller.setStudentEmail(studentEmail);
            
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentDashboard.fxml"));
            Parent root = loader.load();
            
            studentDashboardController controller = loader.getController();
            controller.setStudentEmail(studentEmail);
            
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
    
    // Inner class for displaying group items without timestamp
    public static class GroupItem {
        private int id;
        private String name;
        
        public GroupItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
