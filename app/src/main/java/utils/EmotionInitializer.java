package utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Emotion;

/**
 * Utility class to handle emotion initialization and database checks
 */
public class EmotionInitializer {
    private static final String TAG = "EmotionInitializer";

    private final Context context;
    private final DatabaseReference emotionsRef;

    public interface EmotionInitCallback {
        void onInitComplete(boolean success, List<Emotion> emotions);
    }

    public EmotionInitializer(Context context) {
        this.context = context;
        this.emotionsRef = FirebaseDatabase.getInstance().getReference("emotions");
    }

    /**
     * Verify emotions exist in the database and initialize them if needed
     */
    public void verifyAndInitEmotions(EmotionInitCallback callback) {

        // Create the map of all expected emotions
        Map<String, Emotion> expectedEmotions = createExpectedEmotions();

        emotionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {

                    // Check if all expected emotions exist in the database
                    Set<String> existingEmotionKeys = new HashSet<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        existingEmotionKeys.add(child.getKey());
                    }

                    // Check for missing emotions
                    Set<String> missingEmotionKeys = new HashSet<>(expectedEmotions.keySet());
                    missingEmotionKeys.removeAll(existingEmotionKeys);

                    if (missingEmotionKeys.isEmpty()) {
                        Log.d(TAG, "All expected emotions exist in database");
                        // All emotions exist, return them
                        List<Emotion> emotions = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Emotion emotion = child.getValue(Emotion.class);
                            if (emotion != null) {
                                emotions.add(emotion);
                            }
                        }

                        if (callback != null) {
                            callback.onInitComplete(true, emotions);
                        }
                    } else {
                        // Some emotions are missing, add them
                        Log.d(TAG, "Missing " + missingEmotionKeys.size() + " emotions, adding them to database");

                        Map<String, Object> updates = new HashMap<>();
                        for (String key : missingEmotionKeys) {
                            updates.put(key, expectedEmotions.get(key));
                        }

                        emotionsRef.updateChildren(updates).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Successfully added missing emotions");

                                // Now fetch all emotions again
                                emotionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot updatedSnapshot) {
                                        List<Emotion> emotions = new ArrayList<>();
                                        for (DataSnapshot child : updatedSnapshot.getChildren()) {
                                            Emotion emotion = child.getValue(Emotion.class);
                                            if (emotion != null) {
                                                emotions.add(emotion);
                                            }
                                        }

                                        if (callback != null) {
                                            callback.onInitComplete(true, emotions);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error fetching emotions after update: " + error.getMessage());
                                        if (callback != null) {
                                            callback.onInitComplete(false, new ArrayList<>());
                                        }
                                    }
                                });
                            } else {
                                Log.e(TAG, "Failed to add missing emotions: " + task.getException().getMessage());
                                if (callback != null) {
                                    callback.onInitComplete(false, new ArrayList<>());
                                }
                            }
                        });
                    }
                } else {
                    // No emotions found, initialize all of them
                    Log.d(TAG, "No emotions found, initializing all " + expectedEmotions.size() + " emotions");
                    initializeAllEmotions(expectedEmotions, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking emotions: " + error.getMessage());
                showToast("Error: " + error.getMessage());
                if (callback != null) {
                    callback.onInitComplete(false, new ArrayList<>());
                }
            }
        });
    }

    /**
     * Force reset of the entire emotions database
     */
    public void forceResetEmotions(EmotionInitCallback callback) {
        Log.d(TAG, "Forcing complete reset of emotions database");

        emotionsRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, Emotion> allEmotions = createExpectedEmotions();
                initializeAllEmotions(allEmotions, callback);
            } else {
                Log.e(TAG, "Failed to clear emotions database: " + task.getException().getMessage());
                if (callback != null) {
                    callback.onInitComplete(false, new ArrayList<>());
                }
            }
        });
    }

    /**
     * Initialize all emotions in a single transaction
     */
    private void initializeAllEmotions(Map<String, Emotion> emotions, EmotionInitCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        List<Emotion> resultEmotions = new ArrayList<>();

        for (Map.Entry<String, Emotion> entry : emotions.entrySet()) {
            updates.put(entry.getKey(), entry.getValue());
            resultEmotions.add(entry.getValue());
        }

        emotionsRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully initialized " + emotions.size() + " emotions");
                if (callback != null) {
                    callback.onInitComplete(true, resultEmotions);
                }
            } else {
                Log.e(TAG, "Failed to initialize emotions: " + task.getException().getMessage());
                if (callback != null) {
                    callback.onInitComplete(false, new ArrayList<>());
                }
            }
        });
    }

    /**
     * Create the full list of expected emotions
     * This should include ALL emotions that should be in the database
     */
    private Map<String, Emotion> createExpectedEmotions() {
        Map<String, Emotion> emotions = new HashMap<>();

        // High Energy Pleasant
        emotions.put("excited", new Emotion("Excited", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling very enthusiastic and eager", 10));
        emotions.put("joyful", new Emotion("Joyful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling happiness and delight", 9));
        emotions.put("proud", new Emotion("Proud", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling deep satisfaction with achievements", 8));
        emotions.put("optimistic", new Emotion("Optimistic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling hopeful about the future", 7));
        emotions.put("cheerful", new Emotion("Cheerful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling noticeably happy and positive", 6));
        emotions.put("energetic", new Emotion("Energetic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling full of energy and vigor", 10));
        emotions.put("enthusiastic", new Emotion("Enthusiastic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling intense and eager interest", 9));
        emotions.put("elated", new Emotion("Elated", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extremely happy and exhilarated", 10));
        emotions.put("inspired", new Emotion("Inspired", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling mentally stimulated to do something", 8));
        emotions.put("passionate", new Emotion("Passionate", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling intense desire or enthusiasm", 9));
        emotions.put("confident", new Emotion("Confident", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling self-assured and certain", 7));
        emotions.put("accomplished", new Emotion("Accomplished", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling successful completion of something", 8));
        emotions.put("adventurous", new Emotion("Adventurous", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling willing to take risks and try new things", 8));
        emotions.put("playful", new Emotion("Playful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling full of fun and high spirits", 7));
        emotions.put("amused", new Emotion("Amused", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling entertained or finding something funny", 6));
        emotions.put("ecstatic", new Emotion("Ecstatic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling overwhelming happiness and joy", 10));
        emotions.put("amazed", new Emotion("Amazed", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling great surprise or wonder", 9));
        emotions.put("astonished", new Emotion("Astonished", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extremely surprised", 9));
        emotions.put("eager", new Emotion("Eager", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling keen interest, enthusiasm, or impatience", 8));
        emotions.put("hopeful", new Emotion("Hopeful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling optimistic about a future outcome", 7));
        emotions.put("thrilled", new Emotion("Thrilled", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extremely excited and pleased", 10));
        emotions.put("delighted", new Emotion("Delighted", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling great pleasure", 9));
        emotions.put("jubilant", new Emotion("Jubilant", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling extreme joy, especially because of success", 10));
        emotions.put("lively", new Emotion("Lively", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling full of life and energy", 8));
        emotions.put("motivated", new Emotion("Motivated", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling eager to act or work", 8));

        // High Energy Unpleasant
        emotions.put("angry", new Emotion("Angry", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling strong displeasure or hostility", 10));
        emotions.put("anxious", new Emotion("Anxious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried or nervous", 9));
        emotions.put("frustrated", new Emotion("Frustrated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling upset and annoyed at unresolved problems", 8));
        emotions.put("stressed", new Emotion("Stressed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling mental or emotional strain", 7));
        emotions.put("overwhelmed", new Emotion("Overwhelmed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling buried under too many tasks or emotions", 6));
        emotions.put("furious", new Emotion("Furious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling extremely angry", 10));
        emotions.put("enraged", new Emotion("Enraged", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling intense anger", 10));
        emotions.put("outraged", new Emotion("Outraged", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling extreme anger from perceived injustice", 10));
        emotions.put("irritated", new Emotion("Irritated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling annoyed or slightly angry", 7));
        emotions.put("agitated", new Emotion("Agitated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling troubled, nervous, or upset", 8));
        emotions.put("nervous", new Emotion("Nervous", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling easily agitated or worried", 8));
        emotions.put("panicked", new Emotion("Panicked", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling sudden uncontrollable fear or anxiety", 10));
        emotions.put("afraid", new Emotion("Afraid", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling fear or apprehension", 9));
        emotions.put("terrified", new Emotion("Terrified", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling extreme fear", 10));
        emotions.put("shocked", new Emotion("Shocked", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling sudden surprise or alarm", 9));
        emotions.put("disgusted", new Emotion("Disgusted", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling strong aversion or repulsion", 8));
        emotions.put("resentful", new Emotion("Resentful", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling bitter or indignant", 7));
        emotions.put("jealous", new Emotion("Jealous", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling resentment toward others for their advantages", 8));
        emotions.put("envious", new Emotion("Envious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling discontent with someone's position or possessions", 7));
        emotions.put("impatient", new Emotion("Impatient", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling restless or eager for something to happen", 7));
        emotions.put("indignant", new Emotion("Indignant", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling anger at perceived unfair treatment", 8));
        emotions.put("restless", new Emotion("Restless", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling unable to rest or relax", 7));
        emotions.put("alarmed", new Emotion("Alarmed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling frightened, disturbed, or in danger", 9));
        emotions.put("disturbed", new Emotion("Disturbed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling troubled or uneasy", 7));
        emotions.put("perplexed", new Emotion("Perplexed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling confused or puzzled", 6));

        // Low Energy Pleasant
        emotions.put("calm", new Emotion("Calm", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling tranquil and peaceful", 4));
        emotions.put("content", new Emotion("Content", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling satisfied with current state", 3));
        emotions.put("relaxed", new Emotion("Relaxed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from tension", 2));
        emotions.put("grateful", new Emotion("Grateful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling thankful and appreciative", 4));
        emotions.put("serene", new Emotion("Serene", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling clear and calm", 1));
        emotions.put("peaceful", new Emotion("Peaceful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from disturbance", 2));
        emotions.put("satisfied", new Emotion("Satisfied", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling content with fulfillment of desire", 3));
        emotions.put("at ease", new Emotion("At Ease", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling comfortable and relaxed", 2));
        emotions.put("fulfilled", new Emotion("Fulfilled", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling satisfied or happy because of fully developing one's potential", 4));
        emotions.put("comforted", new Emotion("Comforted", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling consoled in a time of distress", 3));
        emotions.put("cozy", new Emotion("Cozy", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling comfortable, warm, and relaxed", 2));
        emotions.put("secure", new Emotion("Secure", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling safe and free from worry", 3));
        emotions.put("tranquil", new Emotion("Tranquil", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from disturbance; calm", 1));
        emotions.put("carefree", new Emotion("Carefree", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from anxiety or responsibility", 3));
        emotions.put("relieved", new Emotion("Relieved", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling reassured and free from anxiety or distress", 3));
        emotions.put("blessed", new Emotion("Blessed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling a deep sense of well-being or grace", 4));
        emotions.put("balanced", new Emotion("Balanced", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling stable or in equilibrium", 3));
        emotions.put("loved", new Emotion("Loved", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling deep affection from others", 4));
        emotions.put("appreciated", new Emotion("Appreciated", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling valued or recognized", 4));
        emotions.put("refreshed", new Emotion("Refreshed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling revitalized or reinvigorated", 4));
        emotions.put("hopeful", new Emotion("Hopeful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling optimistic about a future outcome", 4));
        emotions.put("mellow", new Emotion("Mellow", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling softened by age or experience; gentle", 2));
        emotions.put("nostalgic", new Emotion("Nostalgic", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling a sentimental longing for the past", 3));
        emotions.put("tender", new Emotion("Tender", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling gentle, loving, or kind", 3));
        emotions.put("compassionate", new Emotion("Compassionate", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling concern for the sufferings of others", 4));

        // Low Energy Unpleasant
        emotions.put("sad", new Emotion("Sad", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling sorrow or unhappiness", 4));
        emotions.put("tired", new Emotion("Tired", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling in need of rest or sleep", 3));
        emotions.put("bored", new Emotion("Bored", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling weary from lack of interest", 2));
        emotions.put("disappointed", new Emotion("Disappointed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling let down or discouraged", 4));
        emotions.put("lonely", new Emotion("Lonely", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling isolated or without companionship", 3));
        emotions.put("depressed", new Emotion("Depressed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling severe despondency and dejection", 4));
        emotions.put("gloomy", new Emotion("Gloomy", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling dark or depressed", 4));
        emotions.put("miserable", new Emotion("Miserable", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling wretchedly unhappy or uncomfortable", 4));
        emotions.put("hopeless", new Emotion("Hopeless", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling despair; having no expectation of good", 4));
        emotions.put("apathetic", new Emotion("Apathetic", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling lack of interest, enthusiasm, or concern", 2));
        emotions.put("empty", new Emotion("Empty", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling a lack of meaning or purpose", 3));
        emotions.put("exhausted", new Emotion("Exhausted", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling extremely tired", 3));
        emotions.put("drained", new Emotion("Drained", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling depleted of energy or resources", 3));
        emotions.put("defeated", new Emotion("Defeated", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling beaten or having lost", 4));
        emotions.put("neglected", new Emotion("Neglected", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling not receiving proper care or attention", 3));
        emotions.put("rejected", new Emotion("Rejected", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling dismissed or refused", 4));
        emotions.put("isolated", new Emotion("Isolated", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling alone or separated from others", 3));
        emotions.put("helpless", new Emotion("Helpless", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling unable to help oneself; powerless", 4));
        emotions.put("guilty", new Emotion("Guilty", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling responsible for wrongdoing", 4));
        emotions.put("ashamed", new Emotion("Ashamed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling embarrassed or guilty due to actions", 4));
        emotions.put("regretful", new Emotion("Regretful", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling sad, repentant, or disappointed over something", 4));
        emotions.put("homesick", new Emotion("Homesick", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling longing for home during absence from it", 3));
        emotions.put("grieving", new Emotion("Grieving", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling intense sorrow, especially from loss", 4));
        emotions.put("insecure", new Emotion("Insecure", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling uncertain or anxious about oneself", 3));
        emotions.put("embarrassed", new Emotion("Embarrassed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling self-conscious, ashamed, or awkward", 3));

        return emotions;
    }

    private void showToast(String message) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}