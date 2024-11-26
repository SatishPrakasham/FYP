package com.example.smartdiet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Locale;

public class ProgressPage extends AppCompatActivity {

    private ProgressBar progressCalories, progressProtein, progressCarbs;
    private TextView tvCaloriesProgress, tvProteinProgress, tvCarbsProgress;
    private FirebaseFirestore firestore;
    private String userId;  // Store user ID passed from login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_page);

        // Initialize views
        progressCalories = findViewById(R.id.progressCalories);
        progressProtein = findViewById(R.id.progressProtein);
        progressCarbs = findViewById(R.id.progressCarbs);
        tvCaloriesProgress = findViewById(R.id.tvCaloriesProgress);
        tvProteinProgress = findViewById(R.id.tvProteinProgress);
        tvCarbsProgress = findViewById(R.id.tvCarbsProgress);

        firestore = FirebaseFirestore.getInstance();

        // Get userId passed from login
        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no user ID
            return;
        }

        // Load progress data for the specific user
        loadUserProgress();

        // Setup navigation (optional example for managing bottom navigation clicks)
        setupBottomNavigation();
    }

    private void loadUserProgress() {
        firestore.collection("users").document(userId).collection("NutritionData")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalCalories = 0;
                    double totalProtein = 0;
                    double totalCarbs = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String nutritionInfo = document.getString("nutritionInfo");
                        if (nutritionInfo != null) {
                            totalCalories += extractValueFromInfo(nutritionInfo, "Calories");
                            totalProtein += extractValueFromInfo(nutritionInfo, "Protein");
                            totalCarbs += extractValueFromInfo(nutritionInfo, "Carbs");
                        }
                    }

                    // Update progress bars with loaded data
                    updateProgressBars(totalCalories, totalProtein, totalCarbs);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading progress", e));
    }

    private double extractValueFromInfo(String info, String nutrient) {
        try {
            String[] lines = info.split("\n");
            for (String line : lines) {
                if (line.startsWith(nutrient)) {
                    String value = line.replaceAll("[^\\d.]", "");
                    return Double.parseDouble(value);
                }
            }
        } catch (Exception e) {
            Log.e("Parsing", "Error extracting value for " + nutrient, e);
        }
        return 0;
    }

    private void updateProgressBars(double totalCalories, double totalProtein, double totalCarbs) {
        boolean caloriesGoalMet = totalCalories >= 2000;
        boolean proteinGoalMet = totalProtein >= 70;
        boolean carbsGoalMet = totalCarbs >= 300;

        // Cap the progress at the goals
        totalCalories = Math.min(totalCalories, 2000);
        totalProtein = Math.min(totalProtein, 70);
        totalCarbs = Math.min(totalCarbs, 300);

        // Update progress bars and labels
        progressCalories.setProgress((int) totalCalories);
        tvCaloriesProgress.setText(String.format(Locale.getDefault(),
                "Calories: %.0f / 2000 kcal", totalCalories));

        progressProtein.setProgress((int) totalProtein);
        tvProteinProgress.setText(String.format(Locale.getDefault(),
                "Protein: %.0f / 70 g", totalProtein));

        progressCarbs.setProgress((int) totalCarbs);
        tvCarbsProgress.setText(String.format(Locale.getDefault(),
                "Carbs: %.0f / 300 g", totalCarbs));

        // Change color if goals are met
        if (caloriesGoalMet) {
            progressCalories.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
        }
        if (proteinGoalMet) {
            progressProtein.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
        }
        if (carbsGoalMet) {
            progressCarbs.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
        }

        // Check if all goals are met and reset if needed
        if (caloriesGoalMet && proteinGoalMet && carbsGoalMet) {
            showCompletionAndReset();
        }
    }

    private void showCompletionAndReset() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You've met your daily nutrition goals! Your progress will reset now.")
                .setPositiveButton("OK", (dialog, which) -> {
                    resetProgressBars();
                    dialog.dismiss();
                })
                .show();
    }

    private void resetProgressBars() {
        // Reset progress bars to 0
        progressCalories.setProgress(0);
        progressProtein.setProgress(0);
        progressCarbs.setProgress(0);

        // Reset labels
        tvCaloriesProgress.setText("Calories: 0 / 2000 kcal");
        tvProteinProgress.setText("Protein: 0 / 70 g");
        tvCarbsProgress.setText("Carbs: 0 / 300 g");

        // Clear user's nutrition data from Firestore to reset tracking
        clearUserNutritionData();
    }

    private void clearUserNutritionData() {
        firestore.collection("users").document(userId).collection("NutritionData")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error clearing data", e));
    }

    private void setupBottomNavigation() {
        findViewById(R.id.iconTrophy).setOnClickListener(view -> {
            // Navigate to Achievement Page
            startActivity(new Intent(this, ProgressPage.class));
        });
        findViewById(R.id.iconMenu).setOnClickListener(view -> {
            // Navigate to Menu Page
            startActivity(new Intent(this, RecipePage.class));
        });
        findViewById(R.id.iconCommunity).setOnClickListener(view -> {
            // Navigate to Community Page
            startActivity(new Intent(this, Homepage.class));
        });
        findViewById(R.id.iconScan).setOnClickListener(view -> {
            // Navigate to Scan Page
            startActivity(new Intent(this, FoodScanScreen.class));
        });
        findViewById(R.id.iconProfile).setOnClickListener(view -> {
            // Navigate to Profile Page
            startActivity(new Intent(this, Profile_page.class));
        });
    }
}
