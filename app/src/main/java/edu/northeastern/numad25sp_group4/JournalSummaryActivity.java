package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;

import adapters.EntryImageAdapter;
import models.Emotion;
import models.EmotionEntry;
import utils.FirebaseHelper;

public class JournalSummaryActivity extends AppCompatActivity implements EntryImageAdapter.OnImageRemoveListener {

    private static final int MAX_CHAR_COUNT = 500;
    private static final int MAX_IMAGES = 3;
    private static final int MAX_TAGS = 6;
    private static final int REQUEST_IMAGE_PERMISSION = 101;
    private static final int REQUEST_SPEECH_PERMISSION = 102;

    // Views
    private TextView tvEmotionSummary;
    private Button btnAddAnotherEmotion;
    private EditText etJournalText;
    private TextView tvCharCount;
    private Button btnVoiceToText;
    private ImageButton btnCamera;
    private TextView tvImageCount;
    private RecyclerView recyclerImages;
    private EditText etTags;
    private LinearLayout llTags;
    private LinearLayout llTagsDisplay;
    private HorizontalScrollView tagsScrollView;
    private ImageView ivBackArrow;
    private Button btnSaveEntry;

    // Data
    private EmotionEntry currentEntry;
    private List<String> tags = new ArrayList<>();
    private List<Uri> imageUris = new ArrayList<>();
    private List<byte[]> imageBytesList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;
    private EntryImageAdapter imageAdapter;

    // Voice recognition
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean isPaused = false;
    private AlertDialog voiceDialog = null;
    private StringBuilder currentSessionText = new StringBuilder();
    private int availableCharacters = MAX_CHAR_COUNT;
    private Animation pulseAnimation;
    private Handler recognitionHandler = new Handler(Looper.getMainLooper());
    private Runnable restartRecognitionRunnable;

    // Activity Result Launchers
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        if (imageUris.size() < MAX_IMAGES) {
                            addImage(selectedImageUri);
                        } else {
                            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_summary);

        // Initialize Firebase Helper
        firebaseHelper = FirebaseHelper.getInstance();

        // Initialize entry
        initEmotionEntry();

        // Initialize views
        initViews();

        // Set up animations
        setupAnimations();

        // Set up listeners
        setupListeners();

        // Set up adapters
        setupAdapters();

        // Set limits
        setupLimits();

        // Update UI with current entry data
        updateUI();

        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeechRecognizer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initialize speech recognizer if needed
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeechRecognizer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop listening and release resources
        stopVoiceRecognition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up speech recognizer
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    private void initEmotionEntry() {
        currentEntry = null;

        // Check if we're getting a current entry from intent (add second emotion flow)
        if (getIntent().hasExtra("CURRENT_ENTRY")) {
            try {
                currentEntry = (EmotionEntry) getIntent().getSerializableExtra("CURRENT_ENTRY");
            } catch (Exception e) {
                // Failed to get the current entry
                e.printStackTrace();
            }
        }

        // Check if we're editing an existing entry - NOTE: entryeditactivity does this instead
        if (currentEntry == null && getIntent().hasExtra("ENTRY_ID")) {
            String entryId = getIntent().getStringExtra("ENTRY_ID");
            currentEntry = new EmotionEntry();
            currentEntry.setEntryId(entryId);
        } else if (currentEntry == null && getIntent().hasExtra("EMOTION")) {
            // Get the emotion from the intent
            Emotion emotion = (Emotion) getIntent().getSerializableExtra("EMOTION");

            // Create a new entry
            currentEntry = new EmotionEntry();

            // Set current user ID and timestamp
            if (firebaseHelper.getCurrentUser() != null) {
                currentEntry.setUserId(firebaseHelper.getCurrentUser().getUid());
            }
            currentEntry.setTimestamp(new Date());

            // Add the emotion to the entry
            if (emotion != null) {
                currentEntry.addEmotion(emotion);
            }
        }

        // If still null, create a new entry (this shouldn't happen)
        if (currentEntry == null) {
            currentEntry = new EmotionEntry();
            if (firebaseHelper.getCurrentUser() != null) {
                currentEntry.setUserId(firebaseHelper.getCurrentUser().getUid());
            }
            currentEntry.setTimestamp(new Date());
        }
    }

