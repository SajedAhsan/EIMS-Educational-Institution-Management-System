package Student;

import database.DatabaseManager;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import progress.ProgressTrackerService;
import progress.model.Assignment;
import progress.model.Course;
import progress.model.Exam;

public class StudentProgressTrackerController {

    private static final boolean DEBUG_ASSIGNMENT_CHART = false;

    @FXML private ListView<Course> courseListView;
    @FXML private Label selectedCourseLabel;

    @FXML private Label noAttendanceLabel;
    @FXML private Label attendanceDetailsLabel;
    @FXML private Label attendanceWarningLabel;
    @FXML private PieChart attendancePieChart;

    @FXML private Label noAssignmentsLabel;
    @FXML private Label assignmentsSummaryLabel;
    @FXML private Label assignmentAverageLabel;
    @FXML private VBox assignmentColorLegendBox;
    @FXML private TableView<Assignment> assignmentsTable;
    @FXML private TableColumn<Assignment, String> colAssignmentTitle;
    @FXML private TableColumn<Assignment, String> colAssignmentScore;
    @FXML private TableColumn<Assignment, String> colAssignmentStatus;
    @FXML private BarChart<String, Number> assignmentScoreBarChart;

    @FXML private Label noExamsLabel;
    @FXML private Label examsSummaryLabel;
    @FXML private TableView<Exam> examsTable;
    @FXML private TableColumn<Exam, String> colExamTitle;
    @FXML private TableColumn<Exam, String> colExamScore;
    @FXML private TableColumn<Exam, String> colExamMaxScore;
    @FXML private BarChart<String, Number> examScoreBarChart;

    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final ProgressTrackerService progressService = ProgressTrackerService.getInstance();

    private final ProgressTrackerService.ProgressDataListener progressListener = this::handleProgressDataChanged;

    private String studentEmail;
    private int studentId = -1;
    private String studentName = "Student";
    private List<Course> studentCourses = new ArrayList<>();
    private boolean progressListenerRegistered;

    public void initialize() {
        rebuildAssignmentChartWithExplicitAxes();
        setupCourseListView();
        setupTableColumns();
        setupChartAxes();
        resetDashboardState("Select a course from the left.");
    }

    private void rebuildAssignmentChartWithExplicitAxes() {
        if (assignmentScoreBarChart == null) {
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 10);

        xAxis.setLabel("Assignment Number");
        yAxis.setLabel("Number Achieved");

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(100);
        yAxis.setTickUnit(10);

        xAxis.setTickLabelsVisible(true);
        yAxis.setTickLabelsVisible(true);
        xAxis.setTickMarkVisible(true);
        yAxis.setTickMarkVisible(true);
        xAxis.setOpacity(1.0);
        yAxis.setOpacity(1.0);

        BarChart<String, Number> rebuiltChart = new BarChart<>(xAxis, yAxis);
        rebuiltChart.setTitle("Assignments Performance");
        rebuiltChart.setLegendVisible(false);
        rebuiltChart.setAnimated(false);
        rebuiltChart.setPrefHeight(400);

        Node oldChart = assignmentScoreBarChart;
        if (oldChart.getParent() instanceof VBox) {
            VBox parent = (VBox) oldChart.getParent();
            int chartIndex = parent.getChildren().indexOf(oldChart);
            parent.getChildren().set(chartIndex, rebuiltChart);
            assignmentScoreBarChart = rebuiltChart;
        }
    }

