package db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import models.EmotionEntry;

/**
 * DAO for managing EmotionEntry entities in the local Room database.
 * Provides methods to insert, update, delete, and query emotion entries.
 */
@Dao
public interface EmotionEntryDao {

    /**
     * Insert a new emotion entry. Aborts if an entry with the same ID already exists.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertEntry(EmotionEntry entry);

    /**
     * Update an existing emotion entry.
     */
    @Update
    void updateEntry(EmotionEntry entry);

    /**
     * Delete an emotion entry.
     */
    @Delete
    void deleteEntry(EmotionEntry entry);

    /**
     * Get a single entry by its ID.
     */
    @Query("SELECT * FROM emotion_entries WHERE entryId = :entryId LIMIT 1")
    EmotionEntry getEntryById(String entryId);

    /**
     * Get all entries for a specific user.
     */
    @Query("SELECT * FROM emotion_entries WHERE userId = :userId ORDER BY timestamp ASC")
    List<EmotionEntry> getAllEntriesForUser(String userId);

    /**
     * Get entries for a user in a specific date range.
     */
    @Query("SELECT * FROM emotion_entries WHERE userId = :userId AND timestamp BETWEEN :startMillis AND :endMillis ORDER BY timestamp ASC")
    List<EmotionEntry> getEntriesInRange(String userId, long startMillis, long endMillis);

    /**
     * Delete all entries for a specific user (e.g., when resetting or deleting account data).
     *
     */
    @Query("DELETE FROM emotion_entries WHERE userId = :userId")
    void clearAllEntriesForUser(String userId);

    /**
     * Gets all emotion entries that have not been synced with Firebase.
     */
    @Query("SELECT * FROM emotion_entries WHERE isSynced = 0")
    List<EmotionEntry> getUnsyncedEntries();


}