    private void initViews() {
        tvEmotionSummary = findViewById(R.id.tv_emotion_summary);
        btnAddAnotherEmotion = findViewById(R.id.btn_add_another_emotion);
        etJournalText = findViewById(R.id.et_journal_text);
        tvCharCount = findViewById(R.id.tv_char_count);
        btnVoiceToText = findViewById(R.id.btn_voice_to_text);
        btnCamera = findViewById(R.id.btn_camera);
        tvImageCount = findViewById(R.id.tv_image_count);
        recyclerImages = findViewById(R.id.recycler_images);
        etTags = findViewById(R.id.et_tags);
        llTags = findViewById(R.id.ll_tags);
        llTagsDisplay = findViewById(R.id.ll_tags_display);
        tagsScrollView = findViewById(R.id.tags_scroll_view);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        btnSaveEntry = findViewById(R.id.btn_save_entry);

        // Set up RecyclerView
        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupAnimations() {
        // Create pulsing animation for mic icon
        pulseAnimation = new ScaleAnimation(
                1f, 1.2f, // Start and end X scale
                1f, 1.2f, // Start and end Y scale
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot Y
        pulseAnimation.setDuration(800);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        pulseAnimation.setRepeatMode(Animation.REVERSE);
    }

    private void setupListeners() {
        // Back arrow click listener
        ivBackArrow.setOnClickListener(v -> onBackPressed());

        // Add another emotion click listener
        btnAddAnotherEmotion.setOnClickListener(v -> {
            // Save current input fields to the entry
            saveInputFieldsToEntry();

            // Navigate back to the primary emotion selection screen with current entry
            Intent intent = new Intent(JournalSummaryActivity.this, PrimaryEmotionActivity.class);
            intent.putExtra("CURRENT_ENTRY", currentEntry);
            intent.putExtra("ADDING_SECOND_EMOTION", true);
            startActivity(intent);
        });

        // Journal text change listener for character count
        etJournalText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int currentLength = s.length();
                tvCharCount.setText(currentLength + "/" + MAX_CHAR_COUNT + " characters");

                // Disable voice to text if max characters reached
                if (currentLength >= MAX_CHAR_COUNT) {
                    btnVoiceToText.setEnabled(false);
                    btnVoiceToText.setAlpha(0.5f);
                } else {
                    btnVoiceToText.setEnabled(true);
                    btnVoiceToText.setAlpha(1.0f);
                }
            }
        });

        // Voice to text click listener
        btnVoiceToText.setOnClickListener(v -> {
            if (etJournalText.length() < MAX_CHAR_COUNT) {
                if (!isListening) {
                    startVoiceRecognition();
                } else {
                    stopVoiceRecognition();
                }
            } else {
                Toast.makeText(this, "Character limit reached", Toast.LENGTH_SHORT).show();
            }
        });

