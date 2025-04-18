package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseError;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import adapters.EmotionEntryAdapter;
import models.EmotionEntry;
import utils.FirebaseHelper;

public class EntryListActivity extends AppCompatActivity {

    private static final String TAG = "EntryListActivity";

    private TextView tvEntryListDate;
    private EmotionEntryAdapter adapter;
    private RecyclerView rvEntries;
    private ImageView ivBackArrow;
    private LocalDate selectedDate;
    private FirebaseHelper firebaseHelper;
    private String userId;

    private ArrayList<EmotionEntry> emotionEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_list);

        // Initialize helper
        firebaseHelper = FirebaseHelper.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
        }

        // Initialize views
        initViews();

        loadSelectedDate();
        setupBackButton();
        setupRecyclerView();
        loadEntriesForDate();
    }

    private void initViews() {
        tvEntryListDate = findViewById(R.id.tv_entry_list_date);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        rvEntries = findViewById(R.id.rv_entries);
    }

    private void setupBackButton() {
        ivBackArrow.setOnClickListener(view -> finish());
    }

    private void setupRecyclerView() {
        // Initialize adapter with click listener
        adapter = new EmotionEntryAdapter(this, emotionEntries, entry -> {
            // Navigate to EntryEditActivity when an entry is clicked
            if (entry != null && entry.getEntryId() != null) {
                Intent intent = new Intent(EntryListActivity.this, EntryEditActivity.class);
                intent.putExtra("entryId", entry.getEntryId());
                startActivity(intent);
            }
        });

        // Set up RecyclerView
        rvEntries.setAdapter(adapter);
        rvEntries.setLayoutManager(new LinearLayoutManager(this));

        // Enable swipe to delete
        enableSwipeToDelete();
    }

    private void loadSelectedDate() {
        Intent intent = getIntent();
        long selectedDateMillis = intent.getLongExtra("selectedDate", -1);

        if (selectedDateMillis != -1) {
            // Convert millis to LocalDate
            selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Update the date title UI
            if (tvEntryListDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                tvEntryListDate.setText(formatter.format(selectedDate));
            }
        } else {
            Toast.makeText(this, "No date selected. Returning to previous screen.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadEntriesForDate() {
        if (userId == null || selectedDate == null) {
            updateEmptyState(true);
            return;
        }

        firebaseHelper.getEntriesForDate(userId, selectedDate, new FirebaseHelper.FilteredEntriesListener() {
            @Override
            public void onSuccess(List<EmotionEntry> filteredEntries) {
                emotionEntries.clear();
                emotionEntries.addAll(filteredEntries);

                // Sort entries by timestamp (earliest to latest)
                emotionEntries.sort(Comparator.comparing(EmotionEntry::getTimestamp));

                adapter.notifyDataSetChanged();

                // Show fallback if no entries
                updateEmptyState(emotionEntries.isEmpty());
            }

            @Override
            public void onFailure(DatabaseError error) {
                Log.e(TAG, "Failed to fetch entries", error.toException());
                Toast.makeText(EntryListActivity.this, "Failed to load entries.", Toast.LENGTH_SHORT).show();
                updateEmptyState(true);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        TextView tvNoEntries = findViewById(R.id.tv_no_entries);
        if (tvNoEntries != null) {
            tvNoEntries.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (rvEntries != null) {
            rvEntries.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // No move support
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                EmotionEntry entryToDelete = emotionEntries.get(position);

                // Show confirmation dialog
                new AlertDialog.Builder(EntryListActivity.this)
                        .setTitle("Delete Entry")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // Remove from DB and list
                            firebaseHelper.deleteEntry(entryToDelete, new FirebaseHelper.EntryDeleteCallback() {
                                @Override
                                public void onSuccess() {
                                    emotionEntries.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    updateEmptyState(emotionEntries.isEmpty());
                                }

                                @Override
                                public void onFailure(DatabaseError error) {
                                    Toast.makeText(EntryListActivity.this,
                                            "Failed to delete entry", Toast.LENGTH_SHORT).show();
                                    adapter.notifyItemChanged(position); // restore item
                                }
                            });
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // Restore if canceled
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }
        };

        new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(rvEntries);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity (e.g., after editing or deleting an entry)
        loadEntriesForDate();
    }
}