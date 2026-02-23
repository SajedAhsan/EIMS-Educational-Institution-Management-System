package main.java.Student;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.database.DatabaseManager;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class StudentGroupDetailsController {
    
    @FXML
    private Label groupNameLabel;
    
    @FXML
    private Label teacherNameLabel;
    
    @FXML
    private Label teacherEmailLabel;
    
    @FXML
    private ListView<String> membersList;
    
    @FXML
    private ListView<AssetItem> assetsList;
    
    @FXML
    private Label assetsStatusLabel;
    
    private int groupId;
    private String studentEmail;
    private DatabaseManager dbManager;
    private ObservableList<String> members;
    private ObservableList<AssetItem> assets;
    private List<DatabaseManager.AssetData> assetDataList;
    
    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        members = FXCollections.observableArrayList();
        assets = FXCollections.observableArrayList();
        assetDataList = new ArrayList<>();
        membersList.setItems(members);
        assetsList.setItems(assets);
    }
    
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    
    public void setStudentEmail(String email) {
        this.studentEmail = email;
        loadGroupDetails();
    }
    
    private void loadGroupDetails() {
        try {
            // Load group information including teacher details
            DatabaseManager.GroupDetailData groupData = dbManager.getGroupById(groupId);
            
            if (groupData != null) {
                groupNameLabel.setText(groupData.getName());
                teacherNameLabel.setText(groupData.getTeacherName());
                teacherEmailLabel.setText(groupData.getTeacherEmail());
                
                loadMembers();
                loadAssets();
            } else {
                showAlert(AlertType.ERROR, "Error", "Group not found");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load group details: " + e.getMessage());
        }
    }
    
    private void loadMembers() {
        try {
            members.clear();
            List<DatabaseManager.MemberData> memberList = dbManager.getGroupMembers(groupId);
            
            for (DatabaseManager.MemberData member : memberList) {
                members.add(member.getName() + " (" + member.getEmail() + ")");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load members: " + e.getMessage());
        }
    }
    
    private void loadAssets() {
        try {
            assets.clear();
            assetDataList.clear();
            List<DatabaseManager.AssetData> assetList = dbManager.getGroupAssets(groupId);
            
            if (assetList.isEmpty()) {
                assetsStatusLabel.setText("No materials shared yet");
                assetsStatusLabel.setVisible(true);
            } else {
                assetsStatusLabel.setVisible(false);
                assetDataList.addAll(assetList);
                for (DatabaseManager.AssetData asset : assetList) {
                    String description = asset.getDescription();
                    assets.add(new AssetItem(asset.getId(), asset.getTitle(), description, asset.getFilePath()));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load assets: " + e.getMessage());
        }
    }
    
    @FXML
    void handleAssetSelection(MouseEvent event) {
        if (event.getClickCount() == 2) {
            AssetItem selected = assetsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openAsset(selected);
            }
        }
    }
    
    @FXML
    void handleDownload(ActionEvent event) {
        AssetItem selected = assetsList.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a material to download");
            return;
        }
        
        downloadAsset(selected);
    }
    
    private void openAsset(AssetItem asset) {
        try {
            if (asset.getFilePath() == null || asset.getFilePath().isEmpty()) {
                showAlert(AlertType.WARNING, "No File", "This material has no file attached");
                return;
            }
            
            File file = new File(asset.getFilePath());
            if (!file.exists()) {
                showAlert(AlertType.ERROR, "File Not Found", "The file does not exist: " + asset.getFilePath());
                return;
            }
            
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert(AlertType.ERROR, "Error", "Cannot open file: Desktop operations not supported");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to open file: " + e.getMessage());
        }
    }
    
    private void downloadAsset(AssetItem asset) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        
        try {
            if (asset.getFilePath() == null || asset.getFilePath().isEmpty()) {
                showAlert(AlertType.WARNING, "No File", "This material has no file attached");
                return;
            }
            
            File sourceFile = new File(asset.getFilePath());
            if (!sourceFile.exists()) {
                showAlert(AlertType.ERROR, "File Not Found", "The file does not exist: " + asset.getFilePath());
                return;
            }
            
            // Open file chooser to let user select download location
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(sourceFile.getName());
            
            // Add extension filters based on file type
            String fileName = sourceFile.getName();
            String extension = "";
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1).toLowerCase();
            }
            
            switch (extension) {
                case "pdf":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                    break;
                case "doc":
                case "docx":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx"));
                    break;
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif"));
                    break;
                case "mp4":
                case "avi":
                case "mov":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov"));
                    break;
                default:
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            }
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            
            File destFile = fileChooser.showSaveDialog(assetsList.getScene().getWindow());
            
            if (destFile != null) {
                // Use streams for binary file copying to preserve file integrity
                fis = new FileInputStream(sourceFile);
                fos = new FileOutputStream(destFile);
                
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                
                fos.flush();
                
                showAlert(AlertType.INFORMATION, "Success", 
                    "File downloaded successfully!\n\nSaved to: " + destFile.getAbsolutePath() + 
                    "\nSize: " + String.format("%.2f", destFile.length() / 1024.0) + " KB");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Download Error", "Failed to download file: " + e.getMessage());
        } finally {
            // Close streams in finally block to ensure they're always closed
            try {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentGroupsList.fxml"));
            Parent root = loader.load();
            
            StudentGroupsListController controller = loader.getController();
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
    
    // Inner class for displaying asset items
    public static class AssetItem {
        private int id;
        private String title;
        private String description;
        private String filePath;
        
        public AssetItem(int id, String title, String description, String filePath) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.filePath = filePath;
        }
        
        public int getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        @Override
        public String toString() {
            String display = "📄 " + title;
            if (description != null && !description.isEmpty()) {
                display += " - " + description;
            }
            return display;
        }
    }
}
