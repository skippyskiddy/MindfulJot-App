package db.repository;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import db.MindfulJotDatabase;
import db.dao.EmotionEntryDao;
import models.EmotionEntry;
import db.sync.SyncManager;

/**
 * Repository for managing EmotionEntry data from the local Room database.
 * Provides async methods to insert, update, delete, and query EmotionEntry records.
 * Triggers one-way syncs to Firebase for unsynced entries.
 * Used as a singleton.
 */
public class EmotionEntryRepository {
    private static volatile EmotionEntryRepository instance;

    public static EmotionEntryRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (EmotionEntryRepository.class) {
                if (instance == null) {
                    instance = new EmotionEntryRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private final EmotionEntryDao entryDao;
    private final Executor executor;
    private final Context context;

    /**
     * Callback interface for async data operations.
     */
    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(Throwable error);
    }

    private EmotionEntryRepository(Context context) {
        MindfulJotDatabase db = MindfulJotDatabase.getInstance(context);
        this.entryDao = db.emotionEntryDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.context = context.getApplicationContext();
    }

    /**
     * Inserts a new emotion entry.
     * Triggers a Firebase sync if the entry is not marked as synced.
     * Doesn't use callback.
     */
    public void insertEntry(EmotionEntry entry) {
        executor.execute(() -> {
            try {
                entryDao.insertEntry(entry);
                if (!entry.isSynced()) {
                    SyncManager.triggerSync(context);
                }
            } catch (Exception e) {
                Log.e("EmotionEntryRepo", "Error inserting entry", e);
            }
        });
    }

    /**
     * Updates an existing emotion entry.
     * Triggers a Firebase sync if the entry is not marked as synced.
     * Doesn't use callback.
     */
    public void updateEntry(EmotionEntry entry) {
        executor.execute(() -> {
            try {
                entryDao.updateEntry(entry);
                if (!entry.isSynced()) {
                    SyncManager.triggerSync(context);
                }
            } catch (Exception e) {
                Log.e("EmotionEntryRepo", "Error updating entry", e);
            }
        });
    }

    /**
     * Deletes an emotion entry.
     * Doesn't use callback.
     */
    public void deleteEntry(EmotionEntry entry) {
        executor.execute(() -> {
            try {
                entryDao.deleteEntry(entry);
            } catch (Exception e) {
                Log.e("EmotionEntryRepo", "Error deleting entry", e);
            }
        });
    }

    /**
     * Gets a single entry by its ID.
     */
    public void getEntryById(String entryId, Callback<EmotionEntry> callback) {
        executor.execute(() -> {
            try {
                EmotionEntry result = entryDao.getEntryById(entryId);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Gets all entries for a specific user.
     */
    public void getAllEntriesForUser(String userId, Callback<List<EmotionEntry>> callback) {
        executor.execute(() -> {
            try {
                List<EmotionEntry> result = entryDao.getAllEntriesForUser(userId);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Gets entries for a user in a specific date range.
     */
    public void getEntriesInRange(String userId, long startMillis, long endMillis, Callback<List<EmotionEntry>> callback) {
        executor.execute(() -> {
            try {
                List<EmotionEntry> result = entryDao.getEntriesInRange(userId, startMillis, endMillis);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Gets all unsynced emotion entries.
     */
    public void getUnsyncedEntries(Callback<List<EmotionEntry>> callback) {
        executor.execute(() -> {
            try {
                List<EmotionEntry> result = entryDao.getUnsyncedEntries();
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Deletes all entries for a specific user.
     * Doesn't use callback.
     */
    public void clearAllEntriesForUser(String userId) {
        executor.execute(() -> {
            try {
                entryDao.clearAllEntriesForUser(userId);
            } catch (Exception e) {
                Log.e("EmotionEntryRepo", "Error clearing entries for user", e);
            }
        });
    }
}
