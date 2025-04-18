package edu.northeastern.numad25sp_group4;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import adapters.EntryImageAdapter;
import models.Emotion;
import models.EmotionEntry;
import utils.FirebaseHelper;

public class EntryEditActivity extends AppCompatActivity implements EntryImageAdapter.OnImageRemoveListener {

    private static final String TAG = "EntryEditActivity";
    private static final int MAX_CHAR_COUNT = 500;
    private static final int MAX_IMAGES = 3;
    private static final int MAX_TAGS = 6;
    private static final int REQUEST_IMAGE_PERMISSION = 101;

    // UI Components
    private TextView tvEntryTitle;
    private Button btnEditEntry;
    private Button btnDeleteEntry;
    private Button btnDoneEditing;
    private TextView tvEmotionDisplay;
    private EditText etJournalText;
    private TextView tvCharCount;
    private ImageButton btnCamera;
    private TextView tvImageCount;
    private RecyclerView recyclerImages;
    private TextView tvNoImages;
    private EditText etTags;
    private LinearLayout llTags;
    private LinearLayout llTagsDisplay;
    private TextView tvNoTags;
    private HorizontalScrollView tagsScrollView;
    private ImageView ivBackArrow;
    private View tagsInputContainer;
    private View llMediaControls;

    // Data
    private String entryId;
    private EmotionEntry currentEntry;
    private boolean isEditMode = false;
    private List<String> tags = new ArrayList<>();
    private List<Uri> imageUris = new ArrayList<>();
    private List<byte[]> imageBytesList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;
    private EntryImageAdapter imageAdapter;

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
        setContentView(R.layout.activity_entry_edit);

        // Initialize Firebase Helper
        firebaseHelper = FirebaseHelper.getInstance();

