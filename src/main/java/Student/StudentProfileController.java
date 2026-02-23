package Student;

import database.DatabaseManager;
import database.DatabaseManager.StudentProfileData;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
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

public class StudentProfileController {

    // ── Profile labels ─────────────────────────────────────────────────────
    @FXML private Label labelName;
    @FXML private Label labelEmail;
    @FXML private Label labelStudentNo;
    @FXML private Label labelLevelTerm;
    @FXML private Label labelMobileNumber;
    @FXML private Label labelBankAccount;
    @FXML private Label labelPhoneNumber;
    @FXML private Label labelMobileBanking;
    @FXML private Label labelPresentAddress;
    @FXML private Label labelPermanentAddress;
    @FXML private Label labelContactPerson;
    @FXML private Label labelBirthRegNo;
    @FXML private Label labelBirthDate;
    @FXML private Label labelNid;
    @FXML private Label labelNameBangla;

    // ── Photo ──────────────────────────────────────────────────────────────
    @FXML private ImageView photoView;
    @FXML private Label     photoLabel;

    private String studentEmail;
    private StudentProfileData profileData;
    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Public API ─────────────────────────────────────────────────────────

    public void setStudentEmail(String email) {
        this.studentEmail = email;
        loadProfile();
    }

    // ── Load / display ─────────────────────────────────────────────────────

    private void loadProfile() {
        try {
            profileData = db.getStudentProfile(studentEmail);
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

    private void displayProfile(StudentProfileData p) {
        labelName.setText(orDummy(p.getName(), "Dummy Name"));
        labelEmail.setText(orDummy(p.getEmail(), "Dummy X"));
        labelStudentNo.setText(orDummy(p.getStudentNo(), "Dummy X"));
        labelLevelTerm.setText(orDummy(p.getLevelTerm(), "Dummy X"));
        labelMobileNumber.setText(orDummy(p.getMobileNumber(), "Dummy X"));
        labelBankAccount.setText(orDummy(p.getBankAccount(), "Dummy X"));
        labelPhoneNumber.setText(orDummy(p.getPhoneNumber(), "Dummy X"));
        labelMobileBanking.setText(orDummy(p.getMobileBanking(), "Dummy X"));
        labelPresentAddress.setText(orDummy(p.getPresentAddress(), "Dummy X"));
        labelPermanentAddress.setText(orDummy(p.getPermanentAddress(), "Dummy X"));
        labelContactPerson.setText(orDummy(p.getContactPerson(), "Dummy X"));
        labelBirthRegNo.setText(orDummy(p.getBirthRegNo(), "Dummy X"));
        labelBirthDate.setText(orDummy(p.getBirthDate(), "Dummy X"));
        labelNid.setText(orDummy(p.getNid(), "Dummy X"));
        labelNameBangla.setText(orDummy(p.getNameBangla(), "Dummy X"));

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
                db.updateStudentProfile(studentEmail, profileData);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ── Back button ────────────────────────────────────────────────────────

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentDashboard.fxml"));
            Parent root = loader.load();
            studentDashboardController ctrl = loader.getController();
            ctrl.setStudentEmail(studentEmail);
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
        ColumnConstraints labelCol = new ColumnConstraints(180);
        ColumnConstraints fieldCol = new ColumnConstraints(300);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        // Field helper
        TextField fName          = makeField(profileData.getName());
        TextField fStudentNo     = makeField(profileData.getStudentNo());
        TextField fLevelTerm     = makeField(profileData.getLevelTerm());
        TextField fMobileNumber  = makeField(profileData.getMobileNumber());
        TextField fBankAccount   = makeField(profileData.getBankAccount());
        TextField fPhoneNumber   = makeField(profileData.getPhoneNumber());
        TextField fMobileBanking = makeField(profileData.getMobileBanking());
        TextArea  fPresentAddr   = makeArea(profileData.getPresentAddress());
        TextArea  fPermanentAddr = makeArea(profileData.getPermanentAddress());
        TextArea  fContactPerson = makeArea(profileData.getContactPerson());
        TextField fBirthRegNo    = makeField(profileData.getBirthRegNo());
        TextField fBirthDate     = makeField(profileData.getBirthDate());
        TextField fNid           = makeField(profileData.getNid());
        TextField fNameBangla    = makeField(profileData.getNameBangla());

        Object[][] rows = {
            {"Name",                              fName},
            {"Student No",                        fStudentNo},
            {"Level/Term",                        fLevelTerm},
            {"Mobile Number",                     fMobileNumber},
            {"Bank Account Number",               fBankAccount},
            {"Phone Number",                      fPhoneNumber},
            {"Mobile Banking Account",            fMobileBanking},
            {"Present/Residential Address",       fPresentAddr},
            {"Permanent Address",                 fPermanentAddr},
            {"Contact Person (Name/Address/No.)", fContactPerson},
            {"Birth Registration No",             fBirthRegNo},
            {"Birth Date (e.g. 01-01-2000)",      fBirthDate},
            {"NID",                               fNid},
            {"Name (Bangla)",                     fNameBangla}
        };

        for (int i = 0; i < rows.length; i++) {
            grid.add(new Label((String) rows[i][0]), 0, i);
            grid.add((Node) rows[i][1], 1, i);
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(460);

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
        dialog.setTitle("Edit Contact Information");
        dialog.setScene(new Scene(root));
        dialog.setResizable(false);

        btnCancel.setOnAction(e -> dialog.close());
        btnSave.setOnAction(e -> {
            profileData.setName(fName.getText().trim());
            profileData.setStudentNo(fStudentNo.getText().trim());
            profileData.setLevelTerm(fLevelTerm.getText().trim());
            profileData.setMobileNumber(fMobileNumber.getText().trim());
            profileData.setBankAccount(fBankAccount.getText().trim());
            profileData.setPhoneNumber(fPhoneNumber.getText().trim());
            profileData.setMobileBanking(fMobileBanking.getText().trim());
            profileData.setPresentAddress(fPresentAddr.getText().trim());
            profileData.setPermanentAddress(fPermanentAddr.getText().trim());
            profileData.setContactPerson(fContactPerson.getText().trim());
            profileData.setBirthRegNo(fBirthRegNo.getText().trim());
            profileData.setBirthDate(fBirthDate.getText().trim());
            profileData.setNid(fNid.getText().trim());
            profileData.setNameBangla(fNameBangla.getText().trim());

            try {
                db.updateStudentProfile(studentEmail, profileData);
                displayProfile(profileData);
                dialog.close();
                showAlert(AlertType.INFORMATION, "Success", "Information saved");
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
                boolean ok = db.updateStudentPassword(studentEmail, oldPwd, newPwd);
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

    private static TextArea makeArea(String value) {
        TextArea ta = new TextArea(value != null ? value : "");
        ta.setPrefRowCount(2);
        ta.setWrapText(true);
        ta.setPrefWidth(300);
        return ta;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? null : message);
        alert.showAndWait();
    }
}
