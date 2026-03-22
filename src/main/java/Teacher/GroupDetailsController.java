package Teacher;

import database.DatabaseManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import progress.ProgressTrackerService;
import progress.model.Course;
import util.forum.Announcement;
import util.forum.CourseCommunicationService;
import util.forum.ForumMessage;
import util.forum.ForumThread;
import util.forum.MentionUtils;

public class GroupDetailsController {

    private enum DashboardPage {
        STUDENTS,
        ASSIGNMENTS,
        ANNOUNCEMENTS,
        DISCUSSION,
        MESSAGES,
        ATTENDANCE,
        SUBMISSIONS
    }

    private static final String MENU_SELECTED_STYLE =
        "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;";
    private static final String MENU_NORMAL_STYLE =
        "-fx-background-color: #f8fafc; -fx-text-fill: #0f172a;";
    private static final double ATTENDANCE_RISK_THRESHOLD = 75.0;
    private static final String NON_WORKING_DAY_STATUS = "Not a working day";
    private static final String[] SCORE_RANGE_BUCKETS = {
        "0-10", "11-20", "21-30", "31-40", "41-50",
        "51-60", "61-70", "71-80", "81-90", "91-100"
    };

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
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
    @FXML private Button attendanceMenuButton;
    @FXML private Button submissionsMenuButton;

    // Students page
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colMemberName;
    @FXML private TableColumn<StudentRow, String> colMemberEmail;

    // Assignments page
    @FXML private TextField assetTitleField;
    @FXML private TextArea assetDescriptionArea;
    @FXML private TextField assetFilePathField;
    @FXML private TextField assetTypeField;
    @FXML private DatePicker deadlineDatePicker;
    @FXML private TextField deadlineTimeField;
    @FXML private ListView<String> sharedAssetsList;

    // Submissions page
    @FXML private TableView<DatabaseManager.SubmissionData> submissionsTable;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colStudentName;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colAssignmentName;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colAssignmentId;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colSubmissionTime;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colStatus;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colFile;
    @FXML private TableColumn<DatabaseManager.SubmissionData, String> colGrade;
    @FXML private Label statsLabel;
    @FXML private ComboBox<String> submissionsFilterCombo;
    @FXML private ComboBox<DatabaseManager.AssetData> performanceAssignmentCombo;
    @FXML private Label performanceAssignmentHintLabel;
    @FXML private Label performanceStatsLabel;
    @FXML private BarChart<String, Number> performanceDistributionBarChart;

    // Announcements page
    @FXML private ListView<Announcement> announcementsListView;
    @FXML private TextField announcementTitleField;
    @FXML private TextArea announcementMessageArea;

    // Discussion page
    @FXML private ListView<ForumThread> forumThreadsListView;
    @FXML private VBox forumMessagesVBox;
    @FXML private TextArea newForumMessageArea;
    @FXML private Label forumReplyContextLabel;
    @FXML private TextField assignmentThreadTitleField;

    // Messages page
    @FXML private Label unreadMessagesHeaderLabel;
    @FXML private ListView<CourseCommunicationService.MessageNotification> unreadMessagesListView;
    @FXML private Label messagePreviewThreadLabel;
    @FXML private Label messagePreviewBodyLabel;

    // Attendance page
    @FXML private ComboBox<Course> attendanceCourseCombo;
    @FXML private DatePicker attendanceDatePicker;
    @FXML private TableView<AttendanceRow> attendanceTable;
    @FXML private TableColumn<AttendanceRow, String> colAttendanceStudent;
    @FXML private TableColumn<AttendanceRow, String> colAttendanceStatus;
    @FXML private Label attendanceSummaryLabel;
    @FXML private BarChart<String, Number> attendanceOverviewBarChart;

    private ObservableList<StudentRow> studentsObs;
    private ObservableList<String> assets;
    private ObservableList<DatabaseManager.SubmissionData> submissionsObs;
    private List<DatabaseManager.AssetData> assetDataList;
    private List<DatabaseManager.MemberData> memberDataList;

    private int groupId;
    private String teacherEmail;
    private DatabaseManager dbManager;
    private int selectedAssetId = -1;
    private int teacherId = -1;
    private String teacherName = "Teacher";
    private int activeForumThreadId = -1;
    private Integer replyToMessageId = null;

    private boolean initialized;
    private DashboardPage currentPage = DashboardPage.STUDENTS;
    private final CourseCommunicationService communicationService = CourseCommunicationService.getInstance();
    private final ProgressTrackerService progressTrackerService = ProgressTrackerService.getInstance();
    private final List<CourseCommunicationService.CourseUser> courseUsers = new ArrayList<>();
    private final Map<Integer, ForumMessage> messageById = new HashMap<>();
    private final List<DatabaseManager.SubmissionData> allGroupSubmissions = new ArrayList<>();
    private final ObservableList<AttendanceRow> attendanceRows = FXCollections.observableArrayList();
    private int selectedAttendanceCourseId = -1;
    private LocalDate attendanceTrackingStartDate = LocalDate.now();

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

    public static class AttendanceRow {
        private final int studentId;
        private final String studentName;
        private String status;
        private double attendancePercent;

        public AttendanceRow(int studentId, String studentName, String status, double attendancePercent) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.status = status;
            this.attendancePercent = attendancePercent;
        }

        public int getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public double getAttendancePercent() {
            return attendancePercent;
        }

        public void setAttendancePercent(double attendancePercent) {
            this.attendancePercent = attendancePercent;
        }
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        dbManager = DatabaseManager.getInstance();
        studentsObs = FXCollections.observableArrayList();
        assets = FXCollections.observableArrayList();
        submissionsObs = FXCollections.observableArrayList();
        assetDataList = new ArrayList<>();
        memberDataList = new ArrayList<>();

        updateMenuSelection(DashboardPage.STUDENTS);
        if (statusLabel != null) {
            statusLabel.setText("Loading group dashboard...");
        }
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setTeacherEmail(String email) {
        this.teacherEmail = email;
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
    void handleShowAttendance(ActionEvent event) {
        loadDashboardPage(DashboardPage.ATTENDANCE);
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
            case ATTENDANCE -> "AttendanceManagementPage.fxml";
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
            case ATTENDANCE -> configureAttendancePage();
            case SUBMISSIONS -> configureSubmissionsPage();
        }
    }

