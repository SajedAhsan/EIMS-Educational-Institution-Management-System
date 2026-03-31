package Student;

import database.DatabaseManager;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import util.PlagiarismDetector;
import util.forum.Announcement;
import util.forum.CourseCommunicationService;
import util.forum.ForumMessage;
import util.forum.ForumThread;
import util.forum.MentionUtils;

public class StudentGroupDetailsController {

    private enum DashboardPage {
        STUDENTS,
        ASSIGNMENTS,
        ANNOUNCEMENTS,
        DISCUSSION,
        MESSAGES,
        SUBMISSIONS
    }

    private static final String MENU_SELECTED_STYLE =
        "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;";
    private static final String MENU_NORMAL_STYLE =
        "-fx-background-color: #f8fafc; -fx-text-fill: #0f172a;";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Dashboard shell
    @FXML private Label groupNameLabel;
    @FXML private Label statusLabel;
    @FXML private StackPane centerPane;
    @FXML private Button studentsMenuButton;
    @FXML private Button assignmentsMenuButton;
    @FXML private Button announcementsMenuButton;
    @FXML private Button discussionMenuButton;
    @FXML private Button messagesMenuButton;
    @FXML private Button submissionsMenuButton;

    // Students page
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colMemberName;
    @FXML private TableColumn<StudentRow, String> colMemberEmail;

    // Assignments page
    @FXML private ListView<AssetItem> assetsList;
    @FXML private Label assetsStatusLabel;
    @FXML private Label deadlineLabel;
    @FXML private Label submissionStatusLabel;
    @FXML private Label gradeLabel;
    @FXML private Label feedbackLabel;

    // Announcements page
    @FXML private ListView<Announcement> announcementsListView;

    // Discussion page
    @FXML private ListView<ForumThread> forumThreadsListView;
    @FXML private VBox forumMessagesVBox;
    @FXML private TextArea newForumMessageArea;
    @FXML private Label forumReplyContextLabel;

    // Messages page
    @FXML private Label unreadMessagesHeaderLabel;
    @FXML private ListView<CourseCommunicationService.MessageNotification> unreadMessagesListView;
    @FXML private Label messagePreviewThreadLabel;
    @FXML private Label messagePreviewBodyLabel;

    // Submissions page
    @FXML private Label studentSubmissionsStatsLabel;
    @FXML private TableView<DatabaseManager.SubmissionData> submissionsTable;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colAssignmentName;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colSubmissionTime;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colStatus;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colFile;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colGrade;

    private final CourseCommunicationService communicationService = CourseCommunicationService.getInstance();

    private DatabaseManager dbManager;
    private ObservableList<StudentRow> studentsObs;
    private ObservableList<AssetItem> assetsObs;
    private ObservableList<DatabaseManager.SubmissionData> ownSubmissionsObs;

    private final List<DatabaseManager.MemberData> memberDataList = new ArrayList<>();
    private final List<DatabaseManager.AssetData> assetDataList = new ArrayList<>();
    private final List<CourseCommunicationService.CourseUser> courseUsers = new ArrayList<>();
    private final Map<Integer, ForumMessage> messageById = new HashMap<>();

    private boolean initialized;
    private DashboardPage currentPage = DashboardPage.STUDENTS;

    private int groupId;
    private String studentEmail;
    private int studentId = -1;
    private String studentName = "Student";
    private int groupTeacherId = -1;
    private String teacherName = "Teacher";
    private String teacherEmail = "";
    private int activeForumThreadId = -1;
    private Integer replyToMessageId = null;
    private int selectedAssetId = -1;

    public static class StudentRow {
        private final String name;
        private final String email;

