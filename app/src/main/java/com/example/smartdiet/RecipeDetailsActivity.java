package com.example.smartdiet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class RecipeDetailsActivity extends AppCompatActivity {

    private TextView tvDishName, tvDescription, tvLink;
    private ImageView ivRecipeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);

        // Initialize Views
        tvDishName = findViewById(R.id.tvDishName);
        tvDescription = findViewById(R.id.tvDescription);
        tvLink = findViewById(R.id.tvLink);
        ivRecipeImage = findViewById(R.id.ivRecipeImage);

        // Get data from Intent
        Intent intent = getIntent();
        String dishName = intent.getStringExtra("dishName");
        String description = intent.getStringExtra("description");
        String link = intent.getStringExtra("link");
        String imageUrl = intent.getStringExtra("imageUrl");

        // Set data to views
        tvDishName.setText(dishName);
        tvDescription.setText(description);
        tvLink.setText(link);

        // Load the image using Glide
        Glide.with(this).load(imageUrl).into(ivRecipeImage);

        // Set click listener for the link to open in browser
        tvLink.setOnClickListener(v -> openLink(link));
    }

    // Open the link in a browser
    private void openLink(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
        }
    }
}
