package db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import models.Emotion;

/**
 * DAO for managing Emotion entities in the local Room database.
 * Used for seeding, querying, and retrieving emotion metadata.
 */
@Dao
public interface EmotionDao {

    /**
     * Insert a list of emotions. Ignores duplicates based on the primary key (emotion name).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Emotion> emotions);

    /**
     * Get all emotions in the database.
     */
    @Query("SELECT * FROM emotions")
    List<Emotion> getAllEmotions();

    /**
     * Get the count of all emotions in the database.
     */
    @Query("SELECT COUNT(*) FROM emotions")
    int count();

    /**
     * Get all emotions by category.
     */
    @Query("SELECT * FROM emotions WHERE category = :category ORDER BY energyLevel DESC")
    List<Emotion> getEmotionsByCategory(String category);

    /**
     * Get a specific emotion by its name.
     */
    @Query("SELECT * FROM emotions WHERE name = :name LIMIT 1")
    Emotion getEmotionByName(String name);
}