        public StudentRow(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    public static class AssetItem {
        private final int id;
        private final String title;
        private final String description;
        private final String filePath;
        private final LocalDateTime deadline;

        public AssetItem(int id, String title, String description, String filePath, LocalDateTime deadline) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.filePath = filePath;
            this.deadline = deadline;
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

        public LocalDateTime getDeadline() {
            return deadline;
        }

        @Override
        public String toString() {
            StringBuilder display = new StringBuilder(title);
            if (description != null && !description.isBlank()) {
                display.append(" - ").append(description);
            }
            if (deadline != null) {
                display.append("  |  Due: ").append(deadline.format(DT_FMT));
            }
            return display.toString();
        }
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        dbManager = DatabaseManager.getInstance();
        studentsObs = FXCollections.observableArrayList();
        assetsObs = FXCollections.observableArrayList();
        ownSubmissionsObs = FXCollections.observableArrayList();

        updateMenuSelection(DashboardPage.STUDENTS);
        if (statusLabel != null) {
            statusLabel.setText("Loading group dashboard...");
        }
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setStudentEmail(String email) {
        this.studentEmail = email;
        try {
            studentId = dbManager.getStudentIdByEmail(email);
            DatabaseManager.StudentProfileData profile = dbManager.getStudentProfile(email);
            if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
                studentName = profile.getName();
            } else if (email != null && !email.isBlank()) {
                studentName = email;
            }
        } catch (SQLException ignored) {
        }
        loadGroupDetails();
    }

    @FXML
    void handleShowStudents(ActionEvent event) {
        loadDashboardPage(DashboardPage.STUDENTS);
    }

    @FXML
    void handleShowAssignments(ActionEvent event) {
        loadDashboardPage(DashboardPage.ASSIGNMENTS);
    }

    @FXML
    void handleShowAnnouncements(ActionEvent event) {
        loadDashboardPage(DashboardPage.ANNOUNCEMENTS);
    }

    @FXML
    void handleShowDiscussion(ActionEvent event) {
        loadDashboardPage(DashboardPage.DISCUSSION);
    }

    @FXML
    void handleShowMessages(ActionEvent event) {
        loadDashboardPage(DashboardPage.MESSAGES);
    }

    @FXML
    void handleShowSubmissions(ActionEvent event) {
        loadDashboardPage(DashboardPage.SUBMISSIONS);
    }

    private void loadDashboardPage(DashboardPage page) {
        if (centerPane == null) {
            return;
        }

        String pageFile = switch (page) {
            case STUDENTS -> "StudentsPage.fxml";
            case ASSIGNMENTS -> "AssignmentsPage.fxml";
            case ANNOUNCEMENTS -> "AnnouncementsPage.fxml";
            case DISCUSSION -> "DiscussionPage.fxml";
            case MESSAGES -> "MessagesPage.fxml";
            case SUBMISSIONS -> "SubmissionsPage.fxml";
        };

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(pageFile));
            loader.setController(this);
            Parent pageRoot = loader.load();
            centerPane.getChildren().setAll(pageRoot);

            currentPage = page;
            updateMenuSelection(page);
            configureLoadedPage(page);
            updateMessagesMenuBadge();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to load page: " + pageFile + "\n" + e.getMessage());
        }
    }

    private void configureLoadedPage(DashboardPage page) {
        switch (page) {
            case STUDENTS -> configureStudentsPage();
            case ASSIGNMENTS -> configureAssignmentsPage();
            case ANNOUNCEMENTS -> configureAnnouncementsPage();
            case DISCUSSION -> configureDiscussionPage();
            case MESSAGES -> configureMessagesPage();
            case SUBMISSIONS -> configureSubmissionsPage();
        }
    }

