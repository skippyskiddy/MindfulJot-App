package adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import edu.northeastern.numad25sp_group4.R;
public class EntryImageAdapter extends RecyclerView.Adapter<EntryImageAdapter.ImageViewHolder> {

    private final Context context;
    private final List<Uri> imageUris;
    private final OnImageRemoveListener listener;
    private boolean editMode = false; // Add this flag to track edit mode

    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }

    public EntryImageAdapter(Context context, List<Uri> imageUris, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.listener = listener;
    }

    // Add this method to update edit mode state
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        notifyDataSetChanged(); // Refresh all views
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        // Use Picasso to load the image
        Picasso.get()
                .load(imageUri)
                .resize(240, 240)
                .centerCrop()
                .into(holder.ivEntryImage);

        // Only show remove button in edit mode
        holder.btnRemoveImage.setVisibility(editMode ? View.VISIBLE : View.GONE);

        // Set click listener for remove button
        holder.btnRemoveImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEntryImage;
        ImageButton btnRemoveImage;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEntryImage = itemView.findViewById(R.id.iv_entry_image);
            btnRemoveImage = itemView.findViewById(R.id.btn_remove_image);
        }
    }
}