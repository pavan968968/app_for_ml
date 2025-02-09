package com.example.mainactivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private final List<Object> imageList; // Accepts both String (URLs) and Integer (Drawables)

    public ImageSliderAdapter(List<Object> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.slider_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Object image = imageList.get(position);

        if (image instanceof String) {
            // Load from URL using Glide
            Glide.with(holder.imageView.getContext())
                    .load((String) image)
                    .into(holder.imageView);
        } else if (image instanceof Integer) {
            // Load from drawable resource
            holder.imageView.setImageResource((Integer) image);
        }
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    // ViewHolder class
    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage); // Ensure this ID exists in slider_item.xml
        }
    }
}
