package models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import db.converters.EmotionEntryConverters;

/**
 * Model class representing an emotion entry
 */
@Entity(tableName = "emotion_entries")
@TypeConverters(EmotionEntryConverters.class)
public class EmotionEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @NonNull
    private String entryId;
    private String userId;
    private List<Emotion> emotions; // Limited to max 2 emotions per entry
    private String journalText;
    private List<String> imageUrls; // URLs to stored images, max 3
    private List<String> tags; // Tags associated with the entry, max 6
    private Date timestamp;
    private boolean isSynced = false;

    // Empty constructor required for Firebase
    public EmotionEntry() {
        emotions = new ArrayList<>();
        imageUrls = new ArrayList<>();
        tags = new ArrayList<>();
    }

    @Ignore
    public EmotionEntry(@NonNull String entryId, String userId, Date timestamp) {
        this.entryId = entryId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.emotions = new ArrayList<>();
        this.imageUrls = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.journalText = "";
    }

    // Getters and setters
    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(@NonNull String entryId) {
        this.entryId = entryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Emotion> getEmotions() {
        return emotions;
    }

    public void setEmotions(List<Emotion> emotions) {
        this.emotions = emotions;
    }

    public void addEmotion(Emotion emotion) {
        if (emotions.size() < 2) {
            emotions.add(emotion);
        }
    }

    public String getJournalText() {
        return journalText;
    }

    public void setJournalText(String journalText) {
        this.journalText = journalText;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public void addImageUrl(String imageUrl) {
        if (imageUrls.size() < 3) {
            imageUrls.add(imageUrl);
        }
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (tags.size() < 6) {
            tags.add(tag);
        }
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

}