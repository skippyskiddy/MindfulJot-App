package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import edu.northeastern.numad25sp_group4.R;

/**
 * Adapter for the tutorial carousel
 */
public class TutorialPagerAdapter extends RecyclerView.Adapter<TutorialPagerAdapter.TutorialViewHolder> {

    private List<TutorialSlide> tutorialSlides;
    private Context context;

    /**
     * Class representing a single tutorial slide
     */
    public static class TutorialSlide {
        private final int imageResourceId;
        private final String stepNumber;
        private final String description;

        public TutorialSlide(int imageResourceId, String stepNumber, String description) {
            this.imageResourceId = imageResourceId;
            this.stepNumber = stepNumber;
            this.description = description;
        }

        public int getImageResourceId() {
            return imageResourceId;
        }

        public String getStepNumber() {
            return stepNumber;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * ViewHolder for tutorial slides
     */
    public static class TutorialViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivTutorialImage;
        private final TextView tvStepNumber;
        private final TextView tvDescription;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTutorialImage = itemView.findViewById(R.id.iv_tutorial_image);
            tvStepNumber = itemView.findViewById(R.id.tv_tutorial_step_number);
            tvDescription = itemView.findViewById(R.id.tv_tutorial_description);
        }

        public void bind(TutorialSlide slide) {
            ivTutorialImage.setImageResource(slide.getImageResourceId());
            tvStepNumber.setText(slide.getStepNumber());
            tvDescription.setText(slide.getDescription());
        }
    }

    public TutorialPagerAdapter(Context context, List<TutorialSlide> tutorialSlides) {
        this.context = context;
        this.tutorialSlides = tutorialSlides;
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.tutorial_slide_item, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        holder.bind(tutorialSlides.get(position));
    }

    @Override
    public int getItemCount() {
        return tutorialSlides.size();
    }
}