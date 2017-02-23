package in.letsecho.appengine.library;

public class UserConnection {
    public static final String REQUEST_SENT = "RequestSent";
    public static final String REQUEST_RECEIVED = "RequestReceived";
    public static final String REQUEST_SENT_REJECTED = "RequestSentRejected";
    public static final String REQUEST_RECEVIED_REJECTED = "RequestReceivedRejected";
    public static final String REQUEST_SENT_BLOCKED = "RequestSentBlocked";
    public static final String REQUEST_RECEIVED_BLOCKED = "RequestReceivedBlocked";
    public static final String REQUEST_SENT_ACCEPTED = "RequestSentAccepted";
    public static final String REQUEST_RECEIVED_ACCEPTED = "RequestReceivedAccepted";
    public static final String CONNECTED = "Connected";

    private String chatId;
    private String status;
    private Long timestamp;
    private Boolean notified;

    public UserConnection() {}

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getNotified() {
        return notified;
    }

    public void setNotified(Boolean notified) {
        this.notified = notified;
    }
}
