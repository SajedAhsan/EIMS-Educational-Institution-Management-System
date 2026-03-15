package util.forum;

import java.time.LocalDateTime;

public class Announcement {
    private final int announcementID;
    private final int courseID;
    private final int teacherID;
    private final String title;
    private final String message;
    private final LocalDateTime timestamp;

    public Announcement(int announcementID, int courseID, int teacherID,
                        String title, String message, LocalDateTime timestamp) {
        this.announcementID = announcementID;
        this.courseID = courseID;
        this.teacherID = teacherID;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getAnnouncementID() {
        return announcementID;
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

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