        // Get the entry ID from intent
        if (getIntent().hasExtra("entryId")) {
            entryId = getIntent().getStringExtra("entryId");
        } else {
            // No entry ID provided, show error and finish
            Toast.makeText(this, "Error: No entry ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Set up adapters
        setupAdapters();

        // Set up listeners
        setupListeners();

        // Load entry data
        loadEntryData();
    }

    private void initViews() {
        tvEntryTitle = findViewById(R.id.tv_entry_title);
        btnEditEntry = findViewById(R.id.btn_edit_entry);
        btnDeleteEntry = findViewById(R.id.btn_delete_entry);
        btnDoneEditing = findViewById(R.id.btn_done_editing);
        tvEmotionDisplay = findViewById(R.id.tv_emotion_display);
        etJournalText = findViewById(R.id.et_journal_text);
        tvCharCount = findViewById(R.id.tv_char_count);
        btnCamera = findViewById(R.id.btn_camera);
        tvImageCount = findViewById(R.id.tv_image_count);
        recyclerImages = findViewById(R.id.recycler_images);
        tvNoImages = findViewById(R.id.tv_no_images);
        etTags = findViewById(R.id.et_tags);
        llTags = findViewById(R.id.ll_tags);
        llTagsDisplay = findViewById(R.id.ll_tags_display);
        tvNoTags = findViewById(R.id.tv_no_tags);
        tagsScrollView = findViewById(R.id.tags_scroll_view);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        tagsInputContainer = findViewById(R.id.tags_input_container);
        llMediaControls = findViewById(R.id.ll_media_controls);

        // Set up RecyclerView for images
        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Set character limit for journal text
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(MAX_CHAR_COUNT);
        etJournalText.setFilters(filters);
    }

    private void setupAdapters() {
        // Set up image adapter
        imageAdapter = new EntryImageAdapter(this, imageUris, this);
        recyclerImages.setAdapter(imageAdapter);
    }

    private void setupListeners() {
        // Back arrow click listener
        ivBackArrow.setOnClickListener(v -> {
            // Check if we're in edit mode with unsaved changes
            if (isEditMode) {
                showUnsavedChangesDialog();
            } else {
                finish();
            }
        });

        // Edit button click listener
        btnEditEntry.setOnClickListener(v -> {
            enableEditMode(true);
        });

        // Done editing button click listener
        btnDoneEditing.setOnClickListener(v -> {
            saveChanges();
        });

        // Delete button click listener
        btnDeleteEntry.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        // Journal text change listener
        etJournalText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditMode) {
                    int currentLength = s.length();
                    tvCharCount.setText(currentLength + "/" + MAX_CHAR_COUNT + " characters");
                }
            }
        });

        // Camera button click listener
        btnCamera.setOnClickListener(v -> {
            openImagePicker();
        });

        // Tags input listener
        etTags.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (!etTags.getText().toString().trim().isEmpty()) {
                    addTag();
                }
                hideKeyboard(etTags);
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
    }

    private void loadEntryData() {
        if (entryId == null || entryId.isEmpty()) {
            Toast.makeText(this, "Invalid entry ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading indicator (could add a progress bar here)
        setViewsEnabled(false);

        // Query the database for this entry
        firebaseHelper.getAllEntries(Objects.requireNonNull(firebaseHelper.getCurrentUser()).getUid(),
                new FirebaseHelper.FilteredEntriesListener() {
                    @Override
                    public void onSuccess(List<EmotionEntry> entries) {
                        // Find the entry with matching ID
                        for (EmotionEntry entry : entries) {
                            if (entry.getEntryId() != null && entry.getEntryId().equals(entryId)) {
                                currentEntry = entry;
                                break;
                            }
                        }

                        if (currentEntry != null) {
                            // Entry found, update UI
                            updateUIWithEntryData();
                        } else {
                            // Entry not found
                            Toast.makeText(EntryEditActivity.this, "Entry not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        // Enable views
                        setViewsEnabled(true);
                    }

                    @Override
                    public void onFailure(DatabaseError error) {
                        Log.e(TAG, "Error loading entry: " + error.getMessage());
                        Toast.makeText(EntryEditActivity.this, "Error loading entry", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateUIWithEntryData() {
        if (currentEntry == null) return;

        // Set entry date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault());
        String formattedDate = dateFormat.format(currentEntry.getTimestamp());
        tvEntryTitle.setText("Entry on " + formattedDate);

        // Set emotions
        updateEmotionDisplay();

        // Set journal text
        if (!TextUtils.isEmpty(currentEntry.getJournalText())) {
            etJournalText.setText(currentEntry.getJournalText());
        } else {
            etJournalText.setHint("No journal text for this entry");
        }

        // Set tags
        tags.clear();
        llTagsDisplay.removeAllViews();
        llTags.removeAllViews(); // Clear edit mode tags too

        if (currentEntry.getTags() != null && !currentEntry.getTags().isEmpty()) {
            // Make a deep copy of the tags to avoid reference issues
            List<String> entryTags = new ArrayList<>(currentEntry.getTags());
            tags.addAll(entryTags);

            // Add tags to display view (visible in read mode)
            for (String tag : tags) {
                addTagToDisplay(tag);
            }
            tvNoTags.setVisibility(View.GONE);
        } else {
            tvNoTags.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Loaded tags: " + tags.size() + " - " + tags.toString());

        // Set images
        imageUris.clear();  // Clear existing images

        if (currentEntry.getImageUrls() != null && !currentEntry.getImageUrls().isEmpty()) {
            tvNoImages.setVisibility(View.GONE);
            recyclerImages.setVisibility(View.VISIBLE);

            // Load images from URLs
            for (String imageUrl : currentEntry.getImageUrls()) {
                try {
                    // Convert Firebase Storage URL to Uri
                    Uri imageUri = Uri.parse(imageUrl);
                    imageUris.add(imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing image URL: " + e.getMessage());
                }
            }

            // Update adapter with loaded images
            if (imageAdapter != null) {
                imageAdapter.notifyDataSetChanged();
            }

            // Update image count text
            tvImageCount.setText(currentEntry.getImageUrls().size() +
                    (currentEntry.getImageUrls().size() == 1 ? " image" : " images") + " attached");
        } else {
            tvNoImages.setVisibility(View.VISIBLE);
            recyclerImages.setVisibility(View.GONE);
        }

        // Return to read-only mode
        enableEditMode(false);
    }

    private void updateEmotionDisplay() {
        if (currentEntry == null || currentEntry.getEmotions() == null || currentEntry.getEmotions().isEmpty()) {
            tvEmotionDisplay.setText("No emotions recorded");
            return;
        }

        StringBuilder emotionsText = new StringBuilder("Emotions: ");
        List<Emotion> emotions = currentEntry.getEmotions();

        for (int i = 0; i < emotions.size(); i++) {
            emotionsText.append(emotions.get(i).getName());
            if (i < emotions.size() - 1) {
                emotionsText.append(", ");
            }
        }

        tvEmotionDisplay.setText(emotionsText.toString());
    }

    private void enableEditMode(boolean enable) {
        isEditMode = enable;

        // Toggle edit mode UI elements
        etJournalText.setEnabled(enable);
        btnEditEntry.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnDoneEditing.setVisibility(enable ? View.VISIBLE : View.GONE);

        // Toggle character count visibility
        tvCharCount.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (enable) {
            int currentLength = etJournalText.getText().length();
            tvCharCount.setText(currentLength + "/" + MAX_CHAR_COUNT + " characters");
        }

        // Toggle media controls
        llMediaControls.setVisibility(enable ? View.VISIBLE : View.GONE);

        // Toggle tags edit mode
        tagsInputContainer.setVisibility(enable ? View.VISIBLE : View.GONE);

        if (enable) {
            // In edit mode: Clear display and add tags to edit container
            llTags.removeAllViews();
            for (String tag : tags) {
                addTagView(tag);
            }
            llTagsDisplay.setVisibility(View.GONE);
        } else {
            // In view mode: Clear edit container and show tags in display
            llTagsDisplay.removeAllViews();
            llTagsDisplay.setVisibility(View.VISIBLE);
            for (String tag : tags) {
                addTagToDisplay(tag);
            }

            // Make sure images are synced with entry data when returning to view mode
            syncImagesWithEntry();
        }

        // Make sure images are visible if they exist
        boolean hasImages = imageUris != null && !imageUris.isEmpty();
        recyclerImages.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        tvNoImages.setVisibility(hasImages ? View.GONE : View.VISIBLE);
        tvImageCount.setVisibility(hasImages ? View.VISIBLE : View.GONE);

        if (hasImages) {
            tvImageCount.setText(imageUris.size() +
                    (imageUris.size() == 1 ? " image" : " images") + " attached");
        }

        // Update visibility of no tags/images messages
        updateNoTagsVisibility();
        updateNoImagesVisibility();
    }


    private void updateNoTagsVisibility() {
        tvNoTags.setVisibility(
                (!isEditMode && (tags == null || tags.isEmpty())) ? View.VISIBLE : View.GONE);
    }

    private void updateNoImagesVisibility() {
        tvNoImages.setVisibility(
                (!isEditMode && (currentEntry == null || currentEntry.getImageUrls() == null || currentEntry.getImageUrls().isEmpty()))
                        ? View.VISIBLE : View.GONE);
    }

    private void openImagePicker() {
        if (!isEditMode) return;

        if (imageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
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
            tvNoImages.setVisibility(View.GONE);
            tvImageCount.setText(count + (count == 1 ? " image" : " images") + " attached");
            imageAdapter.notifyDataSetChanged();
        } else {
            tvImageCount.setVisibility(View.GONE);
            if (!isEditMode) {
                recyclerImages.setVisibility(View.GONE);
                tvNoImages.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addTag() {
        String tagText = etTags.getText().toString().trim();

        if (!tagText.isEmpty() && tags.size() < MAX_TAGS) {
            // Don't add duplicate tags
            if (!tags.contains(tagText)) {
                tags.add(tagText);
                addTagView(tagText);

                // Log tag added - debug purposes
                Log.d(TAG, "Tag added: " + tagText + ", total tags now: " + tags.size());
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

            // Remove the tag view from the parent layout
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

    private void addTagToDisplay(String tagText) {
        // Inflate the tag view for read-only display
        View tagView = LayoutInflater.from(this).inflate(R.layout.item_tag, null, false);

        // Set the tag text
        TextView tvTagText = tagView.findViewById(R.id.tv_tag_text);
        tvTagText.setText(tagText);

        // Hide the remove button in display mode
        ImageView ivTagRemove = tagView.findViewById(R.id.iv_tag_remove);
        ivTagRemove.setVisibility(View.GONE);

        // Add layout parameters
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 8, 8); // Add bottom margin for wrapping
        tagView.setLayoutParams(layoutParams);

        // Add the tag to the display layout
        llTagsDisplay.addView(tagView);
    }

    private void saveChanges() {
        if (currentEntry == null) {
            Toast.makeText(this, "Error: No entry to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable UI while saving
        setViewsEnabled(false);
        btnDoneEditing.setEnabled(false);

        // Update entry with edited data
        currentEntry.setJournalText(etJournalText.getText().toString().trim());

        // Save tags with logging
        Log.d(TAG, "Saving tags: " + tags.size() + " - " + tags.toString());
        currentEntry.setTags(new ArrayList<>(tags));

        // Check if we need to handle image uploads
        if (!imageBytesList.isEmpty()) {
            uploadImagesAndSaveEntry();
        } else {
            // No new images, just save the entry
            saveEntryToFirebase();
        }
    }

    private void uploadImagesAndSaveEntry() {
        Toast.makeText(this, "Uploading images...", Toast.LENGTH_SHORT).show();

        // Track upload progress
        final int[] uploadedCount = {0};
        final List<String> imageUrls = new ArrayList<>();

        // Check if user is logged in
        if (firebaseHelper.getCurrentUser() == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            setViewsEnabled(true);
            btnDoneEditing.setEnabled(true);
            return;
        }

        String userId = firebaseHelper.getCurrentUser().getUid();

        // Upload each image
        for (int i = 0; i < imageBytesList.size(); i++) {
            final int imageIndex = i;
            byte[] imageData = imageBytesList.get(i);

            // Upload to Firebase Storage
            firebaseHelper.uploadImage(userId, imageData)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get download URL for this image
                        taskSnapshot.getStorage().getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    // Add URL to our list
                                    imageUrls.add(uri.toString());
                                    uploadedCount[0]++;

                                    // If all uploads complete, save entry
                                    if (uploadedCount[0] >= imageBytesList.size()) {
                                        // Update entry with new image URLs
                                        // Keep existing URLs if any
                                        List<String> existingUrls = currentEntry.getImageUrls();
                                        if (existingUrls == null) {
                                            existingUrls = new ArrayList<>();
                                        }
                                        existingUrls.addAll(imageUrls);

                                        // Ensure we don't exceed the max images limit
                                        while (existingUrls.size() > MAX_IMAGES) {
                                            existingUrls.remove(existingUrls.size() - 1);
                                        }

                                        currentEntry.setImageUrls(existingUrls);

                                        // Now save the entry with updated URLs
                                        saveEntryToFirebase();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                                    uploadedCount[0]++;

                                    // Continue with remaining uploads even if this one failed
                                    if (uploadedCount[0] >= imageBytesList.size()) {
                                        saveEntryToFirebase();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image: " + e.getMessage());
                        uploadedCount[0]++;

                        // Continue with remaining uploads even if this one failed
                        if (uploadedCount[0] >= imageBytesList.size()) {
                            saveEntryToFirebase();
                        }
                    });
        }
    }

    private void saveEntryToFirebase() {
        // Save the updated entry to Firebase
        firebaseHelper.saveEmotionEntry(currentEntry)
                .addOnSuccessListener(aVoid -> {
                    // Update UI with current entry data without waiting for Firebase refresh
                    Log.d(TAG, "Entry saved successfully. Tags count: " +
                            (currentEntry.getTags() != null ? currentEntry.getTags().size() : 0));

                    Toast.makeText(EntryEditActivity.this, "Entry updated successfully", Toast.LENGTH_SHORT).show();

                    // DON'T clear the imageUris here - instead, sync with current entry urls
                    syncImagesWithEntry();

                    // Return to view mode and re-enable views
                    enableEditMode(false);
                    setViewsEnabled(true);
                    btnDoneEditing.setEnabled(true);

                    // Clear only the bytes list for new uploads
                    imageBytesList.clear();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating entry: " + e.getMessage());
                    Toast.makeText(EntryEditActivity.this, "Error updating entry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setViewsEnabled(true);
                    btnDoneEditing.setEnabled(true);
                });
    }

    private void syncImagesWithEntry() {
        imageUris.clear();

        if (currentEntry != null && currentEntry.getImageUrls() != null) {
            for (String url : currentEntry.getImageUrls()) {
                imageUris.add(Uri.parse(url));
            }
        }

        // Notify adapter of changes
        if (imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
        }
    }


    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteEntry();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEntry() {
        if (currentEntry == null) {
            Toast.makeText(this, "Error: No entry to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete the entry from Firebase
        firebaseHelper.deleteEntry(currentEntry, new FirebaseHelper.EntryDeleteCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EntryEditActivity.this, "Entry deleted successfully", Toast.LENGTH_SHORT).show();
                // Return to the entries list activity
                finish();
            }

            @Override
            public void onFailure(DatabaseError error) {
                Log.e(TAG, "Error deleting entry: " + error.getMessage());
                Toast.makeText(EntryEditActivity.this, "Error deleting entry: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save them before leaving?")
                .setPositiveButton("Save", (dialog, which) -> {
                    saveChanges();
                    finish();
                })
                .setNegativeButton("Discard", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void setViewsEnabled(boolean enabled) {
        btnEditEntry.setEnabled(enabled);
        btnDeleteEntry.setEnabled(enabled);
        ivBackArrow.setEnabled(enabled);
    }

    @Override
    public void onImageRemove(int position) {
        if (position >= 0 && position < imageUris.size()) {
            // Remove from UI list
            imageUris.remove(position);

            // Remove from bytes list if it's a new image
            if (position < imageBytesList.size()) {
                imageBytesList.remove(position);
            }
            // If it's an existing image, mark it for removal from Firebase
            else if (currentEntry != null && currentEntry.getImageUrls() != null && position < currentEntry.getImageUrls().size()) {
                // Remove from the entry's image URLs too
                List<String> updatedUrls = new ArrayList<>(currentEntry.getImageUrls());
                if (position < updatedUrls.size()) {
                    updatedUrls.remove(position);
                    currentEntry.setImageUrls(updatedUrls);
                }
            }

            // Update UI
            imageAdapter.notifyDataSetChanged();
            updateImageUI();
        }
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void hideKeyboard(View view) {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
        }
    }
}