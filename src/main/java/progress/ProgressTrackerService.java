package progress;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import progress.model.Assignment;
import progress.model.AttendanceRecord;
import progress.model.Course;
import progress.model.Exam;
import progress.model.Student;

/**
 * In-memory progress tracker service shared by the student and teacher dashboards.
 * Attendance is tracked here during the current app session.
 */
public class ProgressTrackerService {

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

    private final Map<Integer, Student> studentsById = new LinkedHashMap<>();
    private final Map<Integer, Course> coursesById = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> courseIdsByStudent = new HashMap<>();
    private final Map<Integer, Set<LocalDate>> classDatesHeldByCourse = new HashMap<>();
    private final Map<String, List<Assignment>> assignmentsByKey = new HashMap<>();
    private final Map<String, List<Exam>> examsByKey = new HashMap<>();
    private final Map<String, List<AttendanceRecord>> attendanceByKey = new HashMap<>();

    private final AtomicInteger assignmentIdSeq = new AtomicInteger(1);
    private final AtomicInteger examIdSeq = new AtomicInteger(1);

    private ProgressTrackerService() {
        addCourseInternal(new Course(1001, "CSE101", "Programming Fundamentals"));
    }

    public static ProgressTrackerService getInstance() {
        return INSTANCE;
    }

    public synchronized void ensureStudentExists(int studentId, String name, String email) {
        if (studentsById.containsKey(studentId)) {
            return;
        }
        studentsById.put(studentId, new Student(studentId, name, email));
    }

    public synchronized void ensureCourseExists(int courseId, String code, String title) {
        if (coursesById.containsKey(courseId)) {
            return;
        }
        addCourseInternal(new Course(courseId, code, title));
    }

    public synchronized List<Course> getAllCourses() {
        return new ArrayList<>(coursesById.values());
    }

    public synchronized void enrollStudentInCourse(int studentId, int courseId) {
        courseIdsByStudent.computeIfAbsent(studentId, id -> new LinkedHashSet<>()).add(courseId);
    }

    public synchronized void initializeMockDataForStudent(int studentId, String name, String email) {
        ensureStudentExists(studentId, name, email);
    }

    public synchronized void initializeMockDataForCourse(int studentId, int courseId) {
        courseIdsByStudent.computeIfAbsent(studentId, id -> new LinkedHashSet<>()).add(courseId);
    }

    public synchronized List<Course> getCoursesForStudent(int studentId) {
        Set<Integer> courseIds = courseIdsByStudent.getOrDefault(studentId, Collections.emptySet());
        List<Course> courses = new ArrayList<>();
        for (Integer courseId : courseIds) {
            Course c = coursesById.get(courseId);
            if (c != null) {
                courses.add(c);
            }
        }
        courses.sort(Comparator.comparing(Course::getCode));
        return courses;
    }

    public synchronized List<Assignment> getAssignments(int studentId, int courseId) {
        return new ArrayList<>(assignmentsByKey.getOrDefault(key(studentId, courseId), Collections.emptyList()));
    }

    public synchronized List<Exam> getExams(int studentId, int courseId) {
        return new ArrayList<>(examsByKey.getOrDefault(key(studentId, courseId), Collections.emptyList()));
    }

    public synchronized List<AttendanceRecord> getAttendanceRecords(int studentId, int courseId) {
        return new ArrayList<>(attendanceByKey.getOrDefault(key(studentId, courseId), Collections.emptyList()));
    }

    public synchronized void addAssignment(int studentId,
                                           int courseId,
                                           String title,
                                           double maxScore,
                                           Double score) {
        List<Assignment> assignments = assignmentsByKey.computeIfAbsent(key(studentId, courseId), k -> new ArrayList<>());
        assignments.add(new Assignment(assignmentIdSeq.getAndIncrement(), courseId, title, maxScore, score));
    }

    public synchronized void addExam(int studentId,
                                     int courseId,
                                     String title,
                                     double maxScore,
                                     Double score) {
        List<Exam> exams = examsByKey.computeIfAbsent(key(studentId, courseId), k -> new ArrayList<>());
        exams.add(new Exam(examIdSeq.getAndIncrement(), courseId, title, maxScore, score));
    }

    public synchronized void markAttendance(int studentId,
                                            int courseId,
                                            LocalDate date,
                                            boolean present) {
        classDatesHeldByCourse.computeIfAbsent(courseId, k -> new LinkedHashSet<>()).add(date);
        List<AttendanceRecord> records = attendanceByKey.computeIfAbsent(key(studentId, courseId), k -> new ArrayList<>());
        for (AttendanceRecord record : records) {
            if (record.getDate().equals(date)) {
                record.setPresent(present);
                return;
            }
        }
        records.add(new AttendanceRecord(courseId, studentId, date, present));
    }

    public synchronized int getTotalClassDays(int courseId) {
        return classDatesHeldByCourse.getOrDefault(courseId, Collections.emptySet()).size();
    }

    public synchronized boolean isPresentOnDate(int studentId, int courseId, LocalDate date) {
        return isPresentOnDate(studentId, courseId, date, null);
    }

    public synchronized boolean isPresentOnDate(int studentId,
                                                int courseId,
                                                LocalDate date,
                                                LocalDate attendanceStartDate) {
        if (date == null) {
            return false;
        }
        if (attendanceStartDate != null && date.isBefore(attendanceStartDate)) {
            return false;
        }

        List<AttendanceRecord> records = getAttendanceRecordsAfterDate(studentId, courseId, attendanceStartDate);
        for (AttendanceRecord record : records) {
            if (record.getDate().equals(date)) {
                return record.isPresent();
            }
        }
        return false;
    }

