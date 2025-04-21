package adapters;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.numad25sp_group4.R;
import models.Emotion;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;

public class EmotionAdapter extends RecyclerView.Adapter<EmotionAdapter.EmotionViewHolder> {

    private static final String TAG = "EmotionAdapter";
    private final Context context;
    private final List<Emotion> emotionList;
    private int selectedPosition = -1;
    private final OnEmotionSelectedListener listener;
    private final Emotion.Category category;

    // Interface for click listener
    public interface OnEmotionSelectedListener {
        void onEmotionSelected(Emotion emotion);
    }

    public EmotionAdapter(Context context, Emotion.Category category, OnEmotionSelectedListener listener) {
        this.context = context;
        this.emotionList = new ArrayList<>();
        this.listener = listener;
        this.category = category;
        Log.d(TAG, "EmotionAdapter created for category: " + category.name());
    }

    @NonNull
    @Override
    public EmotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called");
        View view = LayoutInflater.from(context).inflate(R.layout.item_emotion, parent, false);
        return new EmotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmotionViewHolder holder, int position) {
        Emotion emotion = emotionList.get(position);
        Log.d(TAG, "Binding emotion at position " + position + ": " + emotion.getName());

        holder.tvEmotionName.setText(emotion.getName());

        // Apply color based on energy level - higher energy emotions have brighter/more vibrant colors
        // 0.5 to 1.0 alpha range
        float alphaFactor = emotion.getEnergyLevel() * 0.05f + 0.5f;
        holder.cardEmotion.setAlpha(alphaFactor);

        // Set card color based on emotion category
        int colorRes;
        int textColor;
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                colorRes = R.color.high_energy_pleasant;
                textColor = 0xFF5F5000;
                break;
            case HIGH_ENERGY_UNPLEASANT:
                colorRes = R.color.high_energy_unpleasant;
                textColor = 0xFF8E2020;
                break;
            case LOW_ENERGY_PLEASANT:
                colorRes = R.color.low_energy_pleasant;
                textColor = 0xFF0F5B0F;
                break;
            case LOW_ENERGY_UNPLEASANT:
                colorRes = R.color.low_energy_unpleasant;
                textColor = 0xFF004975;
                break;
            default:
                colorRes = R.color.card_background;
                textColor = 0xFFFFFFFF;
        }
        holder.cardEmotion.setCardBackgroundColor(context.getResources().getColor(colorRes));
        holder.tvEmotionName.setTextColor(textColor);

        // Selection status
        if (selectedPosition == position) {
            Log.d(TAG, "Setting selected state for position " + position);
            holder.cardEmotion.setStrokeWidth(4);
            holder.cardEmotion.setStrokeColor(context.getResources().getColor(R.color.white));
        } else {
            holder.cardEmotion.setStrokeWidth(0);
        }

        // Click listener
        holder.cardEmotion.setOnClickListener(v -> {
            Log.d(TAG, "Card clicked at position " + holder.getAdapterPosition());
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Update selection visuals
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            // Notify listener
            listener.onEmotionSelected(emotion);
        });

        // Long click listener for tooltip
        holder.cardEmotion.setOnLongClickListener(v -> {
            Log.d(TAG, "Long press detected on emotion: " + emotion.getName());
            showDefinitionTooltip(v, emotion);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        int count = emotionList.size();
        Log.d(TAG, "getItemCount returning: " + count);
        return count;
    }

    public void setEmotions(List<Emotion> emotions) {
        Log.d(TAG, "setEmotions called with " + emotions.size() + " emotions");
        this.emotionList.clear();
        this.emotionList.addAll(emotions);
        // Reset selection when updating emotions
        selectedPosition = -1;

        // Log each emotion being added
        for (int i = 0; i < emotions.size(); i++) {
            Log.d(TAG, "Emotion " + i + ": " + emotions.get(i).getName());
        }

        notifyDataSetChanged();
        Log.d(TAG, "Adapter data updated, new count: " + emotionList.size());
    }

    private void showDefinitionTooltip(View anchorView, Emotion emotion) {
        // Create popup layout
        View tooltipView = LayoutInflater.from(context).inflate(R.layout.layout_emotion_tooltip, null);

        // Set emotion name and definition
        TextView tvEmotionName = tooltipView.findViewById(R.id.tv_tooltip_emotion);
        TextView tvDefinition = tooltipView.findViewById(R.id.tv_tooltip_definition);

        tvEmotionName.setText(emotion.getName());
        tvDefinition.setText(emotion.getDefinition());

        // Apply text color (keep your existing color logic here)

        // Create and configure popup
        final PopupWindow popupWindow = new PopupWindow(
                tooltipView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set a transparent background to prevent default padding
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Enable overlap with anchor (important!)
        popupWindow.setOverlapAnchor(true);

        // Measure tooltip to get actual dimensions
        tooltipView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int tooltipHeight = tooltipView.getMeasuredHeight();

        // Calculate position to appear directly above anchor with no gap
        int yOffset = -(tooltipHeight);

        // Show popup
        popupWindow.showAsDropDown(anchorView, 0, yOffset, Gravity.CENTER);

        // Dismiss logic (keep your existing code)
        tooltipView.setOnClickListener(v -> popupWindow.dismiss());
        tooltipView.postDelayed(popupWindow::dismiss, 3000);
    }

    static class EmotionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardEmotion;
        TextView tvEmotionName;

        EmotionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEmotion = itemView.findViewById(R.id.card_emotion);
            tvEmotionName = itemView.findViewById(R.id.tv_emotion_name);
        }
    }
}