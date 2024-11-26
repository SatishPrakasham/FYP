package com.example.smartdiet;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class RecipeAdapter extends ArrayAdapter<Recipe> {

    public RecipeAdapter(Context context, List<Recipe> recipes) {
        super(context, 0, recipes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
        }

        Recipe recipe = getItem(position);

        TextView dishNameTextView = convertView.findViewById(android.R.id.text1);
        TextView proteinTypeTextView = convertView.findViewById(android.R.id.text2);

        if (recipe != null) {
            dishNameTextView.setText(recipe.getDishName());
            proteinTypeTextView.setText(recipe.getProteinType());

            // Set click listener to open RecipeDetailsActivity
            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RecipeDetailsActivity.class);
                intent.putExtra("dishName", recipe.getDishName());
                intent.putExtra("description", recipe.getDescription());
                intent.putExtra("link", recipe.getLink());
                intent.putExtra("imageUrl", recipe.getImageUrl());
                getContext().startActivity(intent);
            });
        }

        return convertView;
    }
}
