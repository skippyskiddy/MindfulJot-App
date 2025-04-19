package utils;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;

import db.MindfulJotDatabase;
import db.dao.EmotionDao;
import models.Emotion;

/**
 * Wrapper utility class that connects EmotionInitializer (Firebase) to Room.
 * It seeds the Room database with Emotion objects retrieved from Firebase
 * only if the expected number of emotions is not already present.
 */
public class EmotionSeeder {

    private static final String TAG = "EmotionSeeder";
    private static final int EXPECTED_EMOTION_COUNT = 100;

    private final Context context;
    private final EmotionInitializer initializer;
    private final EmotionDao emotionDao;

    public EmotionSeeder(Context context) {
        this.context = context.getApplicationContext();
        this.initializer = new EmotionInitializer(context);
        this.emotionDao = MindfulJotDatabase.getInstance(context).emotionDao();
    }

    /**
     * Seeds the Room database with Firebase emotions only if the local database does not
     * already contain all expected emotions.
     */
    public void seedIfNeeded() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int localCount = emotionDao.count();
            if (localCount == EXPECTED_EMOTION_COUNT) {
                Log.d(TAG, "Room DB already has all " + EXPECTED_EMOTION_COUNT + " emotions. Skipping seed.");
                return;
            }

            initializer.verifyAndInitEmotions((success, firebaseEmotions) -> {
                if (success) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            emotionDao.insertAll(firebaseEmotions);
                            Log.d(TAG, "Seeded Room with " + firebaseEmotions.size() + " emotions.");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to seed emotions into Room", e);
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to fetch emotions from Firebase.");
                }
            });
        });
    }
}
