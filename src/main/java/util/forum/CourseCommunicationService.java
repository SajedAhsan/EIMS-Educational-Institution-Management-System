package util.forum;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import database.DatabaseManager;

public class CourseCommunicationService {

    public enum NotificationReason {
        MENTION,
        REPLY
    }

    public static class CourseUser {
        private final int id;
        private final String userType;
        private final String displayName;
        private final String username;

        public CourseUser(int id, String userType, String displayName, String username) {
            this.id = id;
            this.userType = userType;
            this.displayName = displayName;
            this.username = MentionUtils.normalizeUsername(username);
        }

        public int getId() {
            return id;
        }

        public String getUserType() {
            return userType;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class MessageNotification {
        private final int notificationId;
        private final int courseId;
        private final int threadId;
        private final int messageId;
        private final Integer replyToMessageId;
        private final int recipientId;
        private final String recipientType;
        private final int senderId;
        private final String senderType;
        private final String senderDisplayName;
        private final String threadTitle;
        private final String previewText;
        private final NotificationReason reason;
        private final LocalDateTime timestamp;
        private boolean seen;

        public MessageNotification(int notificationId,
                                   int courseId,
                                   int threadId,
                                   int messageId,
                                   Integer replyToMessageId,
                                   int recipientId,
                                   String recipientType,
                                   int senderId,
                                   String senderType,
                                   String senderDisplayName,
                                   String threadTitle,
                                   String previewText,
                                   NotificationReason reason,
                                   LocalDateTime timestamp) {
            this.notificationId = notificationId;
            this.courseId = courseId;
            this.threadId = threadId;
            this.messageId = messageId;
            this.replyToMessageId = replyToMessageId;
            this.recipientId = recipientId;
            this.recipientType = normalizeUserType(recipientType);
            this.senderId = senderId;
            this.senderType = normalizeUserType(senderType);
            this.senderDisplayName = senderDisplayName;
            this.threadTitle = threadTitle;
            this.previewText = previewText;
            this.reason = reason;
            this.timestamp = timestamp;
            this.seen = false;
        }

        public int getNotificationId() {
            return notificationId;
        }

        public int getCourseId() {
            return courseId;
        }

        public int getThreadId() {
            return threadId;
        }

        public int getMessageId() {
            return messageId;
        }

        public Integer getReplyToMessageId() {
            return replyToMessageId;
        }

        public int getRecipientId() {
            return recipientId;
        }

        public String getRecipientType() {
            return recipientType;
        }

        public int getSenderId() {
            return senderId;
        }

        public String getSenderType() {
            return senderType;
        }

        public String getSenderDisplayName() {
            return senderDisplayName;
        }

        public String getThreadTitle() {
            return threadTitle;
        }

        public String getPreviewText() {
            return previewText;
        }

        public NotificationReason getReason() {
            return reason;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public boolean isSeen() {
            return seen;
        }

        public void markSeen() {
            this.seen = true;
        }
    }

    private static final CourseCommunicationService INSTANCE = new CourseCommunicationService();

    private final AtomicInteger messageNotificationIdSequence = new AtomicInteger(1);

    // Message notifications remain in memory for lightweight unread tracking.
    private final List<MessageNotification> messageNotifications = new CopyOnWriteArrayList<>();

    private CourseCommunicationService() {
    }

    public static CourseCommunicationService getInstance() {
        return INSTANCE;
    }

    public Announcement postAnnouncement(int courseId,
                                         int teacherId,
                                         String title,
                                         String message,
                                         String courseName,
                                         List<DatabaseManager.MemberData> enrolledStudents,
                                         DatabaseManager dbManager) throws SQLException {
        DatabaseManager store = dbManager != null ? dbManager : DatabaseManager.getInstance();
        Announcement announcement = store.createForumAnnouncement(
            courseId,
            teacherId,
            title,
            message,
            LocalDateTime.now()
        );

        if (store != null && enrolledStudents != null) {
            for (DatabaseManager.MemberData student : enrolledStudents) {
                store.createNotification(
                    student.getId(),
                    "STUDENT",
                    "New announcement posted in " + courseName + ": " + title
                );
            }
        }

        return announcement;
    }

    public List<Announcement> getAnnouncementsForCourse(int courseId) {
        try {
            return DatabaseManager.getInstance().getForumAnnouncementsByCourse(courseId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ForumThread ensureGeneralThread(int courseId, int teacherId) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        try {
            ForumThread existing = dbManager.getGeneralForumThread(courseId);
            if (existing != null) {
                return existing;
            }

            return dbManager.createForumThread(
                courseId,
                teacherId,
                "General Discussion",
                ForumThread.ThreadType.GENERAL,
                null,
                LocalDateTime.now()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return new ForumThread(
                -1,
                courseId,
                teacherId,
                "General Discussion",
                ForumThread.ThreadType.GENERAL,
                null,
                LocalDateTime.now()
            );
        }
    }

    public ForumThread createAssignmentThread(int courseId,
                                              int teacherId,
                                              String title,
                                              Integer assignmentId) {
        try {
            return DatabaseManager.getInstance().createForumThread(
                courseId,
                teacherId,
                title,
                ForumThread.ThreadType.ASSIGNMENT,
                assignmentId,
                LocalDateTime.now()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return new ForumThread(
                -1,
                courseId,
                teacherId,
                title,
                ForumThread.ThreadType.ASSIGNMENT,
                assignmentId,
                LocalDateTime.now()
            );
        }
    }

    public List<ForumThread> getThreadsForCourse(int courseId) {
        List<ForumThread> result;
        try {
            result = DatabaseManager.getInstance().getForumThreadsByCourse(courseId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        result.sort((a, b) -> {
            if (a.getType() == ForumThread.ThreadType.GENERAL && b.getType() != ForumThread.ThreadType.GENERAL) {
                return -1;
            }
            if (a.getType() != ForumThread.ThreadType.GENERAL && b.getType() == ForumThread.ThreadType.GENERAL) {
                return 1;
            }
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        return result;
    }

    public ForumMessage postMessage(int courseId,
                                    int threadId,
                                    int authorId,
                                    String authorType,
                                    String authorDisplayName,
                                    String messageText,
                                    Integer parentMessageId,
                                    String courseName,
                                    List<CourseUser> participants,
                                    DatabaseManager dbManager,
                                    boolean notifyOnReply) throws SQLException {
        DatabaseManager store = dbManager != null ? dbManager : DatabaseManager.getInstance();
        ForumMessage message = store.createForumMessage(
            threadId,
            authorId,
            authorType,
            authorDisplayName,
            messageText,
            parentMessageId,
            LocalDateTime.now()
        );

        if (store != null) {
            triggerMentionNotifications(message, courseId, courseName, participants, store);
            if (notifyOnReply && parentMessageId != null) {
                triggerReplyNotification(message, courseId, parentMessageId, courseName, store);
            }
        }

        return message;
    }

    public List<ForumMessage> getMessagesForThread(int threadId) {
        try {
            return DatabaseManager.getInstance().getForumMessagesByThread(threadId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ForumMessage getMessageById(int messageId) {
        try {
            return DatabaseManager.getInstance().getForumMessageById(messageId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ForumThread getThreadById(int threadId) {
        try {
            return DatabaseManager.getInstance().getForumThreadById(threadId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<MessageNotification> getMessageNotificationsForUser(int courseId,
                                                                    int userId,
                                                                    String userType,
                                                                    boolean onlyUnseen) {
        String normalizedType = normalizeUserType(userType);
        List<MessageNotification> result = new ArrayList<>();

        for (MessageNotification notification : messageNotifications) {
            boolean sameCourse = notification.getCourseId() == courseId;
            boolean sameUser = notification.getRecipientId() == userId
                && notification.getRecipientType().equals(normalizedType);
            if (!sameCourse || !sameUser) {
                continue;
            }
            if (onlyUnseen && notification.isSeen()) {
                continue;
            }
            result.add(notification);
        }

        result.sort(Comparator.comparing(MessageNotification::getTimestamp).reversed());
        return result;
    }

    public int countUnseenMessageNotifications(int courseId,
                                               int userId,
                                               String userType) {
        String normalizedType = normalizeUserType(userType);
        int count = 0;
        for (MessageNotification notification : messageNotifications) {
            if (notification.getCourseId() == courseId
                && notification.getRecipientId() == userId
                && notification.getRecipientType().equals(normalizedType)
                && !notification.isSeen()) {
                count++;
            }
        }
        return count;
    }

    public void markMessageNotificationSeen(int notificationId,
                                            int recipientId,
                                            String recipientType) {
        String normalizedType = normalizeUserType(recipientType);
        for (MessageNotification notification : messageNotifications) {
            if (notification.getNotificationId() == notificationId
                && notification.getRecipientId() == recipientId
                && notification.getRecipientType().equals(normalizedType)) {
                notification.markSeen();
                return;
            }
        }
    }

    public Set<String> detectMentions(String messageText) {
        return MentionUtils.extractMentionUsernames(messageText);
    }

    public boolean canUserPostInGroup(int courseId,
                                      int userId,
                                      String userType,
                                      DatabaseManager dbManager) throws SQLException {
        if (dbManager == null || userId <= 0 || userType == null) {
            return false;
        }

        String normalizedType = userType.trim().toUpperCase();
        if ("TEACHER".equals(normalizedType)) {
            return isTeacherOfGroup(courseId, userId, dbManager);
        }
        if ("STUDENT".equals(normalizedType)) {
            return isStudentOfGroup(courseId, userId, dbManager);
        }

        return false;
    }

    private boolean isTeacherOfGroup(int courseId,
                                     int userId,
                                     DatabaseManager dbManager) throws SQLException {
        DatabaseManager.GroupDetailData group = dbManager.getGroupById(courseId);
        if (group == null || group.getTeacherEmail() == null || group.getTeacherEmail().isBlank()) {
            return false;
        }

        int teacherId = dbManager.getTeacherIdByEmail(group.getTeacherEmail());
        return teacherId == userId;
    }

    private boolean isStudentOfGroup(int courseId,
                                     int userId,
                                     DatabaseManager dbManager) throws SQLException {
        List<DatabaseManager.MemberData> members = dbManager.getGroupMembers(courseId);
        for (DatabaseManager.MemberData member : members) {
            if (member.getId() == userId) {
                return true;
            }
        }
        return false;
    }

    private void triggerMentionNotifications(ForumMessage message,
                                             int courseId,
                                             String courseName,
                                             List<CourseUser> participants,
                                             DatabaseManager dbManager) throws SQLException {
        if (participants == null || participants.isEmpty()) {
            return;
        }

        Set<String> mentions = MentionUtils.extractMentionUsernames(message.getMessageText());
        if (mentions.isEmpty()) {
            return;
        }

        Map<String, CourseUser> mentionIndex = new HashMap<>();
        for (CourseUser user : participants) {
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                mentionIndex.putIfAbsent(MentionUtils.normalizeUsername(user.getUsername()), user);
            }
        }

        Set<String> notified = new HashSet<>();
        for (String mention : mentions) {
            CourseUser target = mentionIndex.get(mention);
            if (target == null) {
                continue;
            }
            if (target.getId() == message.getAuthorID() &&
                target.getUserType().equalsIgnoreCase(message.getAuthorType())) {
                continue;
            }
            String key = target.getUserType() + ":" + target.getId();
            if (notified.add(key)) {
                dbManager.createNotification(
                    target.getId(),
                    target.getUserType(),
                    "You were mentioned in a discussion in course " + courseName + "."
                );

                createMessageNotificationIfAbsent(
                    message,
                    courseId,
                    message.getParentMessageID(),
                    target.getId(),
                    target.getUserType(),
                    NotificationReason.MENTION
                );
            }
        }
    }

    private void triggerReplyNotification(ForumMessage newMessage,
                                          int courseId,
                                          int parentMessageId,
                                          String courseName,
                                          DatabaseManager dbManager) throws SQLException {
        ForumMessage parentMessage = getMessageById(parentMessageId);

        if (parentMessage == null) {
            return;
        }

        boolean sameAuthor = parentMessage.getAuthorID() == newMessage.getAuthorID()
            && parentMessage.getAuthorType().equalsIgnoreCase(newMessage.getAuthorType());
        if (sameAuthor) {
            return;
        }

        dbManager.createNotification(
            parentMessage.getAuthorID(),
            parentMessage.getAuthorType(),
            newMessage.getAuthorDisplayName() + " replied to your message in course " + courseName + "."
        );

        createMessageNotificationIfAbsent(
            newMessage,
            courseId,
            parentMessageId,
            parentMessage.getAuthorID(),
            parentMessage.getAuthorType(),
            NotificationReason.REPLY
        );
    }

    private void createMessageNotificationIfAbsent(ForumMessage sourceMessage,
                                                   int courseId,
                                                   Integer replyToMessageId,
                                                   int recipientId,
                                                   String recipientType,
                                                   NotificationReason reason) {
        String normalizedRecipientType = normalizeUserType(recipientType);
        for (MessageNotification existing : messageNotifications) {
            if (existing.getMessageId() == sourceMessage.getMessageID()
                && existing.getRecipientId() == recipientId
                && existing.getRecipientType().equals(normalizedRecipientType)) {
                return;
            }
        }

        ForumThread thread = getThreadById(sourceMessage.getThreadID());
        String threadTitle = thread != null ? thread.getTitle() : "Discussion";
        String preview = buildPreview(sourceMessage.getMessageText());

        messageNotifications.add(new MessageNotification(
            messageNotificationIdSequence.getAndIncrement(),
            courseId,
            sourceMessage.getThreadID(),
            sourceMessage.getMessageID(),
            replyToMessageId,
            recipientId,
            normalizedRecipientType,
            sourceMessage.getAuthorID(),
            sourceMessage.getAuthorType(),
            sourceMessage.getAuthorDisplayName(),
            threadTitle,
            preview,
            reason,
            sourceMessage.getTimestamp()
        ));
    }

    private static String buildPreview(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 120) {
            return compact;
        }
        return compact.substring(0, 117) + "...";
    }

    private static String normalizeUserType(String userType) {
        if (userType == null) {
            return "";
        }
        return userType.trim().toUpperCase(Locale.ROOT);
    }
}
