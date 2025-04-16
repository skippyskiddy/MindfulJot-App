package utils;

import androidx.annotation.NonNull;

import models.Emotion;
import models.EmotionEntry;
import models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to handle Firebase interactions
 */
public class FirebaseHelper {
    private static FirebaseHelper instance;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;

    // Database references
    private DatabaseReference usersRef;
    private DatabaseReference entriesRef;
    private DatabaseReference emotionsRef;

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        usersRef = database.getReference("users");
        entriesRef = database.getReference("entries");
        emotionsRef = database.getReference("emotions");

        // Initialize default emotions if needed
        initDefaultEmotions();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    /**
     * Creates a new user in Firebase Auth and Database
     */
    public void createUser(String email, String password, String name, final OnCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String userId = auth.getCurrentUser().getUid();

                        // Create user object
                        User user = new User(userId, name, email);

                        // Save to database
                        usersRef.child(userId).setValue(user)
                                .addOnCompleteListener(listener);
                    } else {
                        // Pass the task to the listener for error handling
                        listener.onComplete(task);
                    }
                });
    }

    /**
     * Sign in existing user
     */
    public void signInUser(String email, String password, final OnCompleteListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    /**
     * Sign out current user
     */
    public void signOut() {
        auth.signOut();
    }

    /**
     * Get currently logged in user
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Get user data from database
     */
    public void getUserData(String userId, ValueEventListener listener) {
        usersRef.child(userId).addValueEventListener(listener);
    }

    /**
     * Update user data
     */
    public Task<Void> updateUserData(String userId, Map<String, Object> updates) {
        return usersRef.child(userId).updateChildren(updates);
    }

    /**
     * Save emotion entry to database
     */
    public Task<Void> saveEmotionEntry(EmotionEntry entry) {
        if (entry.getEntryId() == null || entry.getEntryId().isEmpty()) {
            // Generate new entry ID
            String entryId = entriesRef.push().getKey();
            entry.setEntryId(entryId);
        }

        return entriesRef.child(entry.getEntryId()).setValue(entry);
    }

    /**
     * Get user's most recent emotion entry
     */
    public void getLatestEmotionEntry(String userId, ValueEventListener listener) {
        Query query = entriesRef.orderByChild("userId").equalTo(userId)
                .limitToLast(1);
        query.addListenerForSingleValueEvent(listener);
    }

    /**
     * Callback interface for retrieving a list of filtered EmotionEntry objects.
     * This listener is used when querying the Firebase Realtime Database
     * for entries that match a specific condition (e.g. date).
     * Provides filtered list of entries to caller once data is ready.
     */
    public interface FilteredEntriesListener {
        void onSuccess(List<EmotionEntry> entries);
        void onFailure(DatabaseError error);
    }

    /**
     * Get all emotion entries for a user on a specific date
     * Uses LocalDate and custom callback
     */
    public void getEntriesForDate(String userId, LocalDate date, FilteredEntriesListener listener) {
        long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Query query = entriesRef.orderByChild("userId").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<EmotionEntry> filteredEntries = new ArrayList<>();

                for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                    EmotionEntry entry = entrySnapshot.getValue(EmotionEntry.class);
                    if (entry != null && entry.getTimestamp() != null) {
                        long timestamp = entry.getTimestamp().getTime();
                        if (timestamp >= startOfDay && timestamp < endOfDay) {
                            filteredEntries.add(entry);
                        }
                    }
                }

                listener.onSuccess(filteredEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onFailure(error);
            }
        });
    }

    /**
     * Get all emotion entries for a user
     * Uses LocalDate and custom callback
     */
    public void getAllEntries(String userId, FilteredEntriesListener listener) {
        Query query = entriesRef.orderByChild("userId").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<EmotionEntry> allEntries = new ArrayList<>();

                for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                    EmotionEntry entry = entrySnapshot.getValue(EmotionEntry.class);
                    if (entry != null && entry.getTimestamp() != null) {
                        allEntries.add(entry);
                    }
                }

                listener.onSuccess(allEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onFailure(error);
            }
        });
    }

    /**
     * Get all emotion entries for a user within a specific date range
     * Uses LocalDate and custom callback
     */
    public void getEntriesInRange(String userId, LocalDate startDate, LocalDate endDate, FilteredEntriesListener listener) {
        long startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Query query = entriesRef.orderByChild("userId").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<EmotionEntry> filteredEntries = new ArrayList<>();

                for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                    EmotionEntry entry = entrySnapshot.getValue(EmotionEntry.class);
                    if (entry != null && entry.getTimestamp() != null) {
                        long timestamp = entry.getTimestamp().getTime();
                        if (timestamp >= startMillis && timestamp < endMillis) {
                            filteredEntries.add(entry);
                        }
                    }
                }

                listener.onSuccess(filteredEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onFailure(error);
            }
        });
    }

    public interface EntryDeleteCallback {
        void onSuccess();
        void onFailure(DatabaseError error);
    }

    public void deleteEntry(EmotionEntry entry, EntryDeleteCallback callback) {
        if (entry.getEntryId() == null) {
            callback.onFailure(DatabaseError.fromException(new IllegalArgumentException("Entry has no ID")));
            return;
        }

        entriesRef.child(entry.getEntryId()).removeValue((error, ref) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Get all emotion entries for a specific date
     */
    /*
    public void getEntriesForDateOLD(String userId, Date date, ValueEventListener listener) {
        // Convert date to start and end timestamps
        // This is simplified - you'd need to handle time conversion properly
        long startOfDay = date.getTime(); // Start of day
        long endOfDay = startOfDay + 86400000; // End of day (24 hours later)

        Query query = entriesRef.orderByChild("userId").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<EmotionEntry> entries = new ArrayList<>();
                for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                    EmotionEntry entry = entrySnapshot.getValue(EmotionEntry.class);
                    if (entry != null && entry.getTimestamp() != null) {
                        long timestamp = entry.getTimestamp().getTime();
                        if (timestamp >= startOfDay && timestamp < endOfDay) {
                            entries.add(entry);
                        }
                    }
                }

                // Create a DataSnapshot containing just the filtered entries
                // This is a simplified approach
                listener.onDataChange(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error);
            }
        });
    }
    */

    /**
     * Get all emotions
     */
    public void getAllEmotions(ValueEventListener listener) {
        emotionsRef.addListenerForSingleValueEvent(listener);
    }

    /**
     * Get emotions by category
     */
    public void getEmotionsByCategory(Emotion.Category category, ValueEventListener listener) {
        Query query = emotionsRef.orderByChild("category").equalTo(category.name());
        query.addListenerForSingleValueEvent(listener);
    }

    /**
     * Delete an emotion entry
     */
    public Task<Void> deleteEmotionEntry(String entryId) {
        return entriesRef.child(entryId).removeValue();
    }

    /**
     * Upload image to Firebase Storage
     */
    public StorageReference uploadImage(String userId, byte[] imageData) {
        String imageName = "image_" + new Date().getTime() + ".jpg";
        StorageReference imageRef = storage.getReference("images")
                .child(userId)
                .child(imageName);

        imageRef.putBytes(imageData);
        return imageRef;
    }

    /**
     * Initialize default emotions in the database (if not already present)
     */
    private void initDefaultEmotions() {
        emotionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // No emotions exist, create defaults
                    createDefaultEmotions();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    /**
     * Create default set of emotions
     */
    private void createDefaultEmotions() {
        Map<String, Emotion> emotions = new HashMap<>();

        // High Energy Pleasant
        emotions.put("excited", new Emotion("Excited", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling very enthusiastic and eager", 10));
        emotions.put("joyful", new Emotion("Joyful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling happiness and delight", 9));
        emotions.put("proud", new Emotion("Proud", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling deep satisfaction with achievements", 8));
        emotions.put("optimistic", new Emotion("Optimistic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling hopeful about the future", 7));
        emotions.put("cheerful", new Emotion("Cheerful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling noticeably happy and positive", 6));

        // High Energy Unpleasant
        emotions.put("angry", new Emotion("Angry", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling strong displeasure or hostility", 10));
        emotions.put("anxious", new Emotion("Anxious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried or nervous", 9));
        emotions.put("frustrated", new Emotion("Frustrated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling upset and annoyed at unresolved problems", 8));
        emotions.put("stressed", new Emotion("Stressed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling mental or emotional strain", 7));
        emotions.put("overwhelmed", new Emotion("Overwhelmed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling buried under too many tasks or emotions", 6));

        // Low Energy Pleasant
        emotions.put("calm", new Emotion("Calm", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling tranquil and peaceful", 4));
        emotions.put("content", new Emotion("Content", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling satisfied with current state", 3));
        emotions.put("relaxed", new Emotion("Relaxed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from tension", 2));
        emotions.put("grateful", new Emotion("Grateful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling thankful and appreciative", 4));
        emotions.put("serene", new Emotion("Serene", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling clear and calm", 1));

        // Low Energy Unpleasant
        emotions.put("sad", new Emotion("Sad", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling sorrow or unhappiness", 4));
        emotions.put("tired", new Emotion("Tired", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling in need of rest or sleep", 3));
        emotions.put("bored", new Emotion("Bored", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling weary from lack of interest", 2));
        emotions.put("disappointed", new Emotion("Disappointed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling let down or discouraged", 4));
        emotions.put("lonely", new Emotion("Lonely", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling isolated or without companionship", 3));

        // Save all emotions to database
        emotions.forEach((key, emotion) -> {
            emotionsRef.child(key).setValue(emotion);
        });
    }
}