    public synchronized double getAttendancePercent(int studentId, int courseId) {
        return getAttendancePercent(studentId, courseId, null);
    }

    public synchronized double getAttendancePercent(int studentId,
                                                    int courseId,
                                                    LocalDate attendanceStartDate) {
        List<AttendanceRecord> records = getAttendanceRecordsAfterDate(studentId, courseId, attendanceStartDate);
        if (records.isEmpty()) {
            return 0.0;
        }

        int present = 0;
        for (AttendanceRecord r : records) {
            if (r.isPresent()) {
                present++;
            }
        }
        return (present * 100.0) / records.size();
    }

    public synchronized CourseMetrics getCourseMetrics(int studentId, int courseId) {
        Course course = coursesById.get(courseId);
        if (course == null) {
            return new CourseMetrics(
                new Course(courseId, "N/A", "Unknown Course"),
                0,
                0,
                0.0,
                Collections.emptyMap(),
                Collections.emptyList(),
                0,
                0,
                0.0,
                Collections.emptyList(),
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
            );
        }

        List<AttendanceRecord> attendance = getAttendanceRecords(studentId, courseId);
        int present = 0;
        for (AttendanceRecord r : attendance) {
            if (r.isPresent()) {
                present++;
            }
        }
        int totalClassDays = getTotalClassDays(courseId);
        int absent = Math.max(0, totalClassDays - present);
        double attendancePct = totalClassDays == 0 ? 0.0 : (present * 100.0) / totalClassDays;

        List<Assignment> assignments = getAssignments(studentId, courseId);
        int completedAssignments = 0;
        double assignmentScoreSum = 0.0;
        int assignmentScoreCount = 0;
        for (Assignment assignment : assignments) {
            if (assignment.isCompleted()) {
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
            buildWeeklyAttendance(attendance),
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
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        result.put("Week-1", 0.0);
        result.put("Week-2", 0.0);
        result.put("Week-3", 0.0);
        result.put("Week-4", 0.0);

        if (studentIds == null || studentIds.isEmpty()) {
            return result;
        }

        Map<String, Integer> presentByWeek = new LinkedHashMap<>();
        Map<String, Integer> totalByWeek = new LinkedHashMap<>();
        for (String week : result.keySet()) {
            presentByWeek.put(week, 0);
            totalByWeek.put(week, 0);
        }

        for (Integer studentId : studentIds) {
            List<AttendanceRecord> records = getAttendanceRecordsAfterDate(studentId, courseId, attendanceStartDate);
            Map<String, Double> single = buildWeeklyAttendance(records);
            for (Map.Entry<String, Double> entry : single.entrySet()) {
                String week = entry.getKey();
                if (presentByWeek.containsKey(week)) {
                    double pct = entry.getValue();
                    if (pct > 0) {
                        presentByWeek.put(week, presentByWeek.get(week) + (int) Math.round(pct));
                        totalByWeek.put(week, totalByWeek.get(week) + 100);
                    }
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

        int present = 0;
        int absent = 0;
        for (Integer studentId : studentIds) {
            boolean isPresent = isPresentOnDate(studentId, courseId, date, attendanceStartDate);
            if (isPresent) {
                present++;
            } else {
                absent++;
            }
        }
        return new ClassAttendanceSummary(present, absent);
    }

    private void addCourseInternal(Course course) {
        coursesById.put(course.getId(), course);
    }

    private static String key(int studentId, int courseId) {
        return studentId + ":" + courseId;
    }

    private List<AttendanceRecord> getAttendanceRecordsAfterDate(int studentId,
                                                                 int courseId,
                                                                 LocalDate attendanceStartDate) {
        List<AttendanceRecord> records =
            attendanceByKey.getOrDefault(key(studentId, courseId), Collections.emptyList());

        if (attendanceStartDate == null) {
            return new ArrayList<>(records);
        }

        List<AttendanceRecord> filtered = new ArrayList<>();
        for (AttendanceRecord record : records) {
            if (!record.getDate().isBefore(attendanceStartDate)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private static Map<String, Double> buildWeeklyAttendance(List<AttendanceRecord> records) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        result.put("Week-1", 0.0);
        result.put("Week-2", 0.0);
        result.put("Week-3", 0.0);
        result.put("Week-4", 0.0);

        if (records == null || records.isEmpty()) {
            return result;
        }

        Map<String, Integer> presentByWeek = new HashMap<>();
        Map<String, Integer> totalByWeek = new HashMap<>();
        for (String w : result.keySet()) {
            presentByWeek.put(w, 0);
            totalByWeek.put(w, 0);
        }

        LocalDate today = LocalDate.now();
        WeekFields wf = WeekFields.of(Locale.getDefault());
        int thisWeek = today.get(wf.weekOfWeekBasedYear());

        for (AttendanceRecord record : records) {
            int week = record.getDate().get(wf.weekOfWeekBasedYear());
            int diff = thisWeek - week;
            if (diff < 0 || diff > 3) {
                continue;
            }

            String label = "Week-" + (4 - diff);
            totalByWeek.put(label, totalByWeek.get(label) + 1);
            if (record.isPresent()) {
                presentByWeek.put(label, presentByWeek.get(label) + 1);
            }
        }

        for (String w : result.keySet()) {
            int total = totalByWeek.get(w);
            int present = presentByWeek.get(w);
            result.put(w, total == 0 ? 0.0 : (present * 100.0) / total);
        }

        return result;
    }
}
