package com.example.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class board extends AppCompatActivity {
    private ViewPager2 viewPager;
    private Handler sliderHandler = new Handler();
    private List<Object> imageList; // Supports both URLs and Drawables
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        // Initialize buttons
        setupButtonClickListeners();

        // Initialize ViewPager2 for auto image slider
        setupImageSlider();
    }

    private void setupButtonClickListeners() {
        int[] buttonIds = {R.id.bt1, R.id.bt2, R.id.bt3, R.id.bt4, R.id.bt5};
        for (int id : buttonIds) {
            Button button = findViewById(id);
            if (button != null) {
                if (id == R.id.bt1) {
                    // Handle Open Camera button
                    button.setOnClickListener(v -> {
                        Intent intent = new Intent(board.this, CameraActivity.class);
                        startActivity(intent);
                    });
                } else {
                    // Other buttons show toast
                    button.setOnClickListener(v ->
                            Toast.makeText(board.this, button.getText() + " Clicked", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }
    }

    private void setupImageSlider() {
        viewPager = findViewById(R.id.viewPager);

        // Initialize image list (Drawable resources & URLs)
        imageList = new ArrayList<>();
        imageList.add(R.drawable.image1); // Local drawable image
        imageList.add(R.drawable.image2);
        imageList.add("https://example.com/image1.jpg"); // Online image URL
        imageList.add("https://example.com/image2.jpg");

        // Set up adapter
        ImageSliderAdapter adapter = new ImageSliderAdapter(imageList);
        viewPager.setAdapter(adapter);

        // Auto slide functionality
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                super.onPageSelected(position);
            }
        });

        startAutoSlider();
    }

    private void startAutoSlider() {
        Runnable sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (imageList != null && !imageList.isEmpty()) {
                    currentPage = (currentPage + 1) % imageList.size();
                    viewPager.setCurrentItem(currentPage, true);
                    sliderHandler.postDelayed(this, 3000); // Change image every 3 seconds
                }
            }
        };

        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAutoSlider();
    }
}
