package Teacher;

import database.DatabaseManager;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class GroupDetailsController {
    
    @FXML
    private Label groupNameLabel;
    
    @FXML
    private ListView<String> membersList;
    
    @FXML
    private TextField assetTitleField;
    
    @FXML
    private TextArea assetDescriptionArea;
    
    @FXML
    private TextField assetFilePathField;
    
    @FXML
    private TextField assetTypeField;
    
    @FXML
    private ListView<String> sharedAssetsList;
    
    @FXML
    private Label statusLabel;
    
    private ObservableList<String> members;
    private ObservableList<String> assets;
    private int groupId;
    private String teacherEmail;
    private DatabaseManager dbManager;
    
    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        members = FXCollections.observableArrayList();
        assets = FXCollections.observableArrayList();
        
        membersList.setItems(members);
        sharedAssetsList.setItems(assets);
    }
    
    public void setGroupId(int groupId) {
        this.groupId = groupId;
        // Don't load immediately, wait for setTeacherEmail
    }
    
    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
        // Now that both are set, load the details
        loadGroupDetails();
    }
    
    private void loadGroupDetails() {
        try {
            System.out.println("Loading group details for groupId: " + groupId);
            
            // Load group info
            DatabaseManager.GroupDetailData groupData = dbManager.getGroupById(groupId);
            if (groupData != null) {
                String groupName = groupData.getName();
                groupNameLabel.setText(groupName);
                System.out.println("Group name: " + groupName);
            } else {
                System.out.println("Group not found!");
                showAlert(AlertType.ERROR, "Error", "Group not found in database");
                return;
            }
            
            // Load members
            loadMembers();
            
            // Load assets
            loadAssets();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load group details: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Unexpected Error", "An error occurred: " + e.getMessage());
        }
    }
    
    private void loadMembers() {
        try {
            members.clear();
            List<DatabaseManager.MemberData> memberList = dbManager.getGroupMembers(groupId);
            
            System.out.println("Loading members for group " + groupId + ": Found " + memberList.size() + " members");
            
            for (DatabaseManager.MemberData data : memberList) {
                members.add(data.getName() + " (" + data.getEmail() + ")");
            }
            
            if (members.isEmpty()) {
                statusLabel.setText("No members in this group");
            } else {
                statusLabel.setText(members.size() + " member(s) in group");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load members: " + e.getMessage());
        }
    }
    
    private void loadAssets() {
        try {
            assets.clear();
            List<DatabaseManager.AssetData> assetList = dbManager.getGroupAssets(groupId);
            
            System.out.println("Loading assets for group " + groupId + ": Found " + assetList.size() + " assets");
            
            for (DatabaseManager.AssetData data : assetList) {
                assets.add(data.getTitle() + " [" + data.getAssetType() + "]");
            }
            
            if (assets.isEmpty()) {
                statusLabel.setText("No assets shared yet");
            } else {
                statusLabel.setText(assets.size() + " asset(s) shared");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load assets: " + e.getMessage());
        }
    }
    
    @FXML
    void handleBrowseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Educational Asset");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov")
        );
        
        Stage stage = (Stage) assetFilePathField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            assetFilePathField.setText(selectedFile.getAbsolutePath());
            
            // Auto-detect file type
            String fileName = selectedFile.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
            assetTypeField.setText(extension);
        }
    }
    
    @FXML
    void handleAddAsset(ActionEvent event) {
        String title = assetTitleField.getText().trim();
        String description = assetDescriptionArea.getText().trim();
        String filePath = assetFilePathField.getText().trim();
        String assetType = assetTypeField.getText().trim();
        
        if (title.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter an asset title");
            return;
        }
        
        if (filePath.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter a file path or URL");
            return;
        }
        
        if (assetType.isEmpty()) {
            assetType = "File";
        }
        
        try {
            dbManager.addEducationalAsset(groupId, title, description, filePath, assetType);
            
            showAlert(AlertType.INFORMATION, "Success", "Educational asset shared successfully!");
            
            // Clear fields
            assetTitleField.clear();
            assetDescriptionArea.clear();
            assetFilePathField.clear();
            assetTypeField.clear();
            
            // Reload assets
            loadAssets();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to share asset: " + e.getMessage());
        }
    }
    
    @FXML
    void handleRefreshAssets(ActionEvent event) {
        loadAssets();
    }
    
    @FXML
    void handleBack(ActionEvent event) {
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
}
