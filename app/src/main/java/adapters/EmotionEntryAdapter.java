package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import edu.northeastern.numad25sp_group4.R;
import models.Emotion;
import models.EmotionEntry;

public class EmotionEntryAdapter extends RecyclerView.Adapter<EmotionEntryAdapter.MyViewHolder> {
    Context context;
    ArrayList<EmotionEntry> emotionEntries;
    private OnEntryClickListener listener;

    public EmotionEntryAdapter(Context context, ArrayList<EmotionEntry> emotionEntries, OnEntryClickListener listener) {
        this.context = context;
        this.emotionEntries = emotionEntries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmotionEntryAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_entry, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmotionEntryAdapter.MyViewHolder holder, int position) {
        EmotionEntry entry = emotionEntries.get(position);

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });

        // Format time
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(entry.getTimestamp());

        holder.tvTime.setText(formattedTime);

        if (!entry.getEmotions().isEmpty()) {
            holder.tvEmotion1.setText(entry.getEmotions().get(0).getName());
            holder.tvEmotion1.setTextColor(getColorForCategory(entry.getEmotions().get(0).getCategory()));
            holder.emotion1Row.setVisibility(View.VISIBLE);
            holder.emotion1Dot.setBackgroundResource(
                    getDotDrawableForCategory(entry.getEmotions().get(0).getCategory())
            );
        } else {
            holder.emotion1Row.setVisibility(View.GONE);
        }

        if (entry.getEmotions().size() > 1) {
            holder.tvEmotion2.setText(entry.getEmotions().get(1).getName());
            holder.tvEmotion2.setTextColor(getColorForCategory(entry.getEmotions().get(1).getCategory()));
            holder.emotion2Row.setVisibility(View.VISIBLE);
            holder.emotion2Dot.setBackgroundResource(
                    getDotDrawableForCategory(entry.getEmotions().get(1).getCategory())
            );
        } else {
            holder.emotion2Row.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return emotionEntries.size();
    }

    private int getDotDrawableForCategory(Emotion.Category category) {
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                return R.drawable.dot_hep;
            case HIGH_ENERGY_UNPLEASANT:
                return R.drawable.dot_heu;
            case LOW_ENERGY_PLEASANT:
                return R.drawable.dot_lep;
            case LOW_ENERGY_UNPLEASANT:
                return R.drawable.dot_leu;
            default:
                return R.drawable.dot_hep;
        }
    }

    private int getColorForCategory(Emotion.Category category) {
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                return R.color.high_energy_pleasant;
            case HIGH_ENERGY_UNPLEASANT:
                return R.color.high_energy_unpleasant;
            case LOW_ENERGY_PLEASANT:
                return R.color.low_energy_pleasant;
            case LOW_ENERGY_UNPLEASANT:
                return R.color.low_energy_unpleasant;
            default:
                return R.color.high_energy_pleasant;
        }
    }

    public interface OnEntryClickListener {
        void onEntryClick(EmotionEntry entry);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvEmotion1, tvEmotion2;
        LinearLayout emotion1Row, emotion2Row;
        View emotion1Dot, emotion2Dot;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time_logged);
            tvEmotion1 = itemView.findViewById(R.id.tv_emotion1);
            tvEmotion2 = itemView.findViewById(R.id.tv_emotion2);
            emotion1Row = itemView.findViewById(R.id.emotion1_row);
            emotion2Row = itemView.findViewById(R.id.emotion2_row);
            emotion1Dot = emotion1Row.findViewById(R.id.emotion1_dot);
            emotion2Dot = emotion2Row.findViewById(R.id.emotion2_dot);
        }
    }
}