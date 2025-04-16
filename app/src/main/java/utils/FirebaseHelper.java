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
import com.google.firebase.storage.UploadTask;


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
    public UploadTask uploadImage(String userId, byte[] imageData) {
        String imageName = "image_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child("images")
                .child(userId)
                .child(imageName);

        // Return the UploadTask so the caller can add listeners
        return imageRef.putBytes(imageData);
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
     * Moodmeter emotions
     */
    private void createDefaultEmotions() {
        Map<String, Emotion> emotions = new HashMap<>();

        // HIGH ENERGY, LOW PLEASANTNESS (Red quadrant)
        emotions.put("enraged", new Emotion("Enraged", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling intense, uncontrollable anger", 25));
        emotions.put("panicked", new Emotion("Panicked", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling sudden, overwhelming fear", 24));
        emotions.put("stressed", new Emotion("Stressed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling mental or emotional pressure", 23));
        emotions.put("jittery", new Emotion("Jittery", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling nervous and unable to relax", 22));
        emotions.put("shocked", new Emotion("Shocked", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling sudden, intense surprise", 21));
        emotions.put("livid", new Emotion("Livid", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling extremely angry", 20));
        emotions.put("furious", new Emotion("Furious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling intense, passionate anger", 19));
        emotions.put("frustrated", new Emotion("Frustrated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling upset and annoyed at unresolved problems", 18));
        emotions.put("tense", new Emotion("Tense", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling physically or mentally strained", 17));
        emotions.put("stunned", new Emotion("Stunned", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling shocked to the point of being unable to react", 16));
        emotions.put("fuming", new Emotion("Fuming", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling extremely angry and showing it", 15));
        emotions.put("frightened", new Emotion("Frightened", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling afraid or scared", 14));
        emotions.put("angry", new Emotion("Angry", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling strong displeasure or hostility", 13));
        emotions.put("nervous", new Emotion("Nervous", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried and uneasy", 12));
        emotions.put("restless", new Emotion("Restless", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling unable to rest or relax", 11));
        emotions.put("anxious", new Emotion("Anxious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried or nervous", 10));
        emotions.put("apprehensive", new Emotion("Apprehensive", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling fear or anxiety about the future", 9));
        emotions.put("worried", new Emotion("Worried", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling troubled about actual or potential problems", 8));
        emotions.put("irritated", new Emotion("Irritated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling annoyed or bothered", 7));
        emotions.put("annoyed", new Emotion("Annoyed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling slightly angry", 6));
        emotions.put("repulsed", new Emotion("Repulsed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling strong dislike or disgust", 5));
        emotions.put("troubled", new Emotion("Troubled", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried and unhappy", 4));
        emotions.put("concerned", new Emotion("Concerned", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried or anxious", 3));
        emotions.put("uneasy", new Emotion("Uneasy", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling uncomfortable or worried", 2));
        emotions.put("peeved", new Emotion("Peeved", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling mildly annoyed", 1));

        // HIGH ENERGY, HIGH PLEASANTNESS (Yellow quadrant)
        emotions.put("surprised", new Emotion("Surprised", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling caught off guard by an unexpected event", 25));
        emotions.put("upbeat", new Emotion("Upbeat", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling positive and cheerful", 24));
        emotions.put("festive", new Emotion("Festive", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling joyous and celebratory", 23));
        emotions.put("exhilarated", new Emotion("Exhilarated", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extreme happiness and excitement", 22));
        emotions.put("ecstatic", new Emotion("Ecstatic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling overwhelming happiness and joy", 21));
        emotions.put("hyper", new Emotion("Hyper", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling energetic and unable to calm down", 20));
        emotions.put("cheerful", new Emotion("Cheerful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling noticeably happy and positive", 19));
        emotions.put("motivated", new Emotion("Motivated", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling eager and driven to act", 18));
        emotions.put("inspired", new Emotion("Inspired", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling creative and mentally stimulated", 17));
        emotions.put("elated", new Emotion("Elated", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extremely happy and excited", 16));
        emotions.put("energized", new Emotion("Energized", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling full of energy and vitality", 15));
        emotions.put("lively", new Emotion("Lively", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling full of life and energy", 14));
        emotions.put("excited", new Emotion("Excited", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling very enthusiastic and eager", 13));
        emotions.put("optimistic", new Emotion("Optimistic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling hopeful about the future", 12));
        emotions.put("enthusiastic", new Emotion("Enthusiastic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling eager interest and excitement", 11));
        emotions.put("pleased", new Emotion("Pleased", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling happiness and satisfaction", 10));
        emotions.put("focused", new Emotion("Focused", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling concentrated attention", 9));
        emotions.put("happy", new Emotion("Happy", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling or showing pleasure and contentment", 8));
        emotions.put("proud", new Emotion("Proud", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling deep satisfaction with achievements", 7));
        emotions.put("thrilled", new Emotion("Thrilled", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extremely pleased and excited", 6));
        emotions.put("pleasant", new Emotion("Pleasant", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling agreeable and enjoyable", 5));
        emotions.put("joyful", new Emotion("Joyful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling happiness and delight", 4));
        emotions.put("hopeful", new Emotion("Hopeful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling optimistic about the future", 3));
        emotions.put("playful", new Emotion("Playful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling lighthearted and full of fun", 2));
        emotions.put("blissful", new Emotion("Blissful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extremely happy and serene", 1));

        // LOW ENERGY, LOW PLEASANTNESS (Blue quadrant)
        emotions.put("disgusted", new Emotion("Disgusted", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling strong dislike or distaste", 25));
        emotions.put("glum", new Emotion("Glum", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling dejected and unhappy", 24));
        emotions.put("disappointed", new Emotion("Disappointed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling let down by failing expectations", 23));
        emotions.put("down", new Emotion("Down", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling unhappy or depressed", 22));
        emotions.put("apathetic", new Emotion("Apathetic", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling indifferent or uncaring", 21));
        emotions.put("pessimistic", new Emotion("Pessimistic", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling that bad things will happen", 20));
        emotions.put("morose", new Emotion("Morose", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling gloomy and sullen", 19));
        emotions.put("discouraged", new Emotion("Discouraged", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling a loss of confidence or enthusiasm", 18));
        emotions.put("sad", new Emotion("Sad", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling sorrow or unhappiness", 17));
        emotions.put("bored", new Emotion("Bored", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling weary from lack of interest", 16));
        emotions.put("alienated", new Emotion("Alienated", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling isolated or disconnected", 15));
        emotions.put("miserable", new Emotion("Miserable", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling extremely unhappy or uncomfortable", 14));
        emotions.put("lonely", new Emotion("Lonely", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling isolated or without companionship", 13));
        emotions.put("disheartened", new Emotion("Disheartened", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling a loss of hope or courage", 12));
        emotions.put("tired", new Emotion("Tired", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling in need of rest or sleep", 11));
        emotions.put("despondent", new Emotion("Despondent", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling extreme discouragement", 10));
        emotions.put("depressed", new Emotion("Depressed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling persistent sadness and loss of interest", 9));
        emotions.put("sullen", new Emotion("Sullen", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling resentful and gloomy", 8));
        emotions.put("exhausted", new Emotion("Exhausted", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling completely drained of energy", 7));
        emotions.put("fatigued", new Emotion("Fatigued", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling extreme physical or mental tiredness", 6));
        emotions.put("despairing", new Emotion("Despairing", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling complete loss of hope", 5));
        emotions.put("hopeless", new Emotion("Hopeless", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling that there is no possibility of comfort or success", 4));
        emotions.put("desolate", new Emotion("Desolate", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling abandoned and lonely", 3));
        emotions.put("spent", new Emotion("Spent", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling completely used up", 2));
        emotions.put("drained", new Emotion("Drained", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling completely exhausted physically or emotionally", 1));

        // LOW ENERGY, HIGH PLEASANTNESS (Green quadrant)
        emotions.put("at_ease", new Emotion("At Ease", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling comfortable and relaxed", 25));
        emotions.put("easygoing", new Emotion("Easygoing", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling relaxed and tolerant", 24));
        emotions.put("content", new Emotion("Content", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling satisfied with current state", 23));
        emotions.put("loving", new Emotion("Loving", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling deep affection", 22));
        emotions.put("fulfilled", new Emotion("Fulfilled", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling satisfied and complete", 21));
        emotions.put("calm", new Emotion("Calm", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling tranquil and peaceful", 20));
        emotions.put("secure", new Emotion("Secure", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling safe and free from worry", 19));
        emotions.put("satisfied", new Emotion("Satisfied", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling content with what one has", 18));
        emotions.put("grateful", new Emotion("Grateful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling thankful and appreciative", 17));
        emotions.put("touched", new Emotion("Touched", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling emotionally moved", 16));
        emotions.put("relaxed", new Emotion("Relaxed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from tension", 15));
        emotions.put("chill", new Emotion("Chill", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling relaxed and easygoing", 14));
        emotions.put("restful", new Emotion("Restful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling calm and peaceful", 13));
        emotions.put("blessed", new Emotion("Blessed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling fortunate and favored", 12));
        emotions.put("balanced", new Emotion("Balanced", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling stable and harmonious", 11));
        emotions.put("mellow", new Emotion("Mellow", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling softened by experience", 10));
        emotions.put("thoughtful", new Emotion("Thoughtful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling considerate and reflective", 9));
        emotions.put("peaceful", new Emotion("Peaceful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from disturbance", 8));
        emotions.put("comfortable", new Emotion("Comfortable", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling physically at ease", 7));
        emotions.put("carefree", new Emotion("Carefree", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from worry or responsibility", 6));
        emotions.put("sleepy", new Emotion("Sleepy", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling ready for sleep", 5));
        emotions.put("complacent", new Emotion("Complacent", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling self-satisfied", 4));
        emotions.put("tranquil", new Emotion("Tranquil", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from disturbance or agitation", 3));
        emotions.put("cozy", new Emotion("Cozy", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling warm and comfortable", 2));
        emotions.put("serene", new Emotion("Serene", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling clear and calm", 1));

        // Save all emotions to database
        emotions.forEach((key, emotion) -> {
            emotionsRef.child(key).setValue(emotion);
        });
    }
}