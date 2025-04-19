package db.repository;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import db.MindfulJotDatabase;
import db.dao.EmotionDao;
import models.Emotion;

/**
 * Repository for managing Emotion data from the local Room database.
 * Provides async methods to insert and query Emotion records.
 * Separates data access from UI logic and simplifies background threading.
 * Used as a singleton.
 */
public class EmotionRepository {
    private static volatile EmotionRepository instance;

    public static EmotionRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (EmotionRepository.class) {
                if (instance == null) {
                    instance = new EmotionRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private final EmotionDao emotionDao;
    private final Executor executor;

    /**
     * Callback interface for async data operations.
     */
    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(Throwable error);
    }

    private EmotionRepository(Context context) {
        MindfulJotDatabase db = MindfulJotDatabase.getInstance(context);
        this.emotionDao = db.emotionDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Inserts a list of emotions into the database.
     * Doesn't use callback.
     */
    public void insertAll(List<Emotion> emotions) {
        executor.execute(() -> {
            try {
                emotionDao.insertAll(emotions);
            } catch (Exception e) {
                Log.e("EmotionRepository", "Error inserting emotions", e);
            }
        });
    }

    /**
     * Retrieves all emotions from the database.
     */
    public void getAllEmotions(Callback<List<Emotion>> callback) {
        executor.execute(() -> {
            try {
                List<Emotion> result = emotionDao.getAllEmotions();
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Retrieves all emotions within a specific category.
     */
    public void getEmotionsByCategory(Emotion.Category category, Callback<List<Emotion>> callback) {
        executor.execute(() -> {
            try {
                List<Emotion> result = emotionDao.getEmotionsByCategory(category.name());
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Retrieves a single emotion by its name.
     */
    public void getEmotionByName(String name, Callback<Emotion> callback) {
        executor.execute(() -> {
            try {
                Emotion result = emotionDao.getEmotionByName(name);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Gets total number of emotions in the database.
     */
    public void getCount(Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                int count = emotionDao.count();
                callback.onSuccess(count);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }
}