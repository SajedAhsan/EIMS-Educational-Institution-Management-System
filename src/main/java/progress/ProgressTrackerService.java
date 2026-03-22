package progress;

import database.DatabaseManager;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import progress.model.Assignment;
import progress.model.AttendanceRecord;
import progress.model.Course;
import progress.model.Exam;
import progress.model.Student;

/**
 * Shared progress tracker service used by teacher and student dashboards.
 *
 * Data is sourced from the database (working days, attendance, submissions, exam results)
 * so page reloads do not reset progress.
 */
public class ProgressTrackerService {

    public interface ProgressDataListener {
        void onProgressDataChanged(ProgressDataEvent event);
    }

    public enum ProgressDataType {
        ATTENDANCE,
        ASSIGNMENT,
        EXAM
    }

    public static class ProgressDataEvent {
        private final ProgressDataType type;
        private final int studentId;
        private final int courseId;
        private final LocalDateTime changedAt;

        public ProgressDataEvent(ProgressDataType type, int studentId, int courseId) {
            this.type = type;
            this.studentId = studentId;
            this.courseId = courseId;
            this.changedAt = LocalDateTime.now();
        }

        public ProgressDataType getType() {
            return type;
        }

        public int getStudentId() {
            return studentId;
        }

        public int getCourseId() {
            return courseId;
        }

        public LocalDateTime getChangedAt() {
            return changedAt;
        }

        public boolean matches(int targetStudentId, int targetCourseId) {
            boolean studentMatches = studentId <= 0 || targetStudentId <= 0 || studentId == targetStudentId;
            boolean courseMatches = courseId <= 0 || targetCourseId <= 0 || courseId == targetCourseId;
            return studentMatches && courseMatches;
        }
    }

    public static class CourseMetrics {
        private final Course course;
        private final int presentDays;
        private final int absentDays;
        private final double attendancePercent;
        private final Map<String, Double> attendanceByWeek;
        private final List<Assignment> assignments;
        private final int completedAssignments;
        private final int totalAssignments;
        private final double assignmentAveragePercent;
        private final List<Exam> exams;
        private final int examsTaken;
        private final double examAveragePercent;
        private final double overallGradePercent;
        private final double attendanceContribution;
        private final double assignmentContribution;
        private final double examContribution;

        public CourseMetrics(Course course,
                             int presentDays,
                             int absentDays,
                             double attendancePercent,
                             Map<String, Double> attendanceByWeek,
                             List<Assignment> assignments,
                             int completedAssignments,
                             int totalAssignments,
                             double assignmentAveragePercent,
                             List<Exam> exams,
                             int examsTaken,
                             double examAveragePercent,
                             double overallGradePercent,
                             double attendanceContribution,
                             double assignmentContribution,
                             double examContribution) {
            this.course = course;
            this.presentDays = presentDays;
            this.absentDays = absentDays;
            this.attendancePercent = attendancePercent;
            this.attendanceByWeek = attendanceByWeek;
            this.assignments = assignments;
            this.completedAssignments = completedAssignments;
            this.totalAssignments = totalAssignments;
            this.assignmentAveragePercent = assignmentAveragePercent;
            this.exams = exams;
            this.examsTaken = examsTaken;
            this.examAveragePercent = examAveragePercent;
            this.overallGradePercent = overallGradePercent;
            this.attendanceContribution = attendanceContribution;
            this.assignmentContribution = assignmentContribution;
            this.examContribution = examContribution;
        }

        public Course getCourse() {
            return course;
        }

        public int getPresentDays() {
            return presentDays;
        }

        public int getAbsentDays() {
            return absentDays;
        }

        public double getAttendancePercent() {
            return attendancePercent;
        }

        public Map<String, Double> getAttendanceByWeek() {
            return attendanceByWeek;
        }

        public List<Assignment> getAssignments() {
            return assignments;
        }

        public int getCompletedAssignments() {
            return completedAssignments;
        }

