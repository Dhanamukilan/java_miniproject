package com.wipro.chatmod.bean;

/**
 * ChatMessage
 * -----------
 * Simple POJO (bean) that represents a single incoming chat message.
 * Instances of this class are placed on the shared BlockingQueue by the
 * producer thread and picked up by consumer threads for moderation.
 */
public class ChatMessage {

    private String userId;
    private String content;
    private long timestamp;

    /**
     * Default (no-arg) constructor.
     * Required so the bean can be created and populated via setters.
     */
    public ChatMessage() {
    }

    /**
     * Parameterized constructor for convenient one-line creation of a message.
     *
     * @param userId    the id of the user who sent the message
     * @param content   the raw text content of the message
     * @param timestamp the time (epoch millis) the message was created
     */
    public ChatMessage(String userId, String content, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
