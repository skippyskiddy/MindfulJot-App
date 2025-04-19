package db.sync;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.concurrent.Executors;

import db.MindfulJotDatabase;
import db.dao.EmotionEntryDao;
import models.EmotionEntry;

/**
 * SyncManager is a singleton that handles syncing unsynced EmotionEntry objects
 * from Room to Firebase. After a successful sync, it marks those entries
 * as synced in the local database.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    private static volatile SyncManager instance;

    private final EmotionEntryDao emotionEntryDao;
    private final DatabaseReference entriesRef;

    private SyncManager(Context context) {
        this.emotionEntryDao = MindfulJotDatabase.getInstance(context.getApplicationContext()).emotionEntryDao();
        this.entriesRef = FirebaseDatabase.getInstance().getReference("emotion_entries");
    }

    /**
     * Returns the singleton instance of SyncManager.
     */
    public static SyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Convenience method to trigger a sync without manually calling getInstance().
     * Equivalent to: SyncManager.getInstance(context).syncUnsyncedEntries()
     */
    public static void triggerSync(Context context) {
        getInstance(context).syncUnsyncedEntries();
    }

    /**
     * Syncs all unsynced entries from Room to Firebase.
     * After successful upload, updates their synced status locally.
     */
    public void syncUnsyncedEntries() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<EmotionEntry> unsyncedEntries = emotionEntryDao.getUnsyncedEntries();

            if (unsyncedEntries.isEmpty()) {
                Log.d(TAG, "No unsynced entries to sync.");
                return;
            }

            Log.d(TAG, "Syncing " + unsyncedEntries.size() + " entries to Firebase...");

            for (EmotionEntry entry : unsyncedEntries) {
                entriesRef.child(entry.getEntryId()).setValue(entry, (DatabaseError error, DatabaseReference ref) -> {
                    if (error == null) {
                        Log.d(TAG, "Synced entry " + entry.getEntryId() + " successfully.");
                        entry.setSynced(true);
                        Executors.newSingleThreadExecutor().execute(() -> {
                            emotionEntryDao.updateEntry(entry);
                        });
                    } else {
                        Log.e(TAG, "Failed to sync entry " + entry.getEntryId(), error.toException());
                    }
                });
            }
        });
    }
}