        public int getTotalAssignments() {
            return totalAssignments;
        }

        public double getAssignmentAveragePercent() {
            return assignmentAveragePercent;
        }

        public List<Exam> getExams() {
            return exams;
        }

        public int getExamsTaken() {
            return examsTaken;
        }

        public double getExamAveragePercent() {
            return examAveragePercent;
        }

        public double getOverallGradePercent() {
            return overallGradePercent;
        }

        public double getAttendanceContribution() {
            return attendanceContribution;
        }

        public double getAssignmentContribution() {
            return assignmentContribution;
        }

        public double getExamContribution() {
            return examContribution;
        }
    }

    public static class OverallMetrics {
        private final double overallAttendancePercent;
        private final double assignmentCompletionPercent;
        private final double overallExamAveragePercent;
        private final Map<String, Double> courseGradeComparison;

        public OverallMetrics(double overallAttendancePercent,
                              double assignmentCompletionPercent,
                              double overallExamAveragePercent,
                              Map<String, Double> courseGradeComparison) {
            this.overallAttendancePercent = overallAttendancePercent;
            this.assignmentCompletionPercent = assignmentCompletionPercent;
            this.overallExamAveragePercent = overallExamAveragePercent;
            this.courseGradeComparison = courseGradeComparison;
        }

        public double getOverallAttendancePercent() {
            return overallAttendancePercent;
        }

        public double getAssignmentCompletionPercent() {
            return assignmentCompletionPercent;
        }

        public double getOverallExamAveragePercent() {
            return overallExamAveragePercent;
        }

        public Map<String, Double> getCourseGradeComparison() {
            return courseGradeComparison;
        }
    }

    public static class ClassAttendanceSummary {
        private final int presentCount;
        private final int absentCount;

        public ClassAttendanceSummary(int presentCount, int absentCount) {
            this.presentCount = presentCount;
            this.absentCount = absentCount;
        }

        public int getPresentCount() {
            return presentCount;
        }

        public int getAbsentCount() {
            return absentCount;
        }
    }

    private static final ProgressTrackerService INSTANCE = new ProgressTrackerService();

    private static final double ATTENDANCE_WEIGHT = 0.20;
    private static final double ASSIGNMENT_WEIGHT = 0.40;
    private static final double EXAM_WEIGHT = 0.40;

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    private final Map<Integer, Student> studentsById = new LinkedHashMap<>();
    private final Map<Integer, Course> coursesById = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> courseIdsByStudent = new HashMap<>();
    private final List<ProgressDataListener> listeners = new CopyOnWriteArrayList<>();

    private ProgressTrackerService() {
    }

    public static ProgressTrackerService getInstance() {
        return INSTANCE;
    }

