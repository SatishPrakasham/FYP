package com.example.smartdiet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class RecipePage extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private ListView recipeListView;
    private Spinner spinnerProteinType;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private String userId, userGoal, userHealthCondition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_page);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Retrieve userId from Intent
        userId = getIntent().getStringExtra("userId");

        // Initialize UI components
        recipeListView = findViewById(R.id.recipeListView);
        spinnerProteinType = findViewById(R.id.spinnerProteinType);

        // Initialize ListView and Adapter
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(this, recipeList);
        recipeListView.setAdapter(recipeAdapter);

        // Load user profile to determine goal and health condition
        loadUserProfile();

        // Handle Spinner item selection
        spinnerProteinType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProtein = parent.getItemAtPosition(position).toString();
                String collectionName = getCollectionName(userGoal, userHealthCondition); // Get collection name
                fetchRecipes(collectionName, selectedProtein); // Fetch recipes from dynamic collection
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUserProfile() {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userGoal = documentSnapshot.getString("goal");
                        userHealthCondition = documentSnapshot.getString("healthConditions");
                    }
                    // Load protein types into the Spinner once user profile is loaded
                    loadProteinTypes();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show());
    }

    private void loadProteinTypes() {
        List<String> proteinTypes = new ArrayList<>();
        proteinTypes.add("All");
        proteinTypes.add("Chicken");
        proteinTypes.add("Fish");
        proteinTypes.add("Beef");
        proteinTypes.add("Vegetarian");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, proteinTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProteinType.setAdapter(spinnerAdapter);
    }

    private String getCollectionName(String goal, String healthCondition) {
        if (healthCondition.equals("None")) {
            return goal; // e.g., "Gain Weight"
        } else {
            return goal + " " + healthCondition; // e.g., "Gain Weight Diabetes"
        }
    }

    private void fetchRecipes(String collectionName, String proteinType) {
        recipeList.clear(); // Clear the existing list

        firestore.collection(collectionName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String dishName = doc.getString("dishName");
                        String type = doc.getString("proteinType");
                        String description = doc.getString("description");
                        String link = doc.getString("link");
                        String imageUrl = doc.getString("imageUrl");

                        // Filter recipes based on selected protein type
                        if (proteinType.equals("All") || type.equals(proteinType)) {
                            Recipe recipe = new Recipe(dishName, type, description, link, imageUrl);
                            recipeList.add(recipe);
                        }
                    }
                    recipeAdapter.notifyDataSetChanged(); // Refresh the ListView
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load recipes", Toast.LENGTH_SHORT).show());
    }
}