    public void setStudentEmail(String email) {
        this.studentEmail = email;
        resolveStudentIdentity(email);

        progressService.ensureStudentExists(studentId, studentName, email);
        studentCourses = new ArrayList<>();

        try {
            List<DatabaseManager.GroupData> groups = dbManager.getGroupsByStudent(studentId);
            if (groups != null) {
                for (DatabaseManager.GroupData group : groups) {
                    Course course = new Course(group.getId(), "GRP-" + group.getId(), group.getName());
                    progressService.ensureCourseExists(course.getId(), course.getCode(), course.getTitle());
                    progressService.enrollStudentInCourse(studentId, course.getId());
                    studentCourses.add(course);
                }
            }
        } catch (SQLException ex) {
            showAlert(AlertType.ERROR, "Database Error", "Failed to load enrolled courses: " + ex.getMessage());
        }

        if (!progressListenerRegistered) {
            progressService.addProgressDataListener(progressListener);
            progressListenerRegistered = true;
        }

        loadCourses();
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentDashboard.fxml"));
            Parent root = loader.load();
            studentDashboardController ctrl = loader.getController();
            ctrl.setStudentEmail(studentEmail);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setWidth(1200.0);
            stage.setHeight(800.0);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to return to dashboard: " + e.getMessage());
        }
    }

    private void updateDashboardForCourse(Course course) {
        ProgressTrackerService.CourseMetrics metrics =
            progressService.getCourseMetrics(studentId, course.getId());

        int totalClassDays = metrics.getPresentDays() + metrics.getAbsentDays();
        List<Assignment> assignments = metrics.getAssignments();
        List<Exam> exams = metrics.getExams();
        boolean hasAssessments = !assignments.isEmpty() || !exams.isEmpty();

        if (selectedCourseLabel != null) {
            selectedCourseLabel.setText(course.getCode() + "  -  " + course.getTitle());
        }

        updateAttendanceSection(metrics, totalClassDays);
        updateAssignmentsSection(assignments);
        updateExamsSection(exams, hasAssessments);
    }

    private void updateAttendanceSection(ProgressTrackerService.CourseMetrics metrics, int totalClassDays) {
        boolean hasClasses = totalClassDays > 0;

        if (noAttendanceLabel != null) {
            noAttendanceLabel.setText("No classes conducted yet");
        }

        show(noAttendanceLabel, !hasClasses);
        show(attendanceDetailsLabel, hasClasses);
        show(attendancePieChart, hasClasses);
        show(attendanceWarningLabel, false);

        if (!hasClasses) {
            if (attendancePieChart != null) {
                attendancePieChart.getData().clear();
            }
            return;
        }

        int presentDays = metrics.getPresentDays();
        double presentPct = (presentDays * 100.0) / totalClassDays;
        double absentPct = 100.0 - presentPct;

        if (attendanceDetailsLabel != null) {
            attendanceDetailsLabel.setText(String.format(
                "Total classes: %d  |  Present: %.1f%%  |  Absent: %.1f%%",
                totalClassDays,
                presentPct,
                absentPct
            ));
        }

        if (attendanceWarningLabel != null) {
            attendanceWarningLabel.setText("Warning: Attendance below 80%");
            attendanceWarningLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            show(attendanceWarningLabel, absentPct >= 20.0);
        }

        if (attendancePieChart != null) {
            attendancePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data(String.format("Present %.1f%%", presentPct), presentPct),
                new PieChart.Data(String.format("Absent %.1f%%", absentPct), absentPct)
            ));

            Platform.runLater(() -> {
                var data = attendancePieChart.getData();
                if (data.size() >= 2) {
                    applySliceColor(data.get(0), "#22c55e");
                    applySliceColor(data.get(1), "#ef4444");
                }
                for (PieChart.Data d : data) {
                    if (d.getNode() != null) {
                        Tooltip.install(d.getNode(), new Tooltip(d.getName()));
                    }
                }
            });
        }
    }

    private void updateAssignmentsSection(List<Assignment> assignments) {
        boolean hasAssignments = !assignments.isEmpty();

        int gradedAssignments = 0;
        int underEvaluationAssignments = 0;
        int notSubmittedAssignments = 0;
        double gradedScoreTotal = 0.0;
        for (Assignment assignment : assignments) {
            String status = resolveAssignmentStatus(assignment);
            if ("Graded".equalsIgnoreCase(status) && assignment.getScore() != null) {
                gradedAssignments++;
                gradedScoreTotal += toScoreOutOf100(assignment.getScore(), assignment.getMaxScore());
            } else if (isPendingEvaluationStatus(status)) {
                underEvaluationAssignments++;
            } else {
                notSubmittedAssignments++;
            }
        }

        boolean hasGradedAssignments = gradedAssignments > 0;

        if (noAssignmentsLabel != null) {
            noAssignmentsLabel.setText("No assignments given so far");
        }

        show(noAssignmentsLabel, !hasAssignments);
        show(assignmentsSummaryLabel, hasAssignments);
        show(assignmentAverageLabel, hasAssignments);
        show(assignmentColorLegendBox, hasAssignments);
        show(assignmentsTable, hasAssignments);
        show(assignmentScoreBarChart, hasAssignments);

        if (assignmentsTable != null) {
            assignmentsTable.setItems(FXCollections.observableArrayList(assignments));
        }

        if (assignmentScoreBarChart != null) {
            assignmentScoreBarChart.getData().clear();
        }
        if (assignmentAverageLabel != null) {
            assignmentAverageLabel.setText("Average Score: --");
        }

        if (!hasAssignments) {
            return;
        }

        if (assignmentsSummaryLabel != null) {
            assignmentsSummaryLabel.setText(String.format(
                "Assignments: %d  |  Graded: %d  |  Under Evaluation: %d  |  Not Submitted: %d",
                assignments.size(),
                gradedAssignments,
                underEvaluationAssignments,
                notSubmittedAssignments
            ));
        }
        if (assignmentAverageLabel != null) {
            assignmentAverageLabel.setText(hasGradedAssignments
                ? String.format("Average Score: %s", formatScoreValue(gradedScoreTotal / gradedAssignments))
                : "Average Score: --");
        }

        if (assignmentScoreBarChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Score");

            var dataPoints = FXCollections.<XYChart.Data<String, Number>>observableArrayList();
            var categories = FXCollections.<String>observableArrayList();

            for (int index = 0; index < assignments.size(); index++) {
                Assignment assignment = assignments.get(index);
                String status = resolveAssignmentStatus(assignment);
                double score;
                if ("Graded".equalsIgnoreCase(status) && assignment.getScore() != null) {
                    score = toScoreOutOf100(assignment.getScore(), assignment.getMaxScore());
                } else {
                    score = 0.0;
                }

                String assignmentCategory = String.valueOf(index + 1);
                categories.add(assignmentCategory);

                dataPoints.add(new XYChart.Data<>(
                    assignmentCategory,
                    score
                ));
            }

            if (assignmentScoreBarChart.getXAxis() instanceof CategoryAxis) {
                CategoryAxis xAxis = (CategoryAxis) assignmentScoreBarChart.getXAxis();
                xAxis.setCategories(categories);
                if (DEBUG_ASSIGNMENT_CHART) {
                    System.out.println("Assignment categories: " + categories);
                }
            }

            series.setData(dataPoints);

            assignmentScoreBarChart.getData().clear();
            assignmentScoreBarChart.getData().add(series);
            applyAssignmentBarStyles(series, assignments);
        }
    }

    private void updateExamsSection(List<Exam> exams, boolean hasAssessments) {
        boolean hasExams = !exams.isEmpty();

        int takenExams = 0;
        double takenExamScoreTotal = 0.0;
        for (Exam exam : exams) {
            if (exam.getScore() != null) {
                takenExams++;
                takenExamScoreTotal += toScoreOutOf100(exam.getScore(), exam.getMaxScore());
            }
        }

        boolean hasGradedExams = takenExams > 0;

        if (noExamsLabel != null) {
            noExamsLabel.setText(hasAssessments
                ? "No exams available."
                : "No assignments/exams taken so far");
        }

        show(noExamsLabel, !hasExams);
        show(examsSummaryLabel, hasExams);
        show(examsTable, hasExams);
        show(examScoreBarChart, hasExams);

        if (examsTable != null) {
            examsTable.setItems(FXCollections.observableArrayList(exams));
        }

        if (examScoreBarChart != null) {
            examScoreBarChart.getData().clear();
        }

        if (!hasExams) {
            return;
        }

        if (examsSummaryLabel != null) {
            String avgText = hasGradedExams
                ? String.format("%.1f / 100", takenExamScoreTotal / takenExams)
                : "No graded exams yet";
            examsSummaryLabel.setText(String.format(
                "Exams: %d  |  Evaluated: %d  |  Average: %s",
                exams.size(),
                takenExams,
                avgText
            ));
        }

        if (examScoreBarChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Score");

            for (int index = 0; index < exams.size(); index++) {
                Exam exam = exams.get(index);
                double score = exam.getScore() != null
                    ? toScoreOutOf100(exam.getScore(), exam.getMaxScore())
                    : 0.0;

                series.getData().add(new XYChart.Data<>(
                    String.valueOf(index + 1),
                    score
                ));
            }

            examScoreBarChart.getData().clear();
            examScoreBarChart.getData().add(series);
            applyExamBarStyles(series, exams);
        }
    }

    private void setupChartAxes() {
        configureChartAxes(assignmentScoreBarChart, "Assignment Number", "Number Achieved");
        configureChartAxes(examScoreBarChart, "Exam Number", "Number Achieved");
    }

    private void configureChartAxes(BarChart<String, Number> chart, String xLabel, String yLabel) {
        if (chart == null) {
            return;
        }

        if (chart.getXAxis() instanceof CategoryAxis) {
            CategoryAxis axis = (CategoryAxis) chart.getXAxis();
            axis.setLabel(xLabel);
            axis.setTickLabelsVisible(true);
            axis.setTickMarkVisible(true);
            axis.setOpacity(1.0);
        }

        if (chart.getYAxis() instanceof NumberAxis) {
            NumberAxis axis = (NumberAxis) chart.getYAxis();
            axis.setLabel(yLabel);
            axis.setAutoRanging(false);
            axis.setLowerBound(0);
            axis.setUpperBound(100);
            axis.setTickUnit(10);
            axis.setTickLabelsVisible(true);
            axis.setTickMarkVisible(true);
            axis.setOpacity(1.0);
        }

        chart.setLegendVisible(false);
        if (chart == assignmentScoreBarChart) {
            chart.setTitle("Assignments Performance");
            chart.setPrefHeight(400);
        }
    }

    private void setupCourseListView() {
        if (courseListView == null) {
            return;
        }

        courseListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        courseListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateDashboardForCourse(newVal);
                }
            });
    }

    private void setupTableColumns() {
        if (assignmentsTable != null) {
            assignmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            if (colAssignmentTitle != null) {
                colAssignmentTitle.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getTitle()));
            }

            if (colAssignmentScore != null) {
                colAssignmentScore.setCellValueFactory(c -> {
                    Assignment assignment = c.getValue();
                    String status = resolveAssignmentStatus(assignment);
                    if ("Graded".equalsIgnoreCase(status) && assignment.getScore() != null) {
                        return new SimpleStringProperty(
                            String.format("%.0f", toScoreOutOf100(assignment.getScore(), assignment.getMaxScore()))
                        );
                    }
                    if ("Not Submitted".equalsIgnoreCase(status)) {
                        return new SimpleStringProperty("0");
                    }
                    return new SimpleStringProperty(
                        "-"
                    );
                });
            }

            if (colAssignmentStatus != null) {
                colAssignmentStatus.setCellValueFactory(c ->
                    new SimpleStringProperty(resolveAssignmentStatus(c.getValue())));
                colAssignmentStatus.setCellFactory(column -> new TableCell<Assignment, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }

                        setText(item);
                        if ("Under Evaluation".equalsIgnoreCase(item)) {
                            setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #92400e; -fx-font-weight: bold;");
                        } else if ("Not Submitted".equalsIgnoreCase(item)) {
                            setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
                        } else if ("Graded".equalsIgnoreCase(item)) {
                            setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                });
            }
        }

        if (examsTable != null) {
            examsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            if (colExamTitle != null) {
                colExamTitle.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getTitle()));
            }

            if (colExamScore != null) {
                colExamScore.setCellValueFactory(c -> {
                    Exam exam = c.getValue();
                    return new SimpleStringProperty(
                        exam.getScore() != null
                            ? String.format("%.0f", toScoreOutOf100(exam.getScore(), exam.getMaxScore()))
                            : "--"
                    );
                });
            }

            if (colExamMaxScore != null) {
                colExamMaxScore.setCellValueFactory(c ->
                    new SimpleStringProperty(String.format("%.0f", c.getValue().getMaxScore())));
            }
        }
    }

    private void handleProgressDataChanged(ProgressTrackerService.ProgressDataEvent event) {
        if (event == null || studentId <= 0 || courseListView == null) {
            return;
        }

        Course selected = courseListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        if (!event.matches(studentId, selected.getId())) {
            return;
        }

        Platform.runLater(this::refreshCurrentCourse);
    }

    private void resolveStudentIdentity(String email) {
        try {
            int id = dbManager.getStudentIdByEmail(email);
            if (id > 0) {
                studentId = id;
            }

            DatabaseManager.StudentProfileData profile = dbManager.getStudentProfile(email);
            if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
                studentName = profile.getName();
            } else if (email != null && !email.isBlank()) {
                studentName = email;
            }
        } catch (SQLException ex) {
            if (email != null && !email.isBlank()) {
                studentName = email;
            }
        }

        if (studentId <= 0) {
            studentId = Math.abs((email == null ? "student" : email).hashCode());
        }
    }

    private void loadCourses() {
        if (courseListView == null) {
            return;
        }

        courseListView.setItems(FXCollections.observableArrayList(studentCourses));
        if (!studentCourses.isEmpty()) {
            courseListView.getSelectionModel().selectFirst();
        } else {
            resetDashboardState("No enrolled courses found.");
        }
    }

    public void addAssignmentResult(int courseId, String title, double scoreOutOf100) {
        progressService.addAssignment(studentId, courseId, title, 100.0, scoreOutOf100);
        refreshCurrentCourse();
    }

    public void addExamResult(int courseId, String title, double scoreOutOf100) {
        progressService.addExamResult(studentId, courseId, title, 100.0, scoreOutOf100, LocalDate.now());
        refreshCurrentCourse();
    }

    public void updateAttendance(int courseId, LocalDate date, boolean present) {
        progressService.ensureWorkingDayWithDefaultPresent(courseId, List.of(studentId), date);
        progressService.markAttendance(studentId, courseId, date, present);
        refreshCurrentCourse();
    }

    private void refreshCurrentCourse() {
        Course selected = courseListView != null
            ? courseListView.getSelectionModel().getSelectedItem()
            : null;
        if (selected != null) {
            updateDashboardForCourse(selected);
        }
    }

    private void resetDashboardState(String courseMessage) {
        if (selectedCourseLabel != null) {
            selectedCourseLabel.setText(courseMessage);
        }

        if (noAttendanceLabel != null) {
            noAttendanceLabel.setText("No classes conducted yet");
        }
        show(noAttendanceLabel, true);
        show(attendanceDetailsLabel, false);
        show(attendanceWarningLabel, false);
        show(attendancePieChart, false);
        if (attendancePieChart != null) {
            attendancePieChart.getData().clear();
        }

        if (noAssignmentsLabel != null) {
            noAssignmentsLabel.setText("No assignments given so far");
        }
        show(noAssignmentsLabel, true);
        show(assignmentsSummaryLabel, false);
        show(assignmentAverageLabel, false);
        show(assignmentColorLegendBox, false);
        show(assignmentsTable, false);
        show(assignmentScoreBarChart, false);
        if (assignmentsTable != null) {
            assignmentsTable.getItems().clear();
        }
        if (assignmentScoreBarChart != null) {
            assignmentScoreBarChart.getData().clear();
        }
        if (assignmentAverageLabel != null) {
            assignmentAverageLabel.setText("Average Score: --");
        }

        if (noExamsLabel != null) {
            noExamsLabel.setText("No assignments/exams taken so far");
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

    private static void show(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static void applySliceColor(PieChart.Data slice, String hexColor) {
        if (slice.getNode() != null) {
            slice.getNode().setStyle("-fx-pie-color: " + hexColor + ";");
        }
    }

    private String resolveAssignmentStatus(Assignment assignment) {
        if (assignment == null) {
            return "Not Submitted";
        }

        String status = assignment.getStatus();
        if (status == null || status.isBlank()) {
            return assignment.getScore() != null ? "Graded" : "Not Submitted";
        }
        return status;
    }

    private double toScoreOutOf100(Double score, double maxScore) {
        if (score == null) {
            return 0.0;
        }
        if (maxScore <= 0) {
            return Math.max(0.0, Math.min(100.0, score));
        }

        double normalized = (score / maxScore) * 100.0;
        return Math.max(0.0, Math.min(100.0, normalized));
    }

    private void applyAssignmentBarStyles(XYChart.Series<String, Number> series, List<Assignment> assignments) {
        if (series == null || assignments == null) {
            return;
        }

        Platform.runLater(() -> {
            List<XYChart.Data<String, Number>> bars = series.getData();
            for (int i = 0; i < bars.size() && i < assignments.size(); i++) {
                XYChart.Data<String, Number> data = bars.get(i);
                Assignment assignment = assignments.get(i);
                String status = resolveAssignmentStatus(assignment);
                double score = data.getYValue().doubleValue();

                if (data.getNode() != null) {
                    boolean pendingEvaluation = isPendingEvaluationStatus(status);
                    if (pendingEvaluation) {
                        data.getNode().setStyle("-fx-bar-fill: #94a3b8;");
                    } else {
                        data.getNode().setStyle("-fx-bar-fill: " + resolveAssignmentBarColor(score) + ";");
                    }

                    Tooltip.install(data.getNode(), createChartTooltip(
                        "Assignment " + (i + 1),
                        pendingEvaluation
                            ? "Submitted, not graded yet"
                            : String.format("Score: %s marks", formatScoreValue(score))
                    ));
                }
            }
        });
    }

    private void applyExamBarStyles(XYChart.Series<String, Number> series, List<Exam> exams) {
        if (series == null || exams == null) {
            return;
        }

        Platform.runLater(() -> {
            List<XYChart.Data<String, Number>> bars = series.getData();
            for (int i = 0; i < bars.size() && i < exams.size(); i++) {
                XYChart.Data<String, Number> data = bars.get(i);
                Exam exam = exams.get(i);
                boolean graded = exam.getScore() != null;

                if (data.getNode() != null) {
                    data.getNode().setStyle(graded
                        ? "-fx-bar-fill: #0ea5e9;"
                        : "-fx-bar-fill: #94a3b8;");

                    Tooltip.install(data.getNode(), createChartTooltip(
                        exam.getTitle(),
                        (graded ? "Evaluated" : "Under Evaluation")
                            + " | Score: " + formatScoreValue(data.getYValue().doubleValue()) + "/100"
                    ));
                }
            }
        });
    }

    private boolean isPendingEvaluationStatus(String status) {
        return "Under Evaluation".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status);
    }

    private String resolveAssignmentBarColor(double scoreOutOf100) {
        if (scoreOutOf100 < 40.0) {
            return "#ef4444";
        }
        if (scoreOutOf100 < 60.0) {
            return "#eab308";
        }
        if (scoreOutOf100 < 80.0) {
            return "#22c55e";
        }
        return "#a855f7";
    }

    private Tooltip createChartTooltip(String title, String detail) {
        return new Tooltip(title + "\n" + detail);
    }

    private String formatScoreValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