    private void configureStudentsPage() {
        if (studentsTable == null || colMemberName == null || colMemberEmail == null) {
            return;
        }

        colMemberName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colMemberEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));

        studentsTable.setItems(studentsObs);
        studentsTable.setPlaceholder(new Label("No students enrolled yet."));

        if (studentsObs.isEmpty() && groupId > 0) {
            loadMembers();
        }
    }

    private void configureAssignmentsPage() {
        if (sharedAssetsList != null) {
            sharedAssetsList.setItems(assets);
            sharedAssetsList.setPlaceholder(new Label("No assignments shared yet."));
        }
        loadAssets();
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

    private void refreshUnreadMessageNotifications() {
        if (teacherId <= 0 || groupId <= 0) {
            updateMessagesMenuBadge();
            return;
        }

        List<CourseCommunicationService.MessageNotification> unread =
            communicationService.getMessageNotificationsForUser(groupId, teacherId, "TEACHER", true);

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
        if (teacherId <= 0) {
            return;
        }

        if (messagePreviewThreadLabel != null) {
            messagePreviewThreadLabel.setText("Thread: " + notification.getThreadTitle());
        }
        if (messagePreviewBodyLabel != null) {
            messagePreviewBodyLabel.setText(notification.getSenderDisplayName() + ": " + notification.getPreviewText());
        }

        communicationService.markMessageNotificationSeen(notification.getNotificationId(), teacherId, "TEACHER");
        refreshUnreadMessageNotifications();
    }

    private void handleReplyFromNotification(CourseCommunicationService.MessageNotification notification) {
        if (teacherId <= 0) {
            return;
        }

        communicationService.markMessageNotificationSeen(notification.getNotificationId(), teacherId, "TEACHER");
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
            boolean canPost = communicationService.canUserPostInGroup(groupId, teacherId, "TEACHER", dbManager);
            if (!canPost) {
                showAlert(AlertType.WARNING, "Permission Denied",
                    "Only the group teacher or an enrolled student can post in this discussion.");
                return;
            }

            rebuildCourseUsers();
            communicationService.postMessage(
                groupId,
                originalMessage.getThreadID(),
                teacherId,
                "TEACHER",
                teacherName,
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

    private void updateMessagesMenuBadge() {
        if (messagesMenuButton == null) {
            return;
        }

        if (groupId <= 0 || teacherId <= 0) {
            messagesMenuButton.setText("Messages");
            return;
        }

        int unseen = communicationService.countUnseenMessageNotifications(groupId, teacherId, "TEACHER");
        messagesMenuButton.setText(unseen > 0 ? "Messages (" + unseen + ")" : "Messages");
    }

    private void configureSubmissionsPage() {
        if (memberDataList.isEmpty()) {
            loadMembers();
        }
        if (assetDataList.isEmpty()) {
            loadAssets();
        }

        configureSubmissionsTable();
        configureSubmissionsFilter();
        configurePerformanceAnalyticsSection();
        loadSubmissionsForGroup();
    }

    private void configureAttendancePage() {
        if (attendanceCourseCombo == null
            || attendanceDatePicker == null
            || attendanceTable == null
            || colAttendanceStudent == null
            || colAttendanceStatus == null) {
            return;
        }

        if (memberDataList.isEmpty()) {
            loadMembers();
        }

        attendanceTable.setEditable(false);
        attendanceTable.setItems(attendanceRows);

        colAttendanceStudent.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStudentName()));
        colAttendanceStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));

        colAttendanceStatus.setEditable(true);
        colAttendanceStatus.setCellFactory(column -> new TableCell<AttendanceRow, String>() {
            private final ComboBox<String> statusCombo =
                new ComboBox<>(FXCollections.observableArrayList("Present", "Absent"));

            {
                statusCombo.setMaxWidth(Double.MAX_VALUE);
                statusCombo.setOnAction(evt -> {
                    if (!isEditing()) {
                        return;
                    }
                    String selectedStatus = statusCombo.getValue();
                    if (selectedStatus != null) {
                        commitEdit(selectedStatus);
                    }
                });
            }

            @Override
            public void startEdit() {
                if (isEmpty()) {
                    return;
                }

                LocalDate selectedDate = normalizeAttendanceDate(
                    attendanceDatePicker != null ? attendanceDatePicker.getValue() : null
                );
                if (selectedAttendanceCourseId <= 0
                    || !isAttendanceDateValidForGroup(selectedDate)
                    || !progressTrackerService.isWorkingDay(selectedAttendanceCourseId, selectedDate)) {
                    return;
                }

                super.startEdit();
                statusCombo.setValue(getItem());
                setText(null);
                setGraphic(statusCombo);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                statusCombo.show();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                if (isEditing()) {
                    statusCombo.setValue(item);
                    setText(null);
                    setGraphic(statusCombo);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(item);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }

                if ("Present".equalsIgnoreCase(item)) {
                    setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-font-weight: bold;");
                } else if ("Absent".equalsIgnoreCase(item)) {
                    setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold;");
                } else if (NON_WORKING_DAY_STATUS.equalsIgnoreCase(item)) {
                    setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-style: italic;");
                } else {
                    setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #475569;");
                }
            }
        });

        colAttendanceStatus.setOnEditCommit(event -> {
            AttendanceRow row = event.getRowValue();
            String newValue = event.getNewValue();
            if (row == null || newValue == null || selectedAttendanceCourseId <= 0) {
                return;
            }

            LocalDate selectedDate = normalizeAttendanceDate(
                attendanceDatePicker != null ? attendanceDatePicker.getValue() : null
            );
            if (!isAttendanceDateValidForGroup(selectedDate)) {
                showAlert(AlertType.WARNING, "Invalid Date",
                    "Attendance can only be marked from " + getAttendanceTrackingStartDate() + " onward.");
                return;
            }
            if (attendanceDatePicker != null && !selectedDate.equals(attendanceDatePicker.getValue())) {
                attendanceDatePicker.setValue(selectedDate);
            }

            if (!progressTrackerService.isWorkingDay(selectedAttendanceCourseId, selectedDate)) {
                showAlert(AlertType.WARNING, "Non-working Day",
                    "This date is not a working day. Click \"Mark All Present\" first.");
                refreshAttendanceTableAndCharts();
                return;
            }

            boolean present = "Present".equalsIgnoreCase(newValue);

            progressTrackerService.markAttendance(
                row.getStudentId(),
                selectedAttendanceCourseId,
                selectedDate,
                present
            );

            refreshAttendanceTableAndCharts();

            if (statusLabel != null) {
                statusLabel.setText("Updated " + row.getStudentName() + " as " + (present ? "Present" : "Absent")
                    + " for " + selectedDate + ".");
            }
        });

        attendanceTable.setRowFactory(tv -> new TableRow<AttendanceRow>() {
            @Override
            protected void updateItem(AttendanceRow item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");
            }
        });

        String courseTitle = groupDisplayName();
        if (courseTitle == null || courseTitle.isBlank()) {
            courseTitle = "Group " + groupId;
        }
        Course currentGroupCourse = new Course(groupId, "GRP-" + groupId, courseTitle);
        progressTrackerService.ensureCourseExists(
            currentGroupCourse.getId(),
            currentGroupCourse.getCode(),
            currentGroupCourse.getTitle()
        );

        List<Course> availableCourses = List.of(currentGroupCourse);
        attendanceCourseCombo.setItems(FXCollections.observableArrayList(availableCourses));
        configureAttendanceDatePickerBoundary();
        LocalDate initialDate = attendanceDatePicker.getValue() != null
            ? attendanceDatePicker.getValue()
            : LocalDate.now();
        attendanceDatePicker.setValue(normalizeAttendanceDate(initialDate));

        if (!availableCourses.isEmpty()) {
            Course selected = attendanceCourseCombo.getValue();
            if (selected == null) {
                attendanceCourseCombo.getSelectionModel().selectFirst();
                selected = attendanceCourseCombo.getValue();
            }
            if (selected != null) {
                selectedAttendanceCourseId = selected.getId();
                seedAttendanceDataForCourse(selected);
            }
        }

        refreshAttendanceTableAndCharts();
    }

    private void seedAttendanceDataForCourse(Course course) {
        if (course == null) {
            return;
        }

        progressTrackerService.ensureCourseExists(course.getId(), course.getCode(), course.getTitle());
        for (DatabaseManager.MemberData member : memberDataList) {
            int studentId = member.getId();
            String email = member.getEmail() != null ? member.getEmail() : ("student" + studentId + "@example.com");
            String name = member.getName() != null && !member.getName().isBlank() ? member.getName() : email;

            progressTrackerService.ensureStudentExists(studentId, name, email);
            progressTrackerService.enrollStudentInCourse(studentId, course.getId());
        }
    }

    private void refreshAttendanceTableAndCharts() {
        if (attendanceCourseCombo == null || attendanceDatePicker == null || attendanceTable == null) {
            return;
        }

        Course selectedCourse = attendanceCourseCombo.getValue();
        if (selectedCourse == null) {
            attendanceRows.clear();
            if (attendanceSummaryLabel != null) {
                attendanceSummaryLabel.setText("No course selected.");
            }
            if (attendanceOverviewBarChart != null) {
                attendanceOverviewBarChart.getData().clear();
            }
            return;
        }

        selectedAttendanceCourseId = selectedCourse.getId();
        seedAttendanceDataForCourse(selectedCourse);

        LocalDate selectedDate = normalizeAttendanceDate(
            attendanceDatePicker.getValue() != null ? attendanceDatePicker.getValue() : LocalDate.now()
        );
        if (!selectedDate.equals(attendanceDatePicker.getValue())) {
            attendanceDatePicker.setValue(selectedDate);
        }

        boolean isWorkingDay = progressTrackerService.isWorkingDay(selectedAttendanceCourseId, selectedDate);
        attendanceTable.setEditable(isWorkingDay);

        attendanceRows.clear();
        for (DatabaseManager.MemberData member : memberDataList) {
            int studentId = member.getId();
            String status = isWorkingDay
                ? getAttendanceStatusForDate(studentId, selectedAttendanceCourseId, selectedDate)
                : NON_WORKING_DAY_STATUS;
            double percent = progressTrackerService.getAttendancePercent(
                studentId,
                selectedAttendanceCourseId,
                getAttendanceTrackingStartDate()
            );
            attendanceRows.add(new AttendanceRow(studentId, member.getName(), status, percent));
        }

        attendanceTable.refresh();
        updateAttendanceSummary(selectedAttendanceCourseId, selectedDate);
        updateAttendanceOverviewChart(selectedAttendanceCourseId);
    }

    private LocalDate getAttendanceTrackingStartDate() {
        return attendanceTrackingStartDate != null ? attendanceTrackingStartDate : LocalDate.now();
    }

    private boolean isAttendanceDateValidForGroup(LocalDate date) {
        return date != null && !date.isBefore(getAttendanceTrackingStartDate()) && !date.isAfter(LocalDate.now());
    }

    private LocalDate normalizeAttendanceDate(LocalDate date) {
        LocalDate startDate = getAttendanceTrackingStartDate();
        LocalDate today = LocalDate.now();
        LocalDate effectiveDate = date != null ? date : today;
        if (effectiveDate.isBefore(startDate)) return startDate;
        if (effectiveDate.isAfter(today)) return today;
        return effectiveDate;
    }

    private void configureAttendanceDatePickerBoundary() {
        if (attendanceDatePicker == null) {
            return;
        }

        attendanceDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    return;
                }

                if (item.isBefore(getAttendanceTrackingStartDate()) || item.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8;");
                }
            }
        });
    }

    private String getAttendanceStatusForDate(int studentId, int courseId, LocalDate date) {
        if (!isAttendanceDateValidForGroup(date)) {
            return "N/A";
        }

        if (!progressTrackerService.isWorkingDay(courseId, date)) {
            return NON_WORKING_DAY_STATUS;
        }

        return progressTrackerService.isPresentOnDate(studentId, courseId, date, getAttendanceTrackingStartDate())
            ? "Present"
            : "Absent";
    }

    private void updateAttendanceSummary(int courseId, LocalDate date) {
        if (attendanceSummaryLabel == null) {
            return;
        }

        if (!progressTrackerService.isWorkingDay(courseId, date)) {
            attendanceSummaryLabel.setText(
                "This is not a working day. Mark all Present to start this as a working day. "
                    + "Then you can update attendance individually."
            );
            return;
        }

        List<Integer> studentIds = getAttendanceStudentIds();
        ProgressTrackerService.ClassAttendanceSummary summary =
            progressTrackerService.getClassAttendanceSummaryForDate(
                courseId,
                studentIds,
                date,
                getAttendanceTrackingStartDate()
            );

        int present = summary.getPresentCount();
        int absent = summary.getAbsentCount();
        int total = present + absent;
        double presentPct = total == 0 ? 0.0 : (present * 100.0) / total;

        attendanceSummaryLabel.setText(String.format(
            "Date %s: %d present, %d absent (%.1f%% present).",
            date,
            present,
            absent,
            presentPct
        ));
        attendanceSummaryLabel.setText(attendanceSummaryLabel.getText() +
            " Tracking start: " + getAttendanceTrackingStartDate());
    }

    private void updateAttendanceOverviewChart(int courseId) {
        if (attendanceOverviewBarChart == null) {
            return;
        }

        List<Integer> studentIds = getAttendanceStudentIds();
        Map<String, Double> weeklyAttendance = progressTrackerService.getClassWeeklyAttendance(
            courseId,
            studentIds,
            getAttendanceTrackingStartDate()
        );

        attendanceOverviewBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Class Weekly Attendance %");
        weeklyAttendance.forEach((week, pct) ->
            series.getData().add(new XYChart.Data<>(week, pct)));
        attendanceOverviewBarChart.getData().add(series);
    }

    private List<Integer> getAttendanceStudentIds() {
        List<Integer> studentIds = new ArrayList<>();
        for (DatabaseManager.MemberData member : memberDataList) {
            studentIds.add(member.getId());
        }
        return studentIds;
    }

    private boolean isAtRisk(double attendancePercent) {
        return attendancePercent < ATTENDANCE_RISK_THRESHOLD;
    }

    @FXML
    void handleAttendanceCourseChange(ActionEvent event) {
        Course selected = attendanceCourseCombo != null ? attendanceCourseCombo.getValue() : null;
        if (selected == null) {
            return;
        }

        selectedAttendanceCourseId = selected.getId();
        seedAttendanceDataForCourse(selected);
        refreshAttendanceTableAndCharts();
    }

    @FXML
    void handleAttendanceDateChange(ActionEvent event) {
        LocalDate selectedDate = attendanceDatePicker != null ? attendanceDatePicker.getValue() : null;
        if (!isAttendanceDateValidForGroup(selectedDate)) {
            LocalDate clamped = normalizeAttendanceDate(selectedDate);
            if (attendanceDatePicker != null) {
                attendanceDatePicker.setValue(clamped);
            }
            showAlert(AlertType.WARNING, "Invalid Date",
                "Attendance can only be marked between " + getAttendanceTrackingStartDate() + " and today " + LocalDate.now() + ".");
            selectedDate = clamped;
        }

        if (selectedDate != null && selectedAttendanceCourseId > 0
            && !progressTrackerService.isWorkingDay(selectedAttendanceCourseId, selectedDate)
            && statusLabel != null) {
            statusLabel.setText("This is not a working day. Click Mark All Present to activate it.");
        }

        refreshAttendanceTableAndCharts();
    }

    @FXML
    void handleRefreshAttendanceView(ActionEvent event) {
        refreshAttendanceTableAndCharts();
    }

    @FXML
    void handleMarkAllPresent(ActionEvent event) {
        markAttendanceForAll(true);
    }

    @FXML
    void handleMarkAllAbsent(ActionEvent event) {
        markAttendanceForAll(false);
    }

    private void markAttendanceForAll(boolean present) {
        if (selectedAttendanceCourseId <= 0 || attendanceDatePicker == null) {
            showAlert(AlertType.WARNING, "Missing Selection", "Please select a course and date first.");
            return;
        }

        LocalDate selectedDate = normalizeAttendanceDate(attendanceDatePicker.getValue());
        if (!isAttendanceDateValidForGroup(selectedDate)) {
            showAlert(AlertType.WARNING, "Invalid Date",
                "Attendance can only be marked from " + getAttendanceTrackingStartDate() + " onward.");
            return;
        }
        if (!selectedDate.equals(attendanceDatePicker.getValue())) {
            attendanceDatePicker.setValue(selectedDate);
        }

        boolean isWorkingDay = progressTrackerService.isWorkingDay(selectedAttendanceCourseId, selectedDate);
        if (!isWorkingDay && !present) {
            showAlert(AlertType.WARNING, "Non-working Day",
                "This date is not a working day. Click \"Mark All Present\" first.");
            refreshAttendanceTableAndCharts();
            return;
        }

        boolean activatedWorkingDay = false;
        if (!isWorkingDay && present) {
            progressTrackerService.ensureWorkingDayWithDefaultPresent(
                selectedAttendanceCourseId,
                getAttendanceStudentIds(),
                selectedDate
            );
            activatedWorkingDay = true;
        }

        progressTrackerService.markAttendanceForAll(
            selectedAttendanceCourseId,
            getAttendanceStudentIds(),
            selectedDate,
            present
        );

        refreshAttendanceTableAndCharts();
        if (statusLabel != null) {
            if (present && activatedWorkingDay) {
                statusLabel.setText("Marked " + selectedDate + " as Working Day and set all students Present.");
            } else {
                statusLabel.setText((present ? "Marked all present" : "Marked all absent") + " for " + selectedDate + ".");
            }
        }
    }

    @FXML
    void handleExportAttendanceCsv(ActionEvent event) {
        if (attendanceCourseCombo == null || attendanceDatePicker == null || attendanceTable == null) {
            return;
        }

        Course selectedCourse = attendanceCourseCombo.getValue();
        if (selectedCourse == null) {
            showAlert(AlertType.WARNING, "Missing Course", "Please select a course to export attendance.");
            return;
        }

        LocalDate selectedDate = attendanceDatePicker.getValue() != null
            ? attendanceDatePicker.getValue()
            : LocalDate.now();
        selectedDate = normalizeAttendanceDate(selectedDate);
        if (!isAttendanceDateValidForGroup(selectedDate)) {
            showAlert(AlertType.WARNING, "Invalid Date",
                "Attendance can only be exported from " + getAttendanceTrackingStartDate() + " onward.");
            return;
        }
        if (attendanceDatePicker != null && !selectedDate.equals(attendanceDatePicker.getValue())) {
            attendanceDatePicker.setValue(selectedDate);
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Attendance CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("attendance-" + selectedCourse.getCode() + "-" + selectedDate + ".csv");

        File destination = chooser.showSaveDialog(attendanceTable.getScene().getWindow());
        if (destination == null) {
            return;
        }

        refreshAttendanceTableAndCharts();

        List<String> lines = new ArrayList<>();
        lines.add("Course,Date,Student,Status,AttendancePercent,Risk");
        String courseDisplay = selectedCourse.getCode() + " - " + selectedCourse.getTitle();
        for (AttendanceRow row : attendanceRows) {
            lines.add(String.join(",",
                csvEscape(courseDisplay),
                csvEscape(selectedDate.toString()),
                csvEscape(row.getStudentName()),
                csvEscape(row.getStatus()),
                String.format("%.1f", row.getAttendancePercent()),
                csvEscape(isAtRisk(row.getAttendancePercent()) ? "At Risk" : "On Track")
            ));
        }

        try {
            Files.write(destination.toPath(), lines, StandardCharsets.UTF_8);
            showAlert(AlertType.INFORMATION, "Export Complete",
                "Attendance exported successfully to:\n" + destination.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert(AlertType.ERROR, "Export Failed", "Could not export CSV: " + ex.getMessage());
        }
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private void configureSubmissionsTable() {
        if (submissionsTable == null || colStudentName == null || colAssignmentName == null || colAssignmentId == null
            || colSubmissionTime == null || colStatus == null || colFile == null || colGrade == null) {
            return;
        }

        colStudentName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStudentName()));
        colAssignmentName.setCellValueFactory(c -> {
            String title = c.getValue().getAssignmentTitle();
            return new SimpleStringProperty(title != null && !title.isBlank() ? title : "-");
        });
        colAssignmentId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getAssetId())));
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

        submissionsTable.setRowFactory(tv -> new TableRow<DatabaseManager.SubmissionData>() {
            @Override
            protected void updateItem(DatabaseManager.SubmissionData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    Tooltip.install(this, null);
                } else if (item.isPlagiarized()) {
                    setStyle("-fx-background-color: #ffcccc;");
                    int matchId = item.getMatchedSubmissionId();
                    String tip = matchId > 0
                        ? String.format("High similarity (%.0f%%) with submission ID %d",
                            item.getSimilarityScore() * 100, matchId)
                        : String.format("High similarity detected (%.0f%%)",
                            item.getSimilarityScore() * 100);
                    Tooltip.install(this, new Tooltip(tip));
                } else if (item.isLate()) {
                    setStyle("-fx-background-color: #fff3cd;");
                    String deadline = item.getAssignmentDeadline() != null
                        ? item.getAssignmentDeadline().format(DT_FMT)
                        : "not set";
                    Tooltip.install(this, new Tooltip("Late submission. Deadline: " + deadline));
                } else if (item.isEvaluated()) {
                    setStyle("-fx-background-color: #d4edda;");
                    Tooltip.install(this, null);
                } else {
                    setStyle("");
                    Tooltip.install(this, null);
                }
            }
        });

        submissionsTable.setItems(submissionsObs);
        submissionsTable.setPlaceholder(new Label("No submissions found for this group."));
    }

    private void configureSubmissionsFilter() {
        if (submissionsFilterCombo == null) {
            return;
        }

        submissionsFilterCombo.setItems(FXCollections.observableArrayList(
            "All", "Graded", "Ungraded", "Late", "Plagiarized"
        ));
        submissionsFilterCombo.setOnAction(e -> applySubmissionFilter());

        if (submissionsFilterCombo.getValue() == null) {
            submissionsFilterCombo.setValue("All");
        }
    }

    private void loadSubmissionsForGroup() {
        try {
            allGroupSubmissions.clear();
            allGroupSubmissions.addAll(dbManager.getSubmissionsByGroup(groupId));

            applySubmissionFilter();
            updateSubmissionStats(allGroupSubmissions);
            populatePerformanceAssignmentOptions();
            refreshPerformanceDistributionChart();

            if (statusLabel != null) {
                statusLabel.setText("Loaded " + allGroupSubmissions.size() + " submission(s) from all assignments.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load submissions: " + e.getMessage());
        }
    }

    private void applySubmissionFilter() {
        String filter = submissionsFilterCombo != null && submissionsFilterCombo.getValue() != null
            ? submissionsFilterCombo.getValue()
            : "All";

        List<DatabaseManager.SubmissionData> filtered = new ArrayList<>();
        for (DatabaseManager.SubmissionData submission : allGroupSubmissions) {
            if (matchesSubmissionFilter(submission, filter)) {
                filtered.add(submission);
            }
        }

        submissionsObs.setAll(filtered);
    }

    private void configurePerformanceAnalyticsSection() {
        if (performanceAssignmentCombo == null || performanceDistributionBarChart == null) {
            return;
        }

        performanceAssignmentCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DatabaseManager.AssetData item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatAssetForAnalytics(item));
            }
        });
        performanceAssignmentCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DatabaseManager.AssetData item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatAssetForAnalytics(item));
            }
        });
        performanceAssignmentCombo.setOnAction(e -> refreshPerformanceDistributionChart());

        if (performanceDistributionBarChart.getXAxis() instanceof CategoryAxis) {
            CategoryAxis xAxis = (CategoryAxis) performanceDistributionBarChart.getXAxis();
            xAxis.setLabel("Score Range");
        }
        if (performanceDistributionBarChart.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) performanceDistributionBarChart.getYAxis();
            yAxis.setLabel("Number of Students");
            yAxis.setForceZeroInRange(true);
            yAxis.setAutoRanging(true);
        }

        performanceDistributionBarChart.setAnimated(false);
        performanceDistributionBarChart.setLegendVisible(false);
    }

    private String formatAssetForAnalytics(DatabaseManager.AssetData asset) {
        if (asset == null) {
            return "";
        }

        if (asset.getDeadline() == null) {
            return asset.getTitle() + " (No deadline)";
        }

        return asset.getTitle() + " (Due: " + asset.getDeadline().format(DT_FMT) + ")";
    }

    private void populatePerformanceAssignmentOptions() {
        if (performanceAssignmentCombo == null) {
            return;
        }

        Integer previousId = performanceAssignmentCombo.getValue() != null
            ? performanceAssignmentCombo.getValue().getId()
            : null;

        LocalDateTime now = LocalDateTime.now();
        List<DatabaseManager.AssetData> endedAssignments = new ArrayList<>();
        for (DatabaseManager.AssetData asset : assetDataList) {
            LocalDateTime deadline = asset.getDeadline();
            if (deadline != null && !deadline.isAfter(now)) {
                endedAssignments.add(asset);
            }
        }

        endedAssignments.sort((a, b) -> b.getDeadline().compareTo(a.getDeadline()));
        performanceAssignmentCombo.setItems(FXCollections.observableArrayList(endedAssignments));

        if (endedAssignments.isEmpty()) {
            performanceAssignmentCombo.getSelectionModel().clearSelection();
            if (performanceDistributionBarChart != null) {
                performanceDistributionBarChart.getData().clear();
            }
            if (performanceAssignmentHintLabel != null) {
                performanceAssignmentHintLabel.setText(
                    "No assignments with passed deadlines yet. Distribution appears after a deadline is over."
                );
            }
            if (performanceStatsLabel != null) {
                performanceStatsLabel.setText("Total Submissions: 0    Not Submitted: 0");
            }
            return;
        }

        DatabaseManager.AssetData selected = null;
        if (previousId != null) {
            for (DatabaseManager.AssetData asset : endedAssignments) {
                if (asset.getId() == previousId) {
                    selected = asset;
                    break;
                }
            }
        }

        if (selected == null) {
            selected = endedAssignments.get(0);
        }
        performanceAssignmentCombo.getSelectionModel().select(selected);
    }

    private void refreshPerformanceDistributionChart() {
        if (performanceAssignmentCombo == null || performanceDistributionBarChart == null) {
            return;
        }

        DatabaseManager.AssetData selectedAssignment = performanceAssignmentCombo.getValue();
        if (selectedAssignment == null) {
            performanceDistributionBarChart.getData().clear();
            if (performanceAssignmentHintLabel != null) {
                performanceAssignmentHintLabel.setText(
                    "Select an assignment with a passed deadline to view performance distribution."
                );
            }
            if (performanceStatsLabel != null) {
                performanceStatsLabel.setText("Total Submissions: 0    Not Submitted: 0");
            }
            return;
        }

        Map<Integer, DatabaseManager.SubmissionData> latestSubmissionByStudent = new HashMap<>();
        for (DatabaseManager.SubmissionData submission : allGroupSubmissions) {
            if (submission.getAssetId() != selectedAssignment.getId()) {
                continue;
            }

            DatabaseManager.SubmissionData current = latestSubmissionByStudent.get(submission.getStudentId());
            if (current == null || submission.getSubmissionTime().isAfter(current.getSubmissionTime())) {
                latestSubmissionByStudent.put(submission.getStudentId(), submission);
            }
        }

        int[] bucketCounts = new int[SCORE_RANGE_BUCKETS.length];
        int gradedCount = 0;

        for (DatabaseManager.SubmissionData submission : latestSubmissionByStudent.values()) {
            if (!submission.isEvaluated() || submission.getGrade() == null) {
                continue;
            }

            int score = Math.max(0, Math.min(100, submission.getGrade()));
            int bucketIndex = score <= 10 ? 0 : (score - 1) / 10;
            bucketCounts[bucketIndex]++;
            gradedCount++;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < SCORE_RANGE_BUCKETS.length; i++) {
            series.getData().add(new XYChart.Data<>(SCORE_RANGE_BUCKETS[i], bucketCounts[i]));
        }
        performanceDistributionBarChart.getData().clear();
        performanceDistributionBarChart.getData().add(series);

        int totalSubmissions = latestSubmissionByStudent.size();
        int notSubmitted = Math.max(0, memberDataList.size() - totalSubmissions);

        if (performanceAssignmentHintLabel != null) {
            String deadlineText = selectedAssignment.getDeadline() != null
                ? selectedAssignment.getDeadline().format(DT_FMT)
                : "No deadline";
            performanceAssignmentHintLabel.setText(
                "Distribution for " + selectedAssignment.getTitle() + " (Deadline: " + deadlineText + ")"
                    + (gradedCount == 0 ? " - No graded submissions yet." : "")
            );
        }

        if (performanceStatsLabel != null) {
            performanceStatsLabel.setText(
                "Total Submissions: " + totalSubmissions + "    Not Submitted: " + notSubmitted
            );
        }
    }

    private boolean matchesSubmissionFilter(DatabaseManager.SubmissionData submission, String filter) {
        return switch (filter) {
            case "Graded" -> submission.isEvaluated();
            case "Ungraded" -> !submission.isEvaluated();
            case "Late" -> submission.isLate();
            case "Plagiarized" -> submission.isPlagiarized();
            default -> true;
        };
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

    private void updateSubmissionStats(List<DatabaseManager.SubmissionData> fullList) {
        long graded = fullList.stream().filter(DatabaseManager.SubmissionData::isEvaluated).count();
        long pending = fullList.size() - graded;
        long suspicious = fullList.stream().filter(DatabaseManager.SubmissionData::isPlagiarized).count();
        int memberCount = memberDataList.size();

        if (statsLabel != null) {
            statsLabel.setText(String.format(
                "Members: %d  |  Submissions: %d  |  Graded: %d  |  Pending Review: %d  |  Plagiarism Suspected: %d",
                memberCount, fullList.size(), graded, pending, suspicious));
        }
    }

    private void updateMenuSelection(DashboardPage selectedPage) {
        setMenuButtonStyle(studentsMenuButton, selectedPage == DashboardPage.STUDENTS);
        setMenuButtonStyle(assignmentsMenuButton, selectedPage == DashboardPage.ASSIGNMENTS);
        setMenuButtonStyle(announcementsMenuButton, selectedPage == DashboardPage.ANNOUNCEMENTS);
        setMenuButtonStyle(discussionMenuButton, selectedPage == DashboardPage.DISCUSSION);
        setMenuButtonStyle(messagesMenuButton, selectedPage == DashboardPage.MESSAGES);
        setMenuButtonStyle(attendanceMenuButton, selectedPage == DashboardPage.ATTENDANCE);
        setMenuButtonStyle(submissionsMenuButton, selectedPage == DashboardPage.SUBMISSIONS);
    }

    private void setMenuButtonStyle(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setStyle(selected ? MENU_SELECTED_STYLE : MENU_NORMAL_STYLE);
    }

    private void loadGroupDetails() {
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
            String effectiveTeacherEmail = teacherEmail != null ? teacherEmail : groupData.getTeacherEmail();
            teacherEmail = effectiveTeacherEmail;
            teacherId = dbManager.getTeacherIdByEmail(effectiveTeacherEmail);
            if (groupData.getCreatedAt() != null) {
                attendanceTrackingStartDate = groupData.getCreatedAt().toLocalDateTime().toLocalDate();
            } else {
                attendanceTrackingStartDate = LocalDate.now();
            }

            loadMembers();
            loadAssets();
            loadCommunicationSection();
            updateMessagesMenuBadge();
            loadDashboardPage(DashboardPage.STUDENTS);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load group details: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Unexpected Error", "An error occurred: " + e.getMessage());
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
            for (DatabaseManager.MemberData data : memberDataList) {
                studentsObs.add(new StudentRow(data.getName(), data.getEmail()));
            }

            if (studentsTable != null) {
                studentsTable.setItems(studentsObs);
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
            assets.clear();
            assetDataList.clear();

            List<DatabaseManager.AssetData> assetList = dbManager.getGroupAssets(groupId);
            assetDataList.addAll(assetList);

            for (DatabaseManager.AssetData data : assetList) {
                String entry = data.getTitle() + " [" + data.getAssetType() + "]";
                if (data.getDeadline() != null) {
                    entry += "  |  Deadline: " + data.getDeadline().format(DT_FMT);
                }
                assets.add(entry);
            }

            if (sharedAssetsList != null) {
                sharedAssetsList.setItems(assets);
            }

            if (statusLabel != null) {
                statusLabel.setText(assets.isEmpty()
                    ? "No assets shared yet"
                    : assets.size() + " asset(s) shared");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load assets: " + e.getMessage());
        }
    }

    private void loadCommunicationSection() {
        if (teacherId <= 0) {
            return;
        }

        communicationService.ensureGeneralThread(groupId, teacherId);
        try {
            rebuildCourseUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to load course participants: " + e.getMessage());
            return;
        }

        loadAnnouncements();
        loadForumThreads();
        clearReplyState();
        updateMessagesMenuBadge();
    }

    private void rebuildCourseUsers() throws SQLException {
        courseUsers.clear();

        courseUsers.add(new CourseCommunicationService.CourseUser(
            teacherId,
            "TEACHER",
            teacherName,
            MentionUtils.usernameFromEmail(teacherEmail)
        ));

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
            Integer parentId = msg.getParentMessageID();
            if (parentId == null) {
                roots.add(msg);
            } else {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(msg);
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
    void handlePostAnnouncement(ActionEvent event) {
        String title = announcementTitleField != null ? announcementTitleField.getText().trim() : "";
        String message = announcementMessageArea != null ? announcementMessageArea.getText().trim() : "";

        if (title.isEmpty() || message.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Announcement title and message are required.");
            return;
        }

        try {
            List<DatabaseManager.MemberData> enrolledStudents = dbManager.getGroupMembers(groupId);
            communicationService.postAnnouncement(
                groupId,
                teacherId,
                title,
                message,
                groupDisplayName(),
                enrolledStudents,
                dbManager
            );

            announcementTitleField.clear();
            announcementMessageArea.clear();
            loadAnnouncements();
            if (statusLabel != null) {
                statusLabel.setText("Announcement posted successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to post announcement: " + e.getMessage());
        }
    }

    @FXML
    void handleCreateAssignmentThread(ActionEvent event) {
        String title = assignmentThreadTitleField != null
            ? assignmentThreadTitleField.getText().trim()
            : "";

        Integer assignmentId = selectedAssetId > 0 ? selectedAssetId : null;
        if (title.isEmpty()) {
            title = inferThreadTitleFromSelection();
        }

        if (title.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please provide a thread title or select an assignment.");
            return;
        }

        ForumThread created = communicationService.createAssignmentThread(
            groupId,
            teacherId,
            title,
            assignmentId
        );

        if (assignmentThreadTitleField != null) {
            assignmentThreadTitleField.clear();
        }
        activeForumThreadId = created.getThreadID();
        loadForumThreads();

        if (statusLabel != null) {
            statusLabel.setText("Discussion thread created.");
        }
    }

    private String inferThreadTitleFromSelection() {
        if (selectedAssetId <= 0) {
            return "";
        }
        for (DatabaseManager.AssetData asset : assetDataList) {
            if (asset.getId() == selectedAssetId) {
                return "Discussion for " + asset.getTitle();
            }
        }
        return "";
    }

    @FXML
    void handleForumThreadSelection(javafx.scene.input.MouseEvent event) {
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
            boolean canPost = communicationService.canUserPostInGroup(groupId, teacherId, "TEACHER", dbManager);
            if (!canPost) {
                showAlert(AlertType.WARNING, "Permission Denied",
                    "Only the group teacher or an enrolled student can post in this discussion.");
                return;
            }

            rebuildCourseUsers();
            communicationService.postMessage(
                groupId,
                activeForumThreadId,
                teacherId,
                "TEACHER",
                teacherName,
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
    void handleAssetSelection(javafx.scene.input.MouseEvent event) {
        if (sharedAssetsList == null) {
            return;
        }

        int idx = sharedAssetsList.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < assetDataList.size()) {
            // Keep assignment selection for discussion-thread naming only.
            // Do not refresh or mutate the submissions overview from this page.
            selectedAssetId = assetDataList.get(idx).getId();
        }
    }

    @FXML
    void handleDownloadSubmission(ActionEvent event) {
        if (submissionsTable == null) {
            return;
        }

        DatabaseManager.SubmissionData sub = submissionsTable.getSelectionModel().getSelectedItem();
        if (sub == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a submission to download");
            return;
        }
        Path sourcePath = Paths.get(sub.getFilePath());
        if (!Files.exists(sourcePath)) {
            showAlert(AlertType.ERROR, "File Not Found", "Submission file not found: " + sub.getFilePath());
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Submission");
        fc.setInitialFileName(sourcePath.getFileName().toString());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        File dest = fc.showSaveDialog(submissionsTable.getScene().getWindow());
        if (dest != null) {
            try {
                Files.copy(sourcePath, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert(AlertType.INFORMATION, "Downloaded",
                    "Submission saved to:\n" + dest.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Download Error", "Failed to save file: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleEvaluateSubmission(ActionEvent event) {
        if (submissionsTable == null) {
            return;
        }

        DatabaseManager.SubmissionData sub = submissionsTable.getSelectionModel().getSelectedItem();
        if (sub == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please select a submission to evaluate");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Evaluate Submission");
        dialog.setHeaderText("Student: " + sub.getStudentName() +
            "  |  Submitted: " + sub.getSubmissionTime().format(DT_FMT));

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField gradeField = new TextField();
        gradeField.setPromptText("0 - 100");
        if (sub.getGrade() != null) {
            gradeField.setText(String.valueOf(sub.getGrade()));
        }

        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Enter feedback for the student...");
        feedbackArea.setPrefRowCount(4);
        feedbackArea.setWrapText(true);
        if (sub.getFeedback() != null) {
            feedbackArea.setText(sub.getFeedback());
        }

        grid.add(new Label("Grade (0-100):"), 0, 0);
        grid.add(gradeField, 1, 0);
        grid.add(new Label("Feedback:"), 0, 1);
        grid.add(feedbackArea, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            String gradeText = gradeField.getText().trim();
            if (gradeText.isEmpty()) {
                showAlert(AlertType.WARNING, "Validation Error", "Please enter a grade (0-100)");
                return;
            }

            int grade;
            try {
                grade = Integer.parseInt(gradeText);
                if (grade < 0 || grade > 100) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Invalid Grade", "Grade must be a whole number between 0 and 100");
                return;
            }

            String feedback = feedbackArea.getText().trim();
            try {
                dbManager.saveEvaluation(sub.getId(), grade, feedback);
                progressTrackerService.notifyAssignmentDataChanged(sub.getStudentId(), groupId);

                String assignmentTitle = sub.getAssignmentTitle();
                String assetTitle = (assignmentTitle != null && !assignmentTitle.isBlank())
                    ? "\"" + assignmentTitle + "\""
                    : "your submission";

                try {
                    dbManager.createNotification(sub.getStudentId(), "STUDENT",
                        "Your assignment " + assetTitle + " has been graded: " + grade + "/100" +
                        (feedback.isEmpty() ? "." : ". Feedback: " + feedback));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                showAlert(AlertType.INFORMATION, "Saved", "Evaluation saved successfully!");
                loadSubmissionsForGroup();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Database Error", "Failed to save evaluation: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleRecordExamResult(ActionEvent event) {
        if (memberDataList.isEmpty()) {
            loadMembers();
        }

        if (memberDataList.isEmpty()) {
            showAlert(AlertType.WARNING, "No Students", "No students are enrolled in this group yet.");
            return;
        }

        DatabaseManager.SubmissionData selectedSubmission = submissionsTable != null
            ? submissionsTable.getSelectionModel().getSelectedItem()
            : null;
        Integer preselectedStudentId = selectedSubmission != null ? selectedSubmission.getStudentId() : null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Record Exam Result");
        dialog.setHeaderText("Add an exam/quiz result for a student in this group.");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 140, 10, 10));

        ComboBox<DatabaseManager.MemberData> studentCombo =
            new ComboBox<>(FXCollections.observableArrayList(memberDataList));
        studentCombo.setPrefWidth(260);
        studentCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DatabaseManager.MemberData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getEmail() + ")");
                }
            }
        });
        studentCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DatabaseManager.MemberData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getEmail() + ")");
                }
            }
        });

        if (preselectedStudentId != null) {
            for (DatabaseManager.MemberData member : memberDataList) {
                if (member.getId() == preselectedStudentId) {
                    studentCombo.getSelectionModel().select(member);
                    break;
                }
            }
        }
        if (studentCombo.getValue() == null) {
            studentCombo.getSelectionModel().selectFirst();
        }

        TextField examTitleField = new TextField();
        examTitleField.setPromptText("Example: Midterm Exam");

        TextField scoreField = new TextField();
        scoreField.setPromptText("Scored marks");

        TextField maxScoreField = new TextField("100");
        maxScoreField.setPromptText("Max marks");

        DatePicker examDatePicker = new DatePicker(LocalDate.now());
        examDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.isAfter(LocalDate.now())) {
                    setDisable(true);
                }
            }
        });

        grid.add(new Label("Student:"), 0, 0);
        grid.add(studentCombo, 1, 0);
        grid.add(new Label("Exam Title:"), 0, 1);
        grid.add(examTitleField, 1, 1);
        grid.add(new Label("Score:"), 0, 2);
        grid.add(scoreField, 1, 2);
        grid.add(new Label("Max Score:"), 0, 3);
        grid.add(maxScoreField, 1, 3);
        grid.add(new Label("Exam Date:"), 0, 4);
        grid.add(examDatePicker, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtn) {
            return;
        }

        DatabaseManager.MemberData selectedStudent = studentCombo.getValue();
        if (selectedStudent == null) {
            showAlert(AlertType.WARNING, "Validation Error", "Please select a student.");
            return;
        }

        String examTitle = examTitleField.getText() != null ? examTitleField.getText().trim() : "";
        if (examTitle.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter an exam title.");
            return;
        }

        double score;
        double maxScore;
        try {
            score = Double.parseDouble(scoreField.getText().trim());
            maxScore = Double.parseDouble(maxScoreField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Input", "Score and Max Score must be valid numbers.");
            return;
        }

        if (maxScore <= 0) {
            showAlert(AlertType.WARNING, "Validation Error", "Max Score must be greater than 0.");
            return;
        }

        if (score < 0 || score > maxScore) {
            showAlert(AlertType.WARNING, "Validation Error",
                "Score must be between 0 and Max Score.");
            return;
        }

        LocalDate examDate = examDatePicker.getValue() != null ? examDatePicker.getValue() : LocalDate.now();

        try {
            dbManager.addExamResult(groupId, selectedStudent.getId(), examTitle, score, maxScore, examDate);
            progressTrackerService.notifyExamDataChanged(selectedStudent.getId(), groupId);

            try {
                dbManager.createNotification(selectedStudent.getId(), "STUDENT",
                    String.format("Exam result recorded for \"%s\": %.1f/%.1f", examTitle, score, maxScore));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            if (statusLabel != null) {
                statusLabel.setText(String.format(
                    "Recorded exam result for %s: %s (%.1f/%.1f).",
                    selectedStudent.getName(), examTitle, score, maxScore
                ));
            }
            showAlert(AlertType.INFORMATION, "Saved", "Exam result saved successfully.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to save exam result: " + ex.getMessage());
        }
    }

    @FXML
    void handleBrowseFile(ActionEvent event) {
        if (assetFilePathField == null || assetTypeField == null) {
            return;
        }

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
            try {
                Path assignmentsDir = Paths.get("data", "assignments");
                Files.createDirectories(assignmentsDir);
                Path destination = assignmentsDir.resolve(selectedFile.getName());
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                assetFilePathField.setText("data/assignments/" + selectedFile.getName());
                String ext = selectedFile.getName();
                int dot = ext.lastIndexOf('.');
                assetTypeField.setText(dot >= 0 ? ext.substring(dot + 1).toUpperCase() : "File");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "File Copy Error",
                    "Failed to copy file to assignments folder: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleAddAsset(ActionEvent event) {
        String title = assetTitleField != null ? assetTitleField.getText().trim() : "";
        String description = assetDescriptionArea != null ? assetDescriptionArea.getText().trim() : "";
        String filePath = assetFilePathField != null ? assetFilePathField.getText().trim() : "";
        String assetType = assetTypeField != null ? assetTypeField.getText().trim() : "";

        if (title.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter an asset title");
            return;
        }
        if (filePath.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please select a file first");
            return;
        }
        if (assetType.isEmpty()) {
            assetType = "File";
        }

        LocalDateTime deadline = null;
        if (deadlineDatePicker != null && deadlineDatePicker.getValue() != null) {
            LocalDate date = deadlineDatePicker.getValue();
            LocalTime time = LocalTime.of(23, 59);
            if (deadlineTimeField != null && !deadlineTimeField.getText().trim().isEmpty()) {
                try {
                    time = LocalTime.parse(deadlineTimeField.getText().trim(), TIME_FMT);
                } catch (DateTimeParseException ex) {
                    showAlert(AlertType.WARNING, "Invalid Time", "Time must be in HH:mm format (e.g. 23:59)");
                    return;
                }
            }
            deadline = LocalDateTime.of(date, time);
        }

        try {
            dbManager.addEducationalAsset(groupId, title, description, filePath, assetType, deadline);
            String groupName = groupDisplayName();
            String deadlineStr = deadline != null
                ? " (Due: " + deadline.format(DT_FMT) + ")"
                : "";
            try {
                List<DatabaseManager.MemberData> members = dbManager.getGroupMembers(groupId);
                for (DatabaseManager.MemberData member : members) {
                    dbManager.createNotification(member.getId(), "STUDENT",
                        "New assignment in \"" + groupName + "\": " + title + deadlineStr);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            showAlert(AlertType.INFORMATION, "Success", "Assignment shared successfully!" +
                (deadline != null ? "\nDeadline: " + deadline.format(DT_FMT) : ""));

            if (assetTitleField != null) {
                assetTitleField.clear();
            }
            if (assetDescriptionArea != null) {
                assetDescriptionArea.clear();
            }
            if (assetFilePathField != null) {
                assetFilePathField.clear();
            }
            if (assetTypeField != null) {
                assetTypeField.clear();
            }
            if (deadlineDatePicker != null) {
                deadlineDatePicker.setValue(null);
            }
            if (deadlineTimeField != null) {
                deadlineTimeField.clear();
            }

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
