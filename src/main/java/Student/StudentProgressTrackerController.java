package Student;

import database.DatabaseManager;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import progress.ProgressTrackerService;
import progress.model.Assignment;
import progress.model.Course;
import progress.model.Exam;

public class StudentProgressTrackerController {

    // â”€â”€ Sidebar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private ListView<Course> courseListView;

    // â”€â”€ Course header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label selectedCourseLabel;

    // â”€â”€ Attendance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label noAttendanceLabel;
    @FXML private Label attendanceDetailsLabel;
    @FXML private PieChart attendancePieChart;

    // â”€â”€ Assignments â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label noAssignmentsLabel;
    @FXML private Label assignmentsSummaryLabel;
    @FXML private TableView<Assignment> assignmentsTable;
    @FXML private TableColumn<Assignment, String> colAssignmentTitle;
    @FXML private TableColumn<Assignment, String> colAssignmentScore;
    @FXML private TableColumn<Assignment, String> colAssignmentMaxScore;
    @FXML private TableColumn<Assignment, String> colAssignmentStatus;
    @FXML private BarChart<String, Number> assignmentScoreBarChart;

    // â”€â”€ Exams â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label noExamsLabel;
    @FXML private Label examsSummaryLabel;
    @FXML private TableView<Exam> examsTable;
    @FXML private TableColumn<Exam, String> colExamTitle;
    @FXML private TableColumn<Exam, String> colExamScore;
    @FXML private TableColumn<Exam, String> colExamMaxScore;
    @FXML private BarChart<String, Number> examScoreBarChart;

    // â”€â”€ State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final ProgressTrackerService progressService = ProgressTrackerService.getInstance();

    private String studentEmail;
    private int    studentId   = -1;
    private String studentName = "Student";
    private List<Course> studentCourses = new ArrayList<>();

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Lifecycle
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    public void initialize() {
        setupCourseListView();
        setupTableColumns();
        resetDashboardState("Select a course from the left.");
    }