    public void addProgressDataListener(ProgressDataListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeProgressDataListener(ProgressDataListener listener) {
        listeners.remove(listener);
    }

    public void notifyAssignmentDataChanged(int studentId, int courseId) {
        notifyDataChanged(ProgressDataType.ASSIGNMENT, studentId, courseId);
    }

    public void notifyExamDataChanged(int studentId, int courseId) {
        notifyDataChanged(ProgressDataType.EXAM, studentId, courseId);
    }

    public synchronized void ensureStudentExists(int studentId, String name, String email) {
        if (studentId <= 0 || studentsById.containsKey(studentId)) {
            return;
        }
        studentsById.put(studentId, new Student(studentId, name, email));
    }

    public synchronized void ensureCourseExists(int courseId, String code, String title) {
        if (courseId <= 0 || coursesById.containsKey(courseId)) {
            return;
        }
        coursesById.put(courseId, new Course(courseId, code, title));
    }

    public synchronized List<Course> getAllCourses() {
        return new ArrayList<>(coursesById.values());
    }

    public synchronized void enrollStudentInCourse(int studentId, int courseId) {
        if (studentId <= 0 || courseId <= 0) {
            return;
        }
        courseIdsByStudent.computeIfAbsent(studentId, k -> new LinkedHashSet<>()).add(courseId);
    }

    public synchronized void initializeMockDataForStudent(int studentId, String name, String email) {
        ensureStudentExists(studentId, name, email);
    }

    public synchronized void initializeMockDataForCourse(int studentId, int courseId) {
        enrollStudentInCourse(studentId, courseId);
    }

    public synchronized List<Course> getCoursesForStudent(int studentId) {
        syncCoursesFromDatabase(studentId);

        Set<Integer> courseIds = courseIdsByStudent.getOrDefault(studentId, Collections.emptySet());
        List<Course> courses = new ArrayList<>();
        for (Integer courseId : courseIds) {
            Course c = resolveCourse(courseId);
            if (c != null) {
                courses.add(c);
            }
        }
        courses.sort(Comparator.comparing(Course::getCode));
        return courses;
    }

    public synchronized List<Assignment> getAssignments(int studentId, int courseId) {
        List<Assignment> assignments = new ArrayList<>();
        try {
            List<DatabaseManager.AssetData> assets = dbManager.getGroupAssets(courseId);
            Map<Integer, DatabaseManager.SubmissionData> submissionsByAssetId = new HashMap<>();
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
                        status = "Under Evaluation";
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
        } catch (SQLException ignored) {
            return Collections.emptyList();
        }

        assignments.sort(Comparator.comparing(Assignment::getTitle));
        return assignments;
    }

    public synchronized List<Exam> getExams(int studentId, int courseId) {
        List<Exam> exams = new ArrayList<>();
        try {
            List<DatabaseManager.ExamResultData> rows = dbManager.getExamResultsByGroupAndStudent(courseId, studentId);
            for (DatabaseManager.ExamResultData row : rows) {
                exams.add(new Exam(
                    row.getId(),
                    row.getGroupId(),
                    row.getTitle(),
                    row.getMaxScore(),
                    row.getScore()
                ));
            }
        } catch (SQLException ignored) {
            return Collections.emptyList();
        }

        exams.sort(Comparator.comparing(Exam::getId));
        return exams;
    }

    public synchronized List<AttendanceRecord> getAttendanceRecords(int studentId, int courseId) {
        List<AttendanceRecord> records = new ArrayList<>();
        try {
            List<DatabaseManager.AttendanceRecordData> rows = dbManager.getAttendanceRecordsByGroupAndStudent(courseId, studentId);
            for (DatabaseManager.AttendanceRecordData row : rows) {
                records.add(new AttendanceRecord(
                    row.getGroupId(),
                    row.getStudentId(),
                    row.getClassDate(),
                    row.isPresent()
                ));
            }
        } catch (SQLException ignored) {
            return Collections.emptyList();
        }

        records.sort(Comparator.comparing(AttendanceRecord::getDate));
        return records;
    }

    // Legacy compatibility API.
    public synchronized void addAssignment(int studentId,
                                           int courseId,
                                           String title,
                                           double maxScore,
                                           Double score) {
        notifyDataChanged(ProgressDataType.ASSIGNMENT, studentId, courseId);
    }

    public synchronized void addExam(int studentId,
                                     int courseId,
                                     String title,
                                     double maxScore,
                                     Double score) {
        addExamResult(studentId, courseId, title, maxScore, score, LocalDate.now());
    }

    public synchronized void addExamResult(int studentId,
                                           int courseId,
                                           String title,
                                           double maxScore,
                                           Double score,
                                           LocalDate examDate) {
        try {
            dbManager.addExamResult(courseId, studentId, title, score, maxScore, examDate);
            notifyDataChanged(ProgressDataType.EXAM, studentId, courseId);
        } catch (SQLException ignored) {
            // Ignore write failures here; UI callers should surface DB errors where needed.
        }
    }

    public synchronized boolean isWorkingDay(int courseId, LocalDate date) {
        if (date == null || courseId <= 0) {
            return false;
        }
        try {
            return dbManager.isWorkingDay(courseId, date);
        } catch (SQLException ignored) {
            return false;
        }
    }

    public synchronized void ensureWorkingDayWithDefaultPresent(int courseId,
                                                                List<Integer> studentIds,
                                                                LocalDate date) {
        if (courseId <= 0 || date == null) {
            return;
        }

        try {
            boolean wasWorking = dbManager.isWorkingDay(courseId, date);
            dbManager.ensureWorkingDayWithDefaultPresent(courseId, studentIds, date);
            if (!wasWorking) {
                notifyDataChanged(ProgressDataType.ATTENDANCE, -1, courseId);
            }
        } catch (SQLException ignored) {
            // Ignore write failures here; UI callers should surface DB errors where needed.
        }
    }

    public synchronized void markAttendance(int studentId,
                                            int courseId,
                                            LocalDate date,
                                            boolean present) {
        if (studentId <= 0 || courseId <= 0 || date == null) {
            return;
        }

        try {
            dbManager.saveAttendanceRecord(courseId, studentId, date, present);
            notifyDataChanged(ProgressDataType.ATTENDANCE, studentId, courseId);
        } catch (SQLException ignored) {
            // Ignore write failures here; UI callers should surface DB errors where needed.
        }
    }

    public synchronized void markAttendanceForAll(int courseId,
                                                  List<Integer> studentIds,
                                                  LocalDate date,
                                                  boolean present) {
        if (courseId <= 0 || date == null || studentIds == null || studentIds.isEmpty()) {
            return;
        }

        try {
            dbManager.saveAttendanceForAll(courseId, studentIds, date, present);
            notifyDataChanged(ProgressDataType.ATTENDANCE, -1, courseId);
        } catch (SQLException ignored) {
            // Ignore write failures here; UI callers should surface DB errors where needed.
        }
    }

    public synchronized int getTotalClassDays(int courseId) {
        return getWorkingDaysAfterDate(courseId, null).size();
    }

    public synchronized boolean isPresentOnDate(int studentId, int courseId, LocalDate date) {
        return isPresentOnDate(studentId, courseId, date, null);
    }

    public synchronized boolean isPresentOnDate(int studentId,
                                                int courseId,
                                                LocalDate date,
                                                LocalDate attendanceStartDate) {
        if (date == null || courseId <= 0 || studentId <= 0) {
            return false;
        }
        if (attendanceStartDate != null && date.isBefore(attendanceStartDate)) {
            return false;
        }
        if (!isWorkingDay(courseId, date)) {
            return false;
        }

        try {
            String status = dbManager.getAttendanceStatus(courseId, studentId, date);
            return "Present".equalsIgnoreCase(status);
        } catch (SQLException ignored) {
            return false;
        }
    }

    public synchronized double getAttendancePercent(int studentId, int courseId) {
        return getAttendancePercent(studentId, courseId, null);
    }

    public synchronized double getAttendancePercent(int studentId,
                                                    int courseId,
                                                    LocalDate attendanceStartDate) {
        List<LocalDate> workingDays = getWorkingDaysAfterDate(courseId, attendanceStartDate);
        if (workingDays.isEmpty()) {
            return 0.0;
        }

        Set<LocalDate> workingDaySet = new HashSet<>(workingDays);
        int presentCount = 0;
        for (AttendanceRecord record : getAttendanceRecords(studentId, courseId)) {
            LocalDate day = record.getDate();
            if (record.isPresent()
                && (attendanceStartDate == null || !day.isBefore(attendanceStartDate))
                && workingDaySet.contains(day)) {
                presentCount++;
            }
        }

        return (presentCount * 100.0) / workingDays.size();
    }

    public synchronized CourseMetrics getCourseMetrics(int studentId, int courseId) {
        Course course = resolveCourse(courseId);

        List<LocalDate> workingDays = getWorkingDaysAfterDate(courseId, null);
        Set<LocalDate> presentDays = getPresentDays(studentId, courseId, null);

        int present = 0;
        for (LocalDate day : workingDays) {
            if (presentDays.contains(day)) {
                present++;
            }
        }

        int totalClassDays = workingDays.size();
        int absent = Math.max(0, totalClassDays - present);
        double attendancePct = totalClassDays == 0 ? 0.0 : (present * 100.0) / totalClassDays;

        List<Assignment> assignments = getAssignments(studentId, courseId);
        int completedAssignments = 0;
        double assignmentScoreSum = 0.0;
        int assignmentScoreCount = 0;
        for (Assignment assignment : assignments) {
            if (assignment.getScore() != null) {
                completedAssignments++;
                assignmentScoreSum += assignment.getScorePercent();
                assignmentScoreCount++;
            }
        }
        int totalAssignments = assignments.size();
        double assignmentAvgPct = assignmentScoreCount == 0 ? 0.0 : assignmentScoreSum / assignmentScoreCount;

        List<Exam> exams = getExams(studentId, courseId);
        int examsTaken = 0;
        double examScoreSum = 0.0;
        for (Exam exam : exams) {
            if (exam.isTaken()) {
                examsTaken++;
                examScoreSum += exam.getScorePercent();
            }
        }
        double examAvgPct = examsTaken == 0 ? 0.0 : examScoreSum / examsTaken;

        double attendanceContribution = attendancePct * ATTENDANCE_WEIGHT;
        double assignmentContribution = assignmentAvgPct * ASSIGNMENT_WEIGHT;
        double examContribution = examAvgPct * EXAM_WEIGHT;
        double overallGrade = attendanceContribution + assignmentContribution + examContribution;

        return new CourseMetrics(
            course,
            present,
            absent,
            attendancePct,
            buildWeeklyAttendance(workingDays, presentDays),
            assignments,
            completedAssignments,
            totalAssignments,
            assignmentAvgPct,
            exams,
            examsTaken,
            examAvgPct,
            overallGrade,
            attendanceContribution,
            assignmentContribution,
            examContribution
        );
    }

    public synchronized OverallMetrics getOverallMetrics(int studentId) {
        List<Course> courses = getCoursesForStudent(studentId);
        if (courses.isEmpty()) {
            return new OverallMetrics(0.0, 0.0, 0.0, Collections.emptyMap());
        }

        int totalPresent = 0;
        int totalAttendanceRows = 0;
        int totalAssignments = 0;
        int completedAssignments = 0;
        double examSum = 0.0;
        int examCount = 0;

        Map<String, Double> courseComparison = new LinkedHashMap<>();
        for (Course course : courses) {
            CourseMetrics metrics = getCourseMetrics(studentId, course.getId());
            totalPresent += metrics.getPresentDays();
            totalAttendanceRows += metrics.getPresentDays() + metrics.getAbsentDays();
            totalAssignments += metrics.getTotalAssignments();
            completedAssignments += metrics.getCompletedAssignments();

            for (Exam exam : metrics.getExams()) {
                if (exam.isTaken()) {
                    examSum += exam.getScorePercent();
                    examCount++;
                }
            }

            courseComparison.put(course.getCode(), metrics.getOverallGradePercent());
        }

        double overallAttendance = totalAttendanceRows == 0 ? 0.0 : (totalPresent * 100.0) / totalAttendanceRows;
        double assignmentCompletion = totalAssignments == 0 ? 0.0 : (completedAssignments * 100.0) / totalAssignments;
        double examAverage = examCount == 0 ? 0.0 : examSum / examCount;

        return new OverallMetrics(overallAttendance, assignmentCompletion, examAverage, courseComparison);
    }

    public synchronized Map<String, Double> getClassWeeklyAttendance(int courseId, List<Integer> studentIds) {
        return getClassWeeklyAttendance(courseId, studentIds, null);
    }

    public synchronized Map<String, Double> getClassWeeklyAttendance(int courseId,
                                                                     List<Integer> studentIds,
                                                                     LocalDate attendanceStartDate) {
        LinkedHashMap<String, Double> result = emptyWeekMap();
        if (studentIds == null || studentIds.isEmpty()) {
            return result;
        }

        List<LocalDate> workingDays = getWorkingDaysAfterDate(courseId, attendanceStartDate);
        if (workingDays.isEmpty()) {
            return result;
        }

        Map<String, Integer> presentByWeek = emptyWeekCounter();
        Map<String, Integer> totalByWeek = emptyWeekCounter();
        LocalDate today = LocalDate.now();

        Map<Integer, Set<LocalDate>> presentDaysByStudent = new HashMap<>();
        for (Integer studentId : studentIds) {
            if (studentId == null || studentId <= 0) {
                continue;
            }
            presentDaysByStudent.put(studentId, getPresentDays(studentId, courseId, attendanceStartDate));
        }

        for (LocalDate workingDay : workingDays) {
            String bucket = toWeekBucket(workingDay, today);
            if (bucket == null) {
                continue;
            }

            for (Integer studentId : studentIds) {
                if (studentId == null || studentId <= 0) {
                    continue;
                }
                totalByWeek.put(bucket, totalByWeek.get(bucket) + 1);
                Set<LocalDate> presentDays = presentDaysByStudent.get(studentId);
                if (presentDays != null && presentDays.contains(workingDay)) {
                    presentByWeek.put(bucket, presentByWeek.get(bucket) + 1);
                }
            }
        }

        for (String week : result.keySet()) {
            int present = presentByWeek.get(week);
            int total = totalByWeek.get(week);
            result.put(week, total == 0 ? 0.0 : (present * 100.0) / total);
        }

        return result;
    }

    public synchronized ClassAttendanceSummary getClassAttendanceSummaryForDate(int courseId,
                                                                                List<Integer> studentIds,
                                                                                LocalDate date) {
        return getClassAttendanceSummaryForDate(courseId, studentIds, date, null);
    }

    public synchronized ClassAttendanceSummary getClassAttendanceSummaryForDate(int courseId,
                                                                                List<Integer> studentIds,
                                                                                LocalDate date,
                                                                                LocalDate attendanceStartDate) {
        if (date == null) {
            return new ClassAttendanceSummary(0, 0);
        }
        if (attendanceStartDate != null && date.isBefore(attendanceStartDate)) {
            return new ClassAttendanceSummary(0, 0);
        }
        if (studentIds == null || studentIds.isEmpty()) {
            return new ClassAttendanceSummary(0, 0);
        }
        if (!isWorkingDay(courseId, date)) {
            return new ClassAttendanceSummary(0, 0);
        }

        int present = 0;
        int absent = 0;
        for (Integer studentId : studentIds) {
            if (studentId == null || studentId <= 0) {
                continue;
            }
            if (isPresentOnDate(studentId, courseId, date, attendanceStartDate)) {
                present++;
            } else {
                absent++;
            }
        }
        return new ClassAttendanceSummary(present, absent);
    }

    private void syncCoursesFromDatabase(int studentId) {
        if (studentId <= 0) {
            return;
        }

        try {
            List<DatabaseManager.GroupData> groups = dbManager.getGroupsByStudent(studentId);
            for (DatabaseManager.GroupData group : groups) {
                if (group == null) {
                    continue;
                }
                ensureCourseExists(group.getId(), "GRP-" + group.getId(), group.getName());
                enrollStudentInCourse(studentId, group.getId());
            }
        } catch (SQLException ignored) {
            // Best effort sync.
        }
    }

    private Course resolveCourse(int courseId) {
        Course existing = coursesById.get(courseId);
        if (existing != null) {
            return existing;
        }

        try {
            DatabaseManager.GroupDetailData group = dbManager.getGroupById(courseId);
            if (group != null) {
                Course resolved = new Course(courseId, "GRP-" + courseId, group.getName());
                coursesById.put(courseId, resolved);
                return resolved;
            }
        } catch (SQLException ignored) {
            // Fall back to generic title.
        }

        Course fallback = new Course(courseId, "GRP-" + courseId, "Group " + courseId);
        coursesById.put(courseId, fallback);
        return fallback;
    }

    private List<LocalDate> getWorkingDaysAfterDate(int courseId, LocalDate attendanceStartDate) {
        try {
            List<LocalDate> days = dbManager.getWorkingDays(courseId);
            if (attendanceStartDate == null) {
                return days;
            }

            List<LocalDate> filtered = new ArrayList<>();
            for (LocalDate day : days) {
                if (!day.isBefore(attendanceStartDate)) {
                    filtered.add(day);
                }
            }
            return filtered;
        } catch (SQLException ignored) {
            return Collections.emptyList();
        }
    }

    private Set<LocalDate> getPresentDays(int studentId, int courseId, LocalDate attendanceStartDate) {
        Set<LocalDate> presentDays = new HashSet<>();
        for (AttendanceRecord record : getAttendanceRecords(studentId, courseId)) {
            LocalDate date = record.getDate();
            if (record.isPresent() && (attendanceStartDate == null || !date.isBefore(attendanceStartDate))) {
                presentDays.add(date);
            }
        }
        return presentDays;
    }

    private static LinkedHashMap<String, Double> emptyWeekMap() {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        result.put("Week-1", 0.0);
        result.put("Week-2", 0.0);
        result.put("Week-3", 0.0);
        result.put("Week-4", 0.0);
        return result;
    }

    private static LinkedHashMap<String, Integer> emptyWeekCounter() {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        result.put("Week-1", 0);
        result.put("Week-2", 0);
        result.put("Week-3", 0);
        result.put("Week-4", 0);
        return result;
    }

    private static Map<String, Double> buildWeeklyAttendance(List<LocalDate> workingDays,
                                                              Set<LocalDate> presentDays) {
        LinkedHashMap<String, Double> result = emptyWeekMap();
        if (workingDays == null || workingDays.isEmpty()) {
            return result;
        }

        Map<String, Integer> presentByWeek = emptyWeekCounter();
        Map<String, Integer> totalByWeek = emptyWeekCounter();
        LocalDate today = LocalDate.now();

        for (LocalDate workingDay : workingDays) {
            String bucket = toWeekBucket(workingDay, today);
            if (bucket == null) {
                continue;
            }

            totalByWeek.put(bucket, totalByWeek.get(bucket) + 1);
            if (presentDays != null && presentDays.contains(workingDay)) {
                presentByWeek.put(bucket, presentByWeek.get(bucket) + 1);
            }
        }

        for (String week : result.keySet()) {
            int present = presentByWeek.get(week);
            int total = totalByWeek.get(week);
            result.put(week, total == 0 ? 0.0 : (present * 100.0) / total);
        }

        return result;
    }

    private static String toWeekBucket(LocalDate date, LocalDate today) {
        LocalDate startOfDateWeek = date.with(DayOfWeek.MONDAY);
        LocalDate startOfCurrentWeek = today.with(DayOfWeek.MONDAY);
        long weekDiff = ChronoUnit.WEEKS.between(startOfDateWeek, startOfCurrentWeek);
        if (weekDiff < 0 || weekDiff > 3) {
            return null;
        }
        return "Week-" + (4 - weekDiff);
    }

    private void notifyDataChanged(ProgressDataType type, int studentId, int courseId) {
        ProgressDataEvent event = new ProgressDataEvent(type, studentId, courseId);
        for (ProgressDataListener listener : listeners) {
            try {
                listener.onProgressDataChanged(event);
            } catch (RuntimeException ignored) {
                // Do not block the rest of listeners.
            }
        }
    }
}
