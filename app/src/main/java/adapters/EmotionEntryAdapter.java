package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import edu.northeastern.numad25sp_group4.R;
import models.Emotion;
import models.EmotionEntry;

public class EmotionEntryAdapter extends RecyclerView.Adapter<EmotionEntryAdapter.EntryViewHolder> {

    private Context context;
    private List<EmotionEntry> entries;
    private OnEntryClickListener listener;

    public interface OnEntryClickListener {
        void onEntryClick(EmotionEntry entry);
    }

    public EmotionEntryAdapter(Context context, List<EmotionEntry> entries, OnEntryClickListener listener) {
        this.context = context;
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_emotion_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        EmotionEntry entry = entries.get(position);

        // Format the time
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeStr = timeFormat.format(entry.getTimestamp());

        // Build the emotion text
        StringBuilder emotionText = new StringBuilder();
        if (entry.getEmotions() != null && !entry.getEmotions().isEmpty()) {
            for (int i = 0; i < entry.getEmotions().size(); i++) {
                Emotion emotion = entry.getEmotions().get(i);
                emotionText.append(emotion.getName());

                if (i < entry.getEmotions().size() - 1) {
                    emotionText.append(", ");
                }
            }
        } else {
            emotionText.append("No emotion");
        }

        // Set the text
        holder.tvEntryInfo.setText(emotionText + " - Emotion logged at " + timeStr);

        // Set click listener
        holder.cardEntry.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });

        // Set colors based on emotion category
        if (entry.getEmotions() != null && !entry.getEmotions().isEmpty()) {
            Emotion firstEmotion = entry.getEmotions().get(0);
            int cardColor = getColorForCategory(firstEmotion.getCategory());

            // Set card background color
            holder.cardEntry.setCardBackgroundColor(cardColor);

            // Only set stroke if using MaterialCardView
            if (holder.cardEntry instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView materialCard =
                        (com.google.android.material.card.MaterialCardView) holder.cardEntry;
                int strokeColor = getDarkerColor(cardColor);
                materialCard.setStrokeColor(strokeColor);
                materialCard.setStrokeWidth(2);
            }
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private int getColorForCategory(Emotion.Category category) {
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                return context.getResources().getColor(R.color.high_energy_pleasant);
            case HIGH_ENERGY_UNPLEASANT:
                return context.getResources().getColor(R.color.high_energy_unpleasant);
            case LOW_ENERGY_PLEASANT:
                return context.getResources().getColor(R.color.low_energy_pleasant);
            case LOW_ENERGY_UNPLEASANT:
                return context.getResources().getColor(R.color.low_energy_unpleasant);
            default:
                return context.getResources().getColor(R.color.card_background);
        }
    }

    private int getDarkerColor(int color) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // Value component
        return android.graphics.Color.HSVToColor(hsv);
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardEntry;
        TextView tvEntryInfo;

        EntryViewHolder(View itemView) {
            super(itemView);
            cardEntry = itemView.findViewById(R.id.card_entry);
            tvEntryInfo = itemView.findViewById(R.id.tv_entry_info);
        }
    }
}