package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

        public TutorialSlide(int imageResourceId) {
            this.imageResourceId = imageResourceId;
        }

        public int getImageResourceId() {
            return imageResourceId;
        }
    }

    /**
     * ViewHolder for tutorial slides
     */
    public static class TutorialViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivTutorialImage;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTutorialImage = itemView.findViewById(R.id.iv_tutorial_image);
        }

        public void bind(TutorialSlide slide) {
            ivTutorialImage.setImageResource(slide.getImageResourceId());
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