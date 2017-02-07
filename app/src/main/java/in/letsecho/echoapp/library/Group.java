package in.letsecho.echoapp.library;

import com.firebase.geofire.GeoLocation;

import static java.lang.Boolean.TRUE;

public class Group {
    public static String GROUP_TYPE = "Group";
    public static String SERVICE_TYPE = "Service";
    private String id;
    private String chatId;
    private String title;
    private String description;
    private String ownerId;
    private String ownerName;
    private String phoneNo;
    private String type;
    private boolean visible;

    public Group() {}

    public Group(String title, String description, String ownerId, String ownerName, String phoneNo, String type) {
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.phoneNo = phoneNo;
        this.type = type;
        this.visible = TRUE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