    private void configureStudentsPage() {
        if (studentsTable == null || colMemberName == null || colMemberEmail == null) {
            return;
        }

        colMemberName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colMemberEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));

        studentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        studentsTable.setItems(studentsObs);
        studentsTable.setPlaceholder(new Label("No students enrolled yet."));

        if (studentsObs.isEmpty() && groupId > 0) {
            loadMembers();
        }
    }

    private void configureAssignmentsPage() {
        if (assetsList == null) {
            return;
        }

        assetsList.setItems(assetsObs);
        assetsList.setPlaceholder(new Label("No assignments shared yet."));

        loadAssets();
        if (selectedAssetId > 0) {
            AssetItem selected = findAssetById(selectedAssetId);
            if (selected != null) {
                updateDeadlineAndStatusLabels(selected);
            }
        }
    }

    private void configureAnnouncementsPage() {
        if (announcementsListView == null) {
            return;
        }

        announcementsListView.setPlaceholder(new Label("No announcements yet."));
        announcementsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Announcement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + "\n"
                        + item.getMessage() + "\n"
                        + "Posted: " + item.getTimestamp().format(DT_FMT));
                }
            }
        });

        loadAnnouncements();
    }

    private void configureDiscussionPage() {
        if (forumThreadsListView == null) {
            return;
        }

        forumThreadsListView.setPlaceholder(new Label("No threads yet."));
        forumThreadsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ForumThread item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        clearReplyState();
        loadForumThreads();
    }

    private void configureMessagesPage() {
        if (unreadMessagesListView == null) {
            return;
        }

        unreadMessagesListView.setPlaceholder(new Label("No unread replies or mentions."));
        unreadMessagesListView.setCellFactory(list -> new ListCell<>() {
            private final Label titleLabel = new Label();
            private final Label metaLabel = new Label();
            private final Label previewLabel = new Label();
            private final VBox textBox = new VBox(3, titleLabel, metaLabel, previewLabel);
            private final Button replyButton = new Button("Reply");
            private final HBox root = new HBox(10, textBox, replyButton);

            {
                titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937;");
                metaLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                previewLabel.setStyle("-fx-text-fill: #334155;");
                previewLabel.setWrapText(true);
                textBox.setPrefWidth(0);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                replyButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
                replyButton.setOnAction(evt -> {
                    CourseCommunicationService.MessageNotification notification = getItem();
                    if (notification != null) {
                        handleReplyFromNotification(notification);
                    }
                    evt.consume();
                });
            }

            @Override
            protected void updateItem(CourseCommunicationService.MessageNotification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                titleLabel.setText(buildNotificationTitle(item));
                metaLabel.setText("Sent at " + item.getTimestamp().format(DT_FMT));
                previewLabel.setText(item.getPreviewText());
                setGraphic(root);
            }
        });

        unreadMessagesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                handleOpenMessageNotification(newValue);
            }
        });

        refreshUnreadMessageNotifications();
    }

    private String buildNotificationTitle(CourseCommunicationService.MessageNotification notification) {
        String sender = notification.getSenderDisplayName();
        String thread = notification.getThreadTitle();
        if (notification.getReason() == CourseCommunicationService.NotificationReason.REPLY) {
            return sender + " replied to your message in \"" + thread + "\"";
        }
        return sender + " mentioned you in \"" + thread + "\"";
    }

    private void configureSubmissionsPage() {
        configureSubmissionsTable();
        loadOwnSubmissions();
    }

    private void configureSubmissionsTable() {
        if (submissionsTable == null || colAssignmentName == null || colSubmissionTime == null
            || colStatus == null || colFile == null || colGrade == null) {
            return;
        }

        colAssignmentName.setCellValueFactory(c -> {
            String title = c.getValue().getAssignmentTitle();
            return new SimpleStringProperty(title != null && !title.isBlank() ? title : "-");
        });
        colSubmissionTime.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSubmissionTime().format(DT_FMT)));
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(getSubmissionStatusLabel(c.getValue())));
        colFile.setCellValueFactory(c -> {
            String path = c.getValue().getFilePath();
            if (path == null || path.isBlank()) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(Paths.get(path).getFileName().toString());
        });
        colGrade.setCellValueFactory(c -> {
            Integer g = c.getValue().getGrade();
            return new SimpleStringProperty(g != null ? g + "/100" : "-");
        });

        submissionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        submissionsTable.setItems(ownSubmissionsObs);
        submissionsTable.setPlaceholder(new Label("You have not submitted any assignment yet."));
    }

    private void refreshUnreadMessageNotifications() {
        if (groupId <= 0 || studentId <= 0) {
            updateMessagesMenuBadge();
            return;
        }

        List<CourseCommunicationService.MessageNotification> unread =
            communicationService.getMessageNotificationsForUser(groupId, studentId, "STUDENT", true);

        if (unreadMessagesListView != null) {
            unreadMessagesListView.setItems(FXCollections.observableArrayList(unread));
        }
        if (unreadMessagesHeaderLabel != null) {
            unreadMessagesHeaderLabel.setText("Unread Messages (" + unread.size() + ")");
        }
        if (unread.isEmpty() && messagePreviewThreadLabel != null && messagePreviewBodyLabel != null) {
            messagePreviewThreadLabel.setText("No unread message selected");
            messagePreviewBodyLabel.setText("When someone replies to you or mentions @username, it appears here.");
        }

        updateMessagesMenuBadge();
    }

    private void handleOpenMessageNotification(CourseCommunicationService.MessageNotification notification) {
        if (studentId <= 0) {
            return;
        }

        if (messagePreviewThreadLabel != null) {
            messagePreviewThreadLabel.setText("Thread: " + notification.getThreadTitle());
        }
        if (messagePreviewBodyLabel != null) {
            messagePreviewBodyLabel.setText(notification.getSenderDisplayName() + ": " + notification.getPreviewText());
        }

        communicationService.markMessageNotificationSeen(notification.getNotificationId(), studentId, "STUDENT");
        refreshUnreadMessageNotifications();
    }

    private void handleReplyFromNotification(CourseCommunicationService.MessageNotification notification) {
        if (studentId <= 0) {
            return;
        }

        communicationService.markMessageNotificationSeen(notification.getNotificationId(), studentId, "STUDENT");
        refreshUnreadMessageNotifications();

        ForumMessage originalMessage = communicationService.getMessageById(notification.getMessageId());
        if (originalMessage == null) {
            showAlert(AlertType.ERROR, "Message Missing", "The original message could not be found.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to Message");
        dialog.setHeaderText("Reply in \"" + notification.getThreadTitle() + "\"");
        dialog.setContentText("Your reply:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String replyText = result.get().trim();
        if (replyText.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Reply message cannot be empty.");
            return;
        }

        try {
            boolean canPost = communicationService.canUserPostInGroup(groupId, studentId, "STUDENT", dbManager);
            if (!canPost) {
                showAlert(AlertType.WARNING, "Permission Denied",
                    "Only the group teacher or an enrolled student can post in this discussion.");
                return;
            }

            rebuildCourseUsers();
            communicationService.postMessage(
                groupId,
                originalMessage.getThreadID(),
                studentId,
                "STUDENT",
                studentName,
                replyText,
                originalMessage.getMessageID(),
                groupDisplayName(),
                courseUsers,
                dbManager,
                true
            );

            if (currentPage == DashboardPage.DISCUSSION && forumMessagesVBox != null) {
                activeForumThreadId = originalMessage.getThreadID();
                loadForumThreads();
            }
            if (statusLabel != null) {
                statusLabel.setText("Reply posted to thread \"" + notification.getThreadTitle() + "\".");
            }
            refreshUnreadMessageNotifications();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to post reply: " + e.getMessage());
        }
    }

    private void updateMenuSelection(DashboardPage selectedPage) {
        setMenuButtonStyle(studentsMenuButton, selectedPage == DashboardPage.STUDENTS);
        setMenuButtonStyle(assignmentsMenuButton, selectedPage == DashboardPage.ASSIGNMENTS);
        setMenuButtonStyle(announcementsMenuButton, selectedPage == DashboardPage.ANNOUNCEMENTS);
        setMenuButtonStyle(discussionMenuButton, selectedPage == DashboardPage.DISCUSSION);
        setMenuButtonStyle(messagesMenuButton, selectedPage == DashboardPage.MESSAGES);
        setMenuButtonStyle(submissionsMenuButton, selectedPage == DashboardPage.SUBMISSIONS);
    }

    private void setMenuButtonStyle(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setStyle(selected ? MENU_SELECTED_STYLE : MENU_NORMAL_STYLE);
    }

    private void updateMessagesMenuBadge() {
        if (messagesMenuButton == null) {
            return;
        }

        if (groupId <= 0 || studentId <= 0) {
            messagesMenuButton.setText("Messages");
            return;
        }

        int unseen = communicationService.countUnseenMessageNotifications(groupId, studentId, "STUDENT");
        messagesMenuButton.setText(unseen > 0 ? "Messages (" + unseen + ")" : "Messages");
    }

    private void loadGroupDetails() {
        if (groupId <= 0 || studentEmail == null || studentEmail.isBlank()) {
            return;
        }

        try {
            DatabaseManager.GroupDetailData groupData = dbManager.getGroupById(groupId);
            if (groupData == null) {
                showAlert(AlertType.ERROR, "Error", "Group not found in database");
                return;
            }

            if (groupNameLabel != null) {
                groupNameLabel.setText("Group: " + groupData.getName());
            }
            teacherName = groupData.getTeacherName();
            teacherEmail = groupData.getTeacherEmail();
            groupTeacherId = dbManager.getTeacherIdByEmail(teacherEmail);

            loadMembers();
            loadAssets();
            loadCommunicationSection();
            loadOwnSubmissions();
            updateMessagesMenuBadge();
            loadDashboardPage(DashboardPage.STUDENTS);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load group details: " + e.getMessage());
        }
    }

    private String groupDisplayName() {
        String title = groupNameLabel != null ? groupNameLabel.getText() : "";
        if (title.startsWith("Group: ")) {
            return title.substring("Group: ".length()).trim();
        }
        return title;
    }

    private void loadMembers() {
        try {
            memberDataList.clear();
            memberDataList.addAll(dbManager.getGroupMembers(groupId));

            studentsObs.clear();
            for (DatabaseManager.MemberData member : memberDataList) {
                studentsObs.add(new StudentRow(member.getName(), member.getEmail()));
            }

            if (statusLabel != null) {
                statusLabel.setText(memberDataList.isEmpty()
                    ? "No members in this group"
                    : memberDataList.size() + " member(s) in group");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load members: " + e.getMessage());
        }
    }

    private void loadAssets() {
        try {
            assetDataList.clear();
            assetDataList.addAll(dbManager.getGroupAssets(groupId));

            assetsObs.clear();
            for (DatabaseManager.AssetData asset : assetDataList) {
                assetsObs.add(new AssetItem(
                    asset.getId(),
                    asset.getTitle(),
                    asset.getDescription(),
                    asset.getFilePath(),
                    asset.getDeadline()
                ));
            }

            if (assetsStatusLabel != null) {
                if (assetsObs.isEmpty()) {
                    assetsStatusLabel.setText("No materials shared yet");
                    assetsStatusLabel.setVisible(true);
                } else {
                    assetsStatusLabel.setVisible(false);
                }
            }

            if (statusLabel != null && currentPage == DashboardPage.ASSIGNMENTS) {
                statusLabel.setText(assetsObs.isEmpty()
                    ? "No assignments shared yet"
                    : assetsObs.size() + " assignment(s) available");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load materials: " + e.getMessage());
        }
    }

    private void loadOwnSubmissions() {
        if (studentId <= 0) {
            ownSubmissionsObs.clear();
            if (studentSubmissionsStatsLabel != null) {
                studentSubmissionsStatsLabel.setText("Student account not loaded.");
            }
            return;
        }

        try {
            List<DatabaseManager.SubmissionData> own = dbManager.getSubmissionsByGroupAndStudent(groupId, studentId);
            ownSubmissionsObs.setAll(own);

            if (studentSubmissionsStatsLabel != null) {
                long graded = own.stream().filter(DatabaseManager.SubmissionData::isEvaluated).count();
                long pending = own.size() - graded;
                long late = own.stream().filter(DatabaseManager.SubmissionData::isLate).count();
                studentSubmissionsStatsLabel.setText(String.format(
                    "Total: %d  |  Graded: %d  |  Pending: %d  |  Late: %d",
                    own.size(), graded, pending, late));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load your submissions: " + e.getMessage());
        }
    }

    private String getSubmissionStatusLabel(DatabaseManager.SubmissionData submission) {
        if (submission.isPlagiarized()) {
            return "Plagiarized";
        }
        if (submission.isLate()) {
            return submission.isEvaluated() ? "Late (Graded)" : "Late";
        }
        if (submission.isEvaluated()) {
            return "Graded";
        }
        return "Pending";
    }

    private void loadCommunicationSection() {
        communicationService.ensureGeneralThread(groupId, groupTeacherId > 0 ? groupTeacherId : 0);
        try {
            rebuildCourseUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load forum participants: " + e.getMessage());
            return;
        }

        loadAnnouncements();
        loadForumThreads();
        clearReplyState();
        updateMessagesMenuBadge();
    }

    private void rebuildCourseUsers() throws SQLException {
        courseUsers.clear();

        if (groupTeacherId > 0) {
            courseUsers.add(new CourseCommunicationService.CourseUser(
                groupTeacherId,
                "TEACHER",
                teacherName,
                MentionUtils.usernameFromEmail(teacherEmail)
            ));
        }

        if (memberDataList.isEmpty()) {
            memberDataList.addAll(dbManager.getGroupMembers(groupId));
        }

        for (DatabaseManager.MemberData member : memberDataList) {
            courseUsers.add(new CourseCommunicationService.CourseUser(
                member.getId(),
                "STUDENT",
                member.getName(),
                MentionUtils.usernameFromEmail(member.getEmail())
            ));
        }
    }

    private void loadAnnouncements() {
        if (announcementsListView == null) {
            return;
        }
        announcementsListView.setItems(FXCollections.observableArrayList(
            communicationService.getAnnouncementsForCourse(groupId)
        ));
    }

    private void loadForumThreads() {
        if (forumThreadsListView == null) {
            return;
        }

        List<ForumThread> threads = communicationService.getThreadsForCourse(groupId);
        forumThreadsListView.setItems(FXCollections.observableArrayList(threads));

        if (threads.isEmpty()) {
            activeForumThreadId = -1;
            loadForumMessages();
            return;
        }

        ForumThread selected = null;
        for (ForumThread thread : threads) {
            if (thread.getThreadID() == activeForumThreadId) {
                selected = thread;
                break;
            }
        }
        if (selected == null) {
            selected = threads.get(0);
        }

        activeForumThreadId = selected.getThreadID();
        forumThreadsListView.getSelectionModel().select(selected);
        loadForumMessages();
    }

    private void loadForumMessages() {
        if (forumMessagesVBox == null) {
            return;
        }

        forumMessagesVBox.getChildren().clear();
        messageById.clear();

        if (activeForumThreadId <= 0) {
            forumMessagesVBox.getChildren().add(new Label("Select a thread to view messages."));
            return;
        }

        List<ForumMessage> threadMessages = communicationService.getMessagesForThread(activeForumThreadId);
        if (threadMessages.isEmpty()) {
            forumMessagesVBox.getChildren().add(new Label("No messages yet. Start the discussion."));
            return;
        }

        Map<Integer, List<ForumMessage>> childrenByParent = new HashMap<>();
        List<ForumMessage> roots = new ArrayList<>();
        for (ForumMessage msg : threadMessages) {
            messageById.put(msg.getMessageID(), msg);
            Integer parent = msg.getParentMessageID();
            if (parent == null) {
                roots.add(msg);
            } else {
                childrenByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(msg);
            }
        }

        roots.sort(Comparator.comparing(ForumMessage::getTimestamp));
        for (List<ForumMessage> children : childrenByParent.values()) {
            children.sort(Comparator.comparing(ForumMessage::getTimestamp));
        }

        for (ForumMessage root : roots) {
            renderForumMessage(root, childrenByParent, 0);
        }
    }

    private void renderForumMessage(ForumMessage message,
                                    Map<Integer, List<ForumMessage>> childrenByParent,
                                    int depth) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d8dee9; -fx-border-radius: 4; -fx-background-radius: 4;");
        card.setTranslateX(Math.min(depth, 4) * 18.0);

        Label header = new Label(message.getAuthorDisplayName() + "  |  " + message.getTimestamp().format(DT_FMT));
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: #2f3b52;");

        Label body = new Label(message.getMessageText());
        body.setWrapText(true);

        Button replyBtn = new Button("Reply");
        replyBtn.setOnAction(e -> setReplyTarget(message));

        card.getChildren().addAll(header, body, replyBtn);
        forumMessagesVBox.getChildren().add(card);

        List<ForumMessage> children = childrenByParent.get(message.getMessageID());
        if (children == null) {
            return;
        }

        for (ForumMessage child : children) {
            renderForumMessage(child, childrenByParent, depth + 1);
        }
    }

    private void setReplyTarget(ForumMessage message) {
        replyToMessageId = message.getMessageID();
        if (forumReplyContextLabel != null) {
            forumReplyContextLabel.setText("Replying to " + message.getAuthorDisplayName());
        }
    }

    private void clearReplyState() {
        replyToMessageId = null;
        if (forumReplyContextLabel != null) {
            forumReplyContextLabel.setText("");
        }
    }

    @FXML
    void handleForumThreadSelection(MouseEvent event) {
        ForumThread selected = forumThreadsListView != null
            ? forumThreadsListView.getSelectionModel().getSelectedItem()
            : null;
        if (selected == null) {
            return;
        }
        activeForumThreadId = selected.getThreadID();
        clearReplyState();
        loadForumMessages();
    }

    @FXML
    void handlePostForumMessage(ActionEvent event) {
        String messageText = newForumMessageArea != null ? newForumMessageArea.getText().trim() : "";
        if (messageText.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please write a message before posting.");
            return;
        }
        if (activeForumThreadId <= 0) {
            showAlert(AlertType.WARNING, "No Thread Selected", "Please select a discussion thread first.");
            return;
        }

        try {
            boolean canPost = communicationService.canUserPostInGroup(groupId, studentId, "STUDENT", dbManager);
            if (!canPost) {
                showAlert(AlertType.WARNING, "Permission Denied",
                    "Only the group teacher or an enrolled student can post in this discussion.");
                return;
            }

            rebuildCourseUsers();
            communicationService.postMessage(
                groupId,
                activeForumThreadId,
                studentId,
                "STUDENT",
                studentName,
                messageText,
                replyToMessageId,
                groupDisplayName(),
                courseUsers,
                dbManager,
                true
            );

            newForumMessageArea.clear();
            clearReplyState();
            loadForumMessages();
            if (currentPage == DashboardPage.MESSAGES) {
                refreshUnreadMessageNotifications();
            } else {
                updateMessagesMenuBadge();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to post message: " + e.getMessage());
        }
    }

    @FXML
    void handleCancelForumReply(ActionEvent event) {
        clearReplyState();
    }

    @FXML
    void handleAssetSelection(MouseEvent event) {
        if (assetsList == null) {
            return;
        }

        AssetItem selected = assetsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        selectedAssetId = selected.getId();
        updateDeadlineAndStatusLabels(selected);

        if (event.getClickCount() == 2) {
            openAsset(selected);
        }
    }

    private void updateDeadlineAndStatusLabels(AssetItem asset) {
        if (deadlineLabel != null) {
            if (asset.getDeadline() != null) {
                deadlineLabel.setText("Deadline: " + asset.getDeadline().format(DT_FMT));
            } else {
                deadlineLabel.setText("Deadline: not set");
            }
        }

        if (gradeLabel != null) {
            gradeLabel.setText("Grade: not yet evaluated");
        }
        if (feedbackLabel != null) {
            feedbackLabel.setText("");
        }

        if (submissionStatusLabel != null && studentId > 0) {
            try {
                DatabaseManager.SubmissionData sub = dbManager.getSubmissionByStudentAndAsset(studentId, asset.getId());
                if (sub == null) {
                    submissionStatusLabel.setText("Not submitted yet");
                } else {
                    String statusMsg = "Submitted on: " + sub.getSubmissionTime().format(DT_FMT);
                    if (asset.getDeadline() != null) {
                        statusMsg += "\n" + buildDeadlineStatus(sub.getSubmissionTime(), asset.getDeadline());
                    }
                    submissionStatusLabel.setText(statusMsg);

                    if (sub.isEvaluated() && sub.getGrade() != null) {
                        if (gradeLabel != null) {
                            gradeLabel.setText("Grade: " + sub.getGrade() + "/100");
                        }
                        if (feedbackLabel != null) {
                            String fb = sub.getFeedback();
                            feedbackLabel.setText((fb != null && !fb.isBlank()) ? "Feedback: " + fb : "");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String buildDeadlineStatus(LocalDateTime submissionTime, LocalDateTime deadline) {
        Duration diff = Duration.between(submissionTime, deadline);
        long totalMinutes = Math.abs(diff.toMinutes());
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        String timeStr = hours > 0
            ? hours + " hour(s) " + minutes + " minute(s)"
            : minutes + " minute(s)";

        if (!submissionTime.isAfter(deadline)) {
            return "Submitted " + timeStr + " before deadline";
        }
        return "Late submission by " + timeStr;
    }

    @FXML
    void handleDownload(ActionEvent event) {
        if (assetsList == null) {
            return;
        }

        AssetItem selected = assetsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a material to download");
            return;
        }
        downloadAsset(selected);
    }

    @FXML
    void handleSubmitAssignment(ActionEvent event) {
        if (assetsList == null) {
            return;
        }

        AssetItem selected = assetsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select an assignment to submit");
            return;
        }
        if (studentId <= 0) {
            showAlert(AlertType.ERROR, "Error", "Could not identify student account");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Submission File");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("ZIP Archives", "*.zip")
        );

        Stage stage = (Stage) assetsList.getScene().getWindow();
        File chosenFile = fc.showOpenDialog(stage);
        if (chosenFile == null) {
            return;
        }

        try {
            Path submissionsDir = Paths.get("data", "submissions");
            Files.createDirectories(submissionsDir);

            String destName = selected.getId() + "_" + studentId + "_" + chosenFile.getName();
            Path destination = submissionsDir.resolve(destName);
            Files.copy(chosenFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            LocalDateTime now = LocalDateTime.now();
            String relativePath = "data/submissions/" + destName;
            dbManager.addSubmission(selected.getId(), studentId, relativePath, now);

            try {
                PlagiarismDetector.checkAndMark(
                    dbManager,
                    selected.getId(),
                    studentId,
                    relativePath,
                    selected.getTitle(),
                    groupDisplayName(),
                    groupTeacherId
                );
            } catch (Exception plagEx) {
                plagEx.printStackTrace();
            }

            if (groupTeacherId > 0) {
                try {
                    dbManager.createNotification(groupTeacherId, "TEACHER",
                        studentName + " submitted \"" + selected.getTitle() +
                        "\" in group \"" + groupDisplayName() + "\"");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            String statusMsg = "Assignment submitted successfully!";
            if (selected.getDeadline() != null) {
                statusMsg += "\n\n" + buildDeadlineStatus(now, selected.getDeadline());
            }
            showAlert(AlertType.INFORMATION, "Submitted", statusMsg);
            updateDeadlineAndStatusLabels(selected);
            loadOwnSubmissions();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "File Error", "Failed to copy submission file: " + e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to save submission: " + e.getMessage());
        }
    }

    private void openAsset(AssetItem asset) {
        try {
            if (asset.getFilePath() == null || asset.getFilePath().isBlank()) {
                showAlert(AlertType.WARNING, "No File", "This material has no file attached");
                return;
            }

            Path filePath = Paths.get(asset.getFilePath());
            if (!Files.exists(filePath)) {
                showAlert(AlertType.ERROR, "File Not Found", "The file does not exist: " + asset.getFilePath());
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(filePath.toFile());
            } else {
                showAlert(AlertType.ERROR, "Error", "Cannot open file: Desktop operations not supported");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to open file: " + e.getMessage());
        }
    }

    private void downloadAsset(AssetItem asset) {
        try {
            if (asset.getFilePath() == null || asset.getFilePath().isBlank()) {
                showAlert(AlertType.WARNING, "No File", "This material has no file attached");
                return;
            }

            Path sourcePath = Paths.get(asset.getFilePath());
            if (!Files.exists(sourcePath)) {
                showAlert(AlertType.ERROR, "File Not Found", "The file does not exist: " + asset.getFilePath());
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(sourcePath.getFileName().toString());

            String fileName = sourcePath.getFileName().toString();
            String extension = "";
            int idx = fileName.lastIndexOf('.');
            if (idx > 0) {
                extension = fileName.substring(idx + 1).toLowerCase();
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
            if (assetsList == null) {
                return;
            }
            File destFile = fileChooser.showSaveDialog(assetsList.getScene().getWindow());
            if (destFile != null) {
                Files.copy(sourcePath, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert(AlertType.INFORMATION, "Success",
                    "File downloaded successfully!\n\nSaved to: " + destFile.getAbsolutePath() +
                    "\nSize: " + String.format("%.2f", Files.size(destFile.toPath()) / 1024.0) + " KB");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Download Error", "Failed to download file: " + e.getMessage());
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
            stage.setWidth(1200.0);
            stage.setHeight(800.0);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to go back: " + e.getMessage());
        }
    }

    private AssetItem findAssetById(int assetId) {
        for (AssetItem item : assetsObs) {
            if (item.getId() == assetId) {
                return item;
            }
        }
        return null;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
