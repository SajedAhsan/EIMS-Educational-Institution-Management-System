package Teacher;

import database.DatabaseManager;
import database.DatabaseManager.TeacherProfileData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class TeacherProfileController {

    // ── Profile labels ─────────────────────────────────────────────────────
    @FXML private Label labelName;
    @FXML private Label labelEmail;
    @FXML private Label labelRole;
    @FXML private Label labelDepartment;
    @FXML private Label labelSubjects;
    @FXML private Label labelContact;
    @FXML private Label labelYearsExperience;
    @FXML private Label labelQualification;

    // ── Photo ──────────────────────────────────────────────────────────────
    @FXML private ImageView photoView;
    @FXML private Label     photoLabel;

    private String teacherEmail;
    private TeacherProfileData profileData;
    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Public API ─────────────────────────────────────────────────────────

    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
        loadProfile();
    }

    // ── Load / display ─────────────────────────────────────────────────────

    private void loadProfile() {
        try {
            profileData = db.getTeacherProfile(teacherEmail);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Could not load profile: " + e.getMessage());
            return;
        }

        if (profileData == null) {
            showAlert(AlertType.WARNING, "Profile", "No profile found for this account.");
            return;
        }

        displayProfile(profileData);
    }

    private void displayProfile(TeacherProfileData p) {
        labelName.setText(orDummy(p.getName(), "Dummy Name"));
        labelEmail.setText(orDummy(p.getEmail(), "Dummy X"));
        labelRole.setText(orDummy(p.getSubject(), "Dummy X"));
        labelDepartment.setText(orDummy(p.getDepartmentFaculty(), "Dummy X"));
        labelSubjects.setText(orDummy(p.getSubject(), "Dummy X"));
        labelContact.setText(orDummy(p.getPhoneNumber(), "Dummy X"));
        labelYearsExperience.setText(p.getYearsOfExperience() > 0 
            ? String.valueOf(p.getYearsOfExperience()) : "Dummy X");
        labelQualification.setText(orDummy(p.getHighestDegreeQualification(), "Dummy X"));

        // Load saved photo if present
        String path = p.getPhotoPath();
        if (path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) {
                photoView.setImage(new Image(f.toURI().toString()));
                photoLabel.setVisible(false);
                return;
            }
        }
        photoView.setImage(null);
        photoLabel.setVisible(true);
    }

    private static String orDummy(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    // ── Photo click ────────────────────────────────────────────────────────

    @FXML
    void handlePhotoClick(MouseEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Photo");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JPG Images", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) photoView.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        Image img = new Image(file.toURI().toString());
        photoView.setImage(img);
        photoLabel.setVisible(false);

        // Persist path immediately
        if (profileData != null) {
            profileData.setPhotoPath(file.getAbsolutePath());
            try {
                db.updateTeacherProfile(teacherEmail, profileData);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ── Back button ────────────────────────────────────────────────────────

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherDashboard.fxml"));
            Parent root = loader.load();
            teacherDashboardController ctrl = loader.getController();
            ctrl.setTeacherEmail(teacherEmail);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    // ── Edit dialog ────────────────────────────────────────────────────────

    @FXML
    void handleEdit(ActionEvent event) {
        if (profileData == null) return;

        // ── build form ─────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        ColumnConstraints labelCol = new ColumnConstraints(200);
        ColumnConstraints fieldCol = new ColumnConstraints(300);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        // Field helper
        TextField fName              = makeField(profileData.getName());
        TextField fEmail             = makeField(profileData.getEmail());
        fEmail.setDisable(true);  // Email should not be editable
        TextField fSubject           = makeField(profileData.getSubject());
        TextField fPhoneNumber       = makeField(profileData.getPhoneNumber());
        TextField fDepartment        = makeField(profileData.getDepartmentFaculty());
        TextField fYearsExperience   = makeField(String.valueOf(profileData.getYearsOfExperience()));
        TextField fQualification     = makeField(profileData.getHighestDegreeQualification());

        Object[][] rows = {
            {"Full Name",                        fName},
            {"Email Address",                    fEmail},
            {"Role / Position",                  fSubject},
            {"Department / Faculty",             fDepartment},
            {"Subjects / Courses Taught",        fSubject},
            {"Contact Information",              fPhoneNumber},
            {"Years of Experience",              fYearsExperience},
            {"Highest Degree / Qualification",   fQualification}
        };

        for (int i = 0; i < rows.length; i++) {
            grid.add(new Label((String) rows[i][0]), 0, i);
            grid.add((Node) rows[i][1], 1, i);
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(360);

        Button btnSave   = new Button("Save Changes");
        Button btnCancel = new Button("Cancel");
        btnSave.setStyle("-fx-background-color: #0088ff; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCancel.setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold;");

        HBox buttons = new HBox(12, btnCancel, btnSave);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 20, 10, 20));

        VBox root = new VBox(scroll, buttons);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Edit Profile");
        dialog.setScene(new Scene(root));
        dialog.setResizable(false);

        btnCancel.setOnAction(e -> dialog.close());
        btnSave.setOnAction(e -> {
            profileData.setName(fName.getText().trim());
            profileData.setSubject(fSubject.getText().trim());
            profileData.setPhoneNumber(fPhoneNumber.getText().trim());
            profileData.setDepartmentFaculty(fDepartment.getText().trim());
            try {
                int years = Integer.parseInt(fYearsExperience.getText().trim());
                profileData.setYearsOfExperience(years);
            } catch (NumberFormatException ex) {
                profileData.setYearsOfExperience(0);
            }
            profileData.setHighestDegreeQualification(fQualification.getText().trim());

            try {
                db.updateTeacherProfile(teacherEmail, profileData);
                displayProfile(profileData);
                dialog.close();
                showAlert(AlertType.INFORMATION, "Success", "Profile information saved");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(AlertType.ERROR, "Save Failed", "Could not save profile: " + ex.getMessage());
            }
        });

        dialog.showAndWait();
    }

    // ── Change password dialog ─────────────────────────────────────────────

    @FXML
    void handleChangePassword(ActionEvent event) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(24));

        PasswordField fOld     = new PasswordField();  fOld.setPromptText("Current password");
        PasswordField fNew     = new PasswordField();  fNew.setPromptText("New password (min 6 chars)");
        PasswordField fConfirm = new PasswordField();  fConfirm.setPromptText("Confirm new password");
        fOld.setPrefWidth(230);
        fNew.setPrefWidth(230);
        fConfirm.setPrefWidth(230);

        grid.add(new Label("Current Password:"), 0, 0);  grid.add(fOld,     1, 0);
        grid.add(new Label("New Password:"),     0, 1);  grid.add(fNew,     1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);  grid.add(fConfirm, 1, 2);

        Button btnChange = new Button("Change Password");
        Button btnCancel = new Button("Cancel");
        btnChange.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCancel.setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold;");

        HBox buttons = new HBox(12, btnCancel, btnChange);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 20, 12, 20));

        VBox root = new VBox(new Label("  "), grid, buttons);
        Label title = new Label("Change Password");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-padding: 14 20 0 20;");
        root.getChildren().add(0, title);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Change Password");
        dialog.setScene(new Scene(root));
        dialog.setResizable(false);

        btnCancel.setOnAction(e -> dialog.close());
        btnChange.setOnAction(e -> {
            String oldPwd  = fOld.getText();
            String newPwd  = fNew.getText();
            String confirm = fConfirm.getText();

            if (oldPwd.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
                showAlert(AlertType.WARNING, "Validation", "All fields are required.");
                return;
            }
            if (newPwd.length() < 6) {
                showAlert(AlertType.WARNING, "Validation", "New password must be at least 6 characters.");
                return;
            }
            if (!newPwd.equals(confirm)) {
                showAlert(AlertType.WARNING, "Validation", "New password and confirmation do not match.");
                return;
            }
            try {
                boolean ok = db.updateTeacherPassword(teacherEmail, oldPwd, newPwd);
                if (ok) {
                    dialog.close();
                    showAlert(AlertType.INFORMATION, "Success", "Password changed successfully.");
                } else {
                    showAlert(AlertType.ERROR, "Wrong Password", "Current password is incorrect.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(AlertType.ERROR, "Error", "Could not update password: " + ex.getMessage());
            }
        });

        dialog.showAndWait();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static TextField makeField(String value) {
        TextField tf = new TextField(value != null ? value : "");
        tf.setPrefWidth(300);
        return tf;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? null : message);
        alert.showAndWait();
    }
}
