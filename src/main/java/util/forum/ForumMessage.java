package util.forum;

import java.time.LocalDateTime;

public class ForumMessage {
    private final int messageID;
    private final int threadID;
    private final int authorID;
    private final String authorType;
    private final String authorDisplayName;
    private final String messageText;
    private final LocalDateTime timestamp;
    private final Integer parentMessageID;

    public ForumMessage(int messageID, int threadID, int authorID,
                        String authorType, String authorDisplayName,
                        String messageText, LocalDateTime timestamp,
                        Integer parentMessageID) {
        this.messageID = messageID;
        this.threadID = threadID;
        this.authorID = authorID;
        this.authorType = authorType;
        this.authorDisplayName = authorDisplayName;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.parentMessageID = parentMessageID;
    }

    public int getMessageID() {
        return messageID;
    }

    public int getThreadID() {
        return threadID;
    }

    public int getAuthorID() {
        return authorID;
    }

    public String getAuthorType() {
        return authorType;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public String getMessageText() {
        return messageText;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Integer getParentMessageID() {
        return parentMessageID;
    }

    public boolean isReply() {
        return parentMessageID != null;
    }
}
