package progress.model;

import java.time.LocalDate;

public class AttendanceRecord {
    private final int courseId;
    private final int studentId;
    private final LocalDate date;
    private boolean present;

    public AttendanceRecord(int courseId, int studentId, LocalDate date, boolean present) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.date = date;
        this.present = present;
    }

    public int getCourseId() {
        return courseId;
    }

    public int getStudentId() {
        return studentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }
}