        btnCamera.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, we don't need READ_EXTERNAL_STORAGE for ACTION_OPEN_DOCUMENT
                openImagePicker();
            } else {
                // For Android 9 and below, we need READ_EXTERNAL_STORAGE permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_IMAGE_PERMISSION);
                } else {
                    openImagePicker();
                }
            }
        });

        // Tags input listener - improved to properly handle keyboard dismissal
        etTags.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                // Add the tag if text is not empty
                if (!etTags.getText().toString().trim().isEmpty()) {
                    addTag();
                }

                // Hide keyboard
                hideKeyboard(etTags);

                // Clear focus
                etTags.clearFocus();

                return true;
            }
            return false;
        });

        // Also handle space to add tag
        etTags.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if space was added
                if (s.toString().endsWith(" ") && s.length() > 1) {
                    String tagText = s.toString().trim();
                    if (!tagText.isEmpty()) {
                        addTag();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set an click listener on the parent layout to clear focus from tag input
        findViewById(R.id.card_tags).setOnClickListener(v -> {
            etTags.clearFocus();
            hideKeyboard(etTags);
        });

        // Save entry button click listener
        btnSaveEntry.setOnClickListener(v -> saveEntry());
    }

    /**
     * Sets up the speech recognizer with recognition listener
     */
    private void setupSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    if (voiceDialog != null) {
                        TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);
                        if (statusView != null) {
                            statusView.setText("Listening...");
                        }
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    // User started speaking
                    if (voiceDialog != null) {
                        TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);
                        if (statusView != null) {
                            statusView.setText("Hearing you...");
                        }
                    }
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Could use this to show a volume meter
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Not needed for our implementation
                }

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    if (voiceDialog != null) {
                        TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);
                        if (statusView != null) {
                            statusView.setText("Processing...");
                        }
                    }
                }

                @Override
                public void onError(int error) {
                    isListening = false;

                    if (voiceDialog == null || !voiceDialog.isShowing() || isPaused) {
                        return;
                    }

                    TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);

                    switch (error) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            // No speech detected, just restart
                            if (statusView != null) {
                                statusView.setText("Didn't catch that. Please try again.");
                            }
                            // Restart recognition after a short delay
                            if (!isPaused) {
                                recognitionHandler.postDelayed(restartRecognitionRunnable, 1000);
                            }
                            break;

                        case SpeechRecognizer.ERROR_NETWORK:
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            if (statusView != null) {
                                statusView.setText("Network error. Please check your connection.");
                            }
                            // Restart after a longer delay
                            if (!isPaused) {
                                recognitionHandler.postDelayed(restartRecognitionRunnable, 3000);
                            }
                            break;

                        case SpeechRecognizer.ERROR_CLIENT:
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            if (statusView != null) {
                                statusView.setText("Recognition error. Restarting...");
                            }
                            // Wait a bit and restart
                            if (!isPaused) {
                                recognitionHandler.postDelayed(restartRecognitionRunnable, 1000);
                            }
                            break;

                        default:
                            if (statusView != null) {
                                statusView.setText("Error occurred. Restarting...");
                            }
                            // Generic error, restart after a short delay
                            if (!isPaused) {
                                recognitionHandler.postDelayed(restartRecognitionRunnable, 1000);
                            }
                            break;
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;

                    if (voiceDialog == null || !voiceDialog.isShowing() || isPaused) {
                        return;
                    }

                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);

                        // Get UI references
                        TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);
                        TextView previewView = voiceDialog.findViewById(R.id.tv_recognition_preview);
                        TextView charCountView = voiceDialog.findViewById(R.id.tv_dialog_char_count);

                        // Add recognized text to the currentSessionText
                        if (availableCharacters > 0) {
                            // Add space if needed between segments
                            if (currentSessionText.length() > 0 &&
                                    !currentSessionText.toString().endsWith(" ") &&
                                    !currentSessionText.toString().endsWith("\n") &&
                                    !recognizedText.isEmpty()) {
                                currentSessionText.append(" ");
                            }

                            // Trim recognized text if needed
                            if (recognizedText.length() > availableCharacters) {
                                recognizedText = recognizedText.substring(0, availableCharacters);
                            }

                            // Append to current session text
                            currentSessionText.append(recognizedText);

                            // Show in preview
                            if (previewView != null) {
                                previewView.setText(currentSessionText.toString());
                            }

                            // Update character count
                            availableCharacters = MAX_CHAR_COUNT -
                                    (etJournalText.getText().length() + currentSessionText.length());

                            if (charCountView != null) {
                                charCountView.setText(availableCharacters + "/" + MAX_CHAR_COUNT + " characters available");
                            }

                            // Show successful transcription feedback briefly
                            if (statusView != null) {
                                statusView.setText("Recognized: \"" + recognizedText + "\"");
                            }

                            // If we still have characters and not paused, continue listening
                            if (availableCharacters > 0 && !isPaused) {
                                // Start next recognition after a short delay
                                recognitionHandler.postDelayed(restartRecognitionRunnable, 1000);
                            } else if (availableCharacters <= 0) {
                                // No characters left
                                if (statusView != null) {
                                    statusView.setText("Character limit reached");
                                }
                                TextView promptView = voiceDialog.findViewById(R.id.tv_voice_prompt);
                                if (promptView != null) {
                                    promptView.setText("Tap 'Done' to apply text");
                                }

                                // Stop animations
                                ImageView micIcon = voiceDialog.findViewById(R.id.iv_mic_icon);
                                if (micIcon != null) {
                                    micIcon.clearAnimation();
                                }

                                // Update pause button text
                                updatePauseButtonText(true);
                                isPaused = true;
                            }
                        } else {
                            // No characters left
                            if (statusView != null) {
                                statusView.setText("Character limit reached");
                            }
                            TextView promptView = voiceDialog.findViewById(R.id.tv_voice_prompt);
                            if (promptView != null) {
                                promptView.setText("Tap 'Done' to apply text");
                            }

                            // Stop animations
                            ImageView micIcon = voiceDialog.findViewById(R.id.iv_mic_icon);
                            if (micIcon != null) {
                                micIcon.clearAnimation();
                            }

                            // Update pause button text
                            updatePauseButtonText(true);
                            isPaused = true;
                        }
                    } else {
                        // No results, restart if not paused
                        TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);
                        if (statusView != null) {
                            statusView.setText("No speech detected. Please try again.");
                        }

                        if (!isPaused) {
                            recognitionHandler.postDelayed(restartRecognitionRunnable, 1000);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // We could show partial results here if desired
                    // For now, we'll just wait for final results
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Not used in this implementation
                }
            });

            // Create the runnable for restarting recognition
            restartRecognitionRunnable = () -> {
                if (isListening || isPaused || voiceDialog == null || !voiceDialog.isShowing()) {
                    return;
                }

                startListening();
            };
        }
    }

    /**
     * Starts speech recognition and shows the dialog
     */
    private void startVoiceRecognition() {
        // Check permission
        if (!checkVoicePermission()) {
            return;
        }

        // Reset state
        currentSessionText.setLength(0);
        isPaused = false;
        availableCharacters = MAX_CHAR_COUNT - etJournalText.getText().length();

        if (availableCharacters <= 0) {
            Toast.makeText(this, "Character limit reached", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show dialog
        showVoiceRecognitionDialog();

        // Start listening
        startListening();
    }

    /**
     * Shows the voice recognition dialog
     */
    private void showVoiceRecognitionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_voice_input, null);
        builder.setView(dialogView);

        // Get references to dialog views
        TextView tvTitle = dialogView.findViewById(R.id.tv_voice_title);
        ImageView ivMic = dialogView.findViewById(R.id.iv_mic_icon);
        TextView tvListeningStatus = dialogView.findViewById(R.id.tv_listening_status);
        TextView tvRecognitionPreview = dialogView.findViewById(R.id.tv_recognition_preview);
        TextView tvPrompt = dialogView.findViewById(R.id.tv_voice_prompt);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_dialog_char_count);
        Button btnPauseResume = dialogView.findViewById(R.id.btn_pause_resume_listening);
        Button btnStop = dialogView.findViewById(R.id.btn_stop_listening);

        // Update character count
        tvCharCount.setText(availableCharacters + "/" + MAX_CHAR_COUNT + " characters available");

        // Create and show dialog
        voiceDialog = builder.create();
        voiceDialog.setCancelable(false);
        voiceDialog.show();

        // Start animations to indicate active listening
        ivMic.startAnimation(pulseAnimation);

        // Setup button actions
        btnPauseResume.setOnClickListener(v -> togglePauseState());

        btnStop.setOnClickListener(v -> applyRecognizedTextAndClose());
    }

    /**
     * Starts the speech recognition
     */
    private void startListening() {
        if (speechRecognizer == null || isListening) {
            return;
        }

        // Update UI to show we're listening
        if (voiceDialog != null) {
            TextView statusView = voiceDialog.findViewById(R.id.tv_listening_status);
            if (statusView != null) {
                statusView.setText("Listening...");
            }

            TextView promptView = voiceDialog.findViewById(R.id.tv_voice_prompt);
            if (promptView != null) {
                promptView.setText("Speak now. Tap 'Pause' to pause or 'Done' when finished.");
            }

            ImageView micIcon = voiceDialog.findViewById(R.id.iv_mic_icon);
            if (micIcon != null) {
                micIcon.startAnimation(pulseAnimation);
            }

            // Update available character count
            TextView charCountView = voiceDialog.findViewById(R.id.tv_dialog_char_count);
            if (charCountView != null) {
                availableCharacters = MAX_CHAR_COUNT - (etJournalText.getText().length() + currentSessionText.length());
                charCountView.setText(availableCharacters + "/" + MAX_CHAR_COUNT + " characters available");
            }
        }

        // Create and configure recognition intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // Start recognizing
        try {
            speechRecognizer.startListening(intent);
            isListening = true;
        } catch (Exception e) {
            Toast.makeText(this, "Error starting speech recognition", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Stops speech recognition
     */
    private void stopVoiceRecognition() {
        // Cancel any pending recognition restarts
        recognitionHandler.removeCallbacks(restartRecognitionRunnable);

        // Stop speech recognizer if active
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }

        // Close dialog if open
        if (voiceDialog != null && voiceDialog.isShowing()) {
            voiceDialog.dismiss();
            voiceDialog = null;
        }

        // Reset state
        isPaused = false;
    }

    /**
     * Apply recognized text to the journal entry field and close the dialog
     */
    private void applyRecognizedTextAndClose() {
        if (currentSessionText.length() > 0) {
            // Get current text
            String currentText = etJournalText.getText().toString();

            // Add space if needed
            if (!currentText.isEmpty() && !currentText.endsWith(" ") && !currentText.endsWith("\n")) {
                currentText += " ";
            }

            // Combine with recognized text
            String combinedText = currentText + currentSessionText.toString();

            // Ensure we don't exceed max length
            if (combinedText.length() > MAX_CHAR_COUNT) {
                combinedText = combinedText.substring(0, MAX_CHAR_COUNT);
                Toast.makeText(this, "Text truncated to character limit", Toast.LENGTH_SHORT).show();
            }

            // Set the complete text back to the main field
            etJournalText.setText(combinedText);
            etJournalText.setSelection(etJournalText.getText().length());
        }

        // Stop recognition and close dialog
        stopVoiceRecognition();
    }

    /**
     * Toggle between paused and active listening states
     */
    private void togglePauseState() {
        isPaused = !isPaused;

        if (voiceDialog != null && voiceDialog.isShowing()) {
            // Update UI for paused/resume state
            updatePauseButtonText(isPaused);

            ImageView ivMic = voiceDialog.findViewById(R.id.iv_mic_icon);
            TextView tvStatus = voiceDialog.findViewById(R.id.tv_listening_status);

            if (isPaused) {
                // Update UI for paused state
                if (isListening) {
                    speechRecognizer.stopListening();
                    isListening = false;
                }

                if (ivMic != null) {
                    ivMic.clearAnimation();
                }

                if (tvStatus != null) {
                    tvStatus.setText("Paused");
                }

                // Cancel any pending recognition restarts
                recognitionHandler.removeCallbacks(restartRecognitionRunnable);
            } else {
                // Update UI for active listening
                if (ivMic != null) {
                    ivMic.startAnimation(pulseAnimation);
                }

                if (tvStatus != null) {
                    tvStatus.setText("Listening...");
                }

                // Restart listening
                startListening();
            }
        }
    }

    /**
     * Update the pause/resume button text
     */
    private void updatePauseButtonText(boolean paused) {
        if (voiceDialog != null && voiceDialog.isShowing()) {
            Button btnPauseResume = voiceDialog.findViewById(R.id.btn_pause_resume_listening);
            if (btnPauseResume != null) {
                btnPauseResume.setText(paused ? "Resume" : "Pause");
            }
        }
    }

    /**
     * Check if we have permission for the microphone
     */
    private boolean checkVoicePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_SPEECH_PERMISSION);
            return false;
        }
        return true;
    }

    /**
     * Helper method to hide the keyboard
     */
    private void hideKeyboard(View view) {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Sets the emotion text with appropriate colors based on emotion categories.
     * Format: "I'm feeling" in white, and the emotion name(s) in their respective category colors.
     * For two emotions, it displays them on separate lines for better readability.
     */
    private void updateEmotionSummary() {
        List<Emotion> emotions = currentEntry.getEmotions();

        if (emotions == null || emotions.isEmpty()) {
            // No emotions yet
            tvEmotionSummary.setText("I'm feeling...");
            btnAddAnotherEmotion.setVisibility(View.VISIBLE);
            return;
        }

        if (emotions.size() == 1) {
            // Single emotion - display on one line
            String prefix = "I'm feeling ";
            String emotionText = emotions.get(0).getName();

            SpannableString spannableString = new SpannableString(prefix + emotionText);

            // Set prefix color to white
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE),
                    0, prefix.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set emotion name color based on category
            int emotionColor = getColorForCategory(emotions.get(0).getCategory());
            spannableString.setSpan(new ForegroundColorSpan(emotionColor),
                    prefix.length(),
                    prefix.length() + emotionText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvEmotionSummary.setText(spannableString);
            btnAddAnotherEmotion.setVisibility(View.VISIBLE);
        } else if (emotions.size() == 2) {
            // Two emotions - display on separate lines
            String firstLine = "I'm feeling " + emotions.get(0).getName();
            String ampersand = " &";
            String secondLine = emotions.get(1).getName();
            String fullText = firstLine + ampersand + "\n" + secondLine;

            SpannableString spannableString = new SpannableString(fullText);

            // Set "I'm feeling" to white
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE),
                    0, 11,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set first emotion color
            int firstEmotionColor = getColorForCategory(emotions.get(0).getCategory());
            spannableString.setSpan(new ForegroundColorSpan(firstEmotionColor),
                    11,
                    firstLine.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set ampersand "&" to white
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE),
                    firstLine.length(),
                    firstLine.length() + ampersand.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set second emotion color
            int secondEmotionColor = getColorForCategory(emotions.get(1).getCategory());
            spannableString.setSpan(new ForegroundColorSpan(secondEmotionColor),
                    firstLine.length() + ampersand.length() + 1, // +1 for the newline
                    fullText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvEmotionSummary.setText(spannableString);
            btnAddAnotherEmotion.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the appropriate color code for an emotion category
     */
    private int getColorForCategory(Emotion.Category category) {
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                return 0xFFFFE57F; // Yellow for high energy pleasant
            case HIGH_ENERGY_UNPLEASANT:
                return 0xFFFF6B6B; // Red for high energy unpleasant
            case LOW_ENERGY_PLEASANT:
                return 0xFF7FE57F; // Green for low energy pleasant
            case LOW_ENERGY_UNPLEASANT:
                return 0xFF7FB8FF; // Blue for low energy unpleasant
            default:
                return Color.WHITE;
        }
    }

    // Save the current input fields to the entry object
    private void saveInputFieldsToEntry() {
        if (currentEntry != null) {
            // Save journal text
            currentEntry.setJournalText(etJournalText.getText().toString().trim());

            // Save tags
            currentEntry.setTags(new ArrayList<>(tags));

            // Note: images will be saved during the final save
        }
    }

    private void setupAdapters() {
        // Set up image adapter
        imageAdapter = new EntryImageAdapter(this, imageUris, this);
        recyclerImages.setAdapter(imageAdapter);
    }

    private void setupLimits() {
        // Set maximum characters for journal text
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(MAX_CHAR_COUNT);
        etJournalText.setFilters(filters);
    }

    private void updateUI() {
        // Update emotion summary with colored text
        updateEmotionSummary();

        // Update character count
        tvCharCount.setText("0/" + MAX_CHAR_COUNT + " characters");

        // Add any existing tags (if editing or returning from add emotion)
        if (currentEntry.getTags() != null && !currentEntry.getTags().isEmpty()) {
            tags.clear();
            llTags.removeAllViews();

            for (String tag : currentEntry.getTags()) {
                tags.add(tag);
                addTagView(tag);
            }
        }

        // Add any existing journal text (if editing or returning from add emotion)
        if (!TextUtils.isEmpty(currentEntry.getJournalText())) {
            etJournalText.setText(currentEntry.getJournalText());
        }

        // Add any existing images (if editing)
        // This would require loading the images from Firebase Storage
        // For now, we'll skip this part and assume this is a new entry
    }

    private void openImagePicker() {
        if (imageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        // For Android 10+ (API 29+), use ACTION_OPEN_DOCUMENT instead of ACTION_PICK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        }
    }

    private void addImage(Uri imageUri) {
        imageUris.add(imageUri);

        // Convert Uri to byte array for Firebase Storage
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > -1) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
                imageBytesList.add(baos.toByteArray());
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }

        // Update UI
        updateImageUI();
    }

    private void updateImageUI() {
        int count = imageUris.size();

        if (count > 0) {
            tvImageCount.setVisibility(View.VISIBLE);
            recyclerImages.setVisibility(View.VISIBLE);
            tvImageCount.setText(count + (count == 1 ? " image" : " images") + " attached to entry");
            imageAdapter.notifyDataSetChanged();
        } else {
            tvImageCount.setVisibility(View.GONE);
            recyclerImages.setVisibility(View.GONE);
        }
    }

    private void addTag() {
        String tagText = etTags.getText().toString().trim();

        if (!tagText.isEmpty() && tags.size() < MAX_TAGS) {
            // Don't add duplicate tags
            if (!tags.contains(tagText)) {
                tags.add(tagText);
                addTagView(tagText);
            }

            // Clear the input field
            etTags.setText("");
        } else if (tags.size() >= MAX_TAGS) {
            Toast.makeText(this, "Maximum " + MAX_TAGS + " tags allowed", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTagView(String tagText) {
        // Inflate the tag view
        View tagView = LayoutInflater.from(this).inflate(R.layout.item_tag, null, false);

        // Set the tag text
        TextView tvTagText = tagView.findViewById(R.id.tv_tag_text);
        tvTagText.setText(tagText);

        // Set the remove button click listener
        ImageView ivTagRemove = tagView.findViewById(R.id.iv_tag_remove);
        ivTagRemove.setOnClickListener(v -> {
            // Remove the tag from the list
            tags.remove(tagText);

            // Remove the tag view from both possible parent layouts
            if (tagView.getParent() != null) {
                ((LinearLayout) tagView.getParent()).removeView(tagView);
            }

            // Make sure the tag input is enabled if we're below the limit
            etTags.setEnabled(true);

            // Ensure hint is visible for better clarity
            if (tags.isEmpty()) {
                etTags.setHint("Type tag + space or enter");
            }
        });

        // Add layout parameters
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 8, 0);
        tagView.setLayoutParams(layoutParams);

        // Add the tag to the horizontal LinearLayout in the scroll view
        llTags.addView(tagView);

        // Disable the input if we've reached the limit
        if (tags.size() >= MAX_TAGS) {
            etTags.setEnabled(false);
            etTags.setHint("Maximum tags reached");
        } else {
            // Change hint to be clearer once user has added at least one tag
            etTags.setHint("+ Add more tags");
        }

        // Scroll to the end of the tags
        tagsScrollView.post(() -> tagsScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private void saveEntry() {
        // Check if we have at least one emotion
        if (currentEntry.getEmotions() == null || currentEntry.getEmotions().isEmpty()) {
            Toast.makeText(this, "Please select at least one emotion", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save journal text
        currentEntry.setJournalText(etJournalText.getText().toString().trim());

        // Save tags
        currentEntry.setTags(new ArrayList<>(tags));

        // Show loading indicator or disable save button
        btnSaveEntry.setEnabled(false);

        // Upload images to Firebase Storage if there are any
        if (!imageBytesList.isEmpty()) {
            uploadImages();
        } else {
            // No images to upload, directly save the entry
            saveEntryToFirebase();
        }
    }

    private void uploadImages() {
        List<String> imageUrls = new ArrayList<>();
        final int[] uploadCount = {0};
        final int totalImages = imageBytesList.size();

        // Show progress message
        Toast.makeText(this, "Uploading images...", Toast.LENGTH_SHORT).show();

        for (int i = 0; i < imageBytesList.size(); i++) {
            byte[] imageData = imageBytesList.get(i);
            final int imageIndex = i;

            if (firebaseHelper.getCurrentUser() == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                btnSaveEntry.setEnabled(true);
                return;
            }

            String userId = firebaseHelper.getCurrentUser().getUid();
            String filename = "image_" + System.currentTimeMillis() + "_" + imageIndex + ".jpg";

            // Create a direct reference to Firebase Storage
            StorageReference imageRef = FirebaseStorage.getInstance().getReference()
                    .child("images")
                    .child(userId)
                    .child(filename);

            // Upload directly and get the URL in one continuous operation
            imageRef.putBytes(imageData)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        // Get the download URL from the same reference
                        return imageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            if (downloadUri != null) {
                                imageUrls.add(downloadUri.toString());
                            }
                        }

                        uploadCount[0]++;
                        if (uploadCount[0] >= totalImages) {
                            // All uploads complete
                            currentEntry.setImageUrls(imageUrls);
                            saveEntryToFirebase();
                        }
                    });
        }
    }

    private void saveEntryToFirebase() {
        // Make sure we have a valid user ID
        if (firebaseHelper.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            btnSaveEntry.setEnabled(true);
            return;
        }

        // Set user ID if not already set
        if (TextUtils.isEmpty(currentEntry.getUserId())) {
            currentEntry.setUserId(firebaseHelper.getCurrentUser().getUid());
        }

        // Set timestamp if not already set
        if (currentEntry.getTimestamp() == null) {
            currentEntry.setTimestamp(new Date());
        }

        // Save the entry to Firebase
        firebaseHelper.saveEmotionEntry(currentEntry)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(JournalSummaryActivity.this, "Entry saved successfully", Toast.LENGTH_SHORT).show();

                    // Navigate back to the home screen
                    Intent intent = new Intent(JournalSummaryActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(JournalSummaryActivity.this,
                            "Failed to save entry: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnSaveEntry.setEnabled(true);
                });
    }

    @Override
    public void onImageRemove(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            imageBytesList.remove(position);
            imageAdapter.notifyItemRemoved(position);
            updateImageUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SPEECH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "Voice recognition requires microphone permission", Toast.LENGTH_SHORT).show();
            }
        }
    }
}