    /**
     * Called by the parent controller (studentDashboardController) after FXML load.
     * Resolves the student identity, loads courses from DB, and selects the first one.
     */
    public void setStudentEmail(String email) {
        this.studentEmail = email;
        resolveStudentIdentity(email);

        progressService.ensureStudentExists(studentId, studentName, email);
        studentCourses = new ArrayList<>();
        try {
            List<DatabaseManager.GroupData> groups = dbManager.getGroupsByStudent(studentId);
            if (groups != null) {
                for (DatabaseManager.GroupData g : groups) {
                    Course course = new Course(g.getId(), "GRP-" + g.getId(), g.getName());
                    progressService.ensureCourseExists(course.getId(), course.getCode(), course.getTitle());
                    studentCourses.add(course);
                }
            }
        } catch (SQLException ex) {
            showAlert(AlertType.ERROR, "Database Error", "Failed to load enrolled courses: " + ex.getMessage());
        }

        loadCourses();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Navigation
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

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
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to return to dashboard: " + e.getMessage());
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Dashboard update  (called when a course is selected)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /** Updates every section for the chosen course â€” called on sidebar click. */
    private void updateDashboardForCourse(Course course) {
        ProgressTrackerService.CourseMetrics metrics =
            progressService.getCourseMetrics(studentId, course.getId());
        List<Assignment> assignments = loadAssignmentsForCourse(course.getId());
        int totalClassDays = progressService.getTotalClassDays(course.getId());

        if (selectedCourseLabel != null) {
            selectedCourseLabel.setText(course.getCode() + "  â€”  " + course.getTitle());
        }

        updateAttendanceSection(metrics, totalClassDays);
        updateAssignmentsSection(assignments);
        updateExamsSection();
    }

    // â”€â”€ Attendance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void updateAttendanceSection(ProgressTrackerService.CourseMetrics metrics, int totalClassDays) {
        boolean hasData = totalClassDays > 0;

        show(noAttendanceLabel,    !hasData);
        show(attendanceDetailsLabel, hasData);
        show(attendancePieChart,     hasData);

        if (!hasData) return;

        int present = metrics.getPresentDays();
        int absent  = metrics.getAbsentDays();

        if (attendanceDetailsLabel != null) {
            attendanceDetailsLabel.setText(String.format(
                "Total class days: %d  |  Present: %d  |  Absent: %d  |  Attendance: %.1f%%",
                totalClassDays, present, absent, metrics.getAttendancePercent()));
        }

        if (attendancePieChart != null) {
            attendancePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Present (" + present + " days)", present),
                new PieChart.Data("Absent ("  + absent  + " days)", absent)
            ));
            // Apply green / red colours after JavaFX has built the nodes.
            Platform.runLater(() -> {
                var data = attendancePieChart.getData();
                if (data.size() >= 2) {
                    applySliceColor(data.get(0), "#22c55e"); // green â€“ present
                    applySliceColor(data.get(1), "#ef4444"); // red   â€“ absent
                }
                for (PieChart.Data d : data) {
                    if (d.getNode() != null) {
                        Tooltip.install(d.getNode(), new Tooltip(d.getName()));
                    }
                }
            });
        }
    }

    // â”€â”€ Assignments â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void updateAssignmentsSection(List<Assignment> assignments) {
        boolean hasAssignments = !assignments.isEmpty();
        int gradedAssignments = 0;
        double totalScorePercent = 0.0;

        for (Assignment assignment : assignments) {
            if (assignment.isCompleted()) {
                gradedAssignments++;
                totalScorePercent += assignment.getScorePercent();
            }
        }

        boolean hasGrades = gradedAssignments > 0;

        if (noAssignmentsLabel != null) {
            noAssignmentsLabel.setText("No assignment given so far.");
        }

        show(noAssignmentsLabel, !hasAssignments);
        show(assignmentsSummaryLabel, hasAssignments);
        show(assignmentsTable, hasAssignments);
        show(assignmentScoreBarChart, hasAssignments && hasGrades);

        if (assignmentsTable != null) {
            assignmentsTable.setItems(FXCollections.observableArrayList(assignments));
        }

        if (assignmentScoreBarChart != null) {
            assignmentScoreBarChart.getData().clear();
        }

        if (!hasAssignments) {
            return;
        }

        if (assignmentsSummaryLabel != null) {
            String averageText = hasGrades
                ? String.format("%.1f / 100", totalScorePercent / gradedAssignments)
                : "-- / 100";
            assignmentsSummaryLabel.setText(String.format(
                "Assignments given: %d  |  Graded: %d / %d  |  Average: %s",
                assignments.size(),
                gradedAssignments,
                assignments.size(),
                averageText));
        }

        if (assignmentScoreBarChart != null && hasGrades) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Grade / 100");
            for (Assignment assignment : assignments) {
                if (assignment.isCompleted()) {
                    series.getData().add(new XYChart.Data<>(
                        assignment.getTitle(),
                        assignment.getScorePercent()));
                }
            }
            assignmentScoreBarChart.getData().add(series);
            applyBarTooltips(series, "%");
        }
    }

    // â”€â”€ Exams â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void updateExamsSection() {
        if (noExamsLabel != null) {
            noExamsLabel.setText("No exams taken so far.");
        }
        show(noExamsLabel, true);
        show(examsSummaryLabel, false);
        show(examsTable, false);
        show(examScoreBarChart, false);

        if (examsTable != null) {
            examsTable.getItems().clear();
        }
        if (examScoreBarChart != null) {
            examScoreBarChart.getData().clear();
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Setup helpers
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private void setupCourseListView() {
        if (courseListView == null) return;
        courseListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        courseListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) updateDashboardForCourse(newVal);
            });
    }

    private void setupTableColumns() {
        if (assignmentsTable != null) {
            if (colAssignmentTitle != null)
                colAssignmentTitle.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getTitle()));
            if (colAssignmentScore != null)
                colAssignmentScore.setCellValueFactory(c -> {
                    Assignment a = c.getValue();
                    return new SimpleStringProperty(a.isCompleted()
                        ? String.format("%.0f", a.getScore()) : "--");
                });
            if (colAssignmentMaxScore != null)
                colAssignmentMaxScore.setCellValueFactory(c ->
                    new SimpleStringProperty(String.format("%.0f", c.getValue().getMaxScore())));
            if (colAssignmentStatus != null)
                colAssignmentStatus.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getStatus()));
        }

        if (examsTable != null) {
            if (colExamTitle != null)
                colExamTitle.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getTitle()));
            if (colExamScore != null)
                colExamScore.setCellValueFactory(c -> {
                    Exam e = c.getValue();
                    return new SimpleStringProperty(e.isTaken()
                        ? String.format("%.0f", e.getScore()) : "--");
                });
            if (colExamMaxScore != null)
                colExamMaxScore.setCellValueFactory(c ->
                    new SimpleStringProperty(String.format("%.0f", c.getValue().getMaxScore())));
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Identity resolution
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private void resolveStudentIdentity(String email) {
        try {
            int id = dbManager.getStudentIdByEmail(email);
            if (id > 0) studentId = id;
            DatabaseManager.StudentProfileData profile = dbManager.getStudentProfile(email);
            if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
                studentName = profile.getName();
            } else if (email != null && !email.isBlank()) {
                studentName = email;
            }
        } catch (SQLException ex) {
            if (email != null && !email.isBlank()) studentName = email;
        }
        if (studentId <= 0) {
            studentId = Math.abs((email == null ? "student" : email).hashCode());
        }
    }

    private void loadCourses() {
        if (courseListView != null) {
            courseListView.setItems(FXCollections.observableArrayList(studentCourses));
            if (!studentCourses.isEmpty()) {
                courseListView.getSelectionModel().selectFirst();
            } else {
                resetDashboardState("No enrolled courses found.");
            }
        }
    }

    private List<Assignment> loadAssignmentsForCourse(int courseId) {
        List<Assignment> assignments = new ArrayList<>();
        try {
            List<DatabaseManager.AssetData> assets = dbManager.getGroupAssets(courseId);
            Map<Integer, DatabaseManager.SubmissionData> submissionsByAssetId = new LinkedHashMap<>();

            for (DatabaseManager.SubmissionData submission : dbManager.getSubmissionsByGroupAndStudent(courseId, studentId)) {
                submissionsByAssetId.put(submission.getAssetId(), submission);
            }

            for (DatabaseManager.AssetData asset : assets) {
                DatabaseManager.SubmissionData submission = submissionsByAssetId.get(asset.getId());
                Double score = null;
                String status = "Not Submitted";

                if (submission != null) {
                    if (submission.isEvaluated() && submission.getGrade() != null) {
                        score = submission.getGrade().doubleValue();
                        status = "Graded";
                    } else {
                        status = "Submitted";
                    }
                }

                assignments.add(new Assignment(
                    asset.getId(),
                    courseId,
                    asset.getTitle(),
                    100.0,
                    score,
                    status
                ));
            }
        } catch (SQLException ex) {
            showAlert(AlertType.ERROR, "Database Error", "Failed to load assignment progress: " + ex.getMessage());
        }
        return assignments;
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Public API â€” allows other controllers to push live data updates
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    public void addAssignmentResult(int courseId, String title, double scoreOutOf100) {
        progressService.addAssignment(studentId, courseId, title, 100.0, scoreOutOf100);
        refreshCurrentCourse();
    }

    public void addExamResult(int courseId, String title, double scoreOutOf100) {
        progressService.addExam(studentId, courseId, title, 100.0, scoreOutOf100);
        refreshCurrentCourse();
    }

    public void updateAttendance(int courseId, LocalDate date, boolean present) {
        progressService.markAttendance(studentId, courseId, date, present);
        refreshCurrentCourse();
    }

    private void refreshCurrentCourse() {
        Course selected = courseListView != null
            ? courseListView.getSelectionModel().getSelectedItem() : null;
        if (selected != null) updateDashboardForCourse(selected);
    }

    private void resetDashboardState(String courseMessage) {
        if (selectedCourseLabel != null) {
            selectedCourseLabel.setText(courseMessage);
        }

        if (noAttendanceLabel != null) {
            noAttendanceLabel.setText("No attendance recorded yet. Attendance is marked by the teacher; absent by default.");
        }
        show(noAttendanceLabel, true);
        show(attendanceDetailsLabel, false);
        show(attendancePieChart, false);
        if (attendancePieChart != null) {
            attendancePieChart.getData().clear();
        }

        if (noAssignmentsLabel != null) {
            noAssignmentsLabel.setText("No assignment given so far.");
        }
        show(noAssignmentsLabel, true);
        show(assignmentsSummaryLabel, false);
        show(assignmentsTable, false);
        show(assignmentScoreBarChart, false);
        if (assignmentsTable != null) {
            assignmentsTable.getItems().clear();
        }
        if (assignmentScoreBarChart != null) {
            assignmentScoreBarChart.getData().clear();
        }

        if (noExamsLabel != null) {
            noExamsLabel.setText("No exams taken so far.");
        }
        show(noExamsLabel, true);
        show(examsSummaryLabel, false);
        show(examsTable, false);
        show(examScoreBarChart, false);
        if (examsTable != null) {
            examsTable.getItems().clear();
        }
        if (examScoreBarChart != null) {
            examScoreBarChart.getData().clear();
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Utility helpers
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /** Show or hide a node (also toggles managed so it takes no space when hidden). */
    private static void show(javafx.scene.Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** Apply an inline CSS fill colour to a pie slice node. */
    private static void applySliceColor(PieChart.Data slice, String hexColor) {
        if (slice.getNode() != null) {
            slice.getNode().setStyle("-fx-pie-color: " + hexColor + ";");
        }
    }

    private void applyBarTooltips(XYChart.Series<String, Number> series, String suffix) {
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    Tooltip.install(d.getNode(), new Tooltip(
                        d.getXValue() + ": "
                        + String.format("%.1f", d.getYValue().doubleValue()) + suffix));
                }
            }
        });
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
