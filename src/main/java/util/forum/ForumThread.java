package util.forum;

import java.time.LocalDateTime;

public class ForumThread {

    public enum ThreadType {
        GENERAL,
        ASSIGNMENT
    }

    private final int threadID;
    private final int courseID;
    private final int teacherID;
    private final String title;
    private final ThreadType type;
    private final Integer assignmentID;
    private final LocalDateTime timestamp;

    public ForumThread(int threadID, int courseID, int teacherID, String title,
                       ThreadType type, Integer assignmentID, LocalDateTime timestamp) {
        this.threadID = threadID;
        this.courseID = courseID;
        this.teacherID = teacherID;
        this.title = title;
        this.type = type;
        this.assignmentID = assignmentID;
        this.timestamp = timestamp;
    }

    public int getThreadID() {
        return threadID;
    }

    public int getCourseID() {
        return courseID;
    }

    public int getTeacherID() {
        return teacherID;
    }

    public String getTitle() {
        return title;
    }

    public ThreadType getType() {
        return type;
    }

    public Integer getAssignmentID() {
        return assignmentID;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        if (type == ThreadType.GENERAL) {
            return "[General] " + title;
        }
        return "[Assignment] " + title;
    }
}
