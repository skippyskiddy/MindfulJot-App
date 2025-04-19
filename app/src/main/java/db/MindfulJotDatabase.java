package db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import db.converters.EmotionConverters;
import db.converters.EmotionEntryConverters;
import db.dao.EmotionDao;
import db.dao.EmotionEntryDao;
import models.Emotion;
import models.EmotionEntry;

/**
 * The main Room database for the MindfulJot app.
 * Includes tables for emotions and emotion entries.
 */
@Database(
        entities = {Emotion.class, EmotionEntry.class}, version = 1, exportSchema = false
)
@TypeConverters({EmotionConverters.class, EmotionEntryConverters.class})
public abstract class MindfulJotDatabase extends RoomDatabase {
    private static volatile MindfulJotDatabase INSTANCE;
    public abstract EmotionDao emotionDao();
    public abstract EmotionEntryDao emotionEntryDao();

    /**
     * Get a singleton instance of the database.
     */
    public static MindfulJotDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MindfulJotDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MindfulJotDatabase.class,
                                    "mindfuljot_db"
